# React — estado derivado e composição de hooks

## Contexto da dúvida

Durante revisão da FE-08 (CabecalhoApp + ResumoMes), foram explicados dois padrões que aparecem juntos no `CabecalhoApp`: **composição de hooks** (um componente que consome vários hooks customizados) e **estado derivado** (valores calculados no render em vez de guardados em `useState`).

## Resumo destilado

### Composição de hooks

Um componente não fica preso a um hook só. O `CabecalhoApp` consome **dois** hooks customizados ao mesmo tempo:

```tsx
const { requisitante } = useAuth()          // quem está logado
const { data: resumo, isLoading } = useResumo()  // resumo do mês (via API)
```

Cada hook é uma "fonte de dado" independente, com seu próprio ciclo de vida e seu próprio estado de loading. O componente é só o ponto onde elas se encontram. Não há ordem nem dependência entre eles — React executa os dois a cada render.

Isso é o "Lego" do React: hooks customizados encapsulam lógica (busca, cache, auth) e o componente combina os que precisar. Comparável a injetar múltiplos `services` num componente Angular — só que aqui a "injeção" é a chamada do hook.

### Estado derivado — não guarde o que dá pra calcular

```tsx
const nomeExibido = requisitante?.nome ?? '…'
const primeiroNome = nomeExibido.split(' ')[0]
```

`primeiroNome` **não** é `useState`. É calculado direto no corpo do componente, a cada render, a partir de algo que já existe (`requisitante`). 

Regra mental: **se um valor pode ser derivado de props/estado/dados que você já tem, não crie `useState` pra ele.** Guardar em estado só introduz risco de dessincronização (o estado "esquece" de atualizar quando a origem muda). Calcular no render é sempre consistente — o React re-renderiza, o valor recalcula.

`useState` é só pra coisa que o React não consegue recalcular sozinho: input do usuário, toggles, seleções — coisas que vêm "de fora" do fluxo de dados.

### Renderização defensiva — o tri-state de dado assíncrono

Dado que vem de API tem **três** estados, não dois. O `CabecalhoApp` trata os três:

1. **Carregando** (`isLoading`) → mostra skeleton
2. **Carregou, tem dado** (`resumo` existe) → mostra o resumo completo
3. **Carregou, sem dado** (`resumo` é `undefined`) → fallback ("Olá {nome}." sem o resto)

```tsx
{isLoading ? (
  <Skeleton />
) : resumo ? (
  <ResumoCompleto />
) : (
  <Fallback />
)}
```

Optional chaining (`requisitante?.nome`) e nullish coalescing (`?? '…'`) são as ferramentas pra não quebrar quando o dado ainda não chegou. Esquecer o terceiro estado é a causa clássica de `Cannot read property of undefined`.

### Detalhe de JSX: `{' '}` é espaço explícito

```tsx
Olá {primeiroNome}.{' '}
{resumo.pendentes.quantidade}
```

JSX **colapsa** espaços em branco entre expressões e quebras de linha. Pra forçar um espaço literal entre dois trechos, usa-se `{' '}`. Sem ele, `...{primeiroNome}.` e `{quantidade}...` ficariam grudados.

## Pontos-chave

- Um componente pode consumir **vários hooks customizados** — cada um é uma fonte de dado independente.
- **Estado derivado**: se dá pra calcular a partir do que você já tem, calcule no render — não use `useState`.
- `useState` é só pra entrada que o React não recalcula sozinho (input, toggle, seleção).
- Dado assíncrono tem **3 estados**: carregando / com-dado / sem-dado. Trate os três.
- `?.` e `?? ` protegem contra dado que ainda não chegou.
- `{' '}` força um espaço literal em JSX (que normalmente colapsa espaços).

## Pra aprofundar

- `useMemo` — quando o cálculo derivado é caro, memoriza o resultado entre renders
- "Derived state" anti-pattern — artigos clássicos do React sobre por que `useState` + `useEffect` pra sincronizar é quase sempre errado
- Regras dos hooks (sempre no topo, nunca dentro de `if`/loop) e por quê
- `useResumo` vs `usePedidos` — comparar as duas `queryKey` (`['resumo']` constante vs `['pedidos', filtros]`) reforça o conceito de "queryKey é identidade de cache" do TanStack Query
