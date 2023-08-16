package eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SaasOfferingDetails extends OfferingDetails {
    private List<OfferingUserCountOption> userCountOption;
}
