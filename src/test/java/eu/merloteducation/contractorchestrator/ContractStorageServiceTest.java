/*
 *  Copyright 2023-2024 Dataport AöR
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

import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.contractorchestrator.models.entities.*;
import eu.merloteducation.contractorchestrator.models.entities.cooperation.CooperationContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.DataDeliveryContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.DataDeliveryProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension.IonosS3ConsumerTransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension.IonosS3ProviderTransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.saas.SaasContractTemplate;
import eu.merloteducation.contractorchestrator.models.mappers.ContractDtoToPdfMapper;
import eu.merloteducation.contractorchestrator.models.mappers.ContractFromDtoMapper;
import eu.merloteducation.contractorchestrator.models.mappers.ContractToDtoMapper;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import eu.merloteducation.contractorchestrator.service.*;
import eu.merloteducation.gxfscataloglibrary.models.credentials.CastableCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiableCredential;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiablePresentation;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.PojoCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxDataAccountExport;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxSOTermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxVcard;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.NodeKindIRITypeId;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalRegistrationNumberCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.serviceofferings.GxServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.AllowedUserCount;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.DataExchangeCount;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.OfferingRuntime;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.ParticipantTermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotCoopContractServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotDataDeliveryServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotSaasServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotServiceOfferingCredentialSubject;
import eu.merloteducation.modelslib.api.contract.ContractBasicDto;
import eu.merloteducation.modelslib.api.contract.ContractCreateRequest;
import eu.merloteducation.modelslib.api.contract.ContractDto;
import eu.merloteducation.modelslib.api.contract.cooperation.CooperationContractDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ionoss3extension.IonosS3ConsumerTransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ionoss3extension.IonosS3ProviderTransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.saas.SaasContractDetailsDto;
import eu.merloteducation.modelslib.api.contract.saas.SaasContractDto;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.serviceoffering.OfferingMetaDto;
import eu.merloteducation.modelslib.api.serviceoffering.ProviderDetailsDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import eu.merloteducation.s3library.service.StorageClient;
import eu.merloteducation.s3library.service.StorageClientException;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.apache.commons.text.StringSubstitutor;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.*;

import static eu.merloteducation.contractorchestrator.SelfDescriptionDemoData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class ContractStorageServiceTest {

    @Autowired
    private ContractToDtoMapper contractToDtoMapper;

    @Autowired
    private ContractFromDtoMapper contractFromDtoMapper;

    @Autowired
    private ContractDtoToPdfMapper contractDtoToPdfMapper;

    @Autowired
    private EntityManager entityManager;

    @Mock
    private ServiceOfferingOrchestratorClient serviceOfferingOrchestratorClient;

    @Mock
    private OrganizationOrchestratorClient organizationOrchestratorClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private StorageClient storageClient;

    @Mock
    private PdfServiceClient pdfServiceClient;

    @Autowired
    private ContractTemplateRepository contractTemplateRepository;

    @Mock
    private MessageQueueService messageQueueService;

    private ContractStorageService contractStorageService;

    private PageRequest defaultPageRequest;

    private SaasContractTemplate saasContract;
    private DataDeliveryContractTemplate dataDeliveryContract;
    private CooperationContractTemplate coopContract;

    @Autowired
    private PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;
    private final String merlotDomain = "test.eu";

    private ServiceOfferingDto createServiceOfferingOrchestratorResponse(String id, String hash, String name, String providedBy,
                                                             String offeringType) throws JsonProcessingException {
        ServiceOfferingDto dto = new ServiceOfferingDto();

        OfferingMetaDto metaDto = new OfferingMetaDto();
        metaDto.setState("RELEASED");
        metaDto.setHash(hash);
        metaDto.setCreationDate("2023-08-21T15:32:19.100661+02:00");
        metaDto.setModifiedDate("2023-08-21T13:32:19.487564Z");

        ProviderDetailsDto providerDto = new ProviderDetailsDto();
        providerDto.setProviderId(providedBy);
        providerDto.setProviderLegalName("MyProvider");

        GxServiceOfferingCredentialSubject gxCs = getGxServiceOfferingCs(id, name, providedBy);
        MerlotServiceOfferingCredentialSubject merlotCs = getMerlotServiceOfferingCs(id);
        PojoCredentialSubject merlotSpecificCs = switch (offeringType) {
            case MerlotSaasServiceOfferingCredentialSubject.TYPE -> getMerlotSaasServiceOfferingCs(id);
            case MerlotDataDeliveryServiceOfferingCredentialSubject.TYPE -> getMerlotDataDeliveryServiceOfferingCs(id, "Push");
            case MerlotCoopContractServiceOfferingCredentialSubject.TYPE -> getMerlotCoopContractServiceOfferingCs(id);
            default -> null;
        };
        ExtendedVerifiablePresentation vp = createVpFromCsList(
                List.of(
                        gxCs,
                        merlotCs,
                        merlotSpecificCs
                ),
                "did:web:someorga"
        );

        dto.setMetadata(metaDto);
        dto.setProviderDetails(providerDto);
        dto.setSelfDescription(vp);
        return dto;
    }

    private MerlotParticipantDto createOrganizationsOrchestratorResponse(String id) throws JsonProcessingException {
        MerlotParticipantDto dto = new MerlotParticipantDto();
        dto.setSelfDescription(createVpFromCsList(
                List.of(
                        getGxParticipantCs(id),
                        getGxRegistrationNumberCs(id),
                        getMerlotParticipantCs(id)
                ),
                "did:web:someorga"
        ));
        return dto;
    }

    private String getParticipantId(int num) {
        return "did:web:"+ merlotDomain + ":participant:orga-" + num;
    }

    @BeforeAll
    public void setUp() {
        ContractTnc tnc = new ContractTnc();
        tnc.setContent("http://example.com");
        tnc.setHash("hash1234");

        saasContract = new SaasContractTemplate();
        saasContract.setConsumerId(getParticipantId(10));
        saasContract.setProviderId(getParticipantId(20));
        saasContract.setOfferingId("urn:uuid:" + UUID.randomUUID());
        saasContract.setTermsAndConditions(List.of(tnc));
        contractTemplateRepository.save(saasContract);

        dataDeliveryContract = new DataDeliveryContractTemplate();
        dataDeliveryContract.setConsumerId(getParticipantId(20));
        dataDeliveryContract.setProviderId(getParticipantId(10));
        dataDeliveryContract.setOfferingId("urn:uuid:" + UUID.randomUUID());
        dataDeliveryContract.setTermsAndConditions(List.of(tnc));
        dataDeliveryContract.setServiceContractProvisioning(new DataDeliveryProvisioning());
        dataDeliveryContract.getServiceContractProvisioning()
                .setConsumerTransferProvisioning(new IonosS3ConsumerTransferProvisioning());
        dataDeliveryContract.getServiceContractProvisioning()
                .setProviderTransferProvisioning(new IonosS3ProviderTransferProvisioning());
        contractTemplateRepository.save(dataDeliveryContract);

        coopContract = new CooperationContractTemplate();
        coopContract.setConsumerId(getParticipantId(10));
        coopContract.setProviderId(getParticipantId(20));
        coopContract.setOfferingId("urn:uuid:" + UUID.randomUUID());
        coopContract.setTermsAndConditions(List.of(tnc));
        contractTemplateRepository.save(coopContract);

        this.defaultPageRequest = PageRequest.of(0, 9, Sort.by("creationDate").descending());

    }

    @BeforeEach
    public void beforeEach() throws IOException, StorageClientException {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ServiceOfferingDto offering4321 = createServiceOfferingOrchestratorResponse(
                "urn:uuid:4321",
                "4321",
                "OfferingName",
                getParticipantId(40),
                MerlotSaasServiceOfferingCredentialSubject.TYPE
        );

        ServiceOfferingDto saasOffering = createServiceOfferingOrchestratorResponse(
                saasContract.getOfferingId(),
                "1234",
                "MyOffering",
                saasContract.getProviderId(),
                MerlotSaasServiceOfferingCredentialSubject.TYPE
        );

        ServiceOfferingDto dataDeliveryOffering = createServiceOfferingOrchestratorResponse(
                dataDeliveryContract.getOfferingId(),
                "2345",
                "MyOffering",
                dataDeliveryContract.getProviderId(),
                MerlotDataDeliveryServiceOfferingCredentialSubject.TYPE
        );

        ServiceOfferingDto coopOffering = createServiceOfferingOrchestratorResponse(
                coopContract.getOfferingId(),
                "3456",
                "MyOffering",
                coopContract.getProviderId(),
                MerlotCoopContractServiceOfferingCredentialSubject.TYPE
        );


        lenient().when(serviceOfferingOrchestratorClient.getOfferingDetails(eq("urn:uuid:4321"), any()))
                .thenReturn(offering4321);
        lenient().when(messageQueueService.remoteRequestOfferingDetails(eq("urn:uuid:4321")))
                .thenReturn(offering4321);

        lenient().when(serviceOfferingOrchestratorClient.getOfferingDetails(eq(saasContract.getOfferingId()), any()))
                .thenReturn(saasOffering);
        lenient().when(messageQueueService.remoteRequestOfferingDetails(eq(saasContract.getOfferingId())))
                .thenReturn(saasOffering);

        lenient().when(serviceOfferingOrchestratorClient.getOfferingDetails(eq(dataDeliveryContract.getOfferingId()), any()))
                .thenReturn(dataDeliveryOffering);
        lenient().when(messageQueueService.remoteRequestOfferingDetails(eq(dataDeliveryContract.getOfferingId())))
                .thenReturn(dataDeliveryOffering);

        lenient().when(serviceOfferingOrchestratorClient.getOfferingDetails(eq(coopContract.getOfferingId()), any()))
                .thenReturn(coopOffering);
        lenient().when(messageQueueService.remoteRequestOfferingDetails(eq(coopContract.getOfferingId())))
                .thenReturn(coopOffering);

        MerlotParticipantDto organizationOrchestratorResponse = createOrganizationsOrchestratorResponse(getParticipantId(40));
        lenient().when(organizationOrchestratorClient.getOrganizationDetails(any(), any()))
                .thenReturn(organizationOrchestratorResponse);

        lenient().when(storageClient.getItem(any(), any())).thenReturn(new byte[]{0x01, 0x02, 0x03, 0x04});

        contractStorageService = new ContractStorageService(
                entityManager,
                serviceOfferingOrchestratorClient,
                organizationOrchestratorClient,
                pdfServiceClient,
                messageQueueService,
                contractTemplateRepository,
                contractToDtoMapper,
                contractFromDtoMapper,
                contractDtoToPdfMapper,
                storageClient
        );
    }

    @Test
    void getOrganizationContractsExisting() {
        Page<ContractBasicDto> contracts = contractStorageService.getOrganizationContracts(getParticipantId(10),
                PageRequest.of(0, 9, Sort.by("creationDate").descending()), null , "authToken");

        assertFalse(contracts.isEmpty());
    }

    @Test
    void getOrganizationContractsNonExisting() {
        Page<ContractBasicDto> contracts = contractStorageService.getOrganizationContracts(getParticipantId(99),
                PageRequest.of(0, 9, Sort.by("creationDate").descending()), null, "authToken");

        assertTrue(contracts.isEmpty());
    }

    @Test
    void getOrganizationContractsInvalidOrgaId() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                contractStorageService.getOrganizationContracts("garbage", this.defaultPageRequest, null, "authToken"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    void getOrganizationContractsFilteredExisting() {
        Page<ContractBasicDto> contracts = contractStorageService.getOrganizationContracts(getParticipantId(10),
                PageRequest.of(0, 9, Sort.by("creationDate").descending()), ContractState.RELEASED , "authToken");

        assertTrue(contracts.isEmpty());
        contracts = contractStorageService.getOrganizationContracts(getParticipantId(10),
                PageRequest.of(0, 9, Sort.by("creationDate").descending()), ContractState.IN_DRAFT , "authToken");
        assertFalse(contracts.isEmpty());
    }

    @Test
    void getContractByIdExistent() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add("10");
        ContractDto contract = contractStorageService.getContractDetails(saasContract.getId(), "authToken");

        assertEquals(saasContract.getConsumerId(), contract.getDetails().getConsumerId());
    }

    @Test
    void getContractByIdNonExistent() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add("10");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.getContractDetails("Contract:1234", "authToken"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createContractTemplateSaasValidRequest() throws Exception {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId(getParticipantId(40));
        request.setOfferingId(saasContract.getOfferingId());
        ContractDto contract = contractStorageService.addContractTemplate(request, "authToken");

        assertEquals(saasContract.getProviderId(), contract.getDetails().getProviderId());
    }

    @Test
    void createContractTemplateDataDeliveryValidRequest() throws Exception {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId(getParticipantId(40));
        request.setOfferingId(dataDeliveryContract.getOfferingId());
        ContractDto contract = contractStorageService.addContractTemplate(request, "authToken");

        assertEquals(dataDeliveryContract.getProviderId(), contract.getDetails().getProviderId());
    }

    @Test
    void createContractTemplateCooperationValidRequest() throws Exception {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId(getParticipantId(40));
        request.setOfferingId(coopContract.getOfferingId());
        ContractDto contract = contractStorageService.addContractTemplate(request, "authToken");

        assertEquals(coopContract.getProviderId(), contract.getDetails().getProviderId());
    }

    @Test
    void createContractTemplateInvalidConsumerIsProvider() {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId(saasContract.getProviderId());
        request.setOfferingId(saasContract.getOfferingId());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.addContractTemplate(request, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    void createContractTemplateInvalidConsumerId() {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId("garbage");
        request.setOfferingId("urn:uuid:4321");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.addContractTemplate(request, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    void createContractTemplateInvalidOfferingId() {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId(getParticipantId(10));
        request.setOfferingId("garbage");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.addContractTemplate(request, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    @Transactional
    void updateContractExistingAllowedAsConsumer() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(saasContract.getConsumerId());
        SaasContractDto editedContract = (SaasContractDto) contractStorageService.getContractDetails(saasContract.getId(), "authToken");

        editedContract.getNegotiation().setConsumerTncAccepted(true);
        editedContract.getNegotiation().setConsumerAttachmentsAccepted(true);
        editedContract.getNegotiation().setRuntimeSelection("0 unlimited");

        SaasContractDto result = (SaasContractDto) contractStorageService.updateContractTemplate(editedContract, "token",
                representedOrgaIds.iterator().next());

        assertEquals(editedContract.getNegotiation().isConsumerTncAccepted(), result.getNegotiation().isConsumerTncAccepted());
        assertEquals(editedContract.getNegotiation().isConsumerAttachmentsAccepted(), result.getNegotiation().isConsumerAttachmentsAccepted());
        assertEquals(editedContract.getNegotiation().getRuntimeSelection(), result.getNegotiation().getRuntimeSelection());
        assertInstanceOf(SaasContractDto.class, result);
    }

    @Test
    @Transactional
    void updateContractExistingAllowedAsConsumerSaas() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(saasContract.getConsumerId());
        SaasContractDto editedContract = (SaasContractDto) contractStorageService.getContractDetails(saasContract.getId(), "authToken");

        editedContract.getDetails().setId(saasContract.getId());
        editedContract.getNegotiation().setUserCountSelection("0");

        SaasContractDto result = (SaasContractDto) contractStorageService
                .updateContractTemplate(editedContract, "token",
                        representedOrgaIds.iterator().next());

        assertEquals(editedContract.getNegotiation().getUserCountSelection(), result.getNegotiation().getUserCountSelection());
    }

    @Test
    @Transactional
    void updateContractExistingAllowedAsConsumerDataDelivery() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(dataDeliveryContract.getConsumerId());
        DataDeliveryContractDto editedContract = (DataDeliveryContractDto) contractStorageService.getContractDetails(dataDeliveryContract.getId(),
                "authToken");

        editedContract.getNegotiation().setExchangeCountSelection("0");

        DataDeliveryContractDto result = (DataDeliveryContractDto) contractStorageService
                .updateContractTemplate(editedContract, "authToken",
                        representedOrgaIds.iterator().next());

        assertEquals(editedContract.getNegotiation().getExchangeCountSelection(), result.getNegotiation().getExchangeCountSelection());
    }

    @Test
    @Transactional
    void updateContractExistingAllowedAsProvider() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(saasContract.getProviderId());
        SaasContractDto editedContract = (SaasContractDto) contractStorageService.getContractDetails(saasContract.getId(),
                "authToken");

        editedContract.getNegotiation().setProviderTncAccepted(true);
        editedContract.getNegotiation().setAdditionalAgreements("agreement");

        SaasContractDto result = (SaasContractDto) contractStorageService.updateContractTemplate(editedContract, "token",
                representedOrgaIds.iterator().next());
        assertEquals(editedContract.getNegotiation().isProviderTncAccepted(), result.getNegotiation().isProviderTncAccepted());
        assertEquals(editedContract.getNegotiation().getAdditionalAgreements(), result.getNegotiation().getAdditionalAgreements());
    }

    @Test
    @Transactional
    void updateContractNonExistent() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(saasContract.getProviderId());
        SaasContractDto editedContract = new SaasContractDto();
        editedContract.setDetails(new SaasContractDetailsDto());
        editedContract.getDetails().setId("garbage");
        String activeRoleOrgaId = representedOrgaIds.iterator().next();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.updateContractTemplate(editedContract, "authToken",
                        activeRoleOrgaId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @Transactional
    void updateContractNotAuthorized() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add("1234");
        SaasContractDto editedContract = new SaasContractDto();
        editedContract.setDetails(new SaasContractDetailsDto());
        editedContract.getDetails().setId(saasContract.getId());

        String activeRoleOrgaId = representedOrgaIds.iterator().next();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.updateContractTemplate(editedContract, "authToken",
                        activeRoleOrgaId));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    private void assertUpdateThrowsUnprocessableEntity(ContractDto contractDto, String token,
                                                       String activeRoleOrgaId) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.updateContractTemplate(contractDto, token,
                        activeRoleOrgaId));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    @Transactional
    void updateContractModifyImmutableBaseFields() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        String provider = saasContract.getProviderId();
        String consumer = saasContract.getConsumerId();
        representedOrgaIds.add(provider);
        representedOrgaIds.add(consumer);

        SaasContractDto editedContract = (SaasContractDto) contractStorageService.getContractDetails(saasContract.getId(),
                "authToken");

        editedContract.getDetails().setConsumerId(getParticipantId(99));
        editedContract.getDetails().setProviderId(getParticipantId(99));
        editedContract.getDetails().setTermsAndConditions(Collections.emptyList());

        SaasContractDto result = (SaasContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                provider);

        assertNotEquals(editedContract.getDetails().getConsumerId(), result.getDetails().getConsumerId());
        assertEquals(saasContract.getConsumerId(), result.getDetails().getConsumerId());
        assertNotEquals(editedContract.getDetails().getProviderId(), result.getDetails().getProviderId());
        assertEquals(saasContract.getProviderId(), result.getDetails().getProviderId());
        assertNotEquals(editedContract.getDetails().getTermsAndConditions(), result.getDetails().getTermsAndConditions());
    }

    @Test
    @Transactional
    void updateContractModifyForbiddenFieldsAsProvider() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        String provider = saasContract.getProviderId();
        representedOrgaIds.add(provider);

        SaasContractDto editedContract = (SaasContractDto) contractStorageService.getContractDetails(saasContract.getId(),
                "authToken");
        editedContract.getNegotiation().setConsumerTncAccepted(true);
        editedContract.getNegotiation().setConsumerAttachmentsAccepted(true);
        SaasContractDto result = (SaasContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                provider);

        assertNotEquals(editedContract.getNegotiation().isConsumerTncAccepted(), result.getNegotiation().isConsumerTncAccepted());
        assertEquals(saasContract.isConsumerTncAccepted(), result.getNegotiation().isConsumerTncAccepted());
        assertNotEquals(editedContract.getNegotiation().isConsumerAttachmentsAccepted(), result.getNegotiation().isConsumerAttachmentsAccepted());
        assertEquals(saasContract.isConsumerAttachmentsAccepted(), result.getNegotiation().isConsumerAttachmentsAccepted());
    }

    @Test
    @Transactional
    void updateContractModifyForbiddenFieldsAsConsumer() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId();
        representedOrgaIds.add(consumer);

        DataDeliveryContractDto editedContract = (DataDeliveryContractDto) contractStorageService.getContractDetails(dataDeliveryContract.getId(),
                "authToken");

        editedContract.getNegotiation().setProviderTncAccepted(true);
        editedContract.getNegotiation().setAdditionalAgreements("garbage");
        // TODO provisioning fields

        DataDeliveryContractDto result = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);
        assertNotEquals(editedContract.getNegotiation().isProviderTncAccepted(), result.getNegotiation().isProviderTncAccepted());
        assertEquals(dataDeliveryContract.isProviderTncAccepted(), result.getNegotiation().isProviderTncAccepted());
        assertNotEquals(editedContract.getNegotiation().getAdditionalAgreements(), result.getNegotiation().getAdditionalAgreements());
        assertEquals(dataDeliveryContract.getAdditionalAgreements(), result.getNegotiation().getAdditionalAgreements());

    }

    @Test
    @Transactional
    void updateContractSetInvalidSelection() {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = saasContract.getConsumerId();
        representedOrgaIds.add(consumer);

        SaasContractDto editedContract = (SaasContractDto) contractStorageService.getContractDetails(saasContract.getId(),
                "authToken");
        editedContract.getNegotiation().setRuntimeSelection("garbage");
        assertUpdateThrowsUnprocessableEntity(editedContract, "authToken", consumer);
    }

    @Test
    @Transactional
    void updateContractSetInvalidSelectionSaas() {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = saasContract.getConsumerId();
        representedOrgaIds.add(consumer);

        SaasContractDto editedContract = (SaasContractDto) contractStorageService.getContractDetails(saasContract.getId(),
                "authToken");
        editedContract.getNegotiation().setUserCountSelection("garbage");
        assertUpdateThrowsUnprocessableEntity(editedContract, "authToken", consumer);
    }

    @Test
    @Transactional
    void updateContractSetInvalidSelectionDataDelivery() {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = saasContract.getConsumerId();
        representedOrgaIds.add(consumer);

        DataDeliveryContractDto editedContract = (DataDeliveryContractDto) contractStorageService.getContractDetails(dataDeliveryContract.getId(),
                "authToken");
        editedContract.getNegotiation().setExchangeCountSelection("garbage");
        assertUpdateThrowsUnprocessableEntity(editedContract, "authToken", consumer);
    }

    private void assertTransitionThrowsForbidden(String contractId, ContractState state,
                                                 String activeRoleOrgaId) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.transitionContractTemplateState(contractId,
                        state, activeRoleOrgaId, "User Name", "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    private void assertTransitionThrowsBadRequest(String contractId, ContractState state,
                                                 String activeRoleOrgaId) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.transitionContractTemplateState(contractId,
                        state, activeRoleOrgaId, "User Name", "authToken"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @Transactional
    void transitionDataDeliveryConsumerIncompleteToComplete() throws JSONException, IOException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId();
        representedOrgaIds.add(consumer);

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);
        template.setServiceContractProvisioning(new DataDeliveryProvisioning()); // reset provisioning
        template.getServiceContractProvisioning().setConsumerTransferProvisioning(new IonosS3ConsumerTransferProvisioning());
        template.getServiceContractProvisioning().setProviderTransferProvisioning(new IonosS3ProviderTransferProvisioning());
        contractTemplateRepository.save(template);

        DataDeliveryContractDto editedContract = (DataDeliveryContractDto) contractStorageService.getContractDetails(dataDeliveryContract.getId(),
                "authToken");


        assertTransitionThrowsBadRequest(editedContract.getDetails().getId(), ContractState.SIGNED_CONSUMER, consumer);
        editedContract.getNegotiation().setConsumerTncAccepted(true);
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);
        contractStorageService.getContractDetails(editedContract.getDetails().getId(),
                "authToken");
        assertTransitionThrowsBadRequest(editedContract.getDetails().getId(), ContractState.SIGNED_CONSUMER, consumer);

        editedContract.getNegotiation().setConsumerAttachmentsAccepted(true);
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);
        contractStorageService.getContractDetails(editedContract.getDetails().getId(),
                "authToken");
        assertTransitionThrowsBadRequest(editedContract.getDetails().getId(), ContractState.SIGNED_CONSUMER, consumer);

        editedContract.getNegotiation().setExchangeCountSelection("0");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);
        contractStorageService.getContractDetails(editedContract.getDetails().getId(),
                "authToken");
        assertTransitionThrowsBadRequest(editedContract.getDetails().getId(), ContractState.SIGNED_CONSUMER, consumer);

        editedContract.getNegotiation().setRuntimeSelection("0 unlimited");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);
        contractStorageService.getContractDetails(editedContract.getDetails().getId(),
                "authToken");
        assertTransitionThrowsBadRequest(editedContract.getDetails().getId(), ContractState.SIGNED_CONSUMER, consumer);

        ((IonosS3ConsumerTransferProvisioningDto) editedContract.getProvisioning().getConsumerTransferProvisioning()).setDataAddressTargetPath("targetpath/");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);
        contractStorageService.getContractDetails(editedContract.getDetails().getId(),
                "authToken");
        assertTransitionThrowsBadRequest(editedContract.getDetails().getId(), ContractState.SIGNED_CONSUMER, consumer);

        ((IonosS3ConsumerTransferProvisioningDto) editedContract.getProvisioning().getConsumerTransferProvisioning()).setDataAddressTargetBucketName("MyBucket");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);
        contractStorageService.getContractDetails(editedContract.getDetails().getId(),
                "authToken");
        assertTransitionThrowsBadRequest(editedContract.getDetails().getId(), ContractState.SIGNED_CONSUMER, consumer);

        editedContract.getProvisioning().getConsumerTransferProvisioning().setSelectedConnectorId("edc1");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);
        DataDeliveryContractDto result = (DataDeliveryContractDto) contractStorageService.transitionContractTemplateState(editedContract.getDetails().getId(),
                ContractState.SIGNED_CONSUMER, consumer, "User Name", "authToken");
        assertEquals(ContractState.SIGNED_CONSUMER.name(), result.getDetails().getState());
    }

    @Test
    @Transactional
    void transitionDataDeliveryProviderIncompleteToComplete() throws JSONException, IOException,
        StorageClientException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId();
        String provider = dataDeliveryContract.getProviderId();
        representedOrgaIds.add(provider);

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);
        template.setServiceContractProvisioning(new DataDeliveryProvisioning()); // reset provisioning
        template.getServiceContractProvisioning().setConsumerTransferProvisioning(new IonosS3ConsumerTransferProvisioning());
        template.getServiceContractProvisioning().setProviderTransferProvisioning(new IonosS3ProviderTransferProvisioning());
        contractTemplateRepository.save(template);

        DataDeliveryContractDto editedContract = (DataDeliveryContractDto) contractStorageService.getContractDetails(dataDeliveryContract.getId(),
                "authToken");

        editedContract.getNegotiation().setExchangeCountSelection("0");
        editedContract.getNegotiation().setRuntimeSelection("0 unlimited");
        editedContract.getNegotiation().setConsumerTncAccepted(true);
        editedContract.getNegotiation().setConsumerAttachmentsAccepted(true);
        ((IonosS3ConsumerTransferProvisioningDto) editedContract.getProvisioning().getConsumerTransferProvisioning()).setDataAddressTargetPath("targetpath/");
        ((IonosS3ConsumerTransferProvisioningDto) editedContract.getProvisioning().getConsumerTransferProvisioning()).setDataAddressTargetBucketName("MyBucket");
        editedContract.getProvisioning().getConsumerTransferProvisioning().setSelectedConnectorId("edc1");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);

        editedContract = (DataDeliveryContractDto) contractStorageService.transitionContractTemplateState(editedContract.getDetails().getId(),
                ContractState.SIGNED_CONSUMER, consumer, "User Name", "authToken");
        assertEquals(ContractState.SIGNED_CONSUMER.name(), editedContract.getDetails().getState());

        assertTransitionThrowsBadRequest(editedContract.getDetails().getId(), ContractState.RELEASED, provider);

        editedContract.getNegotiation().setProviderTncAccepted(true);
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                provider);
        assertTransitionThrowsBadRequest(editedContract.getDetails().getId(), ContractState.RELEASED, provider);

        editedContract.getProvisioning().getConsumerTransferProvisioning().setDataAddressType("IonosS3Dest");
        editedContract.getProvisioning().getProviderTransferProvisioning().setDataAddressType("IonosS3Source");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                provider);
        assertTransitionThrowsBadRequest(editedContract.getDetails().getId(), ContractState.RELEASED, provider);

        ((IonosS3ProviderTransferProvisioningDto) editedContract.getProvisioning().getProviderTransferProvisioning()).setDataAddressSourceBucketName("MyBucket2");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                provider);
        assertTransitionThrowsBadRequest(editedContract.getDetails().getId(), ContractState.RELEASED, provider);

        ((IonosS3ProviderTransferProvisioningDto) editedContract.getProvisioning().getProviderTransferProvisioning()).setDataAddressSourceFileName("MyFile2.json");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                provider);
        assertTransitionThrowsBadRequest(editedContract.getDetails().getId(), ContractState.RELEASED, provider);

        editedContract.getProvisioning().getProviderTransferProvisioning().setSelectedConnectorId("edc2");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                provider);

        editedContract = (DataDeliveryContractDto) contractStorageService.transitionContractTemplateState(editedContract.getDetails().getId(),
                ContractState.RELEASED, provider, "User Name", "authToken");
        assertEquals(ContractState.RELEASED.name(), editedContract.getDetails().getState());

        verify(pdfServiceClient).getPdfContract(any());
        verify(storageClient).pushItem(eq(editedContract.getDetails().getId() + "/contractPdf"), eq(editedContract.getDetails().getId() + ".pdf"), any());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.NOT_SUPPORTED) // handle transactions manually
    void transitionDataDeliveryProviderIncompleteToCompleteFailAtRelease() throws StorageClientException {
        transactionTemplate = new TransactionTemplate(transactionManager);

        doThrow(StorageClientException.class).when(storageClient).pushItem(any(), any(), any(byte[].class));

        String consumer = dataDeliveryContract.getConsumerId();
        String provider = dataDeliveryContract.getProviderId();

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);
        template.setServiceContractProvisioning(new DataDeliveryProvisioning()); // reset provisioning
        template.getServiceContractProvisioning().setConsumerTransferProvisioning(new IonosS3ConsumerTransferProvisioning());
        template.getServiceContractProvisioning().setProviderTransferProvisioning(new IonosS3ProviderTransferProvisioning());

        transactionTemplate.execute(status -> {
            contractTemplateRepository.save(template);
            return "foo";
        });

        DataDeliveryContractDto editedContract = transactionTemplate.execute(status -> {
            try {
                DataDeliveryContractDto contract = (DataDeliveryContractDto) contractStorageService
                    .getContractDetails(template.getId(), "authToken");

                contract.getNegotiation().setExchangeCountSelection("0");
                contract.getNegotiation().setRuntimeSelection("0 unlimited");
                contract.getNegotiation().setConsumerTncAccepted(true);
                contract.getNegotiation().setConsumerAttachmentsAccepted(true);
                ((IonosS3ConsumerTransferProvisioningDto) contract.getProvisioning().getConsumerTransferProvisioning()).setDataAddressTargetPath("targetpath/");
                ((IonosS3ConsumerTransferProvisioningDto) contract.getProvisioning().getConsumerTransferProvisioning()).setDataAddressTargetBucketName("MyBucket");
                contract.getProvisioning().getConsumerTransferProvisioning().setSelectedConnectorId("edc1");

                contract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(contract, "authToken",
                    consumer);

                contract = (DataDeliveryContractDto) contractStorageService.transitionContractTemplateState(contract.getDetails().getId(),
                    ContractState.SIGNED_CONSUMER, consumer, "User Name", "authToken");

                return contract;
            } catch (JSONException | IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertNotNull(editedContract);

        Exception thrownEx = null;
        try {
            transactionTemplate.execute(status -> {
                try {
                    DataDeliveryContractDto contract = editedContract;
                    contract.getNegotiation().setProviderTncAccepted(true);
                    contract.getProvisioning().getConsumerTransferProvisioning().setDataAddressType("IonosS3Dest");
                    contract.getProvisioning().getProviderTransferProvisioning().setDataAddressType("IonosS3Source");
                    ((IonosS3ProviderTransferProvisioningDto) contract.getProvisioning().getProviderTransferProvisioning()).setDataAddressSourceBucketName("MyBucket2");
                    ((IonosS3ProviderTransferProvisioningDto) contract.getProvisioning().getProviderTransferProvisioning()).setDataAddressSourceFileName("MyFile2.json");
                    contract.getProvisioning().getProviderTransferProvisioning().setSelectedConnectorId("edc2");
                    contract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(contract,
                        "authToken", provider);

                    contract = (DataDeliveryContractDto) contractStorageService.transitionContractTemplateState(
                        contract.getDetails().getId(), ContractState.RELEASED, provider,
                        "User Name", "authToken");

                    return "foo";
                } catch (JSONException | IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception ex) {
            thrownEx = ex;
        }

        assertNotNull(thrownEx);
        assertEquals(thrownEx.getClass(), ResponseStatusException.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ((ResponseStatusException) thrownEx).getStatusCode());
        assertEquals("Encountered error while processing the contract.", ((ResponseStatusException) thrownEx).getReason());

        ContractTemplate contractTemplate = contractTemplateRepository.findById(template.getId()).orElse(null);
        assertNotNull(contractTemplate);
        // the last successful state transition was to the SIGNED_CONSUMER state
        // the contract should still be in that state as transitioning to the RELEASED state failed
        assertEquals(ContractState.SIGNED_CONSUMER, contractTemplate.getState());
    }

    @Test
    @Transactional
    void transitionContractNotAuthorized() {
        SaasContractTemplate template = new SaasContractTemplate(saasContract, false);
        String templateId = template.getId();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.transitionContractTemplateState(templateId,
                        ContractState.SIGNED_CONSUMER, "99", "User Name", "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @Transactional
    void transitionContractProviderNotAllowed() {
        String provider = saasContract.getProviderId();

        SaasContractTemplate template = new SaasContractTemplate(saasContract, false);
        String templateId = template.getId();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.transitionContractTemplateState(templateId,
                        ContractState.SIGNED_CONSUMER, provider, "User Name", "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @Transactional
    void transitionContractConsumerNotAllowed() throws IOException {
        String consumer = dataDeliveryContract.getConsumerId();

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);
        IonosS3ConsumerTransferProvisioning consumerProvisioning =
                (IonosS3ConsumerTransferProvisioning) template.getServiceContractProvisioning()
                        .getConsumerTransferProvisioning();
        String templateId = template.getId();
        template.setExchangeCountSelection("0");
        template.setRuntimeSelection("0 unlimited");
        template.setConsumerTncAccepted(true);
        template.setConsumerAttachmentsAccepted(true);
        consumerProvisioning.setDataAddressTargetPath("targetpath/");
        consumerProvisioning.setDataAddressTargetBucketName("MyBucket");
        consumerProvisioning.setSelectedConnectorId("edc1");
        contractTemplateRepository.save(template);
        contractStorageService.transitionContractTemplateState(templateId,
                ContractState.SIGNED_CONSUMER, consumer, "User Name", "authToken");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.transitionContractTemplateState(templateId,
                        ContractState.RELEASED, consumer, "User Name", "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @Transactional
    void transitionSaasContractRevokedNotAllowed() throws JSONException, IOException {
        String consumer = saasContract.getConsumerId();
        String provider = saasContract.getProviderId();

        SaasContractDto editedContract = (SaasContractDto) contractStorageService.getContractDetails(saasContract.getId(),
                "authToken");

        editedContract.getNegotiation().setConsumerTncAccepted(true);
        editedContract.getNegotiation().setConsumerAttachmentsAccepted(true);
        editedContract.getNegotiation().setUserCountSelection("0");
        editedContract.getNegotiation().setRuntimeSelection("4 day(s)");
        editedContract = (SaasContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);

        SaasContractDto result = (SaasContractDto) contractStorageService.transitionContractTemplateState(editedContract.getDetails().getId(),
                ContractState.SIGNED_CONSUMER, consumer, "User Name", "authToken");

        result.getNegotiation().setProviderTncAccepted(true);

        SaasContractDto result2 = (SaasContractDto) contractStorageService
                .updateContractTemplate(result, "authToken",
                        provider);

        assertEquals(result.getNegotiation().isProviderTncAccepted(), result2.getNegotiation().isProviderTncAccepted());

        result = (SaasContractDto) contractStorageService.transitionContractTemplateState(result.getDetails().getId(),
                ContractState.RELEASED, provider, "User Name", "authToken");

        assertTransitionThrowsBadRequest(result.getDetails().getId(), ContractState.REVOKED, provider);
    }

    @Test
    @Transactional
    void transitionCooperationContractRevokedNotAllowed() throws JSONException, IOException {
        String consumer = coopContract.getConsumerId();
        String provider = coopContract.getProviderId();
        CooperationContractTemplate template = new CooperationContractTemplate(coopContract, false);

        CooperationContractDto editedContract = (CooperationContractDto) contractStorageService.getContractDetails(coopContract.getId(),
                "authToken");

        editedContract.getNegotiation().setConsumerTncAccepted(true);
        editedContract.getNegotiation().setConsumerAttachmentsAccepted(true);
        editedContract.getNegotiation().setRuntimeSelection("4 day(s)");

        CooperationContractDto result = (CooperationContractDto) contractStorageService
                .updateContractTemplate(editedContract, "authToken",
                        consumer);

        result = (CooperationContractDto) contractStorageService.transitionContractTemplateState(result.getDetails().getId(),
                ContractState.SIGNED_CONSUMER, consumer, "User Name", "authToken");

        result.getNegotiation().setProviderTncAccepted(true);

        CooperationContractDto result2 = (CooperationContractDto) contractStorageService
                .updateContractTemplate(result, "authToken",
                        provider);

        assertEquals(result.getNegotiation().isProviderTncAccepted(), result2.getNegotiation().isProviderTncAccepted());

        result = (CooperationContractDto) contractStorageService.transitionContractTemplateState(result.getDetails().getId(),
                ContractState.RELEASED, provider, "User Name", "authToken");

        assertTransitionThrowsBadRequest(result.getDetails().getId(), ContractState.REVOKED, provider);
    }

    @Test
    @Transactional
    void transitionDataDeliveryContractPurge() throws IOException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId();
        String provider = dataDeliveryContract.getProviderId();
        representedOrgaIds.add(consumer);
        representedOrgaIds.add(provider);
        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);
        template.setServiceContractProvisioning(new DataDeliveryProvisioning()); // reset provisioning
        template.getServiceContractProvisioning().setConsumerTransferProvisioning(new IonosS3ConsumerTransferProvisioning());
        template.getServiceContractProvisioning().setProviderTransferProvisioning(new IonosS3ProviderTransferProvisioning());
        contractTemplateRepository.save(template);

        DataDeliveryContractDto result = (DataDeliveryContractDto) contractStorageService.transitionContractTemplateState(template.getId(),
                ContractState.DELETED, consumer, "User Name", "authToken");

        result = (DataDeliveryContractDto) contractStorageService.transitionContractTemplateState(template.getId(),
                ContractState.PURGED, provider, "User Name", "authToken");

        assertNull(contractTemplateRepository.findById(result.getDetails().getId()).orElse(null));
    }

    @Test
    @Transactional
    void transitionDataDeliveryContractPurgeWrongState() {
        String contractId = dataDeliveryContract.getId();
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId();
        String provider = dataDeliveryContract.getProviderId();
        representedOrgaIds.add(consumer);
        representedOrgaIds.add(provider);
        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () ->contractStorageService.transitionContractTemplateState(contractId,
                        ContractState.PURGED, provider, "User Name", "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @Transactional
    void transitionDataDeliveryContractPurgeWrongRole() throws IOException {
        String contractId = dataDeliveryContract.getId();
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId();
        String provider = dataDeliveryContract.getProviderId();
        representedOrgaIds.add(consumer);
        representedOrgaIds.add(provider);

        DataDeliveryContractDto result = (DataDeliveryContractDto) contractStorageService
                .transitionContractTemplateState(contractId, ContractState.DELETED, consumer, "User Name", "authToken");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () ->contractStorageService.transitionContractTemplateState(contractId,
                        ContractState.PURGED, consumer, "User Name", "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }


    @Test
    @Transactional
    void updateDataDeliveryFieldsAfterTransition() throws JSONException, IOException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId();
        String provider = dataDeliveryContract.getProviderId();
        representedOrgaIds.add(consumer);
        representedOrgaIds.add(provider);
        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);
        template.setServiceContractProvisioning(new DataDeliveryProvisioning()); // reset provisioning
        template.getServiceContractProvisioning().setConsumerTransferProvisioning(new IonosS3ConsumerTransferProvisioning());
        template.getServiceContractProvisioning().setProviderTransferProvisioning(new IonosS3ProviderTransferProvisioning());
        contractTemplateRepository.save(template);

        DataDeliveryContractDto editedContract = (DataDeliveryContractDto) contractStorageService.getContractDetails(dataDeliveryContract.getId(),
                "authToken");

        editedContract.getNegotiation().setConsumerTncAccepted(true);
        editedContract.getNegotiation().setConsumerAttachmentsAccepted(true);
        editedContract.getNegotiation().setExchangeCountSelection("0");
        editedContract.getNegotiation().setRuntimeSelection("4 day(s)");
        ((IonosS3ConsumerTransferProvisioningDto) editedContract.getProvisioning().getConsumerTransferProvisioning()).setDataAddressTargetBucketName("MyBucket");
        ((IonosS3ConsumerTransferProvisioningDto) editedContract.getProvisioning().getConsumerTransferProvisioning()).setDataAddressTargetPath("targetpath/");
        editedContract.getProvisioning().getConsumerTransferProvisioning().setSelectedConnectorId("edc1");

        DataDeliveryContractDto result = (DataDeliveryContractDto) contractStorageService
                .updateContractTemplate(editedContract, "authToken",
                        consumer);

        IonosS3ConsumerTransferProvisioningDto editedContractConsumerProvisioning =
                (IonosS3ConsumerTransferProvisioningDto) editedContract.getProvisioning().getConsumerTransferProvisioning();
        IonosS3ConsumerTransferProvisioningDto resultConsumerProvisioning =
                (IonosS3ConsumerTransferProvisioningDto) result.getProvisioning().getConsumerTransferProvisioning();
        assertEquals(editedContract.getNegotiation().isConsumerTncAccepted(), result.getNegotiation().isConsumerTncAccepted());
        assertEquals(editedContract.getNegotiation().isConsumerAttachmentsAccepted(), result.getNegotiation().isConsumerAttachmentsAccepted());
        assertEquals(editedContract.getNegotiation().getExchangeCountSelection(), result.getNegotiation().getExchangeCountSelection());
        assertEquals(editedContract.getNegotiation().getRuntimeSelection(), result.getNegotiation().getRuntimeSelection());
        assertEquals(editedContractConsumerProvisioning.getDataAddressTargetBucketName(), resultConsumerProvisioning.getDataAddressTargetBucketName());
        assertEquals(editedContractConsumerProvisioning.getDataAddressTargetPath(), resultConsumerProvisioning.getDataAddressTargetPath());
        assertEquals(editedContractConsumerProvisioning.getSelectedConnectorId(), resultConsumerProvisioning.getSelectedConnectorId());

        result = (DataDeliveryContractDto) contractStorageService.transitionContractTemplateState(result.getDetails().getId(),
                ContractState.SIGNED_CONSUMER, consumer, "User Name", "authToken");

        IonosS3ProviderTransferProvisioningDto resultProviderProvisioning =
                (IonosS3ProviderTransferProvisioningDto) result.getProvisioning().getProviderTransferProvisioning();
        result.getNegotiation().setProviderTncAccepted(true);
        resultProviderProvisioning.setDataAddressType("IonosS3Source");
        resultProviderProvisioning.setDataAddressSourceBucketName("MyBucket2");
        resultProviderProvisioning.setDataAddressSourceFileName("MyFile2.json");
        resultProviderProvisioning.setSelectedConnectorId("edc2");

        DataDeliveryContractDto result2 = (DataDeliveryContractDto) contractStorageService
                .updateContractTemplate(result, "token", provider);
        IonosS3ProviderTransferProvisioningDto result2ProviderProvisioning =
                (IonosS3ProviderTransferProvisioningDto) result2.getProvisioning().getProviderTransferProvisioning();

        assertEquals(result.getNegotiation().isProviderTncAccepted(), result2.getNegotiation().isProviderTncAccepted());
        assertEquals(resultProviderProvisioning.getDataAddressType(), result2ProviderProvisioning.getDataAddressType());
        assertEquals(resultProviderProvisioning.getDataAddressSourceFileName(), result2ProviderProvisioning.getDataAddressSourceFileName());
        assertEquals(resultProviderProvisioning.getDataAddressSourceBucketName(), result2ProviderProvisioning.getDataAddressSourceBucketName());
        assertEquals(resultProviderProvisioning.getSelectedConnectorId(), result2ProviderProvisioning.getSelectedConnectorId());

        result = (DataDeliveryContractDto) contractStorageService.transitionContractTemplateState(result.getDetails().getId(),
                ContractState.RELEASED, provider, "User Name", "authToken");

        assertNotNull(result.getProvisioning().getValidUntil());

    }

    @Test
    void regenerateDataDeliveryContractValid() throws IOException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId();
        representedOrgaIds.add(consumer);

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);
        template.setServiceContractProvisioning(new DataDeliveryProvisioning()); // reset provisioning
        template.getServiceContractProvisioning().setConsumerTransferProvisioning(new IonosS3ConsumerTransferProvisioning());
        template.getServiceContractProvisioning().setProviderTransferProvisioning(new IonosS3ProviderTransferProvisioning());
        contractTemplateRepository.save(template);

        DataDeliveryContractDto contract = (DataDeliveryContractDto) this.contractStorageService
                .transitionContractTemplateState(dataDeliveryContract.getId(), ContractState.DELETED, consumer, "User Name", "authToken");
        contract = (DataDeliveryContractDto) this.contractStorageService.regenerateContract(dataDeliveryContract.getId(), "authToken");

        assertNotEquals(contract.getDetails().getId(), dataDeliveryContract.getId());
        assertEquals(ContractState.IN_DRAFT.name(), contract.getDetails().getState());
    }

    @Test
    void regenerateSaasContractValid() throws IOException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = saasContract.getConsumerId();
        representedOrgaIds.add(consumer);
        SaasContractDto template = (SaasContractDto) this.contractStorageService
                .transitionContractTemplateState(saasContract.getId(), ContractState.DELETED, consumer, "User Name", "authToken");
        template = (SaasContractDto) this.contractStorageService.regenerateContract(saasContract.getId(), "authToken");

        assertNotEquals(template.getDetails().getId(), saasContract.getId());
        assertEquals(ContractState.IN_DRAFT.name(), template.getDetails().getState());
    }

    @Test
    void regenerateCooperationContractValid() throws IOException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = coopContract.getConsumerId();
        representedOrgaIds.add(consumer);
        CooperationContractDto template = (CooperationContractDto) this.contractStorageService.transitionContractTemplateState(coopContract.getId(),
                ContractState.DELETED, consumer, "User Name", "authToken");
        template = (CooperationContractDto) this.contractStorageService.regenerateContract(template.getDetails().getId(), "authToken");

        assertNotEquals(template.getDetails().getId(), coopContract.getId());
        assertEquals(ContractState.IN_DRAFT.name(), template.getDetails().getState());
    }

    @Test
    void regenerateContractNotAllowedState() {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId();
        representedOrgaIds.add(consumer);
        String templateId = dataDeliveryContract.getId();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () ->this.contractStorageService.regenerateContract(templateId, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void regenerateContractNotAllowedNotRepresenting() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add("garbage");
        String templateId = dataDeliveryContract.getId();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () ->this.contractStorageService.regenerateContract(templateId, "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void regenerateContractNonExistent() {
        Set<String> representedOrgaIds = new HashSet<>();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () ->this.contractStorageService.regenerateContract("garbage", "authToken"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void addContractAttachmentsInDraft() throws IOException {
        String templateId = dataDeliveryContract.getId();
        ContractDto result = this.contractStorageService.addContractAttachment(templateId, new byte[]{},
                "myFile.pdf", "authToken");
        assertNotNull(result);
        assertNotNull(result.getNegotiation().getAttachments());
        assertTrue(result.getNegotiation().getAttachments().contains("myFile.pdf"));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.NOT_SUPPORTED) // handle transactions manually
    void addContractAttachmentsInDraftFail() throws StorageClientException {
        transactionTemplate = new TransactionTemplate(transactionManager);

        doThrow(StorageClientException.class).when(storageClient).pushItem(any(), any(), any(byte[].class));

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);
        template.setServiceContractProvisioning(new DataDeliveryProvisioning()); // reset provisioning
        template.getServiceContractProvisioning().setConsumerTransferProvisioning(new IonosS3ConsumerTransferProvisioning());
        template.getServiceContractProvisioning().setProviderTransferProvisioning(new IonosS3ProviderTransferProvisioning());

        transactionTemplate.execute(status -> {
            contractTemplateRepository.save(template);
            return "foo";
        });

        ContractTemplate check1 = transactionTemplate.execute(status -> {
            return contractTemplateRepository.findById(template.getId()).orElse(null);
        });
        assertNotNull(check1);
        assertFalse(check1.getAttachments().contains("myFile.pdf"));

        Exception thrownEx = null;
        try {
            transactionTemplate.execute(status -> {
                contractStorageService.addContractAttachment(template.getId(), new byte[]{}, "myFile.pdf", "authToken");
                return "foo";
            });
        } catch (Exception e) {
            thrownEx = e;
        }

        assertNotNull(thrownEx);
        assertEquals(thrownEx.getClass(), ResponseStatusException.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ((ResponseStatusException) thrownEx).getStatusCode());
        assertEquals("Encountered error while saving the contract attachment.", ((ResponseStatusException) thrownEx).getReason());

        ContractTemplate check2 = transactionTemplate.execute(status -> {
            return contractTemplateRepository.findById(template.getId()).orElse(null);
        });
        assertNotNull(check2);
        assertFalse(check2.getAttachments().contains("myFile.pdf"));
    }


    @Test
    void addContractAttachmentsInDraftTooManyAttachments() throws IOException {
        String templateId = dataDeliveryContract.getId();
        for (int i = 0; i < 10; i++) {
            this.contractStorageService.addContractAttachment(templateId, new byte[]{},
                    "myFile" + i + ".pdf", "authToken");
        }
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> this.contractStorageService.addContractAttachment(templateId, new byte[]{},
                        "myFile" + 10 + ".pdf", "authToken"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void deleteContractAttachmentsInDraft() throws IOException {
        String templateId = dataDeliveryContract.getId();

        ContractDto result1 = this.contractStorageService.addContractAttachment(templateId, new byte[]{},
                "myFile.pdf", "authToken");
        assertTrue(result1.getNegotiation().getAttachments().contains("myFile.pdf"));

        ContractDto result2 = this.contractStorageService.deleteContractAttachment(templateId,
                "myFile.pdf", "authToken");
        assertNotNull(result2);
        assertNotNull(result2.getNegotiation().getAttachments());
        assertFalse(result2.getNegotiation().getAttachments().contains("myFile.pdf"));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.NOT_SUPPORTED) // handle transactions manually
    void deleteContractAttachmentsInDraftFail() throws StorageClientException {
        transactionTemplate = new TransactionTemplate(transactionManager);

        doThrow(StorageClientException.class).when(storageClient).deleteItem(any(), any());

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);
        template.setServiceContractProvisioning(new DataDeliveryProvisioning()); // reset provisioning
        template.getServiceContractProvisioning().setConsumerTransferProvisioning(new IonosS3ConsumerTransferProvisioning());
        template.getServiceContractProvisioning().setProviderTransferProvisioning(new IonosS3ProviderTransferProvisioning());

        transactionTemplate.execute(status -> {
            contractTemplateRepository.save(template);
            return "foo";
        });

        ContractTemplate check1 = transactionTemplate.execute(status -> {
            contractStorageService.addContractAttachment(template.getId(), new byte[]{},
                "myOtherFile.pdf", "authToken");

                return contractTemplateRepository.findById(template.getId()).orElse(null);
            });
        assertNotNull(check1);
        assertTrue(check1.getAttachments().contains("myOtherFile.pdf"));

        Exception thrownEx = null;
        try {
            transactionTemplate.execute(status -> {
                contractStorageService.deleteContractAttachment(template.getId(), "myOtherFile.pdf", "authToken");
                return "foo";
            });
        } catch (Exception ex) {
            thrownEx = ex;
        }

        assertNotNull(thrownEx);
        assertEquals(thrownEx.getClass(), ResponseStatusException.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ((ResponseStatusException) thrownEx).getStatusCode());
        assertEquals("Encountered error while deleting the contract attachment.", ((ResponseStatusException) thrownEx).getReason());

        ContractTemplate check2 = transactionTemplate.execute(status -> {
            return contractTemplateRepository.findById(template.getId()).orElse(null);
        });
        assertNotNull(check2);
        assertTrue(check2.getAttachments().contains("myOtherFile.pdf"));
    }

    @Test
    void deleteContractAttachmentsInDraftNonExistent() {
        String templateId = dataDeliveryContract.getId();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> this.contractStorageService.deleteContractAttachment(templateId,"garbage",
                        "authToken"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getContractAttachment() throws IOException, StorageClientException {
        String templateId = dataDeliveryContract.getId();

        ContractDto result1 = this.contractStorageService.addContractAttachment(templateId, new byte[]{},
                "myFile.pdf", "authToken");
        assertTrue(result1.getNegotiation().getAttachments().contains("myFile.pdf"));

        byte[] result2 = this.contractStorageService.getContractAttachment(templateId,"myFile.pdf");
        assertNotNull(result2);
    }

    @Test
    void getContractAttachmentNonExistent() {
        String templateId = dataDeliveryContract.getId();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> this.contractStorageService.getContractAttachment(templateId,"garbage"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
