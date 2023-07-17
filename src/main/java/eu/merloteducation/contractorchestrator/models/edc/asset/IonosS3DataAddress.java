package eu.merloteducation.contractorchestrator.models.edc.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class IonosS3DataAddress extends DataAddress{

    private static final String TYPE = "IonosS3";

    @JsonProperty(EdcConstants.EDC_PREFIX + "name")
    private String name;

    @JsonProperty(EdcConstants.EDC_PREFIX + "bucketName")
    private String bucketName;

    @JsonProperty(EdcConstants.EDC_PREFIX + "container")
    private String container;

    @JsonProperty(EdcConstants.EDC_PREFIX + "blobName")
    private String blobName;

    @JsonProperty(EdcConstants.EDC_PREFIX + "keyName")
    private String keyName;

    @JsonProperty(EdcConstants.EDC_PREFIX + "storage")
    private String storage;

    @JsonProperty(EdcConstants.EDC_PREFIX + "type")
    public String getDataType() {
        return TYPE;
    }
}
