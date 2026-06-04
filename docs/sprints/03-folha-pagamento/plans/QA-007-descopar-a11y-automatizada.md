---
task: QA-007
titulo: "Descopar a11y automatizada da suíte E2E (axe-core)"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-03
branch_alvo: feature/qa-007-descopar-a11y-automatizada
integration_branch: null
prioridade: media
esforco: baixo
territorio: front
estado: rascunho
depende_de: [QA-004]
bloqueia: []
skills_dispatched: []
fluxos_qa: []
---

# QA-007 — Descopar a11y automatizada da suíte E2E (axe-core)

> **Status: rascunho.** Aguarda (1) homologação da ADR 0018 pelo humano e (2) decisão do planner sobre **em qual sprint** entra (sprint 03 está em fechamento — provável que vire item da sprint 04 ou follow-up posterior). Quando ambas as condições forem atendidas, planner promove pra `pronto-pra-execucao`, ajusta `sprint:` e `integration_branch:` no frontmatter, e abre o dispatch.

---

## Intake

- **Origem:** ADR 0018 (proposta) — descopa a11y automatizada enquanto audiência do produto for fechada (humano + Pedro). Detectado durante a primeira execução real da suíte E2E em 2026-06-03, quando `site-fluxo-feliz.spec.ts` falhou com violação `serious` legítima de contraste no botão "Ver comprovante" (`bg-emerald-600 text-white`, ratio 3.76:1 vs mínimo WCAG AA 4.5:1).
- **Por quê agora:** quanto mais código gerado assumindo o gate ativo, maior o custo de remover. Remover na mesma sprint da entrega evita acúmulo.
- **Esforço:** baixo (~30-45 min). Mudanças mecânicas em 3 arquivos de código, 1 doc de arquitetura, 1 lock file. Zero lógica de produto afetada.
- **Riscos resumidos:** zero risco funcional (não muda comportamento user-facing). Único cuidado é **não remover `aria-label`/`role`** dos componentes — eles são seletores das specs E2E remanescentes.

---

## Contexto

A suíte E2E entregue na sprint 03 inclui dois pontos de verificação automatizada de a11y via `@axe-core/playwright`:

1. `frontend/e2e/specs/a11y-home.spec.ts` — varre `/` e `/erro` autenticadas, threshold `serious + critical`.
2. `frontend/e2e/specs/site-fluxo-feliz.spec.ts` — bloco inline com `new AxeBuilder({ page }).analyze()` + filtro de `serious|critical`, após seed de pedidos na home.

Ambos foram desenhados conforme **Decisão 11** do `docs/architecture/desenho-testes-automatizados.md`. Essa decisão é revogada pela **ADR 0018** (esta task é a materialização da ADR).

Convenções de marcação semântica que **permanecem** porque são seletores das specs E2E remanescentes:

- `aria-label="Ver comprovante de ${pedido.descricao}"` em `PedidoCard.tsx:38` — usado por `getByRole('button', { name: /ver comprovante de.../ })` em `site-fluxo-feliz.spec.ts:68`.
- `role="dialog"` + `aria-modal="true"` em `ModalArquivo.tsx` — usado por `getByRole('dialog')` em `site-fluxo-feliz.spec.ts:71`.
- Demais `aria-label` em botões usados pelas specs do MVP.

---

## Decisão / abordagem

Mudança mecânica em três níveis: código (specs + deps), documentação (desenho de testes) e formalização (ADR 0018 já preexistente).

1. **Deletar** `frontend/e2e/specs/a11y-home.spec.ts` por completo.
2. **Editar** `frontend/e2e/specs/site-fluxo-feliz.spec.ts`:
   - Remover `import AxeBuilder from '@axe-core/playwright';` (linha 12).
   - Remover o bloco "5. a11y: zero violações..." (linhas 56-64 — comentário, `analyze()`, filter, expect).
   - Renomear o teste de `'fluxo feliz: login → home → comprovante (com a11y)'` pra `'fluxo feliz: login → home → comprovante'` (linha 42).
   - Reordenar comentários de passos (item 5 vira o que era 6, etc.).
3. **Remover** `@axe-core/playwright` dos `devDependencies` de `frontend/package.json` e rodar `npm install` pra atualizar `package-lock.json`.
4. **Atualizar** `docs/architecture/desenho-testes-automatizados.md`:
   - Marcar **Decisão 11** com prefixo `~~Revogada por ADR 0018~~` (ou nota em destaque, conforme estilo do documento).
   - Atualizar tabela MVP de specs (se houver): remover `a11y-home.spec.ts`; remover sufixo "com a11y" de `site-fluxo-feliz`.
   - Buscar qualquer outra menção a `@axe-core/playwright`, `axe`, "a11y automatizada" no documento e atualizar/remover.
5. **Não tocar** em `PedidoCard.tsx`, `ModalArquivo.tsx` ou qualquer componente de produção. O bug específico do contraste (Sec. 3 da ADR 0018) é tratado em FIX separado, se for tratado.

---

## Escopo / arquivos

### Remover
- `frontend/e2e/specs/a11y-home.spec.ts` — descopo total.

### Modificar
- `frontend/e2e/specs/site-fluxo-feliz.spec.ts` — remover import, remover bloco a11y (~9 linhas), renomear teste.
- `frontend/package.json` — remover `@axe-core/playwright` de `devDependencies`.
- `frontend/package-lock.json` — regenerado por `npm install` após editar `package.json` (não editar à mão).
- `docs/architecture/desenho-testes-automatizados.md` — revogar Decisão 11, atualizar tabela MVP, varrer menções restantes.

### Não tocar
- `frontend/src/` — zero código de produção nesta task. O fix do botão verde é assunto separado.
- `frontend/e2e/specs/webhook-cenarios.spec.ts` — não usa a11y, fica intacto.
- `frontend/e2e/fixtures/*` — não usa a11y, fica intacto.
- `frontend/playwright.config.ts` — config não menciona axe, fica intacto.
- Demais ADRs — esta task **não** mexe na ADR 0018 (preexistente) nem em outras.

---

## Testes

**A própria task é uma redução da suíte de teste**, não adição. Não há `testes_novos` no sentido tradicional.

Validação obrigatória:

- `npm run e2e:full` (ou equivalente com stack já no ar: `npm run e2e`) verde **3 vezes consecutivas** com a suíte reduzida.
- A suíte reduzida tem **2 specs ativas** (`site-fluxo-feliz` e `webhook-cenarios`) com **3 `test()` total** (1 do fluxo feliz + 2 do webhook).
- `npm test` (Vitest) continua verde — não deve ter regressão em testes de unit.
- `npm run build` (TypeScript + Vite) sem erros — confirma que remoção de import não deixou referência pendente.

`testes_total` esperado pós-task: 3 (era 5 antes — perda intencional de 2 do `a11y-home` + remoção de uma asserção inline em `site-fluxo-feliz`).

---

## Critérios de aceitação

- [ ] `frontend/e2e/specs/a11y-home.spec.ts` não existe mais (`git status` confirma deleção).
- [ ] `frontend/e2e/specs/site-fluxo-feliz.spec.ts` sem qualquer referência a `AxeBuilder`, `axe`, `@axe-core` ou `a11y` no código.
- [ ] Teste do fluxo feliz renomeado para `'fluxo feliz: login → home → comprovante'` (sem "com a11y").
- [ ] `frontend/package.json` sem `@axe-core/playwright` em `devDependencies`.
- [ ] `frontend/package-lock.json` regenerado por `npm install` (refletindo a remoção).
- [ ] `grep -ri "axe" frontend/e2e/` retorna **zero** matches.
- [ ] `grep -ri "axe-core" frontend/` retorna **zero** matches fora de `node_modules/` e `package-lock.json` (esse ainda pode mencionar `axe-core` como dep transitiva de outras libs — verificar caso a caso).
- [ ] `docs/architecture/desenho-testes-automatizados.md` Decisão 11 marcada como revogada pela ADR 0018.
- [ ] `docs/architecture/desenho-testes-automatizados.md` tabela MVP atualizada (sem `a11y-home`, sem sufixo "com a11y").
- [ ] `npm run e2e` verde com 2 specs e 3 testes (1 fluxo feliz + 2 webhook).
- [ ] `npm run e2e` verde **3 vezes consecutivas** sem flakiness.
- [ ] `npm test` (Vitest) verde — confirma que nada de unit quebrou.
- [ ] `npm run build` sem erros TypeScript.
- [ ] `PedidoCard.tsx` e `ModalArquivo.tsx` **inalterados** (`git diff` confirma — esta task não muda código de produção).
- [ ] `aria-label`, `role="dialog"`, `aria-modal="true"` continuam presentes nos componentes (`grep` confirma).
- [ ] Branch: `feature/qa-007-descopar-a11y-automatizada` saindo da branch correta (definida pelo planner ao promover esta task pra `pronto-pra-execucao`).
- [ ] Status report `docs/sprints/<sprint>/status/QA-007-descopar-a11y-automatizada.md` com frontmatter válido. `testes_novos: 0`, `testes_removidos: 2` (campo opcional — se o template suportar; senão deixar no corpo).
- [ ] ADR 0018 referenciada na seção "Referências" do status.

---

## Fora de escopo (explicitamente)

- **Fix do contraste do botão verde.** A violação detectada (`bg-emerald-600 text-white` em `PedidoCard.tsx:36` e `ModalArquivo.tsx:111`) **não** é resolvida aqui. Decisão de produto: abrir `FIX-NNN` separado se quiser arrumar como melhoria de UX, ou deixar como está. ADR 0018 §3 explica.
- **Remoção de `aria-label` / `role`.** São seletores das specs remanescentes — removê-los quebra o teste do fluxo feliz. Mantém.
- **Reinstalar axe no futuro.** Se o trigger de revisão da ADR 0018 §5 disparar, vira nova ADR + nova task QA-NNN. Não desta.
- **Outros tipos de teste descopados (security scan, carga).** Continuam fora de escopo, não são revisitados aqui.
- **Atualização de skills/runbooks que mencionem a11y.** Se existirem, viram tarefa de planner separada (ex: `docs/runbooks/ROTEIRO-E2E.md`). Esta task se limita ao código + `docs/architecture/desenho-testes-automatizados.md`.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Remover `import AxeBuilder` deixar referência órfã em outro lugar | Baixa | Baixo | `grep -ri "AxeBuilder\|axe-core" frontend/` antes de commitar; CI roda `tsc` e pega |
| `npm install` regenerar `package-lock.json` com diff barulhento (mudanças não relacionadas) | Média | Baixo | Conferir diff do lock; se tiver muito ruído, fazer `npm ci` antes e depois pra comparar |
| Esquecer de atualizar `docs/architecture/desenho-testes-automatizados.md` | Média | Médio | Item explícito no critério de aceitação; reviewer audita |
| Remover acidentalmente `aria-label` "por limpeza" | Baixa | Alto | Critério de aceitação proíbe explicitamente; reviewer audita; teste do fluxo feliz quebra na execução se acontecer |
| Task ser confundida com "removendo a11y do produto" | Média | Baixo | Cabeçalho do plano + ADR 0018 deixam claro: marcação semântica fica, só o **gate automatizado** sai |

---

## Coordenação

- **Pode rodar em paralelo com:** qualquer task que não toque `frontend/e2e/`, `frontend/package.json` ou `docs/architecture/desenho-testes-automatizados.md`.
- **Depende sequencialmente de:** QA-004 (a infra que esta task reduz) + homologação da ADR 0018 pelo humano.
- **Bloqueia:** nada. Decisão tomada agora, execução pode ser depois.
- **Atenção pro Reviewer:** verificar que (a) `aria-label`/`role` ficaram intactos nos componentes; (b) `package-lock.json` não tem ruído estranho; (c) Decisão 11 do desenho realmente foi marcada como revogada e não só "removida silenciosamente" (preservar trilha histórica).
- **Após merge:** atualizar `STATE.md` da sprint (Decisão 11 revogada, suíte reduzida pra 2 specs/3 testes). Se o humano decidir fazer o FIX do botão verde, abrir `FIX-NNN` separado.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido conforme `_TEMPLATE-status.md`, revisão do Reviewer. PR `feature → <branch-base-definida-pelo-planner>`.

`fluxos_qa: []` — task de tooling pura, sem fluxo de produto a validar via QA externo.

---

## Referências

- ADR 0018 — A11y automatizada fora de escopo enquanto audience for fechada (`docs/decisions/0018-a11y-automatizada-fora-de-escopo.md`). Esta task é a materialização da ADR.
- ADR 0017 — Prefixo QA-NNN para tasks de tooling/infra de qualidade (`docs/decisions/0017-prefixo-qa-tasks-tooling-qualidade.md`). QA-007 é mais uma instância do prefixo.
- `docs/architecture/desenho-testes-automatizados.md` — documento alvo da edição (Decisão 11 + tabela MVP).
- `docs/sprints/03-folha-pagamento/plans/QA-004-specs-mvp-3-cenarios.md` — plano original que entregou os artefatos sendo removidos aqui.
- `frontend/e2e/specs/site-fluxo-feliz.spec.ts` — arquivo a modificar.
- `frontend/e2e/specs/a11y-home.spec.ts` — arquivo a deletar.
- `frontend/src/components/PedidoCard.tsx` e `frontend/src/components/ModalArquivo.tsx` — **NÃO** tocar nesta task; referenciados apenas como evidência das marcações semânticas que devem ser preservadas.
- `.claude/agent-memory/qa-test-specialist/project_a11y_fora_de_escopo.md` — memória do qa-test-specialist que reflete a mesma decisão.
