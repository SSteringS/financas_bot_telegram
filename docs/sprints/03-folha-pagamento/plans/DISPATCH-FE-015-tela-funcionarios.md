# DISPATCH — FE-015-tela-funcionarios (single-task)

> **Quando usar:** após BE-025 mergeada em `develop`.
> Os endpoints de funcionário precisam estar em `develop` para testar contra a API real.
> ⚠️ **FE-016 não deve iniciar antes desta mergear** — FE-016 depende de `src/types/folha.ts`
> e `src/api/folha.ts` que são criados aqui.

---

## Pré-condições (git)

- **`feature/be-025-crud-funcionario` mergeada em `develop`** — endpoints GET/POST/PUT/DELETE /api/funcionarios disponíveis.
  Confirmar: `git log origin/develop --oneline | grep be-025`.

---

## O prompt (cole tudo numa sessão `--agent frontend`)

```
Task: FE-015 — Tela Funcionários — lista + formulário de cadastro/edição.

Localize e leia o plano completo (initialPrompt orienta o Glob).
Leia também:
- docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §6 (Tela: Lista de Funcionários)
- docs/decisions/0016-evo09-folha-pagamento.md
- Padrão existente: src/paginas/Home.tsx, src/api/pedidos.ts, src/hooks/usePedidos.ts

## A TASK

Criar a nova aba "Folha de pagamento" no front com a lista de funcionários e formulário de cadastro/edição.

### Criar:
1. src/paginas/folha/FuncionariosPage.tsx — lista de funcionários ativos com tabela.
2. src/components/folha/FuncionarioForm.tsx — formulário de cadastro/edição com campos condicionais.
3. src/api/folha.ts — funções: listarFuncionarios(), criarFuncionario(), atualizarFuncionario(), desativarFuncionario().
4. src/types/folha.ts — types: Funcionario, FormaPagamento (enum), FuncionarioRequest.
   ⚠️ ATENÇÃO: FE-016 vai ESTENDER este arquivo — criar já pensando em extensão (Vale, Adiantamento, Fechamento entrarão aqui).

### Modificar:
5. src/App.tsx — adicionar rota /folha e /folha/funcionarios/:id (esta segunda apontará para FE-016 depois).
6. Navegação/menu principal — adicionar aba "Folha de pagamento".

## REGRAS DURAS

1. Branch: `feature/fe-015-tela-funcionarios` saindo de `origin/develop`.
2. Território: SÓ `frontend/`. Zero `financas_bot_telegram/`.
3. NUNCA a partir de outra feature branch — sempre de origin/develop (regra do agent).
4. Badge laranja para conta_propria=false (ex: pagamento para familiar).
5. Campos condicionais no formulário:
   - PIX selecionado → exibir só chave_pix; ocultar campos TED.
   - TED selecionado → exibir banco, agência, conta, tipo_conta; ocultar chave_pix.
   Usar watch('forma_pagamento') do react-hook-form.
6. Clique na linha da tabela deve navegar para /folha/funcionarios/{id} (rota de FE-016 — pode ser um link vazio por ora se FE-016 ainda não existir).
7. 1 commit: `feat(FE-015): tela funcionarios — lista + formulario cadastro/edicao`.
8. NÃO mergeie. PR pra develop após status report + Reviewer.

## VERIFICAÇÃO ANTES DE CODAR

Confirmar o contrato real da API:
  curl -s https://api.satyansaita.com/v3/api-docs | jq '.paths | keys[]' | grep funcionario
OU verificar no código de BE-025 (já em develop) qual o path exato e o shape do response.

## TESTES

- Componente (mínimo 3):
  FuncionarioForm: ao selecionar PIX → campos TED ocultos; ao selecionar TED → campos PIX ocultos; submit com form válido chama callback.
- Integração leve (mínimo 3):
  FuncionariosPage: lista renderiza com mock de API; badge laranja aparece para conta_propria=false; "Novo funcionário" abre o form.

`testes_novos` ≥ 6.

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/FE-015-tela-funcionarios.md`

Incluir:
- Resultado da verificação do contrato da API (path exato + shape do response).
- Decisão sobre como implementar campos condicionais (watch + render condicional, ou outra abordagem) + justificativa.
- Confirmar que src/types/folha.ts e src/api/folha.ts foram criados e são extensíveis para FE-016.

## SE QUEBRAR

Cenário — endpoint /api/funcionarios retorna shape diferente do esperado:
  Adaptar os types em src/types/folha.ts para o shape real. Anotar a divergência no status report.

Cenário — react-hook-form com watch não funciona como esperado para campos condicionais:
  Alternativa: usar useState para controlar forma_pagamento e renderizar condicionalmente.
  Documentar a decisão no status report.

Pare ao final do status report. Não mergeie.
```

---

## Notas pro humano

- **Pode correr em paralelo com BE-026, BE-027, BE-028** (front é disjunto do back).
- ⚠️ **FE-016 deve iniciar somente após esta mergear** — `src/types/folha.ts` e `src/api/folha.ts` criados aqui são base de FE-016.
- **Estimativa:** 1.5-2h (formulário com campos condicionais é o ponto mais trabalhoso).
- **Após merge desta + merge de BE-028:** despachar FE-016.

## Referências

- `docs/sprints/03-folha-pagamento/plans/FE-015-tela-funcionarios.md`
- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §6
