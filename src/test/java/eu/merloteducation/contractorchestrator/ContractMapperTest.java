package eu.merloteducation.contractorchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.merloteducation.contractorchestrator.models.dto.*;
import eu.merloteducation.contractorchestrator.models.dto.cooperation.CooperationContractDetailsDto;
import eu.merloteducation.contractorchestrator.models.dto.cooperation.CooperationContractDto;
import eu.merloteducation.contractorchestrator.models.dto.cooperation.CooperationContractNegotiationDto;
import eu.merloteducation.contractorchestrator.models.dto.datadelivery.DataDeliveryContractDetailsDto;
import eu.merloteducation.contractorchestrator.models.dto.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.contractorchestrator.models.dto.datadelivery.DataDeliveryContractNegotiationDto;
import eu.merloteducation.contractorchestrator.models.dto.saas.SaasContractDetailsDto;
import eu.merloteducation.contractorchestrator.models.dto.saas.SaasContractDto;
import eu.merloteducation.contractorchestrator.models.dto.saas.SaasContractNegotiationDto;
import eu.merloteducation.contractorchestrator.models.mappers.ContractMapper;
import eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator.ServiceOfferingDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ContractMapperTest {
    @Autowired
    ContractMapper contractMapper;

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapToContractPdfDtoCorrectlySaas() throws IOException {

        ContractPdfDto actual = contractMapper.contractDtoToContractPdfDto(getTestSaasContractDto());
        ContractPdfDto expected = getTestContractPdfDtoSaas();
        assertThat(actual).usingRecursiveComparison().ignoringFields("contractAttachmentFilenames").isEqualTo(expected);
        assertThat(actual.getContractAttachmentFilenames()).containsExactlyInAnyOrderElementsOf(
            expected.getContractAttachmentFilenames());
    }

    @Test
    void mapToContractPdfDtoCorrectlyDataDelivery() throws IOException {

        ContractPdfDto actual = contractMapper.contractDtoToContractPdfDto(getTestDataDeliveryContractDto());
        ContractPdfDto expected = getTestContractPdfDtoDataDelivery();
        assertThat(actual).usingRecursiveComparison().ignoringFields("contractAttachmentFilenames").isEqualTo(expected);
        assertThat(actual.getContractAttachmentFilenames()).containsExactlyInAnyOrderElementsOf(
            expected.getContractAttachmentFilenames());
    }

    @Test
    void mapToContractPdfDtoCorrectlyCooperation() throws IOException {

        ContractPdfDto actual = contractMapper.contractDtoToContractPdfDto(getTestCoopContractDto());
        ContractPdfDto expected = getTestContractPdfDto();
        assertThat(actual).usingRecursiveComparison().ignoringFields("contractAttachmentFilenames").isEqualTo(expected);
        assertThat(actual.getContractAttachmentFilenames()).containsExactlyInAnyOrderElementsOf(
            expected.getContractAttachmentFilenames());
    }

    ContractDto getTestContractDto(ContractDto contractDto) throws JsonProcessingException {

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
        contractDetailsDto.setProviderSignature("12345678");
        contractDetailsDto.setProviderSignatureDate("01.02.2023 10:05");
        contractDetailsDto.setConsumerLegalName("Konsum AG");
        contractDetailsDto.setConsumerSignerUserName("Marco Polo");
        contractDetailsDto.setConsumerSignature("87654321");
        contractDetailsDto.setConsumerSignatureDate("01.02.2023 09:45");

        String jsonString1 = "{\"vcard:country-name\": {\"@value\":\"DE\"}, \"vcard:street-address\": "
            + "{\"@value\":\"Abcstraße 10\"}, \"vcard:locality\": {\"@value\":\"Abcstadt\"}, \"vcard:postal-code\": "
            + "{\"@value\":\"12345\"}}";

        JsonNode providerLegalAddress = mapper.readTree(jsonString1);
        contractDetailsDto.setProviderLegalAddress(providerLegalAddress);

        String jsonString2 = "{\"vcard:country-name\": {\"@value\":\"DE\"}, \"vcard:street-address\": "
            + "{\"@value\":\"Defstraße 10\"}, \"vcard:locality\": {\"@value\":\"Defstadt\"}, "
            + "\"vcard:postal-code\": {\"@value\":\"54321\"}}";

        JsonNode consumerLegalAddress = mapper.readTree(jsonString2);
        contractDetailsDto.setConsumerLegalAddress(consumerLegalAddress);

        contractDto.setDetails(contractDetailsDto);

        Set<String> attachments = new HashSet<>();
        attachments.add("abc.pdf");
        attachments.add("def.pdf");
        contractNegotiationDto.setAttachments(attachments);
        contractNegotiationDto.setRuntimeSelection("5000 Jahre");

        contractDto.setNegotiation(contractNegotiationDto);

        ServiceOfferingDetails serviceOfferingDetails = new ServiceOfferingDetails();

        String jsonString3 = "{\"verifiableCredential\": {\"credentialSubject\": {\"@id\":\"ServiceOffering:1234\","
            + "\"gax-trust-framework:name\": {\"@value\":\"Mein Dienst\"}, \"dct:description\": "
            + "{\"@value\":\"Liefert Daten von A nach B\"}, \"merlot:exampleCosts\": {\"@value\":\"5 €\"},"
            + "\"@type\":\"Datenlieferung\"}}}";

        JsonNode selfDescription = mapper.readTree(jsonString3);
        serviceOfferingDetails.setSelfDescription(selfDescription);
        contractDto.setOffering(serviceOfferingDetails);

        return contractDto;
    }

    SaasContractDto getTestSaasContractDto() throws JsonProcessingException {

        SaasContractDto contractDto = new SaasContractDto();
        contractDto.setDetails(new SaasContractDetailsDto());
        contractDto.setNegotiation(new SaasContractNegotiationDto());

        contractDto = (SaasContractDto) getTestContractDto(contractDto);
        contractDto.getNegotiation().setUserCountSelection("10");

        JsonNode selfDescription = contractDto.getOffering().getSelfDescription();
        JsonNode locatedNode = selfDescription.path("verifiableCredential").path("credentialSubject");
        ObjectNode addedNode = ((ObjectNode) locatedNode).putObject("merlot:hardwareRequirements");
        addedNode.put("@value", "10 RAM");

        ServiceOfferingDetails serviceOfferingDetails = new ServiceOfferingDetails();
        serviceOfferingDetails.setSelfDescription(selfDescription);

        contractDto.setOffering(serviceOfferingDetails);
        return contractDto;
    }

    DataDeliveryContractDto getTestDataDeliveryContractDto() throws JsonProcessingException {

        DataDeliveryContractDto contractDto = new DataDeliveryContractDto();
        contractDto.setDetails(new DataDeliveryContractDetailsDto());
        contractDto.setNegotiation(new DataDeliveryContractNegotiationDto());

        contractDto = (DataDeliveryContractDto) getTestContractDto(contractDto);
        contractDto.getNegotiation().setExchangeCountSelection("10");

        JsonNode selfDescription = contractDto.getOffering().getSelfDescription();
        JsonNode locatedNode = selfDescription.path("verifiableCredential").path("credentialSubject");
        ObjectNode addedNode1 = ((ObjectNode) locatedNode).putObject("merlot:dataAccessType");
        addedNode1.put("@value", "Download");
        ObjectNode addedNode2 = ((ObjectNode) locatedNode).putObject("merlot:dataTransferType");
        addedNode2.put("@value", "Taube");
        return contractDto;
    }

    CooperationContractDto getTestCoopContractDto() throws JsonProcessingException {

        CooperationContractDto contractDto = new CooperationContractDto();
        contractDto.setDetails(new CooperationContractDetailsDto());
        contractDto.setNegotiation(new CooperationContractNegotiationDto());

        contractDto = (CooperationContractDto) getTestContractDto(contractDto);
        return contractDto;
    }

    ContractPdfDto getTestContractPdfDto() {

        ContractPdfDto contractPdfDto = new ContractPdfDto();
        contractPdfDto.setContractId("Contract:1357");
        contractPdfDto.setContractCreationDate("01.02.2023");
        contractPdfDto.setContractRuntime("5000 Jahre");

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
        contractPdfDto.setServiceId("ServiceOffering:1234");
        contractPdfDto.setServiceName("Mein Dienst");
        contractPdfDto.setServiceType("Datenlieferung");
        contractPdfDto.setServiceDescription("Liefert Daten von A nach B");
        contractPdfDto.setServiceExampleCosts("5 €");

        Map<String, String> providerAddress = new HashMap<>();
        providerAddress.put("countryName", "DE");
        providerAddress.put("streetAddress", "Abcstraße 10");
        providerAddress.put("locality", "Abcstadt");
        providerAddress.put("postalCode", "12345");

        contractPdfDto.setProviderLegalAddress(providerAddress);
        contractPdfDto.setProviderSignature("12345678");
        contractPdfDto.setProviderLegalName("MeinAnbieter GmbH");
        contractPdfDto.setProviderSignerUser("Hans Wurst");
        contractPdfDto.setProviderSignatureTimestamp("01.02.2023 10:05");

        Map<String, String> consumerAddress = new HashMap<>();
        consumerAddress.put("countryName", "DE");
        consumerAddress.put("streetAddress", "Defstraße 10");
        consumerAddress.put("locality", "Defstadt");
        consumerAddress.put("postalCode", "54321");

        contractPdfDto.setConsumerLegalAddress(consumerAddress);
        contractPdfDto.setConsumerSignature("87654321");
        contractPdfDto.setConsumerSignerUser("Marco Polo");
        contractPdfDto.setConsumerLegalName("Konsum AG");
        contractPdfDto.setConsumerSignatureTimestamp("01.02.2023 09:45");

        return contractPdfDto;
    }

    ContractPdfDto getTestContractPdfDtoSaas() {

        ContractPdfDto contractPdfDto = getTestContractPdfDto();
        contractPdfDto.setContractUserCount("10");
        contractPdfDto.setServiceHardwareRequirements("10 RAM");

        return contractPdfDto;
    }

    ContractPdfDto getTestContractPdfDtoDataDelivery() {

        ContractPdfDto contractPdfDto = getTestContractPdfDto();
        contractPdfDto.setContractDataTransferCount("10");
        contractPdfDto.setServiceDataAccessType("Download");
        contractPdfDto.setServiceDataTransferType("Taube");

        return contractPdfDto;
    }
}
