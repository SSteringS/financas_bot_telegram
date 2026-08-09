---
**Data:** 2026-05-13
**Branch:** feature/frontend-fase3-completa
**Responsável (instância):** Claude Code (CLI) — overnight FE

---

## O que foi feito

- Criado `src/components/FiltroStatus.tsx`: 3 pills (Tudo / Pendente / Pago), controlled com `value + onChange + contadores`. Visual fiel ao mockup: ativo = bg-zinc-900 branco, pendente inativo = amber, pago inativo = emerald. `aria-pressed` para acessibilidade.
- Criado `src/components/SeletorMes.tsx`: carrossel horizontal scroll com `overflow-x-auto scrollbar-width:none`. Gera últimos 12 meses dinamicamente, pill ativo em zinc-900. Usa `date-fns` para formatar "Maio", "Abril", etc.
- Criado `src/components/BarraBusca.tsx`: input controlled com debounce de 300ms internamente. Label `sr-only` para acessibilidade, ícone de lupa, estilo alinhado ao mockup.
- Criado `src/components/BarraBusca.test.tsx`: 4 testes com fake timers verificando debounce (sem chamada antes de 300ms, chamada após 300ms, única chamada para digitação rápida).
- Atualizado `src/paginas/_Showcase.tsx`: exibe todos os três filtros + PedidoCards, cada um com estado local e log do valor atual.

## Desvios do plano

Nenhum — todos os componentes implementados conforme spec.

## Decisões tomadas durante a execução

- Os testes de debounce usam `fireEvent.change` + `vi.useFakeTimers()` + `act()` em vez de `userEvent.type` com `advanceTimers` — a segunda abordagem causava timeout de 5s no happy-dom com vitest 4.x.
- `SeletorMes` recebe `value: string` no formato `YYYY-MM`, que é o mesmo formato dos params de URL — evita conversão.

## Decisões pendentes

Nenhuma — tarefa fechada.

## Próximos passos / observações pro próximo

- FE-07: usar `FiltroStatus` com conversão `TUDO → undefined`, `PENDENTE → 'pendente'`, `PAGO → 'pago'` ao passar para `listarPedidos()`.
- FE-07: `SeletorMes` gera `de` e `ate` como primeiro e último dia do mês selecionado.

## Arquivos criados/modificados

- `frontend/src/components/FiltroStatus.tsx` (novo)
- `frontend/src/components/SeletorMes.tsx` (novo)
- `frontend/src/components/BarraBusca.tsx` (novo)
- `frontend/src/components/BarraBusca.test.tsx` (novo)
- `frontend/src/paginas/_Showcase.tsx` (modificado: filtros adicionados)
- `frontend/package.json` (modificado: @testing-library/user-event)
