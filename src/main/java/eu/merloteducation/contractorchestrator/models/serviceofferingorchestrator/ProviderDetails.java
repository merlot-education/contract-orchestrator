package eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProviderDetails {
    private String providerId;
    private String providerLegalName;

    private String providerTncContent;
    private String providerTncHash;
}
