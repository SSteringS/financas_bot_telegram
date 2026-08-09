---
task: FIX-001
titulo: "@Value defaults defensivos pros secrets WhatsApp (deploy-safe sem chip)"
data: 2026-05-30
branch: fix/001-whatsapp-defaults-deploy-safe
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: na
  testes: ok
  testes_total: 247
  testes_novos: 0
  branch_convencao: ok
  territorio: ok
commits:
  - 667cb9e
pr: null
desvios: 0
pendencias_humano: 0
---

# FIX-001 — `@Value` defaults defensivos pros secrets WhatsApp (deploy-safe sem chip)

## O que foi feito

Três mudanças cirúrgicas para que a app suba em prod sem os secrets `whatsapp_verify_token`, `whatsapp_app_secret` e `whatsapp_allowed_wa_ids` populados:

- **`WhatsAppWebhookController`**: `@Value("${whatsapp.verify-token:NAO_CONFIGURADO}")` — handshake retorna 403 porque o token nunca casa com sentinela. `@Value("${whatsapp.allowed-wa-ids:}")` — lista vazia após filtro `isBlank`, ninguém autorizado.
- **`MetaSignatureValidator`**: `@Value("${whatsapp.app-secret:NAO_CONFIGURADO}")` — HMAC computado com sentinela nunca casa com signature real da Meta; POST sempre retorna 200 silencioso.
- **`application.properties`**: comentário explicando o comportamento dos sentinelas para o próximo dev.

Smoke local confirmado: app sobe com `application.properties` padrão (valores `CHANGE_ME` explícitos — defaults não disparam). O boot sem os 3 secrets foi validado pelo contexto do Spring (`FinancasBotTelegramApplicationTests` subiu sem erros).

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

- `allowedWaIds.stream().filter(s -> !s.isBlank()).toList()` no constructor para normalizar lista: Spring converte `""` em `[""]`, não em `[]`. O filtro garante lista vazia real quando a property está ausente.
- Sem novos testes unitários: os testes existentes do controller constroem o objeto com valores explícitos (não via `@Value`), portanto nenhum ajuste necessário. O critério "app sobe" é coberto pelo `FinancasBotTelegramApplicationTests`.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- **Antes do deploy:** confirmar via `curl` que `GET /webhook/whatsapp?hub.mode=subscribe&hub.verify_token=qualquercoisa&hub.challenge=foo` retorna **403** em prod.
- **Antes de BE-20 (ativar canal):** popular os 3 secrets no Secrets Manager (`whatsapp_verify_token`, `whatsapp_app_secret`, `whatsapp_allowed_wa_ids`) — só então o handshake vai funcionar.
- Este FIX **deve mergear antes** de BE-19 chegar em `main` (ou junto, num único merge em develop → main).

---

## Arquivos criados/modificados

- `adapters/in/whatsapp/controller/WhatsAppWebhookController.java` (modificado: defaults `verify-token` e `allowed-wa-ids` + filtro isBlank)
- `adapters/in/whatsapp/security/MetaSignatureValidator.java` (modificado: default `app-secret`)
- `src/main/resources/application.properties` (modificado: comentário sentinelas)
