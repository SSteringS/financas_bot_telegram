---
name: qualidade-de-testes
description: >
  O que faz um teste bom vs. ruim — testar comportamento nao implementacao, anti-patterns
  que passam no CI (snapshot, mock excessivo, assertion ausente), cobertura que importa,
  nomenclatura como documentacao viva. Carregar quando a task adiciona testes nao-triviais
  (implementador) ou quando o reviewer avalia tasks com testes_novos >= 5.
load_pattern: shared
used_by: [backend, frontend, reviewer]
created: 2026-05-30
adr: 0015
status: ativa
---

# Skill — Qualidade de Testes

## Quando carregar (gatilho explícito)

- **Backend:** task cria testes de lógica não-trivial (service, domain, adapter) — garantir
  que os testes adicionados têm valor real.
- **Frontend:** task cria testes de componente ou hook com lógica — além de "verde", avaliar
  se está testando comportamento.
- **Reviewer:** `testes_novos >= 5` no frontmatter do status report — ler ao menos 2 testes
  aleatoriamente antes de aceitar o gate `testes: ok`. Teste verde sem assertion é reprovação.
- **Sinal concreto:** aparece `@Test`, `it(`, `describe(`, `render(`, `verify(`, `mock`,
  `testes_novos` no contexto.

## Princípio central

> "O teste deve falhar quando o **comportamento** muda, não quando a **implementação** muda."
> — Kent C. Dodds

Um teste que quebra ao renomear um método privado, mudar um nome de variável interna, ou
refatorar sem alterar o comportamento externo está testando implementação — não tem valor
de regressão real.

## Anti-patterns que passam no CI (mas não têm valor)

### Backend (JUnit + Mockito)

| Anti-pattern | Sintoma | Problema |
|---|---|---|
| **Mock excessivo** | 8+ `@Mock` / `when(...).thenReturn(...)` pra um único teste | Testa o código de glue, não a lógica. Se a lógica mudar internamente, o teste ainda passa. |
| **Assertion ausente** | `verify(mock.metodo())` sem checar resultado; `assertDoesNotThrow` como única assertion | Teste não falha nunca — verde por omissão, não por correção. |
| **Teste de getter/setter** | `assertEquals("x", obj.getNome())` após `obj.setNome("x")` | Zero valor — só confirma que Java funciona. |
| **Arrange gigante, assert trivial** | 30 linhas de setup, 1 linha `assertNotNull(resultado)` | O setup não está testando nada de útil; a assertion é vazia. |
| **Nome que mente** | `deveProcessarComSucesso()` sem nenhuma verificação de "sucesso" | Documentação falsa — quem lê o nome acredita que o comportamento foi verificado. |

### Frontend (Jest + React Testing Library)

| Anti-pattern | Sintoma | Problema |
|---|---|---|
| **Snapshot test frágil** | `expect(container).toMatchSnapshot()` em componente que muda frequentemente | Quebra em qualquer refactor cosmético; custo de manutenção alto, valor de regressão baixo. |
| **Testar estado interno** | `expect(component.state.isLoading).toBe(true)` | Testa implementação; quebra ao refatorar sem mudar comportamento. |
| **Mock de tudo** | `jest.mock('../hooks/usePedidos')` sem deixar nada real rodar | Teste testa apenas que o mock foi chamado. |
| **`getByTestId` em vez de `getByRole`** | `getByTestId('btn-salvar')` | Não testa acessibilidade; frágil (muda com refactor de HTML). |
| **`waitFor` com side-effect** | `await waitFor(() => userEvent.click(btn))` | Uso incorreto do `waitFor` — deve conter assertion, não ação. |

## Cobertura que importa

**Vale:** lógica condicional, caminhos de erro, edge cases (valor zero, lista vazia,
string nula), transformações de dado, regras de negócio.

**Não vale para meta de cobertura:**
- Getters/setters e construtores triviais.
- DTOs e records sem lógica.
- Métodos delegados (método A chama B sem lógica própria).
- Config classes e beans sem comportamento.

**Heurística BE:** cobertura de branch > cobertura de linha. Um `if` com dois caminhos
precisa de dois testes — um pra cada branch.

**Heurística FE:** cada estado visível do componente tem teste.
`isLoading=true`, `error!=null`, `data=[]`, `data=[items]` — 4 estados, ≥4 casos de teste.

## Nomenclatura como documentação viva

**Backend:**
```java
// Ruim — o que é "sucesso"?
@Test void deveProcessarComSucesso() { ... }

// Bom — comportamento + condição
@Test void deveRetornarErroQuandoValorNegativo() { ... }
@Test void deveCalcularSaldoConsiderandoTodasAsMovimentacoes() { ... }
```

**Frontend:**
```ts
// Ruim
it('renders correctly', () => { ... })

// Bom — comportamento observável + condição
it('exibe skeleton enquanto carrega pedidos', () => { ... })
it('exibe mensagem de erro quando a API retorna 500', () => { ... })
it('desabilita botao salvar quando formulario invalido', () => { ... })
```

## Guia rápido para o Reviewer

Quando `testes_novos >= 5`:

1. Escolher 2 testes aleatórios — ler o corpo inteiro (não só o nome).
2. Perguntar: se eu mudar o comportamento que este teste deveria proteger, ele ficaria vermelho?
3. Verificar: há ao menos uma assertion que testa o **resultado** (não só que o mock foi chamado)?
4. Verificar: o nome descreve o comportamento esperado em condição específica?

Se qualquer resposta for "não" → listar no veredito com o trecho do teste + o problema.
Testes sem assertion ou com nome que mente são reprovação, mesmo com `testes: ok` no frontmatter.

## Ler junto

- Skill `boas-praticas-react` — `userEvent`, `waitFor`, hierarquia de seletores RTL.
- Skill `padroes-qualidade-codigo` — SOLID e onde a lógica testável deve morar.
- `docs/runbooks/PRE-MERGE-CHECKLIST.md` — gate `testes` e `cobertura_pct`.
