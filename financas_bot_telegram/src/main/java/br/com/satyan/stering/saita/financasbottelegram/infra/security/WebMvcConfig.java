package br.com.satyan.stering.saita.financasbottelegram.infra.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequisitanteIdArgumentResolver requisitanteIdResolver;
    private final String corsAllowedOrigin;

    public WebMvcConfig(
            RequisitanteIdArgumentResolver requisitanteIdResolver,
            @Value("${app.cors.allowed-origin}") String corsAllowedOrigin) {
        this.requisitanteIdResolver = requisitanteIdResolver;
        this.corsAllowedOrigin = corsAllowedOrigin;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(requisitanteIdResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
                .allowedOrigins(corsAllowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization", "X-Requested-With")
                .exposedHeaders("Set-Cookie")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
