---
task: FIX-padronizar-restclient-builder
titulo: "Padronizar criação do RestClient Telegram via Builder (spec §5.3)"
data: 2026-05-29
branch: fix/padronizar-restclient-builder
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: na
  testes: ok
  testes_total: 248
  testes_novos: 0
  branch_convencao: ok
  territorio: ok
commits:
  - cd48a59
pr: null
desvios: 0
pendencias_humano: 0
---

# FIX — Padronizar criação do RestClient Telegram via Builder (spec §5.3)

## O que foi feito

Único arquivo modificado: `infra/AppConfig.java`.

Antes:
```java
@Bean
public RestClient restClient() {
    return RestClient.create();
}
```

Depois:
```java
// Singleton construído a partir do RestClient.Builder auto-configurado — mantém compatibilidade
// com @RestClientTest; padrão da spec §5.3 (docs/architecture/adapter-whatsapp-cloud-api.md).
@Bean
public RestClient restClient(RestClient.Builder builder) {
    return builder.build();
}
```

O `RestClient.Builder` é auto-configurado pelo Spring Boot (`RestClientAutoConfiguration`). Como os services Telegram (`TelegramMessageSenderService`, `TelegramFileDownloaderService`) montam URLs absolutas manualmente, nenhuma base URL é necessária no bean — `builder.build()` é suficiente.

## Testes Telegram a migrar

Confirmado: **não há testes** para `TelegramMessageSenderService` nem `TelegramFileDownloaderService`. Nenhuma migração necessária.

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

Nenhuma decisão não-óbvia. Mudança mecânica conforme a spec.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

Os dois services Telegram não têm testes. Quando houver necessidade de testar o comportamento HTTP do Telegram sender/downloader, o padrão agora viabiliza `@RestClientTest` + `MockRestServiceServer` (mesma abordagem do BE-18).

---

## Arquivos criados/modificados

- `infra/AppConfig.java` (modificado: `restClient()` via Builder + comentário de convenção)
