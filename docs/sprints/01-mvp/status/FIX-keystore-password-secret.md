# FIX — Mover `keystore_password` pro Secrets Manager

**Data:** 2026-05-13
**Branch:** feature/backend-polish-evo07
**Commit/PR:** (ver commit deste arquivo)
**Responsável (instância):** Claude Code (CLI)

---

## O que foi feito

- Substituída a linha `server.ssl.key-store-password=finbot123` por `server.ssl.key-store-password=${keystore_password}` em `application-prod.properties`.
- Nenhuma outra mudança de código necessária — o `spring.config.import=aws-secretsmanager:finbot-prod-secrets` já existente injeta o valor automaticamente em runtime.

---

## ⚠️ AÇÃO MANUAL OBRIGATÓRIA ANTES DO PRÓXIMO DEPLOY EM PROD

> **Antes de qualquer deploy em produção:** adicionar a chave `keystore_password` com valor `finbot123` (senha atual do keystore p12 em `/opt/finbot/keystore.p12`) no segredo `finbot-prod-secrets` no AWS Secrets Manager (console AWS → Secrets Manager → `finbot-prod-secrets` → "Retrieve secret value" → "Edit").
>
> **Sem essa chave, o app falha ao subir com erro de placeholder não resolvido.**
>
> Sugestão: aproveitar a oportunidade para trocar o valor `finbot123` por algo aleatório mais forte (`openssl rand -base64 24`). Isso exige reasinar o keystore PKCS12 com a nova senha, o que pode ser feito antes ou junto da migração para Let's Encrypt + domínio real.

---

## Desvios do plano

Nenhum — mudança exatamente como especificada no plano.

---

## Decisões tomadas durante a execução

Nenhuma. Mudança trivial: 1 linha no properties.

---

## Decisões pendentes (esperando humano)

- Adicionar `keystore_password` no AWS Secrets Manager antes do próximo deploy (ver aviso acima).
- Decidir se rotaciona a senha do keystore agora (oportunidade) ou mantém `finbot123` até migração Let's Encrypt.

---

## Próximos passos / observações pro próximo

Quando migrar para Let's Encrypt + domínio real, este item (`keystore_password`) fica obsoleto — não haverá mais keystore p12 manual.

---

## Arquivos criados/modificados

- `financas_bot_telegram/src/main/resources/application-prod.properties` (modificado: 1 linha)
- `docs/status/FIX-keystore-password-secret.md` (novo)
