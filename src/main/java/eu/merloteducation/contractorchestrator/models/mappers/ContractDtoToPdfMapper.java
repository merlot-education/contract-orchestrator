package eu.merloteducation.contractorchestrator.models.mappers;

import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.VCard;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.DataDeliveryCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.SaaSCredentialSubject;
import eu.merloteducation.modelslib.api.contract.ContractDto;
import eu.merloteducation.modelslib.api.contract.ContractPdfAddressDto;
import eu.merloteducation.modelslib.api.contract.ContractPdfDto;
import eu.merloteducation.modelslib.api.contract.ContractTncDto;
import eu.merloteducation.modelslib.api.contract.cooperation.CooperationContractDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.modelslib.api.contract.saas.SaasContractDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", imports = {DataDeliveryCredentialSubject.class, SaaSCredentialSubject.class })
public interface ContractDtoToPdfMapper {
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
}
