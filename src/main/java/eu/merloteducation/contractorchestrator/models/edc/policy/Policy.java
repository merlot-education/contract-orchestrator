package eu.merloteducation.contractorchestrator.models.edc.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Policy {

    private static final String TYPE = EdcConstants.ODRL_PREFIX + "Set";

    @JsonProperty(EdcConstants.ODRL_PREFIX + "permissions")
    private static final List<String> PERMISSIONS = Collections.emptyList(); // TODO replace this with proper classes once needed

    @JsonProperty("@type")
    public String getType() {
        return TYPE;
    }

    @JsonProperty(EdcConstants.ODRL_PREFIX + "permissions")
    public List<String> getPermissions() {
        return PERMISSIONS;
    }
}
