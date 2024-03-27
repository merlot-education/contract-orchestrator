package eu.merloteducation.contractorchestrator.models.mappers;

import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.cooperation.CooperationContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ConsumerTransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.DataDeliveryContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.DataDeliveryProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ProviderTransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension.IonosS3ConsumerTransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension.IonosS3ProviderTransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.saas.SaasContractTemplate;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.VCard;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.DataDeliveryCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.SaaSCredentialSubject;
import eu.merloteducation.modelslib.api.contract.*;
import eu.merloteducation.modelslib.api.contract.cooperation.CooperationContractDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ConsumerTransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.DataDeliveryContractProvisioningDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ProviderTransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ionoss3extension.IonosS3ConsumerTransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ionoss3extension.IonosS3ProviderTransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.saas.SaasContractDto;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", imports = { DataDeliveryCredentialSubject.class, SaaSCredentialSubject.class })
public interface ContractMapper {

    default String map(OffsetDateTime offsetDateTime) {

        return offsetDateTime != null ? offsetDateTime.toString() : null;
    }

    @Named("mapParticipantLegalName")
    default String mapParticipantLegalName(MerlotParticipantDto consumerOrgaDetails) {
        return ((MerlotOrganizationCredentialSubject)
                consumerOrgaDetails.getSelfDescription().getVerifiableCredential().getCredentialSubject())
                .getLegalName();
    }

    @Named("mapParticipantLegalAddress")
    default VCard mapParticipantLegalAddress(MerlotParticipantDto consumerOrgaDetails) {
        return ((MerlotOrganizationCredentialSubject)
                consumerOrgaDetails.getSelfDescription().getVerifiableCredential().getCredentialSubject())
                .getLegalAddress();
    }

    @Named("mapOfferingName")
    default String mapOfferingName(ServiceOfferingDto offeringDto) {
        String name = ((MerlotServiceOfferingCredentialSubject)
                offeringDto.getSelfDescription().getVerifiableCredential().getCredentialSubject())
                .getName();
        return name == null ? "" : name;
    }

    @Named("mapOfferingDescription")
    default String mapOfferingDescription(ServiceOfferingDto offeringDto) {
        String description = ((MerlotServiceOfferingCredentialSubject)
                offeringDto.getSelfDescription().getVerifiableCredential().getCredentialSubject())
                .getDescription();
        return description == null ? "" : description;
    }

    @Named("mapOfferingExampleCosts")
    default String mapOfferingExampleCosts(ServiceOfferingDto offeringDto) {
        String exampleCosts = ((MerlotServiceOfferingCredentialSubject)
                offeringDto.getSelfDescription().getVerifiableCredential().getCredentialSubject())
                .getExampleCosts();
        return exampleCosts == null ? "" : exampleCosts;
    }

    @Mapping(target = "id", source = "contract.id")
    @Mapping(target = "creationDate", source = "contract.creationDate")
    @Mapping(target = "offering", source = "offeringDetails")
    @Mapping(target = "providerId", source = "offeringDetails.providerDetails.providerId")
    @Mapping(target = "providerLegalName", source = "offeringDetails.providerDetails.providerLegalName")
    @Mapping(target = "providerActive", source = "providerOrgaDetails.metadata.active")
    @Mapping(target = "consumerId", source = "consumerOrgaDetails.selfDescription.verifiableCredential.credentialSubject.id")
    @Mapping(target = "consumerLegalName", source = "consumerOrgaDetails", qualifiedByName = "mapParticipantLegalName")
    @Mapping(target = "consumerActive", source = "consumerOrgaDetails.metadata.active")
    @Mapping(target = "state", source = "contract.state")
    ContractBasicDto contractToContractBasicDto(ContractTemplate contract, MerlotParticipantDto providerOrgaDetails,
                                                MerlotParticipantDto consumerOrgaDetails,ServiceOfferingDto offeringDetails);

    @Mapping(target = "type", source = "contract.type")
    @Mapping(target = "details.id", source = "contract.id")
    @Mapping(target = "details.creationDate", source = "contract.creationDate")
    @Mapping(target = "details.providerId", source = "contract.providerId")
    @Mapping(target = "details.providerLegalName", source = "offeringDetails.providerDetails.providerLegalName")
    @Mapping(target = "details.providerLegalAddress", source = "providerOrgaDetails", qualifiedByName = "mapParticipantLegalAddress")
    @Mapping(target = "details.providerActive", source = "providerOrgaDetails.metadata.active")
    @Mapping(target = "details.consumerId", source = "contract.consumerId")
    @Mapping(target = "details.consumerLegalName", source = "consumerOrgaDetails", qualifiedByName = "mapParticipantLegalName")
    @Mapping(target = "details.consumerLegalAddress", source = "consumerOrgaDetails", qualifiedByName = "mapParticipantLegalAddress")
    @Mapping(target = "details.consumerActive", source = "consumerOrgaDetails.metadata.active")
    @Mapping(target = "details.state", source = "contract.state")
    @Mapping(target = "details.providerSignerUserName", source = "contract.providerSignature.signerName")
    @Mapping(target = "details.providerSignature", source = "contract.providerSignature.signature")
    @Mapping(target = "details.providerSignatureDate", source = "contract.providerSignature.signatureDate")
    @Mapping(target = "details.consumerSignerUserName", source = "contract.consumerSignature.signerName")
    @Mapping(target = "details.consumerSignature", source = "contract.consumerSignature.signature")
    @Mapping(target = "details.consumerSignatureDate", source = "contract.consumerSignature.signatureDate")
    @Mapping(target = "details.termsAndConditions", source = "contract.termsAndConditions")
    @Mapping(target = "negotiation.runtimeSelection", source = "contract.runtimeSelection", defaultValue = "")
    @Mapping(target = "negotiation.additionalAgreements", source = "contract.additionalAgreements")
    @Mapping(target = "negotiation.attachments", source = "contract.attachments")
    @Mapping(target = "negotiation.consumerTncAccepted", source = "contract.consumerTncAccepted")
    @Mapping(target = "negotiation.consumerAttachmentsAccepted", source = "contract.consumerAttachmentsAccepted")
    @Mapping(target = "negotiation.providerTncAccepted", source = "contract.providerTncAccepted")
    @Mapping(target = "provisioning.validUntil", source = "contract.serviceContractProvisioning.validUntil")
    @Mapping(target = "offering", source = "offeringDetails")
    ContractDto contractToContractDto(ContractTemplate contract, MerlotParticipantDto providerOrgaDetails,
                                      MerlotParticipantDto consumerOrgaDetails, ServiceOfferingDto offeringDetails);

    @InheritConfiguration(name = "contractToContractDto")
    CooperationContractDto contractToContractDto(CooperationContractTemplate contract,
                                                 MerlotParticipantDto providerOrgaDetails, MerlotParticipantDto consumerOrgaDetails,
                                                 ServiceOfferingDto offeringDetails);

    @InheritConfiguration(name = "contractToContractDto")
    @Mapping(target = "negotiation.userCountSelection", source = "contract.userCountSelection", defaultValue = "")
    SaasContractDto contractToContractDto(SaasContractTemplate contract, MerlotParticipantDto providerOrgaDetails,
                                          MerlotParticipantDto consumerOrgaDetails, ServiceOfferingDto offeringDetails);

    @InheritConfiguration(name = "contractToContractDto")
    @Mapping(target = "negotiation.exchangeCountSelection", source = "contract.exchangeCountSelection", defaultValue = "")
    @Mapping(target = "provisioning.consumerTransferProvisioning", source = "contract.serviceContractProvisioning.consumerTransferProvisioning", qualifiedByName = "transferProvisioningToDto")
    @Mapping(target = "provisioning.providerTransferProvisioning", source = "contract.serviceContractProvisioning.providerTransferProvisioning", qualifiedByName = "transferProvisioningToDto")
    DataDeliveryContractDto contractToContractDto(DataDeliveryContractTemplate contract,
                                                  MerlotParticipantDto providerOrgaDetails, MerlotParticipantDto consumerOrgaDetails,
                                                  ServiceOfferingDto offeringDetails);

    @Named("transferProvisioningToDto")
    default ConsumerTransferProvisioningDto transferProvisioningToDto(ConsumerTransferProvisioning provisioning) {
        if (provisioning instanceof IonosS3ConsumerTransferProvisioning ionosProvisioning) {
            return ionosProvisioningToConsumerProvisioningDto(ionosProvisioning);
        }
        throw new IllegalArgumentException("Unknown consumer transfer provisioning type.");
    }

    @Named("transferProvisioningToDto")
    default ProviderTransferProvisioningDto transferProvisioningToDto(ProviderTransferProvisioning provisioning) {
        if (provisioning instanceof IonosS3ProviderTransferProvisioning ionosProvisioning) {
            return ionosProvisioningToProviderProvisioningDto(ionosProvisioning);
        }
        throw new IllegalArgumentException("Unknown consumer transfer provisioning type.");
    }

    @Named("transferProvisioningDtoToProvisioning")
    default ConsumerTransferProvisioning transferProvisioningDtoToProvisioning(ConsumerTransferProvisioningDto provisioning) {
        if (provisioning instanceof IonosS3ConsumerTransferProvisioningDto ionosProvisioning) {
            return ionosProvisioningDtoToConsumerProvisioning(ionosProvisioning);
        }
        throw new IllegalArgumentException("Unknown consumer transfer provisioning type.");
    }

    @Named("transferProvisioningDtoToProvisioning")
    default ProviderTransferProvisioning transferProvisioningDtoToProvisioning(ProviderTransferProvisioningDto provisioning) {
        if (provisioning instanceof IonosS3ProviderTransferProvisioningDto ionosProvisioning) {
            return ionosProvisioningDtoToProviderProvisioning(ionosProvisioning);
        }
        throw new IllegalArgumentException("Unknown consumer transfer provisioning type.");
    }

    @Mapping(target = "selectedConsumerConnectorId", source = "selectedConsumerConnectorId")
    @Mapping(target = "dataAddressTargetBucketName", source = "dataAddressTargetBucketName")
    @Mapping(target = "dataAddressTargetPath", source = "dataAddressTargetPath")
    IonosS3ConsumerTransferProvisioning ionosProvisioningDtoToConsumerProvisioning(IonosS3ConsumerTransferProvisioningDto dto);

    @Mapping(target = "selectedProviderConnectorId", source = "selectedProviderConnectorId")
    @Mapping(target = "dataAddressSourceBucketName", source = "dataAddressSourceBucketName")
    @Mapping(target = "dataAddressSourceFileName", source = "dataAddressSourceFileName")
    IonosS3ProviderTransferProvisioning ionosProvisioningDtoToProviderProvisioning(IonosS3ProviderTransferProvisioningDto dto);

    @Mapping(target = "selectedConsumerConnectorId", source = "selectedConsumerConnectorId")
    @Mapping(target = "dataAddressTargetBucketName", source = "dataAddressTargetBucketName")
    @Mapping(target = "dataAddressTargetPath", source = "dataAddressTargetPath")
    @Mapping(target = "dataAddressType", constant = "IonosS3")
    IonosS3ConsumerTransferProvisioningDto ionosProvisioningToConsumerProvisioningDto(IonosS3ConsumerTransferProvisioning provisioning);

    @Mapping(target = "selectedProviderConnectorId", source = "selectedProviderConnectorId")
    @Mapping(target = "dataAddressSourceBucketName", source = "dataAddressSourceBucketName")
    @Mapping(target = "dataAddressSourceFileName", source = "dataAddressSourceFileName")
    @Mapping(target = "dataAddressType", constant = "IonosS3")
    IonosS3ProviderTransferProvisioningDto ionosProvisioningToProviderProvisioningDto(IonosS3ProviderTransferProvisioning provisioning);

    @Mapping(target = "contractId", expression = "java(contractDto.getDetails().getId().replace(\"Contract:\", \"\"))")
    @Mapping(target = "contractCreationDate", source = "contractDto.details.creationDate")
    @Mapping(target = "contractRuntime", source = "contractDto.negotiation.runtimeSelection", qualifiedByName = "contractRuntime")
    @Mapping(target = "contractTnc", source = "contractDto.details.termsAndConditions", qualifiedByName = "contractTnc")
    @Mapping(target = "contractAttachmentFilenames", expression = "java(contractDto.getNegotiation().getAttachments().stream().toList())")
    @Mapping(target = "serviceId", expression = "java(contractDto.getOffering().getSelfDescription().getVerifiableCredential().getCredentialSubject().getId().replace(\"ServiceOffering:\", \"\"))")
    @Mapping(target = "serviceType", source = "contractDto.offering.selfDescription.verifiableCredential.credentialSubject.type")
    @Mapping(target = "serviceName", source = "contractDto.offering", qualifiedByName = "mapOfferingName")
    @Mapping(target = "serviceDescription", source = "contractDto.offering", qualifiedByName = "mapOfferingDescription")
    @Mapping(target = "serviceExampleCosts", source = "contractDto.offering", qualifiedByName = "mapOfferingExampleCosts")
    @Mapping(target = "providerLegalName", source = "contractDto.details.providerLegalName")
    @Mapping(target = "providerLegalAddress", source = "contractDto.details.providerLegalAddress", qualifiedByName = "legalAddress")
    @Mapping(target = "providerSignerUser", source = "contractDto.details.providerSignerUserName")
    @Mapping(target = "providerSignature", source = "contractDto.details.providerSignature")
    @Mapping(target = "providerSignatureTimestamp", source = "contractDto.details.providerSignatureDate")
    @Mapping(target = "consumerLegalName", source = "contractDto.details.consumerLegalName")
    @Mapping(target = "consumerLegalAddress", source = "contractDto.details.consumerLegalAddress", qualifiedByName = "legalAddress")
    @Mapping(target = "consumerSignerUser", source = "contractDto.details.consumerSignerUserName")
    @Mapping(target = "consumerSignature", source = "contractDto.details.consumerSignature")
    @Mapping(target = "consumerSignatureTimestamp", source = "contractDto.details.consumerSignatureDate")
    ContractPdfDto contractDtoToContractPdfDto(ContractDto contractDto);

    @InheritConfiguration(name = "contractDtoToContractPdfDto")
    @Mapping(target = "contractDataTransferCount", source = "contractDto.negotiation.exchangeCountSelection")
    @Mapping(target = "serviceDataAccessType", expression = "java(((DataDeliveryCredentialSubject) contractDto.getOffering().getSelfDescription().getVerifiableCredential().getCredentialSubject()).getDataAccessType())")
    @Mapping(target = "serviceDataTransferType", expression = "java(((DataDeliveryCredentialSubject) contractDto.getOffering().getSelfDescription().getVerifiableCredential().getCredentialSubject()).getDataTransferType())")
    ContractPdfDto contractDtoToContractPdfDto(DataDeliveryContractDto contractDto);

    @InheritConfiguration(name = "contractDtoToContractPdfDto")
    @Mapping(target = "contractUserCount", source = "contractDto.negotiation.userCountSelection")
    @Mapping(target = "serviceHardwareRequirements", expression = "java(((SaaSCredentialSubject) contractDto.getOffering().getSelfDescription().getVerifiableCredential().getCredentialSubject()).getHardwareRequirements())")
    ContractPdfDto contractDtoToContractPdfDto(SaasContractDto contractDto);

    @InheritConfiguration(name = "contractDtoToContractPdfDto")
    ContractPdfDto contractDtoToContractPdfDto(CooperationContractDto contractDto);

    @Named("contractTnc")
    default List<Map<String, String>> tncMapper(List<ContractTncDto> termsAndConditions) {

        return termsAndConditions.stream().map(contractTncDto -> {
            Map<String, String> map = new HashMap<>();
            map.put("tncLink", contractTncDto.getContent());
            map.put("tncHash", contractTncDto.getHash());
            return map;
        }).toList();
    }

    @Named("legalAddress")
    default ContractPdfAddressDto legalAddressMapper(VCard legalAddress) {
        ContractPdfAddressDto contractPdfAddressDto = new ContractPdfAddressDto();
        contractPdfAddressDto.setCountryName(legalAddress.getCountryName());
        contractPdfAddressDto.setStreetAddress(legalAddress.getStreetAddress());
        contractPdfAddressDto.setLocality(legalAddress.getLocality());
        contractPdfAddressDto.setPostalCode(legalAddress.getPostalCode());

        return contractPdfAddressDto;
    }

    @Named("contractRuntime")
    default String contractRuntimeMapper(String runtimeEnglish) {

        String unlimited = "unlimited";

        Map<String, String> map = new HashMap<>();
        map.put("hour(s)", "Stunde(n)");
        map.put("day(s)", "Tag(e)");
        map.put("week(s)", "Woche(n)");
        map.put("month(s)", "Monat(e)");
        map.put("year(s)", "Jahr(e)");
        map.put(unlimited, "Unbegrenzt");

        String[] runtimeSplit = runtimeEnglish.split(" ");
        String runtimeCount = runtimeSplit[0];
        String runtimeMeasurementEnglish = runtimeSplit[1];

        if (Integer.parseInt(runtimeCount) == 0 || runtimeMeasurementEnglish.equals(unlimited)) {
            return map.get(unlimited);
        }

        String runtimeMeasurementGerman = map.get(runtimeMeasurementEnglish);
        return runtimeCount + " " + runtimeMeasurementGerman;
    }
}

