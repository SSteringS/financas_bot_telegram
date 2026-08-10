# Sprint 04 — Instrumentação e qualidade

**Status:** 🟠 **em construção** — escopo sendo montado conforme o humano revisa os docs do experimento. Nenhuma task dispatchada.

**Objetivo:** instalar o ferramental de qualidade e a instrumentação de medição que o repositório ainda não tem, e corrigir a configuração dos subagentes que hoje impede medir modelo por papel.

**Entrega que define "pronto":** um run completo do fluxo (planner → backend → reviewer → QA) executado de ponta a ponta, com custo por papel coletado, modelo comprovadamente pinado, e métricas de qualidade (cobertura, mutation score, lint) gerando número real em vez de `na`.

## Por que existe

A revisão das métricas do experimento de alocação de modelo (`docs/experiments/models-claude-experiment/`) mostrou que a maior parte do que ele exige **não existe**:

- **Ferramental de qualidade ausente:** JaCoCo, PIT e PMD não estão no `pom.xml`. O comando `jacoco:report` que o agente QA é instruído a rodar **falha hoje**, e por isso `cobertura_pct: na` aparece em 100% dos status reports.
- **Instrumentação de custo inexistente:** zero hooks configurados; não há coleta de tokens ou tempo por papel.
- **Bloqueador fatal de validade:** `planner`, `backend`, `reviewer` e `qa-test-specialist` declaram `model` inválido. Se caírem em `inherit`, todos os runs medem a mesma célula.

Nada disso é andaime descartável. JaCoCo, PIT, PMD e os hooks **permanecem depois que o experimento acabar** — é capacidade permanente do repositório. Daí o nome ser "instrumentação e qualidade", não "preparação do experimento".

## Objetivo secundário — declarado pelo humano

**Várias destas ferramentas serão de primeiro uso.** As tasks devem produzir **entendimento**, não só artefato. Onde couber, o entregável inclui leitura interpretada do resultado, não apenas o resultado bruto. Uma task que instala PIT e reporta "instalado, verde" **não cumpre** o objetivo desta sprint.

## Escopo

> Em construção. O backlog vivo, com detalhamento e itens ainda não refinados, está em
> [`backlog-s04.md`](backlog-s04.md). Conforme os itens forem refinados, viram planos em `plans/`.

**A lista de itens vive apenas no backlog** — não duplicada aqui, para não divergir. Três frentes:

- **Ferramental de qualidade** — PIT, PMD (sem Checkstyle), JaCoCo. Cada um com uma task-piloto de escopo reduzido cujo entregável inclui **leitura interpretada**, não só o relatório.
- **Instrumentação de medição** — mecanismo de "classes tocadas" compartilhado, hooks de subagente, `collect_usage.py`, teste de regressão do schema JSONL.
- **Correção da configuração dos agentes** — pinagem de modelo (B1), `effort` explícito (B4), allowlist de permissões (B6).

## Fluxo de git desta sprint

Diferente da 02b (kaizen), esta sprint é **mista** — e o fluxo muda por task:

| Tipo de task | Território | Fluxo |
|---|---|---|
| Ferramental Maven (PIT, JaCoCo, PMD) | `financas_bot_telegram/pom.xml` | branch + PR + **Reviewer obrigatório** |
| Scripts de coleta | scripts do repositório | branch + PR + Reviewer |
| Configuração de subagente | `.claude/` | **`ai-engineer` ou humano**, com autorização explícita. Commit direto em `develop`, com `git show HEAD` conferido pelo humano antes do push |
| Documentação e decisões | `docs/` | commit direto em `develop` pelo planner |

**Prefixo de task:** `QA-NNN` para o ferramental de qualidade — é tooling que não altera comportamento de feature (ADR 0017). Numeração global e sequencial; a última usada foi `QA-011`.

**Branch de integração:** criar `integration/04-instrumentacao-qualidade` antes do primeiro dispatch, conforme o padrão do repositório.

## Decisões pendentes

1. ~~**Território de `.claude/`.**~~ ✅ **Resolvido em 2026-08-10:** território do humano e do `ai-engineer`. As demais instâncias só mexem com **autorização explícita do dev no controle**, pedida na hora e para a mudança específica. Registrado no `CLAUDE.md` §"Regra de ouro — isolamento por pasta".

   **Consequência para esta sprint:** as tasks de B1, B4 e B6 tocam `.claude/`. Devem ser executadas pelo **`ai-engineer`** ou pelo humano — **não** pelo `backend`. Isso muda a tabela de fluxo de git acima e precisa constar no dispatch.

2. **Ordem em relação ao marco zero.** O deploy `develop → main` está bloqueado pelo FIX-006. A tag do baseline precisa vir **depois** dele. Definir se esta sprint inteira precede a tag, ou se só as tasks que tocam código de produção precisam.

## Estrutura

`plans/` · `status/` · `avaliacoes/` conforme ADR 0010.

## Próximos passos

1. Humano termina a revisão dos docs do experimento, acumulando itens no backlog.
2. Planner refina os itens em planos individuais `QA-NNN`.
3. Resolver as duas decisões pendentes acima.
4. Criar `integration/04-instrumentacao-qualidade`.
5. Dispatch, começando pelo piloto do PIT.
