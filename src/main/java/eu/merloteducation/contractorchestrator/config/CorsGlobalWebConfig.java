package eu.merloteducation.contractorchestrator.config;

import eu.merloteducation.authorizationlibrary.authorization.ActiveRoleHeaderHandlerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsGlobalWebConfig implements WebMvcConfigurer {
    @Value("${cors.global.origins}")
    private String[] corsGlobalOrigins;
    @Value("${cors.global.patterns}")
    private String[] corsGlobalPatterns;

    @Autowired
    private ActiveRoleHeaderHandlerInterceptor activeRoleHeaderHandlerInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(corsGlobalOrigins)
                .allowedOriginPatterns(corsGlobalPatterns)
                .allowedMethods("*");
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(activeRoleHeaderHandlerInterceptor);
    }
}