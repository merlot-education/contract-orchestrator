package eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible=true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DataDeliveryOfferingDetails.class, name = "merlot:MerlotServiceOfferingDataDelivery"),
        @JsonSubTypes.Type(value = SaasOfferingDetails.class, name = "merlot:MerlotServiceOfferingSaaS"),
        @JsonSubTypes.Type(value = CooperationContractOfferingDetails.class, name = "merlot:MerlotServiceOfferingCooperation")
})
public abstract class OfferingDetails {

    private String id;
    private String sdHash;
    private String name;
    private String creationDate;
    private String offeredBy;
    private String merlotState;
    private String type;

    private String description;
    private String modifiedDate;
    private String exampleCosts;
    private List<String> attachments;
    private List<OfferingTerms> termsAndConditions;
    private List<OfferingRuntimeOption> runtimeOption;
    private boolean merlotTermsAndConditionsAccepted;
}
