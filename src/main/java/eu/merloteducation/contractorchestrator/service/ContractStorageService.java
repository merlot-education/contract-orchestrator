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
import java.util.Objects;
import java.util.Set;

import static org.springframework.http.HttpStatus.*;

@Service
public class ContractStorageService {

    private static final String INVALID_FIELD_DATA = "Fields contain invalid data.";
    private static final String INVALID_STATE_TRANSITION = "Requested transition is not allowed.";
    private static final String CONTRACT_NOT_FOUND = "Could not find a contract with this id.";
    private static final String CONTRACT_EDIT_FORBIDDEN = "Not allowed to edit this contract.";
    private static final String CONTRACT_VIEW_FORBIDDEN = "Not allowed to view this contract.";
    private static final String SELECTION_INFINITE = "unlimited";

    private static final String ORGA_PREFIX = "Participant:";

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
                || !originalContract.getCreationDate().isEqual(editedContract.getCreationDate())
                || !originalContract.getOfferingName().equals(editedContract.getOfferingName())
                || !originalContract.getOfferingId().equals(editedContract.getOfferingId())
                || !originalContract.getProviderTncUrl().equals(editedContract.getProviderTncUrl())) {
            return false;
        }

        // depending on the role some other fields may not be changed either
        if (isConsumer && !isProvider) {
            if (originalContract.isProviderMerlotTncAccepted() != editedContract.isProviderMerlotTncAccepted()
                    || !Objects.equals(originalContract.getAdditionalAgreements(), editedContract.getAdditionalAgreements())
                    || !Objects.equals(originalContract.getOfferingAttachments(), editedContract.getOfferingAttachments())) {
                return false;
            }
        } else if (isProvider && !isConsumer) {
            if (originalContract.isConsumerMerlotTncAccepted() != editedContract.isConsumerMerlotTncAccepted()
                    || originalContract.isConsumerOfferingTncAccepted() != editedContract.isConsumerOfferingTncAccepted()
                    || originalContract.isConsumerProviderTncAccepted() != editedContract.isConsumerProviderTncAccepted()) {
                return false;
            }
        }
        // otherwise user both has roles for provider and consumer, hence accept the changes
        return true;
    }

    private boolean isValidFieldSelections(ContractTemplate contract, String authToken) throws JSONException {
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

    private boolean isValidRuntimeSelection(String selection, JSONArray options) throws JSONException {
        boolean foundMatch = false;
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            if ((option.getBoolean("runtimeUnlimited") && selection.equals(SELECTION_INFINITE))
                    || selection.equals(option.getInt("runtimeCount")
                    + " " + option.getString("runtimeMeasurement"))) {
                foundMatch = true;
                break;
            }
        }
        return foundMatch;
    }

    private boolean isValidUserCountSelection(String selection, JSONArray options) throws JSONException {
        boolean foundMatch = false;
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            if ((option.getBoolean("userCountUnlimited") && selection.equals(SELECTION_INFINITE))
                    || selection.equals(String.valueOf(option.getInt("userCountUpTo")))) {
                foundMatch = true;
                break;
            }
        }
        return foundMatch;
    }

    private boolean isValidExchangeCountSelection(String selection, JSONArray options) throws JSONException {
        boolean foundMatch = false;
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            if ((option.getBoolean("exchangeCountUnlimited") && selection.equals(SELECTION_INFINITE))
                    || selection.equals(String.valueOf(option.getInt("exchangeCountUpTo")))) {
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
    public ContractTemplate addContractTemplate(ContractCreateRequest contractCreateRequest, String authToken)
            throws JSONException {

        // check that fields are in a valid format
        if (!contractCreateRequest.getOfferingId().startsWith("ServiceOffering:") ||
                !contractCreateRequest.getConsumerId().matches(ORGA_PREFIX + "\\d+")) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, INVALID_FIELD_DATA);
        }

        // initialize contract fields, id and creation date
        ContractTemplate contract = new ContractTemplate();

        // extract data from request
        contract.setOfferingId(contractCreateRequest.getOfferingId());
        contract.setConsumerId(contractCreateRequest.getConsumerId());

        // TODO associate a contract to the specified service offering to disable further edits
        // TODO this will need a message bus to avoid race conditions
        JSONObject serviceOfferingJson = requestServiceOfferingDetails(authToken,
                contractCreateRequest.getOfferingId());
        if (!serviceOfferingJson.getString("merlotState").equals("RELEASED")) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Referenced service offering is not valid");
        }
        contract.setOfferingName(serviceOfferingJson.getString("name"));
        contract.setProviderId(serviceOfferingJson.getString("offeredBy"));
        if (!serviceOfferingJson.isNull("attachments")) {
            JSONArray jsonAttachments = serviceOfferingJson.getJSONArray("attachments");
            for (int i = 0; i < jsonAttachments.length(); i++) {
                contract.addAttachment(jsonAttachments.get(0).toString());
            }
        }

        // check if consumer and provider are equal, and if so abort
        if (contract.getProviderId().equals(contract.getConsumerId())) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Provider and consumer must not be equal.");
        }

        JSONObject organizationJson = requestOrganizationDetails(
                contract.getProviderId().replace(ORGA_PREFIX, ""));
        contract.setProviderTncUrl(organizationJson.getString("termsAndConditionsLink"));

        return contractTemplateRepository.save(contract);
    }

    /**
     * Given an edited ContractTemplate, this function verifies the updated fields and writes them to the database if allowed.
     *
     * @param editedContract     contract template with edited fields
     * @param authToken          the OAuth2 Token from the user requesting this action
     * @param representedOrgaIds list of organization ids the user represents
     * @return updated contract template from database
     */
    public ContractTemplate updateContractTemplate(ContractTemplate editedContract,
                                                   String authToken,
                                                   Set<String> representedOrgaIds) throws JSONException {

        ContractTemplate contract = contractTemplateRepository.findById(editedContract.getId()).orElse(null);

        if (contract == null) {
            throw new ResponseStatusException(NOT_FOUND, CONTRACT_NOT_FOUND);
        }

        boolean isConsumer = representedOrgaIds.contains(editedContract.getConsumerId().replace(ORGA_PREFIX, ""));
        boolean isProvider = representedOrgaIds.contains(editedContract.getProviderId().replace(ORGA_PREFIX, ""));

        // user must be either consumer or provider of contract
        if (!(isConsumer || isProvider)) {
            throw new ResponseStatusException(FORBIDDEN, CONTRACT_EDIT_FORBIDDEN);
        }

        // state must be either IN_DRAFT or user must be provider if state is SIGNED_CONSUMER
        if (contract.getState() != ContractState.IN_DRAFT) {
            throw new ResponseStatusException(FORBIDDEN, CONTRACT_EDIT_FORBIDDEN);
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
     * Transition the contract template attached to the given id to the target state if allowed.
     *
     * @param contractId         id of the contract template to transition
     * @param targetState        target state of the contract template
     * @param representedOrgaIds list of organization ids the user represents
     * @return updated contract template from database
     */
    public ContractTemplate transitionContractTemplateState(String contractId,
                                                            ContractState targetState,
                                                            Set<String> representedOrgaIds) {
        ContractTemplate contract = contractTemplateRepository.findById(contractId).orElse(null);

        if (contract == null) {
            throw new ResponseStatusException(NOT_FOUND, CONTRACT_NOT_FOUND);
        }

        boolean isConsumer = representedOrgaIds.contains(contract.getConsumerId().replace(ORGA_PREFIX, ""));
        boolean isProvider = representedOrgaIds.contains(contract.getProviderId().replace(ORGA_PREFIX, ""));

        // user must be either consumer or provider of contract
        if (!(isConsumer || isProvider)) {
            throw new ResponseStatusException(FORBIDDEN, CONTRACT_EDIT_FORBIDDEN);
        }

        if (targetState == ContractState.SIGNED_CONSUMER && !isConsumer) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, INVALID_STATE_TRANSITION);
        }

        if (targetState == ContractState.RELEASED && !isProvider) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, INVALID_STATE_TRANSITION);
        }

        try {
            contract.transitionState(targetState);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, INVALID_STATE_TRANSITION);
        }

        // TODO on RELEASED transfer data to EDC of provider and start negotiation

        return contractTemplateRepository.save(contract);
    }

    /**
     * Returns all contracts from the database where the specified organization is either the consumer or provider.
     *
     * @param orgaId   id of the organization requesting this data
     * @param pageable page request
     * @return Page of contracts that are related to this organization
     */
    public Page<ContractTemplate> getOrganizationContracts(String orgaId, Pageable pageable) {
        if (!orgaId.matches(ORGA_PREFIX + "\\d+")) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, INVALID_FIELD_DATA);
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
            throw new ResponseStatusException(NOT_FOUND, CONTRACT_NOT_FOUND);
        }

        if (!(representedOrgaIds.contains(contract.getConsumerId().replace(ORGA_PREFIX, ""))
                || representedOrgaIds.contains(contract.getProviderId().replace(ORGA_PREFIX, "")))) {
            throw new ResponseStatusException(FORBIDDEN, CONTRACT_VIEW_FORBIDDEN);
        }

        return contract;
    }
}
