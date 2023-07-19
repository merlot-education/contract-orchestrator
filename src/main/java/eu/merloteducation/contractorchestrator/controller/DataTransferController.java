package eu.merloteducation.contractorchestrator.controller;

import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
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
@CrossOrigin
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

    @PostMapping("/contract/{contractId}/negotiation/start")
    public IdResponse startContractNegotiation(@PathVariable(value = "contractId") String contractId,
                                               @RequestHeader(name = "Active-Role") String activeRole,
                                               Principal principal) throws Exception {
        Set<String> orgaIds = getRepresentedOrgaIds(principal);
        return edcOrchestrationService.initiateConnectorNegotiation(contractId, activeRole, orgaIds);
    }

    @GetMapping("/contract/{contractId}/negotiation/{negotiationId}/status")
    public ContractNegotiation getContractNegotiationStatus(@PathVariable(value = "contractId") String contractId,
                                                            @PathVariable(value = "negotiationId") String negotiationId,
                                                            @RequestHeader(name = "Active-Role") String activeRole,
                                                            Principal principal) throws Exception {
        Set<String> orgaIds = getRepresentedOrgaIds(principal);
        return edcOrchestrationService.getNegotationStatus(negotiationId, contractId, activeRole, orgaIds);
    }

    @PostMapping("/contract/{contractId}/negotiation/{negotiationId}/transfer/start")
    public IdResponse initiateEdcDataTransfer(@PathVariable(value = "contractId") String contractId,
                                              @PathVariable(value = "negotiationId") String negotiationId,
                                               @RequestHeader(name = "Active-Role") String activeRole,
                                               Principal principal) throws Exception {
        Set<String> orgaIds = getRepresentedOrgaIds(principal);
        return edcOrchestrationService.initiateConnectorTransfer(negotiationId, contractId, activeRole, orgaIds);
    }

    @GetMapping("/contract/{contractId}/transfer/{transferId}/status")
    public IonosS3TransferProcess getEdcTransferStatus(@PathVariable(value = "contractId") String contractId,
                                                       @PathVariable(value = "transferId") String transferId,
                                                       @RequestHeader(name = "Active-Role") String activeRole,
                                                       Principal principal) throws Exception {
        Set<String> orgaIds = getRepresentedOrgaIds(principal);
        return edcOrchestrationService.getTransferStatus(transferId, contractId, activeRole, orgaIds);
    }
}
