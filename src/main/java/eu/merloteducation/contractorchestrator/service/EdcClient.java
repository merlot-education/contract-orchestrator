package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.models.edc.asset.Asset;
import eu.merloteducation.contractorchestrator.models.edc.asset.AssetCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.asset.DataAddress;
import eu.merloteducation.contractorchestrator.models.edc.catalog.CatalogRequest;
import eu.merloteducation.contractorchestrator.models.edc.catalog.DcatCatalog;
import eu.merloteducation.contractorchestrator.models.edc.common.IdResponse;
import eu.merloteducation.contractorchestrator.models.edc.contractdefinition.ContractDefinitionCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.contractdefinition.Criterion;
import eu.merloteducation.contractorchestrator.models.edc.dataplane.DataPlaneCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractNegotiation;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractOffer;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.NegotiationInitiateRequest;
import eu.merloteducation.contractorchestrator.models.edc.policy.Policy;
import eu.merloteducation.contractorchestrator.models.edc.policy.PolicyCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.transfer.IonosS3TransferProcess;
import eu.merloteducation.contractorchestrator.models.edc.transfer.TransferRequest;
import eu.merloteducation.contractorchestrator.models.organisationsorchestrator.OrganisationConnectorExtension;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EdcClient implements IEdcClient {

    private final Logger logger = LoggerFactory.getLogger(EdcClient.class);

    @Getter
    private final OrganisationConnectorExtension connector;
    private final WebClient webClient;

    public EdcClient(OrganisationConnectorExtension connector, WebClient.Builder webClientBuilder) {
        this.connector = connector;
        this.webClient = webClientBuilder
                .baseUrl(connector.getManagementBaseUrl())
                .defaultHeader("X-API-Key", this.connector.getConnectorAccessToken())
                .build();
    }

    @Override
    public IdResponse createAsset(Asset asset, DataAddress dataAddress) {
        logger.debug("Create Asset on {}", this.connector.getManagementBaseUrl());
        AssetCreateRequest assetCreateRequest = new AssetCreateRequest();
        assetCreateRequest.setAsset(asset);
        assetCreateRequest.setDataAddress(dataAddress);

        return webClient
                .post()
                .uri("/v2/assets")
                .bodyValue(assetCreateRequest)
                .retrieve()
                .bodyToMono(IdResponse.class)
                .block();
    }

    @Override
    public IdResponse createPolicyUnrestricted(Policy policy) {
        logger.debug("Create Policy on {}", this.connector.getManagementBaseUrl());
        PolicyCreateRequest policyCreateRequest = new PolicyCreateRequest();
        policyCreateRequest.setPolicy(policy);
        policyCreateRequest.setId(policy.getId());

        return webClient
                .post()
                .uri("/v2/policydefinitions")
                .bodyValue(policy)
                .retrieve()
                .bodyToMono(IdResponse.class)
                .block();
    }

    @Override
    public IdResponse createContractDefinition(String contractDefinitionId, String accessPolicyId, String contractPolicyid,
                                               String assetId) {
        logger.debug("Create Contract Definition on {}", this.connector.getManagementBaseUrl());
        ContractDefinitionCreateRequest createRequest = new ContractDefinitionCreateRequest();
        createRequest.setId(contractDefinitionId);
        createRequest.setAccessPolicyId(accessPolicyId);
        createRequest.setContractPolicyId(contractPolicyid);
        List<Criterion> assetSelector = new ArrayList<>();
        Criterion assetCriterion = new Criterion();
        assetCriterion.setOperator("=");
        assetCriterion.setOperandLeft("https://w3id.org/edc/v0.0.1/ns/id");
        assetCriterion.setOperandRight(assetId);
        assetSelector.add(assetCriterion);
        createRequest.setAssetsSelector(assetSelector);

        return webClient
                .post()
                .uri("/v2/contractdefinitions")
                .bodyValue(createRequest)
                .retrieve()
                .bodyToMono(IdResponse.class)
                .block();
    }

    @Override
    public DcatCatalog queryCatalog(String providerProtocolUrl) {
        logger.debug("Query Catalog on {} with provider url {}", this.connector.getManagementBaseUrl(), providerProtocolUrl);
        CatalogRequest catalogRequest = new CatalogRequest();
        catalogRequest.setProviderUrl(providerProtocolUrl);

        return webClient
                .post()
                .uri("/v2/catalog/request")
                .bodyValue(catalogRequest)
                .retrieve()
                .bodyToMono(DcatCatalog.class)
                .block();
    }

    @Override
    public IdResponse negotiateOffer(String connectorId, String providerId, String connectorAddress,
                                     ContractOffer offer) {
        logger.debug("Negotiate offer on {}", this.connector.getManagementBaseUrl());
        NegotiationInitiateRequest initiateRequest = new NegotiationInitiateRequest();
        initiateRequest.setConnectorId(connectorId);
        initiateRequest.setConsumerId(this.connector.getConnectorId());
        initiateRequest.setProviderId(providerId);
        initiateRequest.setConnectorAddress(connectorAddress);
        initiateRequest.setOffer(offer);

        return webClient
                .post()
                .uri("/v2/contractnegotiations")
                .bodyValue(initiateRequest)
                .retrieve()
                .bodyToMono(IdResponse.class)
                .block();
    }

    @Override
    public ContractNegotiation checkOfferStatus(String negotiationId) {
        logger.debug("Requesting offer status on {} with id {}", this.connector.getManagementBaseUrl(), negotiationId);
        return webClient
                .get()
                .uri("/v2/contractnegotiations/" + negotiationId)
                .retrieve()
                .bodyToMono(ContractNegotiation.class)
                .block();
    }

    @Override
    public IdResponse initiateTransfer(String connectorId, String connectorAddress, String agreementId, String assetId,
                                       DataAddress dataDestination) {
        logger.debug("Initiating transfer on {}", this.connector.getManagementBaseUrl());

        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setConnectorId(connectorId);
        transferRequest.setConnectorAddress(connectorAddress);
        transferRequest.setContractId(agreementId);
        transferRequest.setAssetId(assetId);
        transferRequest.setDataDestination(dataDestination);

        return webClient
                .post()
                .uri("/v2/transferprocesses")
                .bodyValue(transferRequest)
                .retrieve()
                .bodyToMono(IdResponse.class)
                .block();
    }

    @Override
    public IonosS3TransferProcess checkTransferStatus(String transferId) {
        logger.debug("Requesting transfer status on {} with id {}", this.connector.getManagementBaseUrl(), transferId);
        return webClient
                .get()
                .uri("/v2/transferprocesses/" + transferId)
                .retrieve()
                .bodyToMono(IonosS3TransferProcess.class)
                .block();
    }
}
