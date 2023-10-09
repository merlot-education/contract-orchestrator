package eu.merloteducation.contractorchestrator.models.entities;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class SaasContractTemplate extends ContractTemplate {

    private String userCountSelection;

    public SaasContractTemplate() {
        super();
    }

    public SaasContractTemplate(SaasContractTemplate template, boolean regenerate) {
        super(template, regenerate);
        this.userCountSelection = template.getUserCountSelection();
    }

    @Override
    public void transitionState(ContractState targetState) {
        if (targetState == ContractState.SIGNED_CONSUMER
                && (userCountSelection == null || userCountSelection.isEmpty())) {
            throw new IllegalStateException(
                    String.format("Cannot transition from state %s to %s as mandatory fields are not set",
                            getState().name(), targetState.name()));
        }
        if (targetState == ContractState.REVOKED) {
            throw new IllegalStateException(
                    String.format("Not allowed to transition from state %s to %s for this contract type",
                            getState().name(), targetState.name()));
        }
        super.transitionState(targetState);
    }
}
