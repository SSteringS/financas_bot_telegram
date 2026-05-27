# Avaliação — DEP-04: GitHub Actions deploy do front (OIDC)

**Data:** 2026-05-27  
**Reviewer:** claude-reviewer (sessão independente — ADR 0005)  
**Branch revisada:** `feature/dep-04-pipeline-deploy-front`  
**Status report:** `docs/status/DEP-04.md`  
**Plano:** `docs/plans/DEP-04-pipeline-deploy-front.md`

---

## Veredito

**Aprovado** — bloqueantes resolvidos após ações humanas pós-revisão.

1. ✅ `terraform apply` executado pelo humano (2026-05-27) — role IAM `finbot-prod-gha-frontend-deploy` criada na conta.
2. ✅ Pipeline `deploy-frontend.yml` rodou com sucesso (2026-05-27) — OIDC auth → build → S3 sync → CloudFront invalidation verde.
3. ✅ E2E (DEP-06) confirmou front acessível em `https://satyansaita.com` com API funcionando.

~~**Reprovado — 2 bloqueantes.**~~
~~1. `estado: concluido` inválido com `pendencias_humano: 1`.~~
~~2. Terraform apply do `iam-github-oidc.tf` não executado → pipeline nunca testado end-to-end.~~

---

## Gates verificados

| Gate | Reportado | Verificado | Resultado |
|---|---|---|---|
| `build/lint/testes` | `na` | Infra-only + workflow YAML — sem código de produto | ✓ |
| `branch_convencao` | `ok` | `git merge-base --is-ancestor origin/develop HEAD` → exit 0 ✓ | ✓ |
| `territorio` | `ok` | `.github/workflows/deploy-frontend.yml` (back ✓) + `financas_bot_telegram/infra/iam-github-oidc.tf` (back ✓) + `docs/status/` (shared ✓) | ✓ |
| `estado: concluido` | declarado | **INVÁLIDO** com `pendencias_humano: 1` — template exige 0 | ✗ |

---

## Qualidade do código — o que está certo

### `iam-github-oidc.tf`
- Provider OIDC singleton com thumbprint `6938fd4d98bab03faadb97b34396831e3780aea1` (thumbprint estável da CA do GitHub OIDC) ✓
- `assume_role_policy` condiciona `aud = sts.amazonaws.com` **E** `sub = repo:SSteringS/financas_bot_telegram:ref:refs/heads/main` — escopo mínimo e correto; qualquer fork ou branch diferente de `main` não consegue assumir a role ✓
- Policy mínima: `s3:ListBucket` no bucket + `s3:GetObject/PutObject/DeleteObject` em `bucket/*` + `cloudfront:CreateInvalidation` na distribuição — nada além ✓
- Referencia `aws_s3_bucket.frontend.arn` e `aws_cloudfront_distribution.frontend.arn` (sem hardcodes de ARN de recurso) ✓

### `deploy-frontend.yml`
- `on.push.branches: [main]` + `paths: ['frontend/**']` — dispara só no caminho certo ✓
- `permissions: id-token: write; contents: read` — exigido pelo OIDC ✓
- Autenticação via `aws-actions/configure-aws-credentials@v4` ✓
- `aws s3 sync dist/ s3://finbot-frontend-prod-776658251579 --delete` — bucket correto (DEP-02), com `--delete` para remover arquivos obsoletos ✓
- `aws cloudfront create-invalidation --distribution-id E1WG4Q8MG3V9HY --paths "/*"` — distribuição correta (DEP-02) ✓
- ARN hardcoded (`arn:aws:iam::776658251579:role/finbot-prod-gha-frontend-deploy`) — documentado como limitação técnica aceitável (GH Actions não consome outputs Terraform em runtime; ARN é determinístico) ✓

---

## Bloqueantes

### 1. `estado: concluido` inválido com `pendencias_humano: 1`

O PRE-MERGE-CHECKLIST é explícito: *"`estado: concluido` só é válido quando todos os gates relevantes estão `ok` (ou `na`) e `pendencias_humano: 0`."* Com 1 pendência humana em aberto, o estado correto é `bloqueado`.

A pendência é: confirmar se o OIDC provider já existe na conta AWS e, se existir, importar antes do `apply`.

### 2. Terraform apply não executado → pipeline nunca testado

O critério de aceitação principal do plano é: *"Um push de teste em `frontend/**` na `main`: workflow autentica via OIDC, builda, faz `s3 sync` e invalida o CloudFront — verde fim a fim."*

Sem o `terraform apply` do `iam-github-oidc.tf`, a role IAM não existe na conta AWS, e qualquer disparo do workflow falha imediatamente no passo de OIDC. O pipeline não foi testado.

---

## Observações não-bloqueantes

**`frontend/.env.production`**: ✅ corrigido manualmente pelo humano (2026-05-27) — `VITE_API_BASE_URL=https://api.satyansaita.com`. Dependência resolvida.

**`deploy.yml` do back sem filtro de path**: um push só de front redisploya o back à toa. Follow-up correto (fora de escopo).

---

## Ação requerida

1. Corrigir `estado: bloqueado` no frontmatter (aguardando human action do OIDC).
2. Humano: checar `aws iam list-open-id-connect-providers` e, se necessário, importar o provider.
3. Humano: executar `terraform apply` do `iam-github-oidc.tf`.
4. Testar o pipeline com um push real em `frontend/**` na `main` e confirmar run verde fim a fim.
5. Atualizar status report com evidência do run verde + `pendencias_humano: 0` + `estado: concluido`.
6. Reabrir para revisão.
