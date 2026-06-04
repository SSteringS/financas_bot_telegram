---
name: Protocolo reviewer + QA após cada task
description: Após completar cada task, chamar reviewer como subagente → corrigir → OK, depois QA → corrigir → OK
type: feedback
---

Após completar a implementação de cada task (antes de considerar concluída), o fluxo obrigatório é:

1. **Chamar o reviewer como subagente** — passar contexto da task, branch, arquivos criados/modificados
2. **Corrigir os erros reportados** pelo reviewer até ele dar OK
3. **Chamar o QA** (rodar testes automatizados E2E) e corrigir eventuais falhas
4. Só então considerar a task concluída

**Why:** Garantia de qualidade independente antes do merge — o reviewer pode detectar erros que o implementador não viu; o QA valida comportamento end-to-end.

**How to apply:** Mesmo que a task já tenha sido mergeada (merge prematuro), chamar o reviewer para o commit/diff relevante. Se houver problemas, criar commit de correção na branch de integração.
