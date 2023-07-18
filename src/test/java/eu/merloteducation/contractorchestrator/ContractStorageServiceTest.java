package eu.merloteducation.contractorchestrator;

import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.models.entities.*;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import eu.merloteducation.contractorchestrator.service.ContractSignerService;
import eu.merloteducation.contractorchestrator.service.ContractStorageService;
import eu.merloteducation.contractorchestrator.service.MessageQueueService;
import io.netty.util.internal.StringUtil;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;


@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class ContractStorageServiceTest {
    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Value("${serviceoffering-orchestrator.base-uri}")
    private String serviceOfferingOrchestratorBaseUri;

    @Value("${organizations-orchestrator.base-uri}")
    private String organizationsOrchestratorBaseUri;

    @Autowired
    private ContractTemplateRepository contractTemplateRepository;

    @Autowired
    private MessageQueueService messageQueueService;

    @Autowired
    private ContractSignerService contractSignerService;

    @InjectMocks
    private ContractStorageService contractStorageService;

    private PageRequest defaultPageRequest;

    private SaasContractTemplate template1;
    private DataDeliveryContractTemplate template2;

    private String createServiceOfferingOrchestratorResponse(String id, String hash, String name, String offeredBy,
                                                             String offeringType, String typeSpecificFields) {
        // merlot:MerlotServiceOfferingSaaS
        String response = """
                {
                    "id": "${id}",
                    "sdHash": "${hash}",
                    "name": "${name}",
                    "creationDate": null,
                    "offeredBy": "${offeredBy}",
                    "merlotState": "RELEASED",
                    "type": "${offeringType}",
                    "description": null,
                    "modifiedDate": "2023-05-26T09:55:46.189505Z",
                    "dataAccessType": "Download",
                    "exampleCosts": null,
                    "attachments": [
                        "demoAttachment"
                    ],
                    "termsAndConditions": [
                        {
                            "content": "asd",
                            "hash": "asd"
                        }
                    ],
                    "runtimeOption": [
                        {
                            "runtimeCount": 0,
                            "runtimeMeasurement": null,
                            "runtimeUnlimited": true
                        },
                       {
                            "runtimeCount": 5,
                            "runtimeMeasurement": "day(s)",
                            "runtimeUnlimited": false
                        }
                    ],
                    "merlotTermsAndConditionsAccepted": true,
                    "hardwareRequirements": null
                    ${typeSpecificFields}
                }
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("name", name);
        params.put("hash", hash);
        params.put("offeredBy", offeredBy);
        params.put("offeringType", offeringType);
        params.put("typeSpecificFields", typeSpecificFields);
        return StringSubstitutor.replace(response, params, "${", "}");
    }

    private String createOrganizationsOrchestratorResponse(String id) {
        String response = """
                {
                    "id": "Participant:${id}",
                    "merlotId": "${id}",
                    "organizationName": "Gaia-X AISBL",
                    "organizationLegalName": "Gaia-X European Association for Data and Cloud AISBL",
                    "registrationNumber": "0762747721",
                    "termsAndConditionsLink": "http://example.com",
                    "legalAddress": {
                        "countryCode": "BE",
                        "postalCode": "1210",
                        "addressCode": "BE-BRU",
                        "city": "Brüssel",
                        "street": "Avenue des Arts 6-9"
                    }
                }
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        return StringSubstitutor.replace(response, params, "${", "}");
    }

    @BeforeAll
    public void setUp() {
        ReflectionTestUtils.setField(contractStorageService, "serviceOfferingOrchestratorBaseUri", serviceOfferingOrchestratorBaseUri);
        ReflectionTestUtils.setField(contractStorageService, "organizationsOrchestratorBaseUri", organizationsOrchestratorBaseUri);
        ReflectionTestUtils.setField(contractStorageService, "contractTemplateRepository", contractTemplateRepository);
        ReflectionTestUtils.setField(contractStorageService, "messageQueueService", messageQueueService);
        ReflectionTestUtils.setField(contractStorageService, "contractSignerService", contractSignerService);
        ReflectionTestUtils.setField(messageQueueService, "rabbitTemplate", rabbitTemplate);

        template1 = new SaasContractTemplate();
        template1.setConsumerId("Participant:10");
        template1.setProviderId("Participant:20");
        template1.setOfferingId("ServiceOffering:1234");
        template1.setOfferingName("HelloWorld");
        template1.setProviderTncUrl("http://example.com/");
        contractTemplateRepository.save(template1);

        template2 = new DataDeliveryContractTemplate();
        template2.setConsumerId("Participant:20");
        template2.setProviderId("Participant:10");
        template2.setOfferingId("ServiceOffering:2345");
        template2.setOfferingName("HelloWorld2");
        template2.setProviderTncUrl("http://example.com/");
        contractTemplateRepository.save(template2);

        this.defaultPageRequest = PageRequest.of(0, 9, Sort.by("creationDate").descending());

    }

    @BeforeEach
    public void beforeEach() {
        String userCountOption = """
                ,"userCountOption": [
                                        {
                                            "userCountUpTo": 0,
                                            "userCountUnlimited": true
                                        }
                                    ]
                """;

        String exchangeCountOption = """
                ,"exchangeCountOption": [
                                        {
                                            "exchangeCountUpTo": 0,
                                            "exchangeCountUnlimited": true
                                        }
                                    ]
                """;

        lenient().when(restTemplate.exchange(eq(serviceOfferingOrchestratorBaseUri + "/serviceoffering/"
                                + "ServiceOffering:4321"),
                        eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(
                        createServiceOfferingOrchestratorResponse(
                                "ServiceOffering:4321",
                                "4321",
                                "OfferingName",
                                "Participant:40",
                                "merlot:MerlotServiceOfferingSaaS",
                                userCountOption), HttpStatus.OK));

        lenient().when(restTemplate.exchange(eq(serviceOfferingOrchestratorBaseUri + "/serviceoffering/"
                                + template1.getOfferingId()),
                        eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(
                        createServiceOfferingOrchestratorResponse(
                                template1.getOfferingId(),
                                "1234",
                                template1.getOfferingName(),
                                template1.getProviderId(),
                                "merlot:MerlotServiceOfferingSaaS",
                                userCountOption), HttpStatus.OK));

        lenient().when(restTemplate.exchange(eq(serviceOfferingOrchestratorBaseUri + "/serviceoffering/"
                                + template2.getOfferingId()),
                        eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(
                        createServiceOfferingOrchestratorResponse(
                                template2.getOfferingId(),
                                "2345",
                                template2.getOfferingName(),
                                template2.getProviderId(),
                                "merlot:MerlotServiceOfferingDataDelivery",
                                exchangeCountOption), HttpStatus.OK));

        String organizationOrchestratorResponse = createOrganizationsOrchestratorResponse("40");
        lenient().when(restTemplate.exchange(
                        startsWith(organizationsOrchestratorBaseUri + "/organization/"),
                        eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(organizationOrchestratorResponse, HttpStatus.OK));
    }

    @Test
    void getOrganizationContractsExisting() {
        Page<ContractTemplate> contracts = contractStorageService.getOrganizationContracts("Participant:10",
                PageRequest.of(0, 9, Sort.by("creationDate").descending()));

        assertFalse(contracts.isEmpty());
    }

    @Test
    void getOrganizationContractsNonExisting() {
        Page<ContractTemplate> contracts = contractStorageService.getOrganizationContracts("Participant:99",
                PageRequest.of(0, 9, Sort.by("creationDate").descending()));

        assertTrue(contracts.isEmpty());
    }

    @Test
    void getOrganizationContractsInvalidOrgaId() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                contractStorageService.getOrganizationContracts("garbage", this.defaultPageRequest));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    void getContractByIdExistent() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add("10");
        ContractTemplate contract = contractStorageService.getContractDetails(template1.getId(), representedOrgaIds);

        assertEquals(template1.getConsumerId(), contract.getConsumerId());
    }

    @Test
    void getContractByIdNonExistent() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add("10");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.getContractDetails("Contract:1234", representedOrgaIds));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getContractByIdNotAllowed() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add("1234");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.getContractDetails(template1.getId(), representedOrgaIds));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void createContractTemplateSaasValidRequest() throws Exception {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId("Participant:40");
        request.setOfferingId(template1.getOfferingId());
        ContractTemplate contract = contractStorageService.addContractTemplate(request, "token");

        assertEquals(template1.getProviderId(), contract.getProviderId());
        assertEquals(template1.getOfferingName(), contract.getOfferingName());
    }

    @Test
    void createContractTemplateDataDeliveryValidRequest() throws Exception {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId("Participant:40");
        request.setOfferingId(template2.getOfferingId());
        ContractTemplate contract = contractStorageService.addContractTemplate(request, "token");

        assertEquals(template2.getProviderId(), contract.getProviderId());
        assertEquals(template2.getOfferingName(), contract.getOfferingName());
    }

    @Test
    void createContractTemplateInvalidConsumerIsProvider() throws Exception {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId(template1.getProviderId());
        request.setOfferingId(template1.getOfferingId());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.addContractTemplate(request, "token"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    void createContractTemplateInvalidConsumerId() {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId("garbage");
        request.setOfferingId("ServiceOffering:4321");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.addContractTemplate(request, "token"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    void createContractTemplateInvalidOfferingId() {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId("Participant:10");
        request.setOfferingId("garbage");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.addContractTemplate(request, "token"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    @Transactional
    void updateContractExistingAllowedAsConsumer() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template1.getConsumerId().replace("Participant:", ""));
        SaasContractTemplate template = new SaasContractTemplate(template1, false);

        template.setConsumerMerlotTncAccepted(true);
        template.setConsumerOfferingTncAccepted(true);
        template.setConsumerProviderTncAccepted(true);
        template.setRuntimeSelection("unlimited");
        ContractTemplate result = contractStorageService.updateContractTemplate(template, "token",
                representedOrgaIds.iterator().next(), representedOrgaIds);

        assertEquals(template.isConsumerMerlotTncAccepted(), result.isConsumerMerlotTncAccepted());
        assertEquals(template.isConsumerOfferingTncAccepted(), result.isConsumerOfferingTncAccepted());
        assertEquals(template.isConsumerProviderTncAccepted(), result.isConsumerProviderTncAccepted());
        assertEquals(template.getRuntimeSelection(), result.getRuntimeSelection());
        assertInstanceOf(SaasContractTemplate.class, result);
    }

    @Test
    @Transactional
    void updateContractExistingAllowedAsConsumerSaas() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template1.getConsumerId().replace("Participant:", ""));
        SaasContractTemplate template = new SaasContractTemplate(template1, false);

        template.setUserCountSelection("unlimited");
        SaasContractTemplate result = (SaasContractTemplate) contractStorageService
                .updateContractTemplate(template, "token",
                        representedOrgaIds.iterator().next(), representedOrgaIds);

        assertEquals(template.getUserCountSelection(), result.getUserCountSelection());
    }

    @Test
    @Transactional
    void updateContractExistingAllowedAsConsumerDataDelivery() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template2.getConsumerId().replace("Participant:", ""));
        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(template2, false);

        template.setExchangeCountSelection("unlimited");
        DataDeliveryContractTemplate result = (DataDeliveryContractTemplate) contractStorageService
                .updateContractTemplate(template, "token",
                        representedOrgaIds.iterator().next(), representedOrgaIds);

        assertEquals(template.getExchangeCountSelection(), result.getExchangeCountSelection());
    }

    @Test
    @Transactional
    void updateContractExistingAllowedAsProvider() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template1.getProviderId().replace("Participant:", ""));
        SaasContractTemplate template = new SaasContractTemplate(template1, false);

        template.setProviderMerlotTncAccepted(true);
        template.setAdditionalAgreements("agreement");
        List<String> attachments = new ArrayList<>();
        attachments.add("attachment1");
        template.setOfferingAttachments(attachments);
        ContractTemplate result = contractStorageService.updateContractTemplate(template, "token",
                representedOrgaIds.iterator().next(), representedOrgaIds);
        assertEquals(template.isProviderMerlotTncAccepted(), result.isProviderMerlotTncAccepted());
        assertEquals(template.getAdditionalAgreements(), result.getAdditionalAgreements());
        assertEquals(template.getOfferingAttachments(), result.getOfferingAttachments());
    }

    @Test
    @Transactional
    void updateContractNonExistent() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template1.getProviderId().replace("Participant:", ""));
        SaasContractTemplate template = new SaasContractTemplate();
        String activeRoleOrgaId = representedOrgaIds.iterator().next();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.updateContractTemplate(template, "token",
                        activeRoleOrgaId, representedOrgaIds));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @Transactional
    void updateContractNotAuthorized() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add("1234");
        SaasContractTemplate template = new SaasContractTemplate(template1, false);
        String activeRoleOrgaId = representedOrgaIds.iterator().next();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.updateContractTemplate(template, "token",
                        activeRoleOrgaId, representedOrgaIds));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    private void assertUpdateThrowsUnprocessableEntity(ContractTemplate template, String token,
                                                       String activeRoleOrgaId, Set<String> representedOrgaIds) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.updateContractTemplate(template, token,
                        activeRoleOrgaId, representedOrgaIds));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    @Transactional
    void updateContractModifyImmutableBaseFields() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        String provider = template1.getProviderId().replace("Participant:", "");
        String consumer = template1.getConsumerId().replace("Participant:", "");
        representedOrgaIds.add(provider);
        representedOrgaIds.add(consumer);

        SaasContractTemplate template = new SaasContractTemplate(template1, false);
        template.setOfferingId("ServiceOffering:9999");
        template.setOfferingName("garbage");
        template.setConsumerId("Participant:99");
        template.setProviderId("Participant:99");
        template.setProviderTncUrl("garbage");
        ContractTemplate result = contractStorageService.updateContractTemplate(template, "token",
                provider, representedOrgaIds);
        assertNotEquals(template.getOfferingId(), result.getOfferingId());
        assertEquals(template1.getOfferingId(), result.getOfferingId());
        assertNotEquals(template.getOfferingName(), result.getOfferingName());
        assertEquals(template1.getOfferingName(), result.getOfferingName());
        assertNotEquals(template.getConsumerId(), result.getConsumerId());
        assertEquals(template1.getConsumerId(), result.getConsumerId());
        assertNotEquals(template.getProviderId(), result.getProviderId());
        assertEquals(template1.getProviderId(), result.getProviderId());
        assertNotEquals(template.getProviderTncUrl(), result.getProviderTncUrl());
        assertEquals(template1.getProviderTncUrl(), result.getProviderTncUrl());
    }

    @Test
    @Transactional
    void updateContractModifyForbiddenFieldsAsProvider() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        String provider = template1.getProviderId().replace("Participant:", "");
        representedOrgaIds.add(provider);

        SaasContractTemplate template = new SaasContractTemplate(template1, false);
        template.setConsumerMerlotTncAccepted(true);
        template.setConsumerOfferingTncAccepted(true);
        template.setConsumerProviderTncAccepted(true);
        ContractTemplate result = contractStorageService.updateContractTemplate(template, "token",
                provider, representedOrgaIds);

        assertNotEquals(template.isConsumerMerlotTncAccepted(), result.isConsumerMerlotTncAccepted());
        assertEquals(template1.isConsumerMerlotTncAccepted(), result.isConsumerMerlotTncAccepted());
        assertNotEquals(template.isConsumerOfferingTncAccepted(), result.isConsumerOfferingTncAccepted());
        assertEquals(template1.isConsumerOfferingTncAccepted(), result.isConsumerOfferingTncAccepted());
        assertNotEquals(template.isConsumerProviderTncAccepted(), result.isConsumerProviderTncAccepted());
        assertEquals(template1.isConsumerProviderTncAccepted(), result.isConsumerProviderTncAccepted());
    }

    @Test
    @Transactional
    void updateContractModifyForbiddenFieldsAsConsumer() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = template2.getConsumerId().replace("Participant:", "");
        representedOrgaIds.add(consumer);

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(template2, false);
        template.setProviderMerlotTncAccepted(true);
        template.setAdditionalAgreements("garbage");
        List<String> attachments = new ArrayList<>();
        attachments.add("attachment1");
        template.setOfferingAttachments(attachments);
        // TODO provisioning fields

        ContractTemplate result = contractStorageService.updateContractTemplate(template, "token",
                consumer, representedOrgaIds);
        assertNotEquals(template.isProviderMerlotTncAccepted(), result.isProviderMerlotTncAccepted());
        assertEquals(template2.isProviderMerlotTncAccepted(), result.isProviderMerlotTncAccepted());
        assertNotEquals(template.getAdditionalAgreements(), result.getAdditionalAgreements());
        assertEquals(template2.getAdditionalAgreements(), result.getAdditionalAgreements());
        assertNotEquals(template.getOfferingAttachments(), result.getOfferingAttachments());
        assertEquals(template2.getOfferingAttachments(), result.getOfferingAttachments());

    }

    @Test
    @Transactional
    void updateContractSetInvalidSelection() {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = template1.getConsumerId().replace("Participant:", "");
        representedOrgaIds.add(consumer);

        SaasContractTemplate template = new SaasContractTemplate(template1, false);
        template.setRuntimeSelection("garbage");
        assertUpdateThrowsUnprocessableEntity(template, "token", consumer, representedOrgaIds);
    }

    @Test
    @Transactional
    void updateContractSetInvalidSelectionSaas() {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = template1.getConsumerId().replace("Participant:", "");
        representedOrgaIds.add(consumer);

        SaasContractTemplate template = new SaasContractTemplate(template1, false);
        template.setUserCountSelection("garbage");
        assertUpdateThrowsUnprocessableEntity(template, "token", consumer, representedOrgaIds);
    }

    @Test
    @Transactional
    void updateContractSetInvalidSelectionDataDelivery() {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = template1.getConsumerId().replace("Participant:", "");
        representedOrgaIds.add(consumer);

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(template2, false);
        template.setExchangeCountSelection("garbage");
        assertUpdateThrowsUnprocessableEntity(template, "token", consumer, representedOrgaIds);
    }

    private void assertTransitionThrowsForbidden(ContractTemplate template, ContractState state,
                                                 String activeRoleOrgaId) {
        String templateId = template.getId();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.transitionContractTemplateState(templateId,
                        state, activeRoleOrgaId, "userId"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @Transactional
    void transitionDataDeliveryConsumerIncompleteToComplete() {
        String consumer = template2.getConsumerId().replace("Participant:", "");

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(template2, false);
        template.setServiceContractProvisioning(new DataDeliveryProvisioning()); // reset provisioning
        contractTemplateRepository.save(template);

        DataDeliveryProvisioning provisioning =
                (DataDeliveryProvisioning) template.getServiceContractProvisioning();

        assertTransitionThrowsForbidden(template, ContractState.SIGNED_CONSUMER, consumer);
        template.setConsumerMerlotTncAccepted(true);
        contractTemplateRepository.save(template);
        assertTransitionThrowsForbidden(template, ContractState.SIGNED_CONSUMER, consumer);

        template.setConsumerProviderTncAccepted(true);
        contractTemplateRepository.save(template);
        assertTransitionThrowsForbidden(template, ContractState.SIGNED_CONSUMER, consumer);

        template.setConsumerOfferingTncAccepted(true);
        contractTemplateRepository.save(template);
        assertTransitionThrowsForbidden(template, ContractState.SIGNED_CONSUMER, consumer);

        template.setExchangeCountSelection("unlimited");
        contractTemplateRepository.save(template);
        assertTransitionThrowsForbidden(template, ContractState.SIGNED_CONSUMER, consumer);

        template.setRuntimeSelection("unlimited");
        contractTemplateRepository.save(template);
        assertTransitionThrowsForbidden(template, ContractState.SIGNED_CONSUMER, consumer);

        provisioning.setDataAddressTargetFileName("MyFile.json");
        contractTemplateRepository.save(template);
        assertTransitionThrowsForbidden(template, ContractState.SIGNED_CONSUMER, consumer);

        provisioning.setDataAddressTargetBucketName("MyBucket");
        contractTemplateRepository.save(template);
        ContractTemplate result = contractStorageService.transitionContractTemplateState(template.getId(),
                ContractState.SIGNED_CONSUMER, consumer, "userId");
        assertEquals(ContractState.SIGNED_CONSUMER, result.getState());
    }

    @Test
    @Transactional
    void transitionDataDeliveryProviderIncompleteToComplete() {
        String consumer = template2.getConsumerId().replace("Participant:", "");
        String provider = template2.getProviderId().replace("Participant:", "");

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(template2, false);
        template.setServiceContractProvisioning(new DataDeliveryProvisioning()); // reset provisioning
        contractTemplateRepository.save(template);

        DataDeliveryProvisioning provisioning =
                (DataDeliveryProvisioning) template.getServiceContractProvisioning();

        String templateId = template.getId();
        template.setExchangeCountSelection("unlimited");
        template.setRuntimeSelection("unlimited");
        template.setConsumerMerlotTncAccepted(true);
        template.setConsumerProviderTncAccepted(true);
        template.setConsumerOfferingTncAccepted(true);
        provisioning.setDataAddressTargetFileName("MyFile.json");
        provisioning.setDataAddressTargetBucketName("MyBucket");
        contractTemplateRepository.save(template);

        ContractTemplate result = contractStorageService.transitionContractTemplateState(template.getId(),
                ContractState.SIGNED_CONSUMER, consumer, "consumerUserId");
        assertEquals(ContractState.SIGNED_CONSUMER, result.getState());
        assertEquals("consumerUserId", result.getConsumerSignerUserId());

        template = (DataDeliveryContractTemplate) contractTemplateRepository.findById(templateId).orElse(null);
        assertNotNull(template);

        assertTransitionThrowsForbidden(template, ContractState.RELEASED, provider);

        template.setProviderMerlotTncAccepted(true);
        contractTemplateRepository.save(template);
        assertTransitionThrowsForbidden(template, ContractState.RELEASED, provider);

        provisioning = (DataDeliveryProvisioning) template.getServiceContractProvisioning();
        provisioning.setDataAddressName("MyFile.json");
        contractTemplateRepository.save(template);
        assertTransitionThrowsForbidden(template, ContractState.RELEASED, provider);

        provisioning.setDataAddressType("IonosS3");
        contractTemplateRepository.save(template);
        assertTransitionThrowsForbidden(template, ContractState.RELEASED, provider);

        provisioning.setDataAddressSourceBucketName("MyBucket2");
        contractTemplateRepository.save(template);
        assertTransitionThrowsForbidden(template, ContractState.RELEASED, provider);

        provisioning.setDataAddressSourceFileName("MyFile2..json");
        contractTemplateRepository.save(template);

        result = contractStorageService.transitionContractTemplateState(result.getId(),
                ContractState.RELEASED, provider, "providerUserId");
        assertEquals(ContractState.RELEASED, result.getState());
        assertEquals("providerUserId", result.getProviderSignerUserId());
    }

    @Test
    @Transactional
    void transitionContractNotAuthorized() {
        SaasContractTemplate template = new SaasContractTemplate(template1, false);
        String templateId = template.getId();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.transitionContractTemplateState(templateId,
                        ContractState.SIGNED_CONSUMER, "99", "consumerUserId"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @Transactional
    void transitionContractProviderNotAllowed() {
        String provider = template1.getProviderId().replace("Participant:", "");

        SaasContractTemplate template = new SaasContractTemplate(template1, false);
        String templateId = template.getId();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.transitionContractTemplateState(templateId,
                        ContractState.SIGNED_CONSUMER, provider, "providerUserId"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @Transactional
    void transitionContractConsumerNotAllowed() {
        String consumer = template2.getConsumerId().replace("Participant:", "");

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(template2, false);
        DataDeliveryProvisioning provisioning =
                (DataDeliveryProvisioning) template.getServiceContractProvisioning();
        String templateId = template.getId();
        template.setExchangeCountSelection("unlimited");
        template.setRuntimeSelection("unlimited");
        template.setConsumerMerlotTncAccepted(true);
        template.setConsumerProviderTncAccepted(true);
        template.setConsumerOfferingTncAccepted(true);
        provisioning.setDataAddressTargetFileName("MyFile.json");
        provisioning.setDataAddressTargetBucketName("MyBucket");
        contractTemplateRepository.save(template);
        contractStorageService.transitionContractTemplateState(templateId,
                ContractState.SIGNED_CONSUMER, consumer, "consumerUserId");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.transitionContractTemplateState(templateId,
                        ContractState.RELEASED, consumer, "consumerUserId"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @Transactional
    void updateDataDeliveryFieldsAfterTransition() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = template2.getConsumerId().replace("Participant:", "");
        String provider = template2.getProviderId().replace("Participant:", "");
        representedOrgaIds.add(consumer);
        representedOrgaIds.add(provider);
        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(template2, false);
        template.setServiceContractProvisioning(new DataDeliveryProvisioning());
        DataDeliveryProvisioning provisioning =
                (DataDeliveryProvisioning) template.getServiceContractProvisioning();

        template.setConsumerMerlotTncAccepted(true);
        template.setConsumerProviderTncAccepted(true);
        template.setConsumerOfferingTncAccepted(true);
        template.setExchangeCountSelection("unlimited");
        template.setRuntimeSelection("5 day(s)");
        provisioning.setDataAddressTargetBucketName("MyBucket");
        provisioning.setDataAddressTargetFileName("MyFile.json");

        DataDeliveryContractTemplate result = (DataDeliveryContractTemplate) contractStorageService
                .updateContractTemplate(template, "token",
                        representedOrgaIds.iterator().next(), representedOrgaIds);
        DataDeliveryProvisioning resultProvisioning = (DataDeliveryProvisioning) result.getServiceContractProvisioning();

        assertEquals(template.isConsumerMerlotTncAccepted(), result.isConsumerMerlotTncAccepted());
        assertEquals(template.isConsumerOfferingTncAccepted(), result.isConsumerOfferingTncAccepted());
        assertEquals(template.isConsumerProviderTncAccepted(), result.isConsumerProviderTncAccepted());
        assertEquals(template.getExchangeCountSelection(), result.getExchangeCountSelection());
        assertEquals(template.getRuntimeSelection(), result.getRuntimeSelection());
        assertEquals(provisioning.getDataAddressTargetBucketName(), resultProvisioning.getDataAddressTargetBucketName());
        assertEquals(provisioning.getDataAddressTargetFileName(), resultProvisioning.getDataAddressTargetFileName());

        result = (DataDeliveryContractTemplate) contractStorageService.transitionContractTemplateState(result.getId(),
                ContractState.SIGNED_CONSUMER, consumer, "userId");
        resultProvisioning = (DataDeliveryProvisioning) result.getServiceContractProvisioning();

        assertFalse(StringUtil.isNullOrEmpty(result.getConsumerSignerUserId()));
        assertFalse(StringUtil.isNullOrEmpty(result.getConsumerSignature()));
        assertTrue(StringUtil.isNullOrEmpty(result.getProviderSignerUserId()));
        assertTrue(StringUtil.isNullOrEmpty(result.getProviderSignature()));

        result.setProviderMerlotTncAccepted(true);
        resultProvisioning.setDataAddressName("MyFile.json");
        resultProvisioning.setDataAddressType("IonosS3");
        resultProvisioning.setDataAddressSourceBucketName("MyBucket2");
        resultProvisioning.setDataAddressSourceFileName("MyFile2..json");

        DataDeliveryContractTemplate result2 = (DataDeliveryContractTemplate) contractStorageService
                .updateContractTemplate(result, "token",
                        representedOrgaIds.iterator().next(), representedOrgaIds);
        DataDeliveryProvisioning result2Provisioning = (DataDeliveryProvisioning) result2.getServiceContractProvisioning();

        assertEquals(result.isProviderMerlotTncAccepted(), result2.isProviderMerlotTncAccepted());
        assertEquals(resultProvisioning.getDataAddressType(), result2Provisioning.getDataAddressType());
        assertEquals(resultProvisioning.getDataAddressName(), result2Provisioning.getDataAddressName());
        assertEquals(resultProvisioning.getDataAddressSourceFileName(), result2Provisioning.getDataAddressSourceFileName());
        assertEquals(resultProvisioning.getDataAddressSourceBucketName(), result2Provisioning.getDataAddressSourceBucketName());

        result = (DataDeliveryContractTemplate) contractStorageService.transitionContractTemplateState(result.getId(),
                ContractState.RELEASED, provider, "userId");

        assertNotNull(result.getServiceContractProvisioning().getValidUntil());
        assertFalse(StringUtil.isNullOrEmpty(result.getConsumerSignerUserId()));
        assertFalse(StringUtil.isNullOrEmpty(result.getConsumerSignature()));
        assertFalse(StringUtil.isNullOrEmpty(result.getProviderSignerUserId()));
        assertFalse(StringUtil.isNullOrEmpty(result.getProviderSignature()));

    }
}
