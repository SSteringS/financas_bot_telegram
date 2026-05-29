---
**Data:** 2026-05-13
**Branch:** feature/frontend-fase3-completa
**Responsável (instância):** Claude Code (CLI) — overnight FE

---

## O que foi feito

- Criado `src/lib/formato.ts`: funções `formatarMoeda`, `formatarData`, `formatarDataRelativa`, `formatarDiaSemana`, `abreviarMes`, `diaDaMes`. Usa `date-fns` com locale `ptBR`.
- Criado `src/lib/formato.test.ts`: 7 testes cobrindo moeda (inteiro, decimal, zero) e datas.
- Criado `src/components/StatusBadge.tsx`: badge âmbar para PENDENTE, verde para PAGO. Visual fiel ao mockup.
- Criado `src/components/PedidoCard.tsx`: replica fielmente o cartão da variante-c-timeline.html. Botão "Ver comprovante" com w-full, bg-emerald-600, ícone de download, aria-label, minHeight 44px. Aparece apenas se `status === PAGO && temComprovante`.
- Criado `src/paginas/_Showcase.tsx`: página de showcase com 4 exemplos de PedidoCard (pago com comprovante, pendente, pago no mesmo dia, pago com comprovante pendente).
- Atualizado `src/App.tsx`: rota `/_showcase` importa Showcase lazy (dev only).

## Desvios do plano

- O plano dizia "Storybook ou página de showcase opcional". Implementei a página `_Showcase` em vez de Storybook — mais simples, sem dependência adicional, e suficiente para revisão visual.
- `urlFotoPedido` não foi usado no PedidoCard — o mockup não mostra foto do pedido no card (só aparece na listagem sem foto). Mantive apenas o botão de comprovante.

## Decisões tomadas durante a execução

- A legenda de data do cartão exibe "Pago no mesmo dia" quando `dataPedido === dataPagamento`, e "Pedido em X · pago em Y" quando são diferentes. Essa lógica está alinhada com o mockup.
- O componente PedidoCard não mostra a legenda de data para pedidos PENDENTE (sem dataPagamento) — comportamento fiel ao mockup.

## Decisões pendentes

Nenhuma — tarefa fechada.

## Próximos passos / observações pro próximo

- FE-06: os filtros também precisam aparecer no `_Showcase` para validação visual.
- FE-07: `Timeline.tsx` vai usar `formatarDiaSemana`, `abreviarMes` e `diaDaMes` de `formato.ts`.

## Arquivos criados/modificados

- `frontend/src/lib/formato.ts` (novo)
- `frontend/src/lib/formato.test.ts` (novo)
- `frontend/src/components/StatusBadge.tsx` (novo)
- `frontend/src/components/PedidoCard.tsx` (novo)
- `frontend/src/paginas/_Showcase.tsx` (novo)
- `frontend/src/App.tsx` (modificado: rota _showcase lazy)
