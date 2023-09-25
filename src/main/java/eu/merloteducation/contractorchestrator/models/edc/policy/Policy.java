package eu.merloteducation.contractorchestrator.models.edc.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Policy {

    private static final String TYPE = EdcConstants.ODRL_PREFIX + "Set";

    @JsonProperty("@id")
    private String id;

    @JsonProperty(EdcConstants.ODRL_PREFIX + "permission")
    private List<String> permission; // TODO replace this with proper classes once needed

    @JsonProperty(EdcConstants.ODRL_PREFIX + "prohibition")
    private List<String> prohibition; // TODO replace this with proper classes once needed

    @JsonProperty(EdcConstants.ODRL_PREFIX + "obligation")
    private List<String> obligation; // TODO replace this with proper classes once needed

    @JsonProperty(EdcConstants.ODRL_PREFIX + "target")
    private String target;

    @JsonProperty("@type")
    public String getType() {
        return TYPE;
    }
}
