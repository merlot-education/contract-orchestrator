package eu.merloteducation.contractorchestrator.controller;

import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.contractorchestrator.models.EdcIdResponse;
import eu.merloteducation.contractorchestrator.models.EdcNegotiationStatus;
import eu.merloteducation.contractorchestrator.models.EdcTransferStatus;
import eu.merloteducation.contractorchestrator.service.EdcOrchestrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfers")
public class DataTransferController {

    @Autowired
    private EdcOrchestrationService edcOrchestrationService;

    /**
     * POST request to start automated contract negotiation over a given contract id.
     *
     * @param contractId contract id
     * @param activeRole currently selected role by the user in the frontend
     * @param authToken  active OAuth2 token of this user
     * @return negotiation initiation response
     */
    @PostMapping("/contract/{contractId}/negotiation/start")
    @PreAuthorize("@contractAuthorityChecker.canAccessContract(authentication, #contractId)")
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
    @PreAuthorize("@contractAuthorityChecker.canAccessContract(authentication, #contractId)")
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
    @PreAuthorize("@contractAuthorityChecker.canAccessContract(authentication, #contractId)")
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
    @PreAuthorize("@contractAuthorityChecker.canAccessContract(authentication, #contractId)")
    public EdcTransferStatus getEdcTransferStatus(@PathVariable(value = "contractId") String contractId,
                                                  @PathVariable(value = "transferId") String transferId,
                                                  @RequestHeader(name = "Active-Role") OrganizationRoleGrantedAuthority activeRole,
                                                  @RequestHeader(name = "Authorization") String authToken) {
        return new EdcTransferStatus(edcOrchestrationService.getTransferStatus(transferId, contractId,
            activeRole.getOrganizationId(), authToken));
    }
}
