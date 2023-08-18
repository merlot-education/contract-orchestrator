package eu.merloteducation.contractorchestrator;

import eu.merloteducation.contractorchestrator.models.organisationsorchestrator.OrganizationDetails;
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
        orga10.setId("Participant:10");
        orga10.setMerlotId("10");
        orga10.setOrganizationLegalName("Orga10");
        orga10.setOrganizationName("Orga10");
        orga10.setRegistrationNumber("1234");
        orga10.setTermsAndConditionsLink("http://example.com");
        OrganizationAddressModel addressModel = new OrganizationAddressModel();
        addressModel.setAddressCode("DE");
        addressModel.setCountryCode("DE");
        addressModel.setPostalCode("12345");
        addressModel.setCity("City");
        addressModel.setStreet("Street");
        orga10.setLegalAddress(addressModel);
        doReturn(orga10).when(rabbitTemplate)
                .convertSendAndReceiveAsType(anyString(), anyString(), eq("10"),any());
    }

    @Test
    void remoteGetOrgaDetailsExistent() {
        OrganizationDetails details = messageQueueService.remoteRequestOrganizationDetails("10");
        assertNotNull(details);
        assertEquals(orga10.getId(), details.getId());
        assertEquals(orga10.getMerlotId(), details.getMerlotId());
        assertEquals(orga10.getOrganizationLegalName(), details.getOrganizationLegalName());
        assertEquals(orga10.getOrganizationName(), details.getOrganizationName());
        assertEquals(orga10.getRegistrationNumber(), details.getRegistrationNumber());
        assertEquals(orga10.getTermsAndConditionsLink(), details.getTermsAndConditionsLink());
        assertEquals(orga10.getLegalAddress().getAddressCode(), details.getLegalAddress().getAddressCode());
        assertEquals(orga10.getLegalAddress().getCity(), details.getLegalAddress().getCity());
        assertEquals(orga10.getLegalAddress().getStreet(), details.getLegalAddress().getStreet());
        assertEquals(orga10.getLegalAddress().getPostalCode(), details.getLegalAddress().getPostalCode());
        assertEquals(orga10.getLegalAddress().getCountryCode(), details.getLegalAddress().getCountryCode());
    }

    @Test
    void remoteGetOrgaDetailsNonExistent() {
        OrganizationDetails details = messageQueueService.remoteRequestOrganizationDetails("garbage");
        assertNull(details);
    }
}
