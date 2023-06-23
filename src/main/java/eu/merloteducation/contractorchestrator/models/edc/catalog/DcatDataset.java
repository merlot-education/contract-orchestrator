package eu.merloteducation.contractorchestrator.models.edc.catalog;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import eu.merloteducation.contractorchestrator.models.edc.policy.Policy;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DcatDataset {
    @JsonProperty("@id")
    private String id;

    @JsonProperty("@type")
    private String type;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @JsonProperty(EdcConstants.ODRL_PREFIX + "hasPolicy")
    private List<Policy> hasPolicy;

    @JsonProperty(EdcConstants.DCAT_PREFIX + "distribution")
    private DcatDistribution distribution;

    @JsonProperty(EdcConstants.EDC_PREFIX + "version")
    private String version;

    @JsonProperty(EdcConstants.EDC_PREFIX + "name")
    private String name;

    @JsonProperty(EdcConstants.EDC_PREFIX + "description")
    private String description;

    @JsonProperty(EdcConstants.EDC_PREFIX + "id")
    private String assetId;

    @JsonProperty(EdcConstants.EDC_PREFIX + "contenttype")
    private String contenttype;
}
