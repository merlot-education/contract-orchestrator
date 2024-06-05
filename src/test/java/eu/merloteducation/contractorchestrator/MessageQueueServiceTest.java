package eu.merloteducation.contractorchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.merloteducation.contractorchestrator.models.entities.*;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.DataDeliveryContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.DataDeliveryProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension.IonosS3ConsumerTransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension.IonosS3ProviderTransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.saas.SaasContractTemplate;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import eu.merloteducation.contractorchestrator.service.MessageQueueService;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotLegalParticipantCredentialSubject;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

import static eu.merloteducation.contractorchestrator.SelfDescriptionDemoData.*;
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

    @Autowired
    ContractTemplateRepository contractTemplateRepository;

    @MockBean
    RabbitTemplate rabbitTemplate;

    private MerlotParticipantDto orga10;

    @BeforeAll
    void beforeAll() {
        ReflectionTestUtils.setField(messageQueueService, "rabbitTemplate", rabbitTemplate);
    }

    @BeforeEach
    void beforeEach() throws JsonProcessingException {
        when(rabbitTemplate.convertSendAndReceiveAsType(anyString(), anyString(), any(Object.class),any()))
                .thenReturn(null);
        orga10 = new MerlotParticipantDto();
        String orga10Id = "did:web:test.eu:orga-10";
        orga10.setId(orga10Id);
        orga10.setSelfDescription(createVpFromCsList(
                List.of(
                       getGxParticipantCs(orga10Id),
                        getGxRegistrationNumberCs(orga10Id),
                        getMerlotParticipantCs(orga10Id)
                )
        ));
        doReturn(orga10).when(rabbitTemplate)
                .convertSendAndReceiveAsType(anyString(), anyString(), eq("10"),any());
    }

    @Test
    void remoteGetOrgaDetailsExistent() {
        MerlotParticipantDto details = messageQueueService.remoteRequestOrganizationDetails("10");
        assertNotNull(details);
        MerlotLegalParticipantCredentialSubject orga10Cs = orga10.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class);
        MerlotLegalParticipantCredentialSubject detailsCs = details.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class);

        assertEquals(orga10Cs.getId(),
                detailsCs.getId());
        assertEquals(orga10Cs.getLegalName(),
                detailsCs.getLegalName());
        assertEquals(orga10Cs.getTermsAndConditions().getUrl(),
                detailsCs.getTermsAndConditions().getUrl());
        assertEquals(orga10Cs.getTermsAndConditions().getHash(),
                detailsCs.getTermsAndConditions().getHash());
    }

    @Test
    void remoteGetOrgaDetailsNonExistent() {
        MerlotParticipantDto details = messageQueueService.remoteRequestOrganizationDetails("garbage");
        assertNull(details);
    }

    @Transactional
    @Test
    void organizationRevokedHandleAssociatedContracts() {
        String orgaId = "orgaId";
        SaasContractTemplate templateSaas = new SaasContractTemplate();
        templateSaas.setProviderId(orgaId);
        templateSaas = contractTemplateRepository.save(templateSaas);

        DataDeliveryContractTemplate templateData = new DataDeliveryContractTemplate();
        templateData.setConsumerId(orgaId);
        templateData.setRuntimeSelection("anything");
        templateData.setConsumerSignature(new ContractSignature("signer"));
        templateData.setConsumerTncAccepted(true);
        templateData.setAttachments(new HashSet<>());
        templateData.setConsumerTncAccepted(true);
        templateData.setExchangeCountSelection("anything");
        DataDeliveryProvisioning dataDeliveryProvisioning = new DataDeliveryProvisioning();
        IonosS3ConsumerTransferProvisioning consumerProvisioning = new IonosS3ConsumerTransferProvisioning();
        IonosS3ProviderTransferProvisioning providerProvisioning = new IonosS3ProviderTransferProvisioning();
        consumerProvisioning.setDataAddressTargetBucketName("foo");
        consumerProvisioning.setDataAddressTargetPath("bar/");
        consumerProvisioning.setSelectedConnectorId("something");
        dataDeliveryProvisioning.setConsumerTransferProvisioning(consumerProvisioning);
        dataDeliveryProvisioning.setProviderTransferProvisioning(providerProvisioning);
        templateData.setServiceContractProvisioning(dataDeliveryProvisioning);
        templateData.transitionState(ContractState.SIGNED_CONSUMER);
        templateData = contractTemplateRepository.save(templateData);

        messageQueueService.organizationRevokedListener(orgaId);

        ContractTemplate templateSaasAfterOrganizationRevoked =
            contractTemplateRepository.findById(templateSaas.getId()).orElse(null);
        assertNotNull(templateSaasAfterOrganizationRevoked);
        assertEquals(ContractState.DELETED, templateSaasAfterOrganizationRevoked.getState());

        ContractTemplate templateDataAfterOrganizationRevoked =
            contractTemplateRepository.findById(templateData.getId()).orElse(null);
        assertNotNull(templateDataAfterOrganizationRevoked);
        assertEquals(ContractState.REVOKED, templateDataAfterOrganizationRevoked.getState());
    }
}
