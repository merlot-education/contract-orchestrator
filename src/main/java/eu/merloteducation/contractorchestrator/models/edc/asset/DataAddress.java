package eu.merloteducation.contractorchestrator.models.edc.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataAddress {
    private static final String TYPE = EdcConstants.EDC_PREFIX + "DataAddress";

    @JsonProperty(EdcConstants.EDC_PREFIX + "properties")
    private DataAddressProperties properties;

    @JsonProperty("@type")
    public String getType() {
        return TYPE;
    }
}
