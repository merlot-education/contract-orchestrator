package eu.merloteducation.contractorchestrator.models.entities;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Entity
@JsonDeserialize
public class DataDeliveryProvisioning extends ServiceContractProvisioning {
    @JsonView(ContractViews.ProviderView.class)
    private String dataAddressName;

    @JsonView(ContractViews.ProviderView.class)
    private String dataAddressType;

    @JsonView(ContractViews.ProviderView.class)
    private String dataAddressSourceBucketName;

    @JsonView(ContractViews.ProviderView.class)
    private String dataAddressSourceFileName;

    @JsonView(ContractViews.ProviderView.class)
    private String selectedProviderConnectorId;

    @JsonView(ContractViews.ConsumerView.class)
    private String dataAddressTargetBucketName;

    @JsonView(ContractViews.ConsumerView.class)
    private String dataAddressTargetFileName;

    @JsonView(ContractViews.ConsumerView.class)
    private String selectedConsumerConnectorId;

    public DataDeliveryProvisioning() {
        super();
        this.dataAddressName = "";
        this.dataAddressType = "";
        this.dataAddressSourceBucketName = "";
        this.dataAddressSourceFileName = "";
        this.dataAddressTargetBucketName = "";
        this.dataAddressTargetFileName = "";
    }

    public DataDeliveryProvisioning(DataDeliveryProvisioning provisioning) {
        super(provisioning);
        this.dataAddressName = provisioning.getDataAddressName();
        this.dataAddressType = provisioning.getDataAddressType();
        this.dataAddressSourceBucketName = provisioning.getDataAddressSourceBucketName();
        this.dataAddressSourceFileName = provisioning.getDataAddressSourceFileName();
        this.dataAddressTargetBucketName = provisioning.getDataAddressTargetBucketName();
        this.dataAddressTargetFileName = provisioning.getDataAddressTargetFileName();
        this.selectedConsumerConnectorId = provisioning.getSelectedConsumerConnectorId();
        this.selectedProviderConnectorId = provisioning.getSelectedProviderConnectorId();
    }
}
