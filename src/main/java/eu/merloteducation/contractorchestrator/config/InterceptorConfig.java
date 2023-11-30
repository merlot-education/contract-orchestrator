package eu.merloteducation.contractorchestrator.config;

import eu.merloteducation.authorizationlibrary.authorization.ActiveRoleHeaderHandlerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private ActiveRoleHeaderHandlerInterceptor activeRoleHeaderHandlerInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(activeRoleHeaderHandlerInterceptor);
    }
}
