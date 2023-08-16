package eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DataDeliveryOfferingDetails extends OfferingDetails {
    private String dataTransferType;
    private List<OfferingExchangeCountOption> exchangeCountOption;
}
