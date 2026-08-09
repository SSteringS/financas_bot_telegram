# Resumo overnight — deploy trilha 3c (DEP-05 / DEP-04 / DEP-07)

> Atualizado em 2026-05-27 (sessão do Reviewer).

---

## Estado atual das tasks

| Task | PR | Reviewer | Estado | Pronto pra merge? |
|---|---|---|---|---|
| DEP-05 | [#65](https://github.com/SSteringS/financas_bot_telegram/pull/65) | ✅ Aprovado | concluido | **Sim** |
| DEP-07 | [#64](https://github.com/SSteringS/financas_bot_telegram/pull/64) | ✅ Aprovado c/ obs. | concluido | **Sim** |
| DEP-04 | sem PR ainda | ❌ Bloqueado | bloqueado | Não — aguarda ações humanas |

---

## O que o reviewer já fez nesta sessão

- ✅ DEP-05 revisado e aprovado — PR #65 aberto
- ✅ DEP-07 revisado, `shellcheck` limpo (SC2154 falso positivo), `terraform plan` confirmado **in-place** (`~`) duas vezes — PR #64 aberto
- ✅ `frontend/.env.production` corrigido para `https://api.satyansaita.com` (commit `a466b9f` na branch DEP-04)
- ✅ DEP-04 status atualizado para `estado: bloqueado` (reviewer bloqueou: terraform apply não executado, pipeline não testado)
- ✅ Avaliações escritas em `docs/avaliacoes/`: DEP-04, DEP-05, DEP-07

---

## PENDÊNCIAS QUE PRECISAM DE VOCÊ

### P1 — Disco da EC2 (bloqueador imediato do deploy do back)

Se `growpart`/`xfs_growfs` ainda não foi feito:

```bash
sudo growpart /dev/nvme0n1 1
sudo xfs_growfs /
df -h /   # deve mostrar ~10 GB
```

Capar o journald se ainda não feito:
```bash
sudo mkdir -p /etc/systemd/journald.conf.d
echo -e '[Journal]\nSystemMaxUse=200M' | sudo tee /etc/systemd/journald.conf.d/finbot.conf
sudo systemctl restart systemd-journald
```

---

### P2 — Secret `keystore_password` no Secrets Manager

Confirmar que `finbot-prod-secrets` tem a chave `keystore_password=<keystore-password>`. Sem isso o app **não sobe** após redeploy.

```bash
aws secretsmanager get-secret-value --secret-id finbot-prod-secrets --query SecretString --output text
```

---

### P3 — Merge DEP-05 → deploy do back

DEP-05 está **aprovado e com PR aberto (#65)**. Passos:

1. Merge PR #65 em `develop`
2. Abrir PR `develop → main`
3. Pipeline `deploy.yml` redeploya o back automaticamente
4. Verificar: `curl -i https://api.satyansaita.com/api/v1/resumo` → 401 com `Access-Control-Allow-Origin: https://satyansaita.com`

> Depende de P1 (disco liberado) para o deploy não falhar.

---

### P4 — DEP-04: OIDC + terraform apply + pipeline (3 passos)

DEP-04 está **bloqueado** — o código está pronto mas precisa de validação em AWS.

**4a.** Checar se o provider OIDC já existe:
```bash
aws iam list-open-id-connect-providers
```
- Se existir para `token.actions.githubusercontent.com` → importar:
  ```bash
  cd financas_bot_telegram/infra
  terraform import aws_iam_openid_connect_provider.github \
    arn:aws:iam::776658251579:oidc-provider/token.actions.githubusercontent.com
  ```
- Se não existir → `apply` cria normalmente.

**4b.** `terraform plan` (confirmar que não há replace):
```bash
cd financas_bot_telegram/infra
git checkout feature/dep-04-pipeline-deploy-front
terraform plan -var-file=prod.tfvars
```

**4c.** `terraform apply`

**4d.** Quando a role IAM existir na conta, abrir PR da branch `feature/dep-04-pipeline-deploy-front` e mergear. O workflow dispara no próximo push em `frontend/**` na `main`.

**4e.** Fazer um push de teste (qualquer mudança em `frontend/`) e confirmar run verde no GitHub Actions.

Quando tudo verde: atualizar `docs/sprints/01-mvp/status/DEP-04.md` → `estado: concluido`, `pendencias_humano: 0`.

---

### P5 — Merge DEP-07 → apply (opcional, sem urgência)

DEP-07 está **aprovado e com PR aberto (#64)**. O `terraform apply` **não recria a EC2** (plan confirmado in-place). Pode mergear quando quiser — o bootstrap só entra em ação num recreate futuro.

1. Merge PR #64 em `develop`
2. `terraform apply -var-file=prod.tfvars` (atualiza o `user_data` do recurso no state)

---

### P6 — DEP-06: E2E em produção (último passo)

Só executar depois que DEP-05 estiver deployado (back com CORS correto) e DEP-04 tiver o front buildando via pipeline:

```
docs/runbooks/RUNBOOK-dep06-e2e-prod.md
```

Confirma: link mágico, exchange JWT → cookie `Domain=satyansaita.com`, chamadas autenticadas, CORS sem erro no browser.

---

## Ordem sugerida

```
P1 (disco)  →  P2 (secret)  →  P3 (DEP-05 merge + deploy)
                             →  P4 (DEP-04 OIDC + apply + merge)
                             →  P5 (DEP-07 merge + apply)
                             →  P6 (E2E)
```
