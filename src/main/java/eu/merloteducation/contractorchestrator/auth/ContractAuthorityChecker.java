package eu.merloteducation.contractorchestrator.auth;

import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.repositories.ContractTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component("contractAuthorityChecker")
public class ContractAuthorityChecker {

    private static final String PARTICIPANT = "Participant:";

    @Autowired
    private ContractTemplateRepository contractTemplateRepository;

    @Autowired
    private AuthorityChecker authorityChecker;

    public boolean canAccessContract(Authentication authentication, String contractId) {
        ContractTemplate template = contractTemplateRepository.findById(contractId).orElse(null);
        Set<String> representedOrgaIds = authorityChecker.getRepresentedOrgaIds(authentication);
        if (template != null) {
            String consumerId = template.getConsumerId().replace(PARTICIPANT, "");
            String providerId = template.getProviderId().replace(PARTICIPANT, "");
            return (representedOrgaIds.contains(consumerId) || representedOrgaIds.contains(providerId));
        }
        return false;
    }
}
