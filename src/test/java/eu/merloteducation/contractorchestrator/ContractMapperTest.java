package eu.merloteducation.contractorchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.contractorchestrator.models.dto.ContractDetailsDto;
import eu.merloteducation.contractorchestrator.models.dto.ContractDto;
import eu.merloteducation.contractorchestrator.models.dto.ContractTncDto;
import eu.merloteducation.contractorchestrator.models.mappers.ContractMapper;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ContractMapperTest {
    @Autowired
    ContractMapper contractMapper;

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapToContractPdfDtoCorrectlySaas() throws IOException {

    }

    @Test
    void mapToContractPdfDtoCorrectlyDataDelivery() throws IOException {

    }

    @Test
    void mapToContractPdfDtoCorrectlyCooperation() throws IOException {

    }

    ContractDto getTestContractDto(){
        ContractDto contractDto = new ContractDto();
        ContractDetailsDto contractDetailsDto = new ContractDetailsDto();
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
        contractDetailsDto.setConsumerLegalName("Konsum AG");

        JsonNode node = mapper.createObjectNode();
        return null;
    }
}
