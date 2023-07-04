package eu.merloteducation.contractorchestrator.models.edc.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HttpDataAddress extends DataAddress {

    private static final String TYPE = "HttpData";

    @JsonProperty(EdcConstants.EDC_PREFIX + "properties")
    private HttpDataAddressProperties properties;

    @JsonProperty(EdcConstants.EDC_PREFIX + "type")
    public String getDataType() {
        return TYPE;
    }
}
