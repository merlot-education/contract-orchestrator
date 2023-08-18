package eu.merloteducation.contractorchestrator.models.organisationsorchestrator;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrganizationDetails {
    private String id;
    private String organizationLegalName;
    private String termsAndConditionsLink;
}
