---
name: Helpers de teste com valores hardcoded são red flag de gap
description: Padrão metodológico — quando um helper de teste fixa um valor crítico, esse caminho da lógica está sem cobertura efetiva
type: feedback
---

Quando um helper privado de teste hardcoda valor que aparece em condicional/filtro do código de produção, considerar isso um sinal de gap, não conveniência.

**Why:** Em `FecharMesServiceImplTest.adiantamento(...)` o `dataInicio` estava fixo em `2026-01-01` (sempre anterior ao mês testado). O filtro `!a.getDataInicio().isAfter(primeiroDoMes)` da implementação ficou efetivamente sem cobertura — toda chamada batia no mesmo ramo do predicate. Um bug que inverta `isAfter`/`isBefore` ou troque o argumento passaria nos 11 testes existentes.

**How to apply:** Durante análise de gap, para cada helper privado de teste:
1. Listar os campos do objeto retornado.
2. Cruzar com os campos lidos pelo código de produção em condicionais/filtros.
3. Se algum campo hardcoded participa de comparação no production, esse comparator está sem cobertura — incluir como gap (geralmente Importante ou Bloqueante).
4. Recomendar parametrização do helper (ex.: aceitar `dataInicio` como argumento) junto da inclusão dos testes faltantes.

Anti-pattern a sinalizar no relatório: "helper X hardcoda campo Y, que é lido pelo filtro Z em produção — filtro sem cobertura efetiva mesmo com N testes na classe."
