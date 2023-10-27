package eu.merloteducation.contractorchestrator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EdcTransferDeprovisionTask implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(EdcTransferDeprovisionTask.class);
    private final EdcClient edcClient;
    private final String transferId;

    public EdcTransferDeprovisionTask(EdcClient edcClient, String transferId) {
        this.edcClient = edcClient;
        this.transferId = transferId;
    }
    @Override
    public void run() {
        logger.info("Deprovisioning transfer with id {}", transferId);
        this.edcClient.deprovisionTransfer(this.transferId);
    }
}
