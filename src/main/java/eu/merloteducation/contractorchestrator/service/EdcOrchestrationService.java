package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EdcOrchestrationService {

    @Autowired
    private MessageQueueService messageQueueService;

    public void transferContractToParticipatingConnectors(ContractTemplate template) {
        messageQueueService.remoteRequestOrganizationDetails("10");
        // TODO transfer data to EDC of provider and start negotiation
    }


}
