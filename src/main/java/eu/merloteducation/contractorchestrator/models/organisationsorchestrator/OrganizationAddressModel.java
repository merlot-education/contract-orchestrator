package eu.merloteducation.contractorchestrator.models.organisationsorchestrator;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrganizationAddressModel {
    private String countryCode;
    private String postalCode;
    private String addressCode;
    private String city;
    private String street;
}
