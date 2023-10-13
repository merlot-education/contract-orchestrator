package eu.merloteducation.contractorchestrator.models.entities;

import jakarta.persistence.Entity;

@Entity
public class DefaultProvisioning extends ServiceContractProvisioning {
    public DefaultProvisioning() {
        super();
    }

    public DefaultProvisioning(DefaultProvisioning provisioning) {
        super(provisioning);
    }
}
