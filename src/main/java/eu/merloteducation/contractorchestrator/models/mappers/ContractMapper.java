package eu.merloteducation.contractorchestrator.models.mappers;

import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.CooperationContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.DataDeliveryContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.SaasContractTemplate;
import eu.merloteducation.modelslib.api.contract.*;
import eu.merloteducation.modelslib.api.contract.cooperation.CooperationContractDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.modelslib.api.contract.saas.SaasContractDto;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import eu.merloteducation.modelslib.gxfscatalog.datatypes.StringTypeValue;
import eu.merloteducation.modelslib.gxfscatalog.datatypes.VCard;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptions.serviceofferings.DataDeliveryCredentialSubject;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptions.serviceofferings.SaaSCredentialSubject;
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

    default String mapStringTypeValue(StringTypeValue value) {
        return value != null ? value.getValue() : null;
    }

    @Mapping(target = "id", source = "contract.id")
    @Mapping(target = "creationDate", source = "contract.creationDate")
    @Mapping(target = "offering", source = "offeringDetails")
    @Mapping(target = "providerId", source = "offeringDetails.providerDetails.providerId")
    @Mapping(target = "providerLegalName", source = "offeringDetails.providerDetails.providerLegalName")
    @Mapping(target = "consumerId", source = "consumerOrgaDetails.selfDescription.verifiableCredential.credentialSubject.id")
    @Mapping(target = "consumerLegalName", source = "consumerOrgaDetails.selfDescription.verifiableCredential.credentialSubject.legalName")
    @Mapping(target = "state", source = "contract.state")
    ContractBasicDto contractToContractBasicDto(ContractTemplate contract, MerlotParticipantDto consumerOrgaDetails,
                                                ServiceOfferingDto offeringDetails);

    @Mapping(target = "type", source = "contract.type")
    @Mapping(target = "details.id", source = "contract.id")
    @Mapping(target = "details.creationDate", source = "contract.creationDate")
    @Mapping(target = "details.providerId", source = "contract.providerId")
    @Mapping(target = "details.providerLegalName", source = "offeringDetails.providerDetails.providerLegalName")
    @Mapping(target = "details.providerLegalAddress", source = "providerOrgaDetails.selfDescription.verifiableCredential.credentialSubject.legalAddress")
    @Mapping(target = "details.consumerId", source = "contract.consumerId")
    @Mapping(target = "details.consumerLegalName", source = "consumerOrgaDetails.selfDescription.verifiableCredential.credentialSubject.legalName")
    @Mapping(target = "details.consumerLegalAddress", source = "consumerOrgaDetails.selfDescription.verifiableCredential.credentialSubject.legalAddress")
    @Mapping(target = "details.state", source = "contract.state")
    @Mapping(target = "details.providerSignerUserName", source = "contract.providerSignature.signerName")
    @Mapping(target = "details.providerSignature", source = "contract.providerSignature.signature")
    @Mapping(target = "details.providerSignatureDate", source = "contract.providerSignature.signatureDate")
    @Mapping(target = "details.consumerSignerUserName", source = "contract.providerSignature.signerName")
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
    @Mapping(target = "provisioning.dataAddressType", source = "contract.serviceContractProvisioning.dataAddressType", defaultValue = "")
    @Mapping(target = "provisioning.dataAddressSourceBucketName", source = "contract.serviceContractProvisioning.dataAddressSourceBucketName", defaultValue = "")
    @Mapping(target = "provisioning.dataAddressSourceFileName", source = "contract.serviceContractProvisioning.dataAddressSourceFileName", defaultValue = "")
    @Mapping(target = "provisioning.selectedProviderConnectorId", source = "contract.serviceContractProvisioning.selectedProviderConnectorId", defaultValue = "")
    @Mapping(target = "provisioning.dataAddressTargetBucketName", source = "contract.serviceContractProvisioning.dataAddressTargetBucketName", defaultValue = "")
    @Mapping(target = "provisioning.dataAddressTargetFileName", source = "contract.serviceContractProvisioning.dataAddressTargetFileName", defaultValue = "")
    @Mapping(target = "provisioning.selectedConsumerConnectorId", source = "contract.serviceContractProvisioning.selectedConsumerConnectorId", defaultValue = "")
    DataDeliveryContractDto contractToContractDto(DataDeliveryContractTemplate contract,
                                                  MerlotParticipantDto providerOrgaDetails, MerlotParticipantDto consumerOrgaDetails,
                                                  ServiceOfferingDto offeringDetails);

    @Mapping(target = "contractId", expression = "java(contractDto.getDetails().getId().replace(\"Contract:\", \"\"))")
    @Mapping(target = "contractCreationDate", source = "contractDto.details.creationDate")
    @Mapping(target = "contractRuntime", source = "contractDto.negotiation.runtimeSelection", qualifiedByName = "contractRuntime")
    @Mapping(target = "contractTnc", source = "contractDto.details.termsAndConditions", qualifiedByName = "contractTnc")
    @Mapping(target = "contractAttachmentFilenames", expression = "java(contractDto.getNegotiation().getAttachments().stream().toList())")
    @Mapping(target = "serviceId", expression = "java(contractDto.getOffering().getSelfDescription().getVerifiableCredential().getCredentialSubject().getId().replace(\"ServiceOffering:\", \"\"))")
    @Mapping(target = "serviceName", source = "contractDto.offering.selfDescription.verifiableCredential.credentialSubject.name")
    @Mapping(target = "serviceType", source = "contractDto.offering.selfDescription.verifiableCredential.credentialSubject.type")
    @Mapping(target = "serviceDescription", source = "contractDto.offering.selfDescription.verifiableCredential.credentialSubject.description")
    @Mapping(target = "serviceExampleCosts", source = "contractDto.offering.selfDescription.verifiableCredential.credentialSubject.exampleCosts")
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
    @Mapping(target = "serviceDataAccessType", expression = "java(mapStringTypeValue(((DataDeliveryCredentialSubject) contractDto.getOffering().getSelfDescription().getVerifiableCredential().getCredentialSubject()).getDataAccessType()))")
    @Mapping(target = "serviceDataTransferType", expression = "java(mapStringTypeValue(((DataDeliveryCredentialSubject) contractDto.getOffering().getSelfDescription().getVerifiableCredential().getCredentialSubject()).getDataTransferType()))")
    ContractPdfDto contractDtoToContractPdfDto(DataDeliveryContractDto contractDto);

    @InheritConfiguration(name = "contractDtoToContractPdfDto")
    @Mapping(target = "contractUserCount", source = "contractDto.negotiation.userCountSelection")
    @Mapping(target = "serviceHardwareRequirements", expression = "java(mapStringTypeValue(((SaaSCredentialSubject) contractDto.getOffering().getSelfDescription().getVerifiableCredential().getCredentialSubject()).getHardwareRequirements()))")
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
        contractPdfAddressDto.setCountryName(legalAddress.getCountryName().getValue());
        contractPdfAddressDto.setStreetAddress(legalAddress.getStreetAddress().getValue());
        contractPdfAddressDto.setLocality(legalAddress.getLocality().getValue());
        contractPdfAddressDto.setPostalCode(legalAddress.getPostalCode().getValue());

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

