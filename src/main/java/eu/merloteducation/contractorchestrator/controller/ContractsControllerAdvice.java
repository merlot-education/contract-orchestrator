/*
 *  Copyright 2023-2024 Dataport AöR
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.merloteducation.contractorchestrator.controller;

import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.modelslib.api.contract.ContractDto;
import eu.merloteducation.modelslib.api.contract.views.ContractViews;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;

import java.util.Objects;

@ControllerAdvice(assignableTypes = ContractsController.class)
public class ContractsControllerAdvice extends AbstractMappingJacksonResponseBodyAdvice {
    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, @NotNull MediaType contentType,
                                           @NotNull MethodParameter returnType, @NotNull ServerHttpRequest req,
                                           @NotNull ServerHttpResponse res) {
        ServletServerHttpRequest request = (ServletServerHttpRequest) req;

        if (!(bodyContainer.getValue() instanceof ContractDto contractDto)) {
            return;
        }

        bodyContainer.setSerializationView(ContractViews.DetailedView.class);

        if (!request.getHeaders().containsKey("Active-Role")) {
            return;
        }

        String activeRoleString = request.getHeaders().getFirst("Active-Role");
        if (activeRoleString == null) {
            return;
        }

        OrganizationRoleGrantedAuthority activeOrgaRole = new OrganizationRoleGrantedAuthority(activeRoleString);
        if (!activeOrgaRole.isRepresentative()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (contractDto.getDetails().getProviderId().equals(activeOrgaRole.getOrganizationId())) {
            bodyContainer.setSerializationView(ContractViews.ProviderView.class);
        } else if (contractDto.getDetails().getConsumerId().equals(activeOrgaRole.getOrganizationId())) {
            bodyContainer.setSerializationView(ContractViews.ConsumerView.class);
        }
    }
}
