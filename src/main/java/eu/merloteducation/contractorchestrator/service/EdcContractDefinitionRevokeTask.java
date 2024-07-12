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

package eu.merloteducation.contractorchestrator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EdcContractDefinitionRevokeTask implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(EdcContractDefinitionRevokeTask.class);
    private final EdcClient edcClient;
    private final String contractDefinitionId;

    public EdcContractDefinitionRevokeTask(EdcClient edcClient, String contractDefinitionId) {
        this.edcClient = edcClient;
        this.contractDefinitionId = contractDefinitionId;
    }
    @Override
    public void run() {
        logger.info("Revoking contract definition with id {}", contractDefinitionId);
        this.edcClient.revokeContractDefinition(this.contractDefinitionId);
    }
}
