package eu.merloteducation.contractorchestrator.models.edc.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class DcatCatalog {
    @JsonProperty("@id")
    private String id;

    @JsonProperty("@type")
    private String type;

    @JsonProperty(EdcConstants.EDC_PREFIX + "participantId")
    private String participantId;

    @JsonProperty("@context")
    private Map<String, String> context;

    @JsonProperty(EdcConstants.DCAT_PREFIX + "dataset")
    private DcatDataset dataset;

    @JsonProperty(EdcConstants.DCAT_PREFIX + "service")
    private DcatService service;
}
