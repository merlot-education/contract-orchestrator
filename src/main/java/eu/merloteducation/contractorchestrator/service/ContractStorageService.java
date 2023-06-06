package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Set;

import static org.springframework.http.HttpStatus.*;

@Service
public class ContractStorageService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ContractTemplateRepository contractTemplateRepository;

    @Value("${serviceoffering-orchestrator.base-uri}")
    private String serviceOfferingOrchestratorBaseUri;

    @Value("${organizations-orchestrator.base-uri}")
    private String organizationsOrchestratorBaseUri;

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
    public ContractTemplate addContractTemplate(ContractCreateRequest contractCreateRequest, String authToken) throws Exception {

        // check that fields are in a valid format
        if (!contractCreateRequest.getOfferingId().startsWith("ServiceOffering:") ||
                !contractCreateRequest.getConsumerId().matches("Participant:\\d+")) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Creation request contains invalid fields.");
        }

        // initialize contract fields, id and creation date
        ContractTemplate contract = new ContractTemplate();

        // extract data from request
        contract.setOfferingId(contractCreateRequest.getOfferingId());
        contract.setConsumerId(contractCreateRequest.getConsumerId());
        // TODO associate a contract to the specified service offering to disable further edits
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authToken);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        String serviceOfferingResponse = restTemplate.exchange(
                serviceOfferingOrchestratorBaseUri + "serviceoffering/" + contractCreateRequest.getOfferingId(),
                HttpMethod.GET, request, String.class).getBody();

        JSONObject serviceOfferingJson = new JSONObject(serviceOfferingResponse); // TODO replace this with actual model once common library is created
        contract.setOfferingName(serviceOfferingJson.getString("name"));
        contract.setProviderId(serviceOfferingJson.getString("offeredBy"));
        contract.setOfferingAttachments(new ArrayList<>()); // TODO fetch this from response

        // check if consumer and provider are equal, and if so abort
        if (contract.getProviderId().equals(contract.getConsumerId())) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Provider and consumer must not be equal.");
        }


        String organizationResponse = restTemplate.exchange(
                organizationsOrchestratorBaseUri + "organization/" + contract.getProviderId().replace("Participant:", ""),
                HttpMethod.GET, null, String.class).getBody();
        JSONObject organizationJson = new JSONObject(organizationResponse); // TODO replace this with actual model once common library is created
        contract.setProviderTncUrl(organizationJson.getString("termsAndConditionsLink"));

        contractTemplateRepository.save(contract);

        return contract;
    }

    /**
     * Returns all contracts from the database where the specified organization is either the consumer or provider.
     *
     * @param orgaId   id of the organization requesting this data
     * @param pageable page request
     * @return Page of contracts that are related to this organization
     */
    public Page<ContractTemplate> getOrganizationContracts(String orgaId, Pageable pageable) {
        if (!orgaId.matches("Participant:\\d+")) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Organization ID has invalid format.");
        }
        return contractTemplateRepository.findAllByProviderIdOrConsumerId(orgaId, orgaId, pageable);
    }

    /**
     * For a given id, return the corresponding contract database entry.
     *
     * @param contractId         id of the contract
     * @param representedOrgaIds ids of orgas that the user requesting this action represents
     * @return contract object from the database
     */
    public ContractTemplate getContractDetails(String contractId, Set<String> representedOrgaIds) {
        ContractTemplate contract = contractTemplateRepository.findById(contractId).orElse(null);

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
