package eu.merloteducation.contractorchestrator.auth;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ActiveRoleFromHeaderConverter implements Converter<String, OrganizationRoleGrantedAuthority> {
    @Override
    public OrganizationRoleGrantedAuthority convert(@NotNull String activeRoleString) {

        return new OrganizationRoleGrantedAuthority(activeRoleString);
    }
}
