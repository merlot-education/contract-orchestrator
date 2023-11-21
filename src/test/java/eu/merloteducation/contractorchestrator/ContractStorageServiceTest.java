package eu.merloteducation.contractorchestrator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.models.dto.ContractBasicDto;
import eu.merloteducation.contractorchestrator.models.dto.ContractDto;
import eu.merloteducation.contractorchestrator.models.dto.cooperation.CooperationContractDto;
import eu.merloteducation.contractorchestrator.models.dto.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.contractorchestrator.models.dto.saas.SaasContractDetailsDto;
import eu.merloteducation.contractorchestrator.models.dto.saas.SaasContractDto;
import eu.merloteducation.contractorchestrator.models.entities.*;
import eu.merloteducation.contractorchestrator.models.mappers.ContractMapper;
import eu.merloteducation.contractorchestrator.models.organisationsorchestrator.OrganizationDetails;
import eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator.ServiceOfferingDetails;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import eu.merloteducation.contractorchestrator.service.*;
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
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class ContractStorageServiceTest {

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Autowired
    private ContractSignerService contractSignerService;

    @InjectMocks
    private ContractStorageService contractStorageService;

    private PageRequest defaultPageRequest;

    private SaasContractTemplate saasContract;
    private DataDeliveryContractTemplate dataDeliveryContract;
    private CooperationContractTemplate coopContract;

    private String createServiceOfferingOrchestratorResponse(String id, String hash, String name, String offeredBy,
                                                             String offeringType, String typeSpecificFields) {
        String response = """
                {
                    "metadata": {
                        "state": "RELEASED",
                        "hash": "${hash}",
                        "creationDate": "2023-08-21T15:32:19.100661+02:00",
                        "modifiedDate": "2023-08-21T13:32:19.487564Z"
                    },
                    "providerDetails": {
                        "providerId": "${offeredBy}",
                        "providerLegalName": "MyProvider"
                    },
                    "selfDescription": {
                        "type": [
                            "VerifiablePresentation"
                        ],
                        "proof": {
                            "type": "JsonWebSignature2020",
                            "created": "2023-08-21T13:32:19Z",
                            "proofPurpose": "assertionMethod",
                            "verificationMethod": "did:web:compliance.lab.gaia-x.eu",
                            "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..sn4reXz8hO1EUDEPgwFV9rKwFztC9s4T6mfQyhhc8Mcpj0tZCslDYv4EPBY7wqx4viKMNFZoTRBv8NjbccXD90CUFNX_OvvFGJhtzRiXX7rtl29D0dJvMYn_HDIQ1n3bquqEdL4H3N3xxk6o0V2AKMfMp8KMMMllCm2h0t_KDbXvA-6u1WaFMCTTE5rij8-IyQV7e3UA3dz6ZKK8eWREHTGqCkRkdKDvyuBPH6lEaGkqm914OLYFiRiqGSSS0smDcZAetjgLIu2DeAqpt24es2fmQPXy1ewT6GWJS9rgwK_iEeBTZFUvMg45wDxp9dFwrVGAklOP4i9DY_wcT_sGocwwDsQI844HyuQrL0OKoM6vex57fzQh9JfY3srXYnPrsW--2yqXqOzJHfmjDp6M6iroonukknsc7Y35UG92omWB2d86dj_NnQhaKQJY7cvQTPFNF71_ziBM15PZ10tJixdASIzmOiawZIY1GlGhh8tKj_nzS6IbIenFXtBKo9XMs_fzUqzrx9I7EP7B95Oy3hqFV8qQA9EvE91gLkJmm7du4q3DRHt17Se5rZf2bcBPGp0FaFsWR2JPJR0N-NPCd5pliJoVo5MqnxcEgDyZo8nVz3WWfS1hM1oKiNP6-Oh0YEW9aCaEw9bCtBk0-DGBl0M46QNpFnMu8toideFVYcs"
                        },
                        "verifiableCredential": {
                            "issuer": "${offeredBy}",
                            "issuanceDate": "2023-08-21T13:32:19.102073393Z",
                            "proof": {
                                "type": "JsonWebSignature2020",
                                "created": "2023-08-21T13:32:19Z",
                                "proofPurpose": "assertionMethod",
                                "verificationMethod": "did:web:compliance.lab.gaia-x.eu",
                                "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..UAsRkuZgeWte1E4pBO0QcVeociUNUwgT-NOjCPmoAVJ2ghC9aqVp3I-UtX8yVKgSLwx6hod1dg1bX2Q5NbtRz6eTV8W5vNN0W-tLJBA713CwtxgEymve7phxv98UPQSFs5_P1eg3gcYL7vxj4uDNds4AFAzFWo-FqKpRhSZIsD84869pha9h7d533__IxBKCE4mOXwqKlySeZ6w_4w9p8rlCgAnGMDf7369AxB_gUaTApDkRItJzXowcX9tyEb-t7GtvWbvgSBM4SfugvPDCcP0ZP9lybyVyQv8LIRS4CSq_ohmjhwZWmnbOkOPm_usWe-rYGLBryBQILwZvqgX4r0kRQDALmHsOgqZUOqyBW_vw_lpLSM_Wo82avoZXao6HG3ZFoTnWjFrmh1XiBEVBWWBNkYLDEJlqkfhYO0xU0nroTmnD3UGJs-cKoS8jvJiBkQCqYYY-up-YWVoiaLz0b9B0j7Pc1KpyQh1egoEugo9u0S244gBsXWak30Pp6sTI_MUNga3Ybiqj5Ar-KC0Qu6noXS2RcAh9XQmRr4qPP36-tt1H0eoTFDWP8wGhsYkJF7rf-CLf7H2urGFX-4Y_K_RXjQgVJohWYSPjX6O5GYifa16o4iEuMCDhwRX9ekAnOGeTwQywOUicglf5oc5q1rFCCNKxY1386AZ3_LzR3F8"
                            },
                            "credentialSubject": {
                                "@id": "${id}",
                                "@type": "${offeringType}",
                                "@context": {
                                    "merlot": "http://w3id.org/gaia-x/merlot#",
                                    "dct": "http://purl.org/dc/terms/",
                                    "gax-trust-framework": "http://w3id.org/gaia-x/gax-trust-framework#",
                                    "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                                    "sh": "http://www.w3.org/ns/shacl#",
                                    "xsd": "http://www.w3.org/2001/XMLSchema#",
                                    "gax-validation": "http://w3id.org/gaia-x/validation#",
                                    "skos": "http://www.w3.org/2004/02/skos/core#",
                                    "dcat": "http://www.w3.org/ns/dcat#",
                                    "gax-core": "http://w3id.org/gaia-x/core#"
                                },
                                "gax-core:offeredBy": {
                                    "@id": "${offeredBy}"
                                },
                                "gax-trust-framework:name": {
                                    "@type": "xsd:string",
                                    "@value": "${name}"
                                },
                                "gax-trust-framework:termsAndConditions": [
                                    {
                                        "gax-trust-framework:content": {
                                            "@type": "xsd:anyURI",
                                            "@value": "asd"
                                        },
                                        "gax-trust-framework:hash": {
                                            "@type": "xsd:string",
                                            "@value": "asd"
                                        },
                                        "@type": "gax-trust-framework:TermsAndConditions"
                                    }
                                ],
                                "gax-trust-framework:policy": [
                                    {
                                        "@type": "xsd:string",
                                        "@value": "dummyPolicy"
                                    }
                                ],
                                "gax-trust-framework:dataAccountExport": [
                                    {
                                        "gax-trust-framework:formatType": {
                                            "@type": "xsd:string",
                                            "@value": "dummyValue"
                                        },
                                        "gax-trust-framework:accessType": {
                                            "@type": "xsd:string",
                                            "@value": "dummyValue"
                                        },
                                        "gax-trust-framework:requestType": {
                                            "@type": "xsd:string",
                                            "@value": "dummyValue"
                                        },
                                        "@type": "gax-trust-framework:DataAccountExport"
                                    }
                                ],
                                "gax-trust-framework:providedBy": {
                                    "@id": "${offeredBy}"
                                },
                                "merlot:creationDate": {
                                    "@type": "xsd:string",
                                    "@value": "2023-08-21T13:32:19.100660534Z"
                                },
                                "merlot:runtimeOption": [
                                    {
                                        "@type": null,
                                        "merlot:runtimeCount": {
                                            "@type": "xsd:number",
                                            "@value": 0
                                        },
                                        "merlot:runtimeMeasurement": {
                                            "@type": "xsd:string",
                                            "@value": "unlimited"
                                        }
                                    },
                                    {
                                        "@type": null,
                                        "merlot:runtimeCount": {
                                            "@type": "xsd:number",
                                            "@value": 4
                                        },
                                        "merlot:runtimeMeasurement": {
                                            "@type": "xsd:string",
                                            "@value": "day(s)"
                                        }
                                    }
                                ],
                                "merlot:merlotTermsAndConditionsAccepted": true
                                ${typeSpecificFields}
                            },
                            "@context": [
                                "https://www.w3.org/2018/credentials/v1"
                            ],
                            "@id": "https://www.example.org/ServiceOffering.json",
                            "@type": [
                                "VerifiableCredential"
                            ]
                        },
                        "@id": "http://example.edu/verifiablePresentation/self-description1",
                        "@context": [
                            "https://www.w3.org/2018/credentials/v1"
                        ]
                    }
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
                    "metadata": null,
                    "selfDescription": {
                        "proof": {
                            "created": "2023-08-21T09:18:08Z",
                            "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..C2JD8wNX1A4AOym4e_4WRoe7zHdtiRI6Va15y7e7BzpKWJz86WE1iYBQ-ZXQZEYSHBfJGBQIXA_dXGyidT5K6KBdcW6s2acekvU17oXOXWOxv6U6QsZQcBWOcEhXzLc_PSr0_ZaCVzEYx6iisxyniBmQ8-eQWKkGbgnJMBCeFbAGmiqLIF3nRjwi4cpkvfQCm0YQnq44_rdswcWJ07DfQyEeE0hRuZ4Ge7DFGUQG9eqsw4AJnEJfFHjebGY-SGvYRD_eusqmWd1RQCCujkhQDJzplTnOad3Lv_glOJQeyaY_1N-ITFsn8H_FKHkY5YiQ4cmA8v5WPthQY7IPbrKxp5dgEoVEvVykAB5MoDO8fCAolkk9EH2rF-6NZJT_uiM4mG0dNrCrZTgq1nVGO_G2VDVxflIyrsdj9M7NkCXjiTR3FQtMsn7kMH7WPlO86ArIp9ZtID5pjG7_vY_H5604kQHBSPRH4L-Ho01mFscNNl5qPjhV-tqFLaA9NBH7dlY1q1sLJL-w_u1nqYodzOxiLR-IUqin40qQiebr5Z4E4t0SXGdA_be1xY2PuQc9m1LQ7tGL-2z_iOxSzmaxE7JiZNITuvSS3tlgsLpyiN5sG8buArnn-EUDw8RSrT9S4dUgiVevPTqf0Ibu4jsE4_PEoGKjA7a3pbISYLEl-Pww2-g",
                            "proofPurpose": "assertionMethod",
                            "type": "JsonWebSignature2020",
                            "verificationMethod": "did:web:compliance.lab.gaia-x.eu"
                        },
                        "type": [
                            "VerifiablePresentation"
                        ],
                        "verifiableCredential": {
                            "credentialSubject": {
                                "@id": "Participant:${id}",
                                "@type": "merlot:MerlotOrganization",
                                "@context": {
                                    "merlot": "http://w3id.org/gaia-x/merlot#",
                                    "gax-trust-framework": "http://w3id.org/gaia-x/gax-trust-framework#",
                                    "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                                    "sh": "http://www.w3.org/ns/shacl#",
                                    "xsd": "http://www.w3.org/2001/XMLSchema#",
                                    "gax-validation": "http://w3id.org/gaia-x/validation#",
                                    "skos": "http://www.w3.org/2004/02/skos/core#",
                                    "vcard": "http://www.w3.org/2006/vcard/ns#"
                                },
                                "gax-trust-framework:legalName": {
                                    "@type": "xsd:string",
                                    "@value": "MyOrga"
                                },
                                "gax-trust-framework:legalForm": null,
                                "gax-trust-framework:description": null,
                                "gax-trust-framework:registrationNumber": {
                                    "@type": "gax-trust-framework:RegistrationNumber",
                                    "gax-trust-framework:local": {
                                        "@type": "xsd:string",
                                        "@value": "0110"
                                    }
                                },
                                "gax-trust-framework:legalAddress": {
                                    "@type": "vcard:Address",
                                    "vcard:country-name": {
                                        "@type": "xsd:string",
                                        "@value": "DE"
                                    },
                                    "vcard:street-address": {
                                        "@type": "xsd:string",
                                        "@value": "asdasd"
                                    },
                                    "vcard:locality": {
                                        "@type": "xsd:string",
                                        "@value": "Berlin"
                                    },
                                    "vcard:postal-code": {
                                        "@type": "xsd:string",
                                        "@value": "12345"
                                    }
                                },
                                "gax-trust-framework:headquarterAddress": {
                                    "@type": "vcard:Address",
                                    "vcard:country-name": {
                                        "@type": "xsd:string",
                                        "@value": "DE"
                                    },
                                    "vcard:street-address": {
                                        "@type": "xsd:string",
                                        "@value": "asdasd"
                                    },
                                    "vcard:locality": {
                                        "@type": "xsd:string",
                                        "@value": "Berlin"
                                    },
                                    "vcard:postal-code": {
                                        "@type": "xsd:string",
                                        "@value": "12345"
                                    }
                                },
                                "merlot:mailAddress": {
                                    "@type": "xsd:string",
                                    "@value": "mymail@example.com"
                                },
                                "merlot:orgaName": {
                                    "@type": "xsd:string",
                                    "@value": "MyOrga"
                                },
                                "merlot:merlotId": {
                                    "@type": "xsd:string",
                                    "@value": "${id}"
                                },
                                "merlot:addressCode": {
                                    "@type": "xsd:string",
                                    "@value": "DE-BER"
                                },
                                "merlot:termsAndConditions": {
                                    "gax-trust-framework:content": {
                                        "@type": "xsd:anyURI",
                                        "@value": "http://example.com"
                                    },
                                    "gax-trust-framework:hash": {
                                        "@type": "xsd:string",
                                        "@value": "hash1234"
                                    },
                                    "@type": "gax-trust-framework:TermsAndConditions"
                                }
                            },
                            "issuanceDate": "2022-10-19T18:48:09Z",
                            "proof": {
                                "created": "2023-08-21T09:18:08Z",
                                "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..EDmuZTeGy1tPG1dpvEtmDOQoVsoNZizqJjZIQ15iDGcZnE2IPsreJrJlJ25WwYDADcCHhjh-kUcxt8tqq5-ehGavZ6xGyvg6U4Y3QpzrGj8wdVhfcuNEyQMh0HkziVGhToBs6pkq7Aswjws6t0eUc4p5lHxdntn8WPDr1qOUiSfCq5HdSvX-Ib4JtSELSvCvv-v9ALBchtayztLaf6SVfyPJSotx4Nv178gjrAddux-PGBwDQJrTqkULzC6auMafe5daXfi2VRm9NbddOxRBa-scKkN8b6yp7qLijvxSiAazwdvyeSZA7uRAZBTlFjiKFB9Ly3061usN5wGe22Ikv7BBZU3OM6GHzGRYD4TefpVQOMkY1Qec1BzatH2SRd9a1mPtZI6bOYopV_hU7oYsy0wQwH4nj-EoDGtCfhMOpHt_U63eXRLeGnBYjk1q-vpy_UAfmzBo6qx_C0Avvf43JAhstiV85dwx0UC0PU5k89DSJ6bCMNqGTJgidaeHlon859bPx73eHP4ArRxT7EZqMPMFLHI3Q8mckiPXC4X2qz05BEwLb9zMFo9jr7o44O6P8NPGmk6B4z-5JEMFTCYcEbe4yOPqCYtIt13GeiIbLTlPm5DOpemk17imJ3GMtqIf50VWJyWE0KhR0xlihRRhZwERN_uCq0nn-bY64alDALo",
                                "proofPurpose": "assertionMethod",
                                "type": "JsonWebSignature2020",
                                "verificationMethod": "did:web:compliance.lab.gaia-x.eu"
                            },
                            "issuer": "Participant:${id}",
                            "@type": [
                                "VerifiableCredential"
                            ],
                            "@id": "https://www.example.org/legalPerson.json",
                            "@context": [
                                "https://www.w3.org/2018/credentials/v1"
                            ]
                        },
                        "@id": "http://example.edu/verifiablePresentation/self-description1",
                        "@context": [
                            "https://www.w3.org/2018/credentials/v1"
                        ]
                    }
                }
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        return StringSubstitutor.replace(response, params, "${", "}");
    }

    @BeforeAll
    public void setUp() {
        ReflectionTestUtils.setField(contractStorageService, "serviceOfferingOrchestratorClient", serviceOfferingOrchestratorClient);
        ReflectionTestUtils.setField(contractStorageService, "storageClient", storageClient);
        ReflectionTestUtils.setField(contractStorageService, "pdfServiceClient", pdfServiceClient);
        ReflectionTestUtils.setField(contractStorageService, "contractMapper", contractMapper);
        ReflectionTestUtils.setField(contractStorageService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(contractStorageService, "entityManager", entityManager);
        ReflectionTestUtils.setField(contractStorageService, "organizationOrchestratorClient", organizationOrchestratorClient);
        ReflectionTestUtils.setField(contractStorageService, "contractTemplateRepository", contractTemplateRepository);
        ReflectionTestUtils.setField(contractStorageService, "messageQueueService", messageQueueService);
        ReflectionTestUtils.setField(contractStorageService, "contractSignerService", contractSignerService);

        ContractTnc tnc = new ContractTnc();
        tnc.setContent("http://example.com");
        tnc.setHash("hash1234");

        saasContract = new SaasContractTemplate();
        saasContract.setConsumerId("Participant:10");
        saasContract.setProviderId("Participant:20");
        saasContract.setOfferingId("ServiceOffering:1234");
        saasContract.setTermsAndConditions(List.of(tnc));
        contractTemplateRepository.save(saasContract);

        dataDeliveryContract = new DataDeliveryContractTemplate();
        dataDeliveryContract.setConsumerId("Participant:20");
        dataDeliveryContract.setProviderId("Participant:10");
        dataDeliveryContract.setOfferingId("ServiceOffering:2345");
        dataDeliveryContract.setTermsAndConditions(List.of(tnc));
        contractTemplateRepository.save(dataDeliveryContract);

        coopContract = new CooperationContractTemplate();
        coopContract.setConsumerId("Participant:10");
        coopContract.setProviderId("Participant:20");
        coopContract.setOfferingId("ServiceOffering:3456");
        coopContract.setTermsAndConditions(List.of(tnc));
        contractTemplateRepository.save(coopContract);

        this.defaultPageRequest = PageRequest.of(0, 9, Sort.by("creationDate").descending());

    }

    @BeforeEach
    public void beforeEach() throws IOException, StorageClientException {
        String userCountOption = """
                                ,"merlot:userCountOption": [
                                     {
                                         "@type": "merlot:AllowedUserCount",
                                         "merlot:userCountUpTo": {
                                             "@type": "xsd:number",
                                             "@value": 0
                                         }
                                     }
                                 ]
                """;

        String exchangeCountOption = """
                                ,"merlot:dataAccessType": {
                                    "@type": "xsd:string",
                                    "@value": "Download"
                                },
                                "merlot:dataTransferType": {
                                    "@type": "xsd:string",
                                    "@value": "Push"
                                },
                                "merlot:exchangeCountOption": [
                                    {
                                        "@type": null,
                                        "merlot:exchangeCountUpTo": {
                                            "@type": "xsd:number",
                                            "@value": 0
                                        }
                                    }
                                ]
                """;
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ServiceOfferingDetails offering4321 = objectMapper.readValue(
                createServiceOfferingOrchestratorResponse(
                        "ServiceOffering:4321",
                        "4321",
                        "OfferingName",
                        "Participant:40",
                        "merlot:MerlotServiceOfferingSaaS",
                        userCountOption), ServiceOfferingDetails.class);

        ServiceOfferingDetails saasOffering = objectMapper.readValue(
                createServiceOfferingOrchestratorResponse(
                        saasContract.getOfferingId(),
                        "1234",
                        "MyOffering",
                        saasContract.getProviderId(),
                        "merlot:MerlotServiceOfferingSaaS",
                        userCountOption), ServiceOfferingDetails.class);

        ServiceOfferingDetails dataDeliveryOffering = objectMapper.readValue(
                createServiceOfferingOrchestratorResponse(
                        dataDeliveryContract.getOfferingId(),
                        "2345",
                        "MyOffering",
                        dataDeliveryContract.getProviderId(),
                        "merlot:MerlotServiceOfferingDataDelivery",
                        exchangeCountOption), ServiceOfferingDetails.class);

        ServiceOfferingDetails coopOffering = objectMapper.readValue(
                createServiceOfferingOrchestratorResponse(
                        coopContract.getOfferingId(),
                        "3456",
                        "MyOffering",
                        coopContract.getProviderId(),
                        "merlot:MerlotServiceOfferingCooperation",
                        ""), ServiceOfferingDetails.class);


        lenient().when(serviceOfferingOrchestratorClient.getOfferingDetails(eq("ServiceOffering:4321"), any()))
                .thenReturn(offering4321);
        lenient().when(messageQueueService.remoteRequestOfferingDetails(eq("ServiceOffering:4321")))
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

        String organizationOrchestratorResponse = createOrganizationsOrchestratorResponse("40");
        lenient().when(organizationOrchestratorClient.getOrganizationDetails(any(), any()))
                .thenReturn(objectMapper.readValue(organizationOrchestratorResponse, OrganizationDetails.class));

        lenient().when(storageClient.getItem(any(), any())).thenReturn(new byte[]{0x01, 0x02, 0x03, 0x04});
    }

    @Test
    void getOrganizationContractsExisting() {
        Page<ContractBasicDto> contracts = contractStorageService.getOrganizationContracts("Participant:10",
                PageRequest.of(0, 9, Sort.by("creationDate").descending()), null , "authToken");

        assertFalse(contracts.isEmpty());
    }

    @Test
    void getOrganizationContractsNonExisting() {
        Page<ContractBasicDto> contracts = contractStorageService.getOrganizationContracts("Participant:99",
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
        Page<ContractBasicDto> contracts = contractStorageService.getOrganizationContracts("Participant:10",
                PageRequest.of(0, 9, Sort.by("creationDate").descending()), ContractState.RELEASED , "authToken");

        assertTrue(contracts.isEmpty());
        contracts = contractStorageService.getOrganizationContracts("Participant:10",
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
        request.setConsumerId("Participant:40");
        request.setOfferingId(saasContract.getOfferingId());
        ContractDto contract = contractStorageService.addContractTemplate(request, "authToken");

        assertEquals(saasContract.getProviderId(), contract.getDetails().getProviderId());
    }

    @Test
    void createContractTemplateDataDeliveryValidRequest() throws Exception {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId("Participant:40");
        request.setOfferingId(dataDeliveryContract.getOfferingId());
        ContractDto contract = contractStorageService.addContractTemplate(request, "authToken");

        assertEquals(dataDeliveryContract.getProviderId(), contract.getDetails().getProviderId());
    }

    @Test
    void createContractTemplateCooperationValidRequest() throws Exception {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId("Participant:40");
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
        request.setOfferingId("ServiceOffering:4321");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.addContractTemplate(request, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    void createContractTemplateInvalidOfferingId() {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setConsumerId("Participant:10");
        request.setOfferingId("garbage");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.addContractTemplate(request, "authToken"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    @Transactional
    void updateContractExistingAllowedAsConsumer() throws JSONException {
        Set<String> representedOrgaIds = new HashSet<>();
        representedOrgaIds.add(saasContract.getConsumerId().replace("Participant:", ""));
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
        representedOrgaIds.add(saasContract.getConsumerId().replace("Participant:", ""));
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
        representedOrgaIds.add(dataDeliveryContract.getConsumerId().replace("Participant:", ""));
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
        representedOrgaIds.add(saasContract.getProviderId().replace("Participant:", ""));
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
        representedOrgaIds.add(saasContract.getProviderId().replace("Participant:", ""));
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
        String provider = saasContract.getProviderId().replace("Participant:", "");
        String consumer = saasContract.getConsumerId().replace("Participant:", "");
        representedOrgaIds.add(provider);
        representedOrgaIds.add(consumer);

        SaasContractDto editedContract = (SaasContractDto) contractStorageService.getContractDetails(saasContract.getId(),
                "authToken");

        editedContract.getDetails().setConsumerId("Participant:99");
        editedContract.getDetails().setProviderId("Participant:99");
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
        String provider = saasContract.getProviderId().replace("Participant:", "");
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
        String consumer = dataDeliveryContract.getConsumerId().replace("Participant:", "");
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
        String consumer = saasContract.getConsumerId().replace("Participant:", "");
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
        String consumer = saasContract.getConsumerId().replace("Participant:", "");
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
        String consumer = saasContract.getConsumerId().replace("Participant:", "");
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
                        state, activeRoleOrgaId, "userId", "User Name","authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @Transactional
    void transitionDataDeliveryConsumerIncompleteToComplete() throws JSONException, IOException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId().replace("Participant:", "");
        representedOrgaIds.add(consumer);

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);
        template.setServiceContractProvisioning(new DataDeliveryProvisioning()); // reset provisioning
        contractTemplateRepository.save(template);

        DataDeliveryContractDto editedContract = (DataDeliveryContractDto) contractStorageService.getContractDetails(dataDeliveryContract.getId(),
                "authToken");


        assertTransitionThrowsForbidden(editedContract.getDetails().getId(), ContractState.SIGNED_CONSUMER, consumer);
        editedContract.getNegotiation().setConsumerTncAccepted(true);
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);
        contractStorageService.getContractDetails(editedContract.getDetails().getId(),
                "authToken");
        assertTransitionThrowsForbidden(editedContract.getDetails().getId(), ContractState.SIGNED_CONSUMER, consumer);

        editedContract.getNegotiation().setConsumerAttachmentsAccepted(true);
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);
        contractStorageService.getContractDetails(editedContract.getDetails().getId(),
                "authToken");
        assertTransitionThrowsForbidden(editedContract.getDetails().getId(), ContractState.SIGNED_CONSUMER, consumer);

        editedContract.getNegotiation().setExchangeCountSelection("0");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);
        contractStorageService.getContractDetails(editedContract.getDetails().getId(),
                "authToken");
        assertTransitionThrowsForbidden(editedContract.getDetails().getId(), ContractState.SIGNED_CONSUMER, consumer);

        editedContract.getNegotiation().setRuntimeSelection("0 unlimited");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);
        contractStorageService.getContractDetails(editedContract.getDetails().getId(),
                "authToken");
        assertTransitionThrowsForbidden(editedContract.getDetails().getId(), ContractState.SIGNED_CONSUMER, consumer);

        editedContract.getProvisioning().setDataAddressTargetFileName("MyFile.json");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);
        contractStorageService.getContractDetails(editedContract.getDetails().getId(),
                "authToken");
        assertTransitionThrowsForbidden(editedContract.getDetails().getId(), ContractState.SIGNED_CONSUMER, consumer);

        editedContract.getProvisioning().setDataAddressTargetBucketName("MyBucket");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);
        contractStorageService.getContractDetails(editedContract.getDetails().getId(),
                "authToken");
        assertTransitionThrowsForbidden(editedContract.getDetails().getId(), ContractState.SIGNED_CONSUMER, consumer);

        editedContract.getProvisioning().setSelectedConsumerConnectorId("edc1");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);
        DataDeliveryContractDto result = (DataDeliveryContractDto) contractStorageService.transitionContractTemplateState(editedContract.getDetails().getId(),
                ContractState.SIGNED_CONSUMER, consumer, "userId", "User Name", "authToken");
        assertEquals(ContractState.SIGNED_CONSUMER.name(), result.getDetails().getState());
    }

    @Test
    @Transactional
    void transitionDataDeliveryProviderIncompleteToComplete() throws JSONException, IOException,
        StorageClientException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId().replace("Participant:", "");
        String provider = dataDeliveryContract.getProviderId().replace("Participant:", "");
        representedOrgaIds.add(provider);

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);
        template.setServiceContractProvisioning(new DataDeliveryProvisioning()); // reset provisioning
        contractTemplateRepository.save(template);

        DataDeliveryContractDto editedContract = (DataDeliveryContractDto) contractStorageService.getContractDetails(dataDeliveryContract.getId(),
                "authToken");

        editedContract.getNegotiation().setExchangeCountSelection("0");
        editedContract.getNegotiation().setRuntimeSelection("0 unlimited");
        editedContract.getNegotiation().setConsumerTncAccepted(true);
        editedContract.getNegotiation().setConsumerAttachmentsAccepted(true);
        editedContract.getProvisioning().setDataAddressTargetFileName("MyFile.json");
        editedContract.getProvisioning().setDataAddressTargetBucketName("MyBucket");
        editedContract.getProvisioning().setSelectedConsumerConnectorId("edc1");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);

        editedContract = (DataDeliveryContractDto) contractStorageService.transitionContractTemplateState(editedContract.getDetails().getId(),
                ContractState.SIGNED_CONSUMER, consumer, "consumerUserId", "User Name", "authToken");
        assertEquals(ContractState.SIGNED_CONSUMER.name(), editedContract.getDetails().getState());

        assertTransitionThrowsForbidden(editedContract.getDetails().getId(), ContractState.RELEASED, provider);

        editedContract.getNegotiation().setProviderTncAccepted(true);
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                provider);
        assertTransitionThrowsForbidden(editedContract.getDetails().getId(), ContractState.RELEASED, provider);

        editedContract.getProvisioning().setDataAddressType("IonosS3");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                provider);
        assertTransitionThrowsForbidden(editedContract.getDetails().getId(), ContractState.RELEASED, provider);

        editedContract.getProvisioning().setDataAddressSourceBucketName("MyBucket2");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                provider);
        assertTransitionThrowsForbidden(editedContract.getDetails().getId(), ContractState.RELEASED, provider);

        editedContract.getProvisioning().setDataAddressSourceFileName("MyFile2..json");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                provider);
        assertTransitionThrowsForbidden(editedContract.getDetails().getId(), ContractState.RELEASED, provider);

        editedContract.getProvisioning().setSelectedProviderConnectorId("edc2");
        editedContract = (DataDeliveryContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                provider);

        editedContract = (DataDeliveryContractDto) contractStorageService.transitionContractTemplateState(editedContract.getDetails().getId(),
                ContractState.RELEASED, provider, "providerUserId", "User Name", "authToken");
        assertEquals(ContractState.RELEASED.name(), editedContract.getDetails().getState());

        verify(pdfServiceClient).getPdfContract(any());
        verify(storageClient).pushItem(eq(editedContract.getDetails().getId() + "/contractPdf"), anyString(), any());
    }

    @Test
    @Transactional
    void transitionContractNotAuthorized() {
        SaasContractTemplate template = new SaasContractTemplate(saasContract, false);
        String templateId = template.getId();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.transitionContractTemplateState(templateId,
                        ContractState.SIGNED_CONSUMER, "99", "consumerUserId", "User Name", "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @Transactional
    void transitionContractProviderNotAllowed() {
        String provider = saasContract.getProviderId().replace("Participant:", "");

        SaasContractTemplate template = new SaasContractTemplate(saasContract, false);
        String templateId = template.getId();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.transitionContractTemplateState(templateId,
                        ContractState.SIGNED_CONSUMER, provider, "providerUserId", "User Name", "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @Transactional
    void transitionContractConsumerNotAllowed() throws IOException {
        String consumer = dataDeliveryContract.getConsumerId().replace("Participant:", "");

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);
        DataDeliveryProvisioning provisioning = template.getServiceContractProvisioning();
        String templateId = template.getId();
        template.setExchangeCountSelection("0");
        template.setRuntimeSelection("0 unlimited");
        template.setConsumerTncAccepted(true);
        template.setConsumerAttachmentsAccepted(true);
        provisioning.setDataAddressTargetFileName("MyFile.json");
        provisioning.setDataAddressTargetBucketName("MyBucket");
        provisioning.setSelectedConsumerConnectorId("edc1");
        contractTemplateRepository.save(template);
        contractStorageService.transitionContractTemplateState(templateId,
                ContractState.SIGNED_CONSUMER, consumer, "consumerUserId", "User Name", "authToken");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> contractStorageService.transitionContractTemplateState(templateId,
                        ContractState.RELEASED, consumer, "consumerUserId", "User Name", "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @Transactional
    void transitionSaasContractRevokedNotAllowed() throws JSONException, IOException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = saasContract.getConsumerId().replace("Participant:", "");
        String provider = saasContract.getProviderId().replace("Participant:", "");
        representedOrgaIds.add(consumer);
        representedOrgaIds.add(provider);

        SaasContractDto editedContract = (SaasContractDto) contractStorageService.getContractDetails(saasContract.getId(),
                "authToken");

        editedContract.getNegotiation().setConsumerTncAccepted(true);
        editedContract.getNegotiation().setConsumerAttachmentsAccepted(true);
        editedContract.getNegotiation().setUserCountSelection("0");
        editedContract.getNegotiation().setRuntimeSelection("4 day(s)");
        editedContract = (SaasContractDto) contractStorageService.updateContractTemplate(editedContract, "authToken",
                consumer);

        SaasContractDto result = (SaasContractDto) contractStorageService.transitionContractTemplateState(editedContract.getDetails().getId(),
                ContractState.SIGNED_CONSUMER, consumer, "userId", "User Name", "authToken");

        result.getNegotiation().setProviderTncAccepted(true);

        SaasContractDto result2 = (SaasContractDto) contractStorageService
                .updateContractTemplate(result, "authToken",
                        representedOrgaIds.iterator().next());

        assertEquals(result.getNegotiation().isProviderTncAccepted(), result2.getNegotiation().isProviderTncAccepted());

        result = (SaasContractDto) contractStorageService.transitionContractTemplateState(result.getDetails().getId(),
                ContractState.RELEASED, provider, "userId", "User Name", "authToken");

        assertTransitionThrowsForbidden(result.getDetails().getId(), ContractState.REVOKED, provider);
    }

    @Test
    @Transactional
    void transitionCooperationContractRevokedNotAllowed() throws JSONException, IOException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = coopContract.getConsumerId().replace("Participant:", "");
        String provider = coopContract.getProviderId().replace("Participant:", "");
        representedOrgaIds.add(consumer);
        representedOrgaIds.add(provider);
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
                ContractState.SIGNED_CONSUMER, consumer, "userId", "User Name", "authToken");

        result.getNegotiation().setProviderTncAccepted(true);

        CooperationContractDto result2 = (CooperationContractDto) contractStorageService
                .updateContractTemplate(result, "authToken",
                        representedOrgaIds.iterator().next());

        assertEquals(result.getNegotiation().isProviderTncAccepted(), result2.getNegotiation().isProviderTncAccepted());

        result = (CooperationContractDto) contractStorageService.transitionContractTemplateState(result.getDetails().getId(),
                ContractState.RELEASED, provider, "userId", "User Name","authToken");

        assertTransitionThrowsForbidden(result.getDetails().getId(), ContractState.REVOKED, provider);
    }

    @Test
    @Transactional
    void transitionDataDeliveryContractPurge() throws IOException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId().replace("Participant:", "");
        String provider = dataDeliveryContract.getProviderId().replace("Participant:", "");
        representedOrgaIds.add(consumer);
        representedOrgaIds.add(provider);
        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);

        DataDeliveryContractDto result = (DataDeliveryContractDto) contractStorageService.transitionContractTemplateState(template.getId(),
                ContractState.DELETED, consumer, "userId", "User Name", "authToken");

        result = (DataDeliveryContractDto) contractStorageService.transitionContractTemplateState(template.getId(),
                ContractState.PURGED, provider, "userId", "User Name", "authToken");

        assertNull(contractTemplateRepository.findById(result.getDetails().getId()).orElse(null));
    }

    @Test
    @Transactional
    void transitionDataDeliveryContractPurgeWrongState() {
        String contractId = dataDeliveryContract.getId();
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId().replace("Participant:", "");
        String provider = dataDeliveryContract.getProviderId().replace("Participant:", "");
        representedOrgaIds.add(consumer);
        representedOrgaIds.add(provider);
        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () ->contractStorageService.transitionContractTemplateState(contractId,
                        ContractState.PURGED, provider, "userId", "User Name", "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @Transactional
    void transitionDataDeliveryContractPurgeWrongRole() throws IOException {
        String contractId = dataDeliveryContract.getId();
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId().replace("Participant:", "");
        String provider = dataDeliveryContract.getProviderId().replace("Participant:", "");
        representedOrgaIds.add(consumer);
        representedOrgaIds.add(provider);
        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);

        DataDeliveryContractDto result = (DataDeliveryContractDto) contractStorageService
                .transitionContractTemplateState(contractId, ContractState.DELETED, consumer, "userId", "User Name", "authToken");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () ->contractStorageService.transitionContractTemplateState(contractId,
                        ContractState.PURGED, consumer, "userId", "User Name", "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }


    @Test
    @Transactional
    void updateDataDeliveryFieldsAfterTransition() throws JSONException, IOException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId().replace("Participant:", "");
        String provider = dataDeliveryContract.getProviderId().replace("Participant:", "");
        representedOrgaIds.add(consumer);
        representedOrgaIds.add(provider);
        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate(dataDeliveryContract, false);
        template.setServiceContractProvisioning(new DataDeliveryProvisioning());
        DataDeliveryProvisioning provisioning =
                (DataDeliveryProvisioning) template.getServiceContractProvisioning();

        DataDeliveryContractDto editedContract = (DataDeliveryContractDto) contractStorageService.getContractDetails(dataDeliveryContract.getId(),
                "authToken");

        editedContract.getNegotiation().setConsumerTncAccepted(true);
        editedContract.getNegotiation().setConsumerAttachmentsAccepted(true);
        editedContract.getNegotiation().setExchangeCountSelection("0");
        editedContract.getNegotiation().setRuntimeSelection("4 day(s)");
        editedContract.getProvisioning().setDataAddressTargetBucketName("MyBucket");
        editedContract.getProvisioning().setDataAddressTargetFileName("MyFile.json");
        editedContract.getProvisioning().setSelectedConsumerConnectorId("edc1");

        DataDeliveryContractDto result = (DataDeliveryContractDto) contractStorageService
                .updateContractTemplate(editedContract, "authToken",
                        consumer);

        assertEquals(editedContract.getNegotiation().isConsumerTncAccepted(), result.getNegotiation().isConsumerTncAccepted());
        assertEquals(editedContract.getNegotiation().isConsumerAttachmentsAccepted(), result.getNegotiation().isConsumerAttachmentsAccepted());
        assertEquals(editedContract.getNegotiation().getExchangeCountSelection(), result.getNegotiation().getExchangeCountSelection());
        assertEquals(editedContract.getNegotiation().getRuntimeSelection(), result.getNegotiation().getRuntimeSelection());
        assertEquals(editedContract.getProvisioning().getDataAddressTargetBucketName(), result.getProvisioning().getDataAddressTargetBucketName());
        assertEquals(editedContract.getProvisioning().getDataAddressTargetFileName(), result.getProvisioning().getDataAddressTargetFileName());
        assertEquals(editedContract.getProvisioning().getSelectedConsumerConnectorId(), result.getProvisioning().getSelectedConsumerConnectorId());

        result = (DataDeliveryContractDto) contractStorageService.transitionContractTemplateState(result.getDetails().getId(),
                ContractState.SIGNED_CONSUMER, consumer, "userId", "User Name", "authToken");

        result.getNegotiation().setProviderTncAccepted(true);
        result.getProvisioning().setDataAddressType("IonosS3");
        result.getProvisioning().setDataAddressSourceBucketName("MyBucket2");
        result.getProvisioning().setDataAddressSourceFileName("MyFile2..json");
        result.getProvisioning().setSelectedProviderConnectorId("edc2");

        DataDeliveryContractDto result2 = (DataDeliveryContractDto) contractStorageService
                .updateContractTemplate(result, "token", provider);

        assertEquals(result.getNegotiation().isProviderTncAccepted(), result2.getNegotiation().isProviderTncAccepted());
        assertEquals(result.getProvisioning().getDataAddressType(), result2.getProvisioning().getDataAddressType());
        assertEquals(result.getProvisioning().getDataAddressSourceFileName(), result2.getProvisioning().getDataAddressSourceFileName());
        assertEquals(result.getProvisioning().getDataAddressSourceBucketName(), result2.getProvisioning().getDataAddressSourceBucketName());
        assertEquals(result.getProvisioning().getSelectedProviderConnectorId(), result2.getProvisioning().getSelectedProviderConnectorId());

        result = (DataDeliveryContractDto) contractStorageService.transitionContractTemplateState(result.getDetails().getId(),
                ContractState.RELEASED, provider, "userId", "User Name", "authToken");

        assertNotNull(result.getProvisioning().getValidUntil());

    }

    @Test
    void regenerateDataDeliveryContractValid() throws IOException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId().replace("Participant:", "");
        representedOrgaIds.add(consumer);
        DataDeliveryContractDto template = (DataDeliveryContractDto) this.contractStorageService
                .transitionContractTemplateState(dataDeliveryContract.getId(), ContractState.DELETED, consumer, "1234", "User Name", "authToken");
        template = (DataDeliveryContractDto) this.contractStorageService.regenerateContract(dataDeliveryContract.getId(), "authToken");

        assertNotEquals(template.getDetails().getId(), dataDeliveryContract.getId());
        assertEquals(ContractState.IN_DRAFT.name(), template.getDetails().getState());
    }

    @Test
    void regenerateSaasContractValid() throws IOException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = saasContract.getConsumerId().replace("Participant:", "");
        representedOrgaIds.add(consumer);
        SaasContractDto template = (SaasContractDto) this.contractStorageService
                .transitionContractTemplateState(saasContract.getId(), ContractState.DELETED, consumer, "1234", "User Name", "authToken");
        template = (SaasContractDto) this.contractStorageService.regenerateContract(saasContract.getId(), "authToken");

        assertNotEquals(template.getDetails().getId(), saasContract.getId());
        assertEquals(ContractState.IN_DRAFT.name(), template.getDetails().getState());
    }

    @Test
    void regenerateCooperationContractValid() throws IOException {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = coopContract.getConsumerId().replace("Participant:", "");
        representedOrgaIds.add(consumer);
        CooperationContractDto template = (CooperationContractDto) this.contractStorageService.transitionContractTemplateState(coopContract.getId(),
                ContractState.DELETED, consumer, "1234", "User Name", "authToken");
        template = (CooperationContractDto) this.contractStorageService.regenerateContract(template.getDetails().getId(), "authToken");

        assertNotEquals(template.getDetails().getId(), coopContract.getId());
        assertEquals(ContractState.IN_DRAFT.name(), template.getDetails().getState());
    }

    @Test
    void regenerateContractNotAllowedState() {
        Set<String> representedOrgaIds = new HashSet<>();
        String consumer = dataDeliveryContract.getConsumerId().replace("Participant:", "");
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
    void addContractAttachmentsInDraftTooManyAttachments() throws IOException {
        String templateId = dataDeliveryContract.getId();
        for (int i = 0; i < 10; i++) {
            this.contractStorageService.addContractAttachment(templateId, new byte[]{},
                    "myFile" + i + ".pdf", "authToken");
        }
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> this.contractStorageService.addContractAttachment(templateId, new byte[]{},
                        "myFile" + 10 + ".pdf", "authToken"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
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
