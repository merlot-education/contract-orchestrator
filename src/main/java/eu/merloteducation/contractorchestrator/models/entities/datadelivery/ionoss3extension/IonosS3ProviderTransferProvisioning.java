package eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension;

import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.DataDeliveryProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ProviderTransferProvisioning;
import io.netty.util.internal.StringUtil;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
public class IonosS3ProviderTransferProvisioning extends ProviderTransferProvisioning {

    private String dataAddressSourceBucketName;

    private String dataAddressSourceFileName;

    public IonosS3ProviderTransferProvisioning() {
        super();
        this.dataAddressSourceBucketName = "";
        this.dataAddressSourceFileName = "";
    }

    @Override
    public boolean transitionAllowed(ContractState targetState, DataDeliveryProvisioning provisioning) {
        return super.transitionAllowed(targetState, provisioning) && switch (targetState) {
            case RELEASED ->
                    !StringUtil.isNullOrEmpty(dataAddressSourceBucketName)
                    && !StringUtil.isNullOrEmpty(dataAddressSourceFileName)
                    && !dataAddressSourceBucketName.equals(
                            ((IonosS3ConsumerTransferProvisioning) provisioning.getConsumerTransferProvisioning())
                                    .getDataAddressTargetBucketName());
            default -> true;
        };
    }

    @Override
    public ProviderTransferProvisioning makeCopy() {
        IonosS3ProviderTransferProvisioning provisioning = new IonosS3ProviderTransferProvisioning();
        provisioning.setSelectedProviderConnectorId(getSelectedProviderConnectorId());
        provisioning.setDataAddressSourceBucketName(getDataAddressSourceBucketName());
        provisioning.setDataAddressSourceFileName(getDataAddressSourceFileName());
        return null;
    }
}
