---
task: FIX-005-front
titulo: "Atualizar /api/funcionarios → /api/v1/funcionarios no frontend"
data: 2026-06-06
branch: fix/005-padronizar-api-v1-front
responsavel: claude-front
estado: concluido
gates:
  build: ok
  lint: na
  testes: ok
  testes_total: 83
  testes_novos: 0
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits:
  - 308bf96
pr: null
desvios: 0
pendencias_humano: 0
---

# FIX-005-front — Atualizar `/api/funcionarios` → `/api/v1/funcionarios` no frontend

---

## O que foi feito

Search & replace global em dois arquivos:

- `frontend/src/api/folha.ts`: 10 chamadas funcionais (`client.get`, `client.post`, `client.put`, `client.delete`) + 6 JSDoc de cabeçalho atualizados — total de 19 ocorrências de `/api/v1/funcionarios`.
- `frontend/src/types/folha.ts`: 7 ocorrências em JSDoc (interfaces `Funcionario`, `FuncionarioRequest`, `Vale`, `ValeRequest`, `Adiantamento`, `AdiantamentoRequest`, `Fechamento`) — sem impacto funcional, mantém documentação consistente com o backend real.

Verificação pós-substituição: `grep -rn "api/funcionarios" frontend/src/` retornou zero resultados (nenhuma ocorrência sem o prefixo `/v1/`).

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

Substituição mecânica pura — sem lógica condicional, sem construção dinâmica de URL. Todos os paths eram strings literais, tornando o replace_all seguro e sem ambiguidade.

O comentário JSDoc da função `cancelarAdiantamento` (`DELETE /api/funcionarios/adiantamentos/{adiantamentoId}`) também foi atualizado, embora fosse apenas documentação.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- **Smoke manual recomendado:** abrir tela de funcionários em produção/staging e confirmar no DevTools → Network que as chamadas vão para `/api/v1/funcionarios` (não mais 404/401).
- **QA-011 Sub-área A** estava bloqueado por este fix — pode ser desbloqueado agora.
- Testes unitários de `folha.ts` estão agendados para QA-010 (não eram escopo desta task).

---

## Padrões técnicos

Fix mecânico (search & replace de URL string). Sem lógica não-trivial — seção não aplicável.

---

## Arquivos criados/modificados

- `frontend/src/api/folha.ts` (modificado: 10 URLs funcionais + 6 JSDoc atualizados de /api/funcionarios → /api/v1/funcionarios)
- `frontend/src/types/folha.ts` (modificado: 7 JSDoc atualizados de /api/funcionarios → /api/v1/funcionarios)
