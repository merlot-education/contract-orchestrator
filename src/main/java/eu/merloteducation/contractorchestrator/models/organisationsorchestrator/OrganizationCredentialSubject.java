package eu.merloteducation.contractorchestrator.models.organisationsorchestrator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationCredentialSubject {
    @JsonProperty("@id")
    private String id;

    @JsonProperty("gax-trust-framework:legalName")
    private StringTypeValue legalName;

    @JsonProperty("gax-trust-framework:legalAddress")
    private JsonNode legalAddress;

    @JsonProperty("merlot:termsAndConditions")
    private TermsAndConditions termsAndConditions;
}
