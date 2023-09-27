package eu.merloteducation.contractorchestrator.controller;

import eu.merloteducation.contractorchestrator.auth.OrganizationRoleGrantedAuthority;
import eu.merloteducation.contractorchestrator.models.EdcIdResponse;
import eu.merloteducation.contractorchestrator.models.EdcNegotiationStatus;
import eu.merloteducation.contractorchestrator.models.EdcTransferStatus;
import eu.merloteducation.contractorchestrator.service.EdcOrchestrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/transfers")
public class DataTransferController {

    private static final String ROLE_PREFIX_REGEX = "(OrgLegRep|OrgRep)_";

    private static final String INVALID_ACTIVE_ROLE = "Invalid active role.";

    @Autowired
    private EdcOrchestrationService edcOrchestrationService;

    // TODO refactor to library
    private Set<String> getMerlotRoles() {
        // get roles from the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();


        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private Set<String> getRepresentedOrgaIds() {
        Set<String> roles = getMerlotRoles();
        // extract all orgaIds from the OrgRep and OrgLegRep Roles
        return roles
                .stream()
                .filter(s -> s.startsWith("ROLE_OrgRep_") || s.startsWith("ROLE_OrgLegRep_"))
                .map(s -> s.replace("ROLE_OrgRep_", "").replace("ROLE_OrgLegRep_", ""))
                .collect(Collectors.toSet());
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
    @PreAuthorize("@authorityChecker.representsOrganization(authentication, #activeRole.organizationId)")
    public EdcIdResponse startContractNegotiation(@PathVariable(value = "contractId") String contractId,
                                                  @RequestHeader(name = "Active-Role") OrganizationRoleGrantedAuthority activeRole,
                                                  @RequestHeader(name = "Authorization") String authToken) {
        Set<String> orgaIds = getRepresentedOrgaIds();

        return new EdcIdResponse(edcOrchestrationService.initiateConnectorNegotiation(contractId, activeRole.getOrganizationId(),
                orgaIds, authToken));
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
    @PreAuthorize("@authorityChecker.representsOrganization(authentication, #activeRole.organizationId)")
    public EdcNegotiationStatus getContractNegotiationStatus(@PathVariable(value = "contractId") String contractId,
                                                             @PathVariable(value = "negotiationId") String negotiationId,
                                                             @RequestHeader(name = "Active-Role") OrganizationRoleGrantedAuthority activeRole,
                                                             @RequestHeader(name = "Authorization") String authToken) {
        Set<String> orgaIds = getRepresentedOrgaIds();

        return new EdcNegotiationStatus(
                edcOrchestrationService.getNegotationStatus(negotiationId, contractId, activeRole.getOrganizationId(), orgaIds, authToken));
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
    @PreAuthorize("@authorityChecker.representsOrganization(authentication, #activeRole.organizationId)")
    public EdcIdResponse initiateEdcDataTransfer(@PathVariable(value = "contractId") String contractId,
                                                 @PathVariable(value = "negotiationId") String negotiationId,
                                                 @RequestHeader(name = "Active-Role") OrganizationRoleGrantedAuthority activeRole,
                                                 @RequestHeader(name = "Authorization") String authToken) {
        Set<String> orgaIds = getRepresentedOrgaIds();

        return new EdcIdResponse(edcOrchestrationService.initiateConnectorTransfer(negotiationId, contractId,
                activeRole.getOrganizationId(), orgaIds, authToken));
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
    @PreAuthorize("@authorityChecker.representsOrganization(authentication, #activeRole.organizationId)")
    public EdcTransferStatus getEdcTransferStatus(@PathVariable(value = "contractId") String contractId,
                                                  @PathVariable(value = "transferId") String transferId,
                                                  @RequestHeader(name = "Active-Role") OrganizationRoleGrantedAuthority activeRole,
                                                  @RequestHeader(name = "Authorization") String authToken) {
        Set<String> orgaIds = getRepresentedOrgaIds();

        return new EdcTransferStatus(edcOrchestrationService.getTransferStatus(transferId, contractId,
            activeRole.getOrganizationId(), orgaIds, authToken));
    }
}
