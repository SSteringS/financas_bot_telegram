package br.com.satyan.stering.saita.financasbottelegram.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class CorsConfigTest {

    private final WebMvcConfig config = new WebMvcConfig(
            new RequisitanteIdArgumentResolver(), "http://localhost:5173");

    @Test
    void addCorsMappingsNaoDeveLancarExcecao() {
        assertThatNoException().isThrownBy(() -> config.addCorsMappings(new CorsRegistry()));
    }

    @Test
    void addArgumentResolversDeveRegistrarResolver() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
        config.addArgumentResolvers(resolvers);
        assertThat(resolvers).hasSize(1);
        assertThat(resolvers.get(0)).isInstanceOf(RequisitanteIdArgumentResolver.class);
    }

    @Test
    void devePossuirOriginConfigurado() {
        WebMvcConfig cfgComOrigin = new WebMvcConfig(
                new RequisitanteIdArgumentResolver(), "https://finbot.satyan.com.br");
        assertThatNoException().isThrownBy(() -> cfgComOrigin.addCorsMappings(new CorsRegistry()));
    }
}
