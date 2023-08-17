package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.config.MessageQueueConfig;
import eu.merloteducation.contractorchestrator.models.ConnectorDetailsRequest;
import eu.merloteducation.contractorchestrator.models.organisationsorchestrator.OrganisationConnectorExtension;
import eu.merloteducation.contractorchestrator.models.organisationsorchestrator.OrganizationDetails;
import eu.merloteducation.contractorchestrator.models.messagequeue.ContractTemplateUpdated;
import eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator.OfferingDetails;
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

    /**
     * Send a contract creation message to the message bus.
     *
     * @param contractTemplateUpdated data about the created contract
     */
    public void sendContractCreatedMessage(ContractTemplateUpdated contractTemplateUpdated) {
        logger.info("Sending contract created message for contract with id {} and offering with id {}",
                contractTemplateUpdated.getContractId(),
                contractTemplateUpdated.getServiceOfferingId());
        sendContractUpdatedMessage(contractTemplateUpdated, MessageQueueConfig.CONTRACT_CREATED_KEY);
    }

    /**
     * Send a contract purge message to the message bus.
     *
     * @param contractTemplateUpdated data about the purged contract
     */
    public void sendContractPurgedMessage(ContractTemplateUpdated contractTemplateUpdated) {
        logger.info("Sending contract purged message for contract with id {} and offering with id {}",
                contractTemplateUpdated.getContractId(),
                contractTemplateUpdated.getServiceOfferingId());
        sendContractUpdatedMessage(contractTemplateUpdated, MessageQueueConfig.CONTRACT_PURGED_KEY);
    }

    /**
     * Request details of an organization over the message bus.
     *
     * @param orgaId organization id
     * @return organization details
     */
    public OrganizationDetails remoteRequestOrganizationDetails(String orgaId) {
        return rabbitTemplate.convertSendAndReceiveAsType(
                MessageQueueConfig.ORCHESTRATOR_EXCHANGE,
                MessageQueueConfig.ORGANIZATION_REQUEST_KEY,
                orgaId,
                new ParameterizedTypeReference<>() {
                }
        );
    }

    /**
     * Request details of an organization connector over the message bus.
     *
     * @param orgaId organization id
     * @param connectorId connector id
     * @return connector details
     */
    public OrganisationConnectorExtension remoteRequestOrganizationConnectorByConnectorId(String orgaId, String connectorId) {
        return rabbitTemplate.convertSendAndReceiveAsType(
                MessageQueueConfig.ORCHESTRATOR_EXCHANGE,
                MessageQueueConfig.ORGANIZATIONCONNECTOR_REQUEST_KEY,
                new ConnectorDetailsRequest(connectorId, orgaId),
                new ParameterizedTypeReference<>() {
                }
        );
    }

    /**
     * Request details to a service offering on the bus.
     *
     * @param offeringId id of the offering
     * @return offering details
     */
    public OfferingDetails remoteRequestOfferingDetails(String offeringId) {
        return rabbitTemplate.convertSendAndReceiveAsType(
                MessageQueueConfig.ORCHESTRATOR_EXCHANGE,
                MessageQueueConfig.OFFERING_REQUEST_KEY,
                offeringId,
                new ParameterizedTypeReference<>() {
                }
        );
    }
}
