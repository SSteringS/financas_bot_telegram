# React — props, renderização condicional e listas

## Contexto da dúvida

Durante revisão da FE-05 (PedidoCard, StatusBadge, formato.ts, Showcase), foram explicados os conceitos de props, renderização condicional e renderização de listas — os blocos básicos de componentes visuais em React.

## Resumo destilado

### Props = entrada de dados do componente

```tsx
interface StatusBadgeProps {
  status: StatusPedido
}

export function StatusBadge({ status }: StatusBadgeProps) {
  // usa `status`
}
```

`props` é o "parâmetro de entrada" do componente — equivalente ao `@Input()` do Angular, mas em React é literalmente o parâmetro da função. Convenção: declarar `interface XxxProps`, desestruturar no parâmetro. Quem usa passa via atributo JSX: `<StatusBadge status={pedido.status} />`.

### Callback como prop = "pai controla, filho avisa"

```tsx
interface PedidoCardProps {
  pedido: PedidoResumo
  onAbrirComprovante: () => void  // callback
}
```

O componente recebe uma FUNÇÃO como prop e a chama quando algo acontece (clique, etc). O componente não sabe o que aquilo significa — só avisa. Quem decide o que fazer é quem usou o componente. Equivalente ao `@Output() + EventEmitter` do Angular.

### Renderização condicional — 3 padrões (React não tem `*ngIf`)

**1. Early return:**
```tsx
if (status === StatusPedido.PAGO) return <span>Pago</span>
return <span>Pendente</span>
```

**2. Ternário no JSX:**
```tsx
{pago ? <span>Pago</span> : <span>Pendente</span>}
```

**3. Short-circuit `&&`:**
```tsx
{pago && pedido.temComprovante && <button>Ver comprovante</button>}
```
`condição && <JSX>` — false renderiza nada, true renderiza o JSX.

**Pegadinha do `&&`:** se a condição for número `0`, React renderiza o `0` na tela (`0 && x` retorna `0`). Pra condições numéricas, use `cond > 0 && ...` ou `Boolean(cond) && ...`.

### Listas com `.map()` + `key`

```tsx
{pedidos.map((pedido) => (
  <PedidoCard key={pedido.id} pedido={pedido} onAbrirComprovante={...} />
))}
```

React não tem `*ngFor`. Usa `.map()` do array JS pra transformar cada item em JSX.

**O `key` é obrigatório e crítico:** é como o React identifica cada item entre re-renders. `key` deve ser um **ID estável e único** (nunca o índice do array, exceto listas estáticas). Sem `key` bom, React confunde itens ao reordenar/inserir/remover → bugs visuais e perda de estado. Equivalente ao `trackBy` do Angular, mas mandatório.

### Componente apresentacional puro

Componente sem `useState`, sem `useEffect`, sem nada — só recebe props e retorna JSX. Função pura: mesma entrada → mesma saída. Fácil de testar, fácil de raciocinar. `StatusBadge` é o exemplo mínimo.

### Composição

Componentes usam outros componentes. `PedidoCard` renderiza `<StatusBadge .../>` dentro dele. Mesma ideia de componentes aninhados do Angular.

### Variável derivada no corpo

```tsx
export function PedidoCard({ pedido }) {
  const pago = pedido.status === StatusPedido.PAGO  // recalculado a cada render
  // ...
}
```

Como a função re-executa a cada render, variáveis derivadas são recalculadas sempre. OK pra cálculo barato. Se for caro, usar `useMemo` pra memoizar.

## Bibliotecas de formatação

- **`toLocaleString`** — nativo do JS, formata número por locale: `(1500).toLocaleString('pt-BR', {style:'currency', currency:'BRL'})` → `"R$ 1.500,00"`. Não precisa de lib pra moeda.
- **`date-fns`** — lib de datas tree-shakeable (só entra no bundle o que importa), imutável, com locale pt-BR. `format(parseISO("2026-05-04"), "d 'de' MMMM", {locale: ptBR})` → `"4 de maio"`. Muito melhor que o `Date` nativo do JS pra formatação.

## Pra aprofundar

- `useMemo` / `useCallback` — memoização pra evitar recálculo/recriação cara
- `children` como prop — composição mais flexível
- Prop drilling e quando ele vira problema (solução: Context)
- `React.memo` — evitar re-render de componente quando props não mudaram
- Storybook — ferramenta dedicada pra catálogo de componentes (o `_Showcase` é a versão caseira)
- Testar componentes apresentacionais com React Testing Library (`render`, `screen`, `getByRole`)
