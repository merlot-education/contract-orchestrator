package eu.merloteducation.contractorchestrator.models.mappers;

import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ConsumerTransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ProviderTransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension.IonosS3ConsumerTransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension.IonosS3ProviderTransferProvisioning;
import eu.merloteducation.modelslib.api.contract.datadelivery.ConsumerTransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ProviderTransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ionoss3extension.IonosS3ConsumerTransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ionoss3extension.IonosS3ProviderTransferProvisioningDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ContractFromDtoMapper {
    @Mapping(target = "selectedConsumerConnectorId", source = "selectedConsumerConnectorId")
    @Mapping(target = "dataAddressTargetBucketName", source = "dataAddressTargetBucketName")
    @Mapping(target = "dataAddressTargetPath", source = "dataAddressTargetPath")
    IonosS3ConsumerTransferProvisioning ionosProvisioningDtoToConsumerProvisioning(IonosS3ConsumerTransferProvisioningDto dto);

    @Mapping(target = "selectedProviderConnectorId", source = "selectedProviderConnectorId")
    @Mapping(target = "dataAddressSourceBucketName", source = "dataAddressSourceBucketName")
    @Mapping(target = "dataAddressSourceFileName", source = "dataAddressSourceFileName")
    IonosS3ProviderTransferProvisioning ionosProvisioningDtoToProviderProvisioning(IonosS3ProviderTransferProvisioningDto dto);

    @Named("transferProvisioningDtoToProvisioning")
    default ConsumerTransferProvisioning transferProvisioningDtoToProvisioning(ConsumerTransferProvisioningDto provisioning) {
        if (provisioning == null) {
            return null;
        }
        if (provisioning instanceof IonosS3ConsumerTransferProvisioningDto ionosProvisioning) {
            return ionosProvisioningDtoToConsumerProvisioning(ionosProvisioning);
        }
        throw new IllegalArgumentException("Unknown consumer transfer provisioning type.");
    }

    @Named("transferProvisioningDtoToProvisioning")
    default ProviderTransferProvisioning transferProvisioningDtoToProvisioning(ProviderTransferProvisioningDto provisioning) {
        if (provisioning == null) {
            return null;
        }
        if (provisioning instanceof IonosS3ProviderTransferProvisioningDto ionosProvisioning) {
            return ionosProvisioningDtoToProviderProvisioning(ionosProvisioning);
        }
        throw new IllegalArgumentException("Unknown consumer transfer provisioning type.");
    }
}
