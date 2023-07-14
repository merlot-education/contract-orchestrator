package eu.merloteducation.contractorchestrator.models.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Entity
@JsonDeserialize
public class DefaultProvisioning extends ServiceContractProvisioning {
    public DefaultProvisioning() {
        super();
    }
}
