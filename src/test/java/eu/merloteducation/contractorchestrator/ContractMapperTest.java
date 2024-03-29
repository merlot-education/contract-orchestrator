package eu.merloteducation.contractorchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.contractorchestrator.models.mappers.ContractMapper;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescription;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionVerifiableCredential;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.VCard;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.CooperationCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.DataDeliveryCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.SaaSCredentialSubject;
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
        expected.setServiceType("Coop");
        assertThat(actual).usingRecursiveComparison().ignoringFields("contractAttachmentFilenames").isEqualTo(expected);
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

        assertThat(contractMapper.contractRuntimeMapper(hourExample)).isEqualTo("1 Stunde(n)");
        assertThat(contractMapper.contractRuntimeMapper(dayExample)).isEqualTo("2 Tag(e)");
        assertThat(contractMapper.contractRuntimeMapper(weekExample)).isEqualTo("3 Woche(n)");
        assertThat(contractMapper.contractRuntimeMapper(monthExample)).isEqualTo("4 Monat(e)");
        assertThat(contractMapper.contractRuntimeMapper(yearExample)).isEqualTo("5 Jahr(e)");
        assertThat(contractMapper.contractRuntimeMapper(unlimitedExample1)).isEqualTo("Unbegrenzt");
        assertThat(contractMapper.contractRuntimeMapper(unlimitedExample2)).isEqualTo("Unbegrenzt");
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

        VCard providerLegalAddress = new VCard();
        providerLegalAddress.setCountryName("DE");
        providerLegalAddress.setStreetAddress("Abcstraße 10");
        providerLegalAddress.setLocality("Abcstadt");
        providerLegalAddress.setPostalCode("12345");
        contractDetailsDto.setProviderLegalAddress(providerLegalAddress);

        VCard consumerLegalAddress = new VCard();
        consumerLegalAddress.setCountryName("DE");
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

        SelfDescription selfDescription = new SelfDescription();
        selfDescription.setVerifiableCredential(new SelfDescriptionVerifiableCredential());
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

        contractDto.getOffering().getSelfDescription().getVerifiableCredential()
                .setCredentialSubject(new SaaSCredentialSubject());

        SaaSCredentialSubject credentialSubject =
                (SaaSCredentialSubject) contractDto.getOffering().getSelfDescription()
                        .getVerifiableCredential().getCredentialSubject();

        credentialSubject.setId("ServiceOffering:1234");
        credentialSubject.setName("Mein Dienst");
        credentialSubject.setDescription("Liefert Daten von A nach B");
        credentialSubject.setExampleCosts("5 €");
        credentialSubject.setType("SaaS");
        credentialSubject.setHardwareRequirements("10 RAM");

        return contractDto;
    }

    DataDeliveryContractDto getTestDataDeliveryContractDto() throws JsonProcessingException {

        DataDeliveryContractDto contractDto = new DataDeliveryContractDto();
        contractDto.setDetails(new DataDeliveryContractDetailsDto());
        contractDto.setNegotiation(new DataDeliveryContractNegotiationDto());

        contractDto = (DataDeliveryContractDto) getTestContractDto(contractDto);
        contractDto.getNegotiation().setExchangeCountSelection("10");

        contractDto.getOffering().getSelfDescription().getVerifiableCredential()
                .setCredentialSubject(new DataDeliveryCredentialSubject());

        DataDeliveryCredentialSubject credentialSubject =
                (DataDeliveryCredentialSubject) contractDto.getOffering().getSelfDescription()
                        .getVerifiableCredential().getCredentialSubject();

        credentialSubject.setId("ServiceOffering:1234");
        credentialSubject.setName("Mein Dienst");
        credentialSubject.setDescription("Liefert Daten von A nach B");
        credentialSubject.setExampleCosts("5 €");
        credentialSubject.setType("Datenlieferung");
        credentialSubject.setDataAccessType("Download");
        credentialSubject.setDataTransferType("Pull");
        return contractDto;
    }

    CooperationContractDto getTestCoopContractDto() throws JsonProcessingException {

        CooperationContractDto contractDto = new CooperationContractDto();
        contractDto.setDetails(new CooperationContractDetailsDto());
        contractDto.setNegotiation(new CooperationContractNegotiationDto());

        contractDto = (CooperationContractDto) getTestContractDto(contractDto);

        contractDto.getOffering().getSelfDescription().getVerifiableCredential()
                .setCredentialSubject(new CooperationCredentialSubject());

        CooperationCredentialSubject credentialSubject =
                (CooperationCredentialSubject) contractDto.getOffering().getSelfDescription()
                        .getVerifiableCredential().getCredentialSubject();

        credentialSubject.setId("ServiceOffering:1234");
        credentialSubject.setName("Mein Dienst");
        credentialSubject.setDescription("Liefert Daten von A nach B");
        credentialSubject.setExampleCosts("5 €");
        credentialSubject.setType("Coop");

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
        contractPdfDto.setServiceId("1234");
        contractPdfDto.setServiceName("Mein Dienst");
        contractPdfDto.setServiceDescription("Liefert Daten von A nach B");
        contractPdfDto.setServiceExampleCosts("5 €");

        ContractPdfAddressDto providerAddress = new ContractPdfAddressDto();
        providerAddress.setCountryName("DE");
        providerAddress.setStreetAddress("Abcstraße 10");
        providerAddress.setLocality("Abcstadt");
        providerAddress.setPostalCode("12345");

        contractPdfDto.setProviderLegalAddress(providerAddress);
        contractPdfDto.setProviderSignature("12345678");
        contractPdfDto.setProviderLegalName("MeinAnbieter GmbH");
        contractPdfDto.setProviderSignerUser("Hans Wurst");
        contractPdfDto.setProviderSignatureTimestamp("01.02.2023 10:05");

        ContractPdfAddressDto consumerAddress = new ContractPdfAddressDto();
        consumerAddress.setCountryName("DE");
        consumerAddress.setStreetAddress("Defstraße 10");
        consumerAddress.setLocality("Defstadt");
        consumerAddress.setPostalCode("54321");

        contractPdfDto.setConsumerLegalAddress(consumerAddress);
        contractPdfDto.setConsumerSignature("87654321");
        contractPdfDto.setConsumerSignerUser("Marco Polo");
        contractPdfDto.setConsumerLegalName("Konsum AG");
        contractPdfDto.setConsumerSignatureTimestamp("01.02.2023 09:45");

        return contractPdfDto;
    }

    ContractPdfDto getTestContractPdfDtoSaas() {

        ContractPdfDto contractPdfDto = getTestContractPdfDto();
        contractPdfDto.setServiceType("SaaS");
        contractPdfDto.setContractUserCount("10");
        contractPdfDto.setServiceHardwareRequirements("10 RAM");

        return contractPdfDto;
    }

    ContractPdfDto getTestContractPdfDtoDataDelivery() {

        ContractPdfDto contractPdfDto = getTestContractPdfDto();
        contractPdfDto.setServiceType("Datenlieferung");
        contractPdfDto.setContractDataTransferCount("10");
        contractPdfDto.setServiceDataAccessType("Download");
        contractPdfDto.setServiceDataTransferType("Pull");

        return contractPdfDto;
    }
}
