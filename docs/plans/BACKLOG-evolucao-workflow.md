# Backlog — evolução do workflow (melhorias de processo)

Itens de **profissionalização do workflow** que saíram da auditoria de 2026-05-26 (ver ADR `0004`). Não são features do produto (essas estão em `FASE-3-VISUALIZACAO.md`) — são melhorias de processo/tooling. Ordem = prioridade aproximada de impacto.

> Guardado pra sobreviver à separação de sessões. Quando um item virar task formal, promover pra um plano próprio e marcar aqui.

## Em voo (já planejados)

- **CI-01 — gate de CI no PR pra `develop`.** Plano em `docs/plans/CI-01-gate-pr-develop.md`. Pendente: execução pelo back + **decisão de processo do humano** (adotar PR→develop + branch protection, senão o gate é só informativo).
- **FIX-gitattributes-eol.** Plano escrito; pendente execução (mata o drift CRLF/LF).

## A escrever (do codegen pra frente)

### 1. Codegen do `tipos.ts` a partir do OpenAPI
**O quê:** gerar os tipos TS do front a partir do `/v3/api-docs` (springdoc) em vez de manter `tipos.ts` à mão. **Por quê:** mata a classe de bug de drift front/back (o `mesAtual→mes` da FE-12). **Esforço:** médio. **Prioridade:** alta. Decidir ferramenta (ex: `openapi-typescript`) e onde plugar (script npm + talvez no CI).

### 2. `docs/STATE.md` — doc de orientação ("onde estamos agora")
**O quê:** um resumo curto e vivo: fase atual, o que está em `develop`, o que está em voo, próximos passos. **Por quê:** agente que começa frio re-deriva contexto; isso orienta rápido. **Esforço:** baixo. **Prioridade:** média-alta (fica ainda mais útil com sessões especializadas).

### 3. Script de métricas sobre o frontmatter dos status reports
**O quê:** script que lê `docs/status/*.md`, extrai o frontmatter e agrega (tasks por `estado`, `desvios` por área, soma de `testes_total`, taxa de gate-fail). **Por quê:** "falta de métricas formais". **Esforço:** baixo-médio. **Prioridade:** média. Escolher 2-3 métricas que mudam comportamento (cycle time, retrabalho, gate-fail) — não virar teatro.

### 4. Formalizar o template de task (intake)
**O quê:** adicionar ao padrão de plano os campos que faltam: **origem**, **riscos**, **prioridade** (já temos contexto/critérios/dependências/branch). **Por quê:** estruturar o input contract (mesmo princípio do status report). **Esforço:** baixo. **Prioridade:** média.

### 5. ADRs retroativos pras decisões arquiteturais já tomadas
**O quê:** promover a ADR decisões que ficaram só em `aprendizado/`/`CLAUDE.md`. Candidatas: hostnames separados front/API (`front-api-hostnames-separados.md`), módulo Terraform único, esquema de reporting com gates. **Por quê:** ADR é a decisão canônica (ADR 0004). **Esforço:** baixo cada. **Prioridade:** média.

### 6. Arquivos de papel pra sessões especializadas (`docs/roles/`) — ✅ feito (estrutura)
**O quê:** instruções por papel pra reduzir context dilution e viés do planejador. **Feito:** ADR `0005` + arquivos `docs/roles/{planner,backend,frontend,reviewer}.md`. Localização ficou `docs/roles/` (não `.claude/`, que é protegido pra escrita da sessão de planejamento). **Pendente:** adotar o Reviewer no fluxo real (sessão separada) e medir o ganho; `architect.md`/`qa.md` adiados.

### 7. Pequeno — incluir prefixo `CI` no regex de task-id
**O quê:** atualizar o regex em `PRE-MERGE-CHECKLIST.md` pra aceitar `CI-`. **Esforço:** trivial. **Prioridade:** baixa.

## Fora deste backlog (rastreado em outro lugar)

- Deploy DEP-03 a DEP-06 → `FASE-3-VISUALIZACAO.md`.
- Headers de segurança no CloudFront, rotação do token Telegram, `keystore_password` no Secrets Manager → `PENDENCIAS-TECNICAS.md`.
