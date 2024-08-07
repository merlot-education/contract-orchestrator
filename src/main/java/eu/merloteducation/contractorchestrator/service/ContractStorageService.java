/*
 *  Copyright 2024 Dataport. All rights reserved. Developed as part of the MERLOT project.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.models.entities.*;
import eu.merloteducation.contractorchestrator.models.entities.cooperation.CooperationContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.DataDeliveryContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.DataDeliveryProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.saas.SaasContractTemplate;
import eu.merloteducation.contractorchestrator.models.mappers.ContractFromDtoMapper;
import eu.merloteducation.contractorchestrator.models.mappers.ContractToDtoMapper;
import eu.merloteducation.contractorchestrator.models.mappers.ContractDtoToPdfMapper;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiablePresentation;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.PojoCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.serviceofferings.GxServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.AllowedUserCount;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.DataExchangeCount;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.OfferingRuntime;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotCoopContractServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotDataDeliveryServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotSaasServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotServiceOfferingCredentialSubject;
import eu.merloteducation.modelslib.api.contract.ContractBasicDto;
import eu.merloteducation.modelslib.api.contract.ContractCreateRequest;
import eu.merloteducation.modelslib.api.contract.ContractDto;
import eu.merloteducation.modelslib.api.contract.ContractPdfDto;
import eu.merloteducation.modelslib.api.contract.cooperation.CooperationContractDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.DataDeliveryContractProvisioningDto;
import eu.merloteducation.modelslib.api.contract.saas.SaasContractDto;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import eu.merloteducation.modelslib.queue.ContractTemplateUpdated;
import eu.merloteducation.s3library.service.StorageClient;
import eu.merloteducation.s3library.service.StorageClientException;
import io.netty.util.internal.StringUtil;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.json.JSONException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAmount;
import java.util.*;

import static org.springframework.http.HttpStatus.*;

@Service
public class ContractStorageService {

    private static final String INVALID_FIELD_DATA = "Fields contain invalid data.";
    private static final String INVALID_STATE_TRANSITION = "Requested transition is not allowed.";
    private static final String CONTRACT_NOT_FOUND = "Could not find a contract with this id.";
    private static final String CONTRACT_EDIT_FORBIDDEN = "Not allowed to edit this contract.";
    private static final String AUTHORIZATION = "Authorization";

    private final EntityManager entityManager;

    private final ServiceOfferingOrchestratorClient serviceOfferingOrchestratorClient;

    private final OrganizationOrchestratorClient organizationOrchestratorClient;

    private final PdfServiceClient pdfServiceClient;

    private final MessageQueueService messageQueueService;

    private final ContractTemplateRepository contractTemplateRepository;

    private final ContractToDtoMapper contractToDtoMapper;

    private final ContractFromDtoMapper contractFromDtoMapper;

    private final ContractDtoToPdfMapper contractDtoToPdfMapper;

    private final StorageClient storageClient;

    public ContractStorageService(@Autowired EntityManager entityManager,
                                  @Autowired ServiceOfferingOrchestratorClient serviceOfferingOrchestratorClient,
                                  @Autowired OrganizationOrchestratorClient organizationOrchestratorClient,
                                  @Autowired PdfServiceClient pdfServiceClient,
                                  @Autowired MessageQueueService messageQueueService,
                                  @Autowired ContractTemplateRepository contractTemplateRepository,
                                  @Autowired ContractToDtoMapper contractToDtoMapper,
                                  @Autowired ContractFromDtoMapper contractFromDtoMapper,
                                  @Autowired ContractDtoToPdfMapper contractDtoToPdfMapper,
                                  @Autowired StorageClient storageClient) {
        this.entityManager = entityManager;
        this.serviceOfferingOrchestratorClient = serviceOfferingOrchestratorClient;
        this.organizationOrchestratorClient = organizationOrchestratorClient;
        this.pdfServiceClient = pdfServiceClient;
        this.messageQueueService = messageQueueService;
        this.contractTemplateRepository = contractTemplateRepository;
        this.contractToDtoMapper = contractToDtoMapper;
        this.contractFromDtoMapper = contractFromDtoMapper;
        this.contractDtoToPdfMapper = contractDtoToPdfMapper;
        this.storageClient = storageClient;
    }


    private boolean isValidFieldSelections(ContractTemplate contract) throws JSONException {
        ServiceOfferingDto offeringDetails = messageQueueService.remoteRequestOfferingDetails(contract.getOfferingId());

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

        return !(contract instanceof DataDeliveryContractTemplate dataDeliveryContract)
                || StringUtil.isNullOrEmpty(dataDeliveryContract.getExchangeCountSelection())
                || isValidExchangeCountSelection(dataDeliveryContract.getExchangeCountSelection(),
                offeringDetails);
    }

    private boolean isValidRuntimeSelection(String selection, ServiceOfferingDto offeringDetails) throws JSONException {
        MerlotServiceOfferingCredentialSubject credentialSubject = offeringDetails.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotServiceOfferingCredentialSubject.class);
        List<OfferingRuntime> runtimeOptions = credentialSubject.getRuntimeOptions();
        if (runtimeOptions.isEmpty()) {
            return false;
        }

        for (OfferingRuntime option : runtimeOptions) {
            if (selection.equals(option.getRuntimeCount()
                    + " " + option.getRuntimeMeasurement())) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidUserCountSelection(String selection, ServiceOfferingDto offeringDetails) throws JSONException {

        MerlotSaasServiceOfferingCredentialSubject credentialSubject = offeringDetails.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotSaasServiceOfferingCredentialSubject.class);
        List<AllowedUserCount> userCountOptions = credentialSubject.getUserCountOptions();
        if (userCountOptions.isEmpty()) {
            return false;
        }

        for (AllowedUserCount option : userCountOptions) {
            if (selection.equals(Integer.toString(option.getUserCountUpTo()))) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidExchangeCountSelection(String selection, ServiceOfferingDto offeringDetails) throws JSONException {

        MerlotDataDeliveryServiceOfferingCredentialSubject credentialSubject = offeringDetails.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotDataDeliveryServiceOfferingCredentialSubject.class);
        List<DataExchangeCount> exchangeCountOptions = credentialSubject.getExchangeCountOptions();
        if (exchangeCountOptions.isEmpty()) {
            return false;
        }

        for (DataExchangeCount option : exchangeCountOptions) {
            if (selection.equals(Integer.toString(option.getExchangeCountUpTo()))) {
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
        DataDeliveryContractProvisioningDto sourceProvisioning = editedContract.getProvisioning();

        if (isConsumer
                && targetContract.getState() == ContractState.IN_DRAFT) {
            targetContract.setExchangeCountSelection(editedContract.getNegotiation().getExchangeCountSelection());
            targetProvisioning.setConsumerTransferProvisioning(
                    contractFromDtoMapper.transferProvisioningDtoToProvisioning(
                            sourceProvisioning.getConsumerTransferProvisioning()));
        } else if (isProvider
                && (targetContract.getState() == ContractState.IN_DRAFT
                || targetContract.getState() == ContractState.SIGNED_CONSUMER)) {
            if (targetContract.getState() == ContractState.IN_DRAFT) {
                targetContract.setExchangeCountSelection(editedContract.getNegotiation().getExchangeCountSelection());
            }
            targetProvisioning.setProviderTransferProvisioning(
                    contractFromDtoMapper.transferProvisioningDtoToProvisioning(
                            sourceProvisioning.getProviderTransferProvisioning()));
        }
    }

    private void updateContractDependingOnRole(ContractTemplate targetContract,
                                               ContractDto editedContract,
                                               boolean isConsumer,
                                               boolean isProvider) {
        if (targetContract.getState() == ContractState.IN_DRAFT) {
            targetContract.setRuntimeSelection(editedContract.getNegotiation().getRuntimeSelection());
            if (isConsumer) {
                targetContract.setConsumerTncAccepted(editedContract.getNegotiation().isConsumerTncAccepted());
                targetContract.setConsumerAttachmentsAccepted(editedContract.getNegotiation().isConsumerAttachmentsAccepted());
            }
            if (isProvider) {
                targetContract.setProviderTncAccepted(editedContract.getNegotiation().isProviderTncAccepted());
                targetContract.setAdditionalAgreements(editedContract.getNegotiation().getAdditionalAgreements());
            }
        } else if (targetContract.getState() == ContractState.SIGNED_CONSUMER && isProvider) {
            targetContract.setProviderTncAccepted(editedContract.getNegotiation().isProviderTncAccepted());

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
        MerlotParticipantDto providerDetails = organizationOrchestratorClient.getOrganizationDetails(template.getProviderId(),
                Map.of(AUTHORIZATION, authToken));
        MerlotParticipantDto consumerDetails = organizationOrchestratorClient.getOrganizationDetails(template.getConsumerId(),
                Map.of(AUTHORIZATION, authToken));
        ServiceOfferingDto offeringDetails = messageQueueService.remoteRequestOfferingDetails(template.getOfferingId());

        return contractToDtoMapper.contractToContractBasicDto(template, providerDetails, consumerDetails, offeringDetails);
    }

    private ContractDto castAndMapToContractDetailsDto(ContractTemplate template, String authToken) {

        MerlotParticipantDto providerDetails = organizationOrchestratorClient.getOrganizationDetails(template.getProviderId(),
                Map.of(AUTHORIZATION, authToken));
        MerlotParticipantDto consumerDetails = organizationOrchestratorClient.getOrganizationDetails(template.getConsumerId(),
                Map.of(AUTHORIZATION, authToken));
        ServiceOfferingDto offeringDetails = messageQueueService.remoteRequestOfferingDetails(template.getOfferingId());

        if (template instanceof DataDeliveryContractTemplate dataTemplate) {
            return contractToDtoMapper.contractToContractDto(dataTemplate,
                providerDetails, consumerDetails, offeringDetails);
        } else if (template instanceof SaasContractTemplate saasTemplate) {
            return contractToDtoMapper.contractToContractDto(saasTemplate,
                providerDetails, consumerDetails, offeringDetails);
        } else if (template instanceof CooperationContractTemplate coopTemplate) {
            return contractToDtoMapper.contractToContractDto(coopTemplate,
                providerDetails, consumerDetails, offeringDetails);
        }
        throw new IllegalArgumentException("Unknown contract or offering type.");
    }

    private ContractTemplate loadContract(String contractId) {
        ContractTemplate contract = contractTemplateRepository.findById(contractId).orElse(null);

        if (contract == null) {
            throw new ResponseStatusException(NOT_FOUND, CONTRACT_NOT_FOUND);
        }
        return contract;
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
        String regexServiceOfferingId = "(^urn:uuid:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$)";

        String regexOrganizationId = "did:web:[-.A-Za-z0-9:%#]*";
        String offeringId = contractCreateRequest.getOfferingId();
        String consumerId = contractCreateRequest.getConsumerId();

        if (!offeringId.matches(regexServiceOfferingId) || !consumerId.matches(regexOrganizationId)) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, INVALID_FIELD_DATA);
        }

        // for creating a contract we will not use the message bus to be certain that the offer is available upon contract creation
        ServiceOfferingDto offeringDetails = serviceOfferingOrchestratorClient.getOfferingDetails(
            offeringId, Map.of(AUTHORIZATION, authToken));

        GxServiceOfferingCredentialSubject gxServiceOfferingCs = offeringDetails.getSelfDescription()
                .findFirstCredentialSubjectByType(GxServiceOfferingCredentialSubject.class);
        PojoCredentialSubject merlotSpecificServiceOfferingCs =
                getSpecificServiceOfferingCs(offeringDetails.getSelfDescription());

        // in case someone with access rights to the state attempts to load this check the state as well
        if ((offeringDetails.getMetadata().getState() != null && !offeringDetails.getMetadata().getState().equals("RELEASED"))
            || gxServiceOfferingCs == null || merlotSpecificServiceOfferingCs == null) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Referenced service offering is not valid");
        }

        // initialize contract fields, id and creation date
        ContractTemplate contract;

        switch (merlotSpecificServiceOfferingCs.getType()) {
            case MerlotSaasServiceOfferingCredentialSubject.TYPE -> contract = new SaasContractTemplate();
            case MerlotDataDeliveryServiceOfferingCredentialSubject.TYPE -> {
                contract = new DataDeliveryContractTemplate();
                DataDeliveryProvisioning provisioning = new DataDeliveryProvisioning();
                // provider transfer method should be set on service offering level,
                // which should also limit available consumer methods.
                // for now we let the users select the method, and currently only IONOS is supported.
                // If this is to be exchanged later by potential other methods,
                // we need to carefully consider which methods are inter-compatible
                // (or only allow same-type transfers on provider and consumer side)
                contract.setServiceContractProvisioning(provisioning);
            }
            case MerlotCoopContractServiceOfferingCredentialSubject.TYPE -> contract = new CooperationContractTemplate();
            default -> throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Unknown Service Offering Type.");
        }

        // extract data from request
        contract.setOfferingId(offeringId);
        contract.setConsumerId(consumerId);
        contract.setProviderId(gxServiceOfferingCs.getProvidedBy().getId());

        // check if consumer and provider are equal, and if so abort
        if (contract.getProviderId().equals(contract.getConsumerId())) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Provider and consumer must not be equal.");
        }

        // copy all terms and conditions entries from offering to contract
        List<ContractTnc> offeringTnC = gxServiceOfferingCs.getTermsAndConditions().stream().map(ContractTnc::new)
                .toList();
        contract.setTermsAndConditions(offeringTnC);


        contract = contractTemplateRepository.saveAndFlush(contract);
        entityManager.refresh(contract); // refresh entity from database to get newly generated discriminator column
        messageQueueService.sendContractCreatedMessage(new ContractTemplateUpdated(contract.getId(), contract.getOfferingId()));
        return castAndMapToContractDetailsDto(contract, authToken);
    }

    /**
     * For a given contract id this attempts to find the corresponding contract, check access and create a new contract
     * with a copy of all editable fields. A new ID is generated as well as the signatures are reset.
     *
     * @param contractId         id of the contract to copy
     * @param authToken          the OAuth2 Token from the user requesting this action
     * @return newly generated contract
     */
    public ContractDto regenerateContract(String contractId, String authToken) {
        ContractTemplate contract = this.loadContract(contractId);

        if (!(contract.getState() == ContractState.DELETED || contract.getState() == ContractState.ARCHIVED)) {
            throw new ResponseStatusException(FORBIDDEN, CONTRACT_EDIT_FORBIDDEN);
        }

        // Make sure we can still access the requested offering, otherwise exception is thrown
        serviceOfferingOrchestratorClient.getOfferingDetails(
                contract.getOfferingId(), Map.of(AUTHORIZATION, authToken));

        if (contract instanceof DataDeliveryContractTemplate dataDeliveryContract) {
            contract = new DataDeliveryContractTemplate(dataDeliveryContract, true);
        } else if (contract instanceof SaasContractTemplate saasContractTemplate) {
            contract = new SaasContractTemplate(saasContractTemplate, true);
        } else if (contract instanceof CooperationContractTemplate cooperationContractTemplate) {
            contract = new CooperationContractTemplate(cooperationContractTemplate, true);
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

        ContractTemplate contract = this.loadContract(editedContract.getDetails().getId());

        boolean isConsumer = activeRoleOrgaId.equals(contract.getConsumerId());
        boolean isProvider = activeRoleOrgaId.equals(contract.getProviderId());

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
        if (!isValidFieldSelections(contract)) {
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
     * @param userName         the name of the user that requested this action
     * @param authToken        the OAuth2 Token from the user requesting this action
     * @return updated contract template from database
     */
    @Transactional(rollbackOn = {ResponseStatusException.class})
    public ContractDto transitionContractTemplateState(String contractId,
                                                       ContractState targetState,
                                                       String activeRoleOrgaId,
                                                       String userName,
                                                       String authToken) {
        ContractTemplate contract = this.loadContract(contractId);

        boolean isConsumer = activeRoleOrgaId.equals(contract.getConsumerId());
        boolean isProvider = activeRoleOrgaId.equals(contract.getProviderId());

        // user must be either consumer or provider of contract
        if (!(isConsumer || isProvider)) {
            throw new ResponseStatusException(FORBIDDEN, CONTRACT_EDIT_FORBIDDEN);
        }

        // perform checks and set fields if needed
        if (targetState == ContractState.SIGNED_CONSUMER) {
            if (!isConsumer) {
                throw new ResponseStatusException(FORBIDDEN, INVALID_STATE_TRANSITION);
            }
            contract.setConsumerSignature(new ContractSignature(userName));
        }

        if (targetState == ContractState.RELEASED) {
            if (!isProvider) {
                throw new ResponseStatusException(FORBIDDEN, INVALID_STATE_TRANSITION);
            }
            contract.setProviderSignature(new ContractSignature(userName));
            contract.getServiceContractProvisioning().setValidUntil(
                    this.computeValidityTimestamp(contract.getRuntimeSelection()));
        }

        if (targetState == ContractState.PURGED) {
            if (!(contract.getState() == ContractState.DELETED && isProvider)) {
                throw new ResponseStatusException(FORBIDDEN, "Not allowed to purge contract");
            }
            contractTemplateRepository.delete(contract);
            messageQueueService.sendContractPurgedMessage(new ContractTemplateUpdated(contract.getId(), contract.getOfferingId()));
            return castAndMapToContractDetailsDto(contract, authToken);
        }

        // check if transitioning to the target state is generally allowed
        try {
            contract.transitionState(targetState);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage());
        }

        // if all checks passed, save the new state of the contract
        contractTemplateRepository.save(contract);

        ContractDto contractDto = castAndMapToContractDetailsDto(contract, authToken);

        if (targetState == ContractState.RELEASED) {
            // if successfully released, create and save the contract pdf
            saveContractPdf(contractDto);
        }

        return contractDto;
    }

    private void saveContractPdf(ContractDto contractDto) {

        ContractPdfDto contractPdfDto = castAndMapToContractPdfDto(contractDto);
        try {
            byte[] pdfBytes = pdfServiceClient.getPdfContract(contractPdfDto);
            String fileName = contractDto.getDetails().getId() + ".pdf";
            storageClient.pushItem(getPathToContractPdf(contractDto.getDetails().getId()), fileName, pdfBytes);
        } catch (Exception e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Encountered error while processing the contract.");
        }
    }

    private ContractPdfDto castAndMapToContractPdfDto(ContractDto contractDto) {

        if (contractDto instanceof DataDeliveryContractDto dataDto) {
            return contractDtoToPdfMapper.contractDtoToContractPdfDto(dataDto);
        } else if (contractDto instanceof SaasContractDto saasDto) {
            return contractDtoToPdfMapper.contractDtoToContractPdfDto(saasDto);
        } else if (contractDto instanceof CooperationContractDto coopDto) {
            return contractDtoToPdfMapper.contractDtoToContractPdfDto(coopDto);
        }

        throw new IllegalArgumentException("Unknown contract or offering type.");
    }

    /**
     * Returns all contracts from the database where the specified organization is either the consumer or provider.
     *
     * @param orgaId       id of the organization requesting this data
     * @param pageable     page request
     * @param statusFilter optional status filter for the contracts
     * @param authToken    the OAuth2 Token from the user requesting this action
     * @return Page of contracts that are related to this organization
     */
    public Page<ContractBasicDto> getOrganizationContracts(String orgaId, Pageable pageable, ContractState statusFilter,
                                                           String authToken) {
        String regex = "did:web:[-.A-Za-z0-9:%#]*";
        if (!orgaId.matches(regex)) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, INVALID_FIELD_DATA);
        }
        Page<ContractTemplate> contractTemplates;
        if (statusFilter == null) {
            contractTemplates = contractTemplateRepository.findAllByOrgaId(orgaId, pageable);
        } else {
            contractTemplates = contractTemplateRepository.findAllByOrgaIdAndState(orgaId, statusFilter, pageable);
        }


        return contractTemplates.map(template -> mapToContractBasicDto(template, authToken));
    }

    /**
     * For a given id, return the corresponding contract database entry.
     *
     * @param contractId id of the contract
     * @param authToken  the OAuth2 Token from the user requesting this action
     * @return contract object from the database
     */
    public ContractDto getContractDetails(String contractId, String authToken) {
        ContractTemplate contract = this.loadContract(contractId);

        return castAndMapToContractDetailsDto(contract, authToken);
    }

    /**
     * Given a contract id and a file upload, add the file to the bucket and store the reference in the contract.
     *
     * @param contractId contract id
     * @param attachment uploaded file as byte array
     * @param fileName name of the uploaded file
     * @param authToken the OAuth2 Token from the user requesting this action
     * @return updated contract
     */
    @Transactional(rollbackOn = {ResponseStatusException.class})
    public ContractDto addContractAttachment(String contractId, byte[] attachment, String fileName,
                                             String authToken) {
        ContractTemplate contract = this.loadContract(contractId);

        if (contract.getAttachments() != null && contract.getAttachments().size() >= 10) {
            throw new ResponseStatusException(BAD_REQUEST, "Cannot add attachments to contract.");
        }

        // add file to contract
        contract.addAttachment(fileName);
        contractTemplateRepository.save(contract);

        try {
            storageClient.pushItem(contract.getId(), fileName, attachment);
        } catch (StorageClientException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Encountered error while saving the contract attachment.");
        }

        return castAndMapToContractDetailsDto(contract, authToken);
    }

    /**
     * Given a contract id and an attachment reference, delete the attachment from the contract and bucket.
     *
     * @param contractId contract id
     * @param attachmentId reference to the attachment
     * @param authToken the OAuth2 Token from the user requesting this action
     * @return updated contract
     */
    @Transactional(rollbackOn = {ResponseStatusException.class})
    public ContractDto deleteContractAttachment(String contractId, String attachmentId,
                                                String authToken) {
        ContractTemplate contract = this.loadContract(contractId);

        boolean attachmentDeleted = contract.getAttachments().remove(attachmentId);
        if (!attachmentDeleted) {
            throw new ResponseStatusException(NOT_FOUND, "Specified attachment was not found in this contract.");
        }

        contractTemplateRepository.save(contract);

        try {
            storageClient.deleteItem(contract.getId(), attachmentId);
        } catch (StorageClientException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Encountered error while deleting the contract attachment.");
        }

        return castAndMapToContractDetailsDto(contract, authToken);
    }

    /**
     * Given a contract id and an attachment reference, provide the attachment as a download to the user.
     *
     * @param contractId contract id
     * @param attachmentId reference to the attachment
     * @return attachment as byte array
     */
    public byte[] getContractAttachment(String contractId, String attachmentId)
        throws IOException, StorageClientException {
        ContractTemplate contract = this.loadContract(contractId);
        if (!contract.getAttachments().contains(attachmentId)) {
            throw new ResponseStatusException(NOT_FOUND, "No attachment with this ID was found.");
        }

        return storageClient.getItem(contract.getId(), attachmentId);
    }

    public byte[] getContractPdf(String contractId)
        throws IOException, StorageClientException {
        return storageClient.getItem(getPathToContractPdf(contractId), contractId + ".pdf");
    }

    private String getPathToContractPdf(String contractId){
        return contractId + "/contractPdf";
    }

    private PojoCredentialSubject getSpecificServiceOfferingCs(ExtendedVerifiablePresentation vp) {

        // consider all MERLOT specific offering classes
        List<Class<? extends PojoCredentialSubject>> csClasses =
                List.of(
                        MerlotDataDeliveryServiceOfferingCredentialSubject.class,
                        MerlotSaasServiceOfferingCredentialSubject.class,
                        MerlotCoopContractServiceOfferingCredentialSubject.class
                );

        // check if any of them match, and if so, return the pojo object
        for (Class<? extends PojoCredentialSubject> csClass : csClasses) {
            PojoCredentialSubject cs = vp.findFirstCredentialSubjectByType(csClass);
            // if additional logic is needed per type, it can be added here
            if (cs != null) {
                return cs;
            }
        }
        return null;
    }
}
