package eu.merloteducation.contractorchestrator.models.organisationsorchestrator;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationCredentialSubject {
    @JsonProperty("@id")
    private String id;

    @JsonProperty("gax-trust-framework:legalName")
    private StringTypeValue legalName;

    @JsonProperty("merlot:termsConditionsLink")
    private StringTypeValue termsAndConditionsLink;


}
