package eu.merloteducation.contractorchestrator.models.edc.contractdefinition;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Criterion {

    @JsonProperty("@type")
    private String type = EdcConstants.EDC_PREFIX + "CriterionDto";

    @JsonProperty(EdcConstants.EDC_PREFIX + "operandLeft")
    private Object operandLeft;

    @JsonProperty(EdcConstants.EDC_PREFIX + "operator")
    private String operator;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @JsonProperty(EdcConstants.EDC_PREFIX + "operandRight")
    private Object operandRight;
}
