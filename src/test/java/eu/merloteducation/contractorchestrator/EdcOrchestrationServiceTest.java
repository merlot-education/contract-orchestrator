package eu.merloteducation.contractorchestrator;

import eu.merloteducation.contractorchestrator.models.OrganisationConnectorExtension;
import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.DataDeliveryContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.DataDeliveryProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.SaasContractTemplate;
import eu.merloteducation.contractorchestrator.service.ContractStorageService;
import eu.merloteducation.contractorchestrator.service.EdcOrchestrationService;
import eu.merloteducation.contractorchestrator.service.MessageQueueService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EdcOrchestrationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ContractStorageService contractStorageService;

    @Mock
    private MessageQueueService messageQueueService;

    @Autowired
    private EdcOrchestrationService edcOrchestrationService;

    private DataDeliveryContractTemplate validPushContract;
    private DataDeliveryContractTemplate validPullContract;
    private SaasContractTemplate wrongTypeContract;
    private DataDeliveryContractTemplate wrongStateContract;

    @BeforeAll
    public void setUp() {
        ReflectionTestUtils.setField(edcOrchestrationService, "messageQueueService", messageQueueService);
        ReflectionTestUtils.setField(edcOrchestrationService, "contractStorageService", contractStorageService);
        ReflectionTestUtils.setField(edcOrchestrationService, "restTemplate", restTemplate);

        validPushContract = new DataDeliveryContractTemplate();
        validPushContract.setRuntimeSelection("unlimited");
        validPushContract.setConsumerMerlotTncAccepted(true);
        validPushContract.setConsumerOfferingTncAccepted(true);
        validPushContract.setConsumerProviderTncAccepted(true);
        validPushContract.setProviderMerlotTncAccepted(true);
        validPushContract.setExchangeCountSelection("unlimited");
        validPushContract.setConsumerId("Participant:10");
        validPushContract.setProviderId("Participant:20");
        validPushContract.setConsumerSignature("1234");
        validPushContract.setConsumerSignerUserId("1234");
        validPushContract.setProviderSignature("2345");
        validPushContract.setProviderSignerUserId("2345");
        validPushContract.setDataTransferType("Push");
        DataDeliveryProvisioning provisioning = new DataDeliveryProvisioning();
        provisioning.setSelectedProviderConnectorId("edc1");
        provisioning.setDataAddressType("IonosS3");
        provisioning.setDataAddressSourceFileName("sourcefile.json");
        provisioning.setDataAddressSourceBucketName("sourcebucket");
        provisioning.setSelectedConsumerConnectorId("edc2");
        provisioning.setDataAddressTargetFileName("targetfile.json");
        provisioning.setDataAddressTargetBucketName("targetbucket");
        validPushContract.setServiceContractProvisioning(provisioning);

        validPushContract.transitionState(ContractState.SIGNED_CONSUMER);
        validPushContract.transitionState(ContractState.RELEASED);

        validPullContract = new DataDeliveryContractTemplate(validPushContract, true);
        validPullContract.setConsumerSignature("1234");
        validPullContract.setConsumerSignerUserId("1234");
        validPullContract.setProviderSignature("2345");
        validPullContract.setProviderSignerUserId("2345");
        validPullContract.setDataTransferType("Pull");
        validPullContract.transitionState(ContractState.SIGNED_CONSUMER);
        validPullContract.transitionState(ContractState.RELEASED);

        wrongTypeContract = new SaasContractTemplate();
        wrongTypeContract.setRuntimeSelection("unlimited");
        wrongTypeContract.setConsumerMerlotTncAccepted(true);
        wrongTypeContract.setConsumerOfferingTncAccepted(true);
        wrongTypeContract.setConsumerProviderTncAccepted(true);
        wrongTypeContract.setProviderMerlotTncAccepted(true);
        wrongTypeContract.setUserCountSelection("unlimited");
        wrongTypeContract.setConsumerId("Participant:10");
        wrongTypeContract.setProviderId("Participant:20");
        wrongTypeContract.setConsumerSignature("1234");
        wrongTypeContract.setConsumerSignerUserId("1234");
        wrongTypeContract.setProviderSignature("2345");
        wrongTypeContract.setProviderSignerUserId("2345");

        wrongTypeContract.transitionState(ContractState.SIGNED_CONSUMER);
        wrongTypeContract.transitionState(ContractState.RELEASED);

        wrongStateContract = new DataDeliveryContractTemplate(validPushContract, true);

        //when(contractStorageService.getContractDetails(eq("Contract:validPush"), any())).thenReturn(validPushContract);

        doReturn(validPushContract).when(contractStorageService).getContractDetails(eq(validPushContract.getId()), any());
        doReturn(validPullContract).when(contractStorageService).getContractDetails(eq(validPullContract.getId()), any());
        doReturn(wrongTypeContract).when(contractStorageService).getContractDetails(eq(wrongTypeContract.getId()), any());
        doReturn(wrongStateContract).when(contractStorageService).getContractDetails(eq(wrongStateContract.getId()), any());

        OrganisationConnectorExtension edc1 = new OrganisationConnectorExtension();
        edc1.setId("1234");
        edc1.setConnectorId("edc1");
        edc1.setConnectorEndpoint("http://example.com");
        edc1.setOrgaId("Participant:20");
        edc1.setConnectorAccessToken("1234");
        List<String> bucketList = new ArrayList<>();
        bucketList.add("sourcebucket");
        bucketList.add("targetbucket");
        edc1.setBucketNames(bucketList);

        OrganisationConnectorExtension edc2 = new OrganisationConnectorExtension();
        edc1.setId("1234");
        edc1.setConnectorId("edc2");
        edc1.setConnectorEndpoint("http://example.com");
        edc1.setOrgaId("Participant:10");
        edc1.setConnectorAccessToken("1234");
        edc1.setBucketNames(bucketList);

        doReturn(edc1).when(messageQueueService)
                .remoteRequestOrganizationConnectorByConnectorId("20", "edc1");
        doReturn(edc2).when(messageQueueService)
                .remoteRequestOrganizationConnectorByConnectorId("10", "edc2");

        ResponseEntity idResponse = new ResponseEntity<>("""
                {"@context": {"dcat": "https://www.w3.org/ns/dcat/",
                                                             "dct": "https://purl.org/dc/terms/",
                                                             "dspace": "https://w3id.org/dspace/v0.8/",
                                                             "edc": "https://w3id.org/edc/v0.0.1/ns/",
                                                             "odrl": "http://www.w3.org/ns/odrl/2/"},
                                                "@id": "myId",
                                                "@type": "edc:IdResponseDto",
                                                "edc:createdAt": 1689756292703}
                """, HttpStatus.OK);

        doReturn(idResponse).when(restTemplate).exchange(contains("/v2/assets"), eq(HttpMethod.POST), any(), eq(String.class));
        doReturn(idResponse).when(restTemplate).exchange(contains("/v2/policydefinitions"), eq(HttpMethod.POST), any(), eq(String.class));
        doReturn(idResponse).when(restTemplate).exchange(contains("/v2/contractdefinitions"), eq(HttpMethod.POST), any(), eq(String.class));

        doReturn(new ResponseEntity<>("""
                {"@context": {"dcat": "https://www.w3.org/ns/dcat/",
                                                             "dct": "https://purl.org/dc/terms/",
                                                             "dspace": "https://w3id.org/dspace/v0.8/",
                                                             "edc": "https://w3id.org/edc/v0.0.1/ns/",
                                                             "odrl": "http://www.w3.org/ns/odrl/2/"},
                                                "@id": "5a4b9e7c-1ea1-48e1-b136-4bc5074d24fc",
                                                "@type": "dcat:Catalog",
                                                "dcat:dataset": {"@id": "0b54d439-c302-4bdf-9b12-304f618df995",
                                                                 "@type": "dcat:Dataset",
                                                                 "dcat:distribution": [],
                                                                 "edc:contenttype": "application/json",
                                                                 "edc:description": "Description",
                                                                 "edc:id": "myId",
                                                                 "edc:name": "My Asset",
                                                                 "edc:version": "v1.2.3",
                                                                 "odrl:hasPolicy": {"@id": "myId:myId:87ad21db-db75-4c1d-8492-066f8232c097",
                                                                                    "@type": "odrl:Set",
                                                                                    "odrl:obligation": [],
                                                                                    "odrl:permission": {"odrl:action": {"odrl:type": "USE"},
                                                                                                        "odrl:target": "myId"},
                                                                                    "odrl:prohibition": [],
                                                                                    "odrl:target": "myId"}},
                                                "dcat:service": {"@id": "c144c176-6e90-4f20-9a60-45685301cbe8",
                                                                 "@type": "dcat:DataService",
                                                                 "dct:endpointUrl": "http://localhost:8282/protocol",
                                                                 "dct:terms": "connector"},
                                                "edc:participantId": "provider"}
                """, HttpStatus.OK)).when(restTemplate).exchange(contains("/v2/catalog/request"), eq(HttpMethod.POST), any(), eq(String.class));

        doReturn(idResponse).when(restTemplate).exchange(endsWith("/v2/contractnegotiations"), eq(HttpMethod.POST), any(), eq(String.class));

        doReturn(new ResponseEntity<>("""
                {"@context": {"dcat": "https://www.w3.org/ns/dcat/",
                                                                 "dct": "https://purl.org/dc/terms/",
                                                                 "dspace": "https://w3id.org/dspace/v0.8/",
                                                                 "edc": "https://w3id.org/edc/v0.0.1/ns/",
                                                                 "odrl": "http://www.w3.org/ns/odrl/2/"},
                                                    "@id": "623d4060-302b-45a0-89d0-65bc69650822",
                                                    "@type": "edc:ContractNegotiationDto",
                                                    "edc:callbackAddresses": [],
                                                    "edc:contractAgreementId": "myId:myId:6cada15f-0acc-4784-bc51-b1b0aba504e8",
                                                    "edc:counterPartyAddress": "http://localhost:8282/protocol",
                                                    "edc:protocol": "dataspace-protocol-http",
                                                    "edc:state": "FINALIZED",
                                                    "edc:type": "CONSUMER"}
                """, HttpStatus.OK)).when(restTemplate).exchange(contains("/v2/contractnegotiations/"), eq(HttpMethod.GET), any(), eq(String.class));

        doReturn(idResponse).when(restTemplate).exchange(endsWith("/v2/transferprocesses"), eq(HttpMethod.POST), any(), eq(String.class));

        doReturn(new ResponseEntity<>("""
                {"@context": {"dcat": "https://www.w3.org/ns/dcat/",
                                                             "dct": "https://purl.org/dc/terms/",
                                                             "dspace": "https://w3id.org/dspace/v0.8/",
                                                             "edc": "https://w3id.org/edc/v0.0.1/ns/",
                                                             "odrl": "http://www.w3.org/ns/odrl/2/"},
                                                "@id": "9bb91649-7d09-490a-ac3f-33b60a5de02e",
                                                "@type": "edc:TransferProcessDto",
                                                "edc:callbackAddresses": [],
                                                "edc:dataDestination": {"edc:blobName": "device1-data.csv",
                                                                        "edc:bucketName": "merlotedcconsumer",
                                                                        "edc:container": "company2",
                                                                        "edc:keyName": "device1-data.csv",
                                                                        "edc:name": "device1-data.csv",
                                                                        "edc:storage": "s3-eu-central-1.ionoscloud.com",
                                                                        "edc:type": "IonosS3"},
                                                "edc:dataRequest": {"@id": "9bb91649-7d09-490a-ac3f-33b60a5de02e",
                                                                    "@type": "edc:DataRequestDto",
                                                                    "edc:assetId": "myId",
                                                                    "edc:connectorId": "provider",
                                                                    "edc:contractId": "myId:myId:6cada15f-0acc-4784-bc51-b1b0aba504e8"},
                                                "edc:state": "COMPLETED",
                                                "edc:stateTimestamp": 1689756313117,
                                                "edc:type": "CONSUMER"}
                """, HttpStatus.OK)).when(restTemplate).exchange(contains("/v2/transferprocesses/"), eq(HttpMethod.GET), any(), eq(String.class));
    }

    @Test
    void testInitiateNegotiationValidPushProvider() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(validPushContract.getProviderId().replace("Participant:", ""));
        this.edcOrchestrationService.initiateConnectorNegotiation(validPushContract.getId(),
                validPushContract.getProviderId().replace("Participant:", ""), representedOrgaIds);
    }
}
