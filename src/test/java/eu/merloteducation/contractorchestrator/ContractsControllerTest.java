package eu.merloteducation.contractorchestrator;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.merloteducation.contractorchestrator.controller.ContractsController;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.DataDeliveryContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.SaasContractTemplate;
import eu.merloteducation.contractorchestrator.security.JwtAuthConverter;
import eu.merloteducation.contractorchestrator.security.WebSecurityConfig;
import eu.merloteducation.contractorchestrator.service.ContractStorageService;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContractsController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContractsControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ContractStorageService contractStorageService;

    @MockBean
    private JwtAuthConverter jwtAuthConverter;

    private String objectAsJsonString(final Object obj) {
        try {
            return JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .build().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void beforeEach() throws JSONException {
        List<ContractTemplate> contractTemplates = new ArrayList<>();
        SaasContractTemplate template = new SaasContractTemplate();
        template.setProviderId("Participant:10");
        template.setConsumerId("Participant:20");
        contractTemplates.add(template);
        Page<ContractTemplate> contractTemplatesPage = new PageImpl<>(contractTemplates);

        lenient().when(contractStorageService.addContractTemplate(any(), any()))
                .thenReturn(contractTemplates.get(0));
        lenient().when(contractStorageService.getContractDetails(any(), any()))
                .thenReturn(contractTemplates.get(0));
        lenient().when(contractStorageService.updateContractTemplate(any(), any(), any(), any()))
                .thenReturn(contractTemplates.get(0));
        lenient().when(contractStorageService.getOrganizationContracts(any(), any()))
                .thenReturn(contractTemplatesPage);
    }

    @Test
    void getHealthUnauthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/health"))
                .andExpect(status().isOk());
    }
    @Test
    @WithMockUser(username = "user", roles={"USER", "ADMIN", "OrgLegRep_10"})
    void postAddContractUnauthorized() throws Exception
    {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setOfferingId("ServiceOffering:1234");
        request.setConsumerId("Participant:99");

        mvc.perform(MockMvcRequestBuilders
                        .post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer 1234")
                        .content(objectAsJsonString(request))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles={"USER", "ADMIN", "OrgLegRep_10"})
    void postAddContractAuthorizedValid() throws Exception
    {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setOfferingId("ServiceOffering:1234");
        request.setConsumerId("Participant:10");

        mvc.perform(MockMvcRequestBuilders
                        .post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer 1234")
                        .content(objectAsJsonString(request))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles={"USER", "ADMIN", "OrgLegRep_10"})
    void postAddContractAuthorizedInvalid() throws Exception
    {

        mvc.perform(MockMvcRequestBuilders
                        .post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer 1234")
                        .content(objectAsJsonString("garbage"))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user", roles={"USER", "ADMIN", "OrgLegRep_10"})
    void putUpdateContractValidSaas() throws Exception
    {
        ContractTemplate template = new SaasContractTemplate();
        template.setProviderId("Participant:10");
        template.setConsumerId("Participant:20");

        mvc.perform(MockMvcRequestBuilders
                        .put("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer 1234")
                        .header("Active-Role", "OrgLegRep_10")
                        .content(objectAsJsonString(template))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles={"USER", "ADMIN", "OrgLegRep_10"})
    void putUpdateContractValidDataDelivery() throws Exception
    {
        ContractTemplate template = new DataDeliveryContractTemplate();
        template.setProviderId("Participant:10");
        template.setConsumerId("Participant:20");

        mvc.perform(MockMvcRequestBuilders
                        .put("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer 1234")
                        .header("Active-Role", "OrgLegRep_10")
                        .content(objectAsJsonString(template))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles={"USER", "ADMIN", "OrgLegRep_10"})
    void putUpdateContractInvalid() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .put("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer 1234")
                        .content("garbage")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user", roles={"USER", "ADMIN", "OrgLegRep_10"})
    void putUpdateContractForbidden() throws Exception
    {
        ContractTemplate template = new SaasContractTemplate();
        template.setProviderId("Participant:10");
        template.setConsumerId("Participant:20");

        mvc.perform(MockMvcRequestBuilders
                        .put("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer 1234")
                        .header("Active-Role", "garbage")
                        .content(objectAsJsonString(template))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles={"USER", "ADMIN", "OrgLegRep_10"})
    void patchTransitionContractForbidden() throws Exception
    {
        ContractTemplate template = new SaasContractTemplate();
        template.setProviderId("Participant:10");
        template.setConsumerId("Participant:20");

        mvc.perform(MockMvcRequestBuilders
                        .patch("/contract/status/1234/IN_DRAFT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer 1234")
                        .header("Active-Role", "garbage")
                        .content(objectAsJsonString(template))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles={"USER", "ADMIN", "OrgLegRep_10"})
    void patchTransitionContractValid() throws Exception
    {
        ContractTemplate template = new SaasContractTemplate();
        template.setProviderId("Participant:10");
        template.setConsumerId("Participant:20");

        mvc.perform(MockMvcRequestBuilders
                        .patch("/contract/status/1234/IN_DRAFT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer 1234")
                        .header("Active-Role", "OrgLegRep_10")
                        .content(objectAsJsonString(template))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles={"USER", "ADMIN", "OrgLegRep_10"})
    void getOrganizationContractsUnauthorized() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .get("/organization/Participant:99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer 1234")
                        .content("garbage")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles={"USER", "ADMIN", "OrgLegRep_10"})
    void getOrganizationContractsAuthorized() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .get("/organization/Participant:10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer 1234")
                        .content("garbage")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles={"USER", "ADMIN", "OrgLegRep_10"})
    void getContractDetailsValidRequest() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .get("/contract/Contract:1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer 1234")
                        .content("garbage")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

}
