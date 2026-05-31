---
name: boas-praticas-react
description: >
  Boas praticas e padroes de projeto React — regras de hooks, TypeScript em componentes,
  performance, acessibilidade, Compound Components, custom hooks como camada de abstracao,
  state management (Context vs prop drilling vs local), Error Boundary, data fetching patterns.
  Justificativa tecnica com referencia canonica para o relatorio done. Carregar quando a task
  cria componente nao-trivial, hook com logica de negocio, ou envolve decisao de composicao
  ou gerenciamento de estado.
load_pattern: shared
used_by: [frontend, reviewer]
created: 2026-05-30
adr: 0015
status: ativa
---

# Skill — Boas Praticas e Padroes React

## Quando carregar (gatilho explícito)

- Task cria componente com lógica não-trivial (condicional, derivação de estado, efeito).
- Task cria ou refatora hook customizado.
- Task envolve decisão de composição: como compor componentes, onde mora o estado.
- Task toca performance (rerenders, listas grandes, lazy loading).
- **Sinal concreto:** aparece `useEffect`, `useState` com lógica derivada, `useContext`,
  `React.memo`, `useMemo`, `useCallback`, `children as function`, tipos de props complexos.
- **Reviewer:** ao avaliar PR FE que cria componente ou hook nao-trivial â€” verificar se o padrao
  escolhido (composicao, estado, hook) foi o adequado ao problema e se ha justificativa no relatorio done.

## Regras de Hooks

**Dependências de `useEffect`:**
- Sempre incluir tudo que o efeito usa no array de deps — o lint (`exhaustive-deps`) é
  a fonte de verdade, não intuição.
- Stale closure: variável capturada no closure mantém o valor do render onde foi capturada.
  Solução: incluir na dep, usar `useRef` para valores mutáveis sem re-render, ou `useReducer`.
- Efeito de cleanup: retornar função de cleanup quando o efeito assina evento, cria timer
  ou inicia requisição (abortar com `AbortController`).

```tsx
// Correto — AbortController previne setState em componente desmontado
useEffect(() => {
  const controller = new AbortController();
  fetchData(signal: controller.signal).then(setData);
  return () => controller.abort();
}, [fetchData]);
```

**Regra geral:** se sentir necessidade de `// eslint-disable-next-line react-hooks/exhaustive-deps`,
documentar o motivo no comentário — é exceção legítima rara, não atalho.

## TypeScript em React

| Situação | Padrão |
|---|---|
| Props de componente | `interface Props { ... }` (prefira `interface` para extensibilidade) |
| Estado com variantes | Discriminated union: `type State = { status: 'loading' } \| { status: 'success'; data: T }` |
| Generics em hooks | `function useList<T>(initial: T[]): [T[], (item: T) => void]` |
| `as` (type assertion) | Só com type guard imediatamente antes — nunca sem validação |
| `children` | `React.ReactNode` para qualquer filho; `React.ReactElement` se precisa inspecionar |

**Anti-pattern:** `as any` como atalho. Correto: tipar a fonte de dados (ex: OpenAPI codegen
gera os tipos — usar sempre).

## Performance

**Regra antes de otimizar:** medir primeiro (`React DevTools Profiler`, `why-did-you-render`).
Premature optimization aqui é um anti-pattern real.

| Ferramenta | Quando usar | Quando NÃO usar |
|---|---|---|
| `React.memo` | Componente renderiza frequentemente com mesmas props | Todo componente por padrão |
| `useMemo` | Cálculo caro (ex.: filtrar lista de 1000+ itens) | Derivação simples de 2-3 campos |
| `useCallback` | Função passada como prop pra componente memoizado | Função usada apenas localmente |
| `lazy + Suspense` | Rota ou modal pesado nunca visto na tela inicial | Componentes pequenos |

## Padrões de Composição

**Composition over props booleanas crescentes:**
```tsx
// Anti-pattern — props boolean crescem indefinidamente
<Button primary disabled loading icon="check" />

// Padrão — Compound Components
<Button>
  <Button.Icon name="check" />
  <Button.Label>Salvar</Button.Label>
  <Button.Spinner visible={loading} />
</Button>
```

**Custom hook como camada de abstração (o "application layer" do front):**
- UI não conhece fetch, localStorage, nor formato bruto da API.
- Hook encapsula: chamada à API, mapeamento de DTO → domínio, loading/error state.
- Componente recebe dados já prontos pro render.

```tsx
// Hook — conhece a API, mapeia pro domínio
function usePedidos(mes: number) {
  const { data, isLoading, error } = useQuery(/* ... */);
  return { pedidos: data?.map(mapDtoToDomain) ?? [], isLoading, error };
}

// Componente — não conhece fetch nem DTO
function ListaPedidos({ mes }: { mes: number }) {
  const { pedidos, isLoading } = usePedidos(mes);
  if (isLoading) return <Skeleton />;
  return pedidos.map(p => <PedidoCard key={p.id} pedido={p} />);
}
```

## Gerenciamento de Estado

| Tipo de estado | Onde mora | Ferramenta |
|---|---|---|
| Estado local de UI (aberto/fechado, hover) | Componente | `useState` |
| Estado compartilhado entre irmãos próximos | Pai mais próximo (lift state) | `useState` |
| Estado global de UI (tema, locale) | Context | `createContext` + `useContext` |
| Estado de servidor (dados da API) | Cache de query | React Query / SWR |
| Estado de formulário complexo | Hook dedicado | `useReducer` ou React Hook Form |

**Regra:** Context não é substituto de gerenciamento de estado — é canal de injeção de
dependência. Quando Context com valor mutável rerenderiza muitos componentes → separar
em múltiplos contexts ou migrar pra React Query.

## Error Boundary e Suspense

- Cada rota deve ter um `ErrorBoundary` wrapper.
- `Suspense` para data fetching só com biblioteca que suporte (React Query v5, Relay).
- Não usar `Suspense` pra loading de dados com `useEffect` + `useState` — não funciona.

## Acessibilidade (mínimo obrigatório)

- Elementos interativos são `<button>` ou `<a>`, nunca `<div onClick>`.
- Imagens têm `alt` descritivo; decorativas têm `alt=""`.
- Formulários: `<label>` associado com `htmlFor` ou `aria-label`.
- Navegação por teclado: `Tab`, `Enter`, `Escape` funcionam em modais e dropdowns.
- Cores: contraste >= 4.5:1 para texto normal (WCAG AA).

## Relatório done — seção Padrões técnicos (FE)

No status report, a seção `## Padrões técnicos` deve conter:

- Qual padrão de composição/estado foi aplicado e **onde** (componente/hook).
- **Por quê** — o problema que o padrão resolveu (ex.: "props boolean cresciam indefinidamente → Compound Components").
- Referência canônica quando aplicável:
  - [Kent C. Dodds — Compound Components](https://kentcdodds.com/blog/compound-components-with-react-hooks)
  - [Kent C. Dodds — When to useMemo and useCallback](https://kentcdodds.com/blog/usememo-and-usecallback)
  - [TkDodo — Practical React Query](https://tkdodo.eu/blog/practical-react-query)
  - [React Docs — Lifting State Up](https://react.dev/learn/sharing-state-between-components)
- Trade-off consciente (ex.: "optei por Context em vez de prop drilling porque o dado
  é consumido em 5 níveis de profundidade — se causar rerenders, migrar pra React Query").

## Ler junto

- Skill `ecossistema-frontend` — ferramentas de teste (RTL) e tipagem (codegen OpenAPI).
- Skill `seguranca-web-frontend` — quando task toca formulários com dados sensíveis.
- `docs/architecture/especificacao-tecnica.md` — decisões de biblioteca já tomadas no projeto.
