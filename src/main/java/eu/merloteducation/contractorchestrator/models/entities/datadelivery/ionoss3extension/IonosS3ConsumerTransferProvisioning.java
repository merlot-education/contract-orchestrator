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

package eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension;

import eu.merloteducation.contractorchestrator.models.entities.datadelivery.DataDeliveryProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.TransferProvisioning;
import io.netty.util.internal.StringUtil;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
public class IonosS3ConsumerTransferProvisioning extends TransferProvisioning {

    private String dataAddressTargetBucketName;

    private String dataAddressTargetPath;

    public IonosS3ConsumerTransferProvisioning() {
        super();
        this.dataAddressTargetBucketName = "";
        this.dataAddressTargetPath = "";
    }

    @Override
    public boolean configurationValid() {
        return super.configurationValid()
                && !StringUtil.isNullOrEmpty(dataAddressTargetBucketName)
                && !StringUtil.isNullOrEmpty(dataAddressTargetPath);
    }

    @Override
    public boolean commonConfigurationValid(DataDeliveryProvisioning provisioning) {
        boolean valid = super.commonConfigurationValid(provisioning);

        if (provisioning.getProviderTransferProvisioning() instanceof IonosS3ProviderTransferProvisioning providerProv) {
            // if both are ionos, the bucket name is not allowed to be equal
            valid &= !this.getDataAddressTargetBucketName().equals(providerProv.getDataAddressSourceBucketName());
        } else if (provisioning.getProviderTransferProvisioning() != null) {
            valid = false; // for now we only allow if both provider and consumer provisioning are IONOS.
        }

        return valid;
    }

    @Override
    public TransferProvisioning makeCopy() {
        IonosS3ConsumerTransferProvisioning provisioning = new IonosS3ConsumerTransferProvisioning();
        provisioning.setSelectedConnectorId(getSelectedConnectorId());
        provisioning.setDataAddressTargetPath(getDataAddressTargetPath());
        provisioning.setDataAddressTargetBucketName(getDataAddressTargetBucketName());
        return provisioning;
    }
}
