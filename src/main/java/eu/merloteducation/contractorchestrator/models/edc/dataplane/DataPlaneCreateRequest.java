package eu.merloteducation.contractorchestrator.models.edc.dataplane;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DataPlaneCreateRequest {
    @JsonProperty("edctype")
    private static final String EDCTYPE = "dataspaceconnector:dataplaneinstance";

    private String id;

    private String url;

    private List<String> allowedSourceTypes;

    private List<String> allowedDestTypes;

    private Map<String, String> properties;

}
