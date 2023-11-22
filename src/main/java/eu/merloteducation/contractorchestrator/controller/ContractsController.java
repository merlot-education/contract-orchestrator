package eu.merloteducation.contractorchestrator.controller;

import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.contractorchestrator.auth.OrganizationRoleGrantedAuthority;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.models.dto.ContractBasicDto;
import eu.merloteducation.contractorchestrator.models.dto.ContractDto;
import eu.merloteducation.contractorchestrator.models.entities.*;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import eu.merloteducation.contractorchestrator.service.ContractStorageService;
import eu.merloteducation.contractorchestrator.service.EdcOrchestrationService;
import eu.merloteducation.s3library.service.StorageClientException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping("/")
public class ContractsController {

    @Autowired
    private ContractStorageService contractStorageService;

    @Autowired
    private EdcOrchestrationService edcOrchestrationService;

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
    @PreAuthorize("@authorityChecker.representsOrganization(authentication, #contractCreateRequest.consumerId)")
    public ContractDto addContractTemplate(@Valid @RequestBody ContractCreateRequest contractCreateRequest,
                                           @RequestHeader(name = "Authorization") String authToken) {
        return this.contractStorageService.addContractTemplate(contractCreateRequest, authToken);
    }

    /**
     * PUT mapping for updating an existing contract template.
     *
     * @param editedContract contract template with updated fields
     * @param authToken      active OAuth2 token of this user
     * @param activeRole active user role
     * @return updated contract template
     */
    @PutMapping("")
    @PreAuthorize("@contractAuthorityChecker.canAccessContract(authentication, #editedContract.details.id)")
    public ContractDto updateContractTemplate(@Valid @RequestBody ContractDto editedContract,
                                              @RequestHeader(name = "Authorization") String authToken,
                                              @RequestHeader(name = "Active-Role") OrganizationRoleGrantedAuthority activeRole) {
        return contractStorageService.updateContractTemplate(editedContract, authToken, activeRole.getOrganizationId());
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
    @PreAuthorize("@contractAuthorityChecker.canAccessContract(authentication, #contractId)")
    public ContractDto regenerateContractTemplate(@PathVariable(value = "contractId") String contractId,
                                                  @RequestHeader(name = "Authorization") String authToken) {
        return contractStorageService.regenerateContract(contractId, authToken);
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
    @PreAuthorize("@contractAuthorityChecker.canAccessContract(authentication, #contractId)")
    public ContractDto transitionContractTemplate(@PathVariable(value = "contractId") String contractId,
                                                  @PathVariable(value = "status") ContractState status,
                                                  @RequestHeader(name = "Active-Role") OrganizationRoleGrantedAuthority activeRole,
                                                  @RequestHeader(name = "Authorization") String authToken,
                                                  Principal principal) throws IOException {
        JwtAuthenticationToken authenticationToken = (JwtAuthenticationToken) principal;
        Jwt jwt = (Jwt) authenticationToken.getCredentials();
        String userId = (String) jwt.getClaims().get("sub");
        String userName = (String) jwt.getClaims().get("name");

        return contractStorageService.transitionContractTemplateState(contractId, status,
                activeRole.getOrganizationId(), userId, userName, authToken);
    }


    /**
     * Given an attachment PDF file, save it in the bucket and provide a reference in the database
     *
     * @param contractId id of contract template to add an attachment to
     * @param files multipart attachment files
     * @param authToken  active OAuth2 token of this user
     * @return updated contract with new attachment
     */
    @PatchMapping(value = "/contract/{contractId}/attachment")
    @PreAuthorize("@contractAuthorityChecker.isContractProvider(authentication, #contractId)")
    public ContractDto addContractAttachment(@PathVariable(value = "contractId") String contractId,
                                             @RequestPart("file") MultipartFile[] files,
                                             @RequestHeader(name = "Authorization") String authToken) {
        if (files.length != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many files specified");
        }
        try(PDDocument ignored = Loader.loadPDF(files[0].getBytes())) {
            return contractStorageService.addContractAttachment(contractId, files[0].getBytes(),
                    files[0].getOriginalFilename(), authToken);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid contract attachment file.");
        }
    }

    /**
     * Given a contract and attachment PDF file name, delete it from the bucket and database.
     *
     * @param contractId id of contract template to add an attachment to
     * @param attachmentName name of attachment to delete
     * @param authToken  active OAuth2 token of this user
     * @return updated contract with deleted attachment
     */
    @DeleteMapping(value = "/contract/{contractId}/attachment/{attachmentName}")
    @PreAuthorize("@contractAuthorityChecker.isContractProvider(authentication, #contractId)")
    public ContractDto deleteContractAttachment(@PathVariable(value = "contractId") String contractId,
                                             @PathVariable(value = "attachmentName") String attachmentName,
                                             @RequestHeader(name = "Authorization") String authToken) {
        return contractStorageService.deleteContractAttachment(contractId, attachmentName, authToken);
    }

    /**
     * Given a contract and attachment PDF file name, provide the attachment as download
     *
     * @param contractId id of contract template to add an attachment to
     * @param attachmentName name of attachment to delete
     * @return attachment file
     */
    @GetMapping(value = "/contract/{contractId}/attachment/{attachmentName}")
    @PreAuthorize("@contractAuthorityChecker.canAccessContract(authentication, #contractId)")
    public ResponseEntity<Resource> getContractAttachment(@PathVariable(value = "contractId") String contractId,
                                                          @PathVariable(value = "attachmentName") String attachmentName) {
        byte[] attachment;
        try {
            attachment = contractStorageService.getContractAttachment(contractId, attachmentName);
        } catch (IOException | StorageClientException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load attachment.");
        }

        ByteArrayResource resource = new ByteArrayResource(attachment);
        HttpHeaders headers = new HttpHeaders(); headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + attachmentName);
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(attachment.length)
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
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
    @PreAuthorize("@authorityChecker.representsOrganization(authentication, #orgaId)")
    public Page<ContractBasicDto> getOrganizationContracts(@RequestParam(value = "page", defaultValue = "0") int page,
                                                           @RequestParam(value = "size", defaultValue = "9") @Max(15) int size,
                                                           @RequestParam(value = "status", required = false) ContractState status,
                                                           @PathVariable(value = "orgaId") String orgaId,
                                                           @RequestHeader(name = "Authorization") String authToken) {
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
    @PreAuthorize("@contractAuthorityChecker.canAccessContract(authentication, #contractId)")
    public ContractDto getContractDetails(@PathVariable(value = "contractId") String contractId,
                                          @RequestHeader(name = "Authorization") String authToken) {
        return contractStorageService.getContractDetails(contractId, authToken);
    }

    /**
     * Given a contract, provide the contract pdf as download
     *
     * @param contractId id of contract template to add an attachment to
     * @return contractPdf file
     */
    @GetMapping(value = "/contract/{contractId}/contractPdf")
    @PreAuthorize("@contractAuthorityChecker.canAccessContract(authentication, #contractId)")
    public ResponseEntity<Resource> getContractPdf(@PathVariable(value = "contractId") String contractId) {
        byte[] contractPdf;
        try {
            contractPdf = contractStorageService.getContractPdf(contractId);
        } catch (IOException | StorageClientException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load contract pdf.");
        }

        ByteArrayResource resource = new ByteArrayResource(contractPdf);
        HttpHeaders headers = new HttpHeaders(); headers.add(HttpHeaders.CONTENT_DISPOSITION,
            "contractPdf; filename=Vertrag_" + contractId.replace("Contract:", "") + ".pdf");
        return ResponseEntity.ok()
            .headers(headers)
            .contentLength(contractPdf.length)
            .contentType(MediaType.APPLICATION_PDF)
            .body(resource);
    }

}
