package eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceOfferingDetails {
    private JsonNode metadata;
    private JsonNode selfDescription;
    private ProviderDetails providerDetails;
}
