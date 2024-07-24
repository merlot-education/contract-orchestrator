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

package eu.merloteducation.contractorchestrator.controller;

import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.contractorchestrator.service.EdcOrchestrationService;
import eu.merloteducation.modelslib.edc.EdcIdResponse;
import eu.merloteducation.modelslib.edc.EdcNegotiationStatus;
import eu.merloteducation.modelslib.edc.EdcTransferStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfers")
public class DataTransferController {
    private final EdcOrchestrationService edcOrchestrationService;

    public DataTransferController(@Autowired EdcOrchestrationService edcOrchestrationService) {
        this.edcOrchestrationService = edcOrchestrationService;
    }

    /**
     * POST request to start automated contract negotiation over a given contract id.
     *
     * @param contractId contract id
     * @param activeRole currently selected role by the user in the frontend
     * @param authToken  active OAuth2 token of this user
     * @return negotiation initiation response
     */
    @PostMapping("/contract/{contractId}/negotiation/start")
    @PreAuthorize("@contractAuthorityChecker.canAccessContract(authentication, #contractId) " +
            "&& #activeRole.isRepresentative()")
    public EdcIdResponse startContractNegotiation(@PathVariable(value = "contractId") String contractId,
                                                  @RequestHeader(name = "Active-Role") OrganizationRoleGrantedAuthority activeRole,
                                                  @RequestHeader(name = "Authorization") String authToken) {
        return new EdcIdResponse(edcOrchestrationService.initiateConnectorNegotiation(contractId, activeRole.getOrganizationId(), authToken));
    }

    /**
     * GET request to get the current status of the automated negotiation over a contract.
     *
     * @param contractId    contract id
     * @param negotiationId negotiation id
     * @param activeRole    currently selected role by the user in the frontend
     * @param authToken  active OAuth2 token of this user
     * @return status of negotiation
     */
    @GetMapping("/contract/{contractId}/negotiation/{negotiationId}/status")
    @PreAuthorize("@contractAuthorityChecker.canAccessContract(authentication, #contractId) " +
            "&& #activeRole.isRepresentative()")
    public EdcNegotiationStatus getContractNegotiationStatus(@PathVariable(value = "contractId") String contractId,
                                                             @PathVariable(value = "negotiationId") String negotiationId,
                                                             @RequestHeader(name = "Active-Role") OrganizationRoleGrantedAuthority activeRole,
                                                             @RequestHeader(name = "Authorization") String authToken) {
        return new EdcNegotiationStatus(
                edcOrchestrationService.getNegotationStatus(negotiationId, contractId, activeRole.getOrganizationId(),
                        authToken));
    }

    /**
     * POST request for initiating a transfer over a succeded automated negotiation of a contract
     *
     * @param contractId    contract id
     * @param negotiationId negotiation id
     * @param activeRole    currently selected role by the user in the frontend
     * @param authToken  active OAuth2 token of this user
     * @return transfer initiation response
     */
    @PostMapping("/contract/{contractId}/negotiation/{negotiationId}/transfer/start")
    @PreAuthorize("@contractAuthorityChecker.canAccessContract(authentication, #contractId) " +
            "&& #activeRole.isRepresentative()")
    public EdcIdResponse initiateEdcDataTransfer(@PathVariable(value = "contractId") String contractId,
                                                 @PathVariable(value = "negotiationId") String negotiationId,
                                                 @RequestHeader(name = "Active-Role") OrganizationRoleGrantedAuthority activeRole,
                                                 @RequestHeader(name = "Authorization") String authToken) {
        return new EdcIdResponse(edcOrchestrationService.initiateConnectorTransfer(negotiationId, contractId,
                activeRole.getOrganizationId(), authToken));
    }

    /**
     * GET request to get the current transfer status of a given transfer of a contract.
     *
     * @param contractId contract id
     * @param transferId transfer id
     * @param activeRole currently selected role by the user in the frontend
     * @param authToken  active OAuth2 token of this user
     * @return status of transfer
     */
    @GetMapping("/contract/{contractId}/transfer/{transferId}/status")
    @PreAuthorize("@contractAuthorityChecker.canAccessContract(authentication, #contractId) " +
            "&& #activeRole.isRepresentative()")
    public EdcTransferStatus getEdcTransferStatus(@PathVariable(value = "contractId") String contractId,
                                                  @PathVariable(value = "transferId") String transferId,
                                                  @RequestHeader(name = "Active-Role") OrganizationRoleGrantedAuthority activeRole,
                                                  @RequestHeader(name = "Authorization") String authToken) {
        return new EdcTransferStatus(edcOrchestrationService.getTransferStatus(transferId, contractId,
            activeRole.getOrganizationId(), authToken));
    }
}
