package eu.merloteducation.contractorchestrator.models.edc.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import eu.merloteducation.contractorchestrator.models.edc.asset.DataAddress;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString
@Builder
public class TransferRequest {
    private static final Map<String, String> CONTEXT = EdcConstants.EDC_CONTEXT;

    private static final String TYPE = EdcConstants.EDC_PREFIX + "TransferRequestDto";

    private static final boolean MANAGED_RESOURCES = false;

    private static final String PROTOCOL = "dataspace-protocol-http";

    @JsonProperty(EdcConstants.EDC_PREFIX + "connectorId")
    private String connectorId;

    @JsonProperty(EdcConstants.EDC_PREFIX + "connectorAddress")
    private String connectorAddress;

    @JsonProperty(EdcConstants.EDC_PREFIX + "contractId")
    private String contractId;

    @JsonProperty(EdcConstants.EDC_PREFIX + "assetId")
    private String assetId;

    @JsonProperty(EdcConstants.EDC_PREFIX + "dataDestination")
    private DataAddress dataDestination;

    @JsonProperty("@type")
    public String getType() {
        return TYPE;
    }

    @JsonProperty(EdcConstants.EDC_PREFIX + "protocol")
    public String getProtocol() {
        return PROTOCOL;
    }

    @JsonProperty("@context")
    public Map<String, String> getContext() {
        return CONTEXT;
    }

    @JsonProperty(EdcConstants.EDC_PREFIX + "managedResources")
    public boolean isManagedResources() {
        return MANAGED_RESOURCES;
    }
}
