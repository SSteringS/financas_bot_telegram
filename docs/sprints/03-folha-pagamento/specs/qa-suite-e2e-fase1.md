---
sprint: "03"
feature: QA-FASE-1
slug: qa-suite-e2e-fase1
status: refinado
arquiteto: sim
data: 2026-06-01
relacionado_adr: 0017
relacionado_arquitetura: docs/architecture/desenho-testes-automatizados.md
revisoes:
  - 2026-06-01: spec inicial (input pro planner quebrar em tasks QA-NNN)
---

# Spec efêmera — Suíte E2E Fase 1 (MVP)

> **Input pro planner** quebrar em ~6 tasks `QA-NNN` que rodam **em paralelo à sprint 03 (Folha de Pagamento)**, com sequenciamento intra-sprint definido (lote A independente, lote B pós-BE-023).
>
> Fonte arquitetural: `docs/architecture/desenho-testes-automatizados.md` (consolidada 2026-06-01, atualizada na mesma data com correções servindo este input).
>
> **Base:** ADR 0017 `Accepted` (homologado 2026-06-01) — prefixo `QA-NNN` consolidado. Templates e CLAUDE.md raiz ainda não foram atualizados (tarefa do planner, conforme §5 do ADR 0017).

---

## 1. Escopo desta Fase 1

Entregar a infraestrutura mínima de E2E que **substitui** o `docs/runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md` (manual, 45-60 min) por `npm run e2e:full` (~2 min) com 3 specs cobrindo:

- **Fluxo feliz autenticado** (login mágico → home → detalhe → comprovante)
- **Webhook Telegram parametrizado** (2 cenários no MVP: texto puro + sticker; foto+caption fica de fora — ver §3)
- **Acessibilidade** das páginas autenticadas (axe-core, threshold serious+critical)

**Não inclui:**
- Filtros, paginação, regressões dos bugs A e B da FE-12 → Fase 2.
- Schemathesis (contract test) → Fase 2.
- Smoke pós-deploy em prod → Fase 3.
- Cenário foto+caption do webhook → aguarda decisão de mock do download de mídia Telegram (bloqueador conhecido, §3).

---

## 2. Sequenciamento intra-sprint (paralela à 03)

A sprint 03 (Folha de Pagamento) altera o schema via `V6__folha_pagamento.sql` (BE-023). Algumas tasks de E2E **dependem** do schema pós-V6 (helpers que fazem DELETE/INSERT em `pedidos_pagamento` precisam saber que `requisitante_id` existe). Outras são totalmente independentes (setup do framework, scripts de orquestração, docs).

Por isso a Fase 1 fica em **2 lotes**:

### Lote A — independente da sprint 03 (despachar imediatamente)

Não toca SQL, não depende de schema. Pode rodar 100% paralelo a qualquer task da sprint 03.

- **QA-001** Setup Playwright (config, tsconfig, scripts npm, .env.e2e.example)
- **QA-002** Scripts de orquestração (subir-stack, derrubar-stack, aguardar-saude)
- **QA-005** Doc `docs/runbooks/ROTEIRO-E2E.md` (planner)
- **QA-006** Item novo no PRE-MERGE-CHECKLIST (planner)

### Lote B — depende de BE-023 mergeada em develop

Toca SQL real (helper de fixtures, specs que fazem assert via SELECT). Despachar **só depois** que BE-023 mergear em develop e a integration branch da sprint 03 contenha o schema V6.

- **QA-003** Fixtures (`banco.ts`, `auth.ts`, `global-setup.ts`, `payloads-telegram.ts`)
- **QA-004** 3 specs do MVP (`site-fluxo-feliz`, `webhook-cenarios` com 2 cenários, `a11y-home`)

### Diagrama de dependências

```
                  ┌─────────────┐
                  │   QA-001    │  Setup Playwright
                  │  (front/QA) │
                  └──────┬──────┘
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
       ┌─────────────┐       ┌─────────────┐
       │   QA-002    │       │   QA-003    │ ← bloqueada até BE-023 em develop
       │  (front/QA) │       │  (front/QA) │
       │ Orquestração│       │  Fixtures   │
       └──────┬──────┘       └──────┬──────┘
              │                     │
              └──────────┬──────────┘
                         ▼
                  ┌─────────────┐
                  │   QA-004    │  3 specs MVP
                  │  (front/QA) │
                  └─────────────┘

       ┌─────────────┐       ┌─────────────┐
       │   QA-005    │       │   QA-006    │
       │  (planner)  │       │  (planner)  │
       │ Roteiro-E2E │       │PRE-MERGE add│
       └─────────────┘       └─────────────┘
              (independentes — podem rodar a qualquer momento)
```

---

## 3. Bloqueador conhecido — decisão pendente de mock de mídia Telegram

A spec arquitetural §13.1 marca como **bloqueador da Fase 1** a decisão sobre como o back trata o download de mídia em ambiente de teste. Quando o webhook recebe `foto+caption`, o back chama `getFile` no Telegram Bot API com o `file_id`. Em E2E o `file_id` é sintético (não existe no Telegram real) → erro.

**3 opções a comparar em sessão dedicada:**

| # | Opção | Vantagem | Custo |
|---|---|---|---|
| A | WireMock no profile dev | Não polui código de produção. Cobre o caminho completo do download. | Dependência nova (`wiremock-standalone`), porta extra (8089), config no `application-dev.properties`. |
| B | Flag `app.telegram.skip-media-download=true` ativada por profile | Implementação trivial (`if (flag) return null`). Zero dep nova. | Caminho de download nunca é exercitado em E2E — perde regressão dessa parte. |
| C | Stub `TelegramFileDownloader` ativado por `@Profile("e2e")` | Substitui no nível certo (porta hexagonal `out`). Permite stub retornar bytes fake quando útil. | Exige profile `e2e` novo. Pequena cerimônia de wiring Spring. |

**Recomendação preliminar (sem ADR ainda):** opção C, alinhada com a arquitetura hexagonal já adotada. Mas a decisão **é do humano + arquiteto** em sessão dedicada — vira ADR `Proposed` próprio antes de QA-004 ser despachada com o cenário foto+caption.

**Consequência operacional desta Fase 1:**

- QA-004 implementa `webhook-cenarios.spec.ts` com **2 cenários apenas**: texto puro + sticker (ambos não disparam download de mídia).
- O 3º cenário (foto+caption) fica documentado no plano de QA-004 como `// TODO Fase 1.1: adicionar após decisão de mock (ADR 00XX)`.
- Adicionar foto+caption depois é trivial graças à decisão 6 (parametrização) — 1 linha na tabela de cenários.

---

## 4. Tasks propostas (input pro planner)

> Estimativas em horas humanas. Esforço de implementação real é menor (todas QA são front/QA salvo onde indicado).

### QA-001 — Setup Playwright + config

| Campo | Valor |
|---|---|
| **Slug** | setup-playwright-base |
| **Esforço** | baixo (~2h) |
| **Território** | front |
| **Depende de** | nada |
| **Bloqueia** | QA-002, QA-003 |
| **Skills sugeridas** | (nenhuma específica além das always-on do role front; eventualmente `escrita-de-plano-completo` se houver) |
| **Lote** | A |

**Escopo:**

- Adicionar Playwright + axe-core às devDependencies do `frontend/package.json`:
  ```
  @playwright/test
  @axe-core/playwright
  tsx
  mysql2
  dotenv
  ```
- Criar `frontend/playwright.config.ts` com: `baseURL` via `E2E_FRONTEND_URL`, `globalSetup` apontando pra `e2e/fixtures/global-setup.ts` (arquivo será criado por QA-003 — referência por path), `use: { trace: 'on-first-retry', screenshot: 'only-on-failure', video: 'retain-on-failure' }`, projects: só Chromium.
- Criar `frontend/e2e/tsconfig.json` estendendo `../tsconfig.json` + ajustes mínimos pra `@playwright/test`.
- Criar `frontend/.env.e2e.example` versionado (template das credenciais, conteúdo idêntico ao §7.1 da spec arquitetural).
- Adicionar `frontend/.env.e2e` ao `.gitignore` se ainda não estiver.
- Adicionar scripts no `package.json`: `e2e`, `e2e:full`, `e2e:ui`, `e2e:report`.
- Criar `frontend/e2e/specs/.gitkeep` e `frontend/e2e/fixtures/.gitkeep` (pastas vazias com placeholder).

**Critérios de aceite:**
- [ ] `cd frontend && npx playwright test --list` executa sem erro (lista 0 specs).
- [ ] `cd frontend && cat .env.e2e.example` mostra o template completo.
- [ ] `.env.e2e` listado em `.gitignore`.
- [ ] `npm run e2e -- --help` mostra o help do Playwright.

**Fora de escopo:** specs (QA-004), helpers de fixtures (QA-003), scripts de orquestração (QA-002).

**`exige_e2e_full` no plano:** `false` (a infra não existe ainda).
**`testes_novos` esperado:** 0 (setup puro).

### QA-002 — Scripts de orquestração da stack

| Campo | Valor |
|---|---|
| **Slug** | scripts-orquestracao-stack |
| **Esforço** | médio (~2-3h) |
| **Território** | front |
| **Depende de** | QA-001 |
| **Bloqueia** | QA-004 |
| **Skills sugeridas** | nenhuma específica |
| **Lote** | A |

**Escopo:**

- Criar `frontend/e2e/scripts/aguardar-saude.ts` — função `aguardarHealthcheck(url, timeoutMs)` com polling exponencial.
- Criar `frontend/e2e/scripts/subir-stack.ts` — spawn `mvnw spring-boot:run -Dspring-boot.run.profiles=dev` em background, espera `GET /actuator/health` (timeout 60s) → spawn `vite` em background, espera `GET /` (timeout 30s). Sai com exit 0 quando ambos verdes. Falha rápida se MySQL local não estiver em `localhost:3306` (com mensagem clara).
- Criar `frontend/e2e/scripts/derrubar-stack.ts` — SIGTERM nos PIDs registrados pelo subir-stack (via file `.e2e-pids` ou similar).
- Garantir que o script `e2e:full` no `package.json` chame na ordem: `subir-stack → playwright test → (always) derrubar-stack`. Usar `try/finally` semântica em TS (`tsx`).

**Critérios de aceite:**
- [ ] `tsx e2e/scripts/aguardar-saude.ts http://localhost:8080/actuator/health 10000` retorna 0 se back estiver up, 1 se não.
- [ ] `tsx e2e/scripts/subir-stack.ts` sobe back+front e fica idle (manualmente verificado uma vez).
- [ ] `tsx e2e/scripts/derrubar-stack.ts` mata os processos.
- [ ] `npm run e2e:full` executa o ciclo completo (sem specs, exit 0 com aviso "no tests found").
- [ ] Se MySQL não estiver rodando, `subir-stack` falha em < 5s com mensagem "MySQL não respondeu em localhost:3306 — suba seu MySQL local e rode de novo".

**Fora de escopo:** specs (QA-004), fixtures (QA-003).

**`exige_e2e_full` no plano:** `false`.
**`testes_novos` esperado:** 0 (scripts de infra, não-testáveis por unit/integration).

### QA-003 — Fixtures (banco, auth, global-setup, payloads-telegram)

| Campo | Valor |
|---|---|
| **Slug** | fixtures-banco-auth-payloads |
| **Esforço** | médio (~3h) |
| **Território** | front (toca SQL via mysql2/promise) |
| **Depende de** | QA-001, **BE-023 mergeada em develop** |
| **Bloqueia** | QA-004 |
| **Skills sugeridas** | nenhuma específica |
| **Lote** | B |

**Escopo:**

- Criar `frontend/e2e/fixtures/banco.ts` — implementar `garantirRequisitanteE2E`, `limparDadosE2E`, `semearPedidos`, `querySql` conforme spec arquitetural §7.2 (já com **nomes reais das tabelas** — `pedidos_pagamento`, `comprovantes`).
- Criar `frontend/e2e/fixtures/auth.ts` — implementar `loginE2E(page)` conforme spec §7.4 (gera convite via admin API, faz exchange, injeta cookie no contexto).
- Criar `frontend/e2e/fixtures/global-setup.ts` — chama `garantirRequisitanteE2E` antes de tudo (§7.3).
- Criar `frontend/e2e/fixtures/payloads-telegram.ts` — factories sintéticas: `telegramUpdateTextoPuro({ fromUserId, text })`, `telegramUpdateSticker({ fromUserId })`. **Não incluir `telegramUpdateFotoLegenda` ainda** (bloqueado pela decisão de mock de mídia — §3 desta spec).
- Cada factory deve produzir um payload de `Update` válido do Telegram Bot API conforme `https://core.telegram.org/bots/api#update`. Mínimo necessário: `update_id`, `message: { message_id, date, from: { id }, chat: { id } }` + variação por tipo.

**Critérios de aceite:**
- [ ] `garantirRequisitanteE2E()` é idempotente (`INSERT IGNORE`); rodar 2x não falha.
- [ ] `limparDadosE2E()` deleta APENAS rows com `requisitante_id=99` (verificar com SELECT antes/depois usando `requisitante_id=1` que é o Pedro real).
- [ ] `loginE2E(page)` injeta cookie `finbot_session` no contexto; chamada subsequente a `page.goto('/')` resolve sem 401.
- [ ] `payloads-telegram.ts` exporta exatamente 2 factories no MVP (texto puro, sticker).
- [ ] Helper de banco usa as credenciais de `.env.e2e` (não hardcoded).
- [ ] Se `.env.e2e` ausente: mensagem de erro clara apontando pra `.env.e2e.example`.

**Fora de escopo:** specs (QA-004), foto+caption no payloads-telegram (bloqueado).

**`exige_e2e_full` no plano:** `false` (a suíte ainda não está completa).
**`testes_novos` esperado:** 0 (fixtures testáveis por unit seria over-engineering — confiar no smoke da QA-004).
**Atenção pro Reviewer:** confirmar que **nenhum** SQL de `limparDadosE2E` escapa o filtro `requisitante_id=99` (revisão paranóica — é a salvaguarda dos dados do Pedro).

### QA-004 — 3 specs MVP (site-fluxo-feliz, webhook-cenarios, a11y-home)

| Campo | Valor |
|---|---|
| **Slug** | specs-mvp-3-cenarios |
| **Esforço** | médio (~3-4h) |
| **Território** | front |
| **Depende de** | QA-002, QA-003 |
| **Bloqueia** | nada (entregável final do MVP) |
| **Skills sugeridas** | nenhuma específica |
| **Lote** | B |

**Escopo:**

- Criar `frontend/e2e/specs/site-fluxo-feliz.spec.ts` — código exato da spec arquitetural §8.1 (login → home → detalhe → comprovante + a11y inline na home).
- Criar `frontend/e2e/specs/webhook-cenarios.spec.ts` — código da §8.2, **com 2 cenários apenas** (texto puro + sticker). Manter a estrutura parametrizada (`for (const c of cenarios)`) já preparada para adicionar foto+caption no futuro (1 linha).
- Criar `frontend/e2e/specs/a11y-home.spec.ts` — código exato da §8.3.
- Configurar `playwright.config.ts` (atualização do que QA-001 criou) com `reporter: [['html'], ['list']]` e `outputDir: './playwright-report'`.
- Garantir que `playwright-report/` está no `.gitignore` da pasta `frontend/`.

**Critérios de aceite:**
- [ ] `npm run e2e:full` verde com 3 specs (5 testes no total: 1 fluxo feliz + 2 webhook + 1 a11y... e o webhook conta 2 testes pela parametrização → total 4 testes a executar, não 5; confirmar contagem real).
- [ ] Rodar `npm run e2e:full` 3 vezes seguidas em ambiente limpo → 3 verdes consecutivos (sem flakiness).
- [ ] Quando uma spec falha intencionalmente (alterar um assert pra forçar erro): `playwright-report/index.html` mostra screenshot + trace navegável.
- [ ] `webhook-cenarios.spec.ts` tem 1 comentário `// TODO Fase 1.1: adicionar cenário foto+caption após decisão de mock (ADR 00XX)` próximo à tabela `cenarios`.

**Fora de escopo:** cenário foto+caption no webhook (§3 — aguarda decisão de mock).

**`exige_e2e_full` no plano:** `false` (a suíte sendo entregue é a infra de validação — não há sentido em rodar como gate dela mesma).
**`testes_novos` esperado:** **4** (1 fluxo feliz + 2 webhook + 1 a11y — pelo critério "cada `test(...)` ou `for-of-test(...)` conta como teste novo no contexto da pirâmide E2E").
**Atenção pro Reviewer:** rodar manualmente uma vez antes de aprovar. Verificar que `playwright-report/` **não foi commitado**. Conferir tabela de combinações válidas do `e2e_full` no §9.2 da spec arquitetural — esta task NÃO consome esse gate, apenas o constrói.

### QA-005 — Doc `docs/runbooks/ROTEIRO-E2E.md`

| Campo | Valor |
|---|---|
| **Slug** | doc-roteiro-e2e |
| **Esforço** | baixo (~1h) |
| **Território** | plan (docs/) |
| **Depende de** | nada (pode rodar 100% paralelo) |
| **Bloqueia** | nada |
| **Skills sugeridas** | `escrita-de-plano-completo` (se aplicável) |
| **Lote** | A (independente) |

**Escopo:**

Criar `docs/runbooks/ROTEIRO-E2E.md` cobrindo:

1. **Pré-requisitos** — MySQL local rodando, `.env.e2e` preenchido (copiar de `.env.e2e.example`), Java 21, Node 20+, Docker NÃO necessário (decisão arquitetural §5.1 item 1).
2. **Como rodar** — `cd frontend && npm run e2e:full` (referenciar §14.2 da spec arquitetural).
3. **Como interpretar resultado** — onde fica o relatório HTML, como abrir o trace viewer (`npx playwright show-trace`), como interpretar screenshot/video em falhas.
4. **Troubleshooting** — MySQL não está rodando (mensagem clara), `.env.e2e` ausente, porta 8080/5173 ocupada, suíte travada em healthcheck.
5. **Relação com o `ROTEIRO-INTEGRACAO-FRONT-BACK.md`** — este novo roteiro **substitui** o manual para a maior parte dos cenários cobertos. O manual permanece como Camada 5 (Telegram real, cert SSL, ngrok) e como fallback se a suíte E2E estiver quebrada por qualquer motivo.
6. **Como adicionar cenário novo** — apontar §8.2 da spec arquitetural (tabela de cenários parametrizada).
7. **Aviso sobre dados** — cleanup limpa SOMENTE `requisitante_id=99`. Dados do Pedro (`id=1`) ficam intocados. NUNCA rodar com `E2E_DB_NAME` apontando pra produção.

**Critérios de aceite:**
- [ ] Arquivo criado com as 7 seções acima.
- [ ] Referenciado em `docs/runbooks/` (índice se houver).
- [ ] Mencionado no PRE-MERGE-CHECKLIST (relacionado com QA-006).

**Fora de escopo:** o item do PRE-MERGE-CHECKLIST (QA-006).

**`exige_e2e_full` no plano:** `false` (task de documentação).
**`testes_novos` esperado:** 0.

### QA-006 — Item novo no PRE-MERGE-CHECKLIST

| Campo | Valor |
|---|---|
| **Slug** | pre-merge-add-e2e-gate |
| **Esforço** | baixo (~15-30min) |
| **Território** | plan (docs/) |
| **Depende de** | QA-005 (referencia o ROTEIRO-E2E.md) |
| **Bloqueia** | nada |
| **Skills sugeridas** | nenhuma específica |
| **Lote** | A |

**Escopo:**

Editar `docs/runbooks/PRE-MERGE-CHECKLIST.md` adicionando 2 itens, **na seção do implementador** e **na seção do Reviewer**, conforme §9.3 da spec arquitetural:

**Na seção do implementador (gate condicional, não bloqueante por padrão):**
- [ ] Se o plano declara `exige_e2e_full: true`: `npm run e2e:full` verde, bloco `e2e_full` preenchido no status report (ver `docs/architecture/desenho-testes-automatizados.md` §9.2 para schema).

**Na seção do Reviewer (auditoria, bloqueante se inconsistente):**
- [ ] Se `exige_e2e_full: true` no plano: confirmar que `e2e_full.executado: true` e `e2e_full.status: verde` no status report. Caso contrário, **rejeitar** com pendência bloqueante.

Não atualizar o `_TEMPLATE-status.md` ainda — o bloco `e2e_full` no frontmatter só passa a ser obrigatório quando a infra estiver entregue (após QA-004). Esta é uma decisão deliberada: evita que tasks da própria sprint 03 (Folha) sejam obrigadas a preencher um campo que a infra ainda não suporta.

**Critérios de aceite:**
- [ ] PRE-MERGE-CHECKLIST contém os 2 novos itens nas seções corretas.
- [ ] Cada item referencia o documento de spec arquitetural por path completo.
- [ ] Item do Reviewer descreve explicitamente a ação de "rejeitar com pendência bloqueante" em caso de inconsistência.

**Fora de escopo:** atualização do `_TEMPLATE-status.md` (fica pra task futura quando a infra estiver pronta).

**`exige_e2e_full` no plano:** `false`.
**`testes_novos` esperado:** 0.

---

## 5. Regras transversais aplicáveis a todas as tasks QA-NNN desta Fase 1

- **`exige_e2e_full: false`** em todas. A suíte está sendo construída; não há sentido em rodar como gate dela mesma.
- **`testes_novos`** — contar apenas testes do Playwright (cada `test(...)` no spec). Tasks de setup/scripts/fixtures/doc contam 0. Tasks que adicionam specs contam o número de `test(...)` real.
- **Cobertura JaCoCo / Vitest** — `cobertura_pct: na` (E2E não move a métrica de cobertura de unit).
- **Branch** — `feature/qa-NNN-<slug>` saindo de `integration/03-folha-pagamento`. PR `feature → integration` aceito pelo próprio implementador (não passa por revisão humana, conforme CLAUDE.md atualizado em 2026-06-01).
- **Reviewer obrigatório** — toda task QA passa por Reviewer antes do merge (ADR 0005), conforme regra geral.
- **Status report** — usar `_TEMPLATE-status.md` padrão. Frontmatter sem campo `e2e_full` ainda (esse campo entra em uso depois que QA-006 e a infra completa estiverem em develop — combinação válida do §9.2 da spec arquitetural).
- **Território declarado no frontmatter** — `territorio: front` (mesmo que toque SQL via mysql2; o código vive em `frontend/e2e/`). Exceção: QA-005 e QA-006 são `territorio: plan`.
- **Skills** — nenhuma skill obrigatória; planner pode adicionar conforme contexto.

---

## 6. Riscos consolidados desta Fase 1

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| `requisitante_id` ainda não existir em `pedidos_pagamento` quando QA-003 começar (BE-023 não mergeada) | Média (depende do sequenciamento de execução) | Alto | Lote B só despachado após confirmar BE-023 em develop. Planner gate. |
| Cleanup deletar dados do Pedro (`requisitante_id=1`) por bug no SQL do helper | Baixa | **Crítico** | Reviewer paranóico em QA-003. Teste manual antes do PR: `SELECT count FROM pedidos_pagamento` antes/depois, deve ser igual. |
| Flakiness em healthcheck (back demorando >60s pra subir) | Média | Médio | Timeout configurável via env. Aumentar pra 90s no script se necessário. Log claro do tempo esperado vs real. |
| Playwright derrubar a stack incompleta (deixar back rodando após `Ctrl+C`) | Baixa | Baixo | `try/finally` no `e2e:full`. Documentar `kill_orphan_processes.sh` no troubleshooting do ROTEIRO-E2E. |
| Cookie `finbot_session` ter `Domain` errado em dev (não bater com `localhost`) | Média | Médio | `loginE2E` faz parse do `Set-Cookie` real e injeta com `domain: 'localhost'` explícito (já no código §7.4). |
| MSW interferir com requests do Playwright pro back real | Baixa | Alto | Playwright **não** carrega service worker do MSW (config separada, `e2e/` tem seu próprio tsconfig). Confirmar em QA-001. |
| Cenário foto+caption desejado mas decisão de mock não fechou | Alta (já é a realidade) | Baixo | Marcado como bloqueador conhecido. QA-004 não inclui esse cenário. |

---

## 7. Critério de "Fase 1 entregue"

Todos abaixo verdes:

- [ ] QA-001 a QA-006 mergeadas em `integration/03-folha-pagamento` (e integration mergeada em develop conforme ritual de fechamento da sprint 03).
- [ ] `npm run e2e:full` verde 3 vezes seguidas em ambiente limpo (critério arquitetural §10).
- [ ] `docs/runbooks/ROTEIRO-E2E.md` publicado e referenciado pelo PRE-MERGE-CHECKLIST.
- [ ] Bloco `e2e_full` no `_TEMPLATE-status.md` **planejado para sprint 04** (não é desta sprint — ver QA-006 escopo).
- [ ] ADR de mock de mídia Telegram (decisão pendente §3) aberto como `Proposed` para a sprint 04 endereçar.

---

## 8. Métricas de sucesso (medir na retro da sprint 03)

| Métrica | Baseline | Alvo Fase 1 |
|---|---|---|
| Fluxos E2E automatizados | 0 | 3 specs (4 `test(...)` calls) |
| Tempo "validar integração front↔back" | 45-60 min (manual) | ~2 min (`npm run e2e:full`) |
| Tasks `QA-NNN` despachadas | 0 | 6 |
| Decisão pendente de mock de mídia | em aberto | promovida a ADR `Proposed` separado |
| Item no PRE-MERGE-CHECKLIST | inexistente | adicionado nas seções implementador + Reviewer |

---

## 9. Referências

- **Fonte arquitetural:** `docs/architecture/desenho-testes-automatizados.md` (atualizada 2026-06-01).
- **ADR 0017 `Accepted`** (2026-06-01): prefixo `QA-NNN` consolidado.
- **ADR 0005:** Reviewer somente-leitura — base da decisão E+A+D do §5.2 item 10 da arquitetura.
- **ADR 0007:** status report como output contract — base do campo `e2e_full`.
- **`docs/aprendizado/spec-efemera-vs-arquitetura-duradoura.md`** (novo, 2026-06-01): conceito da diferença entre spec efêmera (esta) e arquitetura durável.
- **Sprint 03 plans:** `docs/sprints/03-folha-pagamento/plans/BE-023-migracao-v6-folha-pagamento.md` — fonte da dependência do lote B.
- **`docs/runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md`** — runbook manual que esta Fase 1 substitui em ~80% dos cenários.

---

## 10. O que esta spec NÃO faz

- **Não escreve os planos de task.** O planner ainda precisa copiar `_TEMPLATE-plano.md` para cada `QA-NNN` e preencher conforme esta spec.
- **Não escreve os DISPATCH.** Mesma lógica do EVO-09 — planner cria os DISPATCH-* quando despachar.
- **Não decide o mock de mídia.** Bloqueador §3 fica aberto, vira ADR separado.
- **Não atualiza `_TEMPLATE-status.md`** com bloco `e2e_full`. Essa atualização é desta sprint+1 (quando infra estiver pronta).
- **Não modifica CLAUDE.md raiz** com prefixo QA. Essa mudança depende do ADR 0017 virar `Accepted`.
