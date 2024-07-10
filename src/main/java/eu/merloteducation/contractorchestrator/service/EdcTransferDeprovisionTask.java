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

public class EdcTransferDeprovisionTask implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(EdcTransferDeprovisionTask.class);
    private final EdcClient edcClient;
    private final String transferId;

    public EdcTransferDeprovisionTask(EdcClient edcClient, String transferId) {
        this.edcClient = edcClient;
        this.transferId = transferId;
    }
    @Override
    public void run() {
        logger.info("Deprovisioning transfer with id {}", transferId);
        this.edcClient.deprovisionTransfer(this.transferId);
    }
}
