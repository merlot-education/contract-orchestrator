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

import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotDataDeliveryServiceOfferingCredentialSubject;
import eu.merloteducation.modelslib.api.contract.ContractDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.TransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ionoss3extension.IonosS3ConsumerTransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ionoss3extension.IonosS3ProviderTransferProvisioningDto;
import eu.merloteducation.modelslib.api.organization.IonosS3BucketDto;
import eu.merloteducation.modelslib.api.organization.OrganizationConnectorTransferDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import eu.merloteducation.modelslib.edc.asset.AssetCreateRequest;
import eu.merloteducation.modelslib.edc.asset.AssetProperties;
import eu.merloteducation.modelslib.edc.asset.DataAddress;
import eu.merloteducation.modelslib.edc.asset.ionoss3extension.IonosS3DataDestination;
import eu.merloteducation.modelslib.edc.asset.ionoss3extension.IonosS3DataSource;
import eu.merloteducation.modelslib.edc.catalog.CatalogRequest;
import eu.merloteducation.modelslib.edc.catalog.DcatCatalog;
import eu.merloteducation.modelslib.edc.catalog.DcatDataset;
import eu.merloteducation.modelslib.edc.common.IdResponse;
import eu.merloteducation.modelslib.edc.contractdefinition.ContractDefinitionCreateRequest;
import eu.merloteducation.modelslib.edc.contractdefinition.Criterion;
import eu.merloteducation.modelslib.edc.negotiation.ContractNegotiation;
import eu.merloteducation.modelslib.edc.negotiation.ContractOffer;
import eu.merloteducation.modelslib.edc.negotiation.NegotiationInitiateRequest;
import eu.merloteducation.modelslib.edc.policy.Policy;
import eu.merloteducation.modelslib.edc.policy.PolicyCreateRequest;
import eu.merloteducation.modelslib.edc.transfer.IonosS3TransferProcess;
import eu.merloteducation.modelslib.edc.transfer.TransferRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class EdcOrchestrationService {
    private final MessageQueueService messageQueueService;
    private final ContractStorageService contractStorageService;
    private final ObjectProvider<EdcClient> edcClientProvider;
    private final TaskScheduler taskScheduler;

    public EdcOrchestrationService(@Autowired MessageQueueService messageQueueService,
                                   @Autowired ContractStorageService contractStorageService,
                                   @Autowired ObjectProvider<EdcClient> edcClientProvider,
                                   @Autowired TaskScheduler taskScheduler) {
        this.messageQueueService = messageQueueService;
        this.contractStorageService = contractStorageService;
        this.edcClientProvider = edcClientProvider;
        this.taskScheduler = taskScheduler;
    }

    private DataDeliveryContractDto loadContract(String contractId, String activeRoleOrgaId, String authToken) {
        ContractDto contract = contractStorageService.getContractDetails(contractId, authToken);
        if (!(contract instanceof DataDeliveryContractDto dataDeliveryContractDetailsDto)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Provided contract is not of type Data Delivery.");
        }
        if (!contract.getDetails().getState().equals(ContractState.RELEASED.name())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Provided contract is in wrong state.");
        }
        checkTransferAuthorization(dataDeliveryContractDetailsDto, activeRoleOrgaId);
        return dataDeliveryContractDetailsDto;
    }

    private void checkTransferAuthorization(DataDeliveryContractDto contractDto, String activeRoleOrgaId) {
        boolean isConsumer = activeRoleOrgaId.equals(contractDto.getDetails().getConsumerId());
        boolean isProvider = activeRoleOrgaId.equals(contractDto.getDetails().getProviderId());
        ServiceOfferingDto offeringDetails = contractDto.getOffering();
        MerlotDataDeliveryServiceOfferingCredentialSubject credentialSubject = offeringDetails.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotDataDeliveryServiceOfferingCredentialSubject.class);
        String dataTransferType = credentialSubject.getDataTransferType();

        if (!((dataTransferType.equals("Push") && isProvider) ||
                (dataTransferType.equals("Pull") && isConsumer)
        )) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your role is not authorized to perform the data transfer");
        }
    }

    private OrganizationConnectorTransferDto getOrgaConnector(String orgaId, String connectorId) {
        return messageQueueService
                .remoteRequestOrganizationConnectorByConnectorId(
                        orgaId, connectorId);
    }

    /**
     * Given a contract id, a role and a set of represented organizations, start the automated EDC negotiation
     * over the contract.
     *
     * @param contractId         contract id
     * @param activeRoleOrgaId   currently active role
     * @param authToken          user auth token
     * @return negotiation initiation response
     */
    public IdResponse initiateConnectorNegotiation(String contractId, String activeRoleOrgaId, String authToken) {
        DataDeliveryContractDto contractDto = loadContract(contractId, activeRoleOrgaId, authToken);
        TransferProvisioningDto providerTransferDto = contractDto.getProvisioning().getProviderTransferProvisioning();
        TransferProvisioningDto consumerTransferDto = contractDto.getProvisioning().getConsumerTransferProvisioning();

        OrganizationConnectorTransferDto providerConnector = getOrgaConnector(contractDto.getDetails().getProviderId(),
                providerTransferDto.getSelectedConnectorId());
        OrganizationConnectorTransferDto consumerConnector = getOrgaConnector(contractDto.getDetails().getConsumerId(),
                consumerTransferDto.getSelectedConnectorId());

        EdcClient providerEdcClient = edcClientProvider.getObject(providerConnector);
        EdcClient consumerEdcClient = edcClientProvider.getObject(consumerConnector);

        String contractUuid = contractDto.getDetails().getId().replace("Contract:", "");
        String instanceUuid = contractUuid + "_" + UUID.randomUUID();

        String assetId = instanceUuid + "_Asset";
        String assetName = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
                + "/contract/" + contractDto.getDetails().getId();
        String assetDescription = "Asset automatically generated from MERLOT to execute contract " + contractDto.getDetails().getId();
        String policyId = instanceUuid + "_Policy";
        String contractDefinitionId = instanceUuid + "_ContractDefinition";

        // provider side
        // create asset
        AssetCreateRequest assetCreateRequest = AssetCreateRequest.builder()
                .id(assetId)
                .properties(AssetProperties.builder()
                        .name(assetName)
                        .description(assetDescription)
                        .version("")
                        .contenttype("")
                        .build())
                .dataAddress(getProviderDataAddress(providerTransferDto, providerConnector))
                .build();

        log.info("Creating Asset {} on {}", assetCreateRequest, providerConnector);
        IdResponse assetIdResponse = providerEdcClient.createAsset(assetCreateRequest);

        // create policy
        PolicyCreateRequest policyCreateRequest = PolicyCreateRequest.builder()
                .id(policyId)
                .policy(Policy.builder()
                        .id(policyId)
                        .obligation(Collections.emptyList())
                        .prohibition(Collections.emptyList())
                        .permission(Collections.emptyList())
                        .build())
                .build();
        log.info("Creating Policy {} on {}", policyCreateRequest, providerConnector);
        IdResponse policyIdResponse = providerEdcClient.createPolicy(policyCreateRequest);

        // create contract definition
        ContractDefinitionCreateRequest contractDefinitionCreateRequest = ContractDefinitionCreateRequest.builder()
                .id(contractDefinitionId)
                .contractPolicyId(policyIdResponse.getId())
                .accessPolicyId(policyIdResponse.getId())
                .assetsSelector(List.of(Criterion.builder()
                        .operandLeft("https://w3id.org/edc/v0.0.1/ns/id")
                        .operator("=")
                        .operandRight(assetId)
                        .build()))
                .build();
        log.info("Creating Contract Definition {} on {}", contractDefinitionCreateRequest, providerConnector);
        providerEdcClient.createContractDefinition(contractDefinitionCreateRequest);

        // schedule deletion of the contract definition in 5 minutes
        // note that we currently cannot delete Assets etc. once they are bound to a contract agreement
        // which appears to be irrevocable in our current EDC version.
        taskScheduler.schedule(new EdcContractDefinitionRevokeTask(providerEdcClient,
                contractDefinitionCreateRequest.getId()), Instant.now().plusSeconds(300));

        // consumer side
        // find the offering we are interested in
        CatalogRequest catalogRequest = new CatalogRequest(providerConnector.getProtocolBaseUrl());
        log.info("Query Catalog with request {} on {}", catalogRequest, consumerConnector);
        DcatCatalog catalog = consumerEdcClient.queryCatalog(catalogRequest);
        List<DcatDataset> matches =
                catalog.getDataset().stream().filter(d -> d.getAssetId().equals(assetIdResponse.getId())).toList();
        if (matches.size() != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find the asset in the provider catalog.");
        }
        DcatDataset dataset = matches.get(0);

        // negotiate offer
        NegotiationInitiateRequest negotiationInitiateRequest = NegotiationInitiateRequest.builder()
                .connectorId(catalog.getParticipantId())
                .providerId(catalog.getParticipantId())
                .consumerId(consumerConnector.getConnectorId())
                .counterPartyAddress(providerConnector.getProtocolBaseUrl())
                .offer(ContractOffer.builder()
                        .offerId(dataset.getHasPolicy().get(0).getId())
                        .assetId(dataset.getAssetId())
                        .policy(dataset.getHasPolicy().get(0))
                        .build())
                .build();
        log.info("Negotiate Offer with request {} on {}", negotiationInitiateRequest, consumerConnector);
        return consumerEdcClient.negotiateOffer(negotiationInitiateRequest);
    }

    /**
     * Given a negotiation id and a contract, return the current status of the automated EDC negotiation.
     *
     * @param negotiationId      negotiation id
     * @param contractId         contract id
     * @param activeRoleOrgaId   currently active role
     * @param authToken          user auth token
     * @return status of negotiation
     */
    public ContractNegotiation getNegotationStatus(String negotiationId, String contractId, String activeRoleOrgaId,
                                                   String authToken) {
        DataDeliveryContractDto contractDto = loadContract(contractId, activeRoleOrgaId, authToken);

        OrganizationConnectorTransferDto consumerConnector = getOrgaConnector(contractDto.getDetails().getConsumerId(),
                contractDto.getProvisioning().getConsumerTransferProvisioning().getSelectedConnectorId());

        EdcClient consumerEdcClient = edcClientProvider.getObject(consumerConnector);
        log.info("Check status of offer {} on {}", negotiationId, consumerConnector);
        return consumerEdcClient.checkOfferStatus(negotiationId);
    }

    /**
     * Given a (completed) EDC negotiation id and a contract id, start the EDC data transfer over the contract.
     *
     * @param negotiationId      negotiation id
     * @param contractId         contract id
     * @param activeRoleOrgaId   currently active role
     * @param authToken          user auth token
     * @return transfer initiation response
     */
    public IdResponse initiateConnectorTransfer(String negotiationId, String contractId, String activeRoleOrgaId,
                                                String authToken) {
        DataDeliveryContractDto contractDto = loadContract(contractId, activeRoleOrgaId, authToken);
        TransferProvisioningDto providerTransferDto = contractDto.getProvisioning().getProviderTransferProvisioning();
        TransferProvisioningDto consumerTransferDto = contractDto.getProvisioning().getConsumerTransferProvisioning();

        OrganizationConnectorTransferDto providerConnector = getOrgaConnector(contractDto.getDetails().getProviderId(),
                providerTransferDto.getSelectedConnectorId());
        OrganizationConnectorTransferDto consumerConnector = getOrgaConnector(contractDto.getDetails().getConsumerId(),
                consumerTransferDto.getSelectedConnectorId());

        EdcClient consumerEdcClient = edcClientProvider.getObject(consumerConnector);

        ContractNegotiation negotiation = getNegotationStatus(negotiationId, contractId, activeRoleOrgaId, authToken);

        // consumer side
        // create transfer request
        TransferRequest transferRequest = TransferRequest.builder()
                .connectorId(providerConnector.getConnectorId())
                .counterPartyAddress(negotiation.getCounterPartyAddress())
                .contractId(negotiation.getContractAgreementId())
                .assetId("some-asset") // this needs to be replaced once it is actually used by the EDC, for now it does not seem to matter
                .dataDestination(getConsumerDataAddress(consumerTransferDto, consumerConnector))
                .build();

        log.info("Initiate transfer with request {} on {}", transferRequest, consumerConnector);
        IdResponse transferResponse = consumerEdcClient.initiateTransfer(transferRequest);

        // schedule deprovisioning of transfer related data 5 minutes after the transfer initiation
        taskScheduler.schedule(new EdcTransferDeprovisionTask(consumerEdcClient,
                transferResponse.getId()), Instant.now().plusSeconds(300));

        return transferResponse;
    }

    /**
     * Given a transfer id and a contract, get the current status of the data transfer.
     *
     * @param transferId         transfer id
     * @param contractId         contract id
     * @param activeRoleOrgaId   currently active role
     * @param authToken          user auth token
     * @return status of transfer
     */
    public IonosS3TransferProcess getTransferStatus(String transferId, String contractId, String activeRoleOrgaId,
                                                    String authToken) {
        DataDeliveryContractDto contractDto = loadContract(contractId, activeRoleOrgaId, authToken);

        OrganizationConnectorTransferDto consumerConnector = getOrgaConnector(contractDto.getDetails().getConsumerId(),
                contractDto.getProvisioning().getConsumerTransferProvisioning().getSelectedConnectorId());

        EdcClient consumerEdcClient = edcClientProvider.getObject(consumerConnector);
        log.info("Check status of transfer {} on {}", transferId, consumerConnector);

        return consumerEdcClient.checkTransferStatus(transferId);
    }

    /**
     * Given the consumer provisioning and their connector, build an EDC Data address depending
     * on the specific provisioning type.
     *
     * @param provisioning consumer transfer provisioning from contract
     * @param connector consumer connector
     * @return consumer data address based on the provisioning
     */
    private DataAddress getConsumerDataAddress(TransferProvisioningDto provisioning,
                                               OrganizationConnectorTransferDto connector) {
        // add further transfer methods if needed
        if (provisioning instanceof IonosS3ConsumerTransferProvisioningDto provisioningDto) {
            return buildIonosS3DataDestination(provisioningDto, connector);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown transfer type selected for consumer.");
        }
    }

    /**
     * Given the provider provisioning and their connector, build an EDC Data address depending
     * on the specific provisioning type.
     *
     * @param provisioning provider transfer provisioning from contract
     * @param connector provider connector
     * @return provider data address based on the provisioning
     */
    private DataAddress getProviderDataAddress(TransferProvisioningDto provisioning,
                                               OrganizationConnectorTransferDto connector) {
        // add further transfer methods if needed
        if (provisioning instanceof IonosS3ProviderTransferProvisioningDto provisioningDto) {
            return buildIonosS3DataSource(provisioningDto, connector);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown transfer type selected for provider.");
        }
    }

    private IonosS3DataDestination buildIonosS3DataDestination(IonosS3ConsumerTransferProvisioningDto provisioning,
                                                          OrganizationConnectorTransferDto connector) {
        IonosS3BucketDto consumerSelectedBucket = connector.getIonosS3ExtensionConfig()
                .getBuckets().stream()
                .filter(b -> b.getName().equals(provisioning.getDataAddressTargetBucketName()))
                .findFirst().orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "The target bucket selected in the contract is not configured for the consumer."));
        return IonosS3DataDestination.builder()
                .bucketName(consumerSelectedBucket.getName())
                .path(provisioning.getDataAddressTargetPath())
                .keyName(provisioning.getDataAddressTargetPath())
                .storage(consumerSelectedBucket.getStorageEndpoint())
                .build();
    }

    private IonosS3DataSource buildIonosS3DataSource(IonosS3ProviderTransferProvisioningDto provisioning,
                                                        OrganizationConnectorTransferDto connector) {
        IonosS3BucketDto providerSelectedBucket = connector.getIonosS3ExtensionConfig()
                .getBuckets().stream()
                .filter(b -> b.getName().equals(provisioning.getDataAddressSourceBucketName()))
                .findFirst().orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "The source bucket selected in the contract is not configured for the provider."));

        return IonosS3DataSource.builder()
                .bucketName(providerSelectedBucket.getName())
                .blobName(provisioning.getDataAddressSourceFileName())
                .keyName(provisioning.getDataAddressSourceFileName())
                .storage(providerSelectedBucket.getStorageEndpoint())
                .build();
    }


}
