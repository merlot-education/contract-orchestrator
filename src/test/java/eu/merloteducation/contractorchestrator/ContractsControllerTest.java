package eu.merloteducation.contractorchestrator;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.merloteducation.contractorchestrator.controller.ContractsController;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.DataDeliveryContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.SaasContractTemplate;
import eu.merloteducation.contractorchestrator.security.JwtAuthConverter;
import eu.merloteducation.contractorchestrator.security.JwtAuthConverterProperties;
import eu.merloteducation.contractorchestrator.security.WebSecurityConfig;
import eu.merloteducation.contractorchestrator.service.ContractStorageService;
import eu.merloteducation.contractorchestrator.service.EdcOrchestrationService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ContractsController.class, WebSecurityConfig.class})
@AutoConfigureMockMvc()
class ContractsControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ContractStorageService contractStorageService;

    @MockBean
    private EdcOrchestrationService edcOrchestrationService;

    @Autowired
    private JwtAuthConverter jwtAuthConverter;

    @MockBean
    private JwtAuthConverterProperties jwtAuthConverterProperties;


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
    void postAddContractUnauthorized() throws Exception
    {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setOfferingId("ServiceOffering:1234");
        request.setConsumerId("Participant:99");

        mvc.perform(MockMvcRequestBuilders
                        .post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .content(objectAsJsonString(request))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void postAddContractAuthorizedValid() throws Exception
    {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setOfferingId("ServiceOffering:1234");
        request.setConsumerId("Participant:10");

        mvc.perform(MockMvcRequestBuilders
                        .post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .content(objectAsJsonString(request))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void postAddContractAuthorizedInvalid() throws Exception
    {

        mvc.perform(MockMvcRequestBuilders
                        .post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .content(objectAsJsonString("garbage"))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void putUpdateContractValid() throws Exception
    {
        ContractTemplate template = new SaasContractTemplate();
        template.setProviderId("Participant:10");
        template.setConsumerId("Participant:20");

        mvc.perform(MockMvcRequestBuilders
                        .put("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "OrgLegRep_10")
                        .content(objectAsJsonString(template))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void putUpdateContractInvalid() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .put("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .content("garbage")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void putUpdateContractForbidden() throws Exception
    {
        ContractTemplate template = new SaasContractTemplate();
        template.setProviderId("Participant:10");
        template.setConsumerId("Participant:20");

        mvc.perform(MockMvcRequestBuilders
                        .put("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "garbage")
                        .content(objectAsJsonString(template))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void patchTransitionContractForbidden() throws Exception
    {
        ContractTemplate template = new SaasContractTemplate();
        template.setProviderId("Participant:10");
        template.setConsumerId("Participant:20");

        mvc.perform(MockMvcRequestBuilders
                        .patch("/contract/status/1234/IN_DRAFT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "garbage")
                        .content(objectAsJsonString(template))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchTransitionContractValid() throws Exception
    {
        ContractTemplate template = new SaasContractTemplate();
        template.setProviderId("Participant:10");
        template.setConsumerId("Participant:20");

        mvc.perform(MockMvcRequestBuilders
                        .patch("/contract/status/1234/IN_DRAFT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "OrgLegRep_10")
                        .content(objectAsJsonString(template))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getOrganizationContractsUnauthorized() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .get("/organization/Participant:99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrganizationContractsAuthorized() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .get("/organization/Participant:10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getContractDetailsValidRequestConsumer() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .get("/contract/Contract:1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getContractDetailsValidRequestProvider() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .get("/contract/Contract:1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

}
