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
import org.jetbrains.annotations.Contract;

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
    private String dataAddressSourceBucketName;

    @JsonView(ContractViews.ProviderView.class)
    private String dataAddressSourceFileName;

    @JsonView(ContractViews.ConsumerView.class)
    private String dataAddressTargetBucketName;

    @JsonView(ContractViews.ConsumerView.class)
    private String dataAddressTargetFileName;

    @JsonView(ContractViews.ProviderView.class)
    private String dataAddressType;

    @OneToOne(mappedBy = "serviceContractProvisioning")
    private ContractTemplate contractTemplate;
}
