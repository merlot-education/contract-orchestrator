package eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension;

import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ConsumerTransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.DataDeliveryProvisioning;
import io.netty.util.internal.StringUtil;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
public class IonosS3ConsumerTransferProvisioning extends ConsumerTransferProvisioning {

    private String dataAddressTargetBucketName;

    private String dataAddressTargetPath;

    public IonosS3ConsumerTransferProvisioning() {
        super();
        this.dataAddressTargetBucketName = "";
        this.dataAddressTargetPath = "";
    }

    @Override
    public boolean transitionAllowed(ContractState targetState, DataDeliveryProvisioning provisioning) {
        return super.transitionAllowed(targetState, provisioning) && switch (targetState) {
            case SIGNED_CONSUMER ->
                    !StringUtil.isNullOrEmpty(dataAddressTargetBucketName)
                    && !StringUtil.isNullOrEmpty(dataAddressTargetPath);
            default -> true;
        };
    }

    @Override
    public ConsumerTransferProvisioning makeCopy() {
        IonosS3ConsumerTransferProvisioning provisioning = new IonosS3ConsumerTransferProvisioning();
        provisioning.setSelectedConsumerConnectorId(getSelectedConsumerConnectorId());
        provisioning.setDataAddressTargetPath(getDataAddressTargetPath());
        provisioning.setDataAddressTargetBucketName(getDataAddressTargetBucketName());
        return provisioning;
    }
}
