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

package eu.merloteducation.contractorchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.contractorchestrator.models.mappers.ContractDtoToPdfMapper;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiablePresentation;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotCoopContractServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotDataDeliveryServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotSaasServiceOfferingCredentialSubject;
import eu.merloteducation.modelslib.api.contract.*;
import eu.merloteducation.modelslib.api.contract.cooperation.CooperationContractDetailsDto;
import eu.merloteducation.modelslib.api.contract.cooperation.CooperationContractDto;
import eu.merloteducation.modelslib.api.contract.cooperation.CooperationContractNegotiationDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.DataDeliveryContractDetailsDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.DataDeliveryContractNegotiationDto;
import eu.merloteducation.modelslib.api.contract.saas.SaasContractDetailsDto;
import eu.merloteducation.modelslib.api.contract.saas.SaasContractDto;
import eu.merloteducation.modelslib.api.contract.saas.SaasContractNegotiationDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.*;

import static eu.merloteducation.contractorchestrator.SelfDescriptionDemoData.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ContractMapperTest {

    @Autowired
    private ContractDtoToPdfMapper contractDtoToPdfMapper;

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapToContractPdfDtoCorrectlySaas() throws IOException {

        ContractPdfDto actual = contractDtoToPdfMapper.contractDtoToContractPdfDto(getTestSaasContractDto());
        ContractPdfDto expected = getTestContractPdfDtoSaas();
        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("contractAttachmentFilenames", "contractCreationDate").isEqualTo(expected);
        assertThat(actual.getContractAttachmentFilenames()).containsExactlyInAnyOrderElementsOf(
            expected.getContractAttachmentFilenames());
    }

    @Test
    void mapToContractPdfDtoCorrectlyDataDelivery() throws IOException {

        ContractPdfDto actual = contractDtoToPdfMapper.contractDtoToContractPdfDto(getTestDataDeliveryContractDto());
        ContractPdfDto expected = getTestContractPdfDtoDataDelivery();
        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("contractAttachmentFilenames", "contractCreationDate").isEqualTo(expected);
        assertThat(actual.getContractAttachmentFilenames()).containsExactlyInAnyOrderElementsOf(
            expected.getContractAttachmentFilenames());
    }

    @Test
    void mapToContractPdfDtoCorrectlyCooperation() throws IOException {

        ContractPdfDto actual = contractDtoToPdfMapper.contractDtoToContractPdfDto(getTestCoopContractDto());
        ContractPdfDto expected = getTestContractPdfDto();
        expected.setServiceType(MerlotCoopContractServiceOfferingCredentialSubject.TYPE);
        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("contractAttachmentFilenames", "contractCreationDate").isEqualTo(expected);
        assertThat(actual.getContractAttachmentFilenames()).containsExactlyInAnyOrderElementsOf(
            expected.getContractAttachmentFilenames());
    }

    @Test
    void mapRuntimeCorrectly(){
        String hourExample = "1 hour(s)";
        String dayExample = "2 day(s)";
        String weekExample = "3 week(s)";
        String monthExample = "4 month(s)";
        String yearExample = "5 year(s)";
        String unlimitedExample1 = "0 hour(s)";
        String unlimitedExample2 = "12 unlimited";

        assertThat(contractDtoToPdfMapper.contractRuntimeMapper(hourExample)).isEqualTo("1 Stunde(n)");
        assertThat(contractDtoToPdfMapper.contractRuntimeMapper(dayExample)).isEqualTo("2 Tag(e)");
        assertThat(contractDtoToPdfMapper.contractRuntimeMapper(weekExample)).isEqualTo("3 Woche(n)");
        assertThat(contractDtoToPdfMapper.contractRuntimeMapper(monthExample)).isEqualTo("4 Monat(e)");
        assertThat(contractDtoToPdfMapper.contractRuntimeMapper(yearExample)).isEqualTo("5 Jahr(e)");
        assertThat(contractDtoToPdfMapper.contractRuntimeMapper(unlimitedExample1)).isEqualTo("Unbegrenzt");
        assertThat(contractDtoToPdfMapper.contractRuntimeMapper(unlimitedExample2)).isEqualTo("Unbegrenzt");
    }

    private ContractDto getTestContractDto(ContractDto contractDto) throws JsonProcessingException {

        ContractDetailsDto contractDetailsDto = contractDto.getDetails();
        ContractNegotiationDto contractNegotiationDto = contractDto.getNegotiation();

        contractDetailsDto.setId("Contract:1357");
        contractDetailsDto.setCreationDate("01.02.2023");

        List<ContractTncDto> tncDtoList = new ArrayList<>();

        ContractTncDto tnc1 = new ContractTncDto();
        tnc1.setContent("http://example.com");
        tnc1.setHash("hash1234");
        tncDtoList.add(tnc1);

        ContractTncDto tnc2 = new ContractTncDto();
        tnc2.setContent("http://merlot-education.com");
        tnc2.setHash("hash1357");
        tncDtoList.add(tnc2);

        contractDetailsDto.setTermsAndConditions(tncDtoList);
        contractDetailsDto.setProviderLegalName("MeinAnbieter GmbH");
        contractDetailsDto.setProviderSignerUserName("Hans Wurst");
        contractDetailsDto.setProviderSignatureDate("01.02.2023 10:05");
        contractDetailsDto.setConsumerLegalName("Konsum AG");
        contractDetailsDto.setConsumerSignerUserName("Marco Polo");
        contractDetailsDto.setConsumerSignatureDate("01.02.2023 09:45");

        ContractVcard providerLegalAddress = new ContractVcard();
        providerLegalAddress.setCountryCode("DE");
        providerLegalAddress.setCountrySubdivisionCode("DE-BE");
        providerLegalAddress.setStreetAddress("Abcstraße 10");
        providerLegalAddress.setLocality("Abcstadt");
        providerLegalAddress.setPostalCode("12345");
        contractDetailsDto.setProviderLegalAddress(providerLegalAddress);

        ContractVcard consumerLegalAddress = new ContractVcard();
        consumerLegalAddress.setCountryCode("DE");
        consumerLegalAddress.setCountrySubdivisionCode("DE-BE");
        consumerLegalAddress.setStreetAddress("Defstraße 10");
        consumerLegalAddress.setLocality("Defstadt");
        consumerLegalAddress.setPostalCode("54321");
        contractDetailsDto.setConsumerLegalAddress(consumerLegalAddress);

        contractDto.setDetails(contractDetailsDto);

        Set<String> attachments = new HashSet<>();
        attachments.add("abc.pdf");
        attachments.add("def.pdf");
        contractNegotiationDto.setAttachments(attachments);
        contractNegotiationDto.setRuntimeSelection("5000 year(s)");

        contractDto.setNegotiation(contractNegotiationDto);

        ServiceOfferingDto serviceOfferingDetails = new ServiceOfferingDto();
        serviceOfferingDetails.setSelfDescription(new ExtendedVerifiablePresentation());
        contractDto.setOffering(serviceOfferingDetails);

        return contractDto;
    }

    SaasContractDto getTestSaasContractDto() throws JsonProcessingException {

        SaasContractDto contractDto = new SaasContractDto();
        contractDto.setDetails(new SaasContractDetailsDto());
        contractDto.setNegotiation(new SaasContractNegotiationDto());

        contractDto = (SaasContractDto) getTestContractDto(contractDto);
        contractDto.getNegotiation().setUserCountSelection("10");

        String id = "urn:uuid:1234";

        contractDto.getOffering().setSelfDescription(createVpFromCsList(
                List.of(
                        getGxServiceOfferingCs(id, "Mein Dienst", "did:web:someorga"),
                        getMerlotServiceOfferingCs(id),
                        getMerlotSaasServiceOfferingCs(id)
                ),
                "did:web:someorga"
        ));

        return contractDto;
    }

    DataDeliveryContractDto getTestDataDeliveryContractDto() throws JsonProcessingException {

        DataDeliveryContractDto contractDto = new DataDeliveryContractDto();
        contractDto.setDetails(new DataDeliveryContractDetailsDto());
        contractDto.setNegotiation(new DataDeliveryContractNegotiationDto());

        contractDto = (DataDeliveryContractDto) getTestContractDto(contractDto);
        contractDto.getNegotiation().setExchangeCountSelection("10");

        String id = "urn:uuid:1234";

        contractDto.getOffering().setSelfDescription(createVpFromCsList(
                List.of(
                        getGxServiceOfferingCs(id, "Mein Dienst", "did:web:someorga"),
                        getMerlotServiceOfferingCs(id),
                        getMerlotDataDeliveryServiceOfferingCs(id, "Push")
                ),
                "did:web:someorga"
        ));
        return contractDto;
    }

    CooperationContractDto getTestCoopContractDto() throws JsonProcessingException {

        CooperationContractDto contractDto = new CooperationContractDto();
        contractDto.setDetails(new CooperationContractDetailsDto());
        contractDto.setNegotiation(new CooperationContractNegotiationDto());

        contractDto = (CooperationContractDto) getTestContractDto(contractDto);

        String id = "urn:uuid:1234";

        contractDto.getOffering().setSelfDescription(createVpFromCsList(
                List.of(
                        getGxServiceOfferingCs(id, "Mein Dienst", "did:web:someorga"),
                        getMerlotServiceOfferingCs(id),
                        getMerlotCoopContractServiceOfferingCs(id)
                ),
                "did:web:someorga"
        ));

        return contractDto;
    }

    ContractPdfDto getTestContractPdfDto() {

        ContractPdfDto contractPdfDto = new ContractPdfDto();
        contractPdfDto.setContractId("1357");
        contractPdfDto.setContractCreationDate("01.02.2023");
        contractPdfDto.setContractRuntime("5000 Jahr(e)");

        Map<String, String> tnc1 = new HashMap<>();
        tnc1.put("tncLink", "http://example.com");
        tnc1.put("tncHash", "hash1234");

        Map<String, String> tnc2 = new HashMap<>();
        tnc2.put("tncLink", "http://merlot-education.com");
        tnc2.put("tncHash", "hash1357");

        List<Map<String, String>> tnc = new ArrayList<>();
        tnc.add(tnc1);
        tnc.add(tnc2);

        contractPdfDto.setContractTnc(tnc);

        List<String> attachments = new ArrayList<>();
        attachments.add("abc.pdf");
        attachments.add("def.pdf");

        contractPdfDto.setContractAttachmentFilenames(attachments);
        contractPdfDto.setServiceId("urn:uuid:1234");
        contractPdfDto.setServiceName("Mein Dienst");
        contractPdfDto.setServiceDescription("Some offering description");
        contractPdfDto.setServiceExampleCosts("5€");

        ContractPdfAddressDto providerAddress = new ContractPdfAddressDto();
        providerAddress.setCountryName("DE");
        providerAddress.setStreetAddress("Abcstraße 10");
        providerAddress.setLocality("Abcstadt");
        providerAddress.setPostalCode("12345");

        contractPdfDto.setProviderLegalAddress(providerAddress);
        contractPdfDto.setProviderLegalName("MeinAnbieter GmbH");
        contractPdfDto.setProviderSignerUser("Hans Wurst");
        contractPdfDto.setProviderSignatureTimestamp("01.02.2023 10:05");

        ContractPdfAddressDto consumerAddress = new ContractPdfAddressDto();
        consumerAddress.setCountryName("DE");
        consumerAddress.setStreetAddress("Defstraße 10");
        consumerAddress.setLocality("Defstadt");
        consumerAddress.setPostalCode("54321");

        contractPdfDto.setConsumerLegalAddress(consumerAddress);
        contractPdfDto.setConsumerSignerUser("Marco Polo");
        contractPdfDto.setConsumerLegalName("Konsum AG");
        contractPdfDto.setConsumerSignatureTimestamp("01.02.2023 09:45");

        return contractPdfDto;
    }

    ContractPdfDto getTestContractPdfDtoSaas() {

        ContractPdfDto contractPdfDto = getTestContractPdfDto();
        contractPdfDto.setServiceType(MerlotSaasServiceOfferingCredentialSubject.TYPE);
        contractPdfDto.setContractUserCount("10");
        contractPdfDto.setServiceHardwareRequirements("1.21 Gigawatts");

        return contractPdfDto;
    }

    ContractPdfDto getTestContractPdfDtoDataDelivery() {

        ContractPdfDto contractPdfDto = getTestContractPdfDto();
        contractPdfDto.setServiceType(MerlotDataDeliveryServiceOfferingCredentialSubject.TYPE);
        contractPdfDto.setContractDataTransferCount("10");
        contractPdfDto.setServiceDataAccessType("Download");
        contractPdfDto.setServiceDataTransferType("Push");

        return contractPdfDto;
    }
}
