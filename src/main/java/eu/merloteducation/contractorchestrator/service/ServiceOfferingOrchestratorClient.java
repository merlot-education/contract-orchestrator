package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;

import java.util.Map;

public interface ServiceOfferingOrchestratorClient {
    @GetExchange("/serviceoffering/{offeringId}")
    ServiceOfferingDto getOfferingDetails(@PathVariable String offeringId, @RequestHeader Map<String, String> headers);

}
