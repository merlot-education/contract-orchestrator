package eu.merloteducation.contractorchestrator.models.edc.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class Policy {

    private static final String TYPE = EdcConstants.ODRL_PREFIX + "Set";

    @JsonProperty("@id")
    private String id;

    @JsonProperty(EdcConstants.ODRL_PREFIX + "permission")
    private List<String> permission = Collections.emptyList(); // TODO replace this with proper classes once needed

    @JsonProperty(EdcConstants.ODRL_PREFIX + "prohibition")
    private List<String> prohibition = Collections.emptyList(); // TODO replace this with proper classes once needed

    @JsonProperty(EdcConstants.ODRL_PREFIX + "obligation")
    private List<String> obligation = Collections.emptyList(); // TODO replace this with proper classes once needed

    @JsonProperty(EdcConstants.ODRL_PREFIX + "target")
    private String target;

    public Policy(String id) {
        this.id = id;
    }

    @JsonProperty("@type")
    public String getType() {
        return TYPE;
    }
}
