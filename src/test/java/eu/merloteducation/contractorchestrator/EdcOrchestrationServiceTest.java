/*
 *  Copyright 2024 Dataport. All rights reserved. Developed as part of the MERLOT project.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.merloteducation.contractorchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.contractorchestrator.service.*;
import eu.merloteducation.modelslib.api.contract.datadelivery.DataDeliveryContractDetailsDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.DataDeliveryContractNegotiationDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.DataDeliveryContractProvisioningDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ionoss3extension.IonosS3ConsumerTransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ionoss3extension.IonosS3ProviderTransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.saas.SaasContractDetailsDto;
import eu.merloteducation.modelslib.api.contract.saas.SaasContractDto;
import eu.merloteducation.modelslib.api.organization.IonosS3BucketDto;
import eu.merloteducation.modelslib.api.organization.IonosS3ExtensionConfigDto;
import eu.merloteducation.modelslib.api.organization.OrganizationConnectorTransferDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import eu.merloteducation.modelslib.edc.common.IdResponse;
import eu.merloteducation.modelslib.edc.negotiation.ContractNegotiation;
import eu.merloteducation.modelslib.edc.transfer.IonosS3TransferProcess;
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

import static eu.merloteducation.contractorchestrator.SelfDescriptionDemoData.*;
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

    private String getParticipantId(int num) {
        return "did:web:"+ "test.eu" + "#orga-" + num;
    }

    @BeforeAll
    public void setUp() throws JsonProcessingException {
        ReflectionTestUtils.setField(edcOrchestrationService, "messageQueueService", messageQueueService);
        ReflectionTestUtils.setField(edcOrchestrationService, "contractStorageService", contractStorageService);
        ReflectionTestUtils.setField(edcOrchestrationService, "edcClientProvider", edcClientProvider);

        when(edcClientProvider.getObject(any())).thenReturn(new EdcClientFake());

        ObjectMapper mapper = new ObjectMapper();

        validPushContract = new DataDeliveryContractDto();
        validPushContract.setProvisioning(new DataDeliveryContractProvisioningDto());
        validPushContract.getProvisioning().setProviderTransferProvisioning(new IonosS3ProviderTransferProvisioningDto());
        validPushContract.getProvisioning().setConsumerTransferProvisioning(new IonosS3ConsumerTransferProvisioningDto());
        validPushContract.setNegotiation(new DataDeliveryContractNegotiationDto());
        validPushContract.setDetails(new DataDeliveryContractDetailsDto());
        validPushContract.getNegotiation().setRuntimeSelection("0 unlimited");
        validPushContract.getNegotiation().setConsumerTncAccepted(true);
        validPushContract.getNegotiation().setConsumerAttachmentsAccepted(true);
        validPushContract.getNegotiation().setProviderTncAccepted(true);
        validPushContract.getNegotiation().setExchangeCountSelection("0");
        validPushContract.getDetails().setId("validPushContract");
        validPushContract.getDetails().setConsumerId(getParticipantId(10));
        validPushContract.getDetails().setProviderId(getParticipantId(20));
        validPushContract.getDetails().setState("RELEASED");
        validPushContract.getProvisioning().getProviderTransferProvisioning().setSelectedConnectorId("edc1");
        validPushContract.getProvisioning().getProviderTransferProvisioning().setDataAddressType("IonosS3");
        validPushContract.getProvisioning().getConsumerTransferProvisioning().setDataAddressType("IonosS3");
        ((IonosS3ProviderTransferProvisioningDto) validPushContract.getProvisioning().getProviderTransferProvisioning()).setDataAddressSourceFileName("sourcefile.json");
        ((IonosS3ProviderTransferProvisioningDto) validPushContract.getProvisioning().getProviderTransferProvisioning()).setDataAddressSourceBucketName("sourcebucket");
        validPushContract.getProvisioning().getConsumerTransferProvisioning().setSelectedConnectorId("edc2");
        ((IonosS3ConsumerTransferProvisioningDto) validPushContract.getProvisioning().getConsumerTransferProvisioning()).setDataAddressTargetPath("myTargetPath/");
        ((IonosS3ConsumerTransferProvisioningDto) validPushContract.getProvisioning().getConsumerTransferProvisioning()).setDataAddressTargetBucketName("targetbucket");
        validPushContract.setOffering(new ServiceOfferingDto());
        String pushOfferingId = "urn:uuid:" + UUID.randomUUID();
        validPushContract.getOffering().setSelfDescription(createVpFromCsList(
                List.of(
                        getGxServiceOfferingCs(pushOfferingId, "Some Offering", "did:web:someorga"),
                        getMerlotServiceOfferingCs(pushOfferingId),
                        getMerlotDataDeliveryServiceOfferingCs(pushOfferingId, "Push")
                ),
                "did:web:someorga"
        ));


        validPullContract = new DataDeliveryContractDto();
        validPullContract.setProvisioning(new DataDeliveryContractProvisioningDto());
        validPullContract.getProvisioning().setProviderTransferProvisioning(new IonosS3ProviderTransferProvisioningDto());
        validPullContract.getProvisioning().setConsumerTransferProvisioning(new IonosS3ConsumerTransferProvisioningDto());
        validPullContract.setNegotiation(new DataDeliveryContractNegotiationDto());
        validPullContract.setDetails(new DataDeliveryContractDetailsDto());
        validPullContract.getNegotiation().setRuntimeSelection("0 unlimited");
        validPullContract.getNegotiation().setConsumerTncAccepted(true);
        validPullContract.getNegotiation().setConsumerAttachmentsAccepted(true);
        validPullContract.getNegotiation().setProviderTncAccepted(true);
        validPullContract.getNegotiation().setExchangeCountSelection("0");
        validPullContract.getDetails().setId("validPullContract");
        validPullContract.getDetails().setConsumerId(getParticipantId(10));
        validPullContract.getDetails().setProviderId(getParticipantId(20));
        validPullContract.getDetails().setState("RELEASED");
        validPullContract.getProvisioning().getProviderTransferProvisioning().setSelectedConnectorId("edc1");
        validPullContract.getProvisioning().getProviderTransferProvisioning().setDataAddressType("IonosS3");
        validPullContract.getProvisioning().getConsumerTransferProvisioning().setDataAddressType("IonosS3");
        ((IonosS3ProviderTransferProvisioningDto) validPullContract.getProvisioning().getProviderTransferProvisioning()).setDataAddressSourceFileName("sourcefile.json");
        ((IonosS3ProviderTransferProvisioningDto) validPullContract.getProvisioning().getProviderTransferProvisioning()).setDataAddressSourceBucketName("sourcebucket");
        validPullContract.getProvisioning().getConsumerTransferProvisioning().setSelectedConnectorId("edc2");
        ((IonosS3ConsumerTransferProvisioningDto) validPullContract.getProvisioning().getConsumerTransferProvisioning()).setDataAddressTargetPath("myTargetPath/");
        ((IonosS3ConsumerTransferProvisioningDto) validPullContract.getProvisioning().getConsumerTransferProvisioning()).setDataAddressTargetBucketName("targetbucket");
        validPullContract.setOffering(new ServiceOfferingDto());
        String pullOfferingId = "urn:uuid:" + UUID.randomUUID();
        validPullContract.getOffering().setSelfDescription(createVpFromCsList(
                List.of(
                        getGxServiceOfferingCs(pullOfferingId, "Some Offering", "did:web:someorga"),
                        getMerlotServiceOfferingCs(pullOfferingId),
                        getMerlotDataDeliveryServiceOfferingCs(pullOfferingId, "Pull")
                ),
                "did:web:someorga"
        ));

        wrongTypeContract = new SaasContractDto();
        wrongTypeContract.setDetails(new SaasContractDetailsDto());
        wrongTypeContract.getDetails().setId("wrongTypeContract");
        wrongTypeContract.getDetails().setState("RELEASED");
        wrongTypeContract.getDetails().setConsumerId(getParticipantId(10));
        wrongTypeContract.getDetails().setProviderId(getParticipantId(20));

        wrongStateContract = new DataDeliveryContractDto();
        wrongStateContract.setProvisioning(new DataDeliveryContractProvisioningDto());
        wrongStateContract.getProvisioning().setProviderTransferProvisioning(new IonosS3ProviderTransferProvisioningDto());
        wrongStateContract.getProvisioning().setConsumerTransferProvisioning(new IonosS3ConsumerTransferProvisioningDto());
        wrongStateContract.setNegotiation(new DataDeliveryContractNegotiationDto());
        wrongStateContract.setDetails(new DataDeliveryContractDetailsDto());
        wrongStateContract.getDetails().setId("wrongStateContract");
        wrongStateContract.getDetails().setState("IN_DRAFT");
        wrongStateContract.getDetails().setConsumerId(getParticipantId(10));
        wrongStateContract.getDetails().setProviderId(getParticipantId(20));

        //when(contractStorageService.getContractDetails(eq("Contract:validPush"), any())).thenReturn(validPushContract);

        doReturn(validPushContract).when(contractStorageService).getContractDetails(eq(validPushContract.getDetails().getId()), any());
        doReturn(validPullContract).when(contractStorageService).getContractDetails(eq(validPullContract.getDetails().getId()), any());
        doReturn(wrongTypeContract).when(contractStorageService).getContractDetails(eq(wrongTypeContract.getDetails().getId()), any());
        doReturn(wrongStateContract).when(contractStorageService).getContractDetails(eq(wrongStateContract.getDetails().getId()), any());

        OrganizationConnectorTransferDto edc1 = new OrganizationConnectorTransferDto();
        edc1.setConnectorId("edc1");
        edc1.setConnectorEndpoint("http://example.com");
        edc1.setOrgaId(getParticipantId(20));
        edc1.setConnectorAccessToken("1234");
        List<IonosS3BucketDto> bucketList = List.of(
                new IonosS3BucketDto("sourcebucket", "http://example.com/"),
                new IonosS3BucketDto("targetbucket", "http://example.com/")
        );
        edc1.setIonosS3ExtensionConfig(new IonosS3ExtensionConfigDto(bucketList));

        OrganizationConnectorTransferDto edc2 = new OrganizationConnectorTransferDto();
        edc2.setConnectorId("edc2");
        edc2.setConnectorEndpoint("http://example.com");
        edc2.setOrgaId(getParticipantId(10));
        edc2.setConnectorAccessToken("1234");
        edc2.setIonosS3ExtensionConfig(new IonosS3ExtensionConfigDto(bucketList));

        doReturn(edc1).when(messageQueueService)
                .remoteRequestOrganizationConnectorByConnectorId(getParticipantId(20), "edc1");
        doReturn(edc2).when(messageQueueService)
                .remoteRequestOrganizationConnectorByConnectorId(getParticipantId(10), "edc2");
    }

    @Test
    void testInitiateNegotiationValidPushProvider() {
        IdResponse negotiationId = this.edcOrchestrationService.initiateConnectorNegotiation(validPushContract.getDetails().getId(),
                validPushContract.getDetails().getProviderId(), "authToken");

        assertNotNull(negotiationId);
        assertEquals("edc:IdResponseDto", negotiationId.getType());
        assertEquals(EdcClientFake.FAKE_ID, negotiationId.getId());
    }

    @Test
    void testCheckNegotiationValidPushProvider() {
        ContractNegotiation negotiation = this.edcOrchestrationService.getNegotationStatus("myId", validPushContract.getDetails().getId(),
                validPushContract.getDetails().getProviderId(), "authToken");

        assertNotNull(negotiation);
        assertEquals("edc:ContractNegotiationDto", negotiation.getType());
        assertEquals(EdcClientFake.FAKE_ID, negotiation.getId());
    }

    @Test
    void testInitiateTransferValidPushProvider() {
        IdResponse transferId = this.edcOrchestrationService.initiateConnectorTransfer("myId", validPushContract.getDetails().getId(),
                validPushContract.getDetails().getProviderId(), "authToken");

        assertNotNull(transferId);
        assertEquals("edc:IdResponseDto", transferId.getType());
        assertEquals(EdcClientFake.FAKE_ID, transferId.getId());
        assertEquals(EdcClientFake.FAKE_TIMESTAMP, transferId.getCreatedAt());

    }

    @Test
    void testGetTransferStatusValidPushProvider() {
        IonosS3TransferProcess transferProcess = this.edcOrchestrationService.getTransferStatus("myId", validPushContract.getDetails().getId(),
                validPushContract.getDetails().getProviderId(), "authToken");

        assertNotNull(transferProcess);
        assertEquals(EdcClientFake.FAKE_ID, transferProcess.getId());
        assertEquals("edc:TransferProcessDto", transferProcess.getType());
        assertEquals("edc:DataRequestDto", transferProcess.getDataRequest().getType());
        assertEquals(EdcClientFake.FAKE_ID, transferProcess.getDataRequest().getAssetId());
    }

    @Test
    void testValidPushWrongRole() {

        String consumer = validPushContract.getDetails().getConsumerId();
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
                validPullContract.getDetails().getConsumerId(), "authToken");

        assertNotNull(negotiationId);
        assertEquals("edc:IdResponseDto", negotiationId.getType());
        assertEquals(EdcClientFake.FAKE_ID, negotiationId.getId());
        assertEquals(EdcClientFake.FAKE_TIMESTAMP, negotiationId.getCreatedAt());
    }

    @Test
    void testCheckNegotiationValidPullConsumer() {
        ContractNegotiation negotiation = this.edcOrchestrationService.getNegotationStatus("myId", validPullContract.getDetails().getId(),
                validPullContract.getDetails().getConsumerId(), "authToken");

        assertNotNull(negotiation);

        assertEquals("edc:ContractNegotiationDto", negotiation.getType());
        assertEquals(EdcClientFake.FAKE_ID, negotiation.getId());
    }

    @Test
    void testInitiateTransferValidPullConsumer() {
        IdResponse transferId = this.edcOrchestrationService.initiateConnectorTransfer(EdcClientFake.FAKE_ID, validPullContract.getDetails().getId(),
                validPullContract.getDetails().getConsumerId(), "authToken");

        assertNotNull(transferId);
        assertNotNull(transferId);
        assertEquals("edc:IdResponseDto", transferId.getType());
        assertEquals(EdcClientFake.FAKE_ID, transferId.getId());
        assertEquals(EdcClientFake.FAKE_TIMESTAMP, transferId.getCreatedAt());
    }

    @Test
    void testGetTransferStatusValidPullConsumer() {
        IonosS3TransferProcess transferProcess = this.edcOrchestrationService.getTransferStatus("myId", validPullContract.getDetails().getId(),
                validPullContract.getDetails().getConsumerId(), "authToken");

        assertNotNull(transferProcess);
        assertEquals(EdcClientFake.FAKE_ID, transferProcess.getId());
        assertEquals("edc:TransferProcessDto", transferProcess.getType());
        assertEquals("edc:DataRequestDto", transferProcess.getDataRequest().getType());
        assertEquals(EdcClientFake.FAKE_ID, transferProcess.getDataRequest().getAssetId());
    }

    @Test
    void testValidPullWrongRole() {

        String provider = validPullContract.getDetails().getProviderId();
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

        String consumer = wrongTypeContract.getDetails().getConsumerId();
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

        String consumer = wrongStateContract.getDetails().getConsumerId();
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
