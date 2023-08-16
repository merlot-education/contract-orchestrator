package eu.merloteducation.contractorchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.merloteducation.contractorchestrator.models.organisationsorchestrator.OrganizationDetails;
import eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator.*;
import eu.merloteducation.contractorchestrator.models.entities.*;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.models.mappers.ContractMapper;
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
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.*;

@Service
public class ContractStorageService {

    private static final String INVALID_FIELD_DATA = "Fields contain invalid data.";
    private static final String INVALID_STATE_TRANSITION = "Requested transition is not allowed.";
    private static final String CONTRACT_NOT_FOUND = "Could not find a contract with this id.";
    private static final String CONTRACT_EDIT_FORBIDDEN = "Not allowed to edit this contract.";
    private static final String CONTRACT_VIEW_FORBIDDEN = "Not allowed to view this contract.";

    private static final String ORGA_PREFIX = "Participant:";

    @Autowired
    private ServiceOfferingOrchestratorClient serviceOfferingOrchestratorClient;

    @Autowired
    private OrganizationOrchestratorClient organizationOrchestratorClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MessageQueueService messageQueueService;

    @Autowired
    private ContractSignerService contractSignerService;

    @Autowired
    private ContractTemplateRepository contractTemplateRepository;

    @Autowired
    private ContractMapper contractMapper;

    private boolean isValidFieldSelections(ContractTemplate contract, String authToken) throws JSONException {
        OfferingDetails offeringDetails = serviceOfferingOrchestratorClient.getOfferingDetails(
                contract.getOfferingId(), Map.of("Authorization", authToken));

        // make sure selections are valid
        if (contract.getRuntimeSelection() != null
                && !isValidRuntimeSelection(
                contract.getRuntimeSelection(), offeringDetails)) {
            return false;
        }

        if (contract instanceof SaasContractTemplate saasContract
                && saasContract.getUserCountSelection() != null
                && !isValidUserCountSelection(saasContract.getUserCountSelection(),
                (SaasOfferingDetails) offeringDetails)) {
            return false;
        }

        if (contract instanceof DataDeliveryContractTemplate dataDeliveryContract
                && dataDeliveryContract.getExchangeCountSelection() != null
                && !isValidExchangeCountSelection(dataDeliveryContract.getExchangeCountSelection(),
                (DataDeliveryOfferingDetails) offeringDetails)) {
            return false;
        }

        return true;
    }

    private boolean isValidRuntimeSelection(String selection, OfferingDetails offeringDetails) throws JSONException {
        if (offeringDetails.getRuntimeOption() == null || offeringDetails.getRuntimeOption().isEmpty()) {
            return false;
        }

        for (OfferingRuntimeOption option : offeringDetails.getRuntimeOption()) {
            if (selection.equals(option.getRuntimeCount() + " " + option.getRuntimeMeasurement())) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidUserCountSelection(String selection, SaasOfferingDetails offeringDetails) throws JSONException {
        if (offeringDetails.getUserCountOption() == null || offeringDetails.getUserCountOption().isEmpty()) {
            return false;
        }

        for (OfferingUserCountOption option : offeringDetails.getUserCountOption()) {
            if (selection.equals(String.valueOf(option.getUserCountUpTo()))) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidExchangeCountSelection(String selection, DataDeliveryOfferingDetails offeringDetails) throws JSONException {
        if (offeringDetails.getExchangeCountOption() == null || offeringDetails.getExchangeCountOption().isEmpty()) {
            return false;
        }

        for (OfferingExchangeCountOption option : offeringDetails.getExchangeCountOption()) {
            if (selection.equals(String.valueOf(option.getExchangeCountUpTo()))) {
                return true;
            }
        }
        return false;
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
        DataDeliveryProvisioning targetProvisioning = targetContract.getServiceContractProvisioning();
        DataDeliveryProvisioning editedProvisioning = editedContract.getServiceContractProvisioning();

        if (targetContract.getState() == ContractState.IN_DRAFT) {
            targetContract.setExchangeCountSelection(
                    editedContract.getExchangeCountSelection());
            if (isConsumer) {
                // TODO verify that this is a bucket that belongs to the connector
                targetProvisioning.setDataAddressTargetBucketName(
                        editedProvisioning.getDataAddressTargetBucketName());
                targetProvisioning.setDataAddressTargetFileName(
                        editedProvisioning.getDataAddressTargetFileName());
                targetProvisioning.setSelectedConsumerConnectorId(
                        editedProvisioning.getSelectedConsumerConnectorId());
            }
            if (isProvider) {
                targetProvisioning.setDataAddressType(
                        editedProvisioning.getDataAddressType());
                // TODO verify that this is a bucket that belongs to the connector
                targetProvisioning.setDataAddressSourceBucketName(
                        editedProvisioning.getDataAddressSourceBucketName());
                targetProvisioning.setDataAddressSourceFileName(
                        editedProvisioning.getDataAddressSourceFileName());
                // TODO verify that this is a valid connectorId
                targetProvisioning.setSelectedProviderConnectorId(
                        editedProvisioning.getSelectedProviderConnectorId());
            }
        } else if (targetContract.getState() == ContractState.SIGNED_CONSUMER && isProvider) {
            targetProvisioning.setDataAddressType(
                    editedProvisioning.getDataAddressType());
            // TODO verify that this is a bucket that belongs to the connector
            targetProvisioning.setDataAddressSourceBucketName(
                    editedProvisioning.getDataAddressSourceBucketName());
            targetProvisioning.setDataAddressSourceFileName(
                    editedProvisioning.getDataAddressSourceFileName());
            // TODO verify that this is a valid connectorId
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
        if (numPart == 0L || runtimeParts[1].equals("unlimited")) {
            return null;
        }
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
            throws JSONException, JsonProcessingException {

        // check that fields are in a valid format
        if (!contractCreateRequest.getOfferingId().startsWith("ServiceOffering:") ||
                !contractCreateRequest.getConsumerId().matches(ORGA_PREFIX + "\\d+")) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, INVALID_FIELD_DATA);
        }

        OfferingDetails offeringDetails = serviceOfferingOrchestratorClient.getOfferingDetails(
                contractCreateRequest.getOfferingId(), Map.of("Authorization", authToken));

        // in case someone with access rights to the state attempts to load this check the state as well
        if (offeringDetails.getState() != null && !offeringDetails.getState().equals("RELEASED")) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Referenced service offering is not valid");
        }

        // initialize contract fields, id and creation date
        ContractTemplate contract;

        switch (offeringDetails.getType()) {
            case "merlot:MerlotServiceOfferingSaaS" -> contract = new SaasContractTemplate();
            case "merlot:MerlotServiceOfferingDataDelivery" -> {
                contract = new DataDeliveryContractTemplate();
                // also store a copy of the data transfer type to later decide who can initiate a transfer
                ((DataDeliveryContractTemplate) contract).setDataTransferType(
                        ((DataDeliveryOfferingDetails) offeringDetails).getDataTransferType());
            }
            case "merlot:MerlotServiceOfferingCooperation" -> contract = new CooperationContractTemplate();
            default -> throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Unknown Service Offering Type.");
        }

        // extract data from request
        contract.setOfferingId(contractCreateRequest.getOfferingId());
        contract.setConsumerId(contractCreateRequest.getConsumerId());

        contract.setOfferingName(offeringDetails.getName());
        contract.setProviderId(offeringDetails.getOfferedBy());
        if (offeringDetails.getAttachments() != null) {
            contract.setOfferingAttachments(offeringDetails.getAttachments());
        }

        // check if consumer and provider are equal, and if so abort
        if (contract.getProviderId().equals(contract.getConsumerId())) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Provider and consumer must not be equal.");
        }

        OrganizationDetails organizationDetails = organizationOrchestratorClient.getOrganizationDetails(
                contract.getProviderId().replace(ORGA_PREFIX, ""),
                Map.of("Authorization", authToken));
        contract.setProviderTncUrl(organizationDetails.getTermsAndConditionsLink());

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
        } else if (contract instanceof CooperationContractTemplate cooperationContractTemplate) {
            contract = new CooperationContractTemplate(cooperationContractTemplate, true);
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

        // perform checks and set fields if needed
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
            contract.getServiceContractProvisioning().setValidUntil(
                    this.computeValidityTimestamp(contract.getRuntimeSelection()));
        }

        if (targetState == ContractState.PURGED) {
            if (!(contract.getState() == ContractState.DELETED && isProvider)) {
                throw new ResponseStatusException(FORBIDDEN, "Not allowed to purge contract");
            }
            contractTemplateRepository.delete(contract);
            messageQueueService.sendContractPurgedMessage(new ContractTemplateUpdated(contract));
            return contract;
        }

        // check if transitioning to the target state is generally allowed
        try {
            contract.transitionState(targetState);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(FORBIDDEN, e.getMessage());
        }

        // if all checks passed, save the new state of the contract
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
