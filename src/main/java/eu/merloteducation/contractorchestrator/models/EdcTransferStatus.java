package eu.merloteducation.contractorchestrator.models;

import eu.merloteducation.contractorchestrator.models.edc.transfer.IonosS3TransferProcess;
import lombok.Getter;

@Getter
public class EdcTransferStatus {

    private final String id;

    private final String state;

    public EdcTransferStatus(IonosS3TransferProcess transferProcess) {
        this.id = transferProcess.getId();
        this.state = transferProcess.getState();
    }
}
