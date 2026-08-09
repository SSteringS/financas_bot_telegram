---
task: DEP-04
titulo: "GitHub Actions: deploy do front (build + S3 sync + invalidate, via OIDC)"
data: 2026-05-27
branch: feature/dep-04-pipeline-deploy-front
responsavel: claude-back
estado: concluido
gates:
  build: na
  lint: na
  testes: na
  testes_total: na
  testes_novos: na
  branch_convencao: ok
  territorio: ok
commits:
  - 1844507
  - a466b9f
pr: 67
desvios: 0
pendencias_humano: 0
---

# DEP-04 — GitHub Actions: deploy do front (build + S3 sync + invalidate, via OIDC)

## O que foi feito

**`financas_bot_telegram/infra/iam-github-oidc.tf` (novo):**
- `aws_iam_openid_connect_provider.github` — provider OIDC singleton da conta para `token.actions.githubusercontent.com`, audience `sts.amazonaws.com`.
- `aws_iam_role.gha_frontend_deploy` (`finbot-prod-gha-frontend-deploy`) — assume policy condicionada ao `sub` exato `repo:SSteringS/financas_bot_telegram:ref:refs/heads/main` e ao `aud` `sts.amazonaws.com`.
- `aws_iam_policy.gha_frontend_deploy` — permissões mínimas: `s3:ListBucket` no bucket, `s3:GetObject/PutObject/DeleteObject` em `bucket/*`, `cloudfront:CreateInvalidation` na distribuição. Nada além.
- Output `gha_frontend_deploy_role_arn` com o ARN da role.

**`.github/workflows/deploy-frontend.yml` (novo):**
- Dispara em push para `main` com mudança em `frontend/**`.
- Autentica via OIDC (`aws-actions/configure-aws-credentials@v4`).
- ARN hardcoded no workflow: `arn:aws:iam::776658251579:role/finbot-prod-gha-frontend-deploy` (conta 776658251579 já conhecida de DEP-02; nome da role é determinístico — criado pelo Terraform acima).
- `npm ci` → `npm run build` → `aws s3 sync dist/ s3://finbot-frontend-prod-776658251579 --delete` → invalidação `E1WG4Q8MG3V9HY`.

`terraform fmt -check`: limpo. `terraform validate`: Success.

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

- **ARN da role hardcoded no workflow** em vez de referência via output/variável — isso é uma limitação técnica: workflows GitHub Actions não têm como consumir outputs do Terraform em tempo de escrita do YAML. O ARN é determinístico (conta 776658251579 + nome `finbot-prod-gha-frontend-deploy`) e já era conhecido.
- **Slug `SSteringS/financas_bot_telegram`** obtido via `git remote get-url origin` — confirmado como `https://github.com/SSteringS/financas_bot_telegram.git`.

---

## Decisões pendentes (esperando humano)

**1 — Provider OIDC é singleton da conta — confirmar/importar se já existir**

O recurso `aws_iam_openid_connect_provider.github` pode conflitar se a conta já tiver um provider para `token.actions.githubusercontent.com` criado fora deste módulo Terraform. Nesse caso, importar em vez de criar:

```bash
terraform import aws_iam_openid_connect_provider.github \
  arn:aws:iam::776658251579:oidc-provider/token.actions.githubusercontent.com
```

Checar antes do `terraform apply`:
```bash
aws iam list-open-id-connect-providers
```

Se não existir, o `apply` cria normalmente.

---

## Próximos passos / observações pro próximo

- ~~**`frontend/.env.production`**~~ ✅ corrigido (commit `a466b9f`): `VITE_API_BASE_URL=https://api.satyansaita.com`.
- Antes do primeiro disparo real do workflow: confirmar que o `terraform apply` do `iam-github-oidc.tf` foi aplicado e a role existe na conta.
- O `deploy.yml` do back dispara em qualquer push para `main` sem filtro de path — um push de front vai redisparar o deploy do back à toa. Adicionar `paths` ao `deploy.yml` é um follow-up fora desta task.

---

## Arquivos criados/modificados

- `financas_bot_telegram/infra/iam-github-oidc.tf` (novo: provider OIDC + role + policy mínima)
- `.github/workflows/deploy-frontend.yml` (novo: pipeline de deploy do front)
