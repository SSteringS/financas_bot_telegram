# TanStack Query — gerenciamento de "estado de servidor" em React

## Contexto da dúvida

Durante revisão da FE-07 (Home), foi explicado o TanStack Query — a biblioteca que a Home usa pra buscar e cachear pedidos da API. É o conceito mais importante dessa task.

## Resumo destilado

### O problema: "estado de servidor" ≠ "estado de UI"

- **Estado de UI** (modal aberto, filtro selecionado): seu, síncrono, local. `useState` resolve.
- **Estado de servidor** (lista de pedidos vinda da API): assíncrono, compartilhado, pode envelhecer, pode ser rebuscado, precisa de cache. `useState` + `useEffect` na mão vira um inferno.

Pra exibir uma lista da API você precisaria gerenciar: data, loading, error, cache, "quando rebuscar", deduplicação, "mostrar dado antigo enquanto busca novo". TanStack Query faz tudo isso declarativamente.

### `useQuery` — a peça central

```tsx
useQuery({
  queryKey: ['pedidos', filtros],          // chave de cache
  queryFn: () => listarPedidos(filtros),   // como buscar
  staleTime: 30_000,                       // "fresco" por 30s
  placeholderData: (prev) => prev,         // mantém dado anterior enquanto busca novo
})
```

Retorna `{ data, isLoading, isFetching, isError, refetch, ... }` — você não gerencia esses estados, a lib gerencia.

### `queryKey` — o conceito mais importante

É a **chave de cache**. `['pedidos', filtros]` — quando `filtros` muda, é uma chave diferente → entrada de cache diferente → **a lib rebusca automaticamente**.

A mágica: você muda o filtro → a queryKey muda → ela rebusca e cacheia por combinação de filtro. Volta a um filtro já buscado → serve do cache instantâneo.

### Opções importantes

- **`staleTime`** — por quanto tempo o dado é "fresco". Dentro do prazo, não rebusca (serve do cache). Depois, próximo acesso dispara refetch em background.
- **`placeholderData: (prev) => prev`** — mantém mostrando o dado anterior enquanto busca o novo. Sem isso, mudar filtro faz a tela piscar pra loading. Com isso, mostra o antigo com `isFetching=true` até o novo chegar. (Era `keepPreviousData` na v4.)
- **`retry`** — quantas vezes tentar de novo em falha de rede.

### Estados que a query expõe

- `isLoading` — primeira carga, sem dado nenhum ainda
- `isFetching` — buscando (inclui refetches; pode ser true mesmo tendo dado antigo)
- `isError` — deu erro
- `data` — os dados (undefined até a primeira carga resolver)
- `refetch()` — função pra forçar rebuscar

A distinção `isLoading` vs `isFetching` importa: `isFetching && !data` = primeira carga (mostra skeleton); `isFetching && data` = refetch (mostra dado antigo, atualiza por baixo).

### Setup — `QueryClientProvider`

```tsx
const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 30_000, retry: 1 } },
})

<QueryClientProvider client={queryClient}>
  <App />
</QueryClientProvider>
```

O `QueryClient` é o cache central. O `<QueryClientProvider>` no topo da árvore o disponibiliza pra todos os componentes. Padrão "provider no topo" — comum em React pra estado global.

### Encapsular em hook customizado

Boa prática: não espalhar `useQuery({...})` pelos componentes. Criar um hook:

```tsx
export function usePedidos(filtros) {
  return useQuery({ queryKey: ['pedidos', filtros], queryFn: () => listarPedidos(filtros), ... })
}
```

Vantagens: muda a queryKey/queryFn num lugar só; componente fica limpo (`const { data } = usePedidos(filtros)`); reuso entre componentes.

### Invalidação de cache

```tsx
const qc = useQueryClient()
qc.invalidateQueries({ queryKey: ['pedidos'] })  // marca como stale → rebusca
```

Útil pra "acabei de criar/editar algo, recarrega a lista".

## Padrão complementar: estado na URL

A Home guarda os FILTROS na URL (`?mes=2026-05&status=PAGO&busca=energia`) via `useSearchParams`, não em `useState`. Benefícios:

- **Refresh preserva** o estado
- **Link compartilhável** — manda a URL, abre no mesmo estado
- **Botão voltar funciona** — navegação restaura estado anterior

Regra mental de "onde mora o estado":
- **URL** → filtros, navegação, qualquer coisa que deveria sobreviver a um refresh ou ser compartilhável
- **`useState`** → estado de UI efêmero (modal aberto, hover, etc)
- **TanStack Query** → estado de servidor (dados da API)

Cada tipo no lugar certo.

## Comparação com Angular

| Conceito | React (TanStack Query) | Angular |
|---|---|---|
| Estado de servidor | TanStack Query (camada dedicada) | service + `BehaviorSubject`, NgRx, ou `@tanstack/angular-query` |
| Cache de requests | automático via `queryKey` | manual |
| Loading/error | automático | manual |
| Refetch ao mudar param | automático (queryKey muda) | manual (re-subscribe) |

Angular core dá menos "de graça" pra estado de servidor — você monta com RxJS. TanStack Query é camada dedicada e madura, virou padrão de fato no ecossistema React.

## Pra aprofundar

- `useMutation` — o equivalente do `useQuery` pra escritas (POST/PUT/DELETE), com cache invalidation
- `useInfiniteQuery` — paginação infinita / scroll infinito
- Optimistic updates — atualizar a UI antes da resposta do servidor chegar
- Query devtools (`@tanstack/react-query-devtools`) — painel visual do cache, ótimo pra debug
- `select` option — transformar/derivar dados da query sem recriar a query
- Por que "server state é diferente de client state" (artigos do Tanner Linsley, criador da lib)
