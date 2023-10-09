package eu.merloteducation.contractorchestrator.models.mappers;

import eu.merloteducation.contractorchestrator.models.dto.cooperation.CooperationContractDto;
import eu.merloteducation.contractorchestrator.models.dto.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.contractorchestrator.models.dto.saas.SaasContractDto;
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
    @Mapping(target = "creationDate", source = "contract.creationDate")
    @Mapping(target = "offering", source = "offeringDetails")
    @Mapping(target = "providerId",
            source = "offeringDetails.providerDetails.providerId")
    @Mapping(target = "providerLegalName",
            source = "offeringDetails.providerDetails.providerLegalName")
    @Mapping(target = "consumerId",
            source = "consumerOrgaDetails.selfDescription.verifiableCredential.credentialSubject.id")
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
    @Mapping(target = "details.termsAndConditions", source = "contract.termsAndConditions")
    @Mapping(target = "negotiation.runtimeSelection", source = "contract.runtimeSelection", defaultValue = "")
    @Mapping(target = "negotiation.additionalAgreements", source = "contract.additionalAgreements")
    @Mapping(target = "negotiation.attachments", source = "contract.offeringAttachments")
    @Mapping(target = "negotiation.consumerMerlotTncAccepted", source = "contract.consumerMerlotTncAccepted")
    @Mapping(target = "negotiation.consumerOfferingTncAccepted", source = "contract.consumerOfferingTncAccepted")
    @Mapping(target = "negotiation.consumerProviderTncAccepted", source = "contract.consumerProviderTncAccepted")
    @Mapping(target = "negotiation.providerMerlotTncAccepted", source = "contract.providerMerlotTncAccepted")
    @Mapping(target = "provisioning.validUntil", source = "contract.serviceContractProvisioning.validUntil")
    @Mapping(target = "offering", source = "offeringDetails")
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
}

