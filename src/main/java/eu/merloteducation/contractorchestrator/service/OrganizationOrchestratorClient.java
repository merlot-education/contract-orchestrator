package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.models.organisationsorchestrator.OrganizationDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;

import java.util.Map;

public interface OrganizationOrchestratorClient {
    @GetExchange("/organization/{orgaId}")
    OrganizationDetails getOrganizationDetails(@PathVariable String orgaId, @RequestHeader Map<String, String> headers);
}
