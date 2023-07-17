package eu.merloteducation.contractorchestrator.models.edc.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;

public class DcatService {
    @JsonProperty("@id")
    private String id;

    @JsonProperty("@type")
    private String type;

    @JsonProperty(EdcConstants.DCT_PREFIX + "terms")
    private String terms;

    @JsonProperty(EdcConstants.DCT_PREFIX + "endpointUrl")
    private String endpointUrl;
}
