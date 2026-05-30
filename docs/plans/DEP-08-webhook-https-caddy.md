# DEP-08 — Webhook do Telegram via Caddy/Let's Encrypt + aposentar o keystore

> **Intake (contrato de entrada da task)**
>
> - **Origem:** débito em `docs/PENDENCIAS-TECNICAS.md` ("cert self-signed → Let's Encrypt") + follow-up do DEP-07 (o keystore é o ponto chato de reproduzir). O webhook já quebrou em prod quando alguém rodou `setWebhook` sem reanexar o cert self-signed.
> - **Prioridade:** média-alta — remove uma fonte recorrente de quebra do canal de entrada do bot e destrava a reprodutibilidade do DEP-07.
> - **Esforço:** Fase 1 baixo; Fase 2 médio (mexe na config de TLS da app).
> - **Território / quem executa:** `infra/` (DNS, SG) + Caddy na EC2 (SSH) + `application-prod.properties` (Fase 2) → **Claude do back** + humano (SSH/`setWebhook`/apply).
> - **Branch:** `feature/dep-08-webhook-https-caddy`, a partir de `develop`.
> - **Dependências:** DEP-03 (Caddy no ar) ✅. **Fase 2 depende de um ADR** (topologia de TLS — ver abaixo).
> - **Riscos:**
>   1. **Re-apontar o `setWebhook` errado → bot para de receber updates.** Mitigação: validar com `getWebhookInfo`, mandar mensagem de teste, e ter o comando antigo à mão pra rollback.
>   2. **Fase 2 mal-feita derruba API + webhook juntos** (app deixa de fazer TLS e o Caddy não está apontando certo). Mitigação: só fazer a Fase 2 depois da Fase 1 estável; testar os dois hosts.

---

## Contexto

Hoje a app Spring escuta em **8443 com cert self-signed** (`/opt/finbot/keystore.p12`) e serve **dois** tráfegos: o webhook do Telegram (`POST /webhook`) e a API REST (`/api/v1/**`). O Telegram tolera self-signed via `setWebhook` com `certificate=@cert.pem` — mas isso "desregistra" quando se roda `setWebhook` sem o cert (já causou incidente). O DEP-03 colocou o Caddy na frente da **API** (`api.satyansaita.com`), mas o **webhook** continua batendo direto na 8443 self-signed.

Objetivo: o webhook passar a usar um cert **Let's Encrypt** via Caddy (`bot.satyansaita.com`), e — na Fase 2 — **aposentar o self-signed/keystore** de vez, deixando o Caddy como **único terminador de TLS** da EC2.

## Decisão / abordagem — duas fases

### Fase 1 — `bot.satyansaita.com` via Caddy/LE + re-apontar o webhook (baixo risco, reversível)
- DNS: A record `bot.satyansaita.com` → EIP (igual ao `api.` do DEP-03).
- Caddy: adicionar um site `bot.satyansaita.com { reverse_proxy https://localhost:8443 { transport http { tls_insecure_skip_verify } } }` (mesmo padrão do `api.`). Caddy obtém o cert LE automaticamente.
- Re-registrar o webhook (operacional, via API do Telegram — **sem** anexar cert):
  ```
  curl -F "url=https://bot.satyansaita.com/webhook" \
    "https://api.telegram.org/bot<TOKEN>/setWebhook"
  ```
- Validar: `getWebhookInfo` mostra a URL nova e `last_error_message` vazio; mandar mensagem pro bot e confirmar processamento.

Ao fim da Fase 1, o webhook já está num cert real — mas a app ainda tem o keystore/8443 internamente (Caddy fala HTTPS self-signed com o localhost). É um estado estável e reversível.

### Fase 2 — aposentar o keystore (Caddy vira o único TLS) — **precisa de ADR**
- App: `server.ssl.enabled=false`, `server.port=8080` escutando **só no loopback**, remover config de keystore de `application-prod.properties`.
- Caddy: `api.` e `bot.` passam a `reverse_proxy http://localhost:8080` (sem skip-verify).
- SG (`network.tf`): **fechar 8443** ao mundo (remover o ingress 8443); manter 80/443/22.
- Remover `/opt/finbot/keystore.p12` e a chave `keystore_password` do Secrets Manager.
- **Sinergia com DEP-07:** o `bootstrap.sh` deixa de precisar tratar keystore — o provisionamento do "TLS do webhook" vira "só Caddy". Atualizar o DEP-07 quando esta fase entrar.

> **Decisão arquitetural (Fase 2) → ADR.** "Caddy é o único terminador de TLS na EC2; a app Spring escuta HTTP só no loopback; 8443/keystore aposentados." É uma extensão natural do ADR 0006 (API direto na EC2 atrás de proxy). **Recomendo escrever esse ADR antes de executar a Fase 2** — o planner propõe, o humano ratifica.

## Escopo / arquivos

**Fase 1:**
- `infra/dns.tf` — A record `bot.satyansaita.com` → EIP.
- Caddy na EC2 (SSH) — adicionar o site block `bot.satyansaita.com`.
- Operacional — `setWebhook` pra nova URL (humano).

**Fase 2 (após ADR):**
- `application-prod.properties` — `server.ssl.enabled=false`, `server.port=8080`, remover keystore.
- Caddy — `reverse_proxy http://localhost:8080` nos dois sites.
- `infra/network.tf` — remover ingress 8443.
- Operacional — remover keystore + secret.

**Não tocar:** `frontend/`, lógica de produto.

## Critérios de aceitação

**Fase 1:**
- [ ] `dig +short bot.satyansaita.com` → EIP.
- [ ] `curl -i https://bot.satyansaita.com/webhook` responde via Caddy com **cert LE válido** (sem `-k`); um POST do Telegram chega no controller.
- [ ] `getWebhookInfo` mostra `https://bot.satyansaita.com/webhook`, `last_error_message` vazio.
- [ ] Mandar mensagem pro bot → processada normalmente.

**Fase 2 (após ADR ratificado):**
- [ ] App sobe em `8080` HTTP no loopback; `server.ssl.*` removido; sem keystore.
- [ ] `api.` e `bot.` funcionam via Caddy (`/api/v1/resumo` → 401; webhook ok).
- [ ] SG sem ingress 8443; `terraform plan` in-place (sem replace da EC2).
- [ ] Keystore e `keystore_password` removidos; nada self-signed no box.
- [ ] DEP-07 `bootstrap.sh` atualizado pra não tratar keystore.
- [ ] Status report `docs/sprints/01-mvp/status/DEP-08.md` com frontmatter válido (comandos manuais documentados).

## Coordenação

- **DEP-07 (overnight):** se o `bootstrap.sh` foi escrito com a opção (a) do keystore, a Fase 2 deste DEP-08 o simplifica — alinhar pra não duplicar/conflitar.
- **`setWebhook`** é passo operacional do humano (token no Secrets Manager).
- **Pré-requisito da Fase 1:** disco da EC2 ok (FIX-volume) pra não esbarrar em `No space` ao instalar/configurar.

## Definição de pronto

Gates do `PRE-MERGE-CHECKLIST.md` (build/lint/testes verdes na Fase 2 que toca properties; evidência de infra = `terraform plan` in-place + validações de cert/webhook), status report válido, e **revisão do Reviewer** (toca o canal de entrada do bot + TLS de produção — alto risco). PR pra `develop`; não mergear sozinho. **Fase 2 só após o ADR de topologia de TLS.**

## Referências

- `docs/PENDENCIAS-TECNICAS.md` (débito self-signed → LE) · ADR 0006 (proxy na EC2) · `docs/plans/DEP-03-api-subdominio-proxy.md` (padrão do Caddy) · `docs/plans/DEP-07-codificar-provisionamento-ec2.md` (keystore que esta task aposenta).
- `TelegramWebhookController` → `POST /webhook` (confirmar ausência de prefixo de classe na execução).
</content>
