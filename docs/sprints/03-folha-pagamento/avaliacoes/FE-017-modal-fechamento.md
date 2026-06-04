---
task: FE-017
sprint: 03-folha-pagamento
data: 2026-06-04
avaliador: claude-reviewer
status_report: docs/sprints/03-folha-pagamento/status/FE-017-modal-fechamento.md
veredito_codigo: aprovado_com_observacoes
veredito_final: aprovado_com_observacoes
observacoes_count: 1
roteiro_executado: false
gates_verificados_contra_realidade: ok
skills_eficazes: []
skills_gaps: []
veredito_qa: nao_aplicavel
fluxos_qa_executados: []
fluxos_qa_adicionar: []
fluxos_qa_remover: []
---

# Avaliacao — FE-017 (Modal fechamento com calculo em tempo real)

**Branch:** `feature/fe-017-modal-fechamento`
**Implementador:** claude-front
**Plano:** `docs/sprints/03-folha-pagamento/plans/FE-017-modal-fechamento.md`
**Status report:** `docs/sprints/03-folha-pagamento/status/FE-017-modal-fechamento.md`

---

## 1. Analise de codigo (Reviewer le o diff)

### Veredito de codigo: aprovado com observacoes

A implementacao atende todos os requisitos do plano. O calculo em tempo real funciona corretamente: `valorFinal = salarioBase - totalVales - totalParcelas + ajusteNum`. O `ajuste` usa `parseFloat(ajuste) || 0` — robusto contra string vazia ou invalida. O alerta `data-testid="alerta-negativo"` aparece corretamente quando `valorFinal < 0`, sem bloquear o botao de confirmar (decisao documentada no status report: backend e fonte de verdade). O tratamento de erro 409 usa `instanceof ApiError && e.codigo === 409` — correto. O `setSalvando(false)` so e chamado no catch (nao no finally), o que significa que se o POST for bem-sucedido o modal nao precisa resetar o estado de loading pois chama `onConfirmado` e fecha — comportamento correto. O toast de sucesso em `FolhaFuncionarioPage` e gerenciado via `setTimeout(..., 4000)` com limpeza correta do state.

Os 5 testes cobrem: `open=false` (nao renderiza), calculo inicial, calculo com ajuste, alerta negativo, e loading state. Cobertura adequada para o comportamento critico do componente.

Uma observacao material: bug de label herdado do stub de FE-016 que permaneceu na implementacao final.

### Observacoes materiais

**Observacao 1 — label da linha "Vales" usa comprimento de lista de adiantamentos**
- **O que:** `ModalFechamento.tsx:95`: `listaAdiantamentosAtivos.length === 0 && totalVales === 0 ? '0' : '−'`. A condicional para determinar o sufixo da label "Vales (X)" avalia `listaAdiantamentosAtivos.length` — dado de adiantamentos — em vez de um count de vales. A intenção e mostrar "Vales (0)" quando nao ha vales e "Vales (−)" quando ha desconto de vales.
- **Onde:** `frontend/src/components/folha/ModalFechamento.tsx:95`
- **Por que importa:** Cenario que expoe o bug: funcionario sem adiantamentos ativos (`listaAdiantamentosAtivos.length === 0`) mas com vales no mes (`totalVales > 0`). A label exibiria `Vales (0)` mesmo havendo R$ de vales a descontar. O valor numerico a direita (`− R$ X,XX`) esta correto, mas a label conflita, potencialmente causando confusao ao usuario no momento mais critico da operacao (confirmacao de fechamento).
- **Sugestao:** Corrigir condicional para `totalVales === 0 ? '0' : '−'`. Correcao de 1 linha; abrir FIX imediato.

---

## 2. Gates verificados contra a realidade

| Gate | Status diz | Reviewer reproduziu | Divergencia? |
|---|---|---|---|
| build | ok | ok (`npm run build` saiu com codigo 0, sem erros TS) | nao |
| lint | ok | ok (`npm run lint` sem erros) | nao |
| testes | ok (83 total, 5 novos) | ok (83 total, 15 arquivos de teste passando) | nao |
| branch_convencao | ok | ok (`feature/fe-017-modal-fechamento` — padrao correto) | nao |
| territorio | ok | ok (apenas `frontend/` e `docs/sprints/03-folha-pagamento/status/`) | nao |

Os erros `DOMException [AbortError]` no output dos testes sao do happy-dom durante cleanup de outros arquivos de teste (pre-existentes, nao relacionados a FE-017). Todos os 83 testes passam.

---

## 3. Roteiro de validacao manual (opcional)

Task tem UI visual com logica de calculo. Roteiro para o humano:

### Pre-condicoes

- [ ] Backend rodando com profile `dev`, banco com funcionario que tem: vales abertos no mes corrente, adiantamentos ativos
- [ ] Frontend rodando (`npm run dev`)
- [ ] Mes corrente ainda nao fechado para o funcionario de teste

### Casos

| # | Acao | Esperado | Resultado | Observacao |
|---|---|---|---|---|
| 2.1 | Clicar "Fechar mes YYYY-MM" | Modal abre com salario, total vales, total parcelas e valor final calculado | | |
| 2.2 | Digitar valor positivo no campo ajuste | Valor final atualiza imediatamente (sem reload) | | |
| 2.3 | Digitar valor negativo alto no ajuste (para forcar valor final < 0) | Badge vermelho de alerta aparece; botao confirmar permanece habilitado | | |
| 2.4 | Clicar "Confirmar fechamento" | Botao muda para "Fechando…" e fica desabilitado durante POST | | |
| 2.5 | POST bem-sucedido | Modal fecha; toast verde "Mes fechado — R$ X,XX" aparece por ~4s; botao "Fechar mes" some da tela | | |
| 2.6 | Tentar fechar mes ja fechado (via API direta para criar fechamento duplicado, depois recarregar) | Mensagem "Mes ja fechado. Recarregue a pagina." aparece no modal | | |
| 2.7 | Funcionario sem adiantamentos e com vales | Label "Vales (−)" exibida? (este caso expoe o bug da Observacao 1 — esperado ver "Vales (0)" por engano) | | |

---

## 4. Resultado consolidado

| Item | Resultado |
|---|---|
| Analise de codigo | aprovado com observacoes (1 bug de label UX no modal) |
| Gates contra a realidade | ok |
| Roteiro manual | pendente (humano preenche) |
| **Veredito final** | aprovado com observacoes — pode mergear para integration; abrir FIX para corrigir `ModalFechamento.tsx:95` antes do merge integration→develop |

---

## 5. Skills — feedback loop

Sem observacao de skills nesta task.

---

## 6. Para o planner (proximos passos)

- Abrir FIX para corrigir `ModalFechamento.tsx:95`: substituir `listaAdiantamentosAtivos.length === 0 && totalVales === 0` por `totalVales === 0`. Correcao de 1 linha.
- O bug nao bloqueia o fechamento funcional (o valor numerico esta correto), mas causa confusao de UX no cenario de "sem adiantamentos, com vales". Recomendo corrigir antes do PR integration→develop.
- Sem outros itens pendentes para o planner.

---

## 7. QA — Fluxos automatizados

Nao aplicavel: task sem fluxos QA definidos (`fluxos_qa: []` no plano).
