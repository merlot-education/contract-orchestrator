package eu.merloteducation.contractorchestrator.models.entities.cooperation;

import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.DefaultProvisioning;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class CooperationContractTemplate extends ContractTemplate {

    public CooperationContractTemplate() {
        super();
    }

    public CooperationContractTemplate(CooperationContractTemplate template, boolean regenerate) {
        super(template, regenerate);
    }

    @Override
    public DefaultProvisioning getServiceContractProvisioning() {
        return (DefaultProvisioning) super.getServiceContractProvisioning();
    }

    @Override
    public boolean transitionAllowed(ContractState targetState) {
        return super.transitionAllowed(targetState) && switch (targetState) {
            case REVOKED -> false;
            default -> true;
        };
    }
}
