package eu.merloteducation.contractorchestrator.models.edc.contractdefinition;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
@Builder
public class ContractDefinitionCreateRequest {
    private static final Map<String, String> CONTEXT = EdcConstants.EDC_CONTEXT;

    private static final String TYPE = EdcConstants.EDC_PREFIX + "ContractDefinition";

    private List<Criterion> assetsSelector;

    @JsonProperty("@id")
    private String id;

    @JsonProperty(EdcConstants.EDC_PREFIX + "accessPolicyId")
    private String accessPolicyId;

    @JsonProperty(EdcConstants.EDC_PREFIX + "contractPolicyId")
    private String contractPolicyId;

    @JsonProperty("@context")
    public Map<String, String> getContext() {
        return CONTEXT;
    }

    @JsonProperty("@type")
    public String getType() {
        return TYPE;
    }
}
