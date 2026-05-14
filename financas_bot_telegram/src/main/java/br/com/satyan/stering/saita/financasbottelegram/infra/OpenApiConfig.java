package br.com.satyan.stering.saita.financasbottelegram.infra;

import br.com.satyan.stering.saita.financasbottelegram.infra.security.RequisitanteId;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(RequisitanteId.class);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finbot API")
                        .version("v1")
                        .description("API REST do bot de finanças — consulta de pedidos e comprovantes."))
                .components(new Components()
                        .addSecuritySchemes("AdminApiKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Admin-Key")));
    }
}
