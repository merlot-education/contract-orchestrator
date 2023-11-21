package eu.merloteducation.contractorchestrator.models.mappers;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.mapstruct.Named;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    default String map(OffsetDateTime offsetDateTime) {

        return offsetDateTime != null ? offsetDateTime.toString() : null;
    }

    @Mapping(target = "id", source = "contract.id")
    @Mapping(target = "creationDate", source = "contract.creationDate")
    @Mapping(target = "offering", source = "offeringDetails")
    @Mapping(target = "providerId", source = "offeringDetails.providerDetails.providerId")
    @Mapping(target = "providerLegalName", source = "offeringDetails.providerDetails.providerLegalName")
    @Mapping(target = "consumerId", source = "consumerOrgaDetails.selfDescription.verifiableCredential.credentialSubject.id")
    @Mapping(target = "consumerLegalName", source = "consumerOrgaDetails.selfDescription.verifiableCredential.credentialSubject.legalName.value")
    @Mapping(target = "state", source = "contract.state")
    ContractBasicDto contractToContractBasicDto(ContractTemplate contract, OrganizationDetails consumerOrgaDetails,
        ServiceOfferingDetails offeringDetails);

    @Mapping(target = "type", source = "contract.type")
    @Mapping(target = "details.id", source = "contract.id")
    @Mapping(target = "details.creationDate", source = "contract.creationDate")
    @Mapping(target = "details.providerId", source = "contract.providerId")
    @Mapping(target = "details.providerLegalName", source = "offeringDetails.providerDetails.providerLegalName")
    @Mapping(target = "details.providerLegalAddress", source = "providerOrgaDetails.selfDescription.verifiableCredential.credentialSubject.legalAddress")
    @Mapping(target = "details.consumerId", source = "contract.consumerId")
    @Mapping(target = "details.consumerLegalName", source = "consumerOrgaDetails.selfDescription.verifiableCredential.credentialSubject.legalName.value")
    @Mapping(target = "details.consumerLegalAddress", source = "consumerOrgaDetails.selfDescription.verifiableCredential.credentialSubject.legalAddress")
    @Mapping(target = "details.state", source = "contract.state")
    @Mapping(target = "details.termsAndConditions", source = "contract.termsAndConditions")
    @Mapping(target = "negotiation.runtimeSelection", source = "contract.runtimeSelection", defaultValue = "")
    @Mapping(target = "negotiation.additionalAgreements", source = "contract.additionalAgreements")
    @Mapping(target = "negotiation.attachments", source = "contract.attachments")
    @Mapping(target = "negotiation.consumerTncAccepted", source = "contract.consumerTncAccepted")
    @Mapping(target = "negotiation.consumerAttachmentsAccepted", source = "contract.consumerAttachmentsAccepted")
    @Mapping(target = "negotiation.providerTncAccepted", source = "contract.providerTncAccepted")
    @Mapping(target = "provisioning.validUntil", source = "contract.serviceContractProvisioning.validUntil")
    @Mapping(target = "offering", source = "offeringDetails")
    ContractDto contractToContractDto(ContractTemplate contract, OrganizationDetails providerOrgaDetails,
        OrganizationDetails consumerOrgaDetails, ServiceOfferingDetails offeringDetails);

    @InheritConfiguration(name = "contractToContractDto")
    CooperationContractDto contractToContractDto(CooperationContractTemplate contract,
        OrganizationDetails providerOrgaDetails, OrganizationDetails consumerOrgaDetails,
        ServiceOfferingDetails offeringDetails);

    @InheritConfiguration(name = "contractToContractDto")
    @Mapping(target = "negotiation.userCountSelection", source = "contract.userCountSelection", defaultValue = "")
    SaasContractDto contractToContractDto(SaasContractTemplate contract, OrganizationDetails providerOrgaDetails,
        OrganizationDetails consumerOrgaDetails, ServiceOfferingDetails offeringDetails);

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
        OrganizationDetails providerOrgaDetails, OrganizationDetails consumerOrgaDetails,
        ServiceOfferingDetails offeringDetails);

    @Mapping(target = "contractId", source = "contractDto.details.id")
    @Mapping(target = "contractCreationDate", source = "contractDto.details.creationDate")
    @Mapping(target = "contractRuntime", source = "contractDto.negotiation.runtimeSelection")
    @Mapping(target = "contractTnc", source = "contractDto.details.termsAndConditions", qualifiedByName = "contractTnc")
    @Mapping(target = "contractAttachmentFilenames", expression = "java(contractDto.getNegotiation().getAttachments().stream().toList())")
    @Mapping(target = "serviceId", expression = "java(contractDto.getOffering().getSelfDescription().path(\"verifiableCredential\").path(\"credentialSubject\").path(\"@id\").textValue())")
    @Mapping(target = "serviceName", expression = "java(contractDto.getOffering().getSelfDescription().path(\"verifiableCredential\").path(\"credentialSubject\").path(\"name\").path(\"@value\").textValue())")
    @Mapping(target = "serviceType", expression = "java(contractDto.getOffering().getSelfDescription().path(\"verifiableCredential\").path(\"credentialSubject\").path(\"@type\").textValue())")
    @Mapping(target = "serviceDescription", expression = "java(contractDto.getOffering().getSelfDescription().path(\"verifiableCredential\").path(\"credentialSubject\").path(\"dct:description\").path(\"@value\").textValue())")
    @Mapping(target = "serviceExampleCosts", expression = "java(contractDto.getOffering().getSelfDescription().path(\"verifiableCredential\").path(\"credentialSubject\").path(\"merlot:exampleCosts\").path(\"@value\").textValue())")
    @Mapping(target = "providerLegalName", source = "contractDto.details.providerLegalName")
    @Mapping(target = "providerLegalAddress", source = "contractDto.details.providerLegalAddress", qualifiedByName = "legalAddress")
    @Mapping(target = "providerSignerUser", source = "contract.providerSignature.signerName")
    @Mapping(target = "providerSignature", source = "contract.providerSignature.signature")
    @Mapping(target = "providerSignatureTimestamp", expression = "java(contract.getProviderSignature().getSignatureDate().toString())")
    @Mapping(target = "consumerLegalName", source = "contractDto.details.consumerLegalName")
    @Mapping(target = "consumerLegalAddress", source = "contractDto.details.consumerLegalAddress", qualifiedByName = "legalAddress")
    @Mapping(target = "consumerSignerUser", source = "contract.consumerSignature.signerName")
    @Mapping(target = "consumerSignature", source = "contract.consumerSignature.signature")
    @Mapping(target = "consumerSignatureTimestamp", expression = "java(contract.getConsumerSignature().getSignatureDate().toString())")
    ContractPdfDto contractDtoToContractPdfDto(ContractTemplate contract, ContractDto contractDto);

    @InheritConfiguration(name = "contractDtoToContractPdfDto")
    @Mapping(target = "contractDataTransferCount", source = "contractDto.negotiation.exchangeCountSelection")
    @Mapping(target = "serviceDataAccessType", expression = "java(contractDto.getOffering().getSelfDescription().path(\"verifiableCredential\").path(\"credentialSubject\").path(\"dataAccessType\").path(\"value\").textValue())")
    @Mapping(target = "serviceDataTransferType", expression = "java(contractDto.getOffering().getSelfDescription().path(\"verifiableCredential\").path(\"credentialSubject\").path(\"dataTransferType\").path(\"value\").textValue())")
    ContractPdfDto contractDtoToContractPdfDto(ContractTemplate contract, DataDeliveryContractDto contractDto);

    @InheritConfiguration(name = "contractDtoToContractPdfDto")
    @Mapping(target = "contractUserCount", source = "contractDto.negotiation.userCountSelection")
    @Mapping(target = "serviceHardwareRequirements", expression = "java(contractDto.getOffering().getSelfDescription().path(\"verifiableCredential\").path(\"credentialSubject\").path(\"hardwareRequirements\").path(\"value\").textValue())")
    ContractPdfDto contractDtoToContractPdfDto(ContractTemplate contract, SaasContractDto contractDto);

    @InheritConfiguration(name = "contractDtoToContractPdfDto")
    ContractPdfDto contractDtoToContractPdfDto(ContractTemplate contract, CooperationContractDto contractDto);

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
    default Map<String, String> legalAddressMapper(JsonNode legalAddress) {

        Map<String, String> map = new HashMap<>();
        map.put("countryName", legalAddress.path("countryName").path("value").textValue());
        map.put("streetAddress", legalAddress.path("streetAddress").path("value").textValue());
        map.put("locality", legalAddress.path("locality").path("value").textValue());
        map.put("postalCode", legalAddress.path("postalCode").path("value").textValue());

        return map;
    }
}

