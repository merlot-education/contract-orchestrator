package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.models.dto.ContractDto;
import eu.merloteducation.contractorchestrator.models.dto.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.contractorchestrator.models.edc.catalog.CatalogRequest;
import eu.merloteducation.contractorchestrator.models.edc.contractdefinition.ContractDefinitionCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.contractdefinition.Criterion;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.NegotiationInitiateRequest;
import eu.merloteducation.contractorchestrator.models.edc.policy.PolicyCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.transfer.TransferRequest;
import eu.merloteducation.contractorchestrator.models.organisationsorchestrator.OrganisationConnectorExtension;
import eu.merloteducation.contractorchestrator.models.edc.asset.*;
import eu.merloteducation.contractorchestrator.models.edc.catalog.DcatCatalog;
import eu.merloteducation.contractorchestrator.models.edc.catalog.DcatDataset;
import eu.merloteducation.contractorchestrator.models.edc.common.IdResponse;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractNegotiation;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractOffer;
import eu.merloteducation.contractorchestrator.models.edc.policy.Policy;
import eu.merloteducation.contractorchestrator.models.edc.transfer.IonosS3TransferProcess;
import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator.ServiceOfferingDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.*;

@Service
public class EdcOrchestrationService {

    private static final String ORGA_PREFIX = "Participant:";

    private final Logger logger = LoggerFactory.getLogger(EdcOrchestrationService.class);
    @Autowired
    private MessageQueueService messageQueueService;

    @Autowired
    private ContractStorageService contractStorageService;

    @Autowired
    private ObjectProvider<EdcClient> edcClientProvider;

    private DataDeliveryContractDto validateContract(ContractDto template) {
        if (!(template instanceof DataDeliveryContractDto dataDeliveryContractDetailsDto)){
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Provided contract is not of type Data Delivery.");
        }
        if (!template.getDetails().getState().equals(ContractState.RELEASED.name())){
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Provided contract is in wrong state.");
        }
        return dataDeliveryContractDetailsDto;
    }

    private void checkTransferAuthorization(DataDeliveryContractDto template, String activeRoleOrgaId) {
        boolean isConsumer = activeRoleOrgaId.equals(template.getDetails().getConsumerId().replace(ORGA_PREFIX, ""));
        boolean isProvider = activeRoleOrgaId.equals(template.getDetails().getProviderId().replace(ORGA_PREFIX, ""));
        ServiceOfferingDetails offeringDetails = template.getOffering();
        String dataTransferType = offeringDetails.getSelfDescription().get("verifiableCredential")
                .get("credentialSubject").get("merlot:dataTransferType").get("@value").asText();

        if (!((dataTransferType.equals("Push") && isProvider) ||
                        (dataTransferType.equals("Pull")&& isConsumer)
        )) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your role is not authorized to perform the data transfer");
        }
    }

    private OrganisationConnectorExtension getOrgaConnector(String orgaId, String connectorId) {
        return messageQueueService
                .remoteRequestOrganizationConnectorByConnectorId(
                        orgaId.replace(ORGA_PREFIX, ""), connectorId);
    }

    /**
     * Given a contract id, a role and a set of represented organizations, start the automated EDC negotiation
     * over the contract.
     *
     * @param contractId contract id
     * @param activeRoleOrgaId currently active role
     * @param representedOrgaIds represented organizations
     * @param authToken user auth token
     * @return negotiation initiation response
     */
    public IdResponse initiateConnectorNegotiation(String contractId, String activeRoleOrgaId,
                                                   Set<String> representedOrgaIds, String authToken) {
        DataDeliveryContractDto template = validateContract(
                contractStorageService.getContractDetails(contractId, representedOrgaIds, authToken));
        checkTransferAuthorization(template, activeRoleOrgaId);

        OrganisationConnectorExtension providerConnector = getOrgaConnector(template.getDetails().getProviderId(),
                template.getProvisioning().getSelectedProviderConnectorId());
        OrganisationConnectorExtension consumerConnector = getOrgaConnector(template.getDetails().getConsumerId(),
                template.getProvisioning().getSelectedConsumerConnectorId());

        EdcClient providerEdcClient = edcClientProvider.getObject(providerConnector);
        EdcClient consumerEdcClient = edcClientProvider.getObject(consumerConnector);

        String contractUuid = template.getDetails().getId().replace("Contract:", "");
        String instanceUuid = contractUuid + "_" + UUID.randomUUID();

        String assetId = instanceUuid + "_Asset";
        String assetName = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
                + "/contract/" + template.getDetails().getId();
        String assetDescription = "Asset automatically generated from MERLOT to execute contract " + template.getDetails().getId();
        String policyId = instanceUuid + "_Policy";
        String contractDefinitionId = instanceUuid + "_ContractDefinition";

        // provider side
        // create asset
        AssetCreateRequest assetCreateRequest = new AssetCreateRequest();
        assetCreateRequest.setAsset(
                new Asset(assetId, new AssetProperties(assetName, assetDescription, "", "")));
        assetCreateRequest.setDataAddress(
                new IonosS3DataAddress(
                template.getProvisioning().getDataAddressSourceBucketName(),
                template.getProvisioning().getDataAddressSourceBucketName(),
                providerConnector.getOrgaId(),
                template.getProvisioning().getDataAddressSourceFileName(),
                template.getProvisioning().getDataAddressSourceFileName(),
                "s3-eu-central-1.ionoscloud.com"));
        IdResponse assetIdResponse = providerEdcClient.createAsset(assetCreateRequest);

        // create policy
        Policy policy = new Policy(policyId);
        PolicyCreateRequest policyCreateRequest = new PolicyCreateRequest();
        policyCreateRequest.setPolicy(policy);
        policyCreateRequest.setId(policy.getId());
        IdResponse policyIdResponse = providerEdcClient.createPolicy(policyCreateRequest);

        // create contract definition
        ContractDefinitionCreateRequest createRequest = new ContractDefinitionCreateRequest();
        createRequest.setId(contractDefinitionId);
        createRequest.setAccessPolicyId(policyIdResponse.getId());
        createRequest.setContractPolicyId(policyIdResponse.getId());
        Criterion assetCriterion = new Criterion();
        assetCriterion.setOperator("=");
        assetCriterion.setOperandLeft("https://w3id.org/edc/v0.0.1/ns/id");
        assetCriterion.setOperandRight(assetId);
        createRequest.setAssetsSelector(List.of(assetCriterion));
        providerEdcClient.createContractDefinition(createRequest);

        // consumer side
        // find the offering we are interested in
        CatalogRequest catalogRequest = new CatalogRequest();
        catalogRequest.setProviderUrl(providerConnector.getProtocolBaseUrl());
        DcatCatalog catalog = consumerEdcClient.queryCatalog(catalogRequest);
        List<DcatDataset> matches = catalog.getDataset().stream().filter(d -> d.getAssetId().equals(assetIdResponse.getId())).toList();
        if(matches.size() != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find the asset in the provider catalog.");
        }
        DcatDataset dataset = matches.get(0);

        // negotiate offer
        NegotiationInitiateRequest initiateRequest = new NegotiationInitiateRequest();
        initiateRequest.setConnectorId(catalog.getParticipantId());
        initiateRequest.setConsumerId(consumerConnector.getConnectorId());
        initiateRequest.setProviderId(catalog.getParticipantId());
        initiateRequest.setConnectorAddress(providerConnector.getProtocolBaseUrl());
        initiateRequest.setOffer(new ContractOffer(
                dataset.getHasPolicy().get(0).getId(), dataset.getAssetId(), dataset.getHasPolicy().get(0)
        ));
        return consumerEdcClient.negotiateOffer(initiateRequest);
    }

    /**
     * Given a negotiation id and a contract, return the current status of the automated EDC negotiation.
     *
     * @param negotiationId negotiation id
     * @param contractId contract id
     * @param activeRoleOrgaId currently active role
     * @param representedOrgaIds represented organizations
     * @param authToken user auth token
     * @return status of negotiation
     */
    public ContractNegotiation getNegotationStatus(String negotiationId, String contractId, String activeRoleOrgaId,
                                                   Set<String> representedOrgaIds, String authToken) {
        DataDeliveryContractDto template = validateContract(
                contractStorageService.getContractDetails(contractId, representedOrgaIds, authToken));
        checkTransferAuthorization(template, activeRoleOrgaId);

        OrganisationConnectorExtension consumerConnector = getOrgaConnector(template.getDetails().getConsumerId(),
                template.getProvisioning().getSelectedConsumerConnectorId());

        EdcClient consumerEdcClient = edcClientProvider.getObject(consumerConnector);

        return consumerEdcClient.checkOfferStatus(negotiationId);
    }

    /**
     * Given a (completed) EDC negotiation id and a contract id, start the EDC data transfer over the contract.
     *
     * @param negotiationId negotiation id
     * @param contractId contract id
     * @param activeRoleOrgaId currently active role
     * @param representedOrgaIds represented organizations
     * @param authToken user auth token
     * @return transfer initiation response
     */
    public IdResponse initiateConnectorTransfer(String negotiationId, String contractId, String activeRoleOrgaId,
                                                Set<String> representedOrgaIds, String authToken) {
        DataDeliveryContractDto template = validateContract(
                contractStorageService.getContractDetails(contractId, representedOrgaIds, authToken));
        checkTransferAuthorization(template, activeRoleOrgaId);

        OrganisationConnectorExtension providerConnector = getOrgaConnector(template.getDetails().getProviderId(),
                template.getProvisioning().getSelectedProviderConnectorId());
        OrganisationConnectorExtension consumerConnector = getOrgaConnector(template.getDetails().getConsumerId(),
                template.getProvisioning().getSelectedConsumerConnectorId());

        EdcClient consumerEdcClient = edcClientProvider.getObject(consumerConnector);

        ContractNegotiation negotiation = getNegotationStatus(negotiationId, contractId, activeRoleOrgaId,
                representedOrgaIds, authToken);

        // agreement id is always formatted as contract_definition_id:assetId:random_uuid
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setConnectorId(providerConnector.getConnectorId());
        transferRequest.setConnectorAddress(negotiation.getCounterPartyAddress());
        transferRequest.setContractId(negotiation.getContractAgreementId());
        transferRequest.setAssetId(negotiation.getContractAgreementId().split(":")[1]);
        transferRequest.setDataDestination(new IonosS3DataAddress(
                template.getProvisioning().getDataAddressTargetBucketName(),
                template.getProvisioning().getDataAddressTargetBucketName(),
                template.getDetails().getConsumerId(),
                template.getProvisioning().getDataAddressTargetFileName(),
                template.getProvisioning().getDataAddressTargetFileName(),
                "s3-eu-central-1.ionoscloud.com"
        ));
        return consumerEdcClient.initiateTransfer(transferRequest);
    }

    /**
     * Given a transfer id and a contract, get the current status of the data transfer.
     *
     * @param transferId transfer id
     * @param contractId contract id
     * @param activeRoleOrgaId currently active role
     * @param representedOrgaIds represented organizations
     * @param authToken user auth token
     * @return status of transfer
     */
    public IonosS3TransferProcess getTransferStatus(String transferId, String contractId, String activeRoleOrgaId,
                                                    Set<String> representedOrgaIds, String authToken) {
        DataDeliveryContractDto template = validateContract(
                contractStorageService.getContractDetails(contractId, representedOrgaIds, authToken));
        checkTransferAuthorization(template, activeRoleOrgaId);

        OrganisationConnectorExtension consumerConnector = getOrgaConnector(template.getDetails().getConsumerId(),
                template.getProvisioning().getSelectedConsumerConnectorId());

        EdcClient consumerEdcClient = edcClientProvider.getObject(consumerConnector);

        return consumerEdcClient.checkTransferStatus(transferId);
    }

}
