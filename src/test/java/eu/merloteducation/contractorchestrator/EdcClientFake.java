package eu.merloteducation.contractorchestrator;

import eu.merloteducation.contractorchestrator.models.edc.asset.AssetCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.catalog.CatalogRequest;
import eu.merloteducation.contractorchestrator.models.edc.catalog.DcatCatalog;
import eu.merloteducation.contractorchestrator.models.edc.common.IdResponse;
import eu.merloteducation.contractorchestrator.models.edc.contractdefinition.ContractDefinitionCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractNegotiation;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.NegotiationInitiateRequest;
import eu.merloteducation.contractorchestrator.models.edc.policy.PolicyCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.transfer.IonosS3TransferProcess;
import eu.merloteducation.contractorchestrator.models.edc.transfer.TransferRequest;
import eu.merloteducation.contractorchestrator.service.EdcClient;

public class EdcClientFake implements EdcClient {

    private IdResponse generateFakeIdResponse() {
        IdResponse response = new IdResponse();
        response.setId("myId");
        response.setCreatedAt(1234L);
        response.setType("edc:IdResponseDto");
        return response;
    }
    @Override
    public IdResponse createAsset(AssetCreateRequest assetCreateRequest) {
        return generateFakeIdResponse();
    }

    @Override
    public IdResponse createPolicy(PolicyCreateRequest policyCreateRequest) {
        return generateFakeIdResponse();
    }

    @Override
    public IdResponse createContractDefinition(ContractDefinitionCreateRequest contractDefinitionCreateRequest) {
        return generateFakeIdResponse();
    }

    @Override
    public DcatCatalog queryCatalog(CatalogRequest catalogRequest) {
        return new DcatCatalog();
    }

    @Override
    public IdResponse negotiateOffer(NegotiationInitiateRequest negotiationInitiateRequest) {
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
    public IdResponse initiateTransfer(TransferRequest transferRequest) {
        return generateFakeIdResponse();
    }

    @Override
    public IonosS3TransferProcess checkTransferStatus(String transferId) {
        return new IonosS3TransferProcess();
    }
}
