package eu.merloteducation.contractorchestrator.models.edc.negotiation;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ContractNegotiation {
    @JsonProperty("@type")
    private String type;

    @JsonProperty("@id")
    private String id;

    @JsonProperty(EdcConstants.EDC_PREFIX + "type")
    private String edcType;

    @JsonProperty(EdcConstants.EDC_PREFIX + "protocol")
    private String protocol;

    @JsonProperty(EdcConstants.EDC_PREFIX + "state")
    private String state;

    @JsonProperty(EdcConstants.EDC_PREFIX + "counterPartyAddress")
    private String counterPartyAddress;

    @JsonProperty(EdcConstants.EDC_PREFIX + "callbackAddresses")
    private List<String> callbackAddresses;

    @JsonProperty(EdcConstants.EDC_PREFIX + "contractAgreementId")
    private String contractAgreementId;

    @JsonProperty("@context")
    private Map<String, String> context;
}
