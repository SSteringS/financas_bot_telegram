# React — modelo de renderização e hooks fundamentais

## Contexto da dúvida

Durante revisão da FE-04 (Roteamento + AuthGuard) do frontend, foi feito um crash course dos fundamentos de React pra quem vem de backend / Angular. Este arquivo consolida o modelo mental.

## Resumo destilado

### Componente = função que retorna JSX

```tsx
function Erro() {
  return <div>Algo deu errado</div>
}
```

Sem classe, sem decorator (no React moderno). JSX é HTML-like que o build transforma em `React.createElement(...)`. Expressões JS entram com `{}`.

### O modelo de re-render (a maior mudança vinda de Angular)

- **Angular:** classe instanciada uma vez, vive; change detection atualiza só os pedaços do DOM que mudaram
- **React:** quando o estado muda, **a função do componente inteira roda de novo**, do zero. React compara o JSX retornado com o anterior (virtual DOM diff) e atualiza só o necessário no DOM real

Implicação: o corpo da função roda **múltiplas vezes** na vida do componente. Variáveis locais comuns são recriadas a cada execução. Por isso existem **hooks** — pra "lembrar" coisas entre renders.

### Hooks = funções `use*` que dão memória e ciclo de vida

| Hook | Função | Equivalente Angular |
|---|---|---|
| `useState` | Valor que, ao mudar, dispara re-render | propriedade + change detection |
| `useEffect` | Código que roda APÓS o render (side effects) | `ngOnInit`/`ngOnDestroy`/`ngOnChanges` |
| `useRef` | Valor mutável que persiste entre renders SEM disparar re-render | propriedade de classe comum |
| `useNavigate` | Função pra navegar programaticamente | `Router.navigate()` |
| `useSearchParams` | Lê/escreve query string | `ActivatedRoute.queryParams` |

**Regra de ouro:** hooks só podem ser chamados no topo de um componente (ou de outro hook). Nunca dentro de `if`, loop, ou callback — React rastreia hooks por ordem de chamada.

### `useEffect` em detalhe

```tsx
useEffect(() => {
  // roda DEPOIS do render
  return () => {
    // cleanup — roda ao desmontar ou antes do próximo effect
  }
}, [dependencias])
```

O array de dependências controla quando o effect roda:
- `[]` → uma vez, na montagem (tipo `ngOnInit`)
- `[x, y]` → na montagem e sempre que `x` ou `y` mudam
- sem array → depois de todo render (raramente desejado)

O `return () => {...}` é cleanup — equivalente a `ngOnDestroy`, mas roda também antes de cada re-execução do effect. **Esquecer o cleanup de event listeners = memory leak + handlers duplicados.**

### Corpo do componente deve ser "puro"

O corpo da função deve só **calcular o que renderizar**. Side effects (navegação, listeners, chamadas de API, timers) vão em `useEffect`. Fazer `navigate(...)` direto no corpo do render é bug — roda durante a renderização, antes da hora.

### `useRef` vs `useState` — quando usar cada

- `useState` → quando mudar o valor DEVE atualizar a tela (dispara re-render)
- `useRef` → quando você só quer "lembrar" algo entre renders, sem atualizar a tela (não dispara re-render). Exemplos: referência a elemento DOM, flag de controle, valor de timer

## StrictMode e a double-execution

Em desenvolvimento, o React StrictMode **monta e desmonta cada componente duas vezes** propositalmente, pra forçar você a detectar bugs de cleanup. Consequência: `useEffect` roda 2x em dev.

Pra efeitos idempotentes (adicionar listener, buscar dado cacheável), tudo bem. Pra efeitos **não-idempotentes** (ex: consumir um token single-use), precisa proteger:

```tsx
const tentouRef = useRef(false)
useEffect(() => {
  if (tentouRef.current) return  // segunda execução do StrictMode: sai cedo
  tentouRef.current = true
  // ... efeito não-idempotente aqui ...
}, [])
```

Em produção, StrictMode não double-executa — mas o código com a guarda continua correto.

## Padrão "cache em variável de módulo"

Variáveis declaradas FORA do componente (escopo do módulo) são compartilhadas por todas as instâncias do hook/componente:

```tsx
let cachedStatus = 'loading'  // fora do componente

export function useAuth() {
  // todas as instâncias compartilham cachedStatus
}
```

Útil pra evitar N chamadas de API quando vários componentes usam o mesmo hook. **Trade-offs:** testes paralelos podem contaminar um ao outro (resetar no `beforeEach`), não é reativo a múltiplas tabs, comportamento estranho em hot-reload. Pra MVP single-tab funciona; "jeito idiomático" seria React Context ou TanStack Query como source of truth.

## Pra aprofundar

- React docs "Thinking in React" e "You Might Not Need an Effect"
- Regras dos Hooks (eslint-plugin-react-hooks ajuda a não violar)
- `useMemo` e `useCallback` — memoização pra evitar recálculo/recriação desnecessária
- `useContext` — compartilhar estado sem prop drilling
- Virtual DOM e o algoritmo de reconciliação
- Por que StrictMode existe e o que mais ele detecta além de double-mount
