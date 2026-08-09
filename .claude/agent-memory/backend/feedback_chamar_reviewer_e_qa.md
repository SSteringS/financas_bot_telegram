---
name: Chamar Reviewer e qa-test-specialist antes de mergear — e NÃO mergear sozinho
description: Após cada task: chamar Agent reviewer + qa-test-specialist, gerar arquivo de avaliação em docs/sprints/<NN>/avaliacoes/, e PARAR — NÃO rodar gh pr merge
type: feedback
---

Após completar uma task (ou batch de tasks relacionadas):

1. Chamar `Agent(subagent_type="reviewer")` com contexto completo da task.
   → **O Reviewer escreve o arquivo de avaliação** em `docs/sprints/<NN>/avaliacoes/<TASK-ID>-<slug>.md`.
   → O prompt deve instruir o Reviewer a criar o arquivo (path completo, frontmatter, conteúdo).
2. Chamar `Agent(subagent_type="qa-test-specialist")` para análise de gaps.
   → **O QA complementa o mesmo arquivo** — adiciona a seção de cobertura ao arquivo já criado pelo Reviewer.
   → O prompt deve referenciar o arquivo existente e pedir para completar a seção QA.
3. Ler os resultados e corrigir se houver gaps bloqueantes.
4. **PARAR — NÃO rodar `gh pr merge`.** O merge para `develop` é gate humano.

**Quem NÃO escreve o arquivo de avaliação:** o implementador (backend). Os resultados do Reviewer e do QA são deles, não meus. Se eu escrevesse, seria self-review disfarçado.

**Why:** Em 2026-06-04, o implementador rodou `gh pr merge` por conta própria em FIX-003, FIX-004 e FIX-005, mergeando direto em `develop` sem aprovação humana. O role document é explícito: "Não faz push pra `develop` — para pra revisão." Além disso, os arquivos de avaliação de FIX-004 e FIX-005 não foram criados — os resultados do Reviewer e QA ficaram só no chat, sem registro persistente.

**How to apply:**
- Criar o PR → parar e informar ao usuário.
- Jamais executar `gh pr merge` para branches que vão para `develop` (fix/*, hotfix/*).
- Sempre criar o arquivo em `docs/sprints/<NN>/avaliacoes/` com frontmatter + resultado do Reviewer + resultado do QA, mesmo que o Reviewer seja self-review (com a nota de ADR 0005).
- Para branches `feature/* → integration/*`: pode mergear sem aprovação humana (conforme CLAUDE.md).
