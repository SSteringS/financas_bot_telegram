# FE-12 — Resumo parametrizado + contadores via resumo

## Contexto

Dois bugs identificados na revisão da Fase 3 do front:

- **Bug A (FE-07, contadores):** no `Home.tsx`, `contadores.pendente` e `contadores.pago` são derivados de `data.items.filter(...)`, sendo que `data.items` é a página atual já filtrada pelo status ativo. Resultado: clicar em "Pendente" zera "Pago" mesmo havendo pagos. Demonstrável manualmente: na Home em maio, clique entre os filtros e veja os contadores colapsarem.
- **Bug B (FE-08, header travado):** o `CabecalhoApp` chama `useResumo()` que tem `queryKey: ['resumo']` constante e o endpoint `/api/v1/resumo` não aceita mês. Trocar mês na UI não muda o cabeçalho.

A correção estrutural está em BE-16: o backend passa a aceitar `mes` e `busca` no `/api/v1/resumo` e devolve também o agregado `todos`. Esta task conecta o front no novo contrato.

## Decisão de design

Header e contadores compartilham a **mesma query** — ambos chamam `useResumo(mes, busca)`. TanStack Query dedupes via queryKey (`['resumo', mes, busca]`), uma única chamada de rede serve os dois. O cabeçalho passa a refletir o recorte ativo (mês + busca), e os contadores também.

## Branch

Continuar na branch existente `feature/frontend-fase3-completa` (ainda não mergeada em develop). Não criar branch nova.

## Contrato esperado do backend (BE-16)

```
GET /api/v1/resumo?mes=YYYY-MM&busca=texto

Response 200:
{
  "mes": "2026-05",
  "todos":     { "quantidade": 6, "total": 7227.40 },
  "pendentes": { "quantidade": 3, "total": 5239.90 },
  "pagos":     { "quantidade": 3, "total": 1987.50 }
}
```

Ambos params opcionais; default de `mes` = mês corrente. `busca` vazia/null = sem filtro.

Como BE-16 ainda está em paralelo, o MSW handler é a fonte de verdade temporária pro front — manter rigorosamente o contrato acima.

## Mudanças

### 1. `src/api/tipos.ts`

Atualizar `ResumoMes`:

```ts
export interface ResumoMes {
  mes: string // YYYY-MM (renomeado de mesAtual)
  todos: { quantidade: number; total: number }
  pendentes: { quantidade: number; total: number }
  pagos: { quantidade: number; total: number }
}
```

### 2. `src/api/pedidos.ts`

`obterResumo` aceita filtros:

```ts
export async function obterResumo(params: {
  mes?: string
  busca?: string
}): Promise<ResumoMes> {
  const qs: Record<string, string> = {}
  if (params.mes) qs.mes = params.mes
  if (params.busca) qs.busca = params.busca
  return client.get<ResumoMes>('/api/v1/resumo', qs)
}
```

### 3. `src/hooks/useResumo.ts`

```ts
export function useResumo(mes: string, busca?: string) {
  return useQuery<ResumoMes>({
    queryKey: ['resumo', mes, busca ?? ''],
    queryFn: () => obterResumo({ mes, busca: busca || undefined }),
    staleTime: 60_000,
  })
}
```

### 4. `src/components/CabecalhoApp.tsx`

Ler `mes` e `busca` da URL via `useSearchParams` (mesmo padrão do `Home`). Chamar `useResumo(mes, busca)`. Lógica do texto pode permanecer — só passa a refletir o recorte ativo.

```ts
const [searchParams] = useSearchParams()
const mes = searchParams.get('mes') ?? format(new Date(), 'yyyy-MM')
const busca = searchParams.get('busca') ?? ''
const { data: resumo, isLoading } = useResumo(mes, busca)
```

### 5. `src/paginas/Home.tsx`

Chamar `useResumo(mes, busca)` separadamente (mesma queryKey = mesma resposta de cache, sem rede extra). Derivar contadores do resumo:

```ts
const { data: resumo } = useResumo(mes, busca || undefined)

const contadores = {
  tudo: resumo?.todos.quantidade ?? 0,
  pendente: resumo?.pendentes.quantidade ?? 0,
  pago: resumo?.pagos.quantidade ?? 0,
}
```

Remover os `.filter()` sobre `data.items` (eram a fonte do bug).

### 6. `src/mocks/handlers.ts`

Atualizar handler do `/api/v1/resumo`:

- Aceitar `mes` (default `'2026-05'` pra continuar consistente com a data fake)
- Aceitar `busca`
- Filtrar `pedidosFake` por `dataPedido.startsWith(mes)` e (se houver busca) `descricao.toLowerCase().includes(busca.toLowerCase())`
- Devolver `mes`, `todos`, `pendentes`, `pagos` com `quantidade` e `total`

### 7. Conferir `src/paginas/_Showcase.tsx`

Se mencionar `useResumo` ou `mesAtual`, ajustar pra novo contrato. (Showcase é dev-only, mas convém não quebrar.)

## Testes (faltam e ficam mandatórios nesta task)

### `src/paginas/Home.test.tsx` (novo)

Setup com MSW handlers (importar `setupServer` do MSW node + handlers existentes ou customizados). Renderizar `<MemoryRouter><Home /></MemoryRouter>`. Cenários:

- **Carga inicial em maio:** após carregar, contadores mostram Tudo (6), Pendente (3), Pago (3).
- **Regressão Bug A:** clica em "Pendente" → API chamada com `status=pendente`, lista atualiza, contadores **permanecem** Tudo (6) Pendente (3) Pago (3) — não colapsam.
- **Troca de mês via URL:** abrir com `?mes=2026-04` → contadores refletem abril (9 todos, 0 pendentes, 9 pagos).
- **Busca + contadores:** digita "energia" → após debounce, contadores refletem só os matches.

### `src/components/CabecalhoApp.test.tsx` (novo)

- **Regressão Bug B:** renderizar com `?mes=2026-04` na URL → header mostra dados de abril, não de maio.
- Renderizar com sessão autenticada e resumo OK → mostra primeiro nome do requisitante + linha de pendentes.
- Sem pendentes → texto "Nenhum pedido pendente este mês."
- `isLoading` → skeleton.

### `src/components/Timeline.test.tsx` (novo)

- Recebe 3 pedidos em 2 datas distintas → renderiza 2 grupos (headers de dia).
- Pedido com `dataPedido` = hoje → label "Hoje".
- Pedido com `dataPedido` = ontem → label "Ontem".
- Lista vazia → renderiza sem quebrar (não deve, mas testa).

### `src/components/AuthGuard.test.tsx` (novo)

Mockar `useAuth` (módulo cache global — usar `vi.mock`).

- `status='loading'` → renderiza "Carregando…".
- `status='autenticado'` → renderiza children.
- `status='nao-autenticado'` → dispara `navigate('/erro?motivo=precisa-link')`.

### `src/paginas/Entrar.test.tsx` (novo)

Mockar `exchangeToken` e `useNavigate`.

- Sem `?t` na URL → navega pra `/erro?motivo=token-invalido`.
- Com `?t=valido` → `exchangeToken` chamado, navega pra `/`.
- `exchangeToken` rejeita → navega pra `/erro?motivo=token-invalido`.

## Critérios de aceite

- [ ] `npm test` passa, incluindo os 5 novos arquivos de teste
- [ ] `npm run build` sem erros TS
- [ ] `npm run lint` limpo
- [ ] Manual: rodar `npm run dev`, executar a sequência manual de teste; os dois bugs sumiram
  - Filtrar por "Pendente" / "Pago" → contadores estáveis
  - Trocar mês no `SeletorMes` → header acompanha
- [ ] Sem regressão visual nas outras telas (Entrar, Erro, Modal)

## Coordenação com o back

Plano BE-16 define o contrato definitivo. Front trabalha em paralelo via MSW. Quando back mergear em develop, alinhar com o que o servidor real devolve — qualquer divergência é discutida.

## Referências

- Plano backend: `docs/plans/BE-16-resumo-com-mes-busca.md`
- Bugs originais: `docs/aprendizado/react-tanstack-query.md` (Bug A), `docs/aprendizado/react-estado-derivado-e-composicao-de-hooks.md` (Bug B)
- Relatório de avaliação que apontou a lacuna de testes: `docs/avaliacoes/frontend-fase3-overnight.md`
