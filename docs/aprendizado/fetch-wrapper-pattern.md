# Padrão "fetch wrapper" — cliente HTTP em React/TypeScript

## Contexto da dúvida

Durante revisão da FE-03 (API client) do frontend Fase 3, foi explicado o padrão de criar um wrapper sobre `fetch` em vez de usar bibliotecas como Axios, e a separação entre client "burro" e funções de domínio.

## Resumo destilado

Em React/TS, a forma idiomática moderna de chamar HTTP é:

1. **Wrapper sobre `fetch` nativo** (sem dependência externa) que cuida de URL, headers, credenciais, tratamento de erro
2. **Funções de domínio** que delegam ao wrapper, cada uma representando um endpoint da API

```
src/api/
  client.ts       — wrapper genérico (fetch + headers + erro)
  pedidos.ts      — funções específicas: listarPedidos, buscarPedido, etc
  auth.ts         — exchangeToken, obterMe
  tipos.ts        — tipos TS dos contratos da API
```

A vantagem dessa separação:
- **Testes** mockam o `client`, não o `fetch` global — testes mais focados
- **Reuso** — qualquer função nova de endpoint reusa o client com 1 linha
- **Refactor** — trocar fetch por axios futuramente seria mudar só `client.ts`, sem mexer no resto

## Boas práticas que entram nesse padrão

### `credentials: 'include'` é obrigatório quando usa cookies de sessão

Sem isso, o navegador NÃO envia cookies em requests cross-origin. Como front e API costumam estar em origens diferentes (porta ou subdomínio diferente), isso é o bug mais comum em React + auth via cookie.

```typescript
await fetch(url, {
  credentials: 'include',  // <-- essencial
  ...
})
```

### Custom error class pra erro tipado

```typescript
export class ApiError extends Error {
  constructor(public readonly codigo: number, mensagem: string) {
    super(mensagem)
    this.name = 'ApiError'
  }
}
```

Permite ao caller fazer `if (e instanceof ApiError && e.codigo === 404)` em vez de comparar mensagens de string.

A sintaxe `public readonly codigo: number` no construtor é shortcut do TypeScript pra declarar + atribuir o campo. Equivalente a `this.codigo = codigo` + declaração separada em outras linguagens.

### Genéricos pra type safety

```typescript
async get<T>(path: string): Promise<T>
```

Caller chama `client.get<PedidoResumo>('/pedidos/1')` e o retorno fica tipado. **Atenção:** isso é só hint do TypeScript em compile-time. Em runtime, `response.json() as Promise<T>` é cast — se a API retornar JSON inválido, vai quebrar mais tarde em outro lugar. Pra validação real, usar Zod.

### Pub-sub via CustomEvent pra desacoplar

Quando o cliente HTTP detecta 401 (sessão expirada), ele NÃO pode chamar `useNavigate()` direto — hooks só funcionam dentro de componentes React. Solução: solta um `CustomEvent` no `window` e um listener (dentro do BrowserRouter) faz a navegação.

```typescript
// No client.ts (não-componente):
window.dispatchEvent(new CustomEvent('finbot:sessao-expirada'))

// No App.tsx (componente que tem useNavigate):
useEffect(() => {
  const handler = () => navigate('/erro?motivo=sessao-expirada')
  window.addEventListener('finbot:sessao-expirada', handler)
  return () => window.removeEventListener('finbot:sessao-expirada', handler)
}, [navigate])
```

Padrão "event bus" sem precisar de biblioteca. Em Angular o equivalente seria `Subject`/`Observable` ou `HttpInterceptor`.

### Filtro de params vazios antes de adicionar à URL

```typescript
if (value !== undefined && value !== null && value !== '') {
  url.searchParams.append(key, String(value))
}
```

Sem isso, fica fácil acabar com `?status=undefined` (que vai como string literal "undefined" pro backend) ou `?status=` (param vazio). O backend pode confundir com presença real.

## Comparação com Angular HttpClient

| Conceito | React aqui | Angular |
|---|---|---|
| Cliente HTTP | `fetch` + wrapper próprio | `HttpClient` injetado via DI |
| Serviço de domínio | `pedidos.ts` (funções soltas) | `@Injectable() class PedidosService` |
| Tipagem | `client.get<T>(path)` | `http.get<T>(path)` |
| Interceptor pra 401 | `CustomEvent` + listener no Router | `HttpInterceptor` |
| Testes | `vi.mock`, `vi.stubGlobal` | `HttpClientTestingModule` |
| Async | `async/await` | `Observable` (RxJS) |
| Cancel | `AbortController` | `unsubscribe()` |

Trade-off: Angular força padrão; React deixa você escolher. Para projeto pequeno, fetch + wrapper é leve. Para projeto grande/complexo, Axios (que tem retry, interceptors built-in, cancelamento) pode valer.

## Pontos de atenção típicos desse padrão

1. **JSON sem validação runtime** — `as T` é só cast. Usar Zod pra validar (`schema.parse(body)`)
2. **Sem timeout default** — vale adicionar `AbortController` com timeout pra evitar fetch travar indefinidamente
3. **Sem retry automático** — se quiser retry em GETs, usar TanStack Query (que retém esse tipo de lógica)
4. **`fetch` não rejeita em status HTTP de erro** — só rejeita em network failure. Por isso o wrapper checa `response.ok` manualmente

## Pra aprofundar

- Web Fetch API spec (cobre tudo do `fetch` nativo)
- `AbortController` — pra cancelar requests
- TanStack Query — biblioteca acima do client que cuida de cache, retry, refetch
- Zod — validação runtime de schemas TypeScript
- Axios vs fetch — comparação técnica e quando vale cada um
