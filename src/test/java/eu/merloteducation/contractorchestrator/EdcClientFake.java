package eu.merloteducation.contractorchestrator;

import eu.merloteducation.contractorchestrator.models.edc.asset.AssetCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.catalog.CatalogRequest;
import eu.merloteducation.contractorchestrator.models.edc.catalog.DcatCatalog;
import eu.merloteducation.contractorchestrator.models.edc.catalog.DcatDataset;
import eu.merloteducation.contractorchestrator.models.edc.common.IdResponse;
import eu.merloteducation.contractorchestrator.models.edc.contractdefinition.ContractDefinitionCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.ContractNegotiation;
import eu.merloteducation.contractorchestrator.models.edc.negotiation.NegotiationInitiateRequest;
import eu.merloteducation.contractorchestrator.models.edc.policy.Policy;
import eu.merloteducation.contractorchestrator.models.edc.policy.PolicyCreateRequest;
import eu.merloteducation.contractorchestrator.models.edc.transfer.DataRequest;
import eu.merloteducation.contractorchestrator.models.edc.transfer.IonosS3TransferProcess;
import eu.merloteducation.contractorchestrator.models.edc.transfer.TransferRequest;
import eu.merloteducation.contractorchestrator.service.EdcClient;

import java.util.List;

public class EdcClientFake implements EdcClient {

    public static final String FAKE_ID = "myId";

    public static final long FAKE_TIMESTAMP = 1234L;

    private IdResponse generateFakeIdResponse() {
        IdResponse response = new IdResponse();
        response.setId(FAKE_ID);
        response.setCreatedAt(FAKE_TIMESTAMP);
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
        DcatDataset dataset = new DcatDataset();
        dataset.setAssetId(FAKE_ID);
        dataset.setHasPolicy(List.of(Policy.builder().id("myId").build()));
        DcatCatalog catalog = new DcatCatalog();
        catalog.setDataset(List.of(dataset));
        return catalog;
    }

    @Override
    public IdResponse negotiateOffer(NegotiationInitiateRequest negotiationInitiateRequest) {
        return generateFakeIdResponse();
    }

    @Override
    public ContractNegotiation checkOfferStatus(String negotiationId) {
        ContractNegotiation negotiation = new ContractNegotiation();
        negotiation.setType("edc:ContractNegotiationDto");
        negotiation.setId(FAKE_ID);
        negotiation.setContractAgreementId(FAKE_ID + ":" + FAKE_ID + ":" + FAKE_ID);
        return negotiation;
    }

    @Override
    public IdResponse initiateTransfer(TransferRequest transferRequest) {
        return generateFakeIdResponse();
    }

    @Override
    public IonosS3TransferProcess checkTransferStatus(String transferId) {
        DataRequest request = new DataRequest();
        request.setAssetId(FAKE_ID);
        request.setType("edc:DataRequestDto");
        IonosS3TransferProcess process = new IonosS3TransferProcess();
        process.setId(FAKE_ID);
        process.setType("edc:TransferProcessDto");
        process.setDataRequest(request);
        return process;
    }

    @Override
    public void deprovisionTransfer(String transferId) {
    }

    @Override
    public void revokeContractDefinition(String contractDefinitionId) {
    }
}
