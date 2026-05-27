# Avaliações — revisões de entregas dos implementadores

Esta pasta guarda **relatórios de avaliação** feitos pelo Claude de planejamento sobre o trabalho dos Claudes implementadores (back e front), conforme entregas vão acontecendo.

## Propósito

1. **Calibrar o fluxo de desenvolvimento** — identificar padrões recorrentes (gaps de teste, decisões de produto improvisadas, código abaixo do padrão, etc) e ajustar prompts/CLAUDE.md/processo
2. **Histórico de qualidade** — acompanhar evolução das entregas ao longo do tempo
3. **Feedback estruturado** — base pra discussões com o humano sobre o que melhorar

Não é fiscalização nem auditoria — é **retrospectiva técnica** documentada.

## Convenção

- **Um arquivo por entrega significativa** (overnight de várias tarefas, ou tarefa única particularmente complexa)
- **Nome:** `<area>-<contexto>.md` (ex: `frontend-fase3-overnight.md`, `backend-be01-hotfix.md`)
- **Estrutura padrão** de cada arquivo:
  1. Header com tarefas avaliadas, branch, período, implementador
  2. Nota final (0 a 10) com breve justificativa
  3. Tabela de critérios e notas por dimensão (com pesos)
  4. Pontos fortes detalhados
  5. Pontos a melhorar detalhados
  6. Recomendações pra ajustar fluxo
  7. Recomendação específica pra esta entrega (mergear? ajustar?)

## Quando criar

- Após cada entrega de overnight (várias tarefas mergeadas de uma vez)
- Após qualquer hotfix ou tarefa que produziu aprendizado relevante sobre o processo
- Quando o humano pedir explicitamente "avalia isso aí"

## Quando NÃO criar

- Tarefa pequena e direta sem nada de notável
- Pequenos ajustes (style, fix de typo)
- Quando o status report do implementador já cobriu tudo
