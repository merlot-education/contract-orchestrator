package eu.merloteducation.contractorchestrator.config;

import eu.merloteducation.contractorchestrator.service.OrganizationOrchestratorClient;
import eu.merloteducation.contractorchestrator.service.ServiceOfferingOrchestratorClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class AppConfig {

    @Value("${serviceoffering-orchestrator.base-uri}")
    private String serviceOfferingOrchestratorBaseUri;

    @Value("${organizations-orchestrator.base-uri}")
    private String organizationsOrchestratorBaseUri;

    @Bean
    public ServiceOfferingOrchestratorClient serviceOfferingOrchestratorClient() {
        WebClient webClient = WebClient.builder()
                .baseUrl(serviceOfferingOrchestratorBaseUri)
                .build();
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(webClient))
                .build();
        return httpServiceProxyFactory.createClient(ServiceOfferingOrchestratorClient.class);
    }

    @Bean
    public OrganizationOrchestratorClient organizationOrchestratorClient() {
        WebClient webClient = WebClient.builder()
                .baseUrl(organizationsOrchestratorBaseUri)
                .build();
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(webClient))
                .build();
        return httpServiceProxyFactory.createClient(OrganizationOrchestratorClient.class);
    }
}
