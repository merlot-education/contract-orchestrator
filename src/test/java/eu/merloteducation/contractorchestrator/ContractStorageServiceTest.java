package eu.merloteducation.contractorchestrator;

import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import eu.merloteducation.contractorchestrator.service.ContractStorageService;
import org.apache.commons.text.StringSubstitutor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @Value("${serviceoffering-orchestrator.base-uri}")
    private String serviceOfferingOrchestratorBaseUri;

    @Value("${organizations-orchestrator.base-uri}")
    private String organizationsOrchestratorBaseUri;

    @Autowired
    private ContractTemplateRepository contractTemplateRepository;

    @InjectMocks
    private ContractStorageService contractStorageService;

    private PageRequest defaultPageRequest;

    private ContractTemplate template1;
    private ContractTemplate template2;

    private String createServiceOfferingOrchestratorResponse(String id, String hash, String name, String offeredBy) {
        String response = """
                {
                    "id": "${id}",
                    "sdHash": "${hash}",
                    "name": "${name}",
                    "creationDate": null,
                    "offeredBy": "${offeredBy}",
                    "merlotState": "RELEASED",
                    "type": "merlot:MerlotServiceOfferingSaaS",
                    "description": null,
                    "modifiedDate": "2023-05-26T09:55:46.189505Z",
                    "dataAccessType": "Download",
                    "exampleCosts": null,
                    "attachments": null,
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
                    "hardwareRequirements": null,
                    "userCountOption": [
                        {
                            "userCountUpTo": 0,
                            "userCountUnlimited": true
                        }
                    ],
                    "exchangeCountOption": null
                }
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("name", name);
        params.put("hash", hash);
        params.put("offeredBy", offeredBy);
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

        template1 = new ContractTemplate();
        template1.setConsumerId("Participant:10");
        template1.setProviderId("Participant:20");
        template1.setOfferingId("ServiceOffering:1234");
        template1.setOfferingName("HelloWorld");
        template1.setOfferingAttachments(new ArrayList<>());
        template1.setProviderTncUrl("http://example.com/");
        contractTemplateRepository.save(template1);

        template2 = new ContractTemplate();
        template2.setConsumerId("Participant:20");
        template2.setProviderId("Participant:10");
        template2.setOfferingId("ServiceOffering:2345");
        template2.setOfferingName("HelloWorld2");
        template2.setOfferingAttachments(new ArrayList<>());
        template2.setProviderTncUrl("http://example.com/");
        contractTemplateRepository.save(template2);

        this.defaultPageRequest = PageRequest.of(0, 9, Sort.by("creationDate").descending());

    }

    @BeforeEach
    public void beforeEach() {
        String serviceOfferingOrchestratorResponse =
                createServiceOfferingOrchestratorResponse("ServiceOffering:4321", "4321",
                        "OfferingName", "Participant:40");
        lenient().when(restTemplate.exchange(eq(serviceOfferingOrchestratorBaseUri + "serviceoffering/" + "ServiceOffering:4321"),
                eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(serviceOfferingOrchestratorResponse, HttpStatus.OK));

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
        representedOrgaIds.add("Participant:10");
        ContractTemplate contract = contractStorageService.getContractDetails(template1.getId(), representedOrgaIds);

        assertEquals(template1.getConsumerId(), contract.getConsumerId());
    }

    @Test
    void getContractByIdNonExistent() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add("Participant:10");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.getContractDetails("Contract:1234", representedOrgaIds));

        assertEquals(HttpStatus.NOT_FOUND ,ex.getStatusCode());
    }

    @Test
    void getContractByIdNotAllowed() {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add("Participant:1234");
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
    void createContractTemplateInvalidConsumerId() throws Exception {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId("garbage");
        request.setOfferingId("ServiceOffering:4321");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.addContractTemplate(request, "token"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY ,ex.getStatusCode());
    }

    @Test
    void createContractTemplateInvalidOfferingId() throws Exception {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId("Participant:10");
        request.setOfferingId("garbage");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.addContractTemplate(request, "token"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY ,ex.getStatusCode());
    }
}
