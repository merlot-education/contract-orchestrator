package eu.merloteducation.contractorchestrator.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ActiveRoleHeaderHandlerInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthorityChecker authorityChecker;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NotNull HttpServletResponse response,
                             @NotNull Object handler) {
        if (request.getHeader("Active-Role") == null) {
            return true;
        }
        OrganizationRoleGrantedAuthority activeRole =
                new OrganizationRoleGrantedAuthority(request.getHeader("Active-Role"));
        return authorityChecker.representsOrganization(SecurityContextHolder.getContext().getAuthentication(),
                activeRole.getOrganizationId());
    }
}
