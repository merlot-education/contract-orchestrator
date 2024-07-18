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

package eu.merloteducation.contractorchestrator.models.mappers;

import eu.merloteducation.contractorchestrator.models.entities.datadelivery.TransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension.IonosS3ConsumerTransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension.IonosS3ProviderTransferProvisioning;
import eu.merloteducation.modelslib.api.contract.datadelivery.TransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ionoss3extension.IonosS3ConsumerTransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ionoss3extension.IonosS3ProviderTransferProvisioningDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ContractFromDtoMapper {
    @Mapping(target = "selectedConnectorId", source = "selectedConnectorId")
    @Mapping(target = "dataAddressTargetBucketName", source = "dataAddressTargetBucketName")
    @Mapping(target = "dataAddressTargetPath", source = "dataAddressTargetPath")
    IonosS3ConsumerTransferProvisioning ionosProvisioningDtoToConsumerProvisioning(IonosS3ConsumerTransferProvisioningDto dto);

    @Mapping(target = "selectedConnectorId", source = "selectedConnectorId")
    @Mapping(target = "dataAddressSourceBucketName", source = "dataAddressSourceBucketName")
    @Mapping(target = "dataAddressSourceFileName", source = "dataAddressSourceFileName")
    IonosS3ProviderTransferProvisioning ionosProvisioningDtoToProviderProvisioning(IonosS3ProviderTransferProvisioningDto dto);

    @Named("transferProvisioningDtoToProvisioning")
    default TransferProvisioning transferProvisioningDtoToProvisioning(TransferProvisioningDto provisioning) {
        if (provisioning == null) {
            return null;
        }
        if (provisioning instanceof IonosS3ConsumerTransferProvisioningDto ionosProvisioning) {
            return ionosProvisioningDtoToConsumerProvisioning(ionosProvisioning);
        }
        if (provisioning instanceof IonosS3ProviderTransferProvisioningDto ionosProvisioning) {
            return ionosProvisioningDtoToProviderProvisioning(ionosProvisioning);
        }
        throw new IllegalArgumentException("Unknown transfer provisioning type.");
    }
}
