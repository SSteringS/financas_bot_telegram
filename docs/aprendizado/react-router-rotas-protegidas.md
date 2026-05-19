# React Router — roteamento e padrão de rota protegida

## Contexto da dúvida

Durante revisão da FE-04, foi explicado como o React Router configura rotas e como se implementa "rota protegida" (que exige autenticação) — já que React não tem `CanActivate` guard embutido como o Angular.

## Resumo destilado

### Configuração de rotas

```tsx
<BrowserRouter>
  <Routes>
    <Route path="/entrar" element={<Entrar />} />
    <Route path="/erro" element={<Erro />} />
    <Route path="/" element={<AuthGuard><Home /></AuthGuard>} />
  </Routes>
</BrowserRouter>
```

- `<BrowserRouter>` é o provedor — tudo dentro dele tem acesso aos hooks de rota (`useNavigate`, `useSearchParams`, `useParams`)
- Cada `<Route>` mapeia um `path` a um `element` (componente)
- O `element` pode ser qualquer JSX, incluindo wrappers

### Padrão de rota protegida = componente wrapper

React **não tem** um conceito de "guard" embutido (diferente do `CanActivate` do Angular). O padrão idiomático é um **componente que envolve** a página e decide se renderiza ou redireciona:

```tsx
export function AuthGuard({ children }) {
  const { status } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (status === 'nao-autenticado') {
      navigate('/erro?motivo=precisa-link', { replace: true })
    }
  }, [status, navigate])

  if (status === 'loading') return <Spinner />
  if (status === 'nao-autenticado') return null
  return <>{children}</>
}
```

- `children` é a prop especial que recebe o que estiver "dentro" do componente quando usado (`<AuthGuard><Home/></AuthGuard>` → `<Home/>` chega como `children`)
- Lógica em 3 estados: loading (spinner), não-autenticado (null + redireciona), autenticado (renderiza children)

### Por que `useEffect + navigate` e não `<Navigate>` direto

Renderizar `<Navigate to="..." />` durante a fase de render — antes do `status` estar resolvido — causaria navegação prematura. A navegação é **side effect**, então vai em `useEffect`, que espera o estado estar definido. Regra geral: corpo do componente é puro, side effects em `useEffect`.

### `{ replace: true }` na navegação

```tsx
navigate('/erro?motivo=sessao-expirada', { replace: true })
```

`replace: true` substitui a entrada atual no histórico do navegador em vez de empilhar. Assim o usuário não consegue dar "voltar" e cair de novo num estado quebrado/intermediário. Use sempre que a navegação for "corretiva" (erro, redirect pós-login, etc).

### Hooks de rota mais usados

| Hook | Uso |
|---|---|
| `useNavigate()` | retorna `navigate(path, opts)` pra navegação programática |
| `useSearchParams()` | retorna `[searchParams, setSearchParams]` pra ler/escrever query string |
| `useParams()` | lê parâmetros de rota tipo `/pedidos/:id` |
| `useLocation()` | objeto com pathname, search, etc da URL atual |

### Componente "listener" sem renderização

Quando você precisa de um `useEffect` que vive dentro do Router mas não renderiza nada:

```tsx
function SessaoExpiradaListener() {
  const navigate = useNavigate()
  useEffect(() => {
    const handler = () => navigate('/erro?motivo=sessao-expirada')
    window.addEventListener('finbot:sessao-expirada', handler)
    return () => window.removeEventListener('finbot:sessao-expirada', handler)
  }, [navigate])
  return null  // não renderiza nada — só existe pelo useEffect
}
```

Padrão útil quando código não-componente (ex: o `client.ts` HTTP) precisa disparar navegação. O não-componente dispara um `CustomEvent`, e esse listener (que vive no Router e pode usar `useNavigate`) reage.

### Code splitting de rota

```tsx
const Showcase = lazy(() => import('./paginas/_Showcase').then(m => ({ default: m.Showcase })))
// ...
<Route path="/_showcase" element={
  <Suspense fallback={<div>Carregando…</div>}>
    <Showcase />
  </Suspense>
} />
```

`lazy()` + `import()` dinâmico = a página só é baixada pelo navegador quando a rota é acessada. `<Suspense>` mostra um fallback enquanto carrega. Equivalente ao `loadChildren` do Angular.

## Comparação com Angular Router

| Conceito | React Router | Angular Router |
|---|---|---|
| Config de rotas | `<Routes><Route>` em JSX | array `Routes` em módulo |
| Rota protegida | componente `<AuthGuard>` wrapper | `CanActivate` guard |
| Navegação programática | `useNavigate()` | `Router.navigate()` |
| Query params | `useSearchParams()` | `ActivatedRoute.queryParams` |
| Lazy loading | `lazy()` + `<Suspense>` | `loadChildren` |
| Histórico/replace | `{ replace: true }` | `{ replaceUrl: true }` |

React é mais "você monta" — guard é só um componente; Angular é mais "framework provê" — guard é uma interface (`CanActivate`) que o router conhece.

## Pra aprofundar

- React Router v7 docs (a API mudou bastante entre v5/v6/v7)
- Nested routes e `<Outlet>` — pra layouts compartilhados
- `loader` e `action` do React Router (data APIs) — alternativa a buscar dados em `useEffect`
- Por que React não tem guard embutido (filosofia "library, not framework")
