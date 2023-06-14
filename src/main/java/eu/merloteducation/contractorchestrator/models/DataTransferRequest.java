package eu.merloteducation.contractorchestrator.models;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataTransferRequest {

    @NotNull
    private String contractId;

    @NotNull
    private String dataType;

    @NotNull
    private String targetBaseUrl;
}
