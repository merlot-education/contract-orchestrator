package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.models.edc.asset.Asset;
import eu.merloteducation.contractorchestrator.models.edc.asset.DataAddress;
import eu.merloteducation.contractorchestrator.models.edc.catalog.DcatCatalog;
import eu.merloteducation.contractorchestrator.models.edc.common.IdResponse;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractNegotiation;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractOffer;
import eu.merloteducation.contractorchestrator.models.edc.policy.Policy;
import eu.merloteducation.contractorchestrator.models.edc.transfer.IonosS3TransferProcess;

public interface IEdcClient {
    IdResponse createAsset(Asset asset, DataAddress dataAddress);

    IdResponse createPolicyUnrestricted(Policy policy);

    IdResponse createContractDefinition(String contractDefinitionId, String accessPolicyId, String contractPolicyid,
                                        String assetId);

    DcatCatalog queryCatalog(String providerProtocolUrl);

    IdResponse negotiateOffer(String connectorId, String providerId, String connectorAddress,
                              ContractOffer offer);

    ContractNegotiation checkOfferStatus(String negotiationId);

    IdResponse initiateTransfer(String connectorId, String connectorAddress, String agreementId, String assetId,
                                DataAddress dataDestination);

    IonosS3TransferProcess checkTransferStatus(String transferId);

    eu.merloteducation.contractorchestrator.models.organisationsorchestrator.OrganisationConnectorExtension getConnector();
}
