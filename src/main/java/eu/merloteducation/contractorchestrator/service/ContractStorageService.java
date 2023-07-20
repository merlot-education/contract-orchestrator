package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.models.entities.*;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.models.messagequeue.ContractTemplateUpdated;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;
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
    private MessageQueueService messageQueueService;

    @Autowired
    private ContractSignerService contractSignerService;

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
                serviceOfferingOrchestratorBaseUri + "/serviceoffering/" + offeringId,
                HttpMethod.GET, request, String.class).getBody();

        return new JSONObject(serviceOfferingResponse); // TODO replace this with actual model once common library is created
    }

    private JSONObject requestOrganizationDetails(String orgaId, String authToken) throws JSONException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authToken);
        HttpEntity<String> request = new HttpEntity<>(null, headers);

        String organizationResponse = restTemplate.exchange(
                organizationsOrchestratorBaseUri + "/organization/" + orgaId,
                HttpMethod.GET, request, String.class).getBody();
        return new JSONObject(organizationResponse); // TODO replace this with actual model once common library is created
    }

    private boolean isValidFieldSelections(ContractTemplate contract, String authToken) throws JSONException {
        JSONObject serviceOfferingJson = requestServiceOfferingDetails(authToken, contract.getOfferingId());

        // make sure selections are valid
        if (contract.getRuntimeSelection() != null
                && !isValidRuntimeSelection(
                contract.getRuntimeSelection(), serviceOfferingJson.getJSONArray("runtimeOption"))) {
            return false;
        }

        if (contract instanceof SaasContractTemplate saasContract
                && saasContract.getUserCountSelection() != null
                && !isValidUserCountSelection(saasContract.getUserCountSelection(),
                serviceOfferingJson.getJSONArray("userCountOption"))) {
            return false;
        }

        if (contract instanceof DataDeliveryContractTemplate dataDeliveryContract
                && dataDeliveryContract.getExchangeCountSelection() != null
                && !isValidExchangeCountSelection(dataDeliveryContract.getExchangeCountSelection(),
                serviceOfferingJson.getJSONArray("exchangeCountOption"))) {
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

    private void updateSaasContract(SaasContractTemplate targetContract,
                                    SaasContractTemplate editedContract) {
        if (targetContract.getState() == ContractState.IN_DRAFT) {
            targetContract.setUserCountSelection(editedContract.getUserCountSelection());
        }
    }

    private void updateDataDeliveryContract(DataDeliveryContractTemplate targetContract,
                                            DataDeliveryContractTemplate editedContract,
                                            boolean isConsumer,
                                            boolean isProvider) {
        DataDeliveryProvisioning targetProvisioning =
                (DataDeliveryProvisioning) targetContract.getServiceContractProvisioning();
        DataDeliveryProvisioning editedProvisioning =
                (DataDeliveryProvisioning) editedContract.getServiceContractProvisioning();

        if (targetContract.getState() == ContractState.IN_DRAFT) {
            targetContract.setExchangeCountSelection(
                    editedContract.getExchangeCountSelection());
            if (isConsumer) {
                targetProvisioning.setDataAddressTargetBucketName(
                        editedProvisioning.getDataAddressTargetBucketName());
                targetProvisioning.setDataAddressTargetFileName(
                        editedProvisioning.getDataAddressTargetFileName());
                targetProvisioning.setSelectedConsumerConnectorId(
                        editedProvisioning.getSelectedConsumerConnectorId());
            }
            if (isProvider) {
                targetProvisioning.setDataAddressName(
                        editedProvisioning.getDataAddressName());
                targetProvisioning.setDataAddressType(
                        editedProvisioning.getDataAddressType());
                targetProvisioning.setDataAddressSourceBucketName(
                        editedProvisioning.getDataAddressSourceBucketName());
                targetProvisioning.setDataAddressSourceFileName(
                        editedProvisioning.getDataAddressSourceFileName());
                targetProvisioning.setSelectedProviderConnectorId(
                        editedProvisioning.getSelectedProviderConnectorId());
            }
        } else if (targetContract.getState() == ContractState.SIGNED_CONSUMER && isProvider) {
            targetProvisioning.setDataAddressName(
                    editedProvisioning.getDataAddressName());
            targetProvisioning.setDataAddressType(
                    editedProvisioning.getDataAddressType());
            targetProvisioning.setDataAddressSourceBucketName(
                    editedProvisioning.getDataAddressSourceBucketName());
            targetProvisioning.setDataAddressSourceFileName(
                    editedProvisioning.getDataAddressSourceFileName());
            targetProvisioning.setSelectedProviderConnectorId(
                    editedProvisioning.getSelectedProviderConnectorId());
        }
    }

    private void updateContractDependingOnRole(ContractTemplate targetContract,
                                               ContractTemplate editedContract,
                                               boolean isConsumer,
                                               boolean isProvider) {
        // TODO consider moving this logic into a DTO pattern
        if (targetContract.getState() == ContractState.IN_DRAFT) {
            targetContract.setRuntimeSelection(editedContract.getRuntimeSelection());
            if (isConsumer) {
                targetContract.setConsumerMerlotTncAccepted(editedContract.isConsumerMerlotTncAccepted());
                targetContract.setConsumerProviderTncAccepted(editedContract.isConsumerProviderTncAccepted());
                targetContract.setConsumerOfferingTncAccepted(editedContract.isConsumerOfferingTncAccepted());
            }
            if (isProvider) {
                targetContract.setProviderMerlotTncAccepted(editedContract.isProviderMerlotTncAccepted());
                targetContract.setAdditionalAgreements(editedContract.getAdditionalAgreements());
                targetContract.setOfferingAttachments(editedContract.getOfferingAttachments());
            }
        } else if (targetContract.getState() == ContractState.SIGNED_CONSUMER && isProvider) {
            targetContract.setProviderMerlotTncAccepted(editedContract.isProviderMerlotTncAccepted());

        }

        if (targetContract instanceof SaasContractTemplate targetSaasContractTemplate &&
                editedContract instanceof SaasContractTemplate editedSaasContractTemplate) {
            updateSaasContract(targetSaasContractTemplate, editedSaasContractTemplate);
        }

        if (targetContract instanceof DataDeliveryContractTemplate targetDataDeliveryContractTemplate &&
                editedContract instanceof DataDeliveryContractTemplate editedDataDeliveryContractTemplate) {
            updateDataDeliveryContract(targetDataDeliveryContractTemplate, editedDataDeliveryContractTemplate,
                    isConsumer, isProvider);
        }
    }

    private OffsetDateTime computeValidityTimestamp(String runtimeSelection) {
        String[] runtimeParts = runtimeSelection.split(" ");
        long numPart = Long.parseLong(runtimeParts[0]);
        TemporalAmount temporalAmount = switch (runtimeParts[1]) {
            case "hour(s)" -> Duration.ofHours(numPart);
            case "day(s)" -> Duration.ofDays(numPart);
            case "week(s)" -> Duration.ofDays(numPart * 7);
            case "month(s)" -> Duration.ofDays(numPart * 30);
            case "year(s)" -> Duration.ofDays(numPart * 365);
            default -> throw new IllegalArgumentException("Unknown metric: " + runtimeParts[1]);
        };
        return OffsetDateTime.now().plus(temporalAmount);
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

        JSONObject serviceOfferingJson = requestServiceOfferingDetails(authToken,
                contractCreateRequest.getOfferingId());
        if (!serviceOfferingJson.getString("merlotState").equals("RELEASED")) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Referenced service offering is not valid");
        }

        // initialize contract fields, id and creation date
        ContractTemplate contract;

        if (serviceOfferingJson.getString("type").equals("merlot:MerlotServiceOfferingSaaS")) {
            contract = new SaasContractTemplate();
        } else if (serviceOfferingJson.getString("type").equals("merlot:MerlotServiceOfferingDataDelivery")) {
            contract = new DataDeliveryContractTemplate();
            // also store a copy of the data transfer type to later decide who can initiate a transfer
            ((DataDeliveryContractTemplate) contract).setDataTransferType(serviceOfferingJson.getString("dataTransferType"));
        } else {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Unknown Service Offering Type.");
        }

        // extract data from request
        contract.setOfferingId(contractCreateRequest.getOfferingId());
        contract.setConsumerId(contractCreateRequest.getConsumerId());

        contract.setOfferingName(serviceOfferingJson.getString("name"));
        contract.setProviderId(serviceOfferingJson.getString("offeredBy"));
        if (!serviceOfferingJson.isNull("attachments")) {
            List<String> attachments = new ArrayList<>();
            JSONArray jsonAttachments = serviceOfferingJson.getJSONArray("attachments");
            for (int i = 0; i < jsonAttachments.length(); i++) {
                attachments.add(jsonAttachments.get(0).toString());
            }
            contract.setOfferingAttachments(attachments);
        }

        // check if consumer and provider are equal, and if so abort
        if (contract.getProviderId().equals(contract.getConsumerId())) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Provider and consumer must not be equal.");
        }

        JSONObject organizationJson = requestOrganizationDetails(
                contract.getProviderId().replace(ORGA_PREFIX, ""), authToken);
        contract.setProviderTncUrl(organizationJson.getString("termsAndConditionsLink"));

        contract = contractTemplateRepository.save(contract);

        messageQueueService.sendContractCreatedMessage(new ContractTemplateUpdated(contract));

        return contract;
    }

    /**
     * For a given contract id this attempts to find the corresponding contract, check access and create a new contract
     * with a copy of all editable fields. A new ID is generated as well as the signatures are reset.
     *
     * @param contractId         id of the contract to copy
     * @param representedOrgaIds list of organization ids the user represents
     * @return newly generated contract
     */
    public ContractTemplate regenerateContract(String contractId, Set<String> representedOrgaIds) {
        ContractTemplate contract = contractTemplateRepository.findById(contractId).orElse(null);

        if (contract == null) {
            throw new ResponseStatusException(NOT_FOUND, CONTRACT_NOT_FOUND);
        }

        // user must be either consumer or provider of contract
        if (!(representedOrgaIds.contains(contract.getConsumerId().replace(ORGA_PREFIX, ""))
                || representedOrgaIds.contains(contract.getProviderId().replace(ORGA_PREFIX, "")))) {
            throw new ResponseStatusException(FORBIDDEN, CONTRACT_EDIT_FORBIDDEN);
        }

        if (!(contract.getState() == ContractState.DELETED || contract.getState() == ContractState.ARCHIVED)) {
            throw new ResponseStatusException(FORBIDDEN, CONTRACT_EDIT_FORBIDDEN);
        }

        if (contract instanceof DataDeliveryContractTemplate dataDeliveryContract) {
            contract = new DataDeliveryContractTemplate(dataDeliveryContract, true);
            contract.setServiceContractProvisioning(
                    new DataDeliveryProvisioning((DataDeliveryProvisioning) contract.getServiceContractProvisioning()));
        } else if (contract instanceof SaasContractTemplate saasContractTemplate) {
            contract = new SaasContractTemplate(saasContractTemplate, true);
            contract.setServiceContractProvisioning(
                    new DefaultProvisioning((DefaultProvisioning) contract.getServiceContractProvisioning()));
        } else {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Unknown contract type.");
        }
        contractTemplateRepository.save(contract);
        return contract;
    }

    /**
     * Given an edited ContractTemplate, this function verifies the updated fields and writes them to the database if allowed.
     *
     * @param editedContract     contract template with edited fields
     * @param authToken          the OAuth2 Token from the user requesting this action
     * @param activeRoleOrgaId   the currently selected role of the user
     * @param representedOrgaIds list of organization ids the user represents
     * @return updated contract template from database
     */
    public ContractTemplate updateContractTemplate(ContractTemplate editedContract,
                                                   String authToken,
                                                   String activeRoleOrgaId,
                                                   Set<String> representedOrgaIds) throws JSONException {

        ContractTemplate contract = contractTemplateRepository.findById(editedContract.getId()).orElse(null);

        if (contract == null) {
            throw new ResponseStatusException(NOT_FOUND, CONTRACT_NOT_FOUND);
        }

        boolean isConsumer = activeRoleOrgaId.equals(contract.getConsumerId().replace(ORGA_PREFIX, ""));
        boolean isProvider = activeRoleOrgaId.equals(contract.getProviderId().replace(ORGA_PREFIX, ""));

        // user must be either consumer or provider of contract
        if (!(isConsumer || isProvider)) {
            throw new ResponseStatusException(FORBIDDEN, CONTRACT_EDIT_FORBIDDEN);
        }

        // state must be IN_DRAFT (or SIGNED_CONSUMER with restricted options)
        if (contract.getState() != ContractState.IN_DRAFT && contract.getState() != ContractState.SIGNED_CONSUMER) {
            throw new ResponseStatusException(FORBIDDEN, CONTRACT_EDIT_FORBIDDEN);
        }

        // update the fields that we are allowed to edit in this role, disregard everything else
        updateContractDependingOnRole(contract, editedContract, isConsumer, isProvider);

        // ensure that the selections that were made are valid
        if (!isValidFieldSelections(contract, authToken)) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, INVALID_FIELD_DATA);
        }

        // at this point we have a valid requested update, save it in the db
        return contractTemplateRepository.save(contract);
    }

    /**
     * Transition the contract template attached to the given id to the target state if allowed.
     *
     * @param contractId       id of the contract template to transition
     * @param targetState      target state of the contract template
     * @param activeRoleOrgaId the currently selected role of the user
     * @param userId           the id of the user that requested this action
     * @return updated contract template from database
     */
    public ContractTemplate transitionContractTemplateState(String contractId,
                                                            ContractState targetState,
                                                            String activeRoleOrgaId,
                                                            String userId) {
        ContractTemplate contract = contractTemplateRepository.findById(contractId).orElse(null);

        if (contract == null) {
            throw new ResponseStatusException(NOT_FOUND, CONTRACT_NOT_FOUND);
        }

        boolean isConsumer = activeRoleOrgaId.equals(contract.getConsumerId().replace(ORGA_PREFIX, ""));
        boolean isProvider = activeRoleOrgaId.equals(contract.getProviderId().replace(ORGA_PREFIX, ""));

        // user must be either consumer or provider of contract
        if (!(isConsumer || isProvider)) {
            throw new ResponseStatusException(FORBIDDEN, CONTRACT_EDIT_FORBIDDEN);
        }

        if (contract.getState() == ContractState.DELETED && targetState == ContractState.PURGED && isProvider) {
            contractTemplateRepository.delete(contract);
            messageQueueService.sendContractPurgedMessage(new ContractTemplateUpdated(contract));
            return contract;
        }

        if (targetState == ContractState.SIGNED_CONSUMER) {
            if (!isConsumer) {
                throw new ResponseStatusException(FORBIDDEN, INVALID_STATE_TRANSITION);
            }
            contract.setConsumerSignerUserId(userId);
            contract.setConsumerSignature(contractSignerService.generateContractSignature(contract, userId));
        }

        if (targetState == ContractState.RELEASED) {
            if (!isProvider) {
                throw new ResponseStatusException(FORBIDDEN, INVALID_STATE_TRANSITION);
            }
            contract.setProviderSignerUserId(userId);
            contract.setProviderSignature(contractSignerService.generateContractSignature(contract, userId));
        }

        try {
            contract.transitionState(targetState);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(FORBIDDEN, e.getMessage());
        }

        if (contract.getState() == ContractState.RELEASED && !contract.getRuntimeSelection().equals(SELECTION_INFINITE)) {
            contract.getServiceContractProvisioning().setValidUntil(
                    this.computeValidityTimestamp(contract.getRuntimeSelection()));
        }

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
        System.out.println(contractId);
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
