package eu.merloteducation.contractorchestrator.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component("authorityChecker")
public class AuthorityChecker {

    protected Set<String> getRepresentedOrgaIds(Authentication authentication) {
        Set<String> representedOrgaIds = new HashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority instanceof OrganizationRoleGrantedAuthority orgaRoleAuthority) {
                representedOrgaIds.add(orgaRoleAuthority.getOrganizationId());
            }
        }
        return representedOrgaIds;
    }

    /**
     * Checks whether the user with the currently active authorization represents a given organization by the id.
     *
     * @param authentication current authentication
     * @param orgaId         id of the organization to request
     * @return user represents given organization
     */
    public boolean representsOrganization(Authentication authentication, String orgaId) {
        String numOrgaId = orgaId.replace("Participant:", "");
        Set<String> representedOrgaIds = getRepresentedOrgaIds(authentication);
        return representedOrgaIds.contains(numOrgaId);
    }
}
