package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.config.MessageQueueConfig;
import eu.merloteducation.contractorchestrator.models.OrganizationDetails;
import eu.merloteducation.contractorchestrator.models.messagequeue.ContractTemplateCreated;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Service
public class MessageQueueService {

    @Autowired
    RabbitTemplate rabbitTemplate;

    public void sendContractCreatedMessage(ContractTemplateCreated contractTemplateCreated) {
        rabbitTemplate.convertAndSend(
                MessageQueueConfig.ORCHESTRATOR_EXCHANGE,
                MessageQueueConfig.CONTRACT_CREATED_KEY,
                contractTemplateCreated
        );
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
