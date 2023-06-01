package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.models.entities.Contract;
import eu.merloteducation.contractorchestrator.models.ContractCreateRequest;
import eu.merloteducation.contractorchestrator.repositories.IContractRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
public class ContractStorageService {

    @Autowired
    private IContractRepository contractRepository;

    @Transactional
    public Contract addContractTemplate(ContractCreateRequest contractCreateRequest, String authToken) {

        // initialize contract fields
        Contract contract = new Contract();
        contract.setId("Contract:" + UUID.randomUUID());
        contract.setCreationDate(OffsetDateTime.now());

        // extract data from request
        contract.setOfferingId(contractCreateRequest.getOfferingId());
        contract.setConsumerId(contractCreateRequest.getConsumerId());

        // TODO request details of service offering from service offering orchestrator, use token provided by user accessing this function
        contract.setOfferingName("TODO");
        contract.setProviderId("TODO");
        contract.setOfferingAttachments(new ArrayList<>());

        // TODO request details of provider of service offering from organizations orchestrator, use token provided by user accessing this function
        contract.setProviderTncUrl("TODO");
        contract.setProviderTncHash("TODO");

        contractRepository.save(contract);

        return contract;
    }
}
