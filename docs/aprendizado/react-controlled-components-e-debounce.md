# React — componentes controlados e debounce

## Contexto da dúvida

Durante revisão da FE-06 (FiltroStatus, SeletorMes, BarraBusca), foram explicados os conceitos de componente controlado (controlled component) e debounce — fundamentais pra qualquer formulário/filtro em React.

## Resumo destilado

### Componente controlado

Em React, inputs/seletores podem ser:

- **Uncontrolled** — o DOM segura o valor, você lê via `ref` quando precisa. Raro no React idiomático.
- **Controlled** — o **estado React segura o valor**; o input só "exibe" o estado; toda mudança passa por `onChange`. Esse é o padrão.

Assinatura sempre `value={...} onChange={...}`:

```tsx
<input value={x} onChange={(e) => setX(e.target.value)} />
```

Fluxo: usuário digita → `onChange` dispara → você atualiza o estado → re-render → input mostra o novo estado. O input **nunca muda sozinho**, só reflete o estado.

Em Angular, `[(ngModel)]` é two-way binding — controlado por baixo dos panos, mas a mecânica fica escondida. Em React você vê o ciclo explícito.

### O padrão `value`/`onChange` no nível do componente

Não vale só pra `<input>` — vale pra **qualquer componente de seleção**. Um `FiltroStatus`, um `SeletorMes`, etc. recebem `value` (seleção atual) e `onChange` (callback). Eles **não guardam estado próprio** — o pai guarda. É o "pai controla, filho avisa" aplicado a seletores. O componente é "burro": mostra o que recebe, avisa quando clicam.

### Debounce

**Problema:** se um input de busca dispara `onChange` a cada tecla, e o `onChange` chama a API, digitar "energia" = 7 chamadas. Desperdício + UI piscando.

**Debounce:** "espera o usuário parar de digitar por X ms antes de agir".

**Implementação:**

```tsx
const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

function handleChange(e) {
  const novoValor = e.target.value
  setInputValue(novoValor)              // estado interno atualiza na hora

  if (debounceRef.current) clearTimeout(debounceRef.current)  // cancela o timer anterior
  debounceRef.current = setTimeout(() => {
    onChange(novoValor)                 // só dispara após 300ms de quietude
  }, 300)
}

// cleanup ao desmontar
useEffect(() => () => {
  if (debounceRef.current) clearTimeout(debounceRef.current)
}, [])
```

A cada tecla, cancela o timer anterior e agenda um novo. Só quando o usuário **para** por 300ms o timer "sobrevive" e dispara.

`useRef` (não `useState`) pro timer — não queremos re-render ao mexer no timer, só "lembrar" dele.

### Padrão "estado interno + propagação debounced"

A `BarraBusca` é híbrida:
- **Estado interno** (`inputValue` via `useState`) — o input é controlado por ele, responde **instantâneo** ao digitar
- **`value`/`onChange` externos** — o `onChange` é debounced
- **`useEffect([value])`** sincroniza o estado interno quando o externo muda (ex: reset de filtros limpa o campo)

Sem o estado interno, o input só atualizaria visualmente 300ms depois — UX horrível. Com ele: input instantâneo + query econômica. Padrão muito comum, vale internalizar.

### Cleanup de timers é obrigatório

```tsx
useEffect(() => {
  return () => {
    if (debounceRef.current) clearTimeout(debounceRef.current)
  }
}, [])
```

Sem isso, se o componente desmonta com timer pendente, o `onChange` dispara depois num componente que não existe mais → warning ou bug.

## Testando debounce com fake timers

```tsx
beforeEach(() => vi.useFakeTimers())
afterEach(() => vi.useRealTimers())

it('debounce: digitação rápida = uma única chamada', () => {
  const onChange = vi.fn()
  render(<BarraBusca value="" onChange={onChange} />)
  const input = screen.getByRole('searchbox')

  fireEvent.change(input, { target: { value: 'a' } })
  act(() => { vi.advanceTimersByTime(100) })
  fireEvent.change(input, { target: { value: 'ab' } })
  act(() => { vi.advanceTimersByTime(100) })
  fireEvent.change(input, { target: { value: 'abc' } })
  act(() => { vi.advanceTimersByTime(300) })

  expect(onChange).toHaveBeenCalledTimes(1)
  expect(onChange).toHaveBeenCalledWith('abc')
})
```

- `vi.useFakeTimers()` substitui `setTimeout` real por fake controlável — sem isso, testar debounce exigiria `sleep(300)` real (lento, flaky)
- `vi.advanceTimersByTime(ms)` "avança o relógio" instantaneamente
- `act(() => {...})` envolve operações que disparam updates de estado React — garante que o React processou tudo antes do assert
- `screen.getByRole('searchbox')` — React Testing Library incentiva buscar por papel/role (como usuário/leitor de tela vê), não por classe CSS/id

## Comparação com Angular

| Conceito | React | Angular |
|---|---|---|
| Input ligado a estado | controlled (`value`/`onChange`) | `[(ngModel)]` |
| Debounce | `setTimeout` + `clearTimeout` manual (ou hook custom) | `debounceTime()` (RxJS, "de graça") |
| Timer sem re-render | `useRef` | propriedade de classe |
| Teste de tempo | `vi.useFakeTimers()` + `advanceTimersByTime` | `fakeAsync()` + `tick()` |

Angular tem `debounceTime` elegante via RxJS. React faz na mão ou via hook customizado (`useDebounce`). Trade-off: RxJS poderoso mas com curva; `setTimeout` explícito e sem dependência.

## Pra aprofundar

- Hook customizado `useDebounce` — extrai a lógica de debounce pra reuso
- `useDeferredValue` (React 18) — alternativa nativa pro problema "input rápido, processamento caro"
- Uncontrolled components + `useRef` — quando vale (formulários grandes, integração com libs não-React)
- React Testing Library: filosofia "test behavior, not implementation"; query priority (`getByRole` > `getByLabelText` > `getByText` > `getByTestId`)
- `clsx` / `cn` — utilitários pra construir className condicional de forma limpa
