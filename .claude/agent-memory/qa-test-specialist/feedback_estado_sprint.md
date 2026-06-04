---
name: Sprint só fecha quando todas as atividades levantadas mergearem
description: Critério de fechamento de sprint do projeto — não confundir merge integration→develop com fim da sprint
type: feedback
---

Não inferir que uma sprint está "em fechamento" só porque o PR `integration/<NN>-<slug> → develop` foi mergeado. O critério real é: **todas as atividades levantadas pela sprint precisam estar mergeadas em develop**. Isso inclui follow-ups, fixes derivados e tasks descobertas durante a sprint.

**Why:** correção do humano em 2026-06-03 após eu ter escrito "sprint 03 está em fechamento" baseado no commit `face023 Integration/03 folha pagamento (#88)`. O merge da integration branch é evento intermediário, não fim de sprint.

**How to apply:**
- Ao planejar onde uma task de follow-up entra (sprint atual vs próxima), **não decidir sozinho** baseado em status do PR integration→develop. Apenas declarar que a task existe e marcar `estado: rascunho` com nota "posicionar sprint é decisão do planner".
- Se precisar saber o estado real da sprint, ler `STATE.md` do planner ou perguntar via AskUserQuestion — não inferir do `git log`.
- Esta regra é específica do workflow do projeto; sprints aqui não seguem cadência fixa, fecham quando o trabalho fecha.
