package eu.merloteducation.contractorchestrator.models;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrganizationDetails {
    private String id;
    private String merlotId;
    private String organizationName;
    private String organizationLegalName;
    private String registrationNumber;
    private String termsAndConditionsLink;
    private OrganizationAddressModel legalAddress;

    private String connectorId;
    private String connectorPublicKey;
    private String connectorBaseUrl;
}
