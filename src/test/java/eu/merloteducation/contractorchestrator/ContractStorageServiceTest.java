package eu.merloteducation.contractorchestrator;

import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.DataDeliveryContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.SaasContractTemplate;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import eu.merloteducation.contractorchestrator.service.ContractStorageService;
import eu.merloteducation.contractorchestrator.service.MessageQueueService;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;


@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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

        lenient().when(restTemplate.exchange(eq(serviceOfferingOrchestratorBaseUri + "serviceoffering/"
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

        lenient().when(restTemplate.exchange(eq(serviceOfferingOrchestratorBaseUri + "serviceoffering/"
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

        lenient().when(restTemplate.exchange(eq(serviceOfferingOrchestratorBaseUri + "serviceoffering/"
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
                eq(organizationsOrchestratorBaseUri + "organization/40"),
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

        assertEquals(HttpStatus.NOT_FOUND ,ex.getStatusCode());
    }

    @Test
    void getContractByIdNotAllowed() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add("1234");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.getContractDetails(template1.getId(), representedOrgaIds));

        assertEquals(HttpStatus.FORBIDDEN ,ex.getStatusCode());
    }

    @Test
    void createContractTemplateValidRequest() throws Exception {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId("Participant:10");
        request.setOfferingId("ServiceOffering:4321");
        ContractTemplate contract = contractStorageService.addContractTemplate(request, "token");

        assertEquals("Participant:40", contract.getProviderId());
        assertEquals("OfferingName", contract.getOfferingName());
    }

    @Test
    void createContractTemplateInvalidConsumerId() {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId("garbage");
        request.setOfferingId("ServiceOffering:4321");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.addContractTemplate(request, "token"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY ,ex.getStatusCode());
    }

    @Test
    void createContractTemplateInvalidOfferingId() {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId("Participant:10");
        request.setOfferingId("garbage");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.addContractTemplate(request, "token"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY ,ex.getStatusCode());
    }

    @Test
    @Transactional
    void updateContractExistingAllowedAsConsumer() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template1.getConsumerId().replace("Participant:", ""));
        SaasContractTemplate template = new SaasContractTemplate(template1);

        template.setConsumerMerlotTncAccepted(true);
        template.setConsumerOfferingTncAccepted(true);
        template.setConsumerProviderTncAccepted(true);
        template.setRuntimeSelection("unlimited");
        template.setUserCountSelection("unlimited");
        ContractTemplate result = contractStorageService.updateContractTemplate(template, "token", representedOrgaIds);

        assertEquals(template.isConsumerMerlotTncAccepted(), result.isConsumerMerlotTncAccepted());
        assertEquals(template.isConsumerOfferingTncAccepted(), result.isConsumerOfferingTncAccepted());
        assertEquals(template.isConsumerProviderTncAccepted(), result.isConsumerProviderTncAccepted());
        assertEquals(template.getRuntimeSelection(), result.getRuntimeSelection());
        assertInstanceOf(SaasContractTemplate.class, result);
        assertEquals(template.getRuntimeSelection(), result.getRuntimeSelection());
    }

    @Test
    @Transactional
    void updateContractExistingAllowedAsConsumerSaas() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template1.getConsumerId().replace("Participant:", ""));
        SaasContractTemplate template = new SaasContractTemplate(template1);

        template.setUserCountSelection("unlimited");
        SaasContractTemplate result = (SaasContractTemplate) contractStorageService.updateContractTemplate(
                template, "token", representedOrgaIds);

        assertEquals(template.getUserCountSelection(), result.getUserCountSelection());
    }

    @Test
    @Transactional
    void updateContractExistingAllowedAsConsumerDataDelivery() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template2.getConsumerId().replace("Participant:", ""));
        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(template2);

        template.setExchangeCountSelection("unlimited");
        DataDeliveryContractTemplate result = (DataDeliveryContractTemplate) contractStorageService.updateContractTemplate(
                template, "token", representedOrgaIds);

        assertEquals(template.getExchangeCountSelection(), result.getExchangeCountSelection());
    }

    @Test
    @Transactional
    void updateContractExistingAllowedAsProvider() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template1.getProviderId().replace("Participant:", ""));
        SaasContractTemplate template = new SaasContractTemplate(template1);

        template.setProviderMerlotTncAccepted(true);
        template.setAdditionalAgreements("agreement");
        template.addAttachment("attachment1");
        ContractTemplate result = contractStorageService.updateContractTemplate(template, "token", representedOrgaIds);
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

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.updateContractTemplate(template, "token", representedOrgaIds));
        assertEquals(HttpStatus.NOT_FOUND ,ex.getStatusCode());
    }

    @Test
    @Transactional
    void updateContractNotAuthorized() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add("1234");
        SaasContractTemplate template = new SaasContractTemplate(template1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.updateContractTemplate(template, "token", representedOrgaIds));
        assertEquals(HttpStatus.FORBIDDEN ,ex.getStatusCode());
    }

    private void assertUpdateThrowsUnprocessableEntity(ContractTemplate template, String token, Set<String> representedOrgaIds) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.updateContractTemplate(template, token, representedOrgaIds));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY ,ex.getStatusCode());
    }
    @Test
    @Transactional
    void updateContractModifyImmutableBaseFields() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template1.getProviderId().replace("Participant:", ""));
        representedOrgaIds.add(template1.getConsumerId().replace("Participant:", ""));

        SaasContractTemplate template = new SaasContractTemplate(template1);
        template.setOfferingId("ServiceOffering:9999");
        assertUpdateThrowsUnprocessableEntity(template, "token", representedOrgaIds);

        template = new SaasContractTemplate(template1);
        template.setOfferingName("garbage");
        assertUpdateThrowsUnprocessableEntity(template, "token", representedOrgaIds);

        template = new SaasContractTemplate(template1);
        template.setConsumerId("Participant:99");
        assertUpdateThrowsUnprocessableEntity(template, "token", representedOrgaIds);

        template = new SaasContractTemplate(template1);
        template.setProviderId("Participant:99");
        assertUpdateThrowsUnprocessableEntity(template, "token", representedOrgaIds);

        template = new SaasContractTemplate(template1);
        template.setProviderTncUrl("garbage");
        assertUpdateThrowsUnprocessableEntity(template, "token", representedOrgaIds);
    }

    @Test
    @Transactional
    void updateContractModifyForbiddenFieldsAsProvider() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template1.getProviderId().replace("Participant:", ""));

        SaasContractTemplate template = new SaasContractTemplate(template1);
        template.setConsumerMerlotTncAccepted(true);
        assertUpdateThrowsUnprocessableEntity(template, "token", representedOrgaIds);

        template = new SaasContractTemplate(template1);
        template.setConsumerOfferingTncAccepted(true);
        assertUpdateThrowsUnprocessableEntity(template, "token", representedOrgaIds);

        template = new SaasContractTemplate(template1);
        template.setConsumerProviderTncAccepted(true);
        assertUpdateThrowsUnprocessableEntity(template, "token", representedOrgaIds);
    }

    @Test
    @Transactional
    void updateContractModifyForbiddenFieldsAsConsumer() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template1.getConsumerId().replace("Participant:", ""));

        SaasContractTemplate template = new SaasContractTemplate(template1);
        template.setProviderMerlotTncAccepted(true);
        assertUpdateThrowsUnprocessableEntity(template, "token", representedOrgaIds);

        template = new SaasContractTemplate(template1);
        template.setAdditionalAgreements("garbage");
        assertUpdateThrowsUnprocessableEntity(template, "token", representedOrgaIds);

        template = new SaasContractTemplate(template1);
        template.addAttachment("garbage");
        assertUpdateThrowsUnprocessableEntity(template, "token", representedOrgaIds);
    }

    @Test
    @Transactional
    void updateContractSetInvalidSelection() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template1.getConsumerId().replace("Participant:", ""));

        SaasContractTemplate template = new SaasContractTemplate(template1);
        template.setRuntimeSelection("garbage");
        assertUpdateThrowsUnprocessableEntity(template, "token", representedOrgaIds);
    }

    @Test
    @Transactional
    void updateContractSetInvalidSelectionSaas() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template1.getConsumerId().replace("Participant:", ""));

        SaasContractTemplate template = new SaasContractTemplate(template1);
        template.setUserCountSelection("garbage");
        assertUpdateThrowsUnprocessableEntity(template, "token", representedOrgaIds);
    }

    @Test
    @Transactional
    void updateContractSetInvalidSelectionDataDelivery() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template2.getConsumerId().replace("Participant:", ""));

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(template2);
        template.setExchangeCountSelection("garbage");
        assertUpdateThrowsUnprocessableEntity(template, "token", representedOrgaIds);
    }

    @Test
    @Transactional
    void transitionContractConsumerProviderSign() {
        Set<String> consumer = new HashSet<>();
        consumer.add(template1.getConsumerId().replace("Participant:", ""));

        Set<String> provider = new HashSet<>();
        provider.add(template1.getProviderId().replace("Participant:", ""));

        SaasContractTemplate template = new SaasContractTemplate(template1);

        ContractTemplate result = contractStorageService.transitionContractTemplateState(template.getId(),
                ContractState.SIGNED_CONSUMER ,consumer);
        assertEquals(ContractState.SIGNED_CONSUMER, result.getState());

        result = contractStorageService.transitionContractTemplateState(result.getId(),
                ContractState.RELEASED ,provider);
        assertEquals(ContractState.RELEASED, result.getState());
    }

    @Test
    @Transactional
    void transitionContractNotAuthorized() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add("99");

        SaasContractTemplate template = new SaasContractTemplate(template1);
        String templateId = template.getId();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.transitionContractTemplateState(templateId,
                        ContractState.SIGNED_CONSUMER ,representedOrgaIds));
        assertEquals(HttpStatus.FORBIDDEN ,ex.getStatusCode());
    }

    @Test
    @Transactional
    void transitionContractProviderNotAllowed() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template1.getProviderId().replace("Participant:", ""));

        SaasContractTemplate template = new SaasContractTemplate(template1);
        String templateId = template.getId();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.transitionContractTemplateState(templateId,
                        ContractState.SIGNED_CONSUMER ,representedOrgaIds));
        assertEquals(HttpStatus.FORBIDDEN ,ex.getStatusCode());
    }

    @Test
    @Transactional
    void transitionContractConsumerNotAllowed() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(template1.getConsumerId().replace("Participant:", ""));

        SaasContractTemplate template = new SaasContractTemplate(template1);
        String templateId = template.getId();
        contractStorageService.transitionContractTemplateState(templateId,
                ContractState.SIGNED_CONSUMER ,representedOrgaIds);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.transitionContractTemplateState(templateId,
                        ContractState.RELEASED ,representedOrgaIds));
        assertEquals(HttpStatus.FORBIDDEN ,ex.getStatusCode());
    }
}
