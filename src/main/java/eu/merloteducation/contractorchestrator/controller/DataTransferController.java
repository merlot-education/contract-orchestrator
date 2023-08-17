package eu.merloteducation.contractorchestrator.controller;

import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.models.EdcIdResponse;
import eu.merloteducation.contractorchestrator.models.EdcNegotiationStatus;
import eu.merloteducation.contractorchestrator.models.EdcTransferStatus;
import eu.merloteducation.contractorchestrator.models.edc.common.IdResponse;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractNegotiation;
import eu.merloteducation.contractorchestrator.models.edc.transfer.IonosS3TransferProcess;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import eu.merloteducation.contractorchestrator.service.EdcOrchestrationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/transfers")
public class DataTransferController {

    @Autowired
    private EdcOrchestrationService edcOrchestrationService;

    // TODO refactor to library
    private Set<String> getMerlotRoles(Principal principal) {
        // get roles from the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();


        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private Set<String> getRepresentedOrgaIds(Principal principal) {
        Set<String> roles = getMerlotRoles(principal);
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
     * @param principal  user auth data
     * @return negotiation initiation response
     */
    @PostMapping("/contract/{contractId}/negotiation/start")
    public EdcIdResponse startContractNegotiation(@PathVariable(value = "contractId") String contractId,
                                                  @RequestHeader(name = "Active-Role") String activeRole,
                                                  Principal principal) {
        String activeRoleOrgaId = activeRole.replaceFirst("(OrgLegRep|OrgRep)_", "");
        Set<String> orgaIds = getRepresentedOrgaIds(principal);
        if (!orgaIds.contains(activeRoleOrgaId)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid active role.");
        }
        return new EdcIdResponse(edcOrchestrationService.initiateConnectorNegotiation(contractId, activeRoleOrgaId, orgaIds));
    }

    /**
     * GET request to get the current status of the automated negotiation over a contract.
     *
     * @param contractId    contract id
     * @param negotiationId negotiation id
     * @param activeRole    currently selected role by the user in the frontend
     * @param principal     user auth data
     * @return status of negotiation
     */
    @GetMapping("/contract/{contractId}/negotiation/{negotiationId}/status")
    public EdcNegotiationStatus getContractNegotiationStatus(@PathVariable(value = "contractId") String contractId,
                                                             @PathVariable(value = "negotiationId") String negotiationId,
                                                             @RequestHeader(name = "Active-Role") String activeRole,
                                                             Principal principal) {
        String activeRoleOrgaId = activeRole.replaceFirst("(OrgLegRep|OrgRep)_", "");
        Set<String> orgaIds = getRepresentedOrgaIds(principal);
        if (!orgaIds.contains(activeRoleOrgaId)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid active role.");
        }
        return new EdcNegotiationStatus(
                edcOrchestrationService.getNegotationStatus(negotiationId, contractId, activeRoleOrgaId, orgaIds));
    }

    /**
     * POST request for initiating a transfer over a succeded automated negotiation of a contract
     *
     * @param contractId    contract id
     * @param negotiationId negotiation id
     * @param activeRole    currently selected role by the user in the frontend
     * @param principal     user auth data
     * @return transfer initiation response
     */
    @PostMapping("/contract/{contractId}/negotiation/{negotiationId}/transfer/start")
    public EdcIdResponse initiateEdcDataTransfer(@PathVariable(value = "contractId") String contractId,
                                                 @PathVariable(value = "negotiationId") String negotiationId,
                                                 @RequestHeader(name = "Active-Role") String activeRole,
                                                 Principal principal) {
        String activeRoleOrgaId = activeRole.replaceFirst("(OrgLegRep|OrgRep)_", "");
        Set<String> orgaIds = getRepresentedOrgaIds(principal);
        if (!orgaIds.contains(activeRoleOrgaId)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid active role.");
        }
        return new EdcIdResponse(edcOrchestrationService.initiateConnectorTransfer(negotiationId, contractId, activeRoleOrgaId, orgaIds));
    }

    /**
     * GET request to get the current transfer status of a given transfer of a contract.
     *
     * @param contractId contract id
     * @param transferId transfer id
     * @param activeRole currently selected role by the user in the frontend
     * @param principal  user auth data
     * @return status of transfer
     */
    @GetMapping("/contract/{contractId}/transfer/{transferId}/status")
    public EdcTransferStatus getEdcTransferStatus(@PathVariable(value = "contractId") String contractId,
                                                  @PathVariable(value = "transferId") String transferId,
                                                  @RequestHeader(name = "Active-Role") String activeRole,
                                                  Principal principal) {
        String activeRoleOrgaId = activeRole.replaceFirst("(OrgLegRep|OrgRep)_", "");
        Set<String> orgaIds = getRepresentedOrgaIds(principal);
        if (!orgaIds.contains(activeRoleOrgaId)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid active role.");
        }
        return new EdcTransferStatus(edcOrchestrationService.getTransferStatus(transferId, contractId, activeRoleOrgaId, orgaIds));
    }
}
