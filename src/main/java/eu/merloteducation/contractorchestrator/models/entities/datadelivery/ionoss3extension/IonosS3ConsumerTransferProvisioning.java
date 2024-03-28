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
public class IonosS3ConsumerTransferProvisioning extends TransferProvisioning {

    private String dataAddressTargetBucketName;

    private String dataAddressTargetPath;

    public IonosS3ConsumerTransferProvisioning() {
        super();
        this.dataAddressTargetBucketName = "";
        this.dataAddressTargetPath = "";
    }

    @Override
    public boolean configurationValid(DataDeliveryProvisioning provisioning) {
        return super.configurationValid(provisioning)
                && !StringUtil.isNullOrEmpty(dataAddressTargetBucketName)
                && !StringUtil.isNullOrEmpty(dataAddressTargetPath)
                && commonConfigurationValid(provisioning);
    }

    private boolean commonConfigurationValid(DataDeliveryProvisioning provisioning) {
        if (provisioning.getProviderTransferProvisioning() instanceof IonosS3ProviderTransferProvisioning providerProv) {
            // if both are ionos, the bucket name is not allowed to be equal
            return !this.getDataAddressTargetBucketName().equals(providerProv.getDataAddressSourceBucketName());
        }
        return true;
    }

    @Override
    public TransferProvisioning makeCopy() {
        IonosS3ConsumerTransferProvisioning provisioning = new IonosS3ConsumerTransferProvisioning();
        provisioning.setSelectedConnectorId(getSelectedConnectorId());
        provisioning.setDataAddressTargetPath(getDataAddressTargetPath());
        provisioning.setDataAddressTargetBucketName(getDataAddressTargetBucketName());
        return provisioning;
    }
}
