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

package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import org.springframework.stereotype.Service;

@Service
public class ContractSignerService {

    /**
     * Given a contract object and a signing key, generate a signature for this contract.
     * NOTE: this method needs to be extended later to actually sign, currently it just passes through the input value.
     *
     * @param template   contract object to sign
     * @param signingKey key to use to sign
     */
    public String generateContractSignature(ContractTemplate template, String signingKey) {
        return signingKey;
    }
}
