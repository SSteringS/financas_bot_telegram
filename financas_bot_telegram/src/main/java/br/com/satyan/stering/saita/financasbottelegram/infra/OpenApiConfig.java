package br.com.satyan.stering.saita.financasbottelegram.infra;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finbot API")
                        .version("v1")
                        .description("API REST do bot de finanças — consulta de pedidos e comprovantes."));
    }
}
