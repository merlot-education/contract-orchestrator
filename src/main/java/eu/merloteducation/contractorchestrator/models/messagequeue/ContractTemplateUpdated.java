package eu.merloteducation.contractorchestrator.models.messagequeue;

import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ContractTemplateUpdated {
    @NotNull
    private final String contractId;

    @NotNull
    private final String serviceOfferingId;

    public ContractTemplateUpdated(ContractTemplate contract) {
        this.contractId = contract.getId();
        this.serviceOfferingId = contract.getOfferingId();
    }
}
