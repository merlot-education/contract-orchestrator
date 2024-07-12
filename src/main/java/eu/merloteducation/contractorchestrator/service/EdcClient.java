/*
 *  Copyright 2023-2024 Dataport AöR
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

import eu.merloteducation.modelslib.edc.asset.AssetCreateRequest;
import eu.merloteducation.modelslib.edc.catalog.CatalogRequest;
import eu.merloteducation.modelslib.edc.catalog.DcatCatalog;
import eu.merloteducation.modelslib.edc.common.IdResponse;
import eu.merloteducation.modelslib.edc.contractdefinition.ContractDefinitionCreateRequest;
import eu.merloteducation.modelslib.edc.negotiation.ContractNegotiation;
import eu.merloteducation.modelslib.edc.negotiation.NegotiationInitiateRequest;
import eu.merloteducation.modelslib.edc.policy.PolicyCreateRequest;
import eu.merloteducation.modelslib.edc.transfer.IonosS3TransferProcess;
import eu.merloteducation.modelslib.edc.transfer.TransferRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;


public interface EdcClient {
    @PostExchange("/v3/assets")
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
