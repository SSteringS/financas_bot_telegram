package br.com.satyan.stering.saita.financasbottelegram.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

  @Bean
  public RestClient restClient() {
    return RestClient.create();
  }
}
