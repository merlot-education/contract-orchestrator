package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.config.MessageQueueConfig;
import eu.merloteducation.contractorchestrator.models.OrganizationDetails;
import eu.merloteducation.contractorchestrator.models.messagequeue.ContractTemplateCreated;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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

    public OrganizationDetails requestOrganizationDetails(String orgaId) {
        OrganizationDetails organizationDetails = (OrganizationDetails) rabbitTemplate.convertSendAndReceive(
                MessageQueueConfig.ORCHESTRATOR_EXCHANGE,
                MessageQueueConfig.ORGANIZATION_REQUEST_KEY,
                orgaId
        );
        System.out.println(organizationDetails);
        return organizationDetails;
    }
}
