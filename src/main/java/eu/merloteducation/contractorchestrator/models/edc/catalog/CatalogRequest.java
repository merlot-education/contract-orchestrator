package eu.merloteducation.contractorchestrator.models.edc.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class CatalogRequest {
    private static final Map<String, String> CONTEXT = EdcConstants.EDC_CONTEXT;

    private static final String PROTOCOL = "dataspace-protocol-http";

    @JsonProperty(EdcConstants.EDC_PREFIX + "providerUrl")
    private String providerUrl;

    @JsonProperty("@context")
    public Map<String, String> getContext() {
        return CONTEXT;
    }

    @JsonProperty(EdcConstants.EDC_PREFIX + "protocol")
    public String getProtocol() {
        return PROTOCOL;
    }
}
