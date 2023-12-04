package eu.merloteducation.contractorchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator.ServiceOfferingDetails;
import eu.merloteducation.contractorchestrator.service.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
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
    private ContractStorageService contractStorageService;

    @Mock
    private MessageQueueService messageQueueService;

    @Mock
    private ObjectProvider<EdcClient> edcClientProvider;

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
        ReflectionTestUtils.setField(edcOrchestrationService, "edcClientProvider", edcClientProvider);

        when(edcClientProvider.getObject(any())).thenReturn(new EdcClientFake());

        ObjectMapper mapper = new ObjectMapper();

        validPushContract = new DataDeliveryContractDto();
        validPushContract.setProvisioning(new DataDeliveryContractProvisioningDto());
        validPushContract.setNegotiation(new DataDeliveryContractNegotiationDto());
        validPushContract.setDetails(new DataDeliveryContractDetailsDto());
        validPushContract.getNegotiation().setRuntimeSelection("0 unlimited");
        validPushContract.getNegotiation().setConsumerTncAccepted(true);
        validPushContract.getNegotiation().setConsumerAttachmentsAccepted(true);
        validPushContract.getNegotiation().setProviderTncAccepted(true);
        validPushContract.getNegotiation().setExchangeCountSelection("0");
        validPushContract.getDetails().setId("validPushContract");
        validPushContract.getDetails().setConsumerId("Participant:10");
        validPushContract.getDetails().setProviderId("Participant:20");
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
        validPullContract.getNegotiation().setConsumerTncAccepted(true);
        validPullContract.getNegotiation().setConsumerAttachmentsAccepted(true);
        validPullContract.getNegotiation().setProviderTncAccepted(true);
        validPullContract.getNegotiation().setExchangeCountSelection("0");
        validPullContract.getDetails().setId("validPullContract");
        validPullContract.getDetails().setConsumerId("Participant:10");
        validPullContract.getDetails().setProviderId("Participant:20");
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

        doReturn(validPushContract).when(contractStorageService).getContractDetails(eq(validPushContract.getDetails().getId()), any());
        doReturn(validPullContract).when(contractStorageService).getContractDetails(eq(validPullContract.getDetails().getId()), any());
        doReturn(wrongTypeContract).when(contractStorageService).getContractDetails(eq(wrongTypeContract.getDetails().getId()), any());
        doReturn(wrongStateContract).when(contractStorageService).getContractDetails(eq(wrongStateContract.getDetails().getId()), any());

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
    }

    @Test
    void testInitiateNegotiationValidPushProvider() {
        IdResponse negotiationId = this.edcOrchestrationService.initiateConnectorNegotiation(validPushContract.getDetails().getId(),
                validPushContract.getDetails().getProviderId().replace("Participant:", ""), "authToken");

        assertNotNull(negotiationId);
        assertEquals("edc:IdResponseDto", negotiationId.getType());
        assertEquals(EdcClientFake.FAKE_ID, negotiationId.getId());
    }

    @Test
    void testCheckNegotiationValidPushProvider() {
        ContractNegotiation negotiation = this.edcOrchestrationService.getNegotationStatus("myId", validPushContract.getDetails().getId(),
                validPushContract.getDetails().getProviderId().replace("Participant:", ""), "authToken");

        assertNotNull(negotiation);
        assertEquals("edc:ContractNegotiationDto", negotiation.getType());
        assertEquals(EdcClientFake.FAKE_ID, negotiation.getId());
    }

    @Test
    void testInitiateTransferValidPushProvider() {
        IdResponse transferId = this.edcOrchestrationService.initiateConnectorTransfer("myId", validPushContract.getDetails().getId(),
                validPushContract.getDetails().getProviderId().replace("Participant:", ""), "authToken");

        assertNotNull(transferId);
        assertEquals("edc:IdResponseDto", transferId.getType());
        assertEquals(EdcClientFake.FAKE_ID, transferId.getId());
        assertEquals(EdcClientFake.FAKE_TIMESTAMP, transferId.getCreatedAt());

    }

    @Test
    void testGetTransferStatusValidPushProvider() {
        IonosS3TransferProcess transferProcess = this.edcOrchestrationService.getTransferStatus("myId", validPushContract.getDetails().getId(),
                validPushContract.getDetails().getProviderId().replace("Participant:", ""), "authToken");

        assertNotNull(transferProcess);
        assertEquals(EdcClientFake.FAKE_ID, transferProcess.getId());
        assertEquals("edc:TransferProcessDto", transferProcess.getType());
        assertEquals("edc:DataRequestDto", transferProcess.getDataRequest().getType());
        assertEquals(EdcClientFake.FAKE_ID, transferProcess.getDataRequest().getAssetId());
    }

    @Test
    void testValidPushWrongRole() {

        String consumer = validPushContract.getDetails().getConsumerId().replace("Participant:", "");
        String contractId = validPushContract.getDetails().getId();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.initiateConnectorNegotiation(contractId,
                        consumer, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.getNegotationStatus("myId", contractId,
                        consumer, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.initiateConnectorTransfer("myId", contractId,
                        consumer, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.getTransferStatus("myId", contractId,
                        consumer, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

    }


    @Test
    void testInitiateNegotiationValidPullConsumer() {
        IdResponse negotiationId = this.edcOrchestrationService.initiateConnectorNegotiation(validPullContract.getDetails().getId(),
                validPullContract.getDetails().getConsumerId().replace("Participant:", ""), "authToken");

        assertNotNull(negotiationId);
        assertEquals("edc:IdResponseDto", negotiationId.getType());
        assertEquals(EdcClientFake.FAKE_ID, negotiationId.getId());
        assertEquals(EdcClientFake.FAKE_TIMESTAMP, negotiationId.getCreatedAt());
    }

    @Test
    void testCheckNegotiationValidPullConsumer() {
        ContractNegotiation negotiation = this.edcOrchestrationService.getNegotationStatus("myId", validPullContract.getDetails().getId(),
                validPullContract.getDetails().getConsumerId().replace("Participant:", ""), "authToken");

        assertNotNull(negotiation);

        assertEquals("edc:ContractNegotiationDto", negotiation.getType());
        assertEquals(EdcClientFake.FAKE_ID, negotiation.getId());
    }

    @Test
    void testInitiateTransferValidPullConsumer() {
        IdResponse transferId = this.edcOrchestrationService.initiateConnectorTransfer(EdcClientFake.FAKE_ID, validPullContract.getDetails().getId(),
                validPullContract.getDetails().getConsumerId().replace("Participant:", ""), "authToken");

        assertNotNull(transferId);
        assertNotNull(transferId);
        assertEquals("edc:IdResponseDto", transferId.getType());
        assertEquals(EdcClientFake.FAKE_ID, transferId.getId());
        assertEquals(EdcClientFake.FAKE_TIMESTAMP, transferId.getCreatedAt());
    }

    @Test
    void testGetTransferStatusValidPullConsumer() {
        IonosS3TransferProcess transferProcess = this.edcOrchestrationService.getTransferStatus("myId", validPullContract.getDetails().getId(),
                validPullContract.getDetails().getConsumerId().replace("Participant:", ""), "authToken");

        assertNotNull(transferProcess);
        assertEquals(EdcClientFake.FAKE_ID, transferProcess.getId());
        assertEquals("edc:TransferProcessDto", transferProcess.getType());
        assertEquals("edc:DataRequestDto", transferProcess.getDataRequest().getType());
        assertEquals(EdcClientFake.FAKE_ID, transferProcess.getDataRequest().getAssetId());
    }

    @Test
    void testValidPullWrongRole() {

        String provider = validPullContract.getDetails().getProviderId().replace("Participant:", "");
        String contractId = validPullContract.getDetails().getId();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.initiateConnectorNegotiation(contractId,
                        provider, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.getNegotationStatus("myId", contractId,
                        provider, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.initiateConnectorTransfer("myId", contractId,
                        provider, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.getTransferStatus("myId", contractId,
                        provider, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

    }


    @Test
    void testWrongContractType() {

        String consumer = wrongTypeContract.getDetails().getConsumerId().replace("Participant:", "");
        String contractId = wrongTypeContract.getDetails().getId();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.initiateConnectorNegotiation(contractId,
                        consumer, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.getNegotationStatus("myId", contractId,
                        consumer, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.initiateConnectorTransfer("myId", contractId,
                        consumer, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.getTransferStatus("myId", contractId,
                        consumer, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());

    }

    @Test
    void testWrongContractState() {

        String consumer = wrongStateContract.getDetails().getConsumerId().replace("Participant:", "");
        String contractId = wrongStateContract.getDetails().getId();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.initiateConnectorNegotiation(contractId,
                        consumer, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.getNegotationStatus("myId", contractId,
                        consumer, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.initiateConnectorTransfer("myId", contractId,
                        consumer, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());

        ex = assertThrows(ResponseStatusException.class,
                () -> this.edcOrchestrationService.getTransferStatus("myId", contractId,
                        consumer, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());

    }
}
