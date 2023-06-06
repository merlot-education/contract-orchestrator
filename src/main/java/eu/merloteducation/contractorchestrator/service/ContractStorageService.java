package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
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
import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.*;

@Service
public class ContractStorageService {

    private static final String INVALID_FIELD_DATA = "Fields contain invalid data.";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ContractTemplateRepository contractTemplateRepository;

    @Value("${serviceoffering-orchestrator.base-uri}")
    private String serviceOfferingOrchestratorBaseUri;

    @Value("${organizations-orchestrator.base-uri}")
    private String organizationsOrchestratorBaseUri;

    private JSONObject requestServiceOfferingDetails(String authToken, String offeringId) throws JSONException {
        // request details about service offering
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authToken);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        String serviceOfferingResponse = restTemplate.exchange(
                serviceOfferingOrchestratorBaseUri + "serviceoffering/" + offeringId,
                HttpMethod.GET, request, String.class).getBody();

        return new JSONObject(serviceOfferingResponse); // TODO replace this with actual model once common library is created
    }

    private JSONObject requestOrganizationDetails(String orgaId) throws JSONException {
        String organizationResponse = restTemplate.exchange(
                organizationsOrchestratorBaseUri + "organization/" + orgaId,
                HttpMethod.GET, null, String.class).getBody();
        return new JSONObject(organizationResponse); // TODO replace this with actual model once common library is created
    }

    private boolean immutableFieldsValid(ContractTemplate originalContract, ContractTemplate editedContract,
                                         boolean isConsumer, boolean isProvider) {
        // make sure the basic fields have not changed
        if (!originalContract.getProviderId().equals(editedContract.getProviderId())
                || !originalContract.getConsumerId().equals(editedContract.getConsumerId())
                || !originalContract.getState().equals(editedContract.getState())
                || !originalContract.getCreationDate().equals(editedContract.getCreationDate())
                || !originalContract.getOfferingName().equals(editedContract.getOfferingName())
                || !originalContract.getOfferingId().equals(editedContract.getOfferingId())
                || !originalContract.getProviderTncUrl().equals(editedContract.getProviderTncUrl())) {
            return false;
        }

        // depending on the role some other fields may not be changed either
        if (isConsumer) {
            if (originalContract.isProviderMerlotTncAccepted() != editedContract.isProviderMerlotTncAccepted()
                    || !originalContract.getAdditionalAgreements().equals(editedContract.getAdditionalAgreements())
                    || !originalContract.getOfferingAttachments().equals(editedContract.getOfferingAttachments())) {
                return false;
            }
        } else if (isProvider) {
            if (originalContract.isConsumerMerlotTncAccepted() != editedContract.isConsumerMerlotTncAccepted()
                    || originalContract.isConsumerOfferingTncAccepted() != editedContract.isConsumerOfferingTncAccepted()
                    || originalContract.isConsumerProviderTncAccepted() != editedContract.isConsumerProviderTncAccepted()) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidFieldSelections(ContractTemplate contract, String authToken) throws Exception {
        JSONObject serviceOfferingJson = requestServiceOfferingDetails(authToken, contract.getOfferingId());

        // make sure selections are valid
        if (contract.getRuntimeSelection() != null
                && !isValidRuntimeSelection(
                contract.getRuntimeSelection(), serviceOfferingJson.getJSONArray("runtimeOption"))) {
            return false;
        }

        if (contract.getUserCountSelection() != null && !isValidUserCountSelection(
                contract.getUserCountSelection(), serviceOfferingJson.getJSONArray("userCountOption"))) {
            return false;
        }

        if (contract.getExchangeCountSelection() != null && isValidExchangeCountSelection(
                contract.getExchangeCountSelection(), serviceOfferingJson.getJSONArray("exchangeCountOption"))) {
            return false;
        }

        return true;
    }

    private boolean isValidRuntimeSelection(String selection, JSONArray options) throws Exception {
        boolean foundMatch = false;
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            if ((option.getBoolean("runtimeUnlimited") && selection.equals("Unbegrenzt"))
                    || selection.equals(option.getInt("runtimeCount")
                    + " " + option.getString("runtimeMeasurement"))) {
                foundMatch = true;
                break;
            }
        }
        return foundMatch;
    }

    private boolean isValidUserCountSelection(String selection, JSONArray options) throws Exception {
        boolean foundMatch = false;
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            if ((option.getBoolean("userCountUnlimited") && selection.equals("Unbegrenzt"))
                    || selection.equals("Bis zu " + option.getInt("userCountUpTo"))) {
                foundMatch = true;
                break;
            }
        }
        return foundMatch;
    }

    private boolean isValidExchangeCountSelection(String selection, JSONArray options) throws Exception {
        boolean foundMatch = false;
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            if ((option.getBoolean("exchangeCountUnlimited") && selection.equals("Unbegrenzt"))
                    || selection.equals("Bis zu " + option.getInt("exchangeCountUpTo"))) {
                foundMatch = true;
                break;
            }
        }
        return foundMatch;
    }

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
        JSONObject serviceOfferingJson = requestServiceOfferingDetails(authToken,
                contractCreateRequest.getOfferingId());
        contract.setOfferingName(serviceOfferingJson.getString("name"));
        contract.setProviderId(serviceOfferingJson.getString("offeredBy"));
        JSONArray jsonAttachments = serviceOfferingJson.getJSONArray("attachments");
        List<String> attachments = new ArrayList<>();
        for (int i = 0; i < jsonAttachments.length(); i++) {
            attachments.add(jsonAttachments.get(0).toString());
        }
        contract.setOfferingAttachments(attachments);

        // check if consumer and provider are equal, and if so abort
        if (contract.getProviderId().equals(contract.getConsumerId())) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Provider and consumer must not be equal.");
        }

        JSONObject organizationJson = requestOrganizationDetails(
                contract.getProviderId().replace("Participant:", ""));
        contract.setProviderTncUrl(organizationJson.getString("termsAndConditionsLink"));

        contractTemplateRepository.save(contract);

        return contract;
    }

    /**
     * Given an edited ContractTemplate, this function verifies the updated fields and writes them to the database if allowed.
     *
     * @param editedContract     contract template with edited fields
     * @param authToken          the OAuth2 Token from the user requesting this action
     * @param representedOrgaIds list of organization ids the user represents
     * @return updated contract template from databse
     */
    public ContractTemplate updateContractTemplate(ContractTemplate editedContract,
                                                   String authToken,
                                                   Set<String> representedOrgaIds) throws Exception {

        ContractTemplate contract = contractTemplateRepository.findById(editedContract.getId()).orElse(null);

        boolean isConsumer = representedOrgaIds.contains(editedContract.getConsumerId());
        boolean isProvider = representedOrgaIds.contains(editedContract.getProviderId());

        if (contract == null) {
            throw new ResponseStatusException(NOT_FOUND, "Could not find a contract with this id.");
        }

        // user must be either consumer or provider of contract
        if (!(isConsumer || isProvider)) {
            throw new ResponseStatusException(FORBIDDEN, "Not allowed to edit this contract.");
        }

        // state must be either IN_DRAFT or user must be provider if state is SIGNED_CONSUMER
        if (!(contract.getState() == ContractState.IN_DRAFT
                || (contract.getState() == ContractState.SIGNED_CONSUMER && isProvider))) {
            throw new ResponseStatusException(FORBIDDEN, "Not allowed to edit this contract.");
        }

        // ensure that immutable fields (depending on role) were not modified
        if (!immutableFieldsValid(contract, editedContract, isConsumer, isProvider)) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, INVALID_FIELD_DATA);
        }

        // ensure that the selections that were made are valid
        if (!isValidFieldSelections(editedContract, authToken)) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, INVALID_FIELD_DATA);
        }

        // at this point we have a valid requested update, save it in the db
        return contractTemplateRepository.save(editedContract);
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
