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

    private DataDeliveryContractDto loadContract(String contractId, String activeRoleOrgaId, String authToken) {
        ContractDto contract = contractStorageService.getContractDetails(contractId, authToken);
        if (!(contract instanceof DataDeliveryContractDto dataDeliveryContractDetailsDto)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Provided contract is not of type Data Delivery.");
        }
        if (!contract.getDetails().getState().equals(ContractState.RELEASED.name())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Provided contract is in wrong state.");
        }
        checkTransferAuthorization(dataDeliveryContractDetailsDto, activeRoleOrgaId);
        return dataDeliveryContractDetailsDto;
    }

    private void checkTransferAuthorization(DataDeliveryContractDto contractDto, String activeRoleOrgaId) {
        boolean isConsumer = activeRoleOrgaId.equals(contractDto.getDetails().getConsumerId().replace(ORGA_PREFIX, ""));
        boolean isProvider = activeRoleOrgaId.equals(contractDto.getDetails().getProviderId().replace(ORGA_PREFIX, ""));
        ServiceOfferingDetails offeringDetails = contractDto.getOffering();
        String dataTransferType = offeringDetails.getSelfDescription().get("verifiableCredential")
                .get("credentialSubject").get("merlot:dataTransferType").get("@value").asText();

        if (!((dataTransferType.equals("Push") && isProvider) ||
                (dataTransferType.equals("Pull") && isConsumer)
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
     * @param contractId         contract id
     * @param activeRoleOrgaId   currently active role
     * @param authToken          user auth token
     * @return negotiation initiation response
     */
    public IdResponse initiateConnectorNegotiation(String contractId, String activeRoleOrgaId, String authToken) {
        DataDeliveryContractDto contractDto = loadContract(contractId, activeRoleOrgaId, authToken);

        OrganisationConnectorExtension providerConnector = getOrgaConnector(contractDto.getDetails().getProviderId(),
                contractDto.getProvisioning().getSelectedProviderConnectorId());
        OrganisationConnectorExtension consumerConnector = getOrgaConnector(contractDto.getDetails().getConsumerId(),
                contractDto.getProvisioning().getSelectedConsumerConnectorId());

        EdcClient providerEdcClient = edcClientProvider.getObject(providerConnector);
        EdcClient consumerEdcClient = edcClientProvider.getObject(consumerConnector);

        String contractUuid = contractDto.getDetails().getId().replace("Contract:", "");
        String instanceUuid = contractUuid + "_" + UUID.randomUUID();

        String assetId = instanceUuid + "_Asset";
        String assetName = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
                + "/contract/" + contractDto.getDetails().getId();
        String assetDescription = "Asset automatically generated from MERLOT to execute contract " + contractDto.getDetails().getId();
        String policyId = instanceUuid + "_Policy";
        String contractDefinitionId = instanceUuid + "_ContractDefinition";

        // provider side
        // create asset
        AssetCreateRequest assetCreateRequest = AssetCreateRequest.builder()
                .asset(Asset.builder()
                        .id(assetId)
                        .properties(AssetProperties.builder()
                                .name(assetName)
                                .description(assetDescription)
                                .version("")
                                .contenttype("")
                                .build())
                        .build())
                .dataAddress(IonosS3DataAddress.builder()
                        .name(contractDto.getProvisioning().getDataAddressSourceBucketName())
                        .bucketName(contractDto.getProvisioning().getDataAddressSourceBucketName())
                        .container(providerConnector.getOrgaId())
                        .blobName(contractDto.getProvisioning().getDataAddressSourceFileName())
                        .keyName(contractDto.getProvisioning().getDataAddressSourceFileName())
                        .storage("s3-eu-central-1.ionoscloud.com")  // TODO move this to bucket parameters?
                        .build())
                .build();
        logger.debug("Creating Asset {} on {}", assetCreateRequest, providerConnector);
        IdResponse assetIdResponse = providerEdcClient.createAsset(assetCreateRequest);

        // create policy
        PolicyCreateRequest policyCreateRequest = PolicyCreateRequest.builder()
                .id(policyId)
                .policy(Policy.builder()
                        .id(policyId)
                        .obligation(Collections.emptyList())
                        .prohibition(Collections.emptyList())
                        .permission(Collections.emptyList())
                        .build())
                .build();
        logger.debug("Creating Policy {} on {}", policyCreateRequest, providerConnector);
        IdResponse policyIdResponse = providerEdcClient.createPolicy(policyCreateRequest);

        // create contract definition
        ContractDefinitionCreateRequest contractDefinitionCreateRequest = ContractDefinitionCreateRequest.builder()
                .id(contractDefinitionId)
                .contractPolicyId(policyIdResponse.getId())
                .accessPolicyId(policyIdResponse.getId())
                .assetsSelector(List.of(Criterion.builder()
                        .operandLeft("https://w3id.org/edc/v0.0.1/ns/id")
                        .operator("=")
                        .operandRight(assetId)
                        .build()))
                .build();
        logger.debug("Creating Contract Definition {} on {}", contractDefinitionCreateRequest, providerConnector);
        providerEdcClient.createContractDefinition(contractDefinitionCreateRequest);

        // consumer side
        // find the offering we are interested in
        CatalogRequest catalogRequest = new CatalogRequest(providerConnector.getProtocolBaseUrl());
        logger.debug("Query Catalog with request {} on {}", catalogRequest, consumerConnector);
        DcatCatalog catalog = consumerEdcClient.queryCatalog(catalogRequest);
        List<DcatDataset> matches =
                catalog.getDataset().stream().filter(d -> d.getAssetId().equals(assetIdResponse.getId())).toList();
        if (matches.size() != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find the asset in the provider catalog.");
        }
        DcatDataset dataset = matches.get(0);

        // negotiate offer
        NegotiationInitiateRequest negotiationInitiateRequest = NegotiationInitiateRequest.builder()
                .connectorId(catalog.getParticipantId())
                .providerId(catalog.getParticipantId())
                .consumerId(consumerConnector.getConnectorId())
                .connectorAddress(providerConnector.getProtocolBaseUrl())
                .offer(ContractOffer.builder()
                        .offerId(dataset.getHasPolicy().get(0).getId())
                        .assetId(dataset.getAssetId())
                        .policy(dataset.getHasPolicy().get(0))
                        .build())
                .build();
        logger.debug("Negotiate Offer with request {} on {}", negotiationInitiateRequest, consumerConnector);
        return consumerEdcClient.negotiateOffer(negotiationInitiateRequest);
    }

    /**
     * Given a negotiation id and a contract, return the current status of the automated EDC negotiation.
     *
     * @param negotiationId      negotiation id
     * @param contractId         contract id
     * @param activeRoleOrgaId   currently active role
     * @param authToken          user auth token
     * @return status of negotiation
     */
    public ContractNegotiation getNegotationStatus(String negotiationId, String contractId, String activeRoleOrgaId,
                                                   String authToken) {
        DataDeliveryContractDto contractDto = loadContract(contractId, activeRoleOrgaId, authToken);

        OrganisationConnectorExtension consumerConnector = getOrgaConnector(contractDto.getDetails().getConsumerId(),
                contractDto.getProvisioning().getSelectedConsumerConnectorId());

        EdcClient consumerEdcClient = edcClientProvider.getObject(consumerConnector);
        logger.debug("Check status of offer {} on {}", negotiationId, consumerConnector);
        return consumerEdcClient.checkOfferStatus(negotiationId);
    }

    /**
     * Given a (completed) EDC negotiation id and a contract id, start the EDC data transfer over the contract.
     *
     * @param negotiationId      negotiation id
     * @param contractId         contract id
     * @param activeRoleOrgaId   currently active role
     * @param authToken          user auth token
     * @return transfer initiation response
     */
    public IdResponse initiateConnectorTransfer(String negotiationId, String contractId, String activeRoleOrgaId,
                                                String authToken) {
        DataDeliveryContractDto contractDto = loadContract(contractId, activeRoleOrgaId, authToken);

        OrganisationConnectorExtension providerConnector = getOrgaConnector(contractDto.getDetails().getProviderId(),
                contractDto.getProvisioning().getSelectedProviderConnectorId());
        OrganisationConnectorExtension consumerConnector = getOrgaConnector(contractDto.getDetails().getConsumerId(),
                contractDto.getProvisioning().getSelectedConsumerConnectorId());

        EdcClient consumerEdcClient = edcClientProvider.getObject(consumerConnector);

        ContractNegotiation negotiation = getNegotationStatus(negotiationId, contractId, activeRoleOrgaId, authToken);

        // agreement id is always formatted as contract_definition_id:assetId:random_uuid
        TransferRequest transferRequest = TransferRequest.builder()
                .connectorId(providerConnector.getConnectorId())
                .connectorAddress(negotiation.getCounterPartyAddress())
                .contractId(negotiation.getContractAgreementId())
                .assetId(negotiation.getContractAgreementId().split(":")[1])
                .dataDestination(IonosS3DataAddress.builder()
                        .name(contractDto.getProvisioning().getDataAddressTargetBucketName())
                        .bucketName(contractDto.getProvisioning().getDataAddressTargetBucketName())
                        .container(contractDto.getDetails().getConsumerId())
                        .blobName(contractDto.getProvisioning().getDataAddressTargetFileName())
                        .keyName(contractDto.getProvisioning().getDataAddressTargetFileName())
                        .storage("s3-eu-central-1.ionoscloud.com")
                        .build())
                .build();
        logger.debug("Initiate transfer with request {} on {}", transferRequest, consumerConnector);
        return consumerEdcClient.initiateTransfer(transferRequest);
    }

    /**
     * Given a transfer id and a contract, get the current status of the data transfer.
     *
     * @param transferId         transfer id
     * @param contractId         contract id
     * @param activeRoleOrgaId   currently active role
     * @param authToken          user auth token
     * @return status of transfer
     */
    public IonosS3TransferProcess getTransferStatus(String transferId, String contractId, String activeRoleOrgaId,
                                                    String authToken) {
        DataDeliveryContractDto contractDto = loadContract(contractId, activeRoleOrgaId, authToken);

        OrganisationConnectorExtension consumerConnector = getOrgaConnector(contractDto.getDetails().getConsumerId(),
                contractDto.getProvisioning().getSelectedConsumerConnectorId());

        EdcClient consumerEdcClient = edcClientProvider.getObject(consumerConnector);
        logger.debug("Check status of transfer {} on {}", transferId, consumerConnector);

        // TODO deprovision on finalized transfer
        // curl -X POST -H 'X-Api-Key: password' "http://localhost:9192/management/v2/transferprocesses/{transferProcessId}/deprovision"

        return consumerEdcClient.checkTransferStatus(transferId);
    }

}
