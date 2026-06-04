---
name: Chamar Reviewer e qa-test-specialist antes de mergear
description: Após cada task (ou batch), chamar Agent reviewer e qa-test-specialist e aguardar ok explícito antes do merge — obrigação do implementador
type: feedback
---

Após completar uma task (ou batch de tasks relacionadas), antes de mergear para integration:

1. Chamar `Agent(subagent_type="reviewer")` com contexto completo da task (branch, status report, diff relevante).
2. Chamar `Agent(subagent_type="qa-test-specialist")` para análise de gaps de cobertura.
3. Aguardar o veredito dos dois.
4. Se Reviewer reprovar → corrigir e resubmeter. Se qa-test-specialist apontar gaps bloqueantes → adicionar testes antes de mergear.
5. Só então mergear o PR.

**Why:** O implementador esqueceu de chamar ambos durante o overnight dispatch de BE-027→029 (2026-06-04). O usuário apontou que o fluxo havia sido combinado anteriormente e precisava ser seguido. Reviewers independentes detectam smells arquiteturais (depedência invertida, lógica de domínio no controller, etc.) que `mvn test` verde não detecta.

**How to apply:** Ao final de CADA task (ou ao final de um batch sequencial como um dispatch overnight), chamar os dois agentes ANTES de mergear. Não pular mesmo se os testes estão todos verdes e o código parece certo — o review independente é a regra, não a exceção.
