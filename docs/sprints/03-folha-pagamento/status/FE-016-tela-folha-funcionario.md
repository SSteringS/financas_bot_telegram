---
task: FE-016
titulo: "Tela Folha do Funcionário — vales, adiantamentos, fechamentos"
data: 2026-06-04
branch: feature/fe-016-tela-folha-funcionario
responsavel: claude-front
estado: concluido
gates:
  build: ok
  lint: ok
  testes: ok
  testes_total: 78
  testes_novos: 8
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits:
  - placeholder
pr: null
desvios: 1
pendencias_humano: 0
---

# FE-016 — Tela Folha do Funcionário — vales, adiantamentos, fechamentos

## O que foi feito

Implementada a tela `/folha/funcionarios/:id` substituindo o stub criado em FE-015. A tela agrega três seções independentes (vales, adiantamentos, fechamentos) alimentadas por quatro queries TanStack Query via `useFolhaFuncionario` hook.

**Novos arquivos:**
- `src/types/folha.ts` — estendido com `Vale`, `ValeRequest`, `Adiantamento`, `AdiantamentoRequest`, `Fechamento`
- `src/api/folha.ts` — estendido com `listarVales`, `criarVale`, `listarAdiantamentos`, `criarAdiantamento`, `cancelarAdiantamento`, `listarFechamentos`
- `src/hooks/folha/useFolhaFuncionario.ts` — hook agregador com 4 queries + 3 mutations + `invalidarPosFechamento()`
- `src/components/folha/ValeForm.tsx` — form inline de vale (controlled state)
- `src/components/folha/AdiantamentoForm.tsx` — form inline de adiantamento com `valorTotal` computado
- `src/components/folha/ValesSection.tsx` — seção com seletor de mês (6 meses) + lista + ValeForm
- `src/components/folha/AdiantamentosSection.tsx` — seção de adiantamentos com `parcela X/N` + cancelar
- `src/components/folha/FechamentosSection.tsx` — accordion de fechamentos com expand de `observacao`
- `src/components/folha/ModalFechamento.tsx` — stub com props interface exata para FE-017
- `src/components/folha/FolhaFuncionario.test.tsx` — 8 testes novos
- `src/paginas/folha/FolhaFuncionarioPage.tsx` — substituiu stub pela implementação completa

---

## Desvios do plano

**1 desvio:** Props das seções `ValesSection` e `AdiantamentosSection` usam `Promise<unknown>` em vez de `Promise<void>` para `onCriarVale`/`onCriarAdiantamento`/`onCancelarAdiantamento`. O hook `useFolhaFuncionario` expõe `mutateAsync` que retorna `Promise<Vale>` e `Promise<Adiantamento>` — TypeScript rejeita atribuição para `Promise<void>`. Solução: `Promise<unknown>` é structural-compatible e não força wrapping desnecessário no page. Sem impacto funcional.

---

## Decisões tomadas durante a execução

- **`fecharMes()` NÃO adicionado em `folha.ts`**: o plano de FE-017 explicita que ele adicionará a função `fecharMes()` em `folha.ts`. FE-016 só expõe `invalidarPosFechamento()` para FE-017 usar após o POST.
- **`valorTotal` computado no AdiantamentoForm**: usuário preenche `valorParcela × numParcelas`; total é calculado e exibido como preview antes de submeter. Evita erro do usuário ao somar manualmente.
- **ModalFechamento stub com interface completa**: props definidas são o contrato que FE-017 seguirá sem alteração — `open, funcionarioId, mes, salarioBase, totalVales, listaAdiantamentosAtivos, onClose, onConfirmado`.
- **`parcelasPagas + 1` na exibição da parcela corrente**: `parcelasPagas=0` = 1ª parcela pendente → exibido como `1/N`. Faz sentido semântico para o usuário ("você está na parcela 1 de 3").
- **Endpoint DELETE**: `/api/funcionarios/adiantamentos/{id}` — verificado no `FolhaController.java` (prefix `/api/funcionarios` + `@DeleteMapping("/adiantamentos/{adiantamentoId}")`). Plano havia anotado `/api/adiantamentos/{id}` (incorreto).
- **Decisão §7 (vales na lista do Pedro)**: Opção A aprovada pelo PO — vales NÃO aparecem na lista do Pedro; `Home.tsx` intocado. Anotado no plano FE-016.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

**FE-017** (ModalFechamento):
- Interface de props já definida em `ModalFechamento.tsx` — seguir sem alterar
- Adicionar `fecharMes(funcionarioId, mes, ajuste)` em `src/api/folha.ts`
- Após POST bem-sucedido, chamar `invalidarPosFechamento()` disponível no hook
- `totalVales` passado ao modal já exclui vales fechados (`vale.fechado === false`)
- Erro 409 = mês já fechado; mapear para mensagem clara

---

## Padrões técnicos

**Padrão: Props-down data flow (Prop Drilling controlado)**
As seções (`ValesSection`, `AdiantamentosSection`, `FechamentosSection`) recebem dados e handlers via props, sem queries internas. Toda a orquestração fica no `useFolhaFuncionario` hook. Esse padrão favorece testabilidade (seções testáveis como pure components sem mock de queries) e separa responsabilidade de fetch do rendering.

**Hook agregador com queries independentes:**
TanStack Query com 4 `useQuery` independentes — cada seção tem seu próprio loading/error state. O `isLoading` combinado só bloqueia a tela inicial (loading splash). Após carregar, falha isolada em uma query não derruba as demais.

**`mesFechado` derivado por string prefix:**
`mesReferencia` vem do backend como `"YYYY-MM-DD"` (LocalDate first day of month). Comparação `f.mesReferencia.startsWith(mesSelecionado)` onde `mesSelecionado` é `"YYYY-MM"` — adequado e sem overhead de parse de data.

---

## Arquivos criados/modificados

- `src/types/folha.ts` (modificado: Vale, ValeRequest, Adiantamento, AdiantamentoRequest, Fechamento)
- `src/api/folha.ts` (modificado: 6 funções de API adicionadas)
- `src/hooks/folha/useFolhaFuncionario.ts` (novo)
- `src/components/folha/ValeForm.tsx` (novo)
- `src/components/folha/AdiantamentoForm.tsx` (novo)
- `src/components/folha/ValesSection.tsx` (novo)
- `src/components/folha/AdiantamentosSection.tsx` (novo)
- `src/components/folha/FechamentosSection.tsx` (novo)
- `src/components/folha/ModalFechamento.tsx` (novo — stub FE-017)
- `src/components/folha/FolhaFuncionario.test.tsx` (novo — 8 testes)
- `src/paginas/folha/FolhaFuncionarioPage.tsx` (modificado: implementação completa)
