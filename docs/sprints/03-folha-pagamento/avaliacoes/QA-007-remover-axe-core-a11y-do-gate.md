---
task: QA-007
sprint: 03-folha-pagamento
data: 2026-06-03
avaliador: claude-reviewer
status_report: docs/sprints/03-folha-pagamento/status/QA-007-remover-axe-core-a11y-do-gate.md
veredito_codigo: aprovado
veredito_final: aprovado
observacoes_count: 1
roteiro_executado: false
gates_verificados_contra_realidade: ok
skills_eficazes: []
skills_gaps: []
---

# Avaliacao — QA-007 Remover axe-core/playwright — a11y fora do gate E2E

**Branch:** `feature/qa-007-remover-axe-core-a11y-do-gate`
**Implementador:** claude-front
**Plano:** `docs/sprints/03-folha-pagamento/plans/QA-007-remover-axe-core-a11y-do-gate.md`
**Status report:** `docs/sprints/03-folha-pagamento/status/QA-007-remover-axe-core-a11y-do-gate.md`

---

## 1. Analise de codigo (Reviewer le o diff)

### Veredito de codigo: aprovado

A entrega e cirurgica e correta. O diff real (PR #91, commit `b584825`) altera exatamente 5 arquivos: `frontend/package.json`, `frontend/package-lock.json`, `frontend/e2e/specs/a11y-home.spec.ts` (deletado), `frontend/e2e/specs/site-fluxo-feliz.spec.ts` (AxeBuilder removido) e o status report em `docs/`. Zero vazamento de territorio.

Verificacoes realizadas diretamente no diff:

- `@axe-core/playwright` ausente em `frontend/package.json` (devDependencies): confirmado. Grep em todos os `.ts`/`.tsx` do frontend: zero referencias a `axe-core`, `AxeBuilder` ou `checkA11y` encontradas.
- `frontend/e2e/specs/a11y-home.spec.ts`: arquivo deletado (`deleted file mode` no diff). Confirmado via `ls frontend/e2e/specs/` — apenas `site-fluxo-feliz.spec.ts` e `webhook-cenarios.spec.ts` presentes.
- `frontend/e2e/specs/site-fluxo-feliz.spec.ts`: `import AxeBuilder` removido, bloco a11y (steps 5) removido, numeracao dos steps ajustada (6→5, 7→6), renomeacao do teste de `'fluxo feliz: login → home → comprovante (com a11y)'` para `'fluxo feliz: login → home → comprovante'`. Fluxo principal (login → home → pedidos → modal) intacto e correto.
- `frontend/e2e/specs/webhook-cenarios.spec.ts`: sem alteracao nesta task — confirmado. Zero referencias a axe-core neste arquivo.
- `package-lock.json`: entradas de `@axe-core/playwright` e `axe-core` removidas pelo `npm uninstall`. Confirmado via `git show b584825 -- frontend/package-lock.json | grep axe-core`.

O escopo foi expandido corretamente: o plano original assumia que QA-004 entregaria uma versao sem a11y, mas QA-004 foi implementado com `a11y-home.spec.ts` e `AxeBuilder` inline. A expansao foi autorizada pelo humano e documentada como Desvio 1 no status report — relato factual e completo.

### Observacoes materiais

**Observacao 1 — Hash do commit no status report nao coincide com o hash do PR**
- **O que:** O frontmatter do status report declara `commits: [9d4f111]`. O commit publicado no PR #91 e `b584825`. Os dois hashes existem no repositorio com conteudo identico (diferenca entre si: apenas o campo `commits: pendente` vs `commits: [9d4f111]` dentro do proprio status report — o commit `b584825` e o re-commit que incluiu o hash). Nao e divergencia de conteudo; e a situacao classica de auto-referencia de hash.
- **Onde:** `docs/sprints/03-folha-pagamento/status/QA-007-remover-axe-core-a11y-do-gate.md`, linha `commits: - 9d4f111`.
- **Por que importa:** O hash `9d4f111` existe no repositorio e aponta para o conteudo correto, mas nao e o HEAD da branch nem o commit do PR. Um script que parseia o frontmatter e tenta dar `git show 9d4f111` encontra o commit certo; um que tenta verificar se `9d4f111` e o HEAD da feature branch (`b584825`) encontraria divergencia. Cosmético, nao bloqueante.
- **Sugestao:** Aceitar como esta — a auto-referencia de hash e uma limitacao conhecida do fluxo (o implementador nao sabe o hash antes de commitar). O conteudo e correto. Se o projeto quiser eliminar isso no futuro, o planner pode padronizar `commits: [pendente]` como valor valido no schema e resolvê-lo so na revisao.

---

## 2. Gates verificados contra a realidade

Reviewer rodou os gates localmente na branch `feature/qa-007-remover-axe-core-a11y-do-gate`.

| Gate | Status diz | Reviewer reproduziu | Divergencia? |
|---|---|---|---|
| build | ok | ok (`npm run build` exit 0, `built in 3.75s`, PWA gerado, zero erros TS) | nao |
| lint | ok | ok (`npm run lint` exit 0, zero erros ESLint) | nao |
| testes | ok (61 total, 0 novos) | ok (61/61 Vitest passaram em 3.91s; 12 arquivos de teste; zero regressao) | nao |
| branch_convencao | ok | ok: nome `feature/qa-007-remover-axe-core-a11y-do-gate` bate o padrao `^feature/qa-\d{3}-`; branch saiu de `integration/03-folha-pagamento` (PR #91 confirma base) | nao |
| territorio | ok | ok: diff `integration/03-folha-pagamento...feature/qa-007-remover-axe-core-a11y-do-gate` toca apenas `frontend/` e `docs/sprints/03-folha-pagamento/status/` — zero `financas_bot_telegram/`, zero `frontend/src/` | nao |
| playwright-report/ nao commitado | ok | ok: `git ls-files frontend/playwright-report` vazio; `.gitignore` cobre o diretorio | nao |
| 1 commit de feature | ok | ok: PR #91 tem exatamente 1 commit (`b584825`), confirmado via `gh pr view 91 --json commits` | nao |
| testes_novos = 0 | ok | ok: nenhum arquivo `.spec.ts` adicionado; so remocoes | nao |

**`exige_e2e_full: false`** declarado no plano: confirmado. A task remove infra de teste, nao adiciona cenarios — rodar `e2e:full` seria irrelevante para validar a remocao. Gate nao aplicavel.

**Nota sobre a verificacao de ancestry de branch:** o `git merge-base --is-ancestor integration/03-folha-pagamento feature/qa-007-remover-axe-core-a11y-do-gate` retornou `NOT ancestor`. Investigado: a feature branch foi criada a partir da integration, mas a integration recebeu merges de develop depois — ha `multiple merge bases`. O PR #91 usa `baseRefName: integration/03-folha-pagamento` e o unico commit proprio e `b584825`. O ancestry e correto no sentido pratico (feature saiu da integration); a mensagem de `multiple merge bases` e ruido do historico de syncs, nao indica violacao de fluxo.

---

## 3. Roteiro de validacao manual

Nao aplicavel. A task e uma remocao de dependencia e limpeza de specs — sem UI, sem comportamento novo, sem integracao com servico externo. O resultado verificavel e: ausencia de `@axe-core/playwright` no `package.json` + `npm test` verde + `npm run build` verde. Todos os tres verificados pelo Reviewer diretamente.

---

## 4. Resultado consolidado

| Item | Resultado |
|---|---|
| Analise de codigo | aprovado — remocao cirurgica, zero residuos, fluxo principal preservado |
| Gates contra a realidade | ok — todos reproduzidos e confirmados |
| Roteiro manual | nao aplicavel — tarefa de remocao sem comportamento novo |
| **Veredito final** | **APROVADO — mergear** |

---

## 5. Skills — feedback loop

Sem observacao de skills nesta task. Escopo minimo (npm uninstall + limpeza de imports); nenhuma camada hexagonal envolvida. `skills_eficazes: []`, `skills_gaps: []`.

---

## 6. Para o planner (proximos passos)

- **Merge pode ser feito imediatamente.** Zero bloqueios tecnicos ou de processo.
- **Suíte E2E apos QA-007:** 2 specs ativas, 3 testes Playwright (1 fluxo feliz + 2 webhook). Atualizar STATE.md com a contagem correta.
- **Se a11y for reintroduzida em sprint futura:** reinstalar `@axe-core/playwright` e criar nova spec. O padrao de uso (`AxeBuilder.analyze()` com filtro manual em `serious + critical`) esta documentado em `docs/architecture/desenho-testes-automatizados.md` §8.3.
- **Sem acoes decorrentes desta task:** a Observacao 1 (hash auto-referencial) e cosmetica e nao exige FIX nem pendencia tecnica.
