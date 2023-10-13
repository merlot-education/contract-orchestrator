package eu.merloteducation.contractorchestrator.models.entities;

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
    public void transitionState(ContractState targetState) {
        if (targetState == ContractState.REVOKED) {
            throw new IllegalStateException(
                    String.format("Not allowed to transition from state %s to %s for this contract type",
                            getState().name(), targetState.name()));
        }
        super.transitionState(targetState);
    }
}
