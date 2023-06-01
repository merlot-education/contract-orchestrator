package eu.merloteducation.contractorchestrator.models;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractCreateRequest {

    @NotNull
    private String consumerId;

    @NotNull
    private String offeringId;

}
