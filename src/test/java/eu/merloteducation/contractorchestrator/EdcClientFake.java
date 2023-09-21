package eu.merloteducation.contractorchestrator;

import eu.merloteducation.contractorchestrator.models.edc.asset.Asset;
import eu.merloteducation.contractorchestrator.models.edc.asset.DataAddress;
import eu.merloteducation.contractorchestrator.models.edc.catalog.DcatCatalog;
import eu.merloteducation.contractorchestrator.models.edc.common.IdResponse;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractNegotiation;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractOffer;
import eu.merloteducation.contractorchestrator.models.edc.policy.Policy;
import eu.merloteducation.contractorchestrator.models.edc.transfer.IonosS3TransferProcess;
import eu.merloteducation.contractorchestrator.models.organisationsorchestrator.OrganisationConnectorExtension;
import eu.merloteducation.contractorchestrator.service.IEdcClient;

public class EdcClientFake implements IEdcClient  {

    private IdResponse generateFakeIdResponse() {
        IdResponse response = new IdResponse();
        response.setId("myId");
        response.setCreatedAt(1234L);
        response.setType("edc:IdResponseDto");
        return response;
    }
    @Override
    public IdResponse createAsset(Asset asset, DataAddress dataAddress) {
        return generateFakeIdResponse();
    }

    @Override
    public IdResponse createPolicyUnrestricted(Policy policy) {
        return generateFakeIdResponse();
    }

    @Override
    public IdResponse createContractDefinition(String contractDefinitionId, String accessPolicyId, String contractPolicyid, String assetId) {
        return generateFakeIdResponse();
    }

    @Override
    public DcatCatalog queryCatalog(String providerProtocolUrl) {
        return new DcatCatalog();
    }

    @Override
    public IdResponse negotiateOffer(String connectorId, String providerId, String connectorAddress, ContractOffer offer) {
        return generateFakeIdResponse();
    }

    @Override
    public ContractNegotiation checkOfferStatus(String negotiationId) {
        ContractNegotiation negotiation = new ContractNegotiation();
        negotiation.setType("edc:ContractNegotiationDto");
        negotiation.setId("myId");
        return negotiation;
    }

    @Override
    public IdResponse initiateTransfer(String connectorId, String connectorAddress, String agreementId, String assetId, DataAddress dataDestination) {
        return generateFakeIdResponse();
    }

    @Override
    public IonosS3TransferProcess checkTransferStatus(String transferId) {
        return new IonosS3TransferProcess();
    }

    @Override
    public OrganisationConnectorExtension getConnector() {
        return new OrganisationConnectorExtension();
    }
}
