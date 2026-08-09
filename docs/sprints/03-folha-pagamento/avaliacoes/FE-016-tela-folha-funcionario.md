---
task: FE-016
sprint: 03-folha-pagamento
data: 2026-06-04
avaliador: claude-reviewer
status_report: docs/sprints/03-folha-pagamento/status/FE-016-tela-folha-funcionario.md
veredito_codigo: aprovado_com_observacoes
veredito_final: aprovado_com_observacoes
observacoes_count: 2
roteiro_executado: false
gates_verificados_contra_realidade: ok
skills_eficazes: []
skills_gaps: []
veredito_qa: nao_aplicavel
fluxos_qa_executados: []
fluxos_qa_adicionar: []
fluxos_qa_remover: []
---

# Avaliacao — FE-016 (Tela Folha do Funcionario — vales, adiantamentos, fechamentos)

**Branch:** `feature/fe-016-tela-folha-funcionario`
**Implementador:** claude-front
**Plano:** `docs/sprints/03-folha-pagamento/plans/FE-016-tela-folha-funcionario.md`
**Status report:** `docs/sprints/03-folha-pagamento/status/FE-016-tela-folha-funcionario.md`

---

## 1. Analise de codigo (Reviewer le o diff)

### Veredito de codigo: aprovado com observacoes

A implementacao e solida. Arquitetura props-down esta correta: todas as queries ficam no hook agregador `useFolhaFuncionario`, as secoes sao pure components testáveis sem mock de queries. O endpoint DELETE foi verificado contra o backend e bate: `FolhaController.java` declara `@RequestMapping("/api/funcionarios")` + `@DeleteMapping("/adiantamentos/{adiantamentoId}")` = `/api/funcionarios/adiantamentos/{adiantamentoId}` — exatamente o que `cancelarAdiantamento()` em `folha.ts` envia. A logica `mesFechado` via `startsWith("YYYY-MM")` e correta para o formato `"YYYY-MM-DD"` que o backend retorna. O `totalValesAbertos` na pagina filtra corretamente `!v.fechado` antes de somar. Nenhum uso de `as` (type assertion) nos arquivos novos desta task.

Dois pontos materiais identificados: um bug de label no ModalFechamento (stub) e um edge case nao coberto no display de parcelas.

### Observacoes materiais

**Observacao 1 — label de vales no ModalFechamento (stub) usa campo errado**
- **O que:** Linha 95 do stub `ModalFechamento.tsx` (criado em FE-016): `listaAdiantamentosAtivos.length === 0 && totalVales === 0 ? '0' : '−'`. A condicional usa `listaAdiantamentosAtivos.length` para determinar a label da linha de **vales**. O correto seria `totalVales === 0 ? '0' : '−'`.
- **Onde:** `frontend/src/components/folha/ModalFechamento.tsx:95` (este arquivo e substituido em FE-017, mas o bug permaneceu na implementacao final).
- **Por que importa:** Cenario que expoe o bug: funcionario sem adiantamentos ativos mas com vales do mes — `listaAdiantamentosAtivos.length === 0` e `totalVales > 0`. A label exibe `Vales (0)` em vez de `Vales (−)`, sugerindo ao usuario que nao ha vales sendo descontados, quando ha. Bug de UX que pode confundir o usuario no momento de confirmar o fechamento.
- **Sugestao:** Corrigir a condicional para `totalVales === 0 ? '0' : '−'` em `ModalFechamento.tsx:95`. Este arquivo e de FE-017, portanto abrir FIX curto ou incluir na mesma branch.

**Observacao 2 — parcela X/N quando parcelasPagas === numParcelas**
- **O que:** `AdiantamentosSection.tsx:91` exibe `{adt.parcelasPagas + 1}/{adt.numParcelas}`. Se o backend retornar um adiantamento com `ativo=true` e `parcelasPagas === numParcelas` (estado inconsistente ou race), a UI exibiria `4/3` (para 3 parcelas).
- **Onde:** `frontend/src/components/folha/AdiantamentosSection.tsx:91`
- **Por que importa:** Nao e um bug esperado no fluxo normal (backend faz soft-delete ao pagar a ultima parcela). Mas a UI nao tem guard contra o valor exibido ser maior que o denominador.
- **Sugestao:** Registrar como pendencia tecnica de baixa prioridade. A alternativa simples seria `Math.min(adt.parcelasPagas + 1, adt.numParcelas)` — mas dado que e corner case de estado inconsistente de backend, nao bloqueante.

---

## 2. Gates verificados contra a realidade

| Gate | Status diz | Reviewer reproduziu | Divergencia? |
|---|---|---|---|
| build | ok | ok (`npm run build` saiu com codigo 0, 306 kB JS) | nao |
| lint | ok | ok (`npm run lint` sem erros ou warnings) | nao |
| testes | ok (78 total, 8 novos) | ok (83 total — FE-017 ja integrado; 8 testes desta task confirmados em FolhaFuncionario.test.tsx) | nao |
| branch_convencao | ok | ok (`feature/fe-016-tela-folha-funcionario` — padrao correto) | nao |
| territorio | ok | ok (todos os arquivos em `frontend/` ou `docs/sprints/`) | nao |

Nota sobre `testes_total=78`: o status report foi escrito antes do merge de FE-017. O total atual e 83 (inclui 5 testes de FE-017). Nao e divergencia — e sequencia esperada de merges.

---

## 3. Roteiro de validacao manual (opcional)

Task tem UI visual. Roteiro para o humano:

### Pre-condicoes

- [ ] Backend rodando com profile `dev`, banco com pelo menos 1 funcionario cadastrado
- [ ] Frontend rodando (`npm run dev`)
- [ ] Funcionario de teste com: salario base preenchido, `contaPropria=false` (para testar badge), pelo menos 1 vale e 1 adiantamento cadastrados via API direta

### Casos

| # | Acao | Esperado | Resultado | Observacao |
|---|---|---|---|---|
| 1.1 | Navegar para `/folha/funcionarios/{id}` | Header com nome + salario base visivel | | |
| 1.2 | Funcionario com `contaPropria=false` | Badge "Conta de terceiro" laranja visivel ao lado do nome | | |
| 1.3 | Selecionar mes diferente no dropdown | Lista de vales atualiza para o mes selecionado | | |
| 1.4 | Vale com `fechado=true` | Exibido em cinza com texto tachado | | |
| 1.5 | Clicar "+ Registrar vale" | Formulario inline aparece; cancelar fecha | | |
| 1.6 | Adiantamento ativo | Exibe "Parcela X/N · R$ valor_parcela/mes" | | |
| 1.7 | Clicar "Cancelar" num adiantamento e confirmar | Adiantamento some da lista apos sucesso | | |
| 1.8 | Clicar num fechamento anterior | Accordion expande mostrando observacao | | |
| 1.9 | Mes nao fechado | Botao "Fechar mes YYYY-MM" visivel | | |
| 1.10 | Mes ja fechado | Botao "Fechar mes" NAO aparece | | |

---

## 4. Resultado consolidado

| Item | Resultado |
|---|---|
| Analise de codigo | aprovado com observacoes (2 observacoes: 1 bug de label UX, 1 edge case menor) |
| Gates contra a realidade | ok |
| Roteiro manual | pendente (humano preenche) |
| **Veredito final** | aprovado com observacoes — pode mergear; Observacao 1 (label de vales) deve ser corrigida antes ou em FIX imediato |

---

## 5. Skills — feedback loop

Sem observacao de skills nesta task.

---

## 6. Para o planner (proximos passos)

- Observacao 1 (bug de label `listaAdiantamentosAtivos.length` na linha de vales do ModalFechamento) esta no arquivo `ModalFechamento.tsx` que pertence ao escopo de FE-017. Verificar se FE-017 corrigiu esse ponto (o arquivo foi reescrito).
- Se FE-017 nao corrigiu: abrir FIX para corrigir condicional em `ModalFechamento.tsx:95`.
- Observacao 2: registrar em `PENDENCIAS-TECNICAS.md` como baixa prioridade se desejado.

---

## 7. QA — Fluxos automatizados

Nao aplicavel: task sem fluxos QA definidos (`fluxos_qa: []` no plano).
