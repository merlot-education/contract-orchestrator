/*
 *  Copyright 2024 Dataport. All rights reserved. Developed as part of the MERLOT project.
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

package eu.merloteducation.contractorchestrator.models.entities.saas;

import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.DefaultProvisioning;
import io.netty.util.internal.StringUtil;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class SaasContractTemplate extends ContractTemplate {

    private String userCountSelection;

    public SaasContractTemplate() {
        super();
    }

    public SaasContractTemplate(SaasContractTemplate template, boolean regenerate) {
        super(template, regenerate);
        this.userCountSelection = template.getUserCountSelection();
    }

    @Override
    public DefaultProvisioning getServiceContractProvisioning() {
        return (DefaultProvisioning) super.getServiceContractProvisioning();
    }

    @Override
    public boolean transitionAllowed(ContractState targetState) {
        return super.transitionAllowed(targetState) && switch (targetState) {
            case SIGNED_CONSUMER ->
                    !StringUtil.isNullOrEmpty(userCountSelection);
            case REVOKED -> false;
            default -> true;
        };
    }
}
