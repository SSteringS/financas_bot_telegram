# Resumo overnight — deploy trilha 3c (DEP-05 / DEP-04 / DEP-07)

> Gerado em 2026-05-27. Todas as branches aguardam revisão do Reviewer e merge pelo humano.

---

## Tabela de tasks

| Task | Branch | Commit principal | Checagens locais | Estado |
|---|---|---|---|---|
| DEP-05 | `feature/dep-05-cors-cookie-prod` | `30dd0b1` | `mvn test`: 226/0 ✅ · `mvn package`: ✅ | concluido |
| DEP-04 | `feature/dep-04-pipeline-deploy-front` | `1844507` | `terraform fmt -check`: ✅ · `terraform validate`: ✅ | concluido |
| DEP-07 | `feature/dep-07-codificar-provisionamento-ec2` | `a185f94` | `terraform fmt -check`: ✅ · `terraform validate`: ✅ · shellcheck: ⚠️ ausente no env Windows (revisão manual) | concluido |

---

## CHECKLIST MANUAL PRA MANHÃ

### 0. Antes de tudo — disco da EC2 (bloqueador de deploy)

O disco raiz (agora 10 GB gp3 — já no `ec2.tf` de produção) precisou de `growpart`/`xfs_growfs` manual. Se ainda não foi feito, rodar via SSH antes de qualquer deploy do back:

```bash
sudo growpart /dev/nvme0n1 1
sudo xfs_growfs /
df -h /   # deve mostrar ~10 GB
```

Também capar o journald se ainda não feito:
```bash
sudo cp /etc/systemd/journald.conf.d/finbot.conf /tmp/ 2>/dev/null || \
  echo -e '[Journal]\nSystemMaxUse=200M' | sudo tee /etc/systemd/journald.conf.d/finbot.conf
sudo systemctl restart systemd-journald
```

---

### 1. DEP-05 — revisar diff e fazer deploy do back (após disco liberado)

1. **Revisar diff** da branch `feature/dep-05-cors-cookie-prod` — 3 linhas no `application-prod.properties`.
2. **Reviewer** revisa o PR antes do merge em `develop` (ADR 0005).
3. Merge em `develop` → PR para `main` → pipeline `deploy.yml` redeploya o back com CORS e cookie corrigidos.
4. Verificar pós-deploy: `curl -i https://api.satyansaita.com/api/v1/resumo` → 401 com `Server: Caddy` (Caddy e app de pé).

---

### 2. DEP-04 — criar/confirmar provider OIDC + terraform apply + pipeline

1. **Checar se o provider OIDC já existe na conta:**
   ```bash
   aws iam list-open-id-connect-providers
   ```
   - Se existir para `token.actions.githubusercontent.com`, **importar** antes do apply:
     ```bash
     cd financas_bot_telegram/infra
     terraform import aws_iam_openid_connect_provider.github \
       arn:aws:iam::776658251579:oidc-provider/token.actions.githubusercontent.com
     ```
   - Se não existir, o `apply` cria normalmente.

2. **Revisar diff** da branch `feature/dep-04-pipeline-deploy-front`:
   - `iam-github-oidc.tf` — confirmar que o `sub` tem o slug correto (`SSteringS/financas_bot_telegram`).
   - `.github/workflows/deploy-frontend.yml` — confirmar trigger em `frontend/**` apenas.

3. **Reviewer** revisa o PR (toca IAM e pipeline de prod).

4. `terraform plan` → confirmar que **não há replace** de recurso existente → `terraform apply` do `iam-github-oidc.tf`.

5. Merge em `develop` → PR para `main` → o workflow dispara automaticamente no próximo push com `frontend/**`.

---

### 3. DEP-07 — revisar diff + confirmar terraform plan in-place antes de qualquer apply

1. **Revisar diff** da branch `feature/dep-07-codificar-provisionamento-ec2`:
   - `infra/provision/bootstrap.sh` — revisar linha a linha; em especial as guards de idempotência e o passo do keystore.
   - `infra/ec2.tf` — `user_data` agora usa `templatefile()` em vez do heredoc antigo.

2. **⚠️ Gate obrigatório — terraform plan:**
   ```bash
   cd financas_bot_telegram/infra
   terraform plan -var-file=prod.tfvars
   ```
   - Deve mostrar `~` (update in-place) para `aws_instance.finbot_app`.
   - **Se aparecer `-/+` (replace), PARAR imediatamente.** Isso destruiria a EC2 de produção.

3. **Reviewer** revisa o PR (toca provisionamento de prod, risco alto).

4. `terraform apply` — pode ser feito a qualquer momento, inclusive depois de um recreate planejado futuro. O script **não roda na instância atual** (ADR 0009).

5. **shellcheck:** rodar quando disponível (instalar localmente ou deixar o CI rodar):
   ```bash
   shellcheck financas_bot_telegram/infra/provision/bootstrap.sh
   ```

---

### 4. Pós-deploy — DEP-06: teste E2E em produção

Depois que DEP-05 e os dois lados do domínio estiverem no ar (`application-prod.properties` corrigido no back + `VITE_API_BASE_URL` corrigido no front), executar o runbook:

```
docs/runbooks/RUNBOOK-dep06-e2e-prod.md
```

O E2E confirma: link mágico, exchange JWT → cookie `Domain=satyansaita.com`, chamadas autenticadas, CORS sem erro no browser.

---

### 5. Reviewer — revisar cada PR antes do merge

Ordem recomendada de revisão e merge:
1. DEP-05 (menor risco, desbloqueia o back)
2. DEP-04 (IAM + pipeline — alto risco, revisar com cuidado)
3. DEP-07 (provisionamento EC2 — alto risco, confirmar plan in-place)

---

## Pendências / decisões que precisam de você

| # | Pendência | Task | Urgência |
|---|---|---|---|
| P1 | **Provider OIDC:** checar se já existe na conta antes do `terraform apply` do DEP-04 (importar se existir) | DEP-04 | Antes do apply |
| P2 | **`frontend/.env.production`:** corrigir `VITE_API_BASE_URL` de `https://api.finbot.dom.br` para `https://api.satyansaita.com` — território do Claude do front | DEP-04/06 | Antes do build de prod |
| P3 | **Secret `keystore_password`:** confirmar que `finbot-prod-secrets` tem a chave `keystore_password=finbot123` — sem isso o app não sobe | Estado.md | Antes do redeploy do back |
| P4 | **Pós-recreate:** se a EC2 for recriada com o novo `user_data`, re-registrar o webhook do Telegram com o novo cert self-signed (`/opt/finbot/keystore.pem`) | DEP-07 | Após recreate |
| P5 | **Disco EC2:** `growpart`/`xfs_growfs` se ainda não feito (deploy do back bloqueado enquanto cheio) | FIX-volume | Imediato |
| P6 | **shellcheck** do `bootstrap.sh` — não disponível no env Windows; rodar localmente ou no CI antes de confiar o script num recreate real | DEP-07 | Antes do recreate |
