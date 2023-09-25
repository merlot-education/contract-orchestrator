package eu.merloteducation.contractorchestrator.models.edc.negotiation;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString
@Builder
public class NegotiationInitiateRequest {
    private static final Map<String, String> CONTEXT = EdcConstants.EDC_CONTEXT;

    private static final String TYPE = EdcConstants.EDC_PREFIX + "NegotiationInitiateRequestDto";

    private static final String PROTOCOL = "dataspace-protocol-http";

    @JsonProperty(EdcConstants.EDC_PREFIX + "connectorId")
    private String connectorId;

    @JsonProperty(EdcConstants.EDC_PREFIX + "consumerId")
    private String consumerId;

    @JsonProperty(EdcConstants.EDC_PREFIX + "providerId")
    private String providerId;

    @JsonProperty(EdcConstants.EDC_PREFIX + "connectorAddress")
    private String connectorAddress;

    @JsonProperty(EdcConstants.EDC_PREFIX + "offer")
    private ContractOffer offer;

    @JsonProperty("@context")
    public Map<String, String> getContext() {
        return CONTEXT;
    }

    @JsonProperty("@type")
    public String getType() {
        return TYPE;
    }

    @JsonProperty(EdcConstants.EDC_PREFIX + "protocol")
    public String getProtocol() {
        return PROTOCOL;
    }
}
