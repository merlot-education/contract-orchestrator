package eu.merloteducation.contractorchestrator.models.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class ServiceContractProvisioning {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue
    private Long id;

    private OffsetDateTime validUntil;


    @OneToOne(mappedBy = "serviceContractProvisioning")
    private ContractTemplate contractTemplate;

    public boolean transitionAllowed(ContractState targetState) {
        return targetState != null; // validUntil can be null if selected time is unlimited
    }

    public abstract ServiceContractProvisioning makeCopy();
}
