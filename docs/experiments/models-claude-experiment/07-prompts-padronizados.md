# Prompts padronizados dos runs

> **Estado:** rascunho para aprovação humana. Nenhum run inicia antes do pré-registro (Fase 2).
> Depende dos bloqueadores B1, B3, B4, B10 de `05-instrumentacao-e-harness.md`.

## 1. Por que padronizar

Cada run varia **uma coisa só**: qual modelo cada papel usa. Se o prompt variar junto, o efeito medido é a soma de duas causas e o experimento não responde nada.

Padronizar significa: **texto byte-idêntico entre runs**, com um conjunto fechado e pequeno de parâmetros substituídos mecanicamente.

## 2. Regras de higiene

1. **Substituição mecânica, nunca manual.** Os templates são preenchidos por script a partir da matriz de runs. Digitar prompt à mão reintroduz variância.
2. **O prompt não revela a configuração.** Nenhum papel sabe qual modelo ele ou os outros estão usando.
3. **O prompt não revela que existe um experimento.** Ver §5 — é o vazamento mais sério e exige artefato separado.
4. **Grafo forçado por `@-mention`.** Delegação por linguagem natural é não-determinística (B10). Todo handoff cita o subagente explicitamente.
5. **Sem referência a outro run.** Cada run tem branch e pasta de artefatos próprios; o prompt nunca cita run anterior.
6. **Sem correção no meio.** Se o humano intervir, o run é marcado como contaminado e descartado. Intervenções vão para o registro de descartes, não para `intervencoes.md`.

## 3. Parâmetros

Os únicos campos substituídos:

| Parâmetro | Exemplo | Origem |
|---|---|---|
| `{{RUN_ID}}` | `R-014` | matriz de runs |
| `{{TASK_ID}}` | `EXP-ALTA` | fixo por feature |
| `{{SPEC_PATH}}` | `docs/dispatch-specs/EXP-ALTA.md` | fixo por feature |
| `{{BRANCH}}` | `exp/R-014-alta` | derivado de `RUN_ID` |
| `{{ARTIFACT_DIR}}` | `docs/runs/R-014/` | derivado de `RUN_ID` |

A configuração de modelo por papel **não** é parâmetro de prompt. Entra por `--agents '<JSON>'` (B11), fora do texto.

## 4. Templates

### 4.1 Planner

```
Leia a especificação em {{SPEC_PATH}}.

Produza o plano de execução da task {{TASK_ID}} e grave em
{{ARTIFACT_DIR}}plano.md.

O plano deve conter: quebra em atividades, sequência e dependências entre
elas, arquivos afetados, estratégia de teste, riscos e critérios de pronto.

Trabalhe na branch {{BRANCH}}, já criada e com checkout feito.

Quando o plano estiver gravado, delegue a implementação com @backend,
passando o caminho do plano.
```

### 4.2 Backend

```
Implemente a task {{TASK_ID}} conforme o plano em {{ARTIFACT_DIR}}plano.md
e a especificação em {{SPEC_PATH}}.

Trabalhe na branch {{BRANCH}}.

Ao terminar, grave o relatório de execução em
{{ARTIFACT_DIR}}status.md e solicite a revisão com @reviewer.
```

### 4.3 Reviewer

```
Revise a implementação da task {{TASK_ID}} na branch {{BRANCH}}.

Entrada: especificação em {{SPEC_PATH}}, plano em {{ARTIFACT_DIR}}plano.md,
relatório em {{ARTIFACT_DIR}}status.md.

Grave a avaliação em {{ARTIFACT_DIR}}avaliacao.md.

Ao terminar, acione @qa-test-specialist.
```

### 4.4 QA

```
Valide a task {{TASK_ID}} na branch {{BRANCH}}.

Entrada: especificação em {{SPEC_PATH}}, plano, relatório e avaliação em
{{ARTIFACT_DIR}}.

Grave o resultado em {{ARTIFACT_DIR}}qa.md.

Ao terminar, pare. Não faça merge e não abra PR.
```

> **Nota de desenho.** Os templates são deliberadamente magros. Instrução detalhada mora no arquivo do subagente (`.claude/agents/`), que é constante entre runs. Enriquecer o prompt transferiria para ele responsabilidade que é do papel — e é justamente a qualidade do papel que está sendo medida.

## 5. Vazamento de contexto experimental — decisão pendente

**Problema.** As specs em `specs/SPEC-*.md` contêm score de complexidade, notas metodológicas e uma seção "o que esta spec deliberadamente não decide". Se um agente ler esses arquivos, ele descobre que está sendo medido **e** onde o avaliador vai olhar. Os arquivos vivem no repositório, logo estão ao alcance de `Read` e `Grep`.

A seção "o que não decide" é a mais perigosa: ela orienta o planner exatamente para o espaço de decisão que o experimento quer observar sem interferência.

**Mitigação proposta — dois artefatos por feature:**

| Artefato | Onde | Conteúdo | Quem lê |
|---|---|---|---|
| Spec experimental | `docs/experiments/.../specs/` | tudo, incluindo score e notas | pesquisador |
| **Spec de dispatch** | `docs/dispatch-specs/` | apenas objetivo, contexto, requisitos, critérios de aceitação, fora de escopo | **os runs** |

A spec de dispatch é gerada da experimental por script, removendo score, notas metodológicas e a seção "o que não decide". `{{SPEC_PATH}}` aponta sempre para a de dispatch.

**Decidido em 2026-08-10 — os artefatos de run ficam fora da pasta do experimento durante a execução.**

`{{ARTIFACT_DIR}}` = `docs/runs/<RUN_ID>/`, caminho neutro. **Ao fim do experimento**, os runs são arquivados em `docs/experiments/models-claude-experiment/runs/`.

Motivo: se o diretório onde o agente escreve e lê já fosse a pasta do experimento, ele estaria trabalhando a um nível de distância do desenho, das specs com score e das hipóteses. O arquivamento posterior atende a organização sem colocar o agente lá dentro durante o run.

**Resíduo não resolvido:** a pasta `docs/experiments/` continua legível para qualquer agente que resolva explorar o repositório por conta própria. Nenhum prompt aponta para lá, o que reduz muito a chance — mas não elimina. Opções remanescentes, em ordem de custo: (a) aceitar e registrar como limitação; (b) mover `docs/experiments/` para fora do repositório durante os runs; (c) bloquear o caminho por permissão.

## 6. Dependências não resolvidas

Nenhum run pode iniciar enquanto:

- **B1** — modelo não estiver comprovadamente pinado por papel, com verificação por `message.model` no JSONL. Hoje os quatro agentes da cadeia declaram valor inválido; a hipótese é que caiam em `inherit`, o que faria todos os runs medirem a mesma célula.
- **B4** — `effort` não estiver declarado explicitamente em todos os papéis. Hoje herda da sessão e vira segunda variável livre.
- **B3/B10** — o grafo planner→backend→reviewer→qa não estiver confirmado por `@-mention` num teste de fumaça, com evidência nos arquivos de subagente.
- **B6** — a allowlist de permissões não estiver fixada, sob pena de um run falhar por permissão e outro não.

O piloto de 2 runs (Fase 3) existe para validar exatamente isso antes de gastar as 32 réplicas restantes.
