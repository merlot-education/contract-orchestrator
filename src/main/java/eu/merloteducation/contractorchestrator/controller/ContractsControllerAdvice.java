package eu.merloteducation.contractorchestrator.controller;

import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;

import java.util.Objects;

@ControllerAdvice(assignableTypes = ContractsController.class)
public class ContractsControllerAdvice extends AbstractMappingJacksonResponseBodyAdvice {
    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, @NotNull MediaType contentType,
                                           @NotNull MethodParameter returnType, @NotNull ServerHttpRequest req,
                                           @NotNull ServerHttpResponse res) {
        ServletServerHttpRequest request = (ServletServerHttpRequest)req;

        if (bodyContainer.getValue() instanceof ContractTemplate contractTemplate) {
            bodyContainer.setSerializationView(ContractViews.DetailedView.class);
            if (request.getHeaders().containsKey("Active-Role")) {
                String activeRoleOrgaId = Objects.requireNonNull(request.getHeaders().getFirst("Active-Role"))
                        .replaceFirst("(OrgLegRep|OrgRep)_", "");
                if (contractTemplate.getProviderId().replace("Participant:", "")
                        .equals(activeRoleOrgaId)) {
                    bodyContainer.setSerializationView(ContractViews.ProviderView.class);
                } else if (contractTemplate.getConsumerId().replace("Participant:", "")
                        .equals(activeRoleOrgaId)) {
                    bodyContainer.setSerializationView(ContractViews.ConsumerView.class);
                }
            }
        }
    }
}
