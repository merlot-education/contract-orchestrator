package eu.merloteducation.contractorchestrator.models.mappers;

import eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator.*;
import eu.merloteducation.contractorchestrator.models.organisationsorchestrator.OrganizationDetails;
import eu.merloteducation.contractorchestrator.models.dto.*;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.CooperationContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.DataDeliveryContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.SaasContractTemplate;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    default String map(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toString() : null;
    }

    @Mapping(target = "id", source = "contract.id")
    @Mapping(target = "state", source = "contract.state")
    @Mapping(target = "creationDate", source = "contract.creationDate")
    @Mapping(target = "providerLegalName", source = "providerOrgaDetails.organizationLegalName")
    @Mapping(target = "consumerLegalName", source = "consumerOrgaDetails.organizationLegalName")
    @Mapping(target = "offering", source = "offeringDetails")
    ContractBasicDto contractToContractBasicDto(ContractTemplate contract,
                                                OrganizationDetails providerOrgaDetails,
                                                OrganizationDetails consumerOrgaDetails,
                                                ServiceOfferingDetails offeringDetails);

    @InheritConfiguration
    @Mapping(target = "type", source = "contract.type")
    @Mapping(target = "validUntil", source = "contract.serviceContractProvisioning.validUntil")
    ContractDetailsDto contractToContractDetailsDto(ContractTemplate contract,
                                                    OrganizationDetails providerOrgaDetails,
                                                    OrganizationDetails consumerOrgaDetails,
                                                    ServiceOfferingDetails offeringDetails);

    @InheritConfiguration(name = "contractToContractDetailsDto")
    CooperationContractDetailsDto contractToContractDetailsDto(CooperationContractTemplate contract,
                                                               OrganizationDetails providerOrgaDetails,
                                                               OrganizationDetails consumerOrgaDetails,
                                                               ServiceOfferingDetails offeringDetails);

    @InheritConfiguration(name = "contractToContractDetailsDto")
    SaasContractDetailsDto contractToContractDetailsDto(SaasContractTemplate contract,
                                                        OrganizationDetails providerOrgaDetails,
                                                        OrganizationDetails consumerOrgaDetails,
                                                        ServiceOfferingDetails offeringDetails);

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
                                                                ServiceOfferingDetails offeringDetails);
}

