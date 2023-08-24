package eu.merloteducation.contractorchestrator;

import eu.merloteducation.contractorchestrator.controller.ContractsController;
import eu.merloteducation.contractorchestrator.controller.DataTransferController;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.models.EdcIdResponse;
import eu.merloteducation.contractorchestrator.models.EdcNegotiationStatus;
import eu.merloteducation.contractorchestrator.models.EdcTransferStatus;
import eu.merloteducation.contractorchestrator.models.edc.common.IdResponse;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractNegotiation;
import eu.merloteducation.contractorchestrator.models.edc.transfer.IonosS3TransferProcess;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.SaasContractTemplate;
import eu.merloteducation.contractorchestrator.security.JwtAuthConverter;
import eu.merloteducation.contractorchestrator.security.JwtAuthConverterProperties;
import eu.merloteducation.contractorchestrator.security.WebSecurityConfig;
import eu.merloteducation.contractorchestrator.service.ContractStorageService;
import eu.merloteducation.contractorchestrator.service.EdcOrchestrationService;
import eu.merloteducation.contractorchestrator.service.MessageQueueService;
import jakarta.persistence.Id;
import org.json.JSONException;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({DataTransferController.class, WebSecurityConfig.class})
@AutoConfigureMockMvc()
public class DataTransferControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtAuthConverter jwtAuthConverter;

    @MockBean
    private JwtAuthConverterProperties jwtAuthConverterProperties;

    @MockBean
    private EdcOrchestrationService edcOrchestrationService;

    @MockBean
    private ContractStorageService contractStorageService;

    @MockBean
    private MessageQueueService messageQueueService;

    @BeforeEach
    public void beforeEach() throws JSONException {

        IdResponse idResponse = new IdResponse();
        idResponse.setId("123");

        ContractNegotiation negotiation = new ContractNegotiation();
        negotiation.setId("456");
        negotiation.setState("FINALIZED");
        negotiation.setContractAgreementId("789");

        IonosS3TransferProcess transferProcess = new IonosS3TransferProcess();
        transferProcess.setId("234");
        transferProcess.setState("COMPLETED");

        lenient().when(edcOrchestrationService.initiateConnectorNegotiation(any(), any(), any(), any())).thenReturn(idResponse);
        lenient().when(edcOrchestrationService.initiateConnectorTransfer(any(), any(), any(), any(), any())).thenReturn(idResponse);
        lenient().when(edcOrchestrationService.getNegotationStatus(any(), any(), any(), any(), any())).thenReturn(negotiation);
        lenient().when(edcOrchestrationService.getTransferStatus(any(), any(), any(), any(), any())).thenReturn(transferProcess);
    }

    @Test
    void postStartContractNegotiationUnauthorized() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .post("/transfers/contract/123/negotiation/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getContractNegotiationStatusUnauthorized() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/transfers/contract/1234/negotiation/456/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postStartDataTransferUnauthorized() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .post("/transfers/contract/123/negotiation/456/transfer/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTransferStatusUnauthorized() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/transfers/contract/123/transfer/456/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postStartContractNegotiationValid() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .post("/transfers/contract/123/negotiation/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "10")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getContractNegotiationStatusValid() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/transfers/contract/123/negotiation/456/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "10")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void postStartDataTransferValid() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .post("/transfers/contract/123/negotiation/456/transfer/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "10")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getTransferStatusValid() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/transfers/contract/123/transfer/456/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "10")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void postStartContractNegotiationBadActiveRole() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .post("/transfers/contract/123/negotiation/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "20")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void getContractNegotiationStatusBadActiveRole() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/transfers/contract/123/negotiation/456/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "20")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void postStartDataTransferBadActiveRole() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .post("/transfers/contract/123/negotiation/456/transfer/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "20")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransferStatusBadActiveRole() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/transfers/contract/123/transfer/456/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "20")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

}
