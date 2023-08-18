package eu.merloteducation.contractorchestrator.models.dto;

import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ContractDetailsDto extends ContractBasicDto {

    private String type;

    private String providerTncUrl;

    private String runtimeSelection;

    private String additionalAgreements;

    @JsonView(ContractViews.ConsumerView.class)
    private boolean consumerMerlotTncAccepted;

    @JsonView(ContractViews.ConsumerView.class)
    private boolean consumerOfferingTncAccepted;

    @JsonView(ContractViews.ConsumerView.class)
    private boolean consumerProviderTncAccepted;

    @JsonView(ContractViews.ProviderView.class)
    private boolean providerMerlotTncAccepted;

    private String validUntil;
}
