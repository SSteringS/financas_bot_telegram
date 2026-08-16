# STATE — onde o projeto está agora

> **Doc vivo de orientação.** Existe pra uma sessão que começa fria (planner, back, front ou reviewer) se situar em 1 minuto, sem re-derivar contexto. **Curto de propósito.** Detalhe mora nos planos (`docs/sprints/<NN>-<slug>/plans/`), status reports (`docs/sprints/<NN>-<slug>/status/`) e ADRs (`docs/decisions/`).
>
> **Última atualização:** 2026-08-16 (sprint 04 em execução — QA-012, QA-013, QA-014 e FIX-008 concluídas; ferramental de qualidade completo; sprint 03 com duas tasks de front não executadas; produção restaurada em 2026-08-09).
> **Fonte:** derivado do frontmatter `estado:` dos status reports, do log do git e do histórico de runs do workflow `Deploy to Production`. Onde uma afirmação foi verificada por comando, o comando está citado. **O frontmatter dos status reports manda** — quando este resumo divergir dele, ele está errado.

---

## Sprint atual

**Sprint 04 — Instrumentação e qualidade** (`docs/sprints/04-instrumentacao-qualidade/README.md`).

**Modo:** 🟠 **em execução** — três tasks de ferramental entregues (QA-012, QA-013, QA-014) mais a FIX-008. **O ferramental de qualidade está completo**; o que resta é instrumentação de medição, configuração de agentes e o item #2 (corrigir os testes fracos revelados pelos pilotos).

**Objetivo:** instalar o ferramental de qualidade e a instrumentação de medição que o repositório não tem (JaCoCo, PIT, PMD, coleta de custo por papel), e corrigir a configuração dos subagentes que impede medir modelo por papel.

**Entrega que define "pronto":** um run completo do fluxo planner → backend → reviewer → QA de ponta a ponta, com custo por papel coletado, modelo comprovadamente pinado e métricas de qualidade gerando número real em vez de `na`.

**Backlog vivo:** `docs/sprints/04-instrumentacao-qualidade/backlog-s04.md`. Itens refinados viram planos em `plans/`.

**Branch de integração:** `integration/04-instrumentacao-qualidade` — criada e já usada (PR #124 mergeado em `develop`).

---

## Sprint 04 — estado das tasks

### ✅ Concluído

- **QA-012 — Piloto do PIT (mutation testing) em escopo reduzido.** PIT 1.25.9 + `pitest-junit5-plugin` 1.2.3 no `pom.xml`, escopo de 4 classes, 42 mutantes, leitura interpretada dos 5 sobreviventes. Reviewer: **aprovado com ressalvas** (11/11 critérios, 5 achados de prosa corrigidos). PRs #123 e #124.
  - Baseline registrado por classe no status report. Teto realista das 4 classes: **39/42 ≈ 93%** — 2 sobreviventes são equivalentes demonstrados e 1 é inalcançável na prática. **Não tratar 100% como meta.**

- **QA-013 — PMD: ruleset curado e piloto de leitura interpretada.** PRs #127 e #128. Reviewer: **aprovado com observações**, 2 rodadas (1 achado `high` derrubado e corrigido; `F6` `low` segue aberto, custo de duas frases). Absorveu o item #5 do backlog.
  - `maven-pmd-plugin` fora do ciclo de vida + `pmd-ruleset.xml` com **10 regras justificadas uma a uma**. Gate `lint` do backend **deixa de ser `na`** — informativo, não bloqueante.
  - **Baseline congelado — só vale junto com o hash:** `Q7_producao = 22` · `Q7_teste = 1` · `sha256 = 5100b68f387a0f0d4a8a6d8ba6540c715854709869790373df1118acb71755a9`. **Δ medido contra outro ruleset não é comparável.**
  - Achado de maior valor: **2 bugs reais de locale**, um deles **gravando dado corrompido no banco** (`PaymentProofStrategy:81`). Ver registro de pendências.

- **QA-014 — Piloto do JaCoCo: cobertura medida e comparada com PIT e PMD.** PRs #130 e #131. Reviewer: **2 rodadas** — rodada 1 `rejected` por premissa falha (afirmação do plano que não se sustentava), rodada 2 aprovada após 6 correções de texto. **Nenhum número precisou ser refeito.**
  - **Baseline unit-only, com `lombok.config`:** linha **90,2%** (1681/1864) · branch **75,8%** (326/430) · 374 testes executados dos 422 · 148 classes. ⚠️ **Subestima a cobertura real** — a suíte de integração está fora do recorte, e isso é definição da métrica.
  - **As três ferramentas agora descrevem o mesmo código.** PIT e JaCoCo concordam **linha a linha** em 3 das 4 classes; a única divergência (`LegendaParser`, 94% × 100%) é o construtor privado de classe utilitária, que o JaCoCo filtra e o PIT não — e **o número do JaCoCo é o melhor**.
  - **Achado que mais ensinou:** o Lombok inflava o denominador em 604 métodos e 128 desvios, com **126 dos 128 descobertos** — 55% de toda a "cobertura de branch faltante" era `equals`/`hashCode` gerado. A cobertura de **linha mal se moveu** (+0,9 pp) enquanto a de **branch subiu 17 pp**. Argumento concreto para **nunca reportar cobertura de linha sozinha**.
  - `cobertura_pct` **destravado**, com regra provisória: cobertura das classes de produção que a task tocou; `na` quando não toca nenhuma.

- **FIX-008 — CI aceita `develop` no gate de convenção de branch.** PR #126 mergeado. Destrava o PR de sincronização `develop → integration`, que o gate rejeitava. **Executada pelo planner sob autorização explícita, sem Reviewer** — desvio registrado no status; `territorio: excecao-autorizada`.
  - Débito herdado: a faixa `[a-z0-9]` da regex é **sensível a locale** — sob `LC_COLLATE=en_US`, `feature/QA-013` é **aceita**. Não introduzido pela task; registrado no global.

### 🟢 Derivado das tasks, já materializado

- **ADR 0021 — gate de mutation testing opcional por task.** Critério: `test strength` (mortos ÷ **cobertos**) ≥ **80%**, medido **apenas sobre as classes alteradas pela task**; código pré-existente fica fora do denominador. Sobrevivente classificado como equivalente **com demonstração escrita** não conta contra o piso.
- **Item 11 do `BACKLOG-evolucao-workflow`** (mudanças em `.claude/`) — commit `105d955`, 2026-08-12. Regra de asserção sobre valor na `writing-java-unit-tests`; consistência rótulo × número na `artifact-report-contract`; campo `mutation_gate` no planner/backend/reviewer e nos dois templates de plano; remoção das referências penduradas a `.github/instructions/`.
  - ⚠️ **Config de agente é carregada no spawn** — isso só vale a partir da próxima sessão.
- **`.codex/` deletado pelo humano em 2026-08-13** ("nem uso o codex"), 8 definições de agente fora do repo. `.claude/agents/` é hoje o **único** conjunto de definições. Resíduo: **nenhum agente instrui a medir cobertura** — `.claude/agents/qa-test-specialist.md` nunca mencionou JaCoCo. Fix combinado com o humano em 2026-08-16; executor (`ai-engineer` × planner autorizado) a confirmar.

### 🔵 Próximo a refinar

- **Item #2 do backlog s04 — corrigir os testes fracos revelados pelos pilotos.** ✅ **planejado como `QA-015`** (2026-08-16), `pronto-pra-execucao`, aguardando dispatch. Dois alvos: caso com palavra-chave no **índice 0** em `LegendaParser` (6/8 → **7/8** no PIT) e o `throw` de `parsePedido` em `PaymentRequestStrategy` (branch 7/8 → **8/8** no JaCoCo). Os mutantes equivalentes **não devem** ser perseguidos.
  - **Primeira task do repositório com `mutation_gate: true`.** Piso de 80% de `test strength` sobre as duas classes (ADR 0021); esperado 18/19 ≈ 94,7%.
  - Precisa acontecer **antes** da tag do marco zero — autorizado pelo humano. Toca as classes que a feature do experimento vai tocar, e mexer nelas no meio do experimento invalida a comparação entre runs.
- **Item #8 — gate da convenção `*IntegrationTest`.** A QA-013 trouxe **segunda evidência a favor do ArchUnit**: nenhuma das três ferramentas da sprint mede conformidade arquitetural. Decisão ArchUnit × reflection puro segue com o humano.
- **Ferramental de qualidade está completo** — PIT, PMD e JaCoCo instalados, com baseline congelado em cada. O que resta da sprint é instrumentação de medição e configuração de agentes.
- Demais frentes não refinadas: mecanismo de "classes tocadas" (#7), hooks de coleta de custo, pinagem de modelo dos agentes.

> ⚠️ **Lição da QA-014, que continua valendo:** uma afirmação do plano ("não existe X no repositório") foi publicada com base em `grep` de **três diretórios** e derrubada por `git grep` na árvore inteira — que devolvia 77 linhas. **Verificação de escopo amplo exige busca de escopo amplo.** O que a derrubou foi o `.codex/agents/`, um segundo conjunto de definições de agente que o planner não sabia existir; **o `.codex/` foi deletado pelo humano em 2026-08-13** e hoje `.claude/agents/` é o único. A lição de método sobrevive ao fato que a originou.

### Decisão pendente da sprint

- **Ordem em relação à tag do marco zero.** Definir se a sprint inteira precede a tag do baseline do experimento, ou só as tasks que tocam código de produção. (A dependência antiga do FIX-006 **caiu** — ver "Deploy" abaixo. O `README.md` do experimento, item 6, ainda registra a versão antiga e precisa ser corrigido.)
  - **Resolvido em parte, 2026-08-16:** o humano autorizou o **item #2 antes da tag**. O escopo geral — se o resto da sprint também precede — segue em aberto.

---

## Deploy e produção

**Produção está no ar.** Verificado por `gh run list --workflow="Deploy to Production"`:

| Run | Quando | Resultado |
|---|---|---|
| `31285612236` | 2026-08-09T00:10Z | ❌ failure — crash-loop, causa do incidente |
| `31324695450` | 2026-08-09T16:47Z | ✅ success |
| `31341256203` | 2026-08-09T23:10Z | ✅ success — **estado atual** |

**Incidente encerrado:** produção ficou fora do ar de 2026-08-09T00:12Z até o deploy verde do mesmo dia. Causa e correção em `docs/sprints/03-folha-pagamento/status/FIX-006-whatsapp-defaults-no-properties.md`. FIX-006 e FIX-007 estão em `main` (PR #122) — verificado com `git merge-base --is-ancestor`.

> ⚠️ Versões anteriores deste arquivo diziam "PR develop → main não aberto ainda". **Estava stale e um plano se apoiou nisso.** Se for citar estado de deploy, confira o run antes.

---

## Sprints anteriores

- **Sprint 03 — Folha de Pagamento:** ✅ **fechada 2026-08-12**, 23/25 tasks. A entrega que definia a sprint está em produção. **Sem retrospectiva** — decisão explícita do humano, não esquecimento. **QA-010** (cobertura de testes front) e **QA-011** (expansão E2E de cenários positivos) foram encerradas **sem execução**; os planos seguem em `plans/` e os gaps viraram débito técnico. FIX-006 e FIX-007 passaram de `parcial` para `concluido` na mesma passagem — o código já estava em `main` e o `parcial` refletia só pendência humana, migrada para o registro de débitos.
- **Sprint 02b — Kaizen (workflow):** ✅ fechada 2026-05-31. `RETRO-02b-kaizen-workflow.md`.
- **Sprint 02 — Canal WhatsApp:** ✅ fechada 2026-05-30. `RETRO-02-canal-whatsapp.md`.
- **Sprint 01 — MVP Fase 3:** ✅ fechada. `RETRO-01-mvp-fase3.md`.

---

## Pendências abertas (fora das sprints)

- 🔴 **Rotacionar os segredos que estiveram versionados (FIX-007)** — `admin_api_key` de dev, `keystore_password`, senha do MySQL local. Estiveram num repo **público**; considerar coletados. O FIX interrompeu a exposição, **não remediou**. ⚠️ Trocar o `keystore_password` sem reassinar o `/opt/finbot/keystore.p12` da EC2 **quebra o boot da aplicação**. **É o item mais urgente do repositório.**
- **`keystore_password` não confirmado em `finbot-prod-secrets`** — se faltar, o próximo recreate da EC2 aborta o bootstrap e a instância fica sem aplicação e sem reverse proxy.
- **Validação funcional do webhook WhatsApp pós-deploy (FIX-006)** nunca registrada — o deploy verde prova que a app sobe, não que a guarda fail-closed funciona.
- **QA-010 / QA-011** — gaps de teste de front e de E2E do caminho positivo, herdados da sprint 03.
- 🔴 **`PaymentProofStrategy:81` grava dado corrompido sob locale não-inglês** — `toUpperCase()` sem `Locale` persiste `PİX` em `comprovantes.tipo_pagamento`. O dado errado **fica no banco depois** de o locale ser corrigido. Mais 4 ocorrências da mesma classe, 2 delas ainda não lidas. Achado da QA-013.
- **Testcontainers não alcança o Docker na máquina local** — 48 testes de integração falham aqui e passam no CI (`Tests run: 422`, run `31727999562`). **Não é código**, é ambiente: `docker info` responder no shell não garante que a JVM do Maven alcança o daemon. Enquanto não resolver, toda task tende a fechar `parcial` por um gate que não reflete o repositório.
- **DEP-07:** PR #64 aguarda merge + `terraform apply` (in-place confirmado).
- **DEP-08:** webhook via Caddy/Let's Encrypt — Fase 2, aguarda ADR de topologia TLS.
- **BE-20 / PREP-WA fases 4, 8, 9:** bloqueado por chip WhatsApp Business + Business Verification (dependência externa). Popular os 4 secrets em `finbot-prod-secrets` é pré-condição, e há duas guardas de sentinela a remover quando isso acontecer.
- **§7 — vales na lista do Pedro:** Opção A (filtrar GET) ou B (tag visual). Resolver como FIX.
- Demais débitos: `docs/PENDENCIAS-TECNICAS.md` (inclui os 7 achados da QA-012 e os 2 registrados em 2026-08-12).

---

## Fluxo de branches

```
main (protegida — só via PR; merge dispara deploy)
 └── develop  ← planner commita direto (worktree financas_bot_telegram-planner)
      ├── integration/04-instrumentacao-qualidade  ← sprint atual
      │    └── feature/qa-NNN-<slug>
      ├── fix/NNN-<slug>      → develop direto
      └── hotfix/NNN-<slug>   → develop direto
```

- **feature → integration:** implementador abre e pode auto-aceitar.
- **integration → develop:** planner abre no fim da sprint; **humano homologa** — único gate humano do fluxo de features.
- **fix/hotfix → develop:** PR direto.
- Última task usada por prefixo: **QA-014**, **BE-030**, **FE-017**, **FIX-008**.

---

## Mapa rápido de onde mora o quê

| Preciso de… | Vou em… |
|---|---|
| O que construir (spec de task) | `docs/sprints/<NN>-<slug>/plans/` |
| O que foi feito (execução) | `docs/sprints/<NN>-<slug>/status/` |
| Avaliação do Reviewer | `docs/sprints/<NN>-<slug>/avaliacoes/` |
| Decisão arquitetural canônica | `docs/decisions/` (ADRs) |
| Regra que o agente obedece | `CLAUDE.md` (raiz e `financas_bot_telegram/`) |
| Contrato de artefato dos subagentes | `.claude/skills/artifact-report-contract/` |
| Conceito pra revisitar | `docs/aprendizado/` |
| Definição de pronto / gates | `docs/runbooks/PRE-MERGE-CHECKLIST.md` |
| Como rodar a suíte E2E | `docs/runbooks/ROTEIRO-E2E.md` |
| Como rodar testes de backend / PIT | `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` |
| Débito técnico conhecido | `docs/PENDENCIAS-TECNICAS.md` |
| Melhorias de processo | `docs/plans/BACKLOG-evolucao-workflow.md` |
