package eu.merloteducation.contractorchestrator.models.organisationsorchestrator;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StringTypeValue {
    @JsonProperty("@value")
    private String value;
}
