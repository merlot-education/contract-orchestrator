package eu.merloteducation.contractorchestrator.models.mappers;

import eu.merloteducation.contractorchestrator.models.dto.cooperation.CooperationContractDto;
import eu.merloteducation.contractorchestrator.models.dto.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.contractorchestrator.models.dto.saas.SaasContractDto;
import eu.merloteducation.contractorchestrator.models.entities.*;
import eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator.*;
import eu.merloteducation.contractorchestrator.models.organisationsorchestrator.OrganizationDetails;
import eu.merloteducation.contractorchestrator.models.dto.*;
import org.mapstruct.*;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    default String map(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toString() : null;
    }

    @Mapping(target = "id", source = "contract.id")
    @Mapping(target = "creationDate", source = "contract.creationDate")
    @Mapping(target = "offering", source = "offeringDetails")
    @Mapping(target = "providerLegalName",
            source = "offeringDetails.providerDetails.providerLegalName")
    @Mapping(target = "consumerLegalName",
            source = "consumerOrgaDetails.selfDescription.verifiableCredential.credentialSubject.legalName.value")
    @Mapping(target = "state", source = "contract.state")
    ContractBasicDto contractToContractBasicDto(ContractTemplate contract,
                                                OrganizationDetails consumerOrgaDetails,
                                                ServiceOfferingDetails offeringDetails);

    @Mapping(target = "type", source = "contract.type")
    @Mapping(target = "details.id", source = "contract.id")
    @Mapping(target = "details.creationDate", source = "contract.creationDate")
    @Mapping(target = "details.providerId", source = "contract.providerId")
    @Mapping(target = "details.providerLegalName",
            source = "offeringDetails.providerDetails.providerLegalName")
    @Mapping(target = "details.consumerId", source = "contract.consumerId")
    @Mapping(target = "details.consumerLegalName",
            source = "consumerOrgaDetails.selfDescription.verifiableCredential.credentialSubject.legalName.value")
    @Mapping(target = "details.state", source = "contract.state")
    @Mapping(target = "details.providerSignerUser", source = "contract.providerSignerUserId")
    @Mapping(target = "details.providerSignature", source = "contract.providerSignature")
    @Mapping(target = "details.consumerSignerUser", source = "contract.consumerSignerUserId")
    @Mapping(target = "details.consumerSignature", source = "contract.consumerSignature")
    @Mapping(target = "negotiation.runtimeSelection", source = "contract.runtimeSelection", defaultValue = "")
    @Mapping(target = "negotiation.additionalAgreements", source = "contract.additionalAgreements")
    @Mapping(target = "negotiation.attachments", source = "contract.attachments")
    @Mapping(target = "negotiation.consumerMerlotTncAccepted", source = "contract.consumerMerlotTncAccepted")
    @Mapping(target = "negotiation.consumerOfferingTncAccepted", source = "contract.consumerOfferingTncAccepted")
    @Mapping(target = "negotiation.consumerProviderTncAccepted", source = "contract.consumerProviderTncAccepted")
    @Mapping(target = "negotiation.providerMerlotTncAccepted", source = "contract.providerMerlotTncAccepted")
    @Mapping(target = "provisioning.validUntil", source = "contract.serviceContractProvisioning.validUntil")
    @Mapping(target = "offering", source = "offeringDetails")
    @Mapping(target = "offering.providerDetails.providerTncContent", source = "contract.providerTncUrl")
    @Mapping(target = "offering.providerDetails.providerTncHash", source = "contract.providerTncHash")
    ContractDto contractToContractDto(ContractTemplate contract,
                                                OrganizationDetails consumerOrgaDetails,
                                                ServiceOfferingDetails offeringDetails);

    @InheritConfiguration(name = "contractToContractDto")
    CooperationContractDto contractToContractDto(CooperationContractTemplate contract,
                                                        OrganizationDetails consumerOrgaDetails,
                                                        ServiceOfferingDetails offeringDetails);

    @InheritConfiguration(name = "contractToContractDto")
    @Mapping(target = "negotiation.userCountSelection", source = "contract.userCountSelection", defaultValue = "")
    SaasContractDto contractToContractDto(SaasContractTemplate contract,
                                                 OrganizationDetails consumerOrgaDetails,
                                                 ServiceOfferingDetails offeringDetails);

    @InheritConfiguration(name = "contractToContractDto")
    @Mapping(target = "negotiation.exchangeCountSelection", source = "contract.exchangeCountSelection", defaultValue = "")
    @Mapping(target = "provisioning.dataAddressType", source = "contract.serviceContractProvisioning.dataAddressType", defaultValue = "")
    @Mapping(target = "provisioning.dataAddressSourceBucketName", source = "contract.serviceContractProvisioning.dataAddressSourceBucketName", defaultValue = "")
    @Mapping(target = "provisioning.dataAddressSourceFileName", source = "contract.serviceContractProvisioning.dataAddressSourceFileName", defaultValue = "")
    @Mapping(target = "provisioning.selectedProviderConnectorId", source = "contract.serviceContractProvisioning.selectedProviderConnectorId", defaultValue = "")
    @Mapping(target = "provisioning.dataAddressTargetBucketName", source = "contract.serviceContractProvisioning.dataAddressTargetBucketName", defaultValue = "")
    @Mapping(target = "provisioning.dataAddressTargetFileName", source = "contract.serviceContractProvisioning.dataAddressTargetFileName", defaultValue = "")
    @Mapping(target = "provisioning.selectedConsumerConnectorId", source = "contract.serviceContractProvisioning.selectedConsumerConnectorId", defaultValue = "")
    DataDeliveryContractDto contractToContractDto(DataDeliveryContractTemplate contract,
                                                         OrganizationDetails consumerOrgaDetails,
                                                         ServiceOfferingDetails offeringDetails);



    @BeanMapping(ignoreByDefault = true)
    // allow tnc
    @Mapping(target = "consumerMerlotTncAccepted", source = "negotiation.consumerMerlotTncAccepted")
    @Mapping(target = "consumerOfferingTncAccepted", source = "negotiation.consumerOfferingTncAccepted")
    @Mapping(target = "consumerProviderTncAccepted", source = "negotiation.consumerProviderTncAccepted")
    // allow runtime selection
    @Mapping(target = "runtimeSelection", source = "negotiation.runtimeSelection")
    void updateContractAsConsumerInDraft(ContractDto source, @MappingTarget ContractTemplate target);

    @BeanMapping(ignoreByDefault = true)
    // allow tnc
    @Mapping(target = "providerMerlotTncAccepted", source = "negotiation.providerMerlotTncAccepted")
    // allow runtime selection
    @Mapping(target = "runtimeSelection", source = "negotiation.runtimeSelection")
    // allow additional agreements
    @Mapping(target = "additionalAgreements", source = "negotiation.additionalAgreements")
    // allow attachments
    @Mapping(target = "attachments", source = "negotiation.attachments")
    void updateContractAsProviderInDraft(ContractDto source, @MappingTarget ContractTemplate target);

    @BeanMapping(ignoreByDefault = true)
    // allow tnc
    @Mapping(target = "providerMerlotTncAccepted", source = "negotiation.providerMerlotTncAccepted")
    void updateContractAsProviderSignedConsumer(ContractDto source, @MappingTarget ContractTemplate target);

    @InheritConfiguration(name = "updateContractAsConsumerInDraft")
    // allow exchange count selection
    @Mapping(target = "exchangeCountSelection", source = "negotiation.exchangeCountSelection")

    void updateContractAsConsumerInDraft(DataDeliveryContractDto source, @MappingTarget DataDeliveryContractTemplate target);

    @BeanMapping(ignoreByDefault = true)
    // allow target bucket
    @Mapping(target = "dataAddressTargetBucketName", source = "provisioning.dataAddressTargetBucketName")
    // allow target file
    @Mapping(target = "dataAddressTargetFileName", source = "provisioning.dataAddressTargetFileName")
    // allow consumer connector id
    @Mapping(target = "selectedConsumerConnectorId", source = "provisioning.selectedConsumerConnectorId")
    void updateContractProvisioningAsConsumerInDraft(DataDeliveryContractDto source, @MappingTarget DataDeliveryProvisioning target);

    @InheritConfiguration(name = "updateContractAsProviderInDraft")
    // allow exchange count selection
    @Mapping(target = "exchangeCountSelection", source = "negotiation.exchangeCountSelection")
    void updateContractAsProviderInDraft(DataDeliveryContractDto source, @MappingTarget DataDeliveryContractTemplate target);

    @BeanMapping(ignoreByDefault = true)
    // allow data address type
    @Mapping(target = "dataAddressType", source = "provisioning.dataAddressType")
    // allow source bucket
    @Mapping(target = "dataAddressSourceBucketName", source = "provisioning.dataAddressSourceBucketName")
    // allow source file
    @Mapping(target = "dataAddressSourceFileName", source = "provisioning.dataAddressSourceFileName")
    // allow provider connector id
    @Mapping(target = "selectedProviderConnectorId", source = "provisioning.selectedProviderConnectorId")
    void updateContractProvisioningAsProviderInDraft(DataDeliveryContractDto source, @MappingTarget DataDeliveryProvisioning target);

    @BeanMapping(ignoreByDefault = true)
    // allow data address type
    @Mapping(target = "dataAddressType", source = "provisioning.dataAddressType")
    // allow source bucket
    @Mapping(target = "dataAddressSourceBucketName", source = "provisioning.dataAddressSourceBucketName")
    // allow source file
    @Mapping(target = "dataAddressSourceFileName", source = "provisioning.dataAddressSourceFileName")
    // allow provider connector id
    @Mapping(target = "selectedProviderConnectorId", source = "provisioning.selectedProviderConnectorId")
    void updateContractProvisioningAsProviderSignedConsumer(DataDeliveryContractDto source, @MappingTarget DataDeliveryProvisioning target);

    @InheritConfiguration(name = "updateContractAsConsumerInDraft")
    // allow user count selection
    @Mapping(target = "userCountSelection", source = "negotiation.userCountSelection")
    void updateContractAsConsumerInDraft(SaasContractDto source, @MappingTarget SaasContractTemplate target);

    @InheritConfiguration(name = "updateContractAsProviderInDraft")
    // allow user count selection
    @Mapping(target = "userCountSelection", source = "negotiation.userCountSelection")
    void updateContractAsProviderInDraft(SaasContractDto source, @MappingTarget SaasContractTemplate target);

}

