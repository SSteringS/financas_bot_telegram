---
name: Sprint 03 front tasks complete
description: FE-015, FE-016, FE-017 all merged to integration/03-folha-pagamento as of 2026-06-04
type: project
---

Sprint 03 (EVO-09 Folha de Pagamento) frontend tasks completed on 2026-06-04:

- **FE-015** (PR #98): Tela Funcionários — lista + formulário CRUD. Commit `b1f7648`.
- **FE-016** (PR #102): Tela Folha do Funcionário — vales, adiantamentos, fechamentos. Commit `080a98e`.
- **FE-017** (PR #103): ModalFechamento com cálculo em tempo real. Commit `21f22f2`.

All 3 merged into `integration/03-folha-pagamento`.

**Why:** Integration PR #94 (`integration/03-folha-pagamento` → `develop`) is open and awaiting human review/merge.

**How to apply:** Next FE session starts fresh from `develop` (or the next integration branch). The folha domain types (`src/types/folha.ts`) and API (`src/api/folha.ts`) are complete with all EVO-09 endpoints. The hook `useFolhaFuncionario` is the entry point for the Tela Folha.

Key implementation notes:
- DELETE adiantamento: `/api/funcionarios/adiantamentos/{id}` (NOT `/api/adiantamentos/{id}`)
- `parcelasRestantes` is computed by backend, not stored in DB
- `tipoConta` is a plain String in JSON (not enum), mapped as `'CORRENTE' | 'POUPANCA'`
- `mesReferencia` in Fechamento comes as `"YYYY-MM-DD"` (first day of month); check with `startsWith("YYYY-MM")`
- ModalFechamento interface props are frozen: `open, funcionarioId, mes, salarioBase, totalVales, listaAdiantamentosAtivos, onClose, onConfirmado`
