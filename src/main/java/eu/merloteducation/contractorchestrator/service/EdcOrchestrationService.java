package eu.merloteducation.contractorchestrator.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.contractorchestrator.models.OrganizationDetails;
import eu.merloteducation.contractorchestrator.models.edc.asset.*;
import eu.merloteducation.contractorchestrator.models.edc.catalog.CatalogRequest;
import eu.merloteducation.contractorchestrator.models.edc.catalog.DcatCatalog;
import eu.merloteducation.contractorchestrator.models.edc.common.IdResponse;
import eu.merloteducation.contractorchestrator.models.edc.contractdefinition.ContractDefinitionCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.contractdefinition.Criterion;
import eu.merloteducation.contractorchestrator.models.edc.dataplane.DataPlaneCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractNegotiation;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractOffer;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.NegotiationInitiateRequest;
import eu.merloteducation.contractorchestrator.models.edc.policy.Policy;
import eu.merloteducation.contractorchestrator.models.edc.policy.PolicyCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.transfer.TransferProcess;
import eu.merloteducation.contractorchestrator.models.edc.transfer.TransferRequest;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class EdcOrchestrationService {

    @Autowired
    private MessageQueueService messageQueueService;

    @Autowired
    private RestTemplate restTemplate;

    private void createDataplane(String transferUrl, String publicApiUrl, String managementUrl) {
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
        HttpEntity<DataPlaneCreateRequest> request = new HttpEntity<>(dataPlaneCreateRequest, headers);
        String response =
                restTemplate.exchange(managementUrl + "instances",
                        HttpMethod.POST, request, String.class).getBody();
        System.out.println(response);
    }

    private IdResponse createAsset(String assetId, String assetName, String assetDescription, String assetVersion, String assetContentType,
                                   String dataName, String dataBaseUrl, String dataType,
                                   String managementUrl) {

        AssetCreateRequest assetCreateRequest = new AssetCreateRequest();

        Asset asset = new Asset();
        asset.setId(assetId);
        AssetProperties assetProperties = new AssetProperties();
        assetProperties.setName(assetName);
        assetProperties.setDescription(assetDescription);
        assetProperties.setVersion(assetVersion);
        assetProperties.setContenttype(assetContentType);
        asset.setProperties(assetProperties);

        DataAddress dataAddress = new DataAddress();
        DataAddressProperties dataAddressProperties = new DataAddressProperties();
        dataAddressProperties.setName(dataName);
        dataAddressProperties.setBaseUrl(dataBaseUrl);
        dataAddressProperties.setType(dataType);
        dataAddress.setProperties(dataAddressProperties);

        assetCreateRequest.setAsset(asset);
        assetCreateRequest.setDataAddress(dataAddress);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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

    private IdResponse createPolicyUnrestricted(String policyId, String managementUrl) {
        PolicyCreateRequest policyCreateRequest = new PolicyCreateRequest();
        Policy policy = new Policy();
        policy.setId(policyId);
        policyCreateRequest.setPolicy(policy);
        policyCreateRequest.setId(policyId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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
                                                String assetId, String managementUrl) { // TODO add asset selector
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

    private DcatCatalog queryCatalog(String providerProtocolUrl, String managementUrl) {
        System.out.println("Query Catalog");
        CatalogRequest catalogRequest = new CatalogRequest();
        catalogRequest.setProviderUrl(providerProtocolUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CatalogRequest> request = new HttpEntity<>(catalogRequest, headers);
        String response =
                restTemplate.exchange(managementUrl + "v2/catalog/request",
                        HttpMethod.POST, request, String.class).getBody();

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
                                      String offerId, String assetId, Policy policy, String managementUrl) {
        NegotiationInitiateRequest initiateRequest = new NegotiationInitiateRequest();
        initiateRequest.setConnectorId(connectorId);
        initiateRequest.setConsumerId(consumerId);
        initiateRequest.setProviderId(providerId);
        initiateRequest.setConnectorAddress(connectorAddress);

        ContractOffer offer = new ContractOffer();
        offer.setOfferId(offerId);
        offer.setAssetId(assetId);
        offer.setPolicy(policy);
        initiateRequest.setOffer(offer);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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

    private ContractNegotiation checkOfferStatus(String negotiationId, String managementUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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
                                        String dataDestinationUrl, String managementUrl) {
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setConnectorId(connectorId);
        transferRequest.setConnectorAddress(connectorAddress);
        transferRequest.setContractId(agreementId);
        transferRequest.setAssetId(assetId);
        DataAddress dataDestination = new DataAddress();
        DataAddressProperties dataDestinationProperties = new DataAddressProperties();
        dataDestinationProperties.setType("HttpData");
        dataDestinationProperties.setBaseUrl(dataDestinationUrl);
        dataDestination.setProperties(dataDestinationProperties);
        transferRequest.setDataDestination(dataDestination);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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

    private TransferProcess checkTransferStatus(String transferId, String managementUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<NegotiationInitiateRequest> request = new HttpEntity<>(null, headers);
        String response =
                restTemplate.exchange(managementUrl + "v2/transferprocesses/" + transferId,
                        HttpMethod.GET, request, String.class).getBody();
        System.out.println(response);

        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        TransferProcess transferProcess = null;
        try {
            transferProcess = mapper.readValue(response, TransferProcess.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return transferProcess;
    }

    public void transferContractToParticipatingConnectors(ContractTemplate template) {
        OrganizationDetails providerDetails = messageQueueService.remoteRequestOrganizationDetails(
                template.getProviderId().replace("Participant:", ""));
        OrganizationDetails consumerDetails = messageQueueService.remoteRequestOrganizationDetails(
                template.getConsumerId().replace("Participant:", ""));

        String providerBaseUrl = providerDetails.getConnectorBaseUrl();
        String consumerBaseUrl = consumerDetails.getConnectorBaseUrl();

        String contractUuid = template.getId().replace("Contract:", "");
        String instanceUuid = contractUuid + "_" + UUID.randomUUID();

        String assetId = instanceUuid + "_Asset";
        String assetName = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
                + "/contract/" + template.getId();
        String assetDescription = "Asset automatically generated from MERLOT to execute contract " + template.getId();
        String policyId = instanceUuid + "_Policy";
        String contractDefinitionId = instanceUuid + "_ContractDefinition";

        String providerControlUrl = providerBaseUrl + ":19192/control/";
        String providerPublicUrl = providerBaseUrl + ":19291/public/";
        String providerManagementUrl = providerBaseUrl + ":19193/management/";
        String providerProtocolUrl = providerBaseUrl + ":19194/protocol";

        String consumerManagementUrl = consumerBaseUrl + ":29193/management/";

        // provider side
        createDataplane(providerControlUrl + "/transfer", providerPublicUrl, providerManagementUrl);
        IdResponse assetIdResponse = createAsset(assetId, assetName, assetDescription, "",
                "", template.getServiceContractProvisioning().getDataAddressName(),
                template.getServiceContractProvisioning().getDataAddressBaseUrl(),
                template.getServiceContractProvisioning().getDataAddressType(), providerManagementUrl);
        IdResponse policyIdResponse = createPolicyUnrestricted(policyId, providerManagementUrl);
        createContractDefinition(contractDefinitionId, policyIdResponse.getId(),
                policyIdResponse.getId(), assetIdResponse.getId(), providerManagementUrl);


        //consumer side
        DcatCatalog catalog = queryCatalog(providerProtocolUrl, consumerManagementUrl);
        IdResponse negotiationResponse = negotiateOffer(catalog.getParticipantId(), consumerDetails.getConnectorId(),
                catalog.getParticipantId(), providerProtocolUrl, catalog.getDataset().get(0).getHasPolicy().get(0).getId(),
                catalog.getDataset().get(0).getAssetId(), catalog.getDataset().get(0).getHasPolicy().get(0), consumerManagementUrl);
        ContractNegotiation negotiation = checkOfferStatus(negotiationResponse.getId(), consumerManagementUrl);
        while (!negotiation.getState().equals("FINALIZED")) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            negotiation = checkOfferStatus(negotiationResponse.getId(), consumerManagementUrl);
        }
        IdResponse transfer = initiateTransfer(catalog.getParticipantId(), providerProtocolUrl, negotiation.getContractAgreementId(),
                catalog.getDataset().get(0).getAssetId(), "http://localhost:4000/api/consumer/store",
                consumerManagementUrl);


        TransferProcess transferProcess = checkTransferStatus(transfer.getId(), consumerManagementUrl);
        while (!transferProcess.getState().equals("COMPLETED")) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            transferProcess = checkTransferStatus(transfer.getId(), consumerManagementUrl);
        }

    }

    public void getAllTransfers(ContractTemplate template, String activeOrgaId) {
        // TODO fetch data from management endpoint by POSTing at /v2/transferprocesses/request
    }

    public void getTransferStatus(ContractTemplate template, String activeOrgaId, String transferId) {
        // TODO fetch data from management endpoint by GETting at /v2/transferprocesses/{id}
    }


}
