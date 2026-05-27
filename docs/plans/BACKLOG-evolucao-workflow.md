# Backlog — evolução do workflow (melhorias de processo)

Itens de **profissionalização do workflow** que saíram da auditoria de 2026-05-26 (ver ADR `0004`). Não são features do produto (essas estão em `FASE-3-VISUALIZACAO.md`) — são melhorias de processo/tooling. Ordem = prioridade aproximada de impacto.

> Guardado pra sobreviver à separação de sessões. Quando um item virar task formal, promover pra um plano próprio e marcar aqui.

## Em voo (já planejados)

- **CI-01 — gate de CI no PR pra `develop`.** Plano em `docs/plans/CI-01-gate-pr-develop.md`. **Decisão (2026-05-26): PR→develop com o CI rodando no PR; branch protection adiada** → gate informativo, não bloqueante por ora. Pendente: execução do `ci.yml` pelo back. (Ligar branch protection no GitHub quando quiser tornar bloqueante.)
- **FIX-gitattributes-eol.** ✅ **Feito** (ver `docs/status/FIX-gitattributes-eol.md`) — drift CRLF/LF eliminado, `terraform plan` limpo. Pendente só o PR pra `develop`.
- **FE-13 — codegen do `tipos.ts` a partir do OpenAPI.** ✅ Plano escrito em `docs/plans/FE-13-codegen-tipos-openapi.md` (promovido do item #1 abaixo). Pendente: execução pelo front.

## A escrever (do codegen pra frente)

### 1. Codegen do `tipos.ts` a partir do OpenAPI — ✅ promovido pra plano
**O quê:** gerar os tipos TS do front a partir do `/v3/api-docs` (springdoc) em vez de manter `tipos.ts` à mão. **Por quê:** mata a classe de bug de drift front/back (o `mesAtual→mes` da FE-12). **Esforço:** médio. **Prioridade:** alta. **Plano:** `docs/plans/FE-13-codegen-tipos-openapi.md` (ferramenta escolhida: `openapi-typescript`; estratégia: snapshot do spec versionado + geração do arquivo; check de drift no CI fica como fase 2). Pendente execução pelo front.

### 2. `docs/STATE.md` — doc de orientação ("onde estamos agora") — ✅ feito
**O quê:** um resumo curto e vivo: fase atual, o que está em `develop`, o que está em voo, próximos passos. **Por quê:** agente que começa frio re-deriva contexto; isso orienta rápido. **Feito:** `docs/STATE.md` criado (2026-05-26). Manter atualizado a cada mudança relevante de estado.

### 3. Script de métricas sobre o frontmatter dos status reports — ✅ feito
**O quê:** script que lê `docs/status/*.md`, extrai o frontmatter e agrega (tasks por `estado`, `desvios` por área, soma de `testes_total`, taxa de gate-fail). **Feito:** `docs/scripts/metricas_status.py` (+ `docs/scripts/README.md`). Métricas escolhidas: cobertura do schema canônico, estado das tasks, gate-fails, desvios/pendências, soma de testes. **Achado da 1ª rodada:** só 12/36 reports têm frontmatter e só 1 (DEP-02) segue o schema canônico — os demais são anteriores ao `_TEMPLATE`/ADR 0007. O schema vale dos novos pra frente; a métrica serve pra ver a adoção subir.

### 4. Formalizar o template de task (intake) — ✅ feito
**O quê:** adicionar ao padrão de plano os campos que faltam: **origem**, **riscos**, **prioridade** (já temos contexto/critérios/dependências/branch). **Por quê:** estruturar o input contract (mesmo princípio do status report). **Feito:** `docs/plans/_TEMPLATE.md` criado com bloco de Intake (origem, prioridade, esforço, território/quem executa, branch, dependências, riscos) + seções de corpo. O plano `FE-13` já segue o formato.

### 5. ADRs retroativos pras decisões arquiteturais já tomadas — ✅ feito (com 1 ressalva)
**O quê:** promover a ADR decisões que ficaram só em `aprendizado/`/`CLAUDE.md`. **Feito:**
- **ADR 0006** — front no apex + API em subdomínio (hostnames separados). `Accepted`.
- **ADR 0007** — reporting com gates / status report como output contract. `Accepted`.
- **ADR 0008** — Terraform como módulo raiz único. `Proposed` — o fato está verificado, mas a justificativa original não estava registrada; **aguarda homologação do humano** pra virar `Accepted`.

### 6. Arquivos de papel pra sessões especializadas (`docs/roles/`) — ✅ feito + Reviewer adotado
**O quê:** instruções por papel pra reduzir context dilution e viés do planejador. **Feito:** ADR `0005` + arquivos `docs/roles/{planner,backend,frontend,reviewer}.md`. Localização ficou `docs/roles/` (não `.claude/`, que é protegido pra escrita da sessão de planejamento). **Reviewer adotado (2026-05-26):** revisão independente em sessão separada passa a ser obrigatória pra toda task antes do merge (registrado no `CLAUDE.md`, seção pré-merge). `architect.md`/`qa.md` seguem adiados.

### 7. Pequeno — incluir prefixo `CI` no regex de task-id — ✅ feito
**O quê:** atualizar o regex em `PRE-MERGE-CHECKLIST.md` pra aceitar `CI-`. **Feito:** regex de `branch_convencao` no `PRE-MERGE-CHECKLIST.md` e o comentário de schema no `docs/status/_TEMPLATE.md` agora incluem `ci`/`CI`.

## Fora deste backlog (rastreado em outro lugar)

- Deploy DEP-03 a DEP-06 → `FASE-3-VISUALIZACAO.md`.
- Headers de segurança no CloudFront, rotação do token Telegram, `keystore_password` no Secrets Manager → `PENDENCIAS-TECNICAS.md`.
