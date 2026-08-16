# Sprint 04 — Instrumentação e qualidade

**Status:** 🟠 **em execução** (atualizado 2026-08-16) — **ferramental de qualidade completo**. Entregues: `QA-012` (PIT), `QA-013` (PMD), `QA-014` (JaCoCo) e `FIX-008` (gate de branch do CI). O escopo restante — instrumentação de medição e configuração dos agentes — continua sendo montado conforme o humano revisa os docs do experimento.

**Próxima task:** [`QA-015`](plans/QA-015-fortalecer-testes-revelados-pelos-pilotos.md) — fortalecer os testes fracos revelados pelos pilotos (item #2 do backlog). Plano pronto, aguardando dispatch.

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

- ✅ **Ferramental de qualidade** — PIT, PMD (sem Checkstyle), JaCoCo. Cada um com uma task-piloto de escopo reduzido cujo entregável inclui **leitura interpretada**, não só o relatório. **Concluída:** os três instalados, com baseline congelado em cada. Resta o item #2 (corrigir os testes fracos que os pilotos revelaram) e o item #8 (gate da convenção `*IntegrationTest`).
- ⬜ **Instrumentação de medição** — mecanismo de "classes tocadas" compartilhado (#7), hooks de subagente, `collect_usage.py`, teste de regressão do schema JSONL. Nada refinado ainda.
- ⬜ **Correção da configuração dos agentes** — pinagem de modelo (B1), `effort` explícito (B4), allowlist de permissões (B6). Nada refinado ainda. Soma-se aqui o resíduo da deleção do `.codex/`: **nenhum agente instrui a medir cobertura**.

## Fluxo de git desta sprint

Diferente da 02b (kaizen), esta sprint é **mista** — e o fluxo muda por task:

| Tipo de task | Território | Fluxo |
|---|---|---|
| Ferramental Maven (PIT, JaCoCo, PMD) | `financas_bot_telegram/pom.xml` | branch + PR + **Reviewer obrigatório** |
| Scripts de coleta | scripts do repositório | branch + PR + Reviewer |
| Configuração de subagente | `.claude/` | **`ai-engineer` ou humano**, com autorização explícita. Commit direto em `develop`, com `git show HEAD` conferido pelo humano antes do push |
| Documentação e decisões | `docs/` | commit direto em `develop` pelo planner |

**Prefixo de task:** `QA-NNN` para o ferramental de qualidade — é tooling que não altera comportamento de feature (ADR 0017). Numeração global e sequencial; a última usada foi **`QA-014`** (próxima: `QA-015`). Em FIX, a última foi **`FIX-008`**.

**Branch de integração:** `integration/04-instrumentacao-qualidade` — **criada e em uso**. Já levou três ciclos para `develop` (PRs #124, #128 e #131) e recebeu dois PRs de sincronização vindos de `develop` (#125 e #129).

## Decisões pendentes

1. ~~**Território de `.claude/`.**~~ ✅ **Resolvido em 2026-08-10:** território do humano e do `ai-engineer`. As demais instâncias só mexem com **autorização explícita do dev no controle**, pedida na hora e para a mudança específica. Registrado no `CLAUDE.md` §"Regra de ouro — isolamento por pasta".

   **Consequência para esta sprint:** as tasks de B1, B4 e B6 tocam `.claude/`. Devem ser executadas pelo **`ai-engineer`** ou pelo humano — **não** pelo `backend`. Isso muda a tabela de fluxo de git acima e precisa constar no dispatch.

2. **Ordem em relação ao marco zero.** 🟡 **parcialmente resolvida em 2026-08-16.**

   ~~O deploy `develop → main` está bloqueado pelo FIX-006.~~ **Essa dependência caiu** — produção está no ar desde 2026-08-09 (run `31341256203` verde) e FIX-006/FIX-007 estão em `main` via PR #122. O `README.md` do experimento, item 6, ainda registra a versão antiga.

   **Decidido:** o **item #2 vai antes da tag**, autorizado pelo humano. Motivo: ele toca `LegendaParser` e as strategies, que são exatamente as classes que a feature do experimento vai tocar; mexer nelas depois da tag muda o baseline no meio do voo e os runs deixam de ser comparáveis.

   **Ainda aberto:** se o **resto** da sprint (instrumentação de medição, config de agentes) também precisa preceder a tag, ou se só o que toca código de produção precisa.

## Estrutura

`plans/` · `status/` · `avaliacoes/` conforme ADR 0010.

## Próximos passos

Atualizado em 2026-08-16. Os passos 1 a 5 originais (revisão dos docs, refino dos pilotos, criação da branch de integração, dispatch do PIT) estão **cumpridos**.

1. ✅ **Item #2 refinado** como [`QA-015`](plans/QA-015-fortalecer-testes-revelados-pelos-pilotos.md) em 2026-08-16 — `pronto-pra-execucao`, **aguardando dispatch para o `backend`**. Primeira task sob `mutation_gate: true`.
2. **Fechar a decisão ArchUnit × reflection puro** (item #8) com o humano, para destravar o gate da convenção `*IntegrationTest`.
3. **Corrigir o resíduo do `.codex/`:** nenhum agente instrui a medir cobertura. Território `.claude/` — exige autorização explícita; executor a definir (`ai-engineer` ou planner autorizado).
4. Refinar a frente de instrumentação de medição, começando pelo mecanismo de "classes tocadas" (#7) — é pré-requisito de quatro métricas.
5. Fechar o escopo restante da decisão 2 (o que mais precede a tag do marco zero) e corrigir o item 6 do `README.md` do experimento, que ainda cita o FIX-006 como bloqueador.
