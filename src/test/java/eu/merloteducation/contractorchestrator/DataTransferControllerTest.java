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

import eu.merloteducation.authorizationlibrary.authorization.*;
import eu.merloteducation.authorizationlibrary.config.InterceptorConfig;
import eu.merloteducation.authorizationlibrary.config.MerlotSecurityConfig;
import eu.merloteducation.contractorchestrator.auth.ContractAuthorityChecker;
import eu.merloteducation.contractorchestrator.controller.DataTransferController;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.DataDeliveryContractTemplate;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import eu.merloteducation.contractorchestrator.security.WebSecurityConfig;
import eu.merloteducation.contractorchestrator.service.ContractStorageService;
import eu.merloteducation.contractorchestrator.service.EdcOrchestrationService;
import eu.merloteducation.contractorchestrator.service.MessageQueueService;
import eu.merloteducation.modelslib.edc.common.IdResponse;
import eu.merloteducation.modelslib.edc.negotiation.ContractNegotiation;
import eu.merloteducation.modelslib.edc.transfer.IonosS3TransferProcess;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({DataTransferController.class, WebSecurityConfig.class, ContractAuthorityChecker.class})
@Import({AuthorityChecker.class, ActiveRoleHeaderHandlerInterceptor.class, InterceptorConfig.class,
        MerlotSecurityConfig.class})
@AutoConfigureMockMvc()
class DataTransferControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserInfoOpaqueTokenIntrospector userInfoOpaqueTokenIntrospector;

    @MockBean
    private EdcOrchestrationService edcOrchestrationService;

    @MockBean
    private ContractStorageService contractStorageService;

    @MockBean
    private MessageQueueService messageQueueService;

    @MockBean
    private ContractTemplateRepository contractTemplateRepository;

    @MockBean
    JwtAuthConverter jwtAuthConverter;

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

        DataDeliveryContractTemplate template = new DataDeliveryContractTemplate();
        template.setProviderId("did:web:test.eu#orga-10");
        template.setConsumerId("did:web:test.eu#orga-20");

        lenient().when(contractTemplateRepository.findById(any())).thenReturn(Optional.of(template));

        lenient().when(edcOrchestrationService.initiateConnectorNegotiation(any(), any(), any())).thenReturn(idResponse);
        lenient().when(edcOrchestrationService.initiateConnectorTransfer(any(), any(), any(), any())).thenReturn(idResponse);
        lenient().when(edcOrchestrationService.getNegotationStatus(any(), any(), any(), any())).thenReturn(negotiation);
        lenient().when(edcOrchestrationService.getTransferStatus(any(), any(), any(), any())).thenReturn(transferProcess);
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
                        .header("Active-Role", "OrgLegRep_" + getParticipantId(10))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
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
                        .header("Active-Role", "OrgLegRep_" + getParticipantId(10))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
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
                        .header("Active-Role", "OrgLegRep_" + getParticipantId(10))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
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
                        .header("Active-Role", "OrgLegRep_" + getParticipantId(10))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
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
                        .header("Active-Role", "OrgLegRep_" + getParticipantId(20))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
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
                        .header("Active-Role", "OrgLegRep_" + getParticipantId(20))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
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
                        .header("Active-Role", "OrgLegRep_" + getParticipantId(20))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
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
                        .header("Active-Role", "OrgLegRep_" + getParticipantId(20))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, getParticipantId(10))
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    private String getParticipantId(int num) {
        return "did:web:"+ "test.eu" + "#orga-" + num;
    }

}
