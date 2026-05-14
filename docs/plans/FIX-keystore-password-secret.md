# FIX — Mover `keystore_password` pro Secrets Manager

> Hoje `server.ssl.key-store-password=finbot123` está hardcoded em `application-prod.properties`. Mover pra Secrets Manager pra reduzir surface de exposição.

---

## Pré-requisitos

- Acesso ao AWS Secrets Manager pra adicionar chave `keystore_password` em `finbot-prod-secrets` (passo manual do humano antes do próximo deploy)

---

## Arquivo modificado

**`financas_bot_telegram/src/main/resources/application-prod.properties`**

### Mudança

Trocar a linha:

```properties
server.ssl.key-store-password=finbot123
```

Por:

```properties
server.ssl.key-store-password=${keystore_password}
```

Mais nada precisa mudar no código — o Spring Cloud AWS já injeta valores do `finbot-prod-secrets` em placeholders `${...}` por causa do `spring.config.import=aws-secretsmanager:finbot-prod-secrets` já existente.

---

## Pendência operacional registrada no status

O status report DEVE conter um aviso explícito:

> **Antes do próximo deploy em prod:** adicionar chave `keystore_password` com valor `finbot123` (o valor atual do keystore p12) ao segredo `finbot-prod-secrets` no AWS Secrets Manager. Sem essa chave, o app falha ao subir com erro de placeholder.

Isso é tarefa manual do humano via console AWS. O implementador não tem acesso pra fazer.

---

## Critério de aceitação

- [ ] `server.ssl.key-store-password=${keystore_password}` no `application-prod.properties`
- [ ] Status report alerta sobre adicionar a chave no Secrets Manager antes do deploy
- [ ] `./mvnw clean test` continua verde
- [ ] **Não testar localmente com profile prod** (depende do Secrets Manager real; vai falhar)

---

## Status report

`docs/status/FIX-keystore-password-secret.md`. Cobrir:
- Confirmação da mudança no properties
- **Alerta destacado** sobre a ação manual do humano no Secrets Manager
- Sugestão pro humano: trocar o valor atual `finbot123` por algo random (`openssl rand -base64 24`) já que vai ser oportunidade de rotacionar a senha do keystore. Mas isso obriga regerar/reassinar o keystore. Pode ser feito junto, ou ficar pra evolução de Let's Encrypt depois.

Atualizar `docs/PENDENCIAS-TECNICAS.md` movendo este item pra "Itens resolvidos".
