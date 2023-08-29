package eu.merloteducation.contractorchestrator.models.organisationsorchestrator;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TermsAndConditions {

    @NotNull
    @JsonProperty("gax-trust-framework:content")
    private StringTypeValue content;

    @NotNull
    @JsonProperty("gax-trust-framework:hash")
    private StringTypeValue hash;

    @JsonProperty("@type")
    private String type;
}
