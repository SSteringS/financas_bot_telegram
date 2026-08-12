# STATE — onde o projeto está agora

> **Doc vivo de orientação.** Existe pra uma sessão que começa fria (planner, back, front ou reviewer) se situar em 1 minuto, sem re-derivar contexto. **Curto de propósito.** Detalhe mora nos planos (`docs/sprints/<NN>-<slug>/plans/`), status reports (`docs/sprints/<NN>-<slug>/status/`) e ADRs (`docs/decisions/`).
>
> **Última atualização:** 2026-08-12 (sprint 04 em execução — QA-012 concluída; sprint 03 com duas tasks de front não executadas; produção restaurada em 2026-08-09).
> **Fonte:** derivado do frontmatter `estado:` dos status reports, do log do git e do histórico de runs do workflow `Deploy to Production`. Onde uma afirmação foi verificada por comando, o comando está citado. **O frontmatter dos status reports manda** — quando este resumo divergir dele, ele está errado.

---

## Sprint atual

**Sprint 04 — Instrumentação e qualidade** (`docs/sprints/04-instrumentacao-qualidade/README.md`).

**Modo:** 🟠 **em execução** — primeira task entregue.

**Objetivo:** instalar o ferramental de qualidade e a instrumentação de medição que o repositório não tem (JaCoCo, PIT, PMD, coleta de custo por papel), e corrigir a configuração dos subagentes que impede medir modelo por papel.

**Entrega que define "pronto":** um run completo do fluxo planner → backend → reviewer → QA de ponta a ponta, com custo por papel coletado, modelo comprovadamente pinado e métricas de qualidade gerando número real em vez de `na`.

**Backlog vivo:** `docs/sprints/04-instrumentacao-qualidade/backlog-s04.md`. Itens refinados viram planos em `plans/`.

**Branch de integração:** `integration/04-instrumentacao-qualidade` — criada e já usada (PR #124 mergeado em `develop`).

---

## Sprint 04 — estado das tasks

### ✅ Concluído

- **QA-012 — Piloto do PIT (mutation testing) em escopo reduzido.** PIT 1.25.9 + `pitest-junit5-plugin` 1.2.3 no `pom.xml`, escopo de 4 classes, 42 mutantes, leitura interpretada dos 5 sobreviventes. Reviewer: **aprovado com ressalvas** (11/11 critérios, 5 achados de prosa corrigidos). PRs #123 e #124.
  - Baseline registrado por classe no status report. Teto realista das 4 classes: **39/42 ≈ 93%** — 2 sobreviventes são equivalentes demonstrados e 1 é inalcançável na prática. **Não tratar 100% como meta.**

### 🟢 Derivado da QA-012, já materializado

- **ADR 0021 — gate de mutation testing opcional por task.** Critério: `test strength` (mortos ÷ **cobertos**) ≥ **80%**, medido **apenas sobre as classes alteradas pela task**; código pré-existente fica fora do denominador. Sobrevivente classificado como equivalente **com demonstração escrita** não conta contra o piso.
- **Item 11 do `BACKLOG-evolucao-workflow`** (mudanças em `.claude/`) — commit `105d955`, 2026-08-12. Regra de asserção sobre valor na `writing-java-unit-tests`; consistência rótulo × número na `artifact-report-contract`; campo `mutation_gate` no planner/backend/reviewer e nos dois templates de plano; remoção das referências penduradas a `.github/instructions/`.
  - ⚠️ **Config de agente é carregada no spawn** — isso só vale a partir da próxima sessão.

### 🔵 Próximo a refinar

- **Item #2 do backlog s04 — corrigir os testes fracos revelados pelo piloto.** Alvo concreto já identificado: falta caso com palavra-chave no **índice 0** em `LegendaParser`. Após escrever o teste, rodar o PIT e conferir que a classe sai de 6/8 para 7/8. Os outros dois sobreviventes são equivalentes e **não devem** ser perseguidos.
- Demais frentes ainda não refinadas: PMD (item #4), JaCoCo (#6), mecanismo de "classes tocadas" (#7), hooks de coleta de custo, pinagem de modelo dos agentes.

### Decisão pendente da sprint

- **Ordem em relação à tag do marco zero.** Definir se a sprint inteira precede a tag do baseline do experimento, ou só as tasks que tocam código de produção. (A dependência antiga do FIX-006 **caiu** — ver "Deploy" abaixo.)

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
- Última task usada por prefixo: **QA-012**, **BE-030**, **FE-017**, **FIX-007**.

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
