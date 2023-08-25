package eu.merloteducation.contractorchestrator.service;

import com.fasterxml.jackson.databind.JsonNode;
import eu.merloteducation.contractorchestrator.models.dto.ContractBasicDto;
import eu.merloteducation.contractorchestrator.models.dto.ContractDto;
import eu.merloteducation.contractorchestrator.models.dto.datadelivery.DataDeliveryContractDetailsDto;
import eu.merloteducation.contractorchestrator.models.dto.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.contractorchestrator.models.dto.saas.SaasContractDetailsDto;
import eu.merloteducation.contractorchestrator.models.dto.saas.SaasContractDto;
import eu.merloteducation.contractorchestrator.models.organisationsorchestrator.OrganizationDetails;
import eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator.*;
import eu.merloteducation.contractorchestrator.models.entities.*;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.models.mappers.ContractMapper;
import eu.merloteducation.contractorchestrator.models.messagequeue.ContractTemplateUpdated;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import io.netty.util.internal.StringUtil;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.json.JSONException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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

    private static final String CREDENTIAL_SUBJECT = "credentialSubject";
    private static final String VERIFIABLE_CREDENTIAL = "verifiableCredential";
    private static final String AUTHORIZATION = "Authorization";
    private static final String VALUE = "@value";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ServiceOfferingOrchestratorClient serviceOfferingOrchestratorClient;

    @Autowired
    private OrganizationOrchestratorClient organizationOrchestratorClient;

    @Autowired
    private MessageQueueService messageQueueService;

    @Autowired
    private ContractSignerService contractSignerService;

    @Autowired
    private ContractTemplateRepository contractTemplateRepository;

    @Autowired
    private ContractMapper contractMapper;

    private boolean isValidFieldSelections(ContractTemplate contract, String authToken) throws JSONException {
        ServiceOfferingDetails offeringDetails = messageQueueService.remoteRequestOfferingDetails(contract.getOfferingId());

        // make sure selections are valid
        if (!StringUtil.isNullOrEmpty(contract.getRuntimeSelection())
                && !isValidRuntimeSelection(
                contract.getRuntimeSelection(), offeringDetails)) {
            return false;
        }

        if (contract instanceof SaasContractTemplate saasContract
                && !StringUtil.isNullOrEmpty(saasContract.getUserCountSelection())
                && !isValidUserCountSelection(saasContract.getUserCountSelection(),
                offeringDetails)) {
            return false;
        }

        if (contract instanceof DataDeliveryContractTemplate dataDeliveryContract
                && !StringUtil.isNullOrEmpty(dataDeliveryContract.getExchangeCountSelection())
                && !isValidExchangeCountSelection(dataDeliveryContract.getExchangeCountSelection(),
                offeringDetails)) {
            return false;
        }

        return true;
    }

    private boolean isValidRuntimeSelection(String selection, ServiceOfferingDetails offeringDetails) throws JSONException {
        JsonNode credentialSubject = offeringDetails.getSelfDescription().get(VERIFIABLE_CREDENTIAL).get(CREDENTIAL_SUBJECT);
        JsonNode runtimeOptions = credentialSubject.get("merlot:runtimeOption");
        if (!runtimeOptions.isArray()) {
            return false;
        }

        for (final JsonNode option: runtimeOptions) {
            if (selection.equals(option.get("merlot:runtimeCount").get(VALUE).asText()
                    + " " + option.get("merlot:runtimeMeasurement").get(VALUE).asText())) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidUserCountSelection(String selection, ServiceOfferingDetails offeringDetails) throws JSONException {

        JsonNode credentialSubject = offeringDetails.getSelfDescription().get(VERIFIABLE_CREDENTIAL).get(CREDENTIAL_SUBJECT);
        JsonNode userCountOptions = credentialSubject.get("merlot:userCountOption");
        if (!userCountOptions.isArray()) {
            return false;
        }

        for (final JsonNode option: userCountOptions) {
            if (selection.equals(option.get("merlot:userCountUpTo").get(VALUE).asText())) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidExchangeCountSelection(String selection, ServiceOfferingDetails offeringDetails) throws JSONException {

        JsonNode credentialSubject = offeringDetails.getSelfDescription().get(VERIFIABLE_CREDENTIAL).get(CREDENTIAL_SUBJECT);
        JsonNode exchangeCountOptions = credentialSubject.get("merlot:exchangeCountOption");
        if (!exchangeCountOptions.isArray()) {
            return false;
        }

        for (final JsonNode option: exchangeCountOptions) {
            if (selection.equals(option.get("merlot:exchangeCountUpTo").get(VALUE).asText())) {
                return true;
            }
        }

        return false;
    }

    private void updateSaasContract(SaasContractTemplate targetContract,
                                    SaasContractDto editedContract) {
        if (targetContract.getState() == ContractState.IN_DRAFT) {
            targetContract.setUserCountSelection(editedContract.getNegotiation().getUserCountSelection());
        }
    }

    private void updateDataDeliveryContract(DataDeliveryContractTemplate targetContract,
                                            DataDeliveryContractDto editedContract,
                                            boolean isConsumer,
                                            boolean isProvider) {
        DataDeliveryProvisioning targetProvisioning = targetContract.getServiceContractProvisioning();

        if (targetContract.getState() == ContractState.IN_DRAFT) {
            targetContract.setExchangeCountSelection(
                    editedContract.getNegotiation().getExchangeCountSelection());
            if (isConsumer) {
                // TODO verify that this is a bucket that belongs to the connector
                targetProvisioning.setDataAddressTargetBucketName(
                        editedContract.getProvisioning().getDataAddressTargetBucketName());
                targetProvisioning.setDataAddressTargetFileName(
                        editedContract.getProvisioning().getDataAddressTargetFileName());
                targetProvisioning.setSelectedConsumerConnectorId(
                        editedContract.getProvisioning().getSelectedConsumerConnectorId());
            }
            if (isProvider) {
                targetProvisioning.setDataAddressType(
                        editedContract.getProvisioning().getDataAddressType());
                // TODO verify that this is a bucket that belongs to the connector
                targetProvisioning.setDataAddressSourceBucketName(
                        editedContract.getProvisioning().getDataAddressSourceBucketName());
                targetProvisioning.setDataAddressSourceFileName(
                        editedContract.getProvisioning().getDataAddressSourceFileName());
                // TODO verify that this is a valid connectorId
                targetProvisioning.setSelectedProviderConnectorId(
                        editedContract.getProvisioning().getSelectedProviderConnectorId());
            }
        } else if (targetContract.getState() == ContractState.SIGNED_CONSUMER && isProvider) {
            targetProvisioning.setDataAddressType(
                    editedContract.getProvisioning().getDataAddressType());
            // TODO verify that this is a bucket that belongs to the connector
            targetProvisioning.setDataAddressSourceBucketName(
                    editedContract.getProvisioning().getDataAddressSourceBucketName());
            targetProvisioning.setDataAddressSourceFileName(
                    editedContract.getProvisioning().getDataAddressSourceFileName());
            // TODO verify that this is a valid connectorId
            targetProvisioning.setSelectedProviderConnectorId(
                    editedContract.getProvisioning().getSelectedProviderConnectorId());
        }
    }

    private void updateContractDependingOnRole(ContractTemplate targetContract,
                                               ContractDto editedContract,
                                               boolean isConsumer,
                                               boolean isProvider) {
        // TODO consider moving this logic into a DTO pattern
        if (targetContract.getState() == ContractState.IN_DRAFT) {
            targetContract.setRuntimeSelection(editedContract.getNegotiation().getRuntimeSelection());
            if (isConsumer) {
                targetContract.setConsumerMerlotTncAccepted(editedContract.getNegotiation().isConsumerMerlotTncAccepted());
                targetContract.setConsumerProviderTncAccepted(editedContract.getNegotiation().isConsumerProviderTncAccepted());
                targetContract.setConsumerOfferingTncAccepted(editedContract.getNegotiation().isConsumerOfferingTncAccepted());
            }
            if (isProvider) {
                targetContract.setProviderMerlotTncAccepted(editedContract.getNegotiation().isProviderMerlotTncAccepted());
                targetContract.setAdditionalAgreements(editedContract.getNegotiation().getAdditionalAgreements());
                targetContract.setOfferingAttachments(editedContract.getNegotiation().getAttachments());
            }
        } else if (targetContract.getState() == ContractState.SIGNED_CONSUMER && isProvider) {
            targetContract.setProviderMerlotTncAccepted(editedContract.getNegotiation().isProviderMerlotTncAccepted());

        }

        if (targetContract instanceof SaasContractTemplate targetSaasContractTemplate &&
                editedContract instanceof SaasContractDto editedSaasContractTemplate) {
            updateSaasContract(targetSaasContractTemplate, editedSaasContractTemplate);
        }

        if (targetContract instanceof DataDeliveryContractTemplate targetDataDeliveryContractTemplate &&
                editedContract instanceof DataDeliveryContractDto editedDataDeliveryContractTemplate) {
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

    private ContractBasicDto mapToContractBasicDto(ContractTemplate template, String authToken) {
        OrganizationDetails consumerDetails = organizationOrchestratorClient.getOrganizationDetails(template.getConsumerId(),
                Map.of(AUTHORIZATION, authToken));
        ServiceOfferingDetails offeringDetails = messageQueueService.remoteRequestOfferingDetails(template.getOfferingId());
        return contractMapper.contractToContractBasicDto(template, consumerDetails, offeringDetails);
    }

    private ContractDto castAndMapToContractDetailsDto(ContractTemplate template, String authToken) {

        OrganizationDetails consumerDetails = organizationOrchestratorClient.getOrganizationDetails(template.getConsumerId(),
                Map.of(AUTHORIZATION, authToken));
        ServiceOfferingDetails offeringDetails = messageQueueService.remoteRequestOfferingDetails(template.getOfferingId());

        if (template instanceof DataDeliveryContractTemplate dataTemplate) {
            return contractMapper.contractToContractDto(dataTemplate,
                    consumerDetails, offeringDetails);
        } else if (template instanceof SaasContractTemplate saasTemplate) {
            return contractMapper.contractToContractDto(saasTemplate,
                    consumerDetails, offeringDetails);
        } else if (template instanceof CooperationContractTemplate coopTemplate) {
            return contractMapper.contractToContractDto(coopTemplate,
                    consumerDetails, offeringDetails);
        }
        throw new IllegalArgumentException("Unknown contract or offering type.");
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
    public ContractDto addContractTemplate(ContractCreateRequest contractCreateRequest, String authToken)
            throws JSONException {

        // check that fields are in a valid format
        if (!contractCreateRequest.getOfferingId().startsWith("ServiceOffering:") ||
                !contractCreateRequest.getConsumerId().matches(ORGA_PREFIX + "\\d+")) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, INVALID_FIELD_DATA);
        }

        // for creating a contract we will not use the message bus to be certain that the offer is available upon contract creation
        ServiceOfferingDetails offeringDetails = serviceOfferingOrchestratorClient.getOfferingDetails(
                contractCreateRequest.getOfferingId(), Map.of(AUTHORIZATION, authToken));

        // in case someone with access rights to the state attempts to load this check the state as well
        if (offeringDetails.getMetadata().get("state") != null && !offeringDetails.getMetadata().get("state").asText().equals("RELEASED")) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Referenced service offering is not valid");
        }

        // initialize contract fields, id and creation date
        ContractTemplate contract;

        JsonNode credentialSubject = offeringDetails.getSelfDescription().get(VERIFIABLE_CREDENTIAL).get(CREDENTIAL_SUBJECT);

        switch (credentialSubject.get("@type").asText()) {
            case "merlot:MerlotServiceOfferingSaaS" -> contract = new SaasContractTemplate();
            case "merlot:MerlotServiceOfferingDataDelivery" -> contract = new DataDeliveryContractTemplate();
            case "merlot:MerlotServiceOfferingCooperation" -> contract = new CooperationContractTemplate();
            default -> throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Unknown Service Offering Type.");
        }

        // extract data from request
        contract.setOfferingId(contractCreateRequest.getOfferingId());
        contract.setConsumerId(contractCreateRequest.getConsumerId());
        contract.setProviderId(credentialSubject.get("gax-core:offeredBy").get("@id").asText());

        List<String> attachments = new ArrayList<>();
        if (credentialSubject.has("merlot:attachments")) {
            for (JsonNode attachment : credentialSubject.get("merlot:attachments")) {
                attachments.add(attachment.asText());
            }
        }
        contract.setOfferingAttachments(attachments);

        // check if consumer and provider are equal, and if so abort
        if (contract.getProviderId().equals(contract.getConsumerId())) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Provider and consumer must not be equal.");
        }

        OrganizationDetails organizationDetails = organizationOrchestratorClient.getOrganizationDetails(
                contract.getProviderId().replace(ORGA_PREFIX, ""),
                Map.of(AUTHORIZATION, authToken));
        contract.setProviderTncUrl(organizationDetails.getSelfDescription()
                .getVerifiableCredential().getCredentialSubject().getTermsAndConditions().getContent().getValue());
        contract.setProviderTncHash(organizationDetails.getSelfDescription()
                .getVerifiableCredential().getCredentialSubject().getTermsAndConditions().getHash().getValue());

        contract = contractTemplateRepository.saveAndFlush(contract);
        entityManager.refresh(contract); // refresh entity from database to get newly generated discriminator column
        messageQueueService.sendContractCreatedMessage(new ContractTemplateUpdated(contract));
        return castAndMapToContractDetailsDto(contract, authToken);
    }

    /**
     * For a given contract id this attempts to find the corresponding contract, check access and create a new contract
     * with a copy of all editable fields. A new ID is generated as well as the signatures are reset.
     *
     * @param contractId         id of the contract to copy
     * @param authToken          the OAuth2 Token from the user requesting this action
     * @param representedOrgaIds list of organization ids the user represents
     * @return newly generated contract
     */
    public ContractDto regenerateContract(String contractId, Set<String> representedOrgaIds, String authToken) {
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

        // Make sure we can still access the requested offering, otherwise exception is thrown
        serviceOfferingOrchestratorClient.getOfferingDetails(
                contract.getOfferingId(), Map.of(AUTHORIZATION, authToken));

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

        return castAndMapToContractDetailsDto(contract, authToken);
    }

    /**
     * Given an edited ContractTemplate, this function verifies the updated fields and writes them to the database if allowed.
     *
     * @param editedContract   contract template with edited fields
     * @param authToken        the OAuth2 Token from the user requesting this action
     * @param activeRoleOrgaId the currently selected role of the user
     * @return updated contract template from database
     */
    public ContractDto updateContractTemplate(ContractDto editedContract,
                                                   String authToken,
                                                   String activeRoleOrgaId) throws JSONException {

        ContractTemplate contract = contractTemplateRepository.findById(editedContract.getDetails().getId()).orElse(null);

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
        contract = contractTemplateRepository.save(contract);
        return castAndMapToContractDetailsDto(contract, authToken);
    }

    /**
     * Transition the contract template attached to the given id to the target state if allowed.
     *
     * @param contractId       id of the contract template to transition
     * @param targetState      target state of the contract template
     * @param activeRoleOrgaId the currently selected role of the user
     * @param userId           the id of the user that requested this action
     * @param authToken the OAuth2 Token from the user requesting this action
     * @return updated contract template from database
     */
    public ContractDto transitionContractTemplateState(String contractId,
                                                            ContractState targetState,
                                                            String activeRoleOrgaId,
                                                            String userId,
                                                            String authToken) {
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
            return castAndMapToContractDetailsDto(contract, authToken);
        }

        // check if transitioning to the target state is generally allowed
        try {
            contract.transitionState(targetState);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(FORBIDDEN, e.getMessage());
        }

        // if all checks passed, save the new state of the contract
        contract = contractTemplateRepository.save(contract);

        return castAndMapToContractDetailsDto(contract, authToken);
    }

    /**
     * Returns all contracts from the database where the specified organization is either the consumer or provider.
     *
     * @param orgaId    id of the organization requesting this data
     * @param pageable  page request
     * @param authToken the OAuth2 Token from the user requesting this action
     * @return Page of contracts that are related to this organization
     */
    public Page<ContractBasicDto> getOrganizationContracts(String orgaId, Pageable pageable, String authToken) {
        if (!orgaId.matches(ORGA_PREFIX + "\\d+")) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, INVALID_FIELD_DATA);
        }
        Page<ContractTemplate> contractTemplates = contractTemplateRepository.
                findAllByProviderIdOrConsumerId(orgaId, orgaId, pageable);

        return contractTemplates.map(template -> mapToContractBasicDto(template, authToken));
    }

    /**
     * For a given id, return the corresponding contract database entry.
     *
     * @param contractId         id of the contract
     * @param authToken          the OAuth2 Token from the user requesting this action
     * @return contract object from the database
     */
    public ContractDto getContractDetails(String contractId, Set<String> representedOrgaIds, String authToken) {
        System.out.println(contractId);
        ContractTemplate contract = contractTemplateRepository.findById(contractId).orElse(null);


        if (contract == null) {
            throw new ResponseStatusException(NOT_FOUND, CONTRACT_NOT_FOUND);
        }

        if (!(representedOrgaIds.contains(contract.getConsumerId().replace(ORGA_PREFIX, ""))
                || representedOrgaIds.contains(contract.getProviderId().replace(ORGA_PREFIX, "")))) {
            throw new ResponseStatusException(FORBIDDEN, CONTRACT_VIEW_FORBIDDEN);
        }

        return castAndMapToContractDetailsDto(contract, authToken);
    }
}
