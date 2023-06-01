package eu.merloteducation.contractorchestrator.controller;

import eu.merloteducation.contractorchestrator.models.entities.Contract;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.service.ContractStorageService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
@RequestMapping("/")
public class ContractsController {

    @Autowired
    private ContractStorageService contractStorageService;

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

    @GetMapping("health")
    public void getHealth() {
        // always return code 200
    }

    @PostMapping("")
    public Contract addContractTemplate(Principal principal, @RequestHeader (name="Authorization") String authToken,
                                        HttpServletResponse response,
                                        @Valid @RequestBody ContractCreateRequest contractCreateRequest) {
        if (!getRepresentedOrgaIds(principal).contains(contractCreateRequest.getConsumerId().replace("Participant:", ""))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        authentication.getAuthorities()
        return this.contractStorageService.addContractTemplate(contractCreateRequest, authToken);
    }

}
