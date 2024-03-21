package eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension;

import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.DataDeliveryProvisioning;
import io.netty.util.internal.StringUtil;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
public class IonosS3DataDeliveryProvisioning extends DataDeliveryProvisioning {


    private String dataAddressSourceBucketName;

    private String dataAddressSourceFileName;

    private String dataAddressTargetBucketName;

    private String dataAddressTargetPath;

    public IonosS3DataDeliveryProvisioning() {
        super();
        this.dataAddressSourceBucketName = "";
        this.dataAddressSourceFileName = "";
        this.dataAddressTargetBucketName = "";
        this.dataAddressTargetPath = "";
    }

    public IonosS3DataDeliveryProvisioning(IonosS3DataDeliveryProvisioning provisioning) {
        super(provisioning);
        this.dataAddressSourceBucketName = provisioning.getDataAddressSourceBucketName();
        this.dataAddressSourceFileName = provisioning.getDataAddressSourceFileName();
        this.dataAddressTargetBucketName = provisioning.getDataAddressTargetBucketName();
        this.dataAddressTargetPath = provisioning.getDataAddressTargetPath();
    }

    @Override
    public boolean transitionAllowed(ContractState targetState) {

        return super.transitionAllowed(targetState) && switch (targetState) {
            case SIGNED_CONSUMER ->
                    !StringUtil.isNullOrEmpty(dataAddressTargetBucketName)
                    && !StringUtil.isNullOrEmpty(dataAddressTargetPath);
            case RELEASED ->
                    !StringUtil.isNullOrEmpty(dataAddressSourceBucketName)
                    && !StringUtil.isNullOrEmpty(dataAddressSourceFileName)
                    && !dataAddressSourceBucketName.equals(dataAddressTargetBucketName);
            default -> true;
        };
    }
}
