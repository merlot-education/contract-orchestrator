package eu.merloteducation.contractorchestrator.models.edc.negotiation;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import eu.merloteducation.contractorchestrator.models.edc.policy.Policy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContractOffer {
    private static final String TYPE = EdcConstants.EDC_PREFIX + "ContractOfferDescription";

    @JsonProperty(EdcConstants.EDC_PREFIX + "offerId")
    private String offerId;

    @JsonProperty(EdcConstants.EDC_PREFIX + "assetId")
    private String assetId;

    @JsonProperty(EdcConstants.EDC_PREFIX + "policy")
    private Policy policy;

    @JsonProperty("@type")
    public String getType() {
        return TYPE;
    }
}
