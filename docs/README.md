# Pasta `docs/` — Cérebro Compartilhado do Projeto

Esta pasta é o canal de comunicação entre todas as instâncias de Claude que trabalham neste projeto: a instância de planejamento (que conversa com o humano via Cowork) e as instâncias implementadoras (que rodam no Claude Code do IntelliJ ou via CLI).

A regra geral é simples: **quem implementa lê daqui antes de codar; quem implementa escreve aqui depois de cada tarefa; quem planeja lê aqui pra entender o estado real do projeto.**

---

## Estrutura

```
docs/
├── README.md                         (este arquivo)
├── architecture/                     (fonte da verdade técnica)
│   ├── especificacao-tecnica.md
│   └── design-proposals/             (mockups visuais)
├── plans/                            (planos de execução por fase)
│   └── FASE-X-NOME.md
├── runbooks/                         (roteiros de execução com instruções e prompts)
│   └── ROTEIRO-FRONTEND.md
├── status/                           (relatórios pós-tarefa, escritos pelos implementadores)
│   ├── _TEMPLATE.md
│   ├── BE-XX-titulo.md
│   └── FE-XX-titulo.md
└── decisions/                        (ADRs — Architecture Decision Records)
    ├── _TEMPLATE.md
    └── NNNN-titulo.md
```

---

## Pasta por pasta

### `architecture/` — Fonte da verdade técnica

Decisões fixadas sobre stack, contratos, modelo de dados, design visual. Esta pasta dita o que deve ser construído.

**Quem escreve**: o Claude planejador, sempre com aprovação do humano.

**Quem lê**: tudo e todos. Antes de codar qualquer coisa, o implementador consulta aqui pra confirmar o contrato.

**Regra crítica**: ninguém — humano, planejador, ou implementador — altera arquivos desta pasta sem decisão deliberada e registro em `decisions/`. Se o implementador, durante a execução, perceber que algo precisa mudar (ex: um campo do DTO que não estava previsto), ele **para**, escreve em `status/` o que descobriu, e o humano decide se atualiza `architecture/` antes de prosseguir.

### `plans/` — O que ainda falta fazer

Planos de execução por fase, com tarefas atômicas. Cada tarefa tem ID, descrição, arquivos esperados, critério de aceitação e dependências.

**Quem escreve**: o Claude planejador.

**Quem lê**: humano (pra orquestrar) e implementadores (pra saber o que fazer).

**Convenção**: nomes de arquivo no formato `FASE-N-NOME-EM-CAIXA-ALTA.md`. Tarefas dentro com IDs do tipo `BE-01`, `FE-01`, `DEP-01`, `EVO-01`.

### `runbooks/` — Roteiros de execução

Instruções práticas pra um humano ou agente seguir, com prompts prontos pra Claude Code. Mais "como fazer" do que "o que fazer".

**Quem escreve**: o Claude planejador.

**Quem lê**: humano (especialmente).

**Convenção**: `ROTEIRO-AREA.md` (ex: `ROTEIRO-FRONTEND.md`, `ROTEIRO-DEPLOY.md`).

### `status/` — Diário de bordo das tarefas concluídas

Relatórios curtos escritos pelos implementadores depois de fechar cada tarefa. Documentam o que foi feito, qualquer desvio do plano, decisões locais tomadas durante a execução, e próximos passos.

**Quem escreve**: implementadores (Claude Code do IntelliJ).

**Quem lê**: o Claude planejador na próxima sessão (pra entender o que mudou desde a última conversa).

**Convenção**: nome de arquivo é `<TASK-ID>-titulo-curto.md` (ex: `BE-01-migration-sql.md`, `FE-05-pedido-card.md`). Use o template `_TEMPLATE.md` desta pasta como base.

### `decisions/` — Decisões importantes com data e contexto (ADRs)

Quando algo muda de rumo no projeto, registra aqui — não no commit, não no chat, aqui. Pra você saber em 6 meses por que uma escolha foi feita.

**Quem escreve**: humano com ajuda do Claude planejador. Implementadores **não** escrevem ADR sozinhos.

**Quem lê**: todos, sempre que precisarem entender histórico.

**Convenção**: `NNNN-titulo-em-kebab-case.md` numerado sequencialmente (`0001-`, `0002-`, ...). Use o template `_TEMPLATE.md` desta pasta como base. ADRs nunca são deletados nem editados depois de aceitos — se uma decisão é revogada, cria-se um ADR novo que **supersedes** o antigo.

---

## Fluxo de trabalho

### Quando o humano pede algo novo ao planejador

1. Planejador lê `architecture/` e `status/` pra ver o estado atual
2. Discute com humano, gera ou atualiza `plans/` e/ou `runbooks/`
3. Se a discussão alterou o rumo do projeto, registra ADR em `decisions/`

### Quando um implementador (Claude Code) executa uma tarefa

1. Lê o plano relevante em `plans/` (ex: tarefa BE-05 em `plans/FASE-3-VISUALIZACAO.md`)
2. Lê os contratos em `architecture/` pra confirmar o que deve construir
3. Lê o último relatório em `status/` da tarefa anterior, se aplicável (pra herdar contexto)
4. Executa a tarefa
5. **Antes de fechar a sessão**, escreve `status/<TASK-ID>-titulo.md` seguindo o template
6. Commit + push da implementação E do arquivo de status no mesmo commit

### Quando o planejador volta pra próxima sessão

1. Lê `status/` pra atualizar mental modelo do estado do projeto
2. Conversa com humano sobre próximos passos
3. Atualiza `plans/` se necessário

---

## Regras críticas pra os implementadores

Se você é uma instância de Claude rodando no Claude Code do IntelliJ trabalhando neste projeto, leia com atenção:

1. **Antes de implementar**, leia: o plano da tarefa atual (`plans/`) **e** os arquivos relevantes em `architecture/`. Não pule essa etapa, mesmo que pareça redundante.

2. **Não altere `architecture/`** durante a execução de uma tarefa. Se descobrir que o contrato precisa mudar, pare e documente em `status/<TASK-ID>-titulo.md` na seção "Decisões pendentes". O humano decide se atualiza `architecture/` antes de você prosseguir.

3. **Não invente decisões de produto**. Se faltar algo no plano (ex: comportamento ambíguo, nome de variável, escolha de biblioteca não especificada), pare e pergunte ao humano. Anote a pergunta em `status/`.

4. **Sempre escreva o relatório de status ao terminar**, mesmo que a tarefa tenha sido trivial. O relatório custa 2 minutos e economiza horas de re-explicação na próxima sessão.

5. **Seu commit fecha quando os dois arquivos estão lá**: o código da feature **e** o `status/<TASK-ID>-titulo.md`. Sem o status, a tarefa não está fechada.

---

## Templates

Use os arquivos `_TEMPLATE.md` em cada subpasta como ponto de partida. Eles têm a estrutura mínima esperada.

- `status/_TEMPLATE.md` — relatório pós-tarefa
- `decisions/_TEMPLATE.md` — ADR

Os templates **não** são tarefas. São apenas modelos. Não comite versões "preenchidas" do `_TEMPLATE.md` — sempre crie um arquivo novo com o nome certo (`BE-XX-titulo.md`, `0001-titulo.md`).
