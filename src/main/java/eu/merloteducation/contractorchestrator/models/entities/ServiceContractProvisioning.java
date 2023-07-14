package eu.merloteducation.contractorchestrator.models.entities;

import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@ToString
@Entity
public class ServiceContractProvisioning {
    // TODO table with all parameters related to number of exchanges, data transfer parameters that can change during contract lifetime etc...

    @Id
    @JsonView(ContractViews.BasicView.class)
    @Setter(AccessLevel.NONE)
    private String id;

    @JsonView(ContractViews.ProviderView.class)
    private String dataAddressName;

    @JsonView(ContractViews.ProviderView.class)
    private String dataAddressType;

    @JsonView(ContractViews.ProviderView.class)
    private String dataAddressSourceBucketName;

    @JsonView(ContractViews.ProviderView.class)
    private String dataAddressSourceFileName;

    @JsonView(ContractViews.ConsumerView.class)
    private String dataAddressTargetBucketName;

    @JsonView(ContractViews.ConsumerView.class)
    private String dataAddressTargetFileName;

    @OneToOne(mappedBy = "serviceContractProvisioning")
    @JsonView(ContractViews.InternalView.class)
    private DataDeliveryContractTemplate contractTemplate;

    public ServiceContractProvisioning() {
        this.id = "ServiceContractProvisioning:" + UUID.randomUUID();
        this.dataAddressName = "";
        this.dataAddressType = "";
        this.dataAddressSourceBucketName = "";
        this.dataAddressSourceFileName = "";
        this.dataAddressTargetBucketName = "";
        this.dataAddressTargetFileName = "";
    }
}
