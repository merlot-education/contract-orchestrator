package eu.merloteducation.contractorchestrator.models.organisationsorchestrator;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StringTypeValue {
    @JsonProperty("@value")
    private String value;

    public StringTypeValue(String value) {
        this.value = value;
    }
}