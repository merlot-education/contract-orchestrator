package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.config.MessageQueueConfig;
import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.OrganizationConnectorDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import eu.merloteducation.modelslib.queue.ConnectorDetailsRequest;
import eu.merloteducation.modelslib.queue.ContractTemplateUpdated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageQueueService {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    private ContractTemplateRepository contractTemplateRepository;

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
    public MerlotParticipantDto remoteRequestOrganizationDetails(String orgaId) {
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
    public OrganizationConnectorDto remoteRequestOrganizationConnectorByConnectorId(String orgaId, String connectorId) {
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
    public ServiceOfferingDto remoteRequestOfferingDetails(String offeringId) {
        return rabbitTemplate.convertSendAndReceiveAsType(
                MessageQueueConfig.ORCHESTRATOR_EXCHANGE,
                MessageQueueConfig.OFFERING_REQUEST_KEY,
                offeringId,
                new ParameterizedTypeReference<>() {
                }
        );
    }

    /**
     * Listen for the event that an organization's membership has been revoked on the message bus.
     * In that case, delete in-draft contracts and revoke consumer-signed contracts associated
     * with that organization.
     *
     * @param orgaId id of the organization whose membership has been revoked
     */
    @RabbitListener(queues = MessageQueueConfig.ORGANIZATION_REVOKED_QUEUE)
    public void organizationRevokedListener(String orgaId) {
        logger.info("Organization revoked message: organization ID {}", orgaId);

        List<ContractTemplate> contractsToDelete = contractTemplateRepository.findAllByOrgaIdAndState(orgaId,
            ContractState.IN_DRAFT);

        if (!contractsToDelete.isEmpty()) {
            logger.info("Deleting in-draft contracts associated with organization with ID {}", orgaId);

            contractsToDelete.forEach(contract -> { contract.transitionState(ContractState.DELETED); contractTemplateRepository.save(contract);});
        } else {
            logger.info("No in-draft contracts associated with organization with ID {} found to delete", orgaId);
        }

        List<ContractTemplate> contractsToRevoke = contractTemplateRepository.findAllByOrgaIdAndState(orgaId,
            ContractState.SIGNED_CONSUMER);

        if (!contractsToRevoke.isEmpty()) {
            logger.info("Revoking consumer-signed contracts associated with organization with ID {}", orgaId);

            contractsToRevoke.forEach(contract -> { contract.transitionState(ContractState.REVOKED); contractTemplateRepository.save(contract);});
        } else {
            logger.info("No consumer-signed contracts associated with organization with ID {} found to revoke", orgaId);
        }
    }
}
