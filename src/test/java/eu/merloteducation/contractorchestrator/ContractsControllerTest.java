package eu.merloteducation.contractorchestrator;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.merloteducation.contractorchestrator.auth.*;
import eu.merloteducation.contractorchestrator.controller.ContractsController;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.models.dto.ContractBasicDto;
import eu.merloteducation.contractorchestrator.models.dto.ContractDto;
import eu.merloteducation.contractorchestrator.models.dto.saas.SaasContractDetailsDto;
import eu.merloteducation.contractorchestrator.models.dto.saas.SaasContractDto;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.SaasContractTemplate;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import eu.merloteducation.contractorchestrator.security.WebSecurityConfig;
import eu.merloteducation.contractorchestrator.service.ContractStorageService;
import eu.merloteducation.contractorchestrator.service.EdcOrchestrationService;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ContractsController.class, WebSecurityConfig.class, AuthorityChecker.class,
        ContractAuthorityChecker.class, ActiveRoleHeaderHandlerInterceptor.class})
@AutoConfigureMockMvc()
class ContractsControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ContractStorageService contractStorageService;

    @MockBean
    private ContractTemplateRepository contractTemplateRepository;

    @MockBean
    private EdcOrchestrationService edcOrchestrationService;

    @Autowired
    private JwtAuthConverter jwtAuthConverter;

    @MockBean
    private JwtAuthConverterProperties jwtAuthConverterProperties;

    private SaasContractTemplate template;


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

        template = new SaasContractTemplate();
        template.setProviderId("Participant:10");
        template.setConsumerId("Participant:20");

        List<ContractDto> contractTemplates = new ArrayList<>();
        SaasContractDto saasContractDto = new SaasContractDto();
        saasContractDto.setDetails(new SaasContractDetailsDto());
        saasContractDto.getDetails().setProviderId("Participant:10");
        saasContractDto.getDetails().setConsumerId("Participant:20");
        contractTemplates.add(saasContractDto);

        lenient().when(contractTemplateRepository.findById(template.getId())).thenReturn(Optional.of(template));

        ContractBasicDto contractBasicDto = new ContractBasicDto();
        contractBasicDto.setId(template.getId());
        List<ContractBasicDto> contractDtos = new ArrayList<>();
        contractDtos.add(contractBasicDto);

        Page<ContractBasicDto> contractTemplatesPage = new PageImpl<>(contractDtos);

        lenient().when(contractStorageService.addContractTemplate(any(), any()))
                .thenReturn(saasContractDto);
        lenient().when(contractStorageService.getContractDetails(any(), any()))
                .thenReturn(saasContractDto);
        lenient().when(contractStorageService.updateContractTemplate(any(), any(), any()))
                .thenReturn(saasContractDto);
        lenient().when(contractStorageService.getOrganizationContracts(any(), any(), any(), any()))
                .thenReturn(contractTemplatesPage);
        lenient().when(contractStorageService.getContractAttachment(any(), any(), any()))
                .thenReturn(new byte[]{0x01, 0x02, 0x03, 0x04});
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
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
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
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
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
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void postRegenerateContractAuthorized() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .post("/contract/regenerate/" + template.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "OrgLegRep_10")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void postRegenerateContractUnauthorized() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .post("/contract/regenerate/" + template.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "OrgLegRep_99")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_99")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void putUpdateContractValid() throws Exception
    {
        SaasContractDto contractDto = new SaasContractDto();
        contractDto.setDetails(new SaasContractDetailsDto());
        contractDto.getDetails().setProviderId("Participant:10");
        contractDto.getDetails().setConsumerId("Participant:20");
        contractDto.getDetails().setId(template.getId());

        mvc.perform(MockMvcRequestBuilders
                        .put("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "OrgLegRep_10")
                        .content(objectAsJsonString(contractDto))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
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
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void putUpdateContractForbidden() throws Exception
    {
        SaasContractDto contractDto = new SaasContractDto();
        contractDto.setDetails(new SaasContractDetailsDto());
        contractDto.getDetails().setProviderId("Participant:10");
        contractDto.getDetails().setConsumerId("Participant:20");

        mvc.perform(MockMvcRequestBuilders
                        .put("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "OrgLegRep_20")
                        .content(objectAsJsonString(contractDto))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
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
                        .patch("/contract/status/" + template.getId() + "/IN_DRAFT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "OrgLegRep_20")
                        .content(objectAsJsonString(template))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchTransitionContractValid() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .patch("/contract/status/" + template.getId() + "/IN_DRAFT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .header("Active-Role", "OrgLegRep_10")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
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
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
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
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getContractDetailsValidRequestConsumer() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .get("/contract/" + template.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getContractDetailsValidRequestProvider() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .get("/contract/" + template.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getContractAttachmentAsProvider() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .get("/contract/" + template.getId() + "/attachment/" + "1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getContractAttachmentAsConsumer() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .get("/contract/" + template.getId() + "/attachment/" + "1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getContractAttachmentAsOther() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .get("/contract/" + template.getId() + "/attachment/" + "1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_1234")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void addContractAttachmentPdfAsProvider() throws Exception
    {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "test.pdf",
                MediaType.APPLICATION_PDF_VALUE, Base64.getDecoder()
                .decode(("JVBERi0xLjIgCjkgMCBvYmoKPDwKPj4Kc3RyZWFtCkJULyA5IFRmKFRlc3QpJyBFVAplbmRzdHJlYW0KZW" +
                        "5kb2JqCjQgMCBvYmoKPDwKL1R5cGUgL1BhZ2UKL1BhcmVudCA1IDAgUgovQ29udGVudHMgOSAwIFIKPj4KZ" +
                        "W5kb2JqCjUgMCBvYmoKPDwKL0tpZHMgWzQgMCBSIF0KL0NvdW50IDEKL1R5cGUgL1BhZ2VzCi9NZWRpYUJv" +
                        "eCBbIDAgMCA5OSA5IF0KPj4KZW5kb2JqCjMgMCBvYmoKPDwKL1BhZ2VzIDUgMCBSCi9UeXBlIC9DYXRhbG9n" +
                        "Cj4+CmVuZG9iagp0cmFpbGVyCjw8Ci9Sb290IDMgMCBSCj4+CiUlRU9G")
                        .getBytes(StandardCharsets.UTF_8)));
        mvc.perform(multipart(HttpMethod.PATCH, "/contract/" + template.getId() + "/attachment")
                        .file(multipartFile)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void addContractAttachmentTxtAsProvider() throws Exception
    {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "test.txt",
                "text/plain", "Spring Framework".getBytes());
        mvc.perform(multipart(HttpMethod.PATCH, "/contract/" + template.getId() + "/attachment")
                        .file(multipartFile)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void addContractAttachmentPdfAsConsumer() throws Exception
    {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "test.pdf",
                MediaType.APPLICATION_PDF_VALUE, Base64.getDecoder()
                .decode(("JVBERi0xLjIgCjkgMCBvYmoKPDwKPj4Kc3RyZWFtCkJULyA5IFRmKFRlc3QpJyBFVAplbmRzdHJlYW0KZW" +
                        "5kb2JqCjQgMCBvYmoKPDwKL1R5cGUgL1BhZ2UKL1BhcmVudCA1IDAgUgovQ29udGVudHMgOSAwIFIKPj4KZ" +
                        "W5kb2JqCjUgMCBvYmoKPDwKL0tpZHMgWzQgMCBSIF0KL0NvdW50IDEKL1R5cGUgL1BhZ2VzCi9NZWRpYUJv" +
                        "eCBbIDAgMCA5OSA5IF0KPj4KZW5kb2JqCjMgMCBvYmoKPDwKL1BhZ2VzIDUgMCBSCi9UeXBlIC9DYXRhbG9n" +
                        "Cj4+CmVuZG9iagp0cmFpbGVyCjw8Ci9Sb290IDMgMCBSCj4+CiUlRU9G")
                        .getBytes(StandardCharsets.UTF_8)));
        mvc.perform(multipart(HttpMethod.PATCH, "/contract/" + template.getId() + "/attachment")
                        .file(multipartFile)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteContractAttachmentAsProvider() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .delete("/contract/" + template.getId() + "/attachment/" + "1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void deleteContractAttachmentAsConsumer() throws Exception
    {
        mvc.perform(MockMvcRequestBuilders
                        .delete("/contract/" + template.getId() + "/attachment/" + "1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

}
