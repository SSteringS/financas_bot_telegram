---
task: QA-001
sprint: 03-folha-pagamento
data: 2026-06-01
avaliador: claude-reviewer
status_report: docs/sprints/03-folha-pagamento/status/QA-001-setup-playwright-base.md
veredito_codigo: aprovado_com_observacoes
veredito_final: aprovado_com_observacoes
observacoes_count: 3
roteiro_executado: false
gates_verificados_contra_realidade: divergente
skills_eficazes: []
skills_gaps: []
---

# Avaliacao — QA-001 Setup Playwright + config base

**Branch:** `feature/qa-001-setup-playwright-base`
**Implementador:** claude-front
**Plano:** `docs/sprints/03-folha-pagamento/plans/QA-001-setup-playwright-base.md`
**Status report:** `docs/sprints/03-folha-pagamento/status/QA-001-setup-playwright-base.md`

---

## 1. Analise de codigo (Reviewer le o diff)

A task e essencialmente de setup declarativo: instalar deps, criar arquivos de config, atualizar `.gitignore`. O diff esta limpo — zero mudancas em `frontend/src/`, zero vazamento para `financas_bot_telegram/`. A logica dos arquivos criados e minima e adequada para a funcao de infraestrutura que exercem.

O desvio documentado (stub `global-setup.ts` em vez de deixar o arquivo ausente) e tecnicamente correto: o Playwright de fato lanca `MODULE_NOT_FOUND` ao resolver `globalSetup` no carregamento do config, mesmo com `--list`. O stub e a solucao certa e a documentacao no status report e clara.

Foram encontradas tres observacoes materiais, descritas abaixo.

### Veredito de codigo: aprovado com observacoes

Implementacao correta e dentro do escopo. As observacoes sao de consistencia de nomenclatura e de antecipacao nao coordenada de escopo de tarefa futura — nenhuma impede o funcionamento do setup, mas uma delas vai gerar conflito de merge em QA-004.

### Observacoes materiais

**Observacao 1 — Nome da variavel de ambiente ADMIN diverge do plano e do ROTEIRO-E2E**

- **O que:** O plano (`QA-001-setup-playwright-base.md` linha 57) e o DISPATCH (`DISPATCH-QA-001` linha 46) especificam `E2E_ADMIN_SECRET`. A arquitetura (`desenho-testes-automatizados.md` linhas 451 e 556) usa `E2E_ADMIN_KEY`. O `ROTEIRO-E2E.md` (linha 42) usa `E2E_ADMIN_SECRET`. A implementacao adotou `E2E_ADMIN_KEY`, alinhada com a arquitetura mas conflitante com o plano e o runbook.
- **Onde:** `frontend/.env.e2e.example` linha 11; status report linha 57.
- **Por que importa:** Quem seguir o ROTEIRO-E2E para configurar o `.env.e2e` vai escrever `E2E_ADMIN_SECRET=...` — variavel que o codigo das fixtures (QA-003, baseado em `desenho-testes-automatizados.md`) vai ler como `E2E_ADMIN_KEY`. O teste silenciosamente usara `undefined` como valor do header de admin, causando 401/403 sem mensagem de erro obvio.
- **Sugestao:** Escolher um nome canonico e propagar: ou atualizar `ROTEIRO-E2E.md` linha 42 + o plano QA-001 para `E2E_ADMIN_KEY`, ou renomear o `.env.e2e.example` para `E2E_ADMIN_SECRET` e garantir que a implementacao de QA-003 leia `E2E_ADMIN_SECRET`. A decisao pertence ao planner — abrir observacao para resolucao antes de QA-003.

**Observacao 2 — `outputDir` e `outputFolder` do reporter HTML apontam para o mesmo diretorio**

- **O que:** `playwright.config.ts` tem `outputDir: './playwright-report'` (onde o Playwright grava traces, videos e screenshots de falha) e `reporter: [['html', { outputFolder: 'playwright-report' }]]` (onde o reporter HTML grava o relatorio). Sao dois propositos distintos misturados no mesmo diretorio.
- **Onde:** `frontend/playwright.config.ts` linhas 10-11.
- **Por que importa:** O `outputDir` padrao do Playwright e `./test-results` por razao semantica: e la que ficam os artifacts de debug (nao o relatorio). Misturar os dois no mesmo diretorio cria confusao ao inspecionar falhas (artifacts e HTML misturados). O `.gitignore` ja ignora `test-results/` separadamente — esse diretorio sera criado pelo Playwright de toda forma, ficando fora do `.gitignore` se `outputDir` for movido de volta para o padrao.
- **Sugestao:** Restaurar `outputDir` para o padrao do Playwright (`./test-results`, que ja esta no `.gitignore`) e manter `playwright-report/` apenas para o relatorio HTML. Alteracao de 1 linha em `playwright.config.ts`. Baixa urgencia — nao quebra nenhum teste — mas ideal corrigir antes de QA-004 para nao precisar refatorar novamente.

**Observacao 3 — Antecipacao de escopo de QA-004: `reporter` e `outputDir` incluidos em QA-001**

- **O que:** O plano de QA-004 (`QA-004-specs-mvp-3-cenarios.md` linha 70 e 82) e a spec de Fase 1 (`qa-suite-e2e-fase1.md` linha 243) dizem explicitamente que `reporter: [['html'], ['list']]` e `outputDir` serao **adicionados em QA-004** como modificacao do `playwright.config.ts` criado em QA-001. A implementacao de QA-001 ja os incluiu.
- **Onde:** `frontend/playwright.config.ts` linhas 10-11; `docs/sprints/03-folha-pagamento/plans/QA-004-specs-mvp-3-cenarios.md` linha 82.
- **Por que importa:** QA-004, ao tentar modificar `playwright.config.ts` conforme seu plano, encontrara as linhas ja presentes — pode gerar confusao para o implementador (o plano diz "adicionar" algo que ja existe) ou conflito de merge se QA-004 rodar em branch separada de QA-001. Nao e regressao tecnica, mas e drift entre plano e realidade que o planner precisa reconciliar.
- **Sugestao:** O planner deve atualizar o escopo de QA-004 para refletir que `reporter` e `outputDir` ja existem — QA-004 pode apenas validar/ajustar em vez de criar do zero. Acao de documentacao, nao de codigo.

---

## 2. Gates verificados contra a realidade

| Gate | Status diz | Reviewer reproduziu | Divergencia? |
|---|---|---|---|
| build | ok | ok (`tsc -b && vite build` — exit 0, sem erros TS) | nao |
| lint | ok | ok (`eslint .` — sem output de erros, exit 0) | nao |
| testes | ok (61 total, 0 novos) | ok (61 passed, 12 arquivos; ruido de DOMException e pre-existente no happy-dom, nao e regressao) | nao |
| branch_convencao | ok | ok (`feature/qa-001-setup-playwright-base`; `integration/03-folha-pagamento` e ancestral — exit 0) | nao |
| territorio | ok | ok (apenas `frontend/` + `docs/sprints/03-folha-pagamento/status/`) | nao |
| playwright --list | ok | ok (exit 1 com "No tests found / Total: 0" — sem erro de importacao/config; comportamento documentado e esperado) | nao |
| npm run e2e -- --help | ok | ok (exit 0, mostra help do Playwright) | nao |
| .env.e2e no .gitignore | ok | ok (linha 33 do `.gitignore`) | nao |
| playwright-report/ no .gitignore | ok | ok (linha 34 do `.gitignore`) | nao |
| .env.e2e nunca commitado | ok | ok (nao aparece em nenhum diff da branch) | nao |

**Divergencia encontrada (Observacao 1 acima):** O status report declara `gates: territorio: ok` e `estado: concluido`. O territorio de codigo esta correto. Porem o status report nao declara a divergencia de nomenclatura `E2E_ADMIN_KEY` vs `E2E_ADMIN_SECRET` entre o `.env.e2e.example` entregue e o plano/runbook. Isso nao invalida o `estado: concluido` para esta task de setup — a variavel sera consumida apenas em QA-003 — mas e uma inconsistencia de contrato que deve ser resolvida antes que QA-003 seja despachada.

`gates_verificados_contra_realidade: divergente` pelo motivo acima (inconsistencia documental nao declarada como desvio).

---

## 3. Roteiro de validacao manual

Nao aplicavel: task de infraestrutura/setup. Nenhuma UI ou comportamento visual para validar manualmente. Os criterios de aceitacao sao todos verificaveis por CLI — verificados na secao 2.

---

## 4. Resultado consolidado

| Item | Resultado |
|---|---|
| Analise de codigo | aprovado com observacoes |
| Gates contra a realidade | divergente (inconsistencia documental de nomenclatura — nao e regressao funcional) |
| Roteiro manual | n/a |
| **Veredito final** | ajustar antes de mergear (Obs. 1 deve ser resolvida; Obs. 2 e 3 podem ser tratadas em QA-004 ou como FIX separado) |

A Observacao 1 (nome da variavel ADMIN) e bloqueante para o merge porque cria uma armadilha silenciosa para QA-003: o runbook instrui `E2E_ADMIN_SECRET`, o codigo vai ler `E2E_ADMIN_KEY`, o teste falha com 401 sem mensagem clara. Isso deve ser resolvido com decisao de nomenclatura pelo planner e correcao em pelo menos um dos documentos (`ROTEIRO-E2E.md` ou `.env.e2e.example`) antes do merge.

As Observacoes 2 e 3 sao de baixo risco imediato — podem ser tratadas como ajuste em QA-004 ou em FIX separado ap'os o merge, a criterio do planner.

---

## 5. Skills — feedback loop

Sem observacao de skills nesta task. Task de configuracao declarativa sem camadas de arquitetura envolvidas.

`skills_eficazes: []`, `skills_gaps: []`

---

## 6. Para o planner (proximos passos)

1. **Decisao de nomenclatura (bloqueante para QA-003):** escolher entre `E2E_ADMIN_KEY` (adotado em `desenho-testes-automatizados.md` e na implementacao) e `E2E_ADMIN_SECRET` (usado em `ROTEIRO-E2E.md` e nos planos QA-001/DISPATCH). Propagar o nome escolhido para todos os pontos — pelo menos `ROTEIRO-E2E.md` linha 42, plano QA-001 linha 57, DISPATCH linha 46, e garantir que QA-003 use o mesmo nome ao implementar as fixtures.

2. **Atualizar escopo de QA-004:** o plano de QA-004 deve registrar que `reporter` e `outputDir` ja foram adicionados em QA-001 — a task de QA-004 nao precisa criar essas linhas, apenas verificar/ajustar se necessario.

3. **Observacao 2 (`outputDir` vs `outputFolder`):** baixa prioridade, mas registrar como pendencia tecnica ou incluir no escopo de QA-004 corrigir `outputDir` para `./test-results` (padrao semantico do Playwright).

4. **Ordem de despacho:** QA-002 pode ser despachado em paralelo (nao depende desta correcao). QA-003 deve aguardar a resolucao da Observacao 1 alem de BE-023.

---

> **Ação do planner (2026-06-03):**
> - **Obs 1 resolvida:** decisão de nomenclatura tomada — **`E2E_ADMIN_KEY` é o nome canônico** (alinhado com `desenho-testes-automatizados.md` e com a implementação de QA-001). Propagado para: `ROTEIRO-E2E.md` linha 42, `QA-001-setup-playwright-base.md` linha 57, `DISPATCH-QA-001` linha 46.
> - **Obs 2 registrada:** `outputDir`/`outputFolder` no mesmo diretório → `docs/PENDENCIAS-TECNICAS.md`. QA-004 tem nota para verificar/corrigir.
> - **Obs 3 resolvida:** plano `QA-004-specs-mvp-3-cenarios.md` atualizado — `reporter` e `outputDir` já existem, task não precisa criá-los.
> - Veredito final atualizado para `aprovado_com_observacoes`.
