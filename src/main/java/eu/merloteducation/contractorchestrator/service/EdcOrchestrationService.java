package eu.merloteducation.contractorchestrator.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.contractorchestrator.models.OrganisationConnectorExtension;
import eu.merloteducation.contractorchestrator.models.OrganizationDetails;
import eu.merloteducation.contractorchestrator.models.edc.asset.*;
import eu.merloteducation.contractorchestrator.models.edc.catalog.CatalogRequest;
import eu.merloteducation.contractorchestrator.models.edc.catalog.DcatCatalog;
import eu.merloteducation.contractorchestrator.models.edc.catalog.DcatDataset;
import eu.merloteducation.contractorchestrator.models.edc.common.IdResponse;
import eu.merloteducation.contractorchestrator.models.edc.contractdefinition.ContractDefinitionCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.contractdefinition.Criterion;
import eu.merloteducation.contractorchestrator.models.edc.dataplane.DataPlaneCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractNegotiation;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractOffer;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.NegotiationInitiateRequest;
import eu.merloteducation.contractorchestrator.models.edc.policy.Policy;
import eu.merloteducation.contractorchestrator.models.edc.policy.PolicyCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.transfer.IonosS3TransferProcess;
import eu.merloteducation.contractorchestrator.models.edc.transfer.TransferProcess;
import eu.merloteducation.contractorchestrator.models.edc.transfer.TransferRequest;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.DataDeliveryContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.DataDeliveryProvisioning;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class EdcOrchestrationService {

    private static final String ORGA_PREFIX = "Participant:";
    @Autowired
    private MessageQueueService messageQueueService;

    @Autowired
    private ContractStorageService contractStorageService;

    @Autowired
    private RestTemplate restTemplate;

    private void createDataplane(String transferUrl, String publicApiUrl, String managementUrl, String accessToken) {
        DataPlaneCreateRequest dataPlaneCreateRequest = new DataPlaneCreateRequest();
        dataPlaneCreateRequest.setId("http-push-dataplane");
        dataPlaneCreateRequest.setUrl(transferUrl);
        List<String> sourceTypes = new ArrayList<>();
        sourceTypes.add("HttpData");
        sourceTypes.add("HttpProxy");
        dataPlaneCreateRequest.setAllowedSourceTypes(sourceTypes);
        List<String> destTypes = new ArrayList<>();
        destTypes.add("HttpData");
        dataPlaneCreateRequest.setAllowedDestTypes(destTypes);
        Map<String, String> properties = new HashMap<>();
        properties.put("publicApiUrl", publicApiUrl);
        dataPlaneCreateRequest.setProperties(properties);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", accessToken);
        HttpEntity<DataPlaneCreateRequest> request = new HttpEntity<>(dataPlaneCreateRequest, headers);
        String response =
                restTemplate.exchange(managementUrl + "instances",
                        HttpMethod.POST, request, String.class).getBody();
        System.out.println(response);
    }

    private IdResponse createAsset(Asset asset, DataAddress dataAddress, String managementUrl, String accessToken) {
        AssetCreateRequest assetCreateRequest = new AssetCreateRequest();
        assetCreateRequest.setAsset(asset);
        assetCreateRequest.setDataAddress(dataAddress);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", accessToken);
        HttpEntity<AssetCreateRequest> request = new HttpEntity<>(assetCreateRequest, headers);
        String response =
                restTemplate.exchange(managementUrl + "v2/assets",
                        HttpMethod.POST, request, String.class).getBody();
        System.out.println(response);

        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        IdResponse idResponse = null;
        try {
            idResponse = mapper.readValue(response, IdResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return idResponse;
    }

    private IdResponse createPolicyUnrestricted(Policy policy, String managementUrl, String accessToken) {
        PolicyCreateRequest policyCreateRequest = new PolicyCreateRequest();
        policyCreateRequest.setPolicy(policy);
        policyCreateRequest.setId(policy.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", accessToken);
        HttpEntity<PolicyCreateRequest> request = new HttpEntity<>(policyCreateRequest, headers);
        String response =
                restTemplate.exchange(managementUrl + "v2/policydefinitions",
                        HttpMethod.POST, request, String.class).getBody();
        System.out.println(response);

        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        IdResponse idResponse = null;
        try {
            idResponse = mapper.readValue(response, IdResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return idResponse;
    }

    private IdResponse createContractDefinition(String contractDefinitionId, String accessPolicyId, String contractPolicyid,
                                                String assetId, String managementUrl, String accessToken) { // TODO add asset selector
        ContractDefinitionCreateRequest createRequest = new ContractDefinitionCreateRequest();
        createRequest.setId(contractDefinitionId);
        createRequest.setAccessPolicyId(accessPolicyId);
        createRequest.setContractPolicyId(contractPolicyid);
        List<Criterion> assetSelector = new ArrayList<>();
        //Criterion assetCriterion = new Criterion();
        //assetCriterion.setOperator("=");
        //assetCriterion.setOperandLeft("asset:prop:id");
        //List<String> targetAssets = new ArrayList<>();
        //targetAssets.add(assetId);
        //assetCriterion.setOperandRight(assetId);
        //assetSelector.add(assetCriterion);
        createRequest.setAssetsSelector(assetSelector);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", accessToken);
        HttpEntity<ContractDefinitionCreateRequest> request = new HttpEntity<>(createRequest, headers);
        String response =
                restTemplate.exchange(managementUrl + "v2/contractdefinitions",
                        HttpMethod.POST, request, String.class).getBody();
        System.out.println(response);

        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        IdResponse idResponse = null;
        try {
            idResponse = mapper.readValue(response, IdResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return idResponse;
    }

    private DcatCatalog queryCatalog(String providerProtocolUrl, String managementUrl, String accessToken) {
        System.out.println("Query Catalog");
        CatalogRequest catalogRequest = new CatalogRequest();
        catalogRequest.setProviderUrl(providerProtocolUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", accessToken);
        HttpEntity<CatalogRequest> request = new HttpEntity<>(catalogRequest, headers);
        String response =
                restTemplate.exchange(managementUrl + "v2/catalog/request",
                        HttpMethod.POST, request, String.class).getBody();

        System.out.println(response);

        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DcatCatalog catalogResponse = null;
        try {
            catalogResponse = mapper.readValue(response, DcatCatalog.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(catalogResponse);
        System.out.println(response);
        return catalogResponse;
    }

    private IdResponse negotiateOffer(String connectorId, String consumerId, String providerId, String connectorAddress,
                                      ContractOffer offer, String managementUrl, String accessToken) {
        NegotiationInitiateRequest initiateRequest = new NegotiationInitiateRequest();
        initiateRequest.setConnectorId(connectorId);
        initiateRequest.setConsumerId(consumerId);
        initiateRequest.setProviderId(providerId);
        initiateRequest.setConnectorAddress(connectorAddress);
        initiateRequest.setOffer(offer);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", accessToken);
        HttpEntity<NegotiationInitiateRequest> request = new HttpEntity<>(initiateRequest, headers);
        String response =
                restTemplate.exchange(managementUrl + "v2/contractnegotiations",
                        HttpMethod.POST, request, String.class).getBody();
        System.out.println(response);

        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        IdResponse idResponse = null;
        try {
            idResponse = mapper.readValue(response, IdResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return idResponse;
    }

    private ContractNegotiation checkOfferStatus(String negotiationId, String managementUrl, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", accessToken);
        HttpEntity<NegotiationInitiateRequest> request = new HttpEntity<>(null, headers);
        String response =
                restTemplate.exchange(managementUrl + "v2/contractnegotiations/" + negotiationId,
                        HttpMethod.GET, request, String.class).getBody();
        System.out.println(response);

        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ContractNegotiation contractNegotiation = null;
        try {
            contractNegotiation = mapper.readValue(response, ContractNegotiation.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contractNegotiation;
    }

    private IdResponse initiateTransfer(String connectorId, String connectorAddress, String agreementId, String assetId,
                                        DataAddress dataDestination, String managementUrl, String accessToken) {
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setConnectorId(connectorId);
        transferRequest.setConnectorAddress(connectorAddress);
        transferRequest.setContractId(agreementId);
        transferRequest.setAssetId(assetId);
        transferRequest.setDataDestination(dataDestination);

        System.out.println((IonosS3DataAddress) transferRequest.getDataDestination());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", accessToken);
        HttpEntity<TransferRequest> request = new HttpEntity<>(transferRequest, headers);
        String response =
                restTemplate.exchange(managementUrl + "v2/transferprocesses",
                        HttpMethod.POST, request, String.class).getBody();
        System.out.println(response);

        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        IdResponse idResponse = null;
        try {
            idResponse = mapper.readValue(response, IdResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return idResponse;
    }

    private IonosS3TransferProcess checkTransferStatus(String transferId, String managementUrl, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<NegotiationInitiateRequest> request = new HttpEntity<>(null, headers);
        String response =
                restTemplate.exchange(managementUrl + "v2/transferprocesses/" + transferId,
                        HttpMethod.GET, request, String.class).getBody();
        System.out.println(response);

        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        IonosS3TransferProcess transferProcess = null;
        try {
            transferProcess = mapper.readValue(response, IonosS3TransferProcess.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return transferProcess;
    }

    private DataDeliveryContractTemplate validateContractType(ContractTemplate template) {
        if (!(template instanceof DataDeliveryContractTemplate dataDeliveryContractTemplate)){
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Provided contract is not of type Data Delivery.");
        }
        return dataDeliveryContractTemplate;
    }

    private void checkTransferAuthorization(DataDeliveryContractTemplate template, String activeRoleOrgaId) {
        boolean isConsumer = activeRoleOrgaId.equals(template.getConsumerId().replace(ORGA_PREFIX, ""));
        boolean isProvider = activeRoleOrgaId.equals(template.getProviderId().replace(ORGA_PREFIX, ""));

        if (!(
                (template.getDataTransferType().equals("Push") && isProvider) ||
                        (template.getDataTransferType().equals("Pull")&& isConsumer)
        )) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your role is not authorized to perform the data transfer");
        }
    }

    private OrganisationConnectorExtension getOrgaConnector(String orgaId) {
        List<OrganisationConnectorExtension> connectors = messageQueueService
                .remoteRequestOrganizationConnectors(
                        orgaId.replace("Participant:", ""));

        if (connectors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find a connector for this organization.");
        }
        // TODO for now we assume a single connector per orga
        return connectors.get(0);
    }

    public IdResponse initiateConnectorNegotiation(String contractId, String activeRoleOrgaId,
                                                   Set<String> representedOrgaIds) {
        DataDeliveryContractTemplate template = validateContractType(
                contractStorageService.getContractDetails(contractId, representedOrgaIds));
        checkTransferAuthorization(template, activeRoleOrgaId);
        DataDeliveryProvisioning provisioning = (DataDeliveryProvisioning) template.getServiceContractProvisioning();

        OrganisationConnectorExtension providerConnector = getOrgaConnector(template.getProviderId());
        OrganisationConnectorExtension consumerConnector = getOrgaConnector(template.getConsumerId());

        String providerBaseUrl = providerConnector.getConnectorEndpoint();
        String consumerBaseUrl = consumerConnector.getConnectorEndpoint();

        String contractUuid = template.getId().replace("Contract:", "");
        String instanceUuid = contractUuid + "_" + UUID.randomUUID();

        String assetId = instanceUuid + "_Asset";
        String assetName = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
                + "/contract/" + template.getId();
        String assetDescription = "Asset automatically generated from MERLOT to execute contract " + template.getId();
        String policyId = instanceUuid + "_Policy";
        String contractDefinitionId = instanceUuid + "_ContractDefinition";

        // TODO these port mappings need to be replaced with url mappings once they are hosted in the cloud
        String providerManagementUrl = providerBaseUrl + ":8182/management/";
        String providerProtocolUrl = providerBaseUrl + ":8282/protocol";

        String consumerManagementUrl = consumerBaseUrl + ":9192/management/";

        // provider side
        IdResponse assetIdResponse = createAsset(
                new Asset(assetId, new AssetProperties(assetName, assetDescription, "", "")),
                new IonosS3DataAddress(provisioning.getDataAddressName(), provisioning.getDataAddressSourceBucketName(),
                        providerConnector.getOrgaId(), provisioning.getDataAddressSourceFileName(),
                        provisioning.getDataAddressSourceFileName(),"s3-eu-central-1.ionoscloud.com"),
                providerManagementUrl, providerConnector.getConnectorAccessToken()
        );
        IdResponse policyIdResponse = createPolicyUnrestricted(new Policy(policyId),
                providerManagementUrl, providerConnector.getConnectorAccessToken());
        createContractDefinition(contractDefinitionId, policyIdResponse.getId(),
                policyIdResponse.getId(), assetIdResponse.getId(), providerManagementUrl,
                providerConnector.getConnectorAccessToken());


        // consumer side
        // find the offering we are interested in
        DcatCatalog catalog = queryCatalog(providerProtocolUrl, consumerManagementUrl,
                consumerConnector.getConnectorAccessToken());
        List<DcatDataset> matches = catalog.getDataset().stream().filter(d -> d.getAssetId().equals(assetId)).collect(Collectors.toList());
        if(matches.size() != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find the asset in the provider catalog.");
        }
        DcatDataset dataset = matches.get(0);

        return negotiateOffer(catalog.getParticipantId(), consumerConnector.getConnectorId(),
                catalog.getParticipantId(), providerProtocolUrl,
                new ContractOffer(dataset.getHasPolicy().get(0).getId(), dataset.getAssetId(), dataset.getHasPolicy().get(0)),
                consumerManagementUrl,
                consumerConnector.getConnectorAccessToken());
    }

    public ContractNegotiation getNegotationStatus(String negotiationId, String contractId, String activeRoleOrgaId,
                                                   Set<String> representedOrgaIds) {
        DataDeliveryContractTemplate template = validateContractType(
                contractStorageService.getContractDetails(contractId, representedOrgaIds));
        checkTransferAuthorization(template, activeRoleOrgaId);

        OrganisationConnectorExtension consumerConnector = getOrgaConnector(template.getConsumerId());

        String consumerBaseUrl = consumerConnector.getConnectorEndpoint();
        String consumerManagementUrl = consumerBaseUrl + ":9192/management/";

        return checkOfferStatus(negotiationId, consumerManagementUrl,
                consumerConnector.getConnectorAccessToken());
    }

    public IdResponse initiateConnectorTransfer(String negotiationId, String contractId, String activeRoleOrgaId,
                                                Set<String> representedOrgaIds) {
        DataDeliveryContractTemplate template = validateContractType(
                contractStorageService.getContractDetails(contractId, representedOrgaIds));
        checkTransferAuthorization(template, activeRoleOrgaId);

        DataDeliveryProvisioning provisioning = (DataDeliveryProvisioning) template.getServiceContractProvisioning();
        OrganisationConnectorExtension providerConnector = getOrgaConnector(template.getProviderId());
        OrganisationConnectorExtension consumerConnector = getOrgaConnector(template.getConsumerId());

        String consumerManagementUrl = consumerConnector.getConnectorEndpoint() + ":9192/management/";

        ContractNegotiation negotiation = getNegotationStatus(negotiationId, contractId, activeRoleOrgaId, representedOrgaIds);
        // agreement id is always formatted as {contract_definition_id}:{assetId}:{random_uuid}
        String connectorId = providerConnector.getConnectorId();
        String connectorAddress = negotiation.getCounterPartyAddress();
        String agreementId = negotiation.getContractAgreementId();
        String assetId = negotiation.getContractAgreementId().split(":")[1];
        DataAddress destination =  new IonosS3DataAddress(
                provisioning.getDataAddressName(), provisioning.getDataAddressTargetBucketName(),
                template.getConsumerId(), provisioning.getDataAddressTargetFileName(),
                provisioning.getDataAddressTargetFileName(),"s3-eu-central-1.ionoscloud.com");

        return initiateTransfer(connectorId, connectorAddress, agreementId, assetId, destination, consumerManagementUrl,
                consumerConnector.getConnectorAccessToken());
    }

    public IonosS3TransferProcess getTransferStatus(String transferId, String contractId, String activeRoleOrgaId,
                                                    Set<String> representedOrgaIds) {
        DataDeliveryContractTemplate template = validateContractType(
                contractStorageService.getContractDetails(contractId, representedOrgaIds));
        checkTransferAuthorization(template, activeRoleOrgaId);

        OrganisationConnectorExtension consumerConnector = getOrgaConnector(template.getConsumerId());

        String consumerManagementUrl = consumerConnector.getConnectorEndpoint() + ":9192/management/";

        return checkTransferStatus(transferId, consumerManagementUrl,
                consumerConnector.getConnectorAccessToken());
    }

    public void getAllTransfers(ContractTemplate template, String activeOrgaId) {
        // TODO fetch data from management endpoint by POSTing at /v2/transferprocesses/request
    }

    public void getAllNegotiations(ContractTemplate template, String activeOrgaId) {
        // TODO fetch data from management endpoint by POSTing at /v2/contractnegotiations/request
    }



}
