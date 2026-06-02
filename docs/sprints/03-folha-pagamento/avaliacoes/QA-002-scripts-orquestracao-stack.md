---
task: QA-002
sprint: 03-folha-pagamento
data: 2026-06-01
avaliador: claude-reviewer
status_report: docs/sprints/03-folha-pagamento/status/QA-002-scripts-orquestracao-stack.md
veredito_codigo: aprovado_com_observacoes
veredito_final: aprovado_com_observacoes
observacoes_count: 3
roteiro_executado: true
gates_verificados_contra_realidade: ok
skills_eficazes: []
skills_gaps: []
---

# Avaliacao — QA-002 Scripts de orquestracao da stack

**Branch:** `feature/qa-002-scripts-orquestracao-stack`
**Implementador:** claude-front
**Plano:** `docs/sprints/03-folha-pagamento/plans/QA-002-scripts-orquestracao-stack.md`
**Status report:** `docs/sprints/03-folha-pagamento/status/QA-002-scripts-orquestracao-stack.md`

---

## 1. Analise de codigo (Reviewer le o diff)

### Veredito de codigo: aprovado com observacoes

Os tres scripts entregam o comportamento especificado: polling com backoff exponencial, TCP check MySQL com mensagem canonica, spawn cross-platform, SIGTERM+SIGKILL no Unix e taskkill /F /T no Windows, exit 0 gracioso sem `.e2e-pids`. A logica de fluxo esta correta e robusta para o caso de uso previsto. O `eslint.config.js` foi estendido corretamente para evitar falsos positivos nos scripts Node.js sem contaminar o lint de `src/`.

Tres observacoes materiais identificadas abaixo: uma inconsistencia de documentacao no status report, um hash de commit desatualizado por amend, e a ausencia de verificacao estatica TypeScript dos scripts e2e pelo ciclo de build padrao.

### Observacoes materiais

**Observacao 1 — Status report descreve tratamento de shell diferente do que foi implementado**
- **O que:** O bloco "Decisoes tomadas durante a execucao" do status report afirma: "O `vite.cmd` recebe tratamento diferente: usado o path completo via `node_modules/.bin/vite.cmd` sem `shell`, para evitar ambiguidade de PATH." O codigo real usa a funcao `spawnBackground` para ambos os spawns (back e front), e essa funcao tem `shell: isWin` — ou seja, o vite.cmd e executado com shell no Windows, nao sem shell.
- **Onde:** `frontend/e2e/scripts/subir-stack.ts:73-82` (funcao `spawnBackground`) vs `docs/sprints/03-folha-pagamento/status/QA-002-scripts-orquestracao-stack.md` secao "Decisoes tomadas durante a execucao".
- **Por que importa:** O comportamento real funciona corretamente (path absoluto + shell=true no Windows e valido), mas o status report documenta uma distincao que nao existe na implementacao. Quem herdar o codigo pode ser confundido sobre a intencao do tratamento diferenciado do vite.cmd.
- **Sugestao:** Corrigir o status report para refletir a implementacao real: ambos os spawns usam `shell: isWin`. A justificativa para o path absoluto do vite (evitar ambiguidade de PATH) continua valida mesmo com `shell: true` — o comentario pode ser preservado com essa correcao.

**Observacao 2 — Hash de commit no status report esta defasado por amend**
- **O que:** O campo `commits: [a2e331a]` no frontmatter do status report referencia o commit original antes do amend. O commit atual na branch e `47d123d`. O amend foi feito justamente para atualizar o hash — mas ao fazer o amend, o hash mudou novamente, deixando o status report com o hash pré-amend.
- **Onde:** `docs/sprints/03-folha-pagamento/status/QA-002-scripts-orquestracao-stack.md:17` — `commits: [a2e331a]` deveria ser `commits: [47d123d]`.
- **Por que importa:** E uma inconsistencia cosmética mas pode confundir scripts de auditoria que cruzam hash de commit com o estado real da branch. O commit `a2e331a` existe no reflog (commit pre-amend) mas nao esta em nenhuma branch rastreada.
- **Sugestao:** Atualizar o campo `commits` para `[47d123d]`. Nao requer novo amend — pode ser corrigido num commit separado antes do merge, ou aceito como pendencia cosmética (a propria restricao de self-reference torna isso recorrente).

**Observacao 3 — Scripts e2e ficam fora da verificacao estatica TypeScript do build padrao**
- **O que:** O `e2e/tsconfig.json` existe mas nao esta referenciado em `tsconfig.json` (que so referencia `tsconfig.app.json` e `tsconfig.node.json`). Ao rodar `tsc -p e2e/tsconfig.json`, o compilador falha com TS5097 porque `e2e/tsconfig.json` desabilita `allowImportingTsExtensions` mas `subir-stack.ts` importa com extensao `.ts`. O `tsx` (que usa esbuild internamente) executa os scripts corretamente em runtime — mas os scripts ficam sem type-checking estatico no ciclo de build.
- **Onde:** `frontend/e2e/tsconfig.json` (nao referenciado em `tsconfig.json`); `subir-stack.ts:20` (`import ... from './aguardar-saude.ts'`).
- **Por que importa:** Erros de tipo nos scripts e2e so seriam capturados em runtime (quando o script falha durante execucao de testes), nao no `npm run build`. Para scripts de infraestrutura de QA, isso e aceitavel no curto prazo — mas se o `e2e/tsconfig.json` existe com a intencao de prover verificacao estatica, ele nao esta cumprindo esse papel.
- **Sugestao:** Duas opcoes: (a) remover `e2e/tsconfig.json` e aceitar que os scripts rodam via `tsx` sem type-check estatico (mais honesto); ou (b) corrigir `e2e/tsconfig.json` para herdar de `tsconfig.node.json` em vez de `tsconfig.app.json` (que tem `moduleResolution: bundler` com `allowImportingTsExtensions: true`) e adicionar a referencia em `tsconfig.json`. Sugestao (b) da mais segurança mas pode ser feita em QA-003 ou como FIX separado. Esta task nao e bloqueada por isso.

---

## 2. Gates verificados contra a realidade

| Gate | Status diz | Reviewer reproduziu | Divergencia? |
|---|---|---|---|
| build | ok | ok (`npm run build` exit 0, 1168 modulos, sem erro TS) | nao |
| lint | ok | ok (`npm run lint` exit 0, zero erros) | nao |
| testes | ok (61 total, 0 novos) | ok (61/61 passaram, 3.74s) | nao |
| branch_convencao | ok | ok com ressalva: prefixo `qa` nao esta no pattern do PRE-MERGE-CHECKLIST.md, mas esta homologado pelo ADR 0017; o CLAUDE.md e o checklist estao desatualizados (mudanca downstream pendente do ADR 0017 secao 5) | observacao de processo, nao falha da task |
| territorio | ok | ok: commit 47d123d toca apenas `frontend/`, `frontend/.gitignore`, `frontend/eslint.config.js` e `docs/sprints/03-folha-pagamento/status/` — zero vazamento para `financas_bot_telegram/` ou outros territorios | nao |

**Nota sobre branch_convencao:** o PRE-MERGE-CHECKLIST.md tem o pattern `^(feature/(be|fe|dep|evo|ci)-\d{3}[a-z]?-|...)` que nao inclui `qa`. O ADR 0017 (status: Accepted, 2026-06-01) homologou o prefixo `qa-NNN` e listou a atualizacao do CLAUDE.md e checklist como pendencia downstream. A task QA-002 segue o ADR 0017; o problema e que o checklist nao foi atualizado ainda. O gate nao deve bloquear esta task — deve bloquear a pendencia do planner.

---

## 3. Roteiro de validacao manual

### Pre-condicoes

- [x] `tsx` disponivel (instalado em QA-001)
- [x] MySQL rodando em localhost:3306 (ambiente local do Reviewer)
- [x] Node.js disponivel

### Casos

| # | Acao | Esperado | Resultado | Observacao |
|---|---|---|---|---|
| 1.1 | `npx tsx e2e/scripts/aguardar-saude.ts http://localhost:9999/health 2000` | exit 1, mensagem clara | exit 1 "Healthcheck falhou: http://localhost:9999/health nao respondeu em 2000ms" | ok |
| 1.2 | `npx tsx e2e/scripts/aguardar-saude.ts http://localhost:8080/actuator/health 10000` (back nao rodando) | exit 1, mensagem clara | exit 1 "Healthcheck falhou: http://localhost:8080/actuator/health nao respondeu em 10000ms" | ok |
| 2.1 | `npx tsx e2e/scripts/derrubar-stack.ts` sem `.e2e-pids` | exit 0, mensagem informativa | exit 0 "stack pode ja estar derrubada" | ok |
| 2.2 | MySQL parado em 3306: `npx tsx e2e/scripts/subir-stack.ts` | exit 1 em <5s com mensagem canonica | Nao executavel pelo Reviewer — MySQL esta rodando localmente. Leitura de codigo confirma: `socket.on('error')` retorna a mensagem exata do criterio de aceitacao | verificado via codigo |
| 3.1 | `.e2e-pids` listado no `.gitignore` do frontend | arquivo na linha correta | linha 34 do `frontend/.gitignore`: `.e2e-pids` | ok |
| 3.2 | `npm test` (Vitest) | 61 passando, zero regressao | 61/61 passados | ok |

---

## 4. Resultado consolidado

| Item | Resultado |
|---|---|
| Analise de codigo | aprovado com observacoes (3 observacoes, nenhuma bloqueante) |
| Gates contra a realidade | ok (divergencia cosmética no hash de commit e pendencia downstream do ADR 0017 no checklist — nenhum e responsabilidade desta task) |
| Roteiro manual | aprovado (caso 2.2 verificado via leitura de codigo; comportamento confirmado pelo implementador) |
| **Veredito final** | mergear com as observacoes registradas |

---

## 5. Skills — feedback loop

Sem observacao de skills nesta task. `skills_eficazes: []`, `skills_gaps: []`.

---

## 6. Para o planner (proximos passos)

1. **Pendencia downstream do ADR 0017:** atualizar `CLAUDE.md` secao "Padrao de nome de branch" e `docs/runbooks/PRE-MERGE-CHECKLIST.md` para incluir prefixo `qa` no pattern de validacao. A tarefa foi listada no ADR 0017 secao 5 mas ainda nao foi executada. O gate `branch_convencao` do checklist esta tecnicamente desatualizado em relacao ao ADR vigente.

2. **Observacao 3 (tsconfig e2e):** registrar como pendencia tecnica em `docs/PENDENCIAS-TECNICAS.md` — os scripts e2e nao passam por type-check estatico no ciclo de build. Pode ser resolvido em QA-003 ou como FIX-NNN separado apos a sprint.

3. **QA-004** pode iniciar apos QA-003 (que depende de BE-023 em develop). Sem bloqueio desta avaliacao.
