# DEP-04 — GitHub Actions: deploy do front (build + S3 sync + invalidate, via OIDC)

> **Intake (contrato de entrada da task)**
>
> - **Origem:** `docs/plans/BACKLOG-produto.md` (Fase 3c, DEP-04). Próximo da fila de deploy depois do front já estar hospedado (DEP-02).
> - **Prioridade:** alta — sem pipeline, todo deploy do front é manual (`npm run build` + `aws s3 sync` na mão), propenso a erro e a esquecer a invalidação do CloudFront.
> - **Esforço:** médio (workflow é direto; o grosso é configurar o OIDC corretamente — é a primeira vez que o projeto usa OIDC).
> - **Território / quem executa:** `.github/workflows/` + `financas_bot_telegram/infra/` → **Claude do back**. ⚠️ Um ajuste de 1 linha em `frontend/.env.production` é **território do front** — ver Coordenação.
> - **Branch:** `feature/dep-04-pipeline-deploy-front`, a partir de `develop`.
> - **Dependências:** DEP-02 (bucket S3 + distribuição CloudFront) — ✅ feita. DEP-03 (hostname `api.satyansaita.com`) — **soft**: o pipeline pode ser construído antes, mas o `VITE_API_BASE_URL` do build de prod precisa apontar pro hostname certo antes do front ir pro ar valer de fato.
> - **Riscos:**
>   1. **`aws s3 sync --delete` com path errado apaga o bucket.** Mitigação: conferir que o build sai em `frontend/dist/`; testar o primeiro run observando o output do sync.
>   2. **Trust do OIDC largo demais** (ex.: aceitar qualquer branch/repo). Mitigação: escopar o `sub` a `repo:<owner>/<repo>:ref:refs/heads/main`.
>   3. **`VITE_API_BASE_URL` desatualizado** — hoje `.env.production` aponta pro domínio antigo `api.finbot.dom.br`. Build assim joga o front contra uma API inexistente. Mitigação: corrigir pra `https://api.satyansaita.com` (ver Coordenação).
>   4. **OIDC provider é singleton por conta AWS.** Se alguém adicionar outro depois, conflita. Mitigação: criar uma vez aqui e referenciar; documentar.

---

## Contexto

O front compilado já está hospedado em S3 + CloudFront (DEP-02): bucket `finbot-frontend-prod-776658251579`, distribuição `E1WG4Q8MG3V9HY`, apex `satyansaita.com`. Falta o **pipeline** que, a cada mudança no front, builda e publica automaticamente: `npm ci` → `npm run build` → `aws s3 sync dist/` → invalidar o cache do CloudFront.

A autenticação com a AWS será via **OIDC** (OpenID Connect entre GitHub Actions e a AWS) — **sem secret de longa duração**. É um avanço sobre o `deploy.yml` do backend, que ainda usa `EC2_SSH_KEY`/`EC2_HOST` como secrets. (Migrar o back pra OIDC depois é um follow-up possível, fora desta task.)

## Decisão / abordagem

**Workflow novo `deploy-frontend.yml`, disparado em push pra `main` que toque `frontend/**`, autenticando via OIDC numa role IAM dedicada e escopada.** O Terraform cria o provider OIDC + a role + a policy mínima.

### Por que OIDC e não secret de chave AWS
Credencial estática (access key) no GitHub é risco permanente (vaza, não rotaciona sozinha). OIDC emite um token efêmero por execução, escopado ao repo/branch — sem segredo durável. É o padrão recomendado pra GitHub↔AWS.

### Escopo mínimo da role (princípio do menor privilégio)
A role só precisa de: `s3:ListBucket` no bucket, `s3:PutObject`/`s3:GetObject`/`s3:DeleteObject` em `bucket/*`, e `cloudfront:CreateInvalidation` na distribuição. Nada além disso.

## Escopo / arquivos

**Terraform (`financas_bot_telegram/infra/iam-github-oidc.tf` — criar):**

- `aws_iam_openid_connect_provider` pra `token.actions.githubusercontent.com` (audience `sts.amazonaws.com`). Singleton da conta — se já existir um, importar/referenciar em vez de duplicar.
- `aws_iam_role` (ex.: `finbot-prod-gha-frontend-deploy`) com `assume_role_policy` condicionando:
  - `token.actions.githubusercontent.com:aud = sts.amazonaws.com`
  - `token.actions.githubusercontent.com:sub = repo:<owner>/<repo>:ref:refs/heads/main` (preencher `<owner>/<repo>` com o slug real — pegar do `git remote`).
- `aws_iam_policy` + attachment com as permissões mínimas acima (referenciar o bucket do DEP-02 e o ARN da distribuição). Sugiro expor `frontend_bucket_name` e `cloudfront_distribution_id` via variáveis/outputs já existentes do DEP-02.
- Output do ARN da role (o workflow precisa dele).

**Workflow (`.github/workflows/deploy-frontend.yml` — criar):**

```yaml
name: Deploy frontend
on:
  push:
    branches: [main]
    paths: ['frontend/**']
permissions:
  id-token: write      # exigido pelo OIDC
  contents: read
jobs:
  deploy:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npm run build           # usa frontend/.env.production
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: <ARN da role do iam-github-oidc.tf>
          aws-region: us-east-1
      - name: Sync para o S3
        run: aws s3 sync dist/ s3://finbot-frontend-prod-776658251579 --delete
      - name: Invalidar CloudFront
        run: aws cloudfront create-invalidation --distribution-id E1WG4Q8MG3V9HY --paths "/*"
```

**Não tocar:** `deploy.yml` (deploy do back), `ci.yml` (gate CI-01), nada de código de produto do front/back.

## Critérios de aceitação

- [ ] `iam-github-oidc.tf` aplicado: provider OIDC + role escopada ao repo/branch `main` + policy mínima (S3 do bucket + invalidation da distribuição). `terraform plan` limpo após apply.
- [ ] `deploy-frontend.yml` criado, disparando **só** em push pra `main` com mudança em `frontend/**`.
- [ ] Um push de teste em `frontend/**` na `main`: workflow autentica via OIDC (sem secret de chave AWS), builda, faz `s3 sync` e invalida o CloudFront — verde fim a fim.
- [ ] Após o deploy, `https://satyansaita.com` serve o build novo (e some o `index.html` de smoke-test do DEP-02).
- [ ] Em ~5 min a mudança está em produção.
- [ ] A role NÃO tem permissão além do necessário (revisar a policy).
- [ ] Status report em `docs/sprints/01-mvp/status/DEP-04.md` com frontmatter válido (incluir o slug do repo usado no `sub` do OIDC e o ARN da role).

## Coordenação

- **⚠️ `frontend/.env.production` (território do front):** hoje está `VITE_API_BASE_URL=https://api.finbot.dom.br` (domínio antigo). Precisa virar `https://api.satyansaita.com` antes do build de prod valer. Duas saídas:
  - **(preferida)** o **front** corrige o `.env.production` numa task/PR própria (1 linha) — mantém o território limpo.
  - **(fallback)** o workflow seta `VITE_API_BASE_URL` como env no passo de build, sobrescrevendo o arquivo — evita o cross-território, mas esconde a config no YAML. Decidir com o planner.
- **DEP-03** precisa estar no ar pro `api.satyansaita.com` existir de verdade — senão o front buildado aponta pra um host morto. Construir o pipeline antes é ok; só não "valer" até DEP-03 + env corrigido.
- **Slug do repo:** o `sub` do OIDC precisa do `owner/repo` real do GitHub — o implementador pega do `git remote get-url origin`.
- Nota (follow-up, fora de escopo): o `deploy.yml` do back dispara em qualquer push pra `main` sem filtro de path — um push só de front também redeploya o back à toa. Adicionar `paths: ['financas_bot_telegram/**', ...]` no `deploy.yml` é uma melhoria pequena pra depois.

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build/lint/testes = `na` se não houver código de produto; `terraform plan` limpo + run verde do workflow são a evidência), status report em `docs/sprints/01-mvp/status/DEP-04.md` com frontmatter válido, e **revisão independente pelo Reviewer** antes do merge — toca em IAM e pipeline de produção (alto risco). Abrir PR pra `develop`; não mergear sozinho.

## Referências

- `docs/plans/BACKLOG-produto.md` (DEP-04 original)
- `docs/sprints/01-mvp/status/DEP-02.md` (bucket `finbot-frontend-prod-776658251579`, distribuição `E1WG4Q8MG3V9HY`, conta `776658251579`)
- `docs/plans/DEP-03-api-subdominio-proxy.md` (o hostname da API que o `.env.production` deve apontar)
- `.github/workflows/deploy.yml` (deploy do back via SSH — contraste; candidato a migrar pra OIDC depois)
- Docs: `aws-actions/configure-aws-credentials` (OIDC) e GitHub OIDC `sub` claims.
</content>
