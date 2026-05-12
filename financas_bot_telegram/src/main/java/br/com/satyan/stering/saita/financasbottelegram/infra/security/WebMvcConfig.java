package br.com.satyan.stering.saita.financasbottelegram.infra.security;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequisitanteIdArgumentResolver requisitanteIdResolver;

    public WebMvcConfig(RequisitanteIdArgumentResolver requisitanteIdResolver) {
        this.requisitanteIdResolver = requisitanteIdResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(requisitanteIdResolver);
    }
}
