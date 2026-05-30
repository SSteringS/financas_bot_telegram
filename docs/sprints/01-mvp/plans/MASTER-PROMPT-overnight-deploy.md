# MASTER PROMPT — Overnight de deploy (DEP-05, DEP-04, DEP-07)

Cole o bloco abaixo numa sessão nova do **Claude do back** (IntelliJ/CLI) pra rodar à noite. Ele produz **só o código** das três tasks, em uma branch por task, parando antes de qualquer `apply`/SSH/merge. Os passos manuais (apply, console AWS, SSH, E2E) ficam pra você de manhã — há uma checklist no fim.

> **Por que code-only:** uma sessão desacompanhada não deve rodar `terraform apply`, fazer SSH em prod, mexer no console AWS nem mergear — não tem credenciais seguras pra isso e o domínio é dinheiro. Ela escreve o código, valida localmente (`terraform validate`, `shellcheck`, `mvn`), commita e escreve os status reports. O resto é seu + Reviewer.

---

```
Você é o Claude do back deste projeto. Leia, nesta ordem:
- CLAUDE.md (regras globais)
- docs/roles/backend.md (seu papel)
- docs/STATE.md (estado atual)
- docs/plans/DEP-05-cors-cookie-prod.md
- docs/plans/DEP-04-pipeline-deploy-front.md
- docs/plans/DEP-07-codificar-provisionamento-ec2.md
- docs/sprints/01-mvp/status/DEP-03.md (Caddyfile/unit já instalados — fonte pro DEP-07)
- docs/plans/FIX-crescer-volume-ec2.md (config do journald — fonte pro DEP-07)

## REGRAS DURAS (não violar)
1. CODE-ONLY. NÃO rode `terraform apply`. NÃO faça SSH na EC2. NÃO mexa no console/CLI da AWS. NÃO faça `git push` pra develop nem merge. NÃO rode testes em produção.
2. Validação permitida é LOCAL: `terraform fmt -check` / `terraform validate`, `shellcheck`, `mvn test`, `mvn package -DskipTests`.
3. UMA BRANCH POR TASK, criada a partir de `develop` (`git checkout develop && git pull && git checkout -b feature/<id>-<slug>`). UM commit por task no padrão `feat(<ID>): ...`.
4. Ao terminar CADA task: escreva o status report em docs/sprints/<NN>/status/<ID>.md com frontmatter válido (docs/templates/_TEMPLATE-status.md), e PARE — não mergeie. A revisão é do Reviewer; o apply é do humano.
5. Território: só `financas_bot_telegram/`, `infra/`, `.github/workflows/`. NÃO toque em `frontend/`.
6. Se bater numa decisão de produto que o plano não cobre, NÃO invente: registre como pendência no status report daquela task (estado: bloqueado) e siga pra próxima task.

## ORDEM E ESCOPO

### Task 1 — DEP-05 (a mais rápida)
Branch: feature/dep-05-cors-cookie-prod
- Corrigir em application-prod.properties: app.frontend.base-url, app.cors.allowed-origin (https://satyansaita.com, sem barra final), app.cookie.domain (satyansaita.com). Detalhes no plano.
- `grep` por hosts hardcoded no src/main (não deve haver; tudo é @Value).
- `mvn test` + `mvn package -DskipTests` verdes.
- Status report docs/sprints/01-mvp/status/DEP-05.md.

### Task 2 — DEP-04 (pipeline do front via OIDC)
Branch: feature/dep-04-pipeline-deploy-front
- Criar infra/iam-github-oidc.tf (provider OIDC + role escopada ao repo/branch main + policy mínima S3+CloudFront). Para o `sub` do OIDC, leia o slug owner/repo de `git remote get-url origin`; se não der pra resolver, use o placeholder <OWNER>/<REPO> e marque isso CLARAMENTE no status report como pendência de humano.
- Criar .github/workflows/deploy-frontend.yml conforme o plano (push em main paths frontend/**; OIDC; npm ci/build; s3 sync; invalidate). Bucket finbot-frontend-prod-776658251579, distribuição E1WG4Q8MG3V9HY, region us-east-1.
- `terraform fmt` + `terraform validate` (NÃO `plan`/`apply` — sem credenciais).
- Status report docs/sprints/01-mvp/status/DEP-04.md, anotando: o provider OIDC é singleton da conta (humano confirma/importa se já existir) e o slug do repo usado.

### Task 3 — DEP-07 (codificar provisionamento — a mais densa)
Branch: feature/dep-07-codificar-provisionamento-ec2
- Criar infra/provision/bootstrap.sh idempotente capturando TUDO que hoje é manual: Java, usuário/pasta finbot, unit finbot.service, Caddy+Caddyfile+unit (copiar de docs/sprints/01-mvp/status/DEP-03.md), cap do journald (SystemMaxUse=200M, de docs/plans/FIX-crescer-volume-ec2.md), e o keystore (opção (a) do plano: regenerar self-signed no boot + nota de re-registrar webhook).
- Templates versionados (Caddyfile, finbot.service, drop-in do journald) conforme o plano.
- Religar ec2.tf: user_data via templatefile(), interpolando domain_name. Manter lifecycle ignore_changes=[ami].
- `shellcheck` no bootstrap.sh + `terraform fmt`/`validate`.
- NÃO dá pra confirmar plan in-place nem subir instância de teste (sem credenciais) — registre isso como verificação pendente do humano no status report.
- Status report docs/sprints/01-mvp/status/DEP-07.md.

## AO FINAL
Escreva docs/sprints/01-mvp/status/_RESUMO-overnight-deploy.md com:
- Tabela: task | branch | commit | resultado das checagens locais | estado.
- CHECKLIST MANUAL PRA MANHÃ (humano), na ordem:
  1. DEP-05/DEP-07: revisar diffs; `terraform plan` e CONFIRMAR in-place (sem replace da EC2) antes de qualquer apply.
  2. DEP-04: criar/confirmar o provider OIDC na AWS; preencher o slug do repo se ficou placeholder; `terraform apply` do iam-github-oidc.
  3. Deploy do back (após FIX-volume do disco) com as mudanças do DEP-05.
  4. DEP-07: aplicar quando quiser; validar num recreate (ou instância descartável).
  5. Reviewer revisa cada PR antes do merge em develop.
  6. DEP-06: rodar o E2E em prod por docs/runbooks/RUNBOOK-dep06-e2e-prod.md.
- Pendências/decisões que precisam de você (ex.: slug do repo, escolha do keystore).

NÃO mergeie nada. Pare aqui.
```

---

## Notas pra você (humano), fora do prompt

- **O disco da EC2 (FIX-volume) e o `keystore_password` no Secrets Manager** continuam sendo pré-requisitos manuais seus — não entram no overnight (são apply/console/SSH). Sem o disco resolvido, o **deploy do back** (passo 3 da checklist) não completa.
- **O front (`frontend/.env.production`)** é território de outra sessão — o overnight do back não corrige. Quando for deployar o front (DEP-04), garanta que `VITE_API_BASE_URL=https://api.satyansaita.com` (hoje está no domínio antigo).
- **DEP-06** é seu, de manhã, pelo runbook — depende de tudo acima aplicado.
- Cada branch volta pra você + Reviewer; nada é mergeado pela sessão.

## Referências

- Planos: DEP-04, DEP-05, DEP-07 · Runbook: RUNBOOK-dep06-e2e-prod.md · FIX-crescer-volume-ec2.
- Padrão de overnight anterior: `docs/sprints/01-mvp/status/_RESUMO-overnight-back-2.md`.
</content>
