package eu.merloteducation.contractorchestrator.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OfferingDetails {

    private List<OfferingTerms> termsAndConditions;
    private String state;
    private String type;
    private String dataTransferType;
}
