package eu.merloteducation.contractorchestrator.controller;

import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import eu.merloteducation.contractorchestrator.service.ContractStorageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;

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

    /**
     * GET Health endpoint, return 200 when service is running.
     */
    @GetMapping("health")
    public void getHealth() {
        // always return code 200
    }

    /**
     * POST endpoint for creating a new contract in draft state.
     * The user specifies a ContractCreateRequest which gets saved in the system.
     *
     * @param principal             user data
     * @param authToken             active OAuth2 token of this user
     * @param contractCreateRequest request for creating contract
     * @return basic view of the created contract
     */
    @PostMapping("")
    @JsonView(ContractViews.DetailedView.class)
    public ContractTemplate addContractTemplate(@Valid @RequestBody ContractCreateRequest contractCreateRequest,
                                                @RequestHeader(name = "Authorization") String authToken,
                                                Principal principal) throws Exception {
        if (!getRepresentedOrgaIds(principal).contains(contractCreateRequest.getConsumerId().replace("Participant:", ""))) {
            throw new ResponseStatusException(FORBIDDEN, "No permission to create a contract for this organization.");
        }

        return this.contractStorageService.addContractTemplate(contractCreateRequest, authToken);
    }

    /**
     * PUT mapping for updating an existing contract template.
     *
     * @param editedContract contract template with updated fields
     * @param authToken      active OAuth2 token of this user
     * @param principal      user data
     * @return updated contract template
     */
    @PutMapping("")
    public ContractTemplate updateContractTemplate(@Valid @RequestBody ContractTemplate editedContract,
                                                   @RequestHeader(name = "Authorization") String authToken,
                                                   @RequestHeader(name = "Active-Role") String activeRole,
                                                   Principal principal) throws Exception {
        Set<String> orgaIds = getRepresentedOrgaIds(principal);
        String activeRoleOrgaId = activeRole.replaceFirst("(OrgLegRep|OrgRep)_", "");
        if (!orgaIds.contains(activeRoleOrgaId)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid active role.");
        }

        return contractStorageService.updateContractTemplate(editedContract, authToken, activeRoleOrgaId, getRepresentedOrgaIds(principal));
    }

    /**
     * PATCH mapping for transitioning the contract template with the given id to the given state.
     *
     * @param contractId id of contract template to transition
     * @param status     target state
     * @param activeRole active user role
     * @param principal  user data
     * @return updated contract template
     */
    @PatchMapping("/contract/status/{contractId}/{status}")
    public ContractTemplate transitionContractTemplate(@PathVariable(value = "contractId") String contractId,
                                                       @PathVariable(value = "status") ContractState status,
                                                       @RequestHeader(name = "Active-Role") String activeRole,
                                                       Principal principal) throws Exception {
        Set<String> orgaIds = getRepresentedOrgaIds(principal);
        JwtAuthenticationToken authenticationToken = (JwtAuthenticationToken) principal;
        Jwt jwt = (Jwt) authenticationToken.getCredentials();
        String userId = (String) jwt.getClaims().get("sub");

        String activeRoleOrgaId = activeRole.replaceFirst("(OrgLegRep|OrgRep)_", "");
        if (!orgaIds.contains(activeRoleOrgaId)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid active role.");
        }

        return contractStorageService.transitionContractTemplateState(contractId, status, activeRoleOrgaId, userId);
    }


    /**
     * GET endpoint for querying all contracts that are associated with the specified organization id.
     *
     * @param page      page of pageable
     * @param size      size of pageable
     * @param orgaId    organization id to query
     * @param principal user data
     * @return page of contracts related to this organization
     */
    @GetMapping("organization/{orgaId}")
    @JsonView(ContractViews.BasicView.class)
    public Page<ContractTemplate> getOrganizationContracts(@RequestParam(value = "page", defaultValue = "0") int page,
                                                           @RequestParam(value = "size", defaultValue = "9") int size,
                                                           @PathVariable(value = "orgaId") String orgaId,
                                                           Principal principal) {
        if (!getRepresentedOrgaIds(principal).contains(orgaId.replace("Participant:", ""))) {
            throw new ResponseStatusException(FORBIDDEN, "No permission to access contracts of this id.");
        }

        return contractStorageService.getOrganizationContracts(orgaId, PageRequest.of(page, size,
                Sort.by("creationDate").descending()));
    }

    /**
     * GET endpoint for requesting detailed information about a contract with the given id.
     *
     * @param contractId id of the contract
     * @param principal  user data
     * @return detailed view of this contract
     */
    @GetMapping("contract/{contractId}")
    public ContractTemplate getContractDetails(@PathVariable(value = "contractId") String contractId,
                                               Principal principal) {

        return contractStorageService.getContractDetails(contractId, getRepresentedOrgaIds(principal));
    }

}
