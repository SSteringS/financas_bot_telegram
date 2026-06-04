---
task: FE-017
titulo: "Modal fechamento com cálculo em tempo real"
data: 2026-06-04
branch: feature/fe-017-modal-fechamento
responsavel: claude-front
estado: concluido
gates:
  build: ok
  lint: ok
  testes: ok
  testes_total: 83
  testes_novos: 5
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits:
  - 1863ab4
pr: https://github.com/SSteringS/financas_bot_telegram/pull/103
desvios: 0
pendencias_humano: 0
---

# FE-017 — Modal fechamento com cálculo em tempo real

## O que foi feito

Substituído o stub `ModalFechamento.tsx` pela implementação completa com:
- Preview de cálculo em tempo real: `salárioBase - totalVales - totalParcelas + ajuste`
- Campo ajuste (default 0) atualiza o `valorFinal` imediatamente via `useState`
- Alerta vermelho `[data-testid="alerta-negativo"]` quando `valorFinal < 0`
- POST via `fecharMes()` (adicionada em `folha.ts`) com loading state no botão
- Erro 409 → mensagem específica "Mês já fechado"
- Callback `onConfirmado(fechamento)` dispara invalidação de cache na página
- Toast verde em `FolhaFuncionarioPage` após fechamento confirmado (desaparece em 4s)
- 5 novos testes no `ModalFechamento.test.tsx` com mock de `fecharMes`

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

- **Toast implementado na página, não no modal**: `FolhaFuncionarioPage` gerencia o `toastSucesso` state; o modal chama `onConfirmado(fechamento)` e fecha — separação limpa de responsabilidades.
- **`fecharMes()` recebe `ajuste: number`** (não `BigDecimal`): JSON serializa number JS como número; backend aceita como `BigDecimal`. Compatível.
- **Mock de `fecharMes` nos testes**: `vi.mock('../../api/folha', ...)` — o módulo é mockado inteiramente para não fazer fetch real. O teste de loading mantém a promise pendente durante a verificação e resolve ao final para evitar act() warning.
- **Alerta negativo: sem bloqueio de confirmação**: o usuário pode confirmar mesmo com valor negativo (o backend é fonte de verdade). O badge serve como aviso visual.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

Sprint 03 de front está completa: FE-015 + FE-016 + FE-017 todas em integration.
O PR #94 (`integration/03-folha-pagamento` → `develop`) aguarda revisão humana.

---

## Padrões técnicos

**Cálculo client-side + POST de confirmação:**
O preview é calculado localmente sem roundtrip: `valorFinal = salarioBase - totalVales - totalParcelas + ajuste`. Isso evita delay visual a cada keystroke no campo ajuste. O POST final é a única chamada ao backend. O resultado do POST (`observacao`, `valor`) pode divergir levemente do preview se houver mudanças nos dados entre o carregamento da tela e o fechamento — aceitável por design (informado no tooltip).

**Mocking de módulo com `vi.mock` + `vi.mocked`:**
Padrão Vitest para isolar chamadas de API: `vi.mock('../../api/folha', () => ({ fecharMes: vi.fn() }))` + `const fecharMesMock = vi.mocked(fecharMes)`. Permite controlar o retorno por teste sem afetar outros módulos.

---

## Arquivos criados/modificados

- `frontend/src/api/folha.ts` (modificado: `fecharMes()` adicionada)
- `frontend/src/components/folha/ModalFechamento.tsx` (modificado: implementação completa substituiu stub)
- `frontend/src/components/folha/ModalFechamento.test.tsx` (novo — 5 testes)
- `frontend/src/paginas/folha/FolhaFuncionarioPage.tsx` (modificado: toast de sucesso + atualização de comentário)
