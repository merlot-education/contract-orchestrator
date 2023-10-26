package eu.merloteducation.contractorchestrator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EdcContractDefinitionRevokeTask implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(EdcContractDefinitionRevokeTask.class);
    private final EdcClient edcClient;
    private final String contractDefinitionId;

    public EdcContractDefinitionRevokeTask(EdcClient edcClient, String contractDefinitionId) {
        this.edcClient = edcClient;
        this.contractDefinitionId = contractDefinitionId;
    }
    @Override
    public void run() {
        logger.info("Revoking contract definition with id {}", contractDefinitionId);
        this.edcClient.revokeContractDefinition(this.contractDefinitionId);
    }
}
