# FIX — Normalizar fim de linha (.gitattributes) e eliminar drift CRLF/LF

## Contexto

Diffs com line-ending oscilando (CRLF↔LF) já geraram ruído (a assinatura "N adicionadas / N removidas" em arquivos não alterados de fato) e, no DEP-02, **drift recorrente de `user_data` da EC2** no `terraform plan` (update in-place metadata-only que reaparece a cada plan). Causa: arquivos commitados ora com CRLF (Windows) ora com LF, sem regra consistente. O Terraform lê o `user_data` byte a byte, então o fim de linha muda o hash.

## Objetivo

Fixar fim de linha como **LF** no working tree pra todo o repo via `.gitattributes` (commitado, vale pra todos independente do `core.autocrlf` local) e **renormalizar** os arquivos já versionados pra limpar o CRLF existente.

## Branch

`fix/gitattributes-eol` — nova, a partir de `develop`.

> **EXCEÇÃO DE TERRITÓRIO:** este fix é uma normalização **repo-wide** — o `git add --renormalize` reescreve fins de linha em arquivos de `frontend/`, `financas_bot_telegram/`, `docs/` e raiz. Por natureza ele cruza territórios; não dá pra escopar numa pasta só. Justificativa registrada conforme a regra de precedência do CLAUDE.md. Por isso deve ser um **commit único e dedicado**, sem misturar com mudança funcional.

## Pré-condição (timing)

Fazer **agora**, enquanto `develop` é a única linha ativa (front e back já mergeados, DEP-03 não começou). Branches de feature abertas no momento do renormalize tendem a gerar conflito de fim de linha no merge — então o ideal é o repo estar "quieto", que é o caso.

## Passo 1 — Criar `.gitattributes` na raiz do repo

```gitattributes
# Fim de linha: normaliza tudo pra LF no working tree.
# Alvo de deploy é Linux; Terraform/bash/Spring preferem LF, e o Terraform lê
# user_data byte a byte — CRLF causa drift. Cobre .tf, .tfvars, .sh, .properties,
# .ts, .tsx, .java, .yml, .md, etc. via a regra global abaixo.
* text=auto eol=lf

# Binários — git nunca converte
*.png      binary
*.jpg      binary
*.jpeg     binary
*.gif      binary
*.ico      binary
*.jar      binary
*.p12      binary
*.keystore binary
*.woff     binary
*.woff2    binary

# Arquivos que (se existirem) precisam de CRLF no Windows
*.bat text eol=crlf
*.cmd text eol=crlf
```

## Passo 2 — Renormalizar e commitar

Executado **pelo humano no PowerShell** (regra do projeto: git não roda pelo sandbox). Comandos:

```powershell
git checkout develop
git pull
git checkout -b fix/gitattributes-eol

# cria/edita o .gitattributes (passo 1)

git add .gitattributes
git add --renormalize .
git status        # confere o que mudou — espere MUITOS arquivos (só fim de linha)
git commit -m "fix: normaliza fim de linha pra LF via .gitattributes"
```

> O `git status` vai mostrar muitos arquivos modificados — isso é **esperado** (é só fim de linha). É o motivo de ser um commit isolado.

## Passo 3 — Verificar que o drift sumiu

```powershell
cd financas_bot_telegram/infra
terraform plan -var-file=prod.tfvars
```

- O plan **não** deve mais mostrar o `update in-place` do `aws_instance.finbot_app` por mudança de hash do `user_data`.
- Opcional: `git ls-files --eol` deve mostrar `i/lf` (LF no índice) pros arquivos de texto.

## Critérios de aceitação

- `.gitattributes` na raiz com as regras acima.
- Um **único commit** de renormalização (só fim de linha, zero mudança funcional).
- `terraform plan -var-file=prod.tfvars` limpo quanto ao `user_data` da EC2 (o drift CRLF/LF some). Se sobrar algum outro drift não relacionado, **parar e investigar** antes de aplicar.
- Nenhum arquivo teve conteúdo funcional alterado (revisar o diff por amostragem: deve ser só fim de linha).

## Não-objetivos

- Não aplicar (`terraform apply`) nada aqui — o fix é só de repo/linha. O plan limpo já é a prova.
- Não mexer em conteúdo de código.

## Coordenação

- Território: arquivo de raiz (`.gitattributes`) + renormalização repo-wide (exceção declarada acima). Pode ser executado pelo humano direto (são poucos comandos git) ou por uma instância designada — mas o git roda **no PowerShell**, nunca pelo sandbox.
- Status report em `docs/sprints/01-mvp/status/FIX-gitattributes-eol.md` (frontmatter: `gates.build/lint/testes = na`; registrar o resultado do `terraform plan` como evidência do gate).
- Relacionado: `docs/aprendizado/git-line-endings-crlf-lf.md`.
