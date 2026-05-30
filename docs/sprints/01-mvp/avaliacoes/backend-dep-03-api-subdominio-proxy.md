# Avaliação — DEP-03: Subdomínio api.satyansaita.com + proxy reverso Caddy

**Data:** 2026-05-27  
**Reviewer:** claude-reviewer (sessão independente — ADR 0005)  
**Branch revisada:** `feature/dep-03-api-subdominio-proxy`  
**Status report:** `docs/sprints/01-mvp/status/DEP-03.md`  
**Plano:** `docs/plans/DEP-03-api-subdominio-proxy.md`

---

## Veredito

**Aprovado** — smoke test confirmado pelo reviewer; webhook do Telegram confirmado pelo humano (2026-05-27).

---

## Verificação dos gates

| Gate | Reportado | Verificado | Resultado |
|---|---|---|---|
| `build/lint/testes` | `na` | Task infra-only (Terraform + config manual EC2) — sem código Java/JS alterado. `na` correto | ✓ |
| `branch_convencao` | `ok` | `git merge-base --is-ancestor origin/develop HEAD` → exit 0 ✓ | ✓ |
| `territorio` | `ok` | `git diff --name-only origin/develop...HEAD` → `financas_bot_telegram/infra/` (back ✓) + `docs/sprints/01-mvp/status/` (shared ✓) | ✓ |
| `desvios: 0` | `0` | Sem desvios do plano. Opção A foi a recomendação explícita do plano — não é desvio, é execução da opção recomendada. `0` correto | ✓ |
| `pendencias_humano: 0` | `0` | Caddy instalado e validado em 2026-05-27. Seção "RESOLVIDO" no status report. `0` correto | ✓ |

---

## Smoke test — verificado pelo reviewer

```
curl -si https://api.satyansaita.com/api/v1/resumo

HTTP/1.1 401 Unauthorized
Server: Caddy
Content-Type: application/json;charset=UTF-8
{"codigo":"SESSAO_AUSENTE","mensagem":"Cookie de sessão ausente"}
```

Prova a cadeia completa:
- **TLS válido** — curl executado sem `-k`, cert aceito pelo cliente. Let's Encrypt emitido. ✓
- **`Server: Caddy`** na resposta — Caddy está terminando TLS e servindo a requisição. ✓
- **App Spring alcançada** — resposta `SESSAO_AUSENTE` com corpo JSON é gerada pela security da BE-12, não pelo Caddy. Proxy funcionando. ✓
- **401 esperado** — endpoint `/api/v1/resumo` requer auth; a resposta correta neste contexto é 401, não erro de rede. ✓

Nota: `/actuator/health` → 404 (sem Actuator no classpath) e `/v3/api-docs` → 404 (springdoc desligado em prod) — ambos esperados, documentados no plano e no status report. Não indicam falha de proxy.

---

## Diff vs. plano — linha a linha

### `dns.tf`

| Esperado pelo plano | Implementado |
|---|---|
| `aws_route53_record` tipo A | `aws_route53_record "api_a"` ✓ |
| `api.${var.domain_name}` | `name = "api.${var.domain_name}"` ✓ |
| `aws_eip.finbot_eip.public_ip` | `records = [aws_eip.finbot_eip.public_ip]` ✓ |
| TTL ~300 | `ttl = 300` ✓ |
| Usar data source existente (não criar zona) | `data "aws_route53_zone" "principal"` — referencia, não cria ✓ |

### `network.tf`

| Esperado pelo plano | Implementado |
|---|---|
| Ingress 443 (HTTPS API) | ✓ description "HTTPS API" |
| Ingress 80 (LE challenge) | ✓ description "HTTP - Lets Encrypt challenge + redirect" |
| Não remover 8443 | ✓ regra 8443 preservada |
| Não remover 22 | ✓ regra 22 preservada |
| Ambos `0.0.0.0/0` | ✓ |

Mudança puramente aditiva — zero risco de regressão nas regras existentes.

---

## Observação: regressão do webhook do Telegram — não verificada pelo reviewer

O plano lista como critério: *"Webhook do Telegram segue funcionando — mandar uma mensagem de teste pro bot e confirmar que processa."* O status report também pede explicitamente que o reviewer confirme.

A verificação requer envio de mensagem no Telegram — não é possível via terminal nesta sessão.

**Avaliação de risco**: baixa. A mudança no Terraform é aditiva (adiciona regras, não remove). A porta 8443 e seu ingress no SG permanecem intocados. O Caddyfile usa `reverse_proxy https://localhost:8443` (loopback), sem interferência no acesso externo à 8443. O `tls_insecure_skip_verify` é válido neste contexto (tráfego loopback, cert interno).

**Ação requerida:** o humano confirma o webhook mandando uma mensagem pro bot antes do merge. Evidência: bot responde normalmente após o deploy do DEP-03.

---

## Itens que não pude verificar independentemente

**`terraform plan` limpo**: requer credenciais AWS. O status report afirma "1 add, 1 change, 0 destroy" no apply. A inspeção do diff confirma que as mudanças são as esperadas e puramente aditivas — nenhuma resource foi modificada de forma destrutiva.

---

## Observação menor — nome do arquivo de status

O status report está em `docs/sprints/01-mvp/status/DEP-03.md` (sem slug após o ID). A convenção do template é `<TASK-ID>-titulo-curto.md`. Não bloqueante — outras tarefas usam o mesmo padrão curto — mas vale adotar o slug nas próximas para consistência.

---

## Critérios de aceitação do plano

| Critério | Verificado por |
|---|---|
| `dig api.satyansaita.com` → EIP da EC2 | `nslookup` no status report ✓ |
| SG com ingress 80 e 443; `terraform plan` limpo | Diff confirmado ✓; plan não verificado (sem credenciais) |
| Cert válido (`curl` sem `-k`) + `Server: Caddy` | Confirmado pelo reviewer ✓ |
| Requisição chega no Spring → 401 SESSAO_AUSENTE | Confirmado pelo reviewer ✓ |
| Webhook Telegram segue funcionando | ✓ — confirmado pelo humano (2026-05-27): bot respondendo normalmente |
| Renovação automática do cert | Caddy gerencia ACME — comportamento esperado; não verificável sem aguardar renovação |
| Status report com comandos manuais | ✓ — roteiro completo de instalação do Caddy documentado |

---

## Ação requerida antes do merge

Nenhuma — todos os critérios verificados. PR pode ser aberto.
