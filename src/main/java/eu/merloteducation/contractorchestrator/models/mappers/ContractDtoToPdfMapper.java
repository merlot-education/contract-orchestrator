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

import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.PojoCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.serviceofferings.GxServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotCoopContractServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotDataDeliveryServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotSaasServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotServiceOfferingCredentialSubject;
import eu.merloteducation.modelslib.api.contract.*;
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

@Mapper(componentModel = "spring", imports = {GxServiceOfferingCredentialSubject.class,
        MerlotServiceOfferingCredentialSubject.class, MerlotSaasServiceOfferingCredentialSubject.class,
        MerlotDataDeliveryServiceOfferingCredentialSubject.class, MerlotCoopContractServiceOfferingCredentialSubject.class})
public interface ContractDtoToPdfMapper {
    @Mapping(target = "contractId", expression = "java(contractDto.getDetails().getId().replace(\"Contract:\", \"\"))")
    @Mapping(target = "contractCreationDate", source = "contractDto.details.creationDate")
    @Mapping(target = "contractRuntime", source = "contractDto.negotiation.runtimeSelection", qualifiedByName = "contractRuntime")
    @Mapping(target = "contractTnc", source = "contractDto.details.termsAndConditions", qualifiedByName = "contractTnc")
    @Mapping(target = "contractAttachmentFilenames", expression = "java(contractDto.getNegotiation().getAttachments().stream().toList())")
    @Mapping(target = "serviceId", source = "contractDto.offering", qualifiedByName = "mapOfferingId")
    @Mapping(target = "serviceType", source = "contractDto.offering", qualifiedByName = "mapOfferingType")
    @Mapping(target = "serviceName", source = "contractDto.offering", qualifiedByName = "mapOfferingName")
    @Mapping(target = "serviceDescription", source = "contractDto.offering", qualifiedByName = "mapOfferingDescription")
    @Mapping(target = "serviceExampleCosts", source = "contractDto.offering", qualifiedByName = "mapOfferingExampleCosts")
    @Mapping(target = "providerLegalName", source = "contractDto.details.providerLegalName")
    @Mapping(target = "providerLegalAddress", source = "contractDto.details.providerLegalAddress", qualifiedByName = "legalAddress")
    @Mapping(target = "providerSignerUser", source = "contractDto.details.providerSignerUserName")
    @Mapping(target = "providerSignatureTimestamp", source = "contractDto.details.providerSignatureDate")
    @Mapping(target = "consumerLegalName", source = "contractDto.details.consumerLegalName")
    @Mapping(target = "consumerLegalAddress", source = "contractDto.details.consumerLegalAddress", qualifiedByName = "legalAddress")
    @Mapping(target = "consumerSignerUser", source = "contractDto.details.consumerSignerUserName")
    @Mapping(target = "consumerSignatureTimestamp", source = "contractDto.details.consumerSignatureDate")
    ContractPdfDto contractDtoToContractPdfDto(ContractDto contractDto);

    @InheritConfiguration(name = "contractDtoToContractPdfDto")
    @Mapping(target = "contractDataTransferCount", source = "contractDto.negotiation.exchangeCountSelection")
    @Mapping(target = "serviceDataAccessType", source = "contractDto.offering", qualifiedByName = "mapDataAccessType")
    @Mapping(target = "serviceDataTransferType", source = "contractDto.offering", qualifiedByName = "mapDataTransferType")
    ContractPdfDto contractDtoToContractPdfDto(DataDeliveryContractDto contractDto);

    @InheritConfiguration(name = "contractDtoToContractPdfDto")
    @Mapping(target = "contractUserCount", source = "contractDto.negotiation.userCountSelection")
    @Mapping(target = "serviceHardwareRequirements", source = "contractDto.offering", qualifiedByName = "mapHardwareRequirements")
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
    default ContractPdfAddressDto legalAddressMapper(ContractVcard legalAddress) {
        ContractPdfAddressDto contractPdfAddressDto = new ContractPdfAddressDto();
        contractPdfAddressDto.setCountryName(legalAddress.getCountryCode());
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

    @Named("mapOfferingId")
    default String mapOfferingId(ServiceOfferingDto offeringDto) {
        return offeringDto.getSelfDescription()
                .findFirstCredentialSubjectByType(GxServiceOfferingCredentialSubject.class)
                .getId();
    }

    @Named("mapOfferingType")
    default String mapOfferingType(ServiceOfferingDto offeringDto) {
        // consider all MERLOT specific offering classes
        List<Class<? extends PojoCredentialSubject>> csClasses =
                List.of(
                        MerlotDataDeliveryServiceOfferingCredentialSubject.class,
                        MerlotSaasServiceOfferingCredentialSubject.class,
                        MerlotCoopContractServiceOfferingCredentialSubject.class
                );

        // check if any of them match, and if so, return the type
        for (Class<? extends PojoCredentialSubject> csClass : csClasses) {
            PojoCredentialSubject cs = offeringDto.getSelfDescription().findFirstCredentialSubjectByType(csClass);
            if (cs != null) {
                return cs.getType();
            }
        }
        return "";
    }

    @Named("mapOfferingName")
    default String mapOfferingName(ServiceOfferingDto offeringDto) {
        String name = offeringDto.getSelfDescription()
                .findFirstCredentialSubjectByType(GxServiceOfferingCredentialSubject.class)
                .getName();
        return name == null ? "" : name;
    }

    @Named("mapOfferingDescription")
    default String mapOfferingDescription(ServiceOfferingDto offeringDto) {
        String description = offeringDto.getSelfDescription()
                .findFirstCredentialSubjectByType(GxServiceOfferingCredentialSubject.class)
                .getDescription();
        return description == null ? "" : description;
    }

    @Named("mapOfferingExampleCosts")
    default String mapOfferingExampleCosts(ServiceOfferingDto offeringDto) {
        String exampleCosts = offeringDto.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotServiceOfferingCredentialSubject.class)
                .getExampleCosts();
        return exampleCosts == null ? "" : exampleCosts;
    }

    @Named("mapDataAccessType")
    default String mapDataAccessType(ServiceOfferingDto offeringDto) {
        return offeringDto.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotDataDeliveryServiceOfferingCredentialSubject.class)
                .getDataAccessType();
    }

    @Named("mapDataTransferType")
    default String mapDataTransferType(ServiceOfferingDto offeringDto) {
        return offeringDto.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotDataDeliveryServiceOfferingCredentialSubject.class)
                .getDataTransferType();
    }

    @Named("mapHardwareRequirements")
    default String mapHardwareRequirements(ServiceOfferingDto offeringDto) {
        return offeringDto.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotSaasServiceOfferingCredentialSubject.class)
                .getHardwareRequirements();
    }
}
