package eu.merloteducation.contractorchestrator.controller;

import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.models.dto.ContractBasicDto;
import eu.merloteducation.contractorchestrator.models.dto.ContractDto;
import eu.merloteducation.contractorchestrator.models.entities.*;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import eu.merloteducation.contractorchestrator.service.ContractStorageService;
import eu.merloteducation.contractorchestrator.service.EdcOrchestrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/")
public class ContractsController {

    @Autowired
    private ContractStorageService contractStorageService;

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
     * POST endpoint for creating a new contract in draft state.
     * The user specifies a ContractCreateRequest which gets saved in the system.
     *
     * @param authToken             active OAuth2 token of this user
     * @param contractCreateRequest request for creating contract
     * @return basic view of the created contract
     */
    @PostMapping("")
    @JsonView(ContractViews.ConsumerView.class)
    public ContractDto addContractTemplate(@Valid @RequestBody ContractCreateRequest contractCreateRequest,
                                                  @RequestHeader(name = "Authorization") String authToken) {
        if (!getRepresentedOrgaIds().contains(contractCreateRequest.getConsumerId().replace("Participant:", ""))) {
            throw new ResponseStatusException(FORBIDDEN, "No permission to create a contract for this organization.");
        }

        return this.contractStorageService.addContractTemplate(contractCreateRequest, authToken);
    }

    /**
     * PUT mapping for updating an existing contract template.
     *
     * @param editedContract contract template with updated fields
     * @param authToken      active OAuth2 token of this user
     * @return updated contract template
     */
    @PutMapping("")
    public ContractDto updateContractTemplate(@Valid @RequestBody ContractDto editedContract,
                                                     @RequestHeader(name = "Authorization") String authToken,
                                                     @RequestHeader(name = "Active-Role") String activeRole) {
        Set<String> orgaIds = getRepresentedOrgaIds();
        String activeRoleOrgaId = activeRole.replaceFirst("(OrgLegRep|OrgRep)_", "");
        if (!orgaIds.contains(activeRoleOrgaId)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid active role.");
        }

        return contractStorageService.updateContractTemplate(editedContract, authToken, activeRoleOrgaId);
    }

    /**
     * POST mapping for copying an existing deleted/archived contract into a new template with the same filled fields
     * but a new id and signature.
     *
     * @param contractId id of contract to copy
     * @param authToken  active OAuth2 token of this user
     * @return newly generated contract
     */
    @PostMapping("/contract/regenerate/{contractId}")
    public ContractDto regenerateContractTemplate(@PathVariable(value = "contractId") String contractId,
                                                         @RequestHeader(name = "Authorization") String authToken) {
        Set<String> orgaIds = getRepresentedOrgaIds();

        return contractStorageService.regenerateContract(contractId, orgaIds, authToken);
    }

    /**
     * PATCH mapping for transitioning the contract template with the given id to the given state.
     *
     * @param contractId id of contract template to transition
     * @param status     target state
     * @param activeRole active user role
     * @param principal  user data
     * @param authToken  active OAuth2 token of this user
     * @return updated contract template
     */
    @PatchMapping("/contract/status/{contractId}/{status}")
    public ContractDto transitionContractTemplate(@PathVariable(value = "contractId") String contractId,
                                                         @PathVariable(value = "status") ContractState status,
                                                         @RequestHeader(name = "Active-Role") String activeRole,
                                                         @RequestHeader(name = "Authorization") String authToken,
                                                         Principal principal) {
        Set<String> orgaIds = getRepresentedOrgaIds();
        JwtAuthenticationToken authenticationToken = (JwtAuthenticationToken) principal;
        Jwt jwt = (Jwt) authenticationToken.getCredentials();
        String userId = (String) jwt.getClaims().get("sub");

        String activeRoleOrgaId = activeRole.replaceFirst("(OrgLegRep|OrgRep)_", "");
        if (!orgaIds.contains(activeRoleOrgaId)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid active role.");
        }

        return contractStorageService.transitionContractTemplateState(contractId, status, activeRoleOrgaId, userId, authToken);
    }


    /**
     * GET endpoint for querying all contracts that are associated with the specified organization id.
     *
     * @param page      page of pageable
     * @param size      size of pageable
     * @param status    optional status filter
     * @param orgaId    organization id to query
     * @param authToken active OAuth2 token of this user
     * @return page of contracts related to this organization
     */
    @GetMapping("organization/{orgaId}")
    @JsonView(ContractViews.BasicView.class)
    public Page<ContractBasicDto> getOrganizationContracts(@RequestParam(value = "page", defaultValue = "0") int page,
                                                           @RequestParam(value = "size", defaultValue = "9") @Max(15) int size,
                                                           @RequestParam(value = "status", required = false) ContractState status,
                                                           @PathVariable(value = "orgaId") String orgaId,
                                                           @RequestHeader(name = "Authorization") String authToken) {
        if (!getRepresentedOrgaIds().contains(orgaId.replace("Participant:", ""))) {
            throw new ResponseStatusException(FORBIDDEN, "No permission to access contracts of this id.");
        }

        return contractStorageService.getOrganizationContracts(orgaId, PageRequest.of(page, size,
                Sort.by("creationDate").descending()), status, authToken);
    }

    /**
     * GET endpoint for requesting detailed information about a contract with the given id.
     *
     * @param contractId id of the contract
     * @param authToken  active OAuth2 token of this user
     * @return detailed view of this contract
     */
    @GetMapping("contract/{contractId}")
    public ContractDto getContractDetails(@PathVariable(value = "contractId") String contractId,
                                                 @RequestHeader(name = "Authorization") String authToken) {
        return contractStorageService.getContractDetails(contractId, getRepresentedOrgaIds(), authToken);
    }

}
