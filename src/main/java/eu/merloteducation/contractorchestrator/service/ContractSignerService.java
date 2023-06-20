package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import org.springframework.stereotype.Service;

@Service
public class ContractSignerService {

    /**
     * Given a contract object and a signing key, generate a signature for this contract.
     * TODO this method needs to be extended later to actually sign, currently it just passes through the input value.
     *
     * @param template   contract object to sign
     * @param signingKey key to use to sign
     */
    public String generateContractSignature(ContractTemplate template, String signingKey) {
        return signingKey;
    }
}
