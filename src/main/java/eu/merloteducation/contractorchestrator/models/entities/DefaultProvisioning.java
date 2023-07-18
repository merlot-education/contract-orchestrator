package eu.merloteducation.contractorchestrator.models.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.Entity;

@Entity
@JsonDeserialize
public class DefaultProvisioning extends ServiceContractProvisioning {
    public DefaultProvisioning() {
        super();
    }

    public DefaultProvisioning(DefaultProvisioning provisioning) {
        super(provisioning);
    }
}
