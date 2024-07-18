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

package eu.merloteducation.contractorchestrator;

import eu.merloteducation.contractorchestrator.service.EdcClient;
import eu.merloteducation.modelslib.edc.asset.AssetCreateRequest;
import eu.merloteducation.modelslib.edc.catalog.CatalogRequest;
import eu.merloteducation.modelslib.edc.catalog.DcatCatalog;
import eu.merloteducation.modelslib.edc.catalog.DcatDataset;
import eu.merloteducation.modelslib.edc.common.IdResponse;
import eu.merloteducation.modelslib.edc.contractdefinition.ContractDefinitionCreateRequest;
import eu.merloteducation.modelslib.edc.negotiation.ContractNegotiation;
import eu.merloteducation.modelslib.edc.negotiation.NegotiationInitiateRequest;
import eu.merloteducation.modelslib.edc.policy.Policy;
import eu.merloteducation.modelslib.edc.policy.PolicyCreateRequest;
import eu.merloteducation.modelslib.edc.transfer.DataRequest;
import eu.merloteducation.modelslib.edc.transfer.IonosS3TransferProcess;
import eu.merloteducation.modelslib.edc.transfer.TransferRequest;

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
