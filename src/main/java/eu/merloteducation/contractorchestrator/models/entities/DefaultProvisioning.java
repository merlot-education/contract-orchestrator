package eu.merloteducation.contractorchestrator.models.entities;

import jakarta.persistence.Entity;

@Entity
public class DefaultProvisioning extends ServiceContractProvisioning {
    public DefaultProvisioning() {
        super();
    }

    @Override
    public ServiceContractProvisioning makeCopy() {
        return new DefaultProvisioning();
    }

}
