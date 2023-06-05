package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.models.entities.Contract;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.repositories.ContractRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ContractStorageService {

    @Autowired
    private ContractRepository contractRepository;

    /**
     * Creates a new contract in the database based on the fields in the contractCreateRequest.
     * This is called immediately when a user clicks on the "Buchen" button in the frontend, hence no fields
     * are selected yet.
     *
     * @param contractCreateRequest basic information about the contract to instantiate
     * @param authToken             the OAuth2 Token from the user requesting this action
     * @return Instantiated contract object from the database
     */
    @Transactional
    public Contract addContractTemplate(ContractCreateRequest contractCreateRequest, String authToken) {

        // initialize contract fields
        Contract contract = new Contract();
        contract.setId("Contract:" + UUID.randomUUID());
        contract.setCreationDate(OffsetDateTime.now());

        // extract data from request
        contract.setOfferingId(contractCreateRequest.getOfferingId());
        contract.setConsumerId(contractCreateRequest.getConsumerId());

        // TODO request details of provider of service offering from organizations orchestrator, use token provided by user accessing this function
        contract.setProviderTncUrl("TODO");
        contract.setProviderTncHash("TODO");

        // TODO request details of service offering from service offering orchestrator, use token provided by user accessing this function
        // TODO associate a contract to the specified service offering to disable further edits
        contract.setOfferingName("TODO");
        contract.setProviderId("TODO");
        contract.setOfferingAttachments(new ArrayList<>());

        contractRepository.save(contract);

        return contract;
    }

    /**
     * Returns all contracts from the database where the specified organization is either the consumer or provider.
     *
     * @param orgaId   id of the organization requesting this data
     * @param pageable page request
     * @return Page of contracts that are related to this organization
     */
    public Page<Contract> getOrganizationContracts(String orgaId, Pageable pageable) {
        return contractRepository.findAllByProviderIdOrConsumerId(orgaId, orgaId, pageable);
    }

    /**
     * For a given id, return the corresponding contract database entry.
     *
     * @param contractId         id of the contract
     * @param representedOrgaIds ids of orgas that the user requesting this action represents
     * @return contract object from the database
     */
    public Contract getContractDetails(String contractId, Set<String> representedOrgaIds) {
        Contract contract = contractRepository.findById(contractId).orElse(null);

        if (contract == null) {
            throw new ResponseStatusException(NOT_FOUND, "Could not find a contract with this id.");
        }

        if (!(representedOrgaIds.contains(contract.getConsumerId())
                || representedOrgaIds.contains(contract.getProviderId()))) {
            throw new ResponseStatusException(FORBIDDEN, "Not allowed to request details of this contract.");
        }

        return contract;
    }
}
