package eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplateDeserializer;
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

    private List<OfferingTerms> termsAndConditions;
    private List<String> attachments;
    private String state;
    private String name;
    private String offeredBy;
    private String type;
    private List<OfferingRuntimeOption> runtimeOption;
}
