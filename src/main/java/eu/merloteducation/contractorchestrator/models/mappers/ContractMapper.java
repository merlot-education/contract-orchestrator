package eu.merloteducation.contractorchestrator.models.mappers;

import eu.merloteducation.contractorchestrator.models.OfferingDetails;
import eu.merloteducation.contractorchestrator.models.OrganizationDetails;
import eu.merloteducation.contractorchestrator.models.dto.*;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.CooperationContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.DataDeliveryContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.SaasContractTemplate;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    default String map(OffsetDateTime offsetDateTime) {
        return offsetDateTime.toString();
    }

    @Mapping(target = "id", source = "contract.id")
    @Mapping(target = "providerLegalName", source = "providerOrgaDetails.organizationLegalName")
    @Mapping(target = "consumerLegalName", source = "consumerOrgaDetails.organizationLegalName")
    ContractBasicDto contractToContractBasicDto(ContractTemplate contract,
                                                OrganizationDetails providerOrgaDetails,
                                                OrganizationDetails consumerOrgaDetails,
                                                OfferingDetails offeringDetails);

    @InheritConfiguration
    @Mapping(target = "offeringTermsAndConditions", source = "offeringDetails.termsAndConditions")
    @Mapping(target = "validUntil", source = "contract.serviceContractProvisioning.validUntil")
    ContractDetailsDto contractToContractDetailsDto(ContractTemplate contract,
                                                    OrganizationDetails providerOrgaDetails,
                                                    OrganizationDetails consumerOrgaDetails,
                                                    OfferingDetails offeringDetails);

    @InheritConfiguration(name = "contractToContractDetailsDto")
    CooperationContractDetailsDto contractToContractDetailsDto(CooperationContractTemplate contract,
                                                               OrganizationDetails providerOrgaDetails,
                                                               OrganizationDetails consumerOrgaDetails,
                                                               OfferingDetails offeringDetails);

    @InheritConfiguration(name = "contractToContractDetailsDto")
    SaasContractDetailsDto contractToContractDetailsDto(SaasContractTemplate contract,
                                                        OrganizationDetails providerOrgaDetails,
                                                        OrganizationDetails consumerOrgaDetails,
                                                        OfferingDetails offeringDetails);

    @InheritConfiguration(name = "contractToContractDetailsDto")
    @Mapping(target = "dataAddressType", source = "contract.serviceContractProvisioning.dataAddressType")
    @Mapping(target = "dataAddressSourceBucketName", source = "contract.serviceContractProvisioning.dataAddressSourceBucketName")
    @Mapping(target = "dataAddressSourceFileName", source = "contract.serviceContractProvisioning.dataAddressSourceFileName")
    @Mapping(target = "selectedProviderConnectorId", source = "contract.serviceContractProvisioning.selectedProviderConnectorId")
    @Mapping(target = "dataAddressTargetBucketName", source = "contract.serviceContractProvisioning.dataAddressTargetBucketName")
    @Mapping(target = "dataAddressTargetFileName", source = "contract.serviceContractProvisioning.dataAddressTargetFileName")
    @Mapping(target = "selectedConsumerConnectorId", source = "contract.serviceContractProvisioning.selectedConsumerConnectorId")
    DataDeliveryContractDetailsDto contractToContractDetailsDto(DataDeliveryContractTemplate contract,
                                                        OrganizationDetails providerOrgaDetails,
                                                        OrganizationDetails consumerOrgaDetails,
                                                        OfferingDetails offeringDetails);
}

