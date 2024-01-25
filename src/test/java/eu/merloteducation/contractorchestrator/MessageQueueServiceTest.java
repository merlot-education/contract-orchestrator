package eu.merloteducation.contractorchestrator;

import eu.merloteducation.contractorchestrator.service.MessageQueueService;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescription;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionVerifiableCredential;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.TermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
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

    private MerlotParticipantDto orga10;

    @BeforeAll
    void beforeAll() {
        ReflectionTestUtils.setField(messageQueueService, "rabbitTemplate", rabbitTemplate);
    }

    @BeforeEach
    void beforeEach() {
        when(rabbitTemplate.convertSendAndReceiveAsType(anyString(), anyString(), any(Object.class),any()))
                .thenReturn(null);
        orga10 = new MerlotParticipantDto();
        orga10.setSelfDescription(new SelfDescription());
        orga10.getSelfDescription().setVerifiableCredential(new SelfDescriptionVerifiableCredential());
        orga10.getSelfDescription().getVerifiableCredential().setCredentialSubject(new MerlotOrganizationCredentialSubject());
        MerlotOrganizationCredentialSubject credentialSubject = (MerlotOrganizationCredentialSubject)
                orga10.getSelfDescription().getVerifiableCredential()
                .getCredentialSubject();
        credentialSubject.setId("Participant:10");
        credentialSubject.setLegalName("Orga 10");
        credentialSubject.setTermsAndConditions(new TermsAndConditions());
        credentialSubject.getTermsAndConditions().setContent("http://example.com");
        credentialSubject.getTermsAndConditions().setHash("hash1234");
        doReturn(orga10).when(rabbitTemplate)
                .convertSendAndReceiveAsType(anyString(), anyString(), eq("10"),any());
    }

    @Test
    void remoteGetOrgaDetailsExistent() {
        MerlotParticipantDto details = messageQueueService.remoteRequestOrganizationDetails("10");
        assertNotNull(details);
        MerlotOrganizationCredentialSubject orga10CredentialSubject = (MerlotOrganizationCredentialSubject)
                orga10.getSelfDescription().getVerifiableCredential()
                        .getCredentialSubject();
        MerlotOrganizationCredentialSubject detailsCredentialSubject = (MerlotOrganizationCredentialSubject)
                details.getSelfDescription().getVerifiableCredential()
                        .getCredentialSubject();
        assertEquals(orga10CredentialSubject.getId(),
                detailsCredentialSubject.getId());
        assertEquals(orga10CredentialSubject.getLegalName(),
                detailsCredentialSubject.getLegalName());
        assertEquals(orga10CredentialSubject.getTermsAndConditions().getContent(),
                detailsCredentialSubject.getTermsAndConditions().getContent());
        assertEquals(orga10CredentialSubject.getTermsAndConditions().getHash(),
                detailsCredentialSubject.getTermsAndConditions().getHash());
    }

    @Test
    void remoteGetOrgaDetailsNonExistent() {
        MerlotParticipantDto details = messageQueueService.remoteRequestOrganizationDetails("garbage");
        assertNull(details);
    }
}
