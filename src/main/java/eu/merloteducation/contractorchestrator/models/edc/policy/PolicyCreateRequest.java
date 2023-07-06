package eu.merloteducation.contractorchestrator.models.edc.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class PolicyCreateRequest {
    private static final Map<String, String> CONTEXT = EdcConstants.EDC_CONTEXT;

    @JsonProperty("@id")
    private String id;

    @JsonProperty(EdcConstants.EDC_PREFIX + "policy")
    private Policy policy;

    @JsonProperty("@context")
    public Map<String, String> getContext() {
        return CONTEXT;
    }
}
