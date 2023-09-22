package eu.merloteducation.contractorchestrator.models.edc.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public abstract class DataAddress {
    private static final String TYPE = EdcConstants.EDC_PREFIX + "DataAddress";

    @JsonProperty("@type")
    public String getType() {
        return TYPE;
    }
}
