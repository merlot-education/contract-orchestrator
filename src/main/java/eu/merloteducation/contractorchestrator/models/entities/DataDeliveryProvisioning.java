package eu.merloteducation.contractorchestrator.models.entities;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class DataDeliveryProvisioning extends ServiceContractProvisioning {
    private String dataAddressType;

    private String dataAddressSourceBucketName;

    private String dataAddressSourceFileName;

    private String selectedProviderConnectorId;

    private String dataAddressTargetBucketName;

    private String dataAddressTargetFileName;

    private String selectedConsumerConnectorId;

    public DataDeliveryProvisioning() {
        super();
        this.dataAddressType = "";
        this.dataAddressSourceBucketName = "";
        this.dataAddressSourceFileName = "";
        this.dataAddressTargetBucketName = "";
        this.dataAddressTargetFileName = "";
    }

    public DataDeliveryProvisioning(DataDeliveryProvisioning provisioning) {
        super(provisioning);
        this.dataAddressType = provisioning.getDataAddressType();
        this.dataAddressSourceBucketName = provisioning.getDataAddressSourceBucketName();
        this.dataAddressSourceFileName = provisioning.getDataAddressSourceFileName();
        this.dataAddressTargetBucketName = provisioning.getDataAddressTargetBucketName();
        this.dataAddressTargetFileName = provisioning.getDataAddressTargetFileName();
        this.selectedConsumerConnectorId = provisioning.getSelectedConsumerConnectorId();
        this.selectedProviderConnectorId = provisioning.getSelectedProviderConnectorId();
    }
}
