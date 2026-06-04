# DISPATCH — Engenheiro de IA: implementar ADR 0019

> **Modo:** implementação de ADR já homologada — não é sessão de decisão.
> Não aplicar Output Format de 7 seções. Executar as mudanças listadas abaixo.
>
> **ADR:** `docs/decisions/0019-workflow-reviewer-qa-loop.md` (Accepted, 2026-06-04)
> **Objetivo:** materializar o novo workflow reviewer→QA em todos os artefatos de role e processo.

---

## Contexto resumido (leia a ADR completa antes de começar)

A ADR 0019 define:
1. **Loop por task:** `implementar → reviewer (subagente) → QA (subagente) → push`. Ciclo antes de abrir PR, inclusive dentro de batches/overnight.
2. **Limites:** reviewer sozinho = 3 rounds; loop reviewer+QA completo = 2 rounds. Ao estourar → parar + `estado: bloqueado` + escalar humano.
3. **QA retenta suíte completa** no retry — nunca só o que falhou.
4. **Protocolo de chamada sem viés** — templates canônicos obrigatórios. Proibido pré-justificar decisões ou ancorar aprovação no prompt.
5. **Batch/overnight:** o ciclo acontece task por task, não só no final.

---

## O que fazer — lista de mudanças

### 1. `docs/roles/backend.md`

**Estado atual:** o checklist menciona "Chamou o Reviewer" e "Chamou o qa-test-specialist", mas sem protocolo de chamada, sem round limits, sem template neutro.

**Mudanças:**

a) Adicionar seção nova **antes** do checklist, chamada `## Protocolo de chamada — Reviewer e QA`:

```markdown
## Protocolo de chamada — Reviewer e QA (ADR 0019)

Chamar Reviewer e QA é **obrigatório** antes de abrir PR. O protocolo garante chamada sem viés.

### Round limits

| Ciclo | Limite | Ao estourar |
|---|---|---|
| Reviewer sozinho | 3 rounds | Parar. Status report com `estado: bloqueado`. Não abrir PR. Escalar humano. |
| Loop reviewer+QA completo | 2 loops | Idem. |

Em re-execução do QA (após correção): rodar **a suíte completa**, nunca só os fluxos que falharam.

### Template de chamada do Reviewer (usar literalmente — sem adicionar contexto positivo)

```
Revisar a implementação da task <TASK-ID> — sprint <NN>.
Arquivos modificados: <lista objetiva, sem julgamento>
Status report: <path>
Role: docs/roles/reviewer.md

Procurar problemas, inconsistências com padrões do projeto e violações arquiteturais.
Não confirmar que está correto — questionar o que merecer ser questionado.
Não há contexto adicional além do que está no status report e nos arquivos listados.
```

**Proibido no prompt:** afirmar que o trabalho está correto, pré-justificar decisões, usar linguagem que ancora aprovação ("verificar se está ok", "confirmar que segui o padrão").

### Template de chamada do QA (apenas se `fluxos_qa ≠ []` no plano)

```
Executar fluxos QA para a task <TASK-ID> — sprint <NN>.
fluxos_qa do plano: <lista do frontmatter do plano>
Avaliação do reviewer: <path da avaliação>
Role: docs/roles/qa.md

Executar todos os fluxos listados mais qualquer outro que julgar necessário.
Reportar tudo que encontrar — não confirmar aprovação.
Não há contexto adicional além do que está na avaliação e nos fluxos definidos.
```

Se `fluxos_qa: []` no plano: pular este passo.
```

b) Substituir no checklist as linhas atuais sobre reviewer e QA:

**De:**
```
- [ ] **Chamou o Reviewer** (Agent `reviewer`) e aguardou o veredito. Aprovado ou aprovado-com-observações = pode mergear; reprovado = corrige e recicla.
- [ ] **Chamou o qa-test-specialist** (Agent `qa-test-specialist`) e aguardou a análise de gaps. Gaps bloqueantes = corrige antes do merge; observações = registra no status report.

> **Regra:** não mergear sem o ok explícito do Reviewer **e** do qa-test-specialist. Chamar os dois é responsabilidade do implementador — não esperar que alguém peça.
```

**Para:**
```
- [ ] Ciclo reviewer→QA concluído conforme ADR 0019 e protocolo acima:
  - Reviewer OK (máx. 3 rounds) → se `fluxos_qa ≠ []`: QA OK (máx. 2 loops completos reviewer+QA)
  - Se limite de rounds atingido com issues críticos abertos: `estado: bloqueado` no status report, não abrir PR, escalar humano
- [ ] Chamada do reviewer usou o template neutro (sem pré-justificativas, sem ancoragem de aprovação)

> **Regra:** não mergear sem veredito final aprovado do reviewer. Se fluxos_qa ≠ []: também não mergear sem QA OK. Responsabilidade do implementador — não esperar que alguém peça.
```

---

### 2. `docs/roles/frontend.md`

**Estado atual:** o checklist NÃO menciona reviewer nem QA. A seção termina em "Passou pelo PRE-MERGE-CHECKLIST.md" sem referência ao ciclo.

**Mudanças:**

a) Adicionar a mesma seção `## Protocolo de chamada — Reviewer e QA (ADR 0019)` que o backend (conteúdo idêntico — copiar literalmente).

b) Adicionar ao checklist (após o item "Passou pelo PRE-MERGE-CHECKLIST.md"):

```
- [ ] Ciclo reviewer→QA concluído conforme ADR 0019 e protocolo acima:
  - Reviewer OK (máx. 3 rounds) → se `fluxos_qa ≠ []`: QA OK (máx. 2 loops completos reviewer+QA)
  - Se limite de rounds atingido com issues críticos abertos: `estado: bloqueado` no status report, não abrir PR, escalar humano
- [ ] Chamada do reviewer usou o template neutro (sem pré-justificativas, sem ancoragem de aprovação)

> **Regra:** não mergear sem veredito final aprovado do reviewer. Se fluxos_qa ≠ []: também não mergear sem QA OK. Responsabilidade do implementador — não esperar que alguém peça.
```

c) Atualizar a linha "Ler sempre" para incluir `docs/decisions/0019`.

---

### 3. `docs/roles/reviewer.md`

**Estado atual:** descreve o que fazer ao revisar, mas não orienta como interpretar uma chamada recebida — especialmente sem presumir contexto além do que está no prompt.

**Mudanças:**

a) Adicionar nova seção **no início do arquivo**, logo após o bloco de objetivo, chamada `## Como interpretar a chamada recebida`:

```markdown
## Como interpretar a chamada recebida (ADR 0019)

O reviewer é chamado pelo implementador via template neutro (ADR 0019 §4). Regras ao receber:

- **Não presumir qualidade positiva.** O fato de ter sido chamado não implica que o trabalho está correto. Partir do princípio de que pode haver problemas — é o trabalho achar ou descartar.
- **Contexto válido = só o que está no prompt de chamada.** Se o prompt não justificou uma decisão, o reviewer deve questionar essa decisão. Contexto implícito ("provavelmente seguiu o padrão") não vale.
- **Rounds:** pode ser chamado múltiplas vezes na mesma task (máx. 3). Em cada round, ler o diff novamente — correções podem ter introduzido novos problemas. Não assumir que o que passou antes continua passando.
- **Issue crítico = bloqueante para merge.** Definir claramente na avaliação o que é crítico (bloqueia) vs observação (registrar mas não bloqueia). O implementador decide se corrige ou escalas ao humano.
```

b) Atualizar "Ler sempre" para incluir `docs/decisions/0019`.

---

### 4. `docs/roles/qa.md` — CRIAR (não existe)

Criar `docs/roles/qa.md` com o seguinte conteúdo:

```markdown
---
name: qa-test-specialist
description: Use para executar fluxos de teste automatizado definidos em `fluxos_qa` de um plano de task, após o reviewer dar OK. Dispara quando há uma avaliação de reviewer aprovada e fluxos_qa ≠ [] no plano. NÃO use para implementação (back/front), revisão de código (reviewer) ou planejamento (planner).
tools: Read, Grep, Glob, Bash, AskUserQuestion
skills: []
skills_available: [qualidade-de-testes]
---

# Papel: QA Test Specialist

> Delta sobre o `CLAUDE.md` (regras globais valem sempre). Não duplique aqui o que já está lá.

## Objetivo

Executar os fluxos de teste automatizado definidos no plano da task (`fluxos_qa`) e quaisquer outros que julgar necessários, **após o reviewer ter dado OK**. Reportar tudo que encontrar — issues críticos bloqueiam o merge.

## Como interpretar a chamada recebida (ADR 0019)

O QA é chamado pelo implementador via template neutro (ADR 0019 §4). Regras ao receber:

- **Não confirmar aprovação.** Executar os fluxos e reportar o que for encontrado — positivo e negativo. O fato de ser chamado não implica que vai passar.
- **Contexto válido = só o que está na avaliação do reviewer e nos fluxos definidos.** Não presumir que o código está correto porque o reviewer aprovou o código — QA valida comportamento em execução.
- **Rounds:** pode ser chamado múltiplas vezes na mesma task (máx. 2 loops completos reviewer+QA). Em cada round, rodar a suíte completa — nunca só os fluxos que falharam na rodada anterior.
- **Suíte completa:** em re-execução, rodar **todos** os fluxos do plano mais os adicionais que incluiu na primeira rodada. Correções têm efeitos colaterais.

## Faz

- Executa todos os fluxos listados em `fluxos_qa` do plano da task.
- Identifica fluxos adicionais relevantes que o plano não definiu (lacunas de cobertura) e executa.
- Reporta: issues críticos (bloqueiam merge), recomendados (não bloqueiam), oportunidades (cobertura extra).
- Preenche as seções 7.1–7.5 da avaliação em `docs/sprints/<NN>/avaliacoes/<TASK-ID>-<slug>.md`.
- Atualiza os campos `veredito_qa`, `fluxos_qa_executados`, `fluxos_qa_adicionar`, `fluxos_qa_remover` no frontmatter da avaliação.

## NÃO Faz

- **Não implementa fixes** — reporta. Correção volta pro implementador.
- **Não aprova sem executar** — se não rodou o fluxo, não aprova.
- **Não pula a suíte completa** em re-execução — mesmo que só 1 fluxo tenha falhado antes, rodar tudo.
- **Não modifica código de produção** nem testes de forma permanente sem o ciclo implementador→reviewer.

## Definição de issue crítico vs observação

| Tipo | Definição | Efeito no merge |
|---|---|---|
| Crítico | Comportamento errado em produção; teste que deveria passar falha; regressão confirmada | Bloqueia merge — implementador deve corrigir |
| Recomendado | Cobertura fraca, edge case sem teste, flaky identificado | Não bloqueia, mas registrar no status report |
| Oportunidade | Fluxo adicional útil que o plano não previu | Sugerir para o planner via `fluxos_qa_adicionar` |

## Veredito

- **`aprovado`:** zero issues críticos, todos os fluxos passaram.
- **`aprovado_com_ajustes`:** zero issues críticos, mas há recomendados ou oportunidades identificadas.
- **`reprovado`:** um ou mais issues críticos. Implementador corrige → reviewer → QA de novo (máx. 2 loops completos).

## Checklist do papel

- [ ] Li a avaliação do reviewer (seções 1-6) antes de executar.
- [ ] Executei todos os fluxos de `fluxos_qa` do plano.
- [ ] Identifiquei e executei fluxos adicionais relevantes.
- [ ] Reportei issues críticos, recomendados e oportunidades separadamente.
- [ ] Preenchi seções 7.1–7.5 na avaliação.
- [ ] Atualizei frontmatter: `veredito_qa`, `fluxos_qa_executados`, `fluxos_qa_adicionar`, `fluxos_qa_remover`.
- [ ] Veredito explícito: aprovado / aprovado_com_ajustes / reprovado — com os porquês.

## Ler sempre

`CLAUDE.md` · o plano da task · a avaliação do reviewer · `docs/templates/_TEMPLATE-avaliacao.md` · `docs/decisions/0019`
```

---

### 5. `CLAUDE.md` — seção "Definição de pronto / pré-merge"

**Localizar** a seção que contém:
```
- **Definição de pronto / pré-merge:** antes de mergear, (1) passar pelos gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build, lint, testes, convenção de branch, território); (2) escrever o status report com frontmatter válido conforme `docs/templates/_TEMPLATE-status.md`; (3) **revisão independente pelo Reviewer** — sessão separada (ADR 0005), obrigatória pra toda task antes do merge (decisão 2026-05-26). `estado: concluido` só vale com todos os gates ok e zero pendência.
```

**Substituir por:**
```
- **Definição de pronto / pré-merge:** antes de mergear, (1) passar pelos gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build, lint, testes, convenção de branch, território); (2) escrever o status report com frontmatter válido conforme `docs/templates/_TEMPLATE-status.md`; (3) **ciclo reviewer→QA** conforme ADR 0019 — reviewer (subagente, chamada neutra, máx. 3 rounds) → se `fluxos_qa ≠ []`: QA (subagente, suíte completa, máx. 2 loops completos). Ao estourar o limite com issues críticos abertos: `estado: bloqueado`, sem PR, escalar humano. `estado: concluido` só vale com todos os gates ok, ciclo reviewer→QA concluído e zero pendência.
```

---

### 6. `docs/runbooks/PRE-MERGE-CHECKLIST.md`

**Na seção "Checklist operacional"**, adicionar após o item atual sobre status report:

```
- [ ] **Ciclo reviewer→QA concluído** (ADR 0019): reviewer subagente deu OK (máx. 3 rounds) · se `fluxos_qa ≠ []` no plano: QA subagente deu OK (máx. 2 loops completos) · se limite atingido com issues críticos: `estado: bloqueado`, não abrir PR
- [ ] Chamada do reviewer usou template neutro (sem pré-justificativas, sem ancoragem de aprovação)
```

**Na seção "Checklist do Reviewer"**, adicionar no final:

```
- [ ] Chamada foi recebida via template neutro — não presumir contexto positivo além do que estava no prompt
- [ ] Se este é round 2 ou 3: releu o diff desde o início — não assumiu que o que passou antes continua passando
```

---

### 7. `docs/sprints/03-folha-pagamento/plans/DISPATCH-OVERNIGHT-BACK.md`

**Atualizar o PROTOCOLO** (seção "## PROTOCOLO — repetir para cada task") para incluir o ciclo reviewer→QA após o commit e antes do PR.

**Substituir:**
```
7. git add <arquivos do território> docs/sprints/03-folha-pagamento/status/<TASK-ID>-*.md
8. git commit -m "<mensagem definida no plano>"
9. gh pr create \
     --base integration/03-folha-pagamento \
     ...
10. gh pr merge --squash --delete-branch
11. git fetch origin
12. git log origin/integration/03-folha-pagamento --oneline | head -5  ← confirmar merge
13. → PRÓXIMA TASK
```

**Por:**
```
7. git add <arquivos do território> docs/sprints/03-folha-pagamento/status/<TASK-ID>-*.md
8. git commit -m "<mensagem definida no plano>"
9. [CICLO REVIEWER — máx. 3 rounds]
   Chamar subagente reviewer com template neutro (ver docs/roles/backend.md §Protocolo):
     "Revisar a implementação da task <TASK-ID> — sprint 03.
      Arquivos modificados: <lista objetiva>
      Status report: docs/sprints/03-folha-pagamento/status/<TASK-ID>-*.md
      Role: docs/roles/reviewer.md
      Procurar problemas — não confirmar que está correto."
   - Se reviewer encontrar issues críticos: corrigir → novo commit → voltar ao passo 9 (máx. 3 rounds no total)
   - Se limite de 3 rounds atingido com issues críticos abertos: PARAR (ver Regra de Parada)
   - Se OK (aprovado ou aprovado_com_observações): continuar
   Nota: todas as tasks BE desta sprint têm fluxos_qa: [] → step QA não se aplica aqui
10. gh pr create \
      --base integration/03-folha-pagamento \
      ...
11. gh pr merge --squash --delete-branch
12. git fetch origin
13. git log origin/integration/03-folha-pagamento --oneline | head -5  ← confirmar merge
14. → PRÓXIMA TASK
```

**Atualizar também a REGRA DE PARADA** para incluir:
```
- Reviewer não convergiu em 3 rounds (issues críticos ainda presentes no round 3)
```

---

### 8. `docs/sprints/03-folha-pagamento/plans/DISPATCH-OVERNIGHT-FRONT.md`

Aplicar a mesma mudança que no DISPATCH-OVERNIGHT-BACK, mas:
- Usar `npm run build && npm test` para verificação (não `mvn`)
- Referência ao role: `docs/roles/frontend.md §Protocolo`
- Nota: "todas as tasks FE desta sprint têm fluxos_qa: [] → step QA não se aplica aqui" (verificar `fluxos_qa` dos planos FE-015, FE-016, FE-017 antes de confirmar — se algum tiver fluxos_qa ≠ [], incluir o loop QA para aquela task específica)

---

## Verificação final (após todas as mudanças)

Confirmar que:

```bash
grep -r "ADR 0019\|adr-0019\|0019-workflow" docs/roles/ CLAUDE.md docs/runbooks/
```
Retorna hits em: `backend.md`, `frontend.md`, `reviewer.md`, `qa.md`, `CLAUDE.md`, `PRE-MERGE-CHECKLIST.md`.

```bash
ls docs/roles/qa.md
```
Arquivo existe.

```bash
grep -c "template neutro\|round" docs/sprints/03-folha-pagamento/plans/DISPATCH-OVERNIGHT-BACK.md
```
Retorna > 0.

---

## Não fazer

- Não criar novo ADR — o 0019 já está Accepted.
- Não mudar a lógica das tasks de backend ou frontend (FIX-004, BE-*, FE-*).
- Não tocar em `docs/decisions/0019` — está Accepted, não editar.
- Não tocar em `.claude/agents/` — fora de escopo.
- Não aplicar Output Format de 7 seções nesta sessão — esta é implementação, não decisão.

## Referências

- `docs/decisions/0019-workflow-reviewer-qa-loop.md` — ADR a implementar (ler antes de começar)
- `docs/roles/backend.md` — estado atual (lido pelo planner em 2026-06-04)
- `docs/roles/frontend.md` — estado atual (sem reviewer/QA no checklist)
- `docs/roles/reviewer.md` — estado atual (sem instrução de chamada neutra)
- `docs/runbooks/PRE-MERGE-CHECKLIST.md` — estado atual
- `docs/sprints/03-folha-pagamento/plans/DISPATCH-OVERNIGHT-BACK.md` — a atualizar
- `docs/sprints/03-folha-pagamento/plans/DISPATCH-OVERNIGHT-FRONT.md` — a atualizar
