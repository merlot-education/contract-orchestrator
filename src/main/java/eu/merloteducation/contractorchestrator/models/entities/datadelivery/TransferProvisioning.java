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

package eu.merloteducation.contractorchestrator.models.entities.datadelivery;

import io.netty.util.internal.StringUtil;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class TransferProvisioning {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue
    private Long id;

    private String selectedConnectorId;

    protected TransferProvisioning() {
        selectedConnectorId = "";
    }

    public boolean configurationValid() {
        return !StringUtil.isNullOrEmpty(selectedConnectorId);
    }

    public boolean commonConfigurationValid(DataDeliveryProvisioning provisioning) {
        boolean result = true;

        TransferProvisioning consumerProv = provisioning.getConsumerTransferProvisioning();
        TransferProvisioning providerProv = provisioning.getProviderTransferProvisioning();
        if (consumerProv != null && providerProv != null) {
            result = !consumerProv.getSelectedConnectorId().equals(providerProv.getSelectedConnectorId());
        }

        return result;
    }

    public abstract TransferProvisioning makeCopy();
}
