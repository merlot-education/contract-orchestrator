package eu.merloteducation.contractorchestrator.auth;

import eu.merloteducation.authorizationlibrary.authorization.AuthorityChecker;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component("contractAuthorityChecker")
public class ContractAuthorityChecker {

    @Autowired
    private ContractTemplateRepository contractTemplateRepository;

    @Autowired
    private AuthorityChecker authorityChecker;

    private boolean canAccessContract(Authentication authentication, ContractTemplate template) {
        return this.isContractConsumer(authentication, template) || this.isContractProvider(authentication, template);
    }

    private boolean isContractProvider(Authentication authentication, ContractTemplate template) {
        Set<String> representedOrgaIds = authorityChecker.getRepresentedOrgaIds(authentication);
        if (template != null) {
            String providerId = template.getProviderId();
            return representedOrgaIds.contains(providerId);
        }
        return false;
    }

    private boolean isContractConsumer(Authentication authentication, ContractTemplate template) {
        Set<String> representedOrgaIds = authorityChecker.getRepresentedOrgaIds(authentication);
        if (template != null) {
            String consumerId = template.getConsumerId();
            return representedOrgaIds.contains(consumerId);
        }
        return false;
    }


    /**
     * Given the current authentication and a contract id, check whether the requesting party either
     * represents the consumer or provider of this contract.
     *
     * @param authentication current authentication
     * @param contractId     id of the contract to request
     * @return can access the requested contract
     */
    public boolean canAccessContract(Authentication authentication, String contractId) {
        ContractTemplate template = contractTemplateRepository.findById(contractId).orElse(null);
        return this.canAccessContract(authentication, template);
    }

    public boolean isContractProvider(Authentication authentication, String contractId) {
        ContractTemplate template = contractTemplateRepository.findById(contractId).orElse(null);
        return this.isContractProvider(authentication, template);
    }
}
