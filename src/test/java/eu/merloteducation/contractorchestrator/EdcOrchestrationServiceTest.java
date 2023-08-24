package eu.merloteducation.contractorchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.contractorchestrator.models.dto.datadelivery.DataDeliveryContractDetailsDto;
import eu.merloteducation.contractorchestrator.models.dto.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.contractorchestrator.models.dto.datadelivery.DataDeliveryContractNegotiationDto;
import eu.merloteducation.contractorchestrator.models.dto.datadelivery.DataDeliveryContractProvisioningDto;
import eu.merloteducation.contractorchestrator.models.dto.saas.SaasContractDetailsDto;
import eu.merloteducation.contractorchestrator.models.dto.saas.SaasContractDto;
import eu.merloteducation.contractorchestrator.models.organisationsorchestrator.OrganisationConnectorExtension;
import eu.merloteducation.contractorchestrator.models.edc.common.IdResponse;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractNegotiation;
import eu.merloteducation.contractorchestrator.models.edc.transfer.IonosS3TransferProcess;
import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.DataDeliveryContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.DataDeliveryProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.SaasContractTemplate;
import eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator.ServiceOfferingDetails;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EdcOrchestrationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ContractStorageService contractStorageService;

    @Mock
    private MessageQueueService messageQueueService;

    @Autowired
    private EdcOrchestrationService edcOrchestrationService;

    private DataDeliveryContractDto validPushContract;
    private DataDeliveryContractDto validPullContract;
    private SaasContractDto wrongTypeContract;
    private DataDeliveryContractDto wrongStateContract;

    @BeforeAll
    public void setUp() throws JsonProcessingException {
        ReflectionTestUtils.setField(edcOrchestrationService, "messageQueueService", messageQueueService);
        ReflectionTestUtils.setField(edcOrchestrationService, "contractStorageService", contractStorageService);
        ReflectionTestUtils.setField(edcOrchestrationService, "restTemplate", restTemplate);

        ObjectMapper mapper = new ObjectMapper();

        validPushContract = new DataDeliveryContractDto();
        validPushContract.setProvisioning(new DataDeliveryContractProvisioningDto());
        validPushContract.setNegotiation(new DataDeliveryContractNegotiationDto());
        validPushContract.setDetails(new DataDeliveryContractDetailsDto());
        validPushContract.getNegotiation().setRuntimeSelection("0 unlimited");
        validPushContract.getNegotiation().setConsumerMerlotTncAccepted(true);
        validPushContract.getNegotiation().setConsumerOfferingTncAccepted(true);
        validPushContract.getNegotiation().setConsumerProviderTncAccepted(true);
        validPushContract.getNegotiation().setProviderMerlotTncAccepted(true);
        validPushContract.getNegotiation().setExchangeCountSelection("0");
        validPushContract.getDetails().setId("validPushContract");
        validPushContract.getDetails().setConsumerId("Participant:10");
        validPushContract.getDetails().setProviderId("Participant:20");
        validPushContract.getDetails().setConsumerSignature("1234");
        validPushContract.getDetails().setConsumerSignerUser("1234");
        validPushContract.getDetails().setProviderSignature("2345");
        validPushContract.getDetails().setProviderSignerUser("2345");
        validPushContract.getDetails().setState("RELEASED");
        validPushContract.getProvisioning().setSelectedProviderConnectorId("edc1");
        validPushContract.getProvisioning().setDataAddressType("IonosS3");
        validPushContract.getProvisioning().setDataAddressSourceFileName("sourcefile.json");
        validPushContract.getProvisioning().setDataAddressSourceBucketName("sourcebucket");
        validPushContract.getProvisioning().setSelectedConsumerConnectorId("edc2");
        validPushContract.getProvisioning().setDataAddressTargetFileName("targetfile.json");
        validPushContract.getProvisioning().setDataAddressTargetBucketName("targetbucket");
        validPushContract.setOffering(new ServiceOfferingDetails());
        validPushContract.getOffering().setSelfDescription(mapper.readTree("""
                {
                    "verifiableCredential": {
                        "credentialSubject": {
                            "merlot:dataTransferType": {
                                    "@type": "xsd:string",
                                    "@value": "Push"
                            }
                        }
                    }
                }
                """));


        validPullContract = new DataDeliveryContractDto();
        validPullContract.setProvisioning(new DataDeliveryContractProvisioningDto());
        validPullContract.setNegotiation(new DataDeliveryContractNegotiationDto());
        validPullContract.setDetails(new DataDeliveryContractDetailsDto());
        validPullContract.getNegotiation().setRuntimeSelection("0 unlimited");
        validPullContract.getNegotiation().setConsumerMerlotTncAccepted(true);
        validPullContract.getNegotiation().setConsumerOfferingTncAccepted(true);
        validPullContract.getNegotiation().setConsumerProviderTncAccepted(true);
        validPullContract.getNegotiation().setProviderMerlotTncAccepted(true);
        validPullContract.getNegotiation().setExchangeCountSelection("0");
        validPullContract.getDetails().setId("validPullContract");
        validPullContract.getDetails().setConsumerId("Participant:10");
        validPullContract.getDetails().setProviderId("Participant:20");
        validPullContract.getDetails().setConsumerSignature("1234");
        validPullContract.getDetails().setConsumerSignerUser("1234");
        validPullContract.getDetails().setProviderSignature("2345");
        validPullContract.getDetails().setProviderSignerUser("2345");
        validPullContract.getDetails().setState("RELEASED");
        validPullContract.getProvisioning().setSelectedProviderConnectorId("edc1");
        validPullContract.getProvisioning().setDataAddressType("IonosS3");
        validPullContract.getProvisioning().setDataAddressSourceFileName("sourcefile.json");
        validPullContract.getProvisioning().setDataAddressSourceBucketName("sourcebucket");
        validPullContract.getProvisioning().setSelectedConsumerConnectorId("edc2");
        validPullContract.getProvisioning().setDataAddressTargetFileName("targetfile.json");
        validPullContract.getProvisioning().setDataAddressTargetBucketName("targetbucket");
        validPullContract.setOffering(new ServiceOfferingDetails());
        validPullContract.getOffering().setSelfDescription(mapper.readTree("""
                {
                    "verifiableCredential": {
                        "credentialSubject": {
                            "merlot:dataTransferType": {
                                    "@type": "xsd:string",
                                    "@value": "Pull"
                            }
                        }
                    }
                }
                """));

        wrongTypeContract = new SaasContractDto();
        wrongTypeContract.setDetails(new SaasContractDetailsDto());
        wrongTypeContract.getDetails().setId("wrongTypeContract");
        wrongTypeContract.getDetails().setState("RELEASED");
        wrongTypeContract.getDetails().setConsumerId("Participant:10");
        wrongTypeContract.getDetails().setProviderId("Participant:20");

        wrongStateContract = new DataDeliveryContractDto();
        wrongStateContract.setProvisioning(new DataDeliveryContractProvisioningDto());
        wrongStateContract.setNegotiation(new DataDeliveryContractNegotiationDto());
        wrongStateContract.setDetails(new DataDeliveryContractDetailsDto());
        wrongStateContract.getDetails().setId("wrongStateContract");
        wrongStateContract.getDetails().setState("IN_DRAFT");
        wrongStateContract.getDetails().setConsumerId("Participant:10");
        wrongStateContract.getDetails().setProviderId("Participant:20");

        //when(contractStorageService.getContractDetails(eq("Contract:validPush"), any())).thenReturn(validPushContract);

        doReturn(validPushContract).when(contractStorageService).getContractDetails(eq(validPushContract.getDetails().getId()), any(), any());
        doReturn(validPullContract).when(contractStorageService).getContractDetails(eq(validPullContract.getDetails().getId()), any(), any());
        doReturn(wrongTypeContract).when(contractStorageService).getContractDetails(eq(wrongTypeContract.getDetails().getId()), any(), any());
        doReturn(wrongStateContract).when(contractStorageService).getContractDetails(eq(wrongStateContract.getDetails().getId()), any(), any());

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
                                                                                    "odrl:permission": [],
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
        representedOrgaIds.add(validPushContract.getDetails().getProviderId().replace("Participant:", ""));
        IdResponse negotiationId = this.edcOrchestrationService.initiateConnectorNegotiation(validPushContract.getDetails().getId(),
                validPushContract.getDetails().getProviderId().replace("Participant:", ""), representedOrgaIds, "authToken");

        assertNotNull(negotiationId);
        assertEquals("edc:IdResponseDto", negotiationId.getType());
        assertEquals("myId", negotiationId.getId());
        assertEquals(1689756292703L, negotiationId.getCreatedAt());

    }

    @Test
    void testCheckNegotiationValidPushProvider() {

        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(validPushContract.getDetails().getProviderId().replace("Participant:", ""));
        ContractNegotiation negotiation = this.edcOrchestrationService.getNegotationStatus("myId", validPushContract.getDetails().getId(),
                validPushContract.getDetails().getProviderId().replace("Participant:", ""), representedOrgaIds, "authToken");

        assertNotNull(negotiation);
        assertEquals("edc:ContractNegotiationDto", negotiation.getType());
        assertEquals("623d4060-302b-45a0-89d0-65bc69650822", negotiation.getId());
        assertEquals("myId:myId:6cada15f-0acc-4784-bc51-b1b0aba504e8", negotiation.getContractAgreementId());
        assertTrue(negotiation.getCallbackAddresses().isEmpty());
        assertEquals("http://localhost:8282/protocol", negotiation.getCounterPartyAddress());
        assertEquals("dataspace-protocol-http", negotiation.getProtocol());
        assertEquals("FINALIZED", negotiation.getState());
        assertEquals("CONSUMER", negotiation.getEdcType());
    }

    @Test
    void testInitiateTransferValidPushProvider() {

        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(validPushContract.getDetails().getProviderId().replace("Participant:", ""));
        IdResponse transferId = this.edcOrchestrationService.initiateConnectorTransfer("myId", validPushContract.getDetails().getId(),
                validPushContract.getDetails().getProviderId().replace("Participant:", ""), representedOrgaIds, "authToken");

        assertNotNull(transferId);
        assertEquals("edc:IdResponseDto", transferId.getType());
        assertEquals("myId", transferId.getId());
        assertEquals(1689756292703L, transferId.getCreatedAt());

    }

    @Test
    void testGetTransferStatusValidPushProvider() {

        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(validPushContract.getDetails().getProviderId().replace("Participant:", ""));
        IonosS3TransferProcess transferProcess = this.edcOrchestrationService.getTransferStatus("myId", validPushContract.getDetails().getId(),
                validPushContract.getDetails().getProviderId().replace("Participant:", ""), representedOrgaIds, "authToken");

        assertNotNull(transferProcess);
        assertEquals("9bb91649-7d09-490a-ac3f-33b60a5de02e", transferProcess.getId());
        assertEquals("edc:TransferProcessDto", transferProcess.getType());
        assertTrue(transferProcess.getCallbackAddresses().isEmpty());
        assertEquals("device1-data.csv", transferProcess.getDataDestination().getBlobName());
        assertEquals("merlotedcconsumer", transferProcess.getDataDestination().getBucketName());
        assertEquals("company2", transferProcess.getDataDestination().getContainer());
        assertEquals("device1-data.csv", transferProcess.getDataDestination().getKeyName());
        assertEquals("device1-data.csv", transferProcess.getDataDestination().getName());
        assertEquals("s3-eu-central-1.ionoscloud.com", transferProcess.getDataDestination().getStorage());
        assertEquals("IonosS3", transferProcess.getDataDestination().getDataType());
        assertEquals("9bb91649-7d09-490a-ac3f-33b60a5de02e", transferProcess.getDataRequest().getId());
        assertEquals("edc:DataRequestDto", transferProcess.getDataRequest().getType());
        assertEquals("myId", transferProcess.getDataRequest().getAssetId());
        assertEquals("provider", transferProcess.getDataRequest().getConnectorId());
        assertEquals("myId:myId:6cada15f-0acc-4784-bc51-b1b0aba504e8", transferProcess.getDataRequest().getContractId());
        assertEquals("COMPLETED", transferProcess.getState());
        assertEquals("1689756313117", transferProcess.getStateTimestamp());
        assertEquals("CONSUMER", transferProcess.getEdcType());
    }

    @Test
    void testValidPushWrongRole() {

        String consumer = validPushContract.getDetails().getConsumerId().replace("Participant:", "");
        String contractId = validPushContract.getDetails().getId();

        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(consumer);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.initiateConnectorNegotiation(contractId,
                        consumer, representedOrgaIds, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.getNegotationStatus("myId", contractId,
                        consumer, representedOrgaIds, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.initiateConnectorTransfer("myId", contractId,
                        consumer, representedOrgaIds, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.getTransferStatus("myId", contractId,
                        consumer, representedOrgaIds, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

    }


    @Test
    void testInitiateNegotiationValidPullConsumer() {

        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(validPullContract.getDetails().getConsumerId().replace("Participant:", ""));
        IdResponse negotiationId = this.edcOrchestrationService.initiateConnectorNegotiation(validPullContract.getDetails().getId(),
                validPullContract.getDetails().getConsumerId().replace("Participant:", ""), representedOrgaIds, "authToken");

        assertNotNull(negotiationId);
        assertEquals("edc:IdResponseDto", negotiationId.getType());
        assertEquals("myId", negotiationId.getId());
        assertEquals(1689756292703L, negotiationId.getCreatedAt());
    }

    @Test
    void testCheckNegotiationValidPullConsumer() {

        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(validPullContract.getDetails().getConsumerId().replace("Participant:", ""));
        ContractNegotiation negotiation = this.edcOrchestrationService.getNegotationStatus("myId", validPullContract.getDetails().getId(),
                validPullContract.getDetails().getConsumerId().replace("Participant:", ""), representedOrgaIds, "authToken");

        assertNotNull(negotiation);

        assertEquals("edc:ContractNegotiationDto", negotiation.getType());
        assertEquals("623d4060-302b-45a0-89d0-65bc69650822", negotiation.getId());
        assertEquals("myId:myId:6cada15f-0acc-4784-bc51-b1b0aba504e8", negotiation.getContractAgreementId());
        assertTrue(negotiation.getCallbackAddresses().isEmpty());
        assertEquals("http://localhost:8282/protocol", negotiation.getCounterPartyAddress());
        assertEquals("dataspace-protocol-http", negotiation.getProtocol());
        assertEquals("FINALIZED", negotiation.getState());
        assertEquals("CONSUMER", negotiation.getEdcType());
    }

    @Test
    void testInitiateTransferValidPullConsumer() {

        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(validPullContract.getDetails().getConsumerId().replace("Participant:", ""));
        IdResponse transferId = this.edcOrchestrationService.initiateConnectorTransfer("myId", validPullContract.getDetails().getId(),
                validPullContract.getDetails().getConsumerId().replace("Participant:", ""), representedOrgaIds, "authToken");

        assertNotNull(transferId);
        assertNotNull(transferId);
        assertEquals("edc:IdResponseDto", transferId.getType());
        assertEquals("myId", transferId.getId());
        assertEquals(1689756292703L, transferId.getCreatedAt());
    }

    @Test
    void testGetTransferStatusValidPullConsumer() {

        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(validPullContract.getDetails().getConsumerId().replace("Participant:", ""));
        IonosS3TransferProcess transferProcess = this.edcOrchestrationService.getTransferStatus("myId", validPullContract.getDetails().getId(),
                validPullContract.getDetails().getConsumerId().replace("Participant:", ""), representedOrgaIds, "authToken");

        assertNotNull(transferProcess);
        assertEquals("9bb91649-7d09-490a-ac3f-33b60a5de02e", transferProcess.getId());
        assertEquals("edc:TransferProcessDto", transferProcess.getType());
        assertTrue(transferProcess.getCallbackAddresses().isEmpty());
        assertEquals("device1-data.csv", transferProcess.getDataDestination().getBlobName());
        assertEquals("merlotedcconsumer", transferProcess.getDataDestination().getBucketName());
        assertEquals("company2", transferProcess.getDataDestination().getContainer());
        assertEquals("device1-data.csv", transferProcess.getDataDestination().getKeyName());
        assertEquals("device1-data.csv", transferProcess.getDataDestination().getName());
        assertEquals("s3-eu-central-1.ionoscloud.com", transferProcess.getDataDestination().getStorage());
        assertEquals("IonosS3", transferProcess.getDataDestination().getDataType());
        assertEquals("9bb91649-7d09-490a-ac3f-33b60a5de02e", transferProcess.getDataRequest().getId());
        assertEquals("edc:DataRequestDto", transferProcess.getDataRequest().getType());
        assertEquals("myId", transferProcess.getDataRequest().getAssetId());
        assertEquals("provider", transferProcess.getDataRequest().getConnectorId());
        assertEquals("myId:myId:6cada15f-0acc-4784-bc51-b1b0aba504e8", transferProcess.getDataRequest().getContractId());
        assertEquals("COMPLETED", transferProcess.getState());
        assertEquals("1689756313117", transferProcess.getStateTimestamp());
        assertEquals("CONSUMER", transferProcess.getEdcType());
    }

    @Test
    void testValidPullWrongRole() {

        String provider = validPullContract.getDetails().getProviderId().replace("Participant:", "");
        String contractId = validPullContract.getDetails().getId();

        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(provider);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.initiateConnectorNegotiation(contractId,
                        provider, representedOrgaIds, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.getNegotationStatus("myId", contractId,
                        provider, representedOrgaIds, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.initiateConnectorTransfer("myId", contractId,
                        provider, representedOrgaIds, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.getTransferStatus("myId", contractId,
                        provider, representedOrgaIds, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

    }


    @Test
    void testWrongContractType() {

        String consumer = wrongTypeContract.getDetails().getConsumerId().replace("Participant:", "");
        String contractId = wrongTypeContract.getDetails().getId();

        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(consumer);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.initiateConnectorNegotiation(contractId,
                        consumer, representedOrgaIds, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.getNegotationStatus("myId", contractId,
                        consumer, representedOrgaIds, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.initiateConnectorTransfer("myId", contractId,
                        consumer, representedOrgaIds, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.getTransferStatus("myId", contractId,
                        consumer, representedOrgaIds, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());

    }

    @Test
    void testWrongContractState() {

        String consumer = wrongStateContract.getDetails().getConsumerId().replace("Participant:", "");
        String contractId = wrongStateContract.getDetails().getId();

        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(consumer);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.initiateConnectorNegotiation(contractId,
                        consumer, representedOrgaIds, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.getNegotationStatus("myId", contractId,
                        consumer, representedOrgaIds, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.initiateConnectorTransfer("myId", contractId,
                        consumer, representedOrgaIds, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.getTransferStatus("myId", contractId,
                        consumer, representedOrgaIds, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());

    }
}
