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

package eu.merloteducation.contractorchestrator.auth;

import eu.merloteducation.authorizationlibrary.authorization.AuthorityChecker;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component("contractAuthorityChecker")
public class ContractAuthorityChecker {

    @Autowired
    private ContractTemplateRepository contractTemplateRepository;

    @Autowired
    private AuthorityChecker authorityChecker;

    private boolean canAccessContract(Authentication authentication, ContractTemplate template) {
        return this.isContractConsumer(authentication, template) || this.isContractProvider(authentication, template);
    }

    private boolean isContractProvider(Authentication authentication, ContractTemplate template) {
        Set<String> representedOrgaIds = authorityChecker.getRepresentedOrgaIds(authentication);
        if (template != null) {
            String providerId = template.getProviderId();
            return representedOrgaIds.contains(providerId);
        }
        return false;
    }

    private boolean isContractConsumer(Authentication authentication, ContractTemplate template) {
        Set<String> representedOrgaIds = authorityChecker.getRepresentedOrgaIds(authentication);
        if (template != null) {
            String consumerId = template.getConsumerId();
            return representedOrgaIds.contains(consumerId);
        }
        return false;
    }


    /**
     * Given the current authentication and a contract id, check whether the requesting party either
     * represents the consumer or provider of this contract.
     *
     * @param authentication current authentication
     * @param contractId     id of the contract to request
     * @return can access the requested contract
     */
    public boolean canAccessContract(Authentication authentication, String contractId) {
        ContractTemplate template = contractTemplateRepository.findById(contractId).orElse(null);
        return this.canAccessContract(authentication, template);
    }

    public boolean isContractProvider(Authentication authentication, String contractId) {
        ContractTemplate template = contractTemplateRepository.findById(contractId).orElse(null);
        return this.isContractProvider(authentication, template);
    }
}
