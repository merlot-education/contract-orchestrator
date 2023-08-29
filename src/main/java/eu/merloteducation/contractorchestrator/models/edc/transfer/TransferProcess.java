package eu.merloteducation.contractorchestrator.models.edc.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public abstract class TransferProcess {

    @JsonProperty("@context")
    private Map<String, String> context;

    @JsonProperty("@id")
    private String id;

    @JsonProperty("@type")
    private String type;

    @JsonProperty(EdcConstants.EDC_PREFIX + "state")
    private String state;

    @JsonProperty(EdcConstants.EDC_PREFIX + "stateTimestamp")
    private String stateTimestamp;

    @JsonProperty(EdcConstants.EDC_PREFIX + "type")
    private String edcType;

    @JsonProperty(EdcConstants.EDC_PREFIX + "callbackAddresses")
    private List<String> callbackAddresses;

    @JsonProperty(EdcConstants.EDC_PREFIX + "dataRequest")
    private DataRequest dataRequest;
}
