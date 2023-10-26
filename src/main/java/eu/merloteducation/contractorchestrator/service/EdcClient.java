package eu.merloteducation.contractorchestrator.service;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;


public interface EdcClient {
    @PostExchange("/v2/assets")
    IdResponse createAsset(@RequestBody AssetCreateRequest assetCreateRequest);

    @PostExchange("/v2/policydefinitions")
    IdResponse createPolicy(@RequestBody PolicyCreateRequest policyCreateRequest);

    @PostExchange("/v2/contractdefinitions")
    IdResponse createContractDefinition(@RequestBody ContractDefinitionCreateRequest contractDefinitionCreateRequest);

    @PostExchange("/v2/catalog/request")
    DcatCatalog queryCatalog(@RequestBody CatalogRequest catalogRequest);

    @PostExchange("/v2/contractnegotiations")
    IdResponse negotiateOffer(@RequestBody NegotiationInitiateRequest negotiationInitiateRequest);

    @GetExchange("/v2/contractnegotiations/{negotiationId}")
    ContractNegotiation checkOfferStatus(@PathVariable String negotiationId);

    @PostExchange("/v2/transferprocesses")
    IdResponse initiateTransfer(@RequestBody TransferRequest transferRequest);

    @GetExchange("/v2/transferprocesses/{transferId}")
    IonosS3TransferProcess checkTransferStatus(@PathVariable String transferId);

    @PostExchange("/v2/transferprocesses/{transferId}/deprovision")
    void deprovisionTransfer(@PathVariable String transferId);

    @DeleteExchange("/v2/contractdefinitions/{contractDefinitionId}")
    void revokeContractDefinition(@PathVariable String contractDefinitionId);
}
