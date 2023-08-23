package eu.merloteducation.contractorchestrator;

import eu.merloteducation.contractorchestrator.models.organisationsorchestrator.*;
import eu.merloteducation.contractorchestrator.service.MessageQueueService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MessageQueueServiceTest {

    @Autowired
    MessageQueueService messageQueueService;

    @MockBean
    RabbitTemplate rabbitTemplate;

    private OrganizationDetails orga10;

    @BeforeAll
    void beforeAll() {
        ReflectionTestUtils.setField(messageQueueService, "rabbitTemplate", rabbitTemplate);
    }

    @BeforeEach
    void beforeEach() {
        when(rabbitTemplate.convertSendAndReceiveAsType(anyString(), anyString(), any(Object.class),any()))
                .thenReturn(null);
        orga10 = new OrganizationDetails();
        orga10.setSelfDescription(new OrganizationSelfDescription());
        orga10.getSelfDescription().setVerifiableCredential(new OrganizationVerifiableCredential());
        orga10.getSelfDescription().getVerifiableCredential().setCredentialSubject(new OrganizationCredentialSubject());
        OrganizationCredentialSubject credentialSubject =orga10.getSelfDescription().getVerifiableCredential()
                .getCredentialSubject();
        credentialSubject.setId("Participant:10");
        credentialSubject.setLegalName(new StringTypeValue("Orga 10"));
        credentialSubject.setTermsAndConditionsLink(new StringTypeValue("http://example.com"));
        doReturn(orga10).when(rabbitTemplate)
                .convertSendAndReceiveAsType(anyString(), anyString(), eq("10"),any());
    }

    @Test
    void remoteGetOrgaDetailsExistent() {
        OrganizationDetails details = messageQueueService.remoteRequestOrganizationDetails("10");
        assertNotNull(details);
        assertEquals(orga10.getSelfDescription().getVerifiableCredential().getCredentialSubject().getId(),
                details.getSelfDescription().getVerifiableCredential().getCredentialSubject().getId());
        assertEquals(orga10.getSelfDescription().getVerifiableCredential().getCredentialSubject().getLegalName().getValue(),
                details.getSelfDescription().getVerifiableCredential().getCredentialSubject().getLegalName().getValue());
        assertEquals(orga10.getSelfDescription().getVerifiableCredential().getCredentialSubject().getTermsAndConditionsLink().getValue(),
                details.getSelfDescription().getVerifiableCredential().getCredentialSubject().getTermsAndConditionsLink().getValue());
    }

    @Test
    void remoteGetOrgaDetailsNonExistent() {
        OrganizationDetails details = messageQueueService.remoteRequestOrganizationDetails("garbage");
        assertNull(details);
    }
}
