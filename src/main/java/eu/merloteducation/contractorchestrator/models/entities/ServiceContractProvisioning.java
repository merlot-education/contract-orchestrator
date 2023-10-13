package eu.merloteducation.contractorchestrator.models.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class ServiceContractProvisioning {

    @Id
    @Setter(AccessLevel.NONE)
    private String id;

    private OffsetDateTime validUntil;


    @OneToOne(mappedBy = "serviceContractProvisioning")
    private ContractTemplate contractTemplate;

    protected ServiceContractProvisioning() {
        this.id = "ServiceContractProvisioning:" + UUID.randomUUID();
    }

    protected ServiceContractProvisioning(ServiceContractProvisioning provisioning) {
        this();
    }
}
