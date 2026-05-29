---
**Data:** 2026-05-13
**Branch:** feature/frontend-fase3-completa
**Responsável (instância):** Claude Code (CLI) — overnight FE

---

## O que foi feito

- Instalado `@tanstack/react-query`.
- Criado `src/hooks/usePedidos.ts`: `useQuery` chamando `listarPedidos(filtros)` com `placeholderData` (equivalente TanStack v5 de `keepPreviousData`) e `staleTime: 30_000`.
- Criado `src/hooks/useResumo.ts`: `useQuery` chamando `obterResumo()` com `staleTime: 60_000`.
- Criado `src/components/Timeline.tsx`: agrupa pedidos por `dataPedido`, renderiza header de dia com círculo da variante C (zinc-900 para hoje, amber-100 para outros dias) + sticky, seguido de PedidoCards do dia. Linha vertical decorativa à esquerda.
- Criado `src/components/CarregandoLista.tsx`: skeleton de 3 cartões com `animate-pulse`.
- Criado `src/components/ListaVazia.tsx`: ícone + "Nenhum pedido neste filtro".
- Criado `src/components/ModalComprovante.tsx`: stub mínimo (implementação completa em FE-09).
- Atualizado `src/paginas/Home.tsx`: tela completa com SeletorMes sticky, FiltroStatus, BarraBusca, Timeline, estado vazio, estado de erro, paginação "Carregar mais". Estado dos filtros sincronizado com URL via `useSearchParams`.
- Atualizado `src/main.tsx`: adicionado `<QueryClientProvider>` wrapping App.

## Desvios do plano

- `ModalComprovante` criado como stub (placeholder) pois será implementado completamente em FE-09. A Home.tsx já integra o estado `pedidoIdAberto` e passa para o modal conforme plano.
- O plano menciona "CabecalhoApp com saudação" no topo — deixado para FE-08. A Home está funcional sem ele.
- Os contadores de `FiltroStatus` mostram os contadores da página atual (não o total de pendentes/pagos do banco) — isso é uma limitação da API que não retorna contagem por status separada. Anoto aqui mas não é bloqueante (decisão operacional, não de produto).

## Decisões tomadas durante a execução

- Filtros sincronizados com URL: `mes`, `status`, `busca` como search params. Ao mudar qualquer filtro, reset da paginação para 0.
- `usePedidos` usa `placeholderData: (prev) => prev` em vez da prop `keepPreviousData` que foi removida na v5 do TanStack Query.
- `Timeline` agrupa por `dataPedido` (não por `dataPagamento`) — alinhado com o mockup.

## Decisões pendentes

Nenhuma — tarefa fechada.

## Próximos passos / observações pro próximo

- FE-08: `CabecalhoApp` deve ser inserido no topo da Home antes do SeletorMes (fora do sticky).
- FE-09: substituir o stub `ModalComprovante` pela implementação real com iframe.

## Arquivos criados/modificados

- `frontend/src/hooks/usePedidos.ts` (novo)
- `frontend/src/hooks/useResumo.ts` (novo)
- `frontend/src/components/Timeline.tsx` (novo)
- `frontend/src/components/CarregandoLista.tsx` (novo)
- `frontend/src/components/ListaVazia.tsx` (novo)
- `frontend/src/components/ModalComprovante.tsx` (novo — stub)
- `frontend/src/paginas/Home.tsx` (modificado: implementação completa)
- `frontend/src/main.tsx` (modificado: QueryClientProvider)
- `frontend/package.json` (modificado: @tanstack/react-query)
