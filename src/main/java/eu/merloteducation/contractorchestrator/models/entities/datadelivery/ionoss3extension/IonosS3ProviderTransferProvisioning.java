package eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension;

import eu.merloteducation.contractorchestrator.models.entities.datadelivery.DataDeliveryProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.TransferProvisioning;
import io.netty.util.internal.StringUtil;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
public class IonosS3ProviderTransferProvisioning extends TransferProvisioning {

    private String dataAddressSourceBucketName;

    private String dataAddressSourceFileName;

    public IonosS3ProviderTransferProvisioning() {
        super();
        this.dataAddressSourceBucketName = "";
        this.dataAddressSourceFileName = "";
    }

    @Override
    public boolean configurationValid() {
        return super.configurationValid()
                && !StringUtil.isNullOrEmpty(dataAddressSourceBucketName)
                && !StringUtil.isNullOrEmpty(dataAddressSourceFileName);
    }

    @Override
    public boolean commonConfigurationValid(DataDeliveryProvisioning provisioning) {
        boolean valid = super.commonConfigurationValid(provisioning);

        if (provisioning.getConsumerTransferProvisioning() instanceof IonosS3ConsumerTransferProvisioning consumerProv) {
            // if both are ionos, the bucket name is not allowed to be equal
            valid &= !this.getDataAddressSourceBucketName().equals(consumerProv.getDataAddressTargetBucketName());
        }

        return valid;
    }

    @Override
    public TransferProvisioning makeCopy() {
        IonosS3ProviderTransferProvisioning provisioning = new IonosS3ProviderTransferProvisioning();
        provisioning.setSelectedConnectorId(getSelectedConnectorId());
        provisioning.setDataAddressSourceBucketName(getDataAddressSourceBucketName());
        provisioning.setDataAddressSourceFileName(getDataAddressSourceFileName());
        return null;
    }
}
