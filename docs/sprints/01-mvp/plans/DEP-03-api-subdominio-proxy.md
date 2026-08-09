# DEP-03 — Subdomínio `api.satyansaita.com` → EC2 atrás de proxy reverso

> **Intake (contrato de entrada da task)**
>
> - **Origem:** `docs/plans/BACKLOG-produto.md` (Fase 3c, DEP-03) + ADR 0006 (front e API em hostnames separados). É o próximo da fila de deploy depois de DEP-01/02.
> - **Prioridade:** alta — sem hostname HTTPS válido pra API, o front em prod (`https://satyansaita.com`) não consegue chamar o backend (cert self-signed da 8443 não serve pro browser).
> - **Esforço:** médio (DNS + SG são triviais; o proxy + TLS na EC2 é o grosso, e é trabalho manual de servidor).
> - **Território / quem executa:** `financas_bot_telegram/infra/` (Terraform) + configuração na própria EC2 (SSH) → **Claude do back**. Se mexer em connector/porta da app, `application-prod.properties` (também back).
> - **Branch:** `feature/dep-03-api-subdominio-proxy`, a partir de `develop`.
> - **Dependências:** DEP-01 (zona Route 53) e DEP-02 (front no apex) — ✅ feitas. EC2 da Fase 2 de pé com `finbot.service` rodando — ✅.
> - **Riscos:**
>   1. **Quebrar o webhook do Telegram** (hoje em 8443, self-signed). Mitigação: DEP-03 **não toca** na 8443 nem no webhook; só adiciona um caminho novo (443 via proxy). Ver "Decisão" abaixo.
>   2. **Let's Encrypt falhar no challenge** se o A record não propagou ou a porta 80 não está aberta. Mitigação: abrir 80 no SG e aplicar o DNS **antes** de subir o proxy; conferir `dig api.satyansaita.com` resolvendo pro EIP.
>   3. **`user_data` não re-roda em instância existente** — instalar o proxy é passo manual via SSH (o `user_data` atual só roda na criação). Mitigação: documentar os comandos + opcionalmente refletir no `user_data` pra reprovisionamento futuro.

---

## Contexto

O front está em `https://satyansaita.com` (DEP-02, S3+CloudFront). A API REST (Spring Boot na EC2) precisa de um hostname HTTPS próprio pro browser confiar — hoje a app escuta em **8443 com cert auto-assinado** (suficiente pro webhook do Telegram, que aceita cert self-signed via `setWebhook`, mas **rejeitado por browser**). Sem isso, o `fetch` do front pra API falha no TLS.

ADR 0006 fixou: front no apex + **API em `api.satyansaita.com`, direto na EC2 atrás de proxy reverso HTTPS** (sem CloudFront/ALB na frente). Esta task materializa esse subdomínio.

## Decisão / abordagem

**Proxy reverso na EC2 com Let's Encrypt, terminando TLS pra `api.satyansaita.com` e repassando pra app local. O webhook do Telegram em 8443 fica intocado.**

### Por que Let's Encrypt e não o ACM existente
O cert ACM (`*.satyansaita.com`) está em `us-east-1` e **só pode ser usado por CloudFront/ALB/API Gateway — não é exportável pra instalar em nginx/Caddy na EC2**. Como ADR 0006 recusou pôr CloudFront/ALB na frente da API (custo + acoplamento + hop), a terminação TLS na própria EC2 exige um cert obtido lá — Let's Encrypt.

### Proxy: **Caddy** (recomendado) vs Nginx+certbot
- **Caddy (recomendado):** ACME automático (obtém e renova o cert sozinho), um `Caddyfile` de ~5 linhas. Não está no repo padrão do AL2023 — instalar via binário estático ARM64 + unit systemd.
- **Nginx + certbot (alternativa):** mais conhecido, mas no Amazon Linux 2023 o `certbot` é chato (sem snap; via pip/venv + cron de renovação manual). Mais peças móveis. Escolher só se já houver familiaridade forte com nginx.

### Como a app fica atrás do proxy (sub-decisão — confirmar na execução)
- **Opção A (recomendada, zero mudança na app):** Caddy faz `reverse_proxy https://localhost:8443` com verificação de TLS desligada (`transport http { tls_insecure_skip_verify }`). Tráfego é loopback, então o self-signed interno é aceitável. Webhook segue igual. **Mais rápido pra subir.**
- **Opção B (mais limpa, mexe na app):** adicionar um connector HTTP só no loopback (`127.0.0.1:8080`) na app Spring e apontar `reverse_proxy http://localhost:8080`. Evita TLS interno, mas é mudança em `application-prod.properties`/config (back) e precisa garantir que o connector não escuta em `0.0.0.0`.

Recomendo **A** pra esta task (entrega o subdomínio sem risco de regressão na app) e registrar B como possível limpeza futura.

> **Fora de escopo (follow-up):** migrar o **webhook do Telegram** pra trás do proxy com cert real (`bot.satyansaita.com`) e fechar a 8443 ao mundo. Isso resolve o débito "cert self-signed → LE" (`docs/PENDENCIAS-TECNICAS.md`) mas envolve reconfigurar o `setWebhook` — tratar numa task própria, não aqui.

## Escopo / arquivos

**Terraform (`financas_bot_telegram/infra/`):**

- `dns.tf` — adicionar `aws_route53_record` tipo A: `api.${var.domain_name}` → `aws_eip.finbot_eip.public_ip` (TTL ~300). A zona é o data source `data.aws_route53_zone.principal` já existente.
- `network.tf` — no `aws_security_group.ec2_sg`, adicionar ingress **443** (HTTPS da API) e **80** (HTTP-01 challenge do Let's Encrypt + redirect→443), ambos `0.0.0.0/0`. Não remover a 8443 nem a 22.

**Na EC2 (manual via SSH — documentar no status report):**

- Instalar Caddy (binário ARM64 em `/usr/bin/caddy` + unit systemd `caddy.service` + usuário `caddy`).
- `Caddyfile`:
  ```
  api.satyansaita.com {
      reverse_proxy https://localhost:8443 {
          transport http { tls_insecure_skip_verify }
      }
  }
  ```
- Habilitar e subir o serviço; Caddy obtém o cert no primeiro start (precisa do A record já resolvendo + 80 aberta).

**Não tocar:** porta 8443, `finbot.service`, o fluxo do webhook do Telegram, nada de `frontend/`.

## Critérios de aceitação

- [ ] `dig +short api.satyansaita.com` retorna o EIP da EC2.
- [ ] `aws_security_group.ec2_sg` tem ingress 80 e 443; `terraform plan` limpo depois do apply.
- [ ] **Cert válido** (cadeia Let's Encrypt, `curl` sem `-k` não reclama) e `Server: Caddy` na resposta.
- [ ] A requisição **chega na app Spring** via `api.satyansaita.com` — validar com um endpoint que existe **em prod**: `curl -i https://api.satyansaita.com/api/v1/resumo` → **401** (auth exigida). Um 401 vindo do Spring prova a cadeia proxy→app→security. **Não** usar `/actuator/health` (sem Actuator no classpath) nem `/v3/api-docs` (springdoc **desligado em prod**) — ambos 404 por design.

> **Nota (validação 2026-05-27):** os smoke-tests `/actuator/health` e `/v3/api-docs` retornam 404 — não por falha do proxy, mas porque **prod não serve nenhum dos dois**: o `pom.xml` não tem `spring-boot-starter-actuator`, e `application-prod.properties` traz `springdoc.api-docs.enabled=false`/`swagger-ui.enabled=false` (não expor a API publicamente). A cadeia proxy→app foi confirmada do mesmo jeito (404 é do Spring, com headers de CORS do app). Validar com `/api/v1/resumo` → 401. Follow-up opcional de back: adicionar o Actuator pra ter um health real (a SecurityConfig da BE-12 já libera `/actuator/health` anônimo, hoje apontando pro vazio).
- [ ] **Webhook do Telegram segue funcionando** — mandar uma mensagem de teste pro bot e confirmar que processa (regressão da 8443).
- [ ] Renovação automática do cert confirmada (Caddy faz sozinho; checar `caddy` logs / `systemctl status caddy`).
- [ ] Status report em `docs/sprints/01-mvp/status/DEP-03.md` com frontmatter válido **incluindo os comandos manuais executados na EC2** (pra reprodutibilidade).

## Coordenação

- **DEP-05** depende deste: quando `api.satyansaita.com` estiver no ar, o back ajusta CORS (`allowed-origin = https://satyansaita.com`) e o cookie (`Domain=.satyansaita.com`). Não fazer aqui — só deixar o hostname pronto.
- **DEP-04** (pipeline do front) é independente e pode correr em paralelo.
- Ordem interna obrigatória: aplicar **DNS + SG (Terraform) primeiro**, esperar o A record propagar, **depois** subir o Caddy (senão o challenge do Let's Encrypt falha).

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build/lint/testes = `na` se não houver mudança de código Java; `terraform plan` limpo conta como evidência de infra), status report em `docs/sprints/01-mvp/status/DEP-03.md` com frontmatter válido, e **revisão independente pelo Reviewer** antes do merge (obrigatória — toca em infra de produção). Abrir PR pra `develop`; não mergear sozinho.

## Referências

- `docs/plans/BACKLOG-produto.md` (DEP-03 original; ajustar `finbot.dom.br` → `satyansaita.com`)
- ADR 0006 (`docs/decisions/0006-front-e-api-em-hostnames-separados.md`) e `docs/aprendizado/front-api-hostnames-separados.md`
- `docs/sprints/01-mvp/status/DEP-01.md`, `docs/sprints/01-mvp/status/DEP-02.md` (o que já existe de DNS/cert/infra)
- `docs/PENDENCIAS-TECNICAS.md` (débito "cert self-signed → Let's Encrypt", relacionado ao follow-up do webhook)
</content>
