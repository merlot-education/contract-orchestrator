package eu.merloteducation.contractorchestrator.models.entities;

import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@ToString
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name="discriminator")
public class ServiceContractProvisioning {
    // TODO table with all parameters related to number of exchanges, data transfer parameters that can change during contract lifetime etc...

    @Id
    @JsonView(ContractViews.BasicView.class)
    @Setter(AccessLevel.NONE)
    private String id;

    @JsonView(ContractViews.DetailedView.class)
    private String validUntil;

    @OneToOne(mappedBy = "serviceContractProvisioning")
    @JsonView(ContractViews.InternalView.class)
    private ContractTemplate contractTemplate;

    public ServiceContractProvisioning() {
        this.id = "ServiceContractProvisioning:" + UUID.randomUUID();
    }
}
