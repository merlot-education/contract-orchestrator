package eu.merloteducation.contractorchestrator.models;

import eu.merloteducation.contractorchestrator.models.edc.common.IdResponse;
import lombok.Getter;

@Getter
public class EdcIdResponse {

    private final String id;

    public EdcIdResponse(IdResponse response) {
        this.id = response.getId();
    }
}
