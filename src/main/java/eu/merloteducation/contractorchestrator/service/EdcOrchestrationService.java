package eu.merloteducation.contractorchestrator.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.asset.*;
import eu.merloteducation.contractorchestrator.models.edc.contractdefinition.ContractDefinitionCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.dataplane.DataPlaneCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.policy.Policy;
import eu.merloteducation.contractorchestrator.models.edc.policy.PolicyCreateRequest;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private void createAsset(String assetId, String assetName, String assetDescription, String assetVersion, String assetContentType,
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
    }

    private void createPolicyUnrestricted(String policyId, String managementUrl) {
        PolicyCreateRequest policyCreateRequest = new PolicyCreateRequest();
        Policy policy = new Policy();
        policyCreateRequest.setPolicy(policy);
        policyCreateRequest.setId(policyId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PolicyCreateRequest> request = new HttpEntity<>(policyCreateRequest, headers);
        String response =
                restTemplate.exchange(managementUrl + "v2/policydefinitions",
                        HttpMethod.POST, request, String.class).getBody();
        System.out.println(response);
    }

    private void createContractDefinition(String contractDefinitionId, String accessPolicyId, String contractPolicyid,
                                          String managementUrl) { // TODO add asset selector
        ContractDefinitionCreateRequest createRequest = new ContractDefinitionCreateRequest();
        createRequest.setId(contractDefinitionId);
        createRequest.setAccessPolicyId(accessPolicyId);
        createRequest.setContractPolicyId(contractPolicyid);

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            String mappedResult = mapper.writeValueAsString(createRequest);
            System.out.println(mappedResult);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ContractDefinitionCreateRequest> request = new HttpEntity<>(createRequest, headers);
        String response =
                restTemplate.exchange(managementUrl + "v2/contractdefinitions",
                        HttpMethod.POST, request, String.class).getBody();
        System.out.println(response);

    }
    public void transferContractToParticipatingConnectors(ContractTemplate template,
                                                          String providerBaseUrl,
                                                          String consumerBaseUrl) {
        String providerControlUrl = providerBaseUrl + ":19192/control/";
        String providerPublicUrl = providerBaseUrl + ":19291/public/";
        String providerManagementUrl = providerBaseUrl + ":19193/management/";
        String providerProtocolUrl = providerBaseUrl + ":19194/protocol";
        //messageQueueService.remoteRequestOrganizationDetails("10");

        // provider side
        createDataplane(providerControlUrl + "/transfer", providerPublicUrl, providerManagementUrl);
        createAsset("assetId", "My Asset", "Description", "v1.2.3",
                "application/json", "My Asset",
                "https://jsonplaceholder.typicode.com/users", "HttpData",
                providerManagementUrl);
        createPolicyUnrestricted("policy", providerManagementUrl);
        createContractDefinition("contractDefinition", "policy",
                "policy", providerManagementUrl);

        // TODO transfer data to EDC of provider and start negotiation
    }


}
