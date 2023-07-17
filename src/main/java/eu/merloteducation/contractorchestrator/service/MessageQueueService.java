package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.config.MessageQueueConfig;
import eu.merloteducation.contractorchestrator.models.OrganizationDetails;
import eu.merloteducation.contractorchestrator.models.messagequeue.ContractTemplateUpdated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Service
public class MessageQueueService {

    @Autowired
    RabbitTemplate rabbitTemplate;

    private final Logger logger = LoggerFactory.getLogger(MessageQueueService.class);

    private void sendContractUpdatedMessage(ContractTemplateUpdated contractTemplateUpdated, String routingKey) {
        rabbitTemplate.convertAndSend(
                MessageQueueConfig.ORCHESTRATOR_EXCHANGE,
                routingKey,
                contractTemplateUpdated
        );
    }

    public void sendContractCreatedMessage(ContractTemplateUpdated contractTemplateUpdated) {
        logger.info("Sending contract created message for contract with id {} and offering with id {}",
                contractTemplateUpdated.getContractId(),
                contractTemplateUpdated.getServiceOfferingId());
        sendContractUpdatedMessage(contractTemplateUpdated, MessageQueueConfig.CONTRACT_CREATED_KEY);
    }

    public void sendContractPurgedMessage(ContractTemplateUpdated contractTemplateUpdated) {
        logger.info("Sending contract purged message for contract with id {} and offering with id {}",
                contractTemplateUpdated.getContractId(),
                contractTemplateUpdated.getServiceOfferingId());
        sendContractUpdatedMessage(contractTemplateUpdated, MessageQueueConfig.CONTRACT_PURGED_KEY);
    }

    public OrganizationDetails remoteRequestOrganizationDetails(String orgaId) {
        return rabbitTemplate.convertSendAndReceiveAsType(
                MessageQueueConfig.ORCHESTRATOR_EXCHANGE,
                MessageQueueConfig.ORGANIZATION_REQUEST_KEY,
                orgaId,
                new ParameterizedTypeReference<>() {
                }
        );
    }
}
