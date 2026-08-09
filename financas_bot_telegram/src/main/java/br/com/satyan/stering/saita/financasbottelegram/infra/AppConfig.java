package br.com.satyan.stering.saita.financasbottelegram.infra;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

  // Singleton construído a partir do RestClient.Builder auto-configurado — mantém compatibilidade
  // com @RestClientTest; padrão da spec §5.3 (docs/architecture/adapter-whatsapp-cloud-api.md).
  @Bean
  public RestClient restClient(RestClient.Builder builder) {
    return builder.build();
  }

  @Bean
  public Clock systemClock() {
    return Clock.systemDefaultZone();
  }
}
