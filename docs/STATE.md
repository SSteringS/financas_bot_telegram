# STATE — onde o projeto está agora

> **Doc vivo de orientação.** Existe pra uma sessão que começa fria (planner, back, front ou reviewer) se situar em 1 minuto, sem re-derivar contexto. **Curto de propósito.** Detalhe mora nos planos (`docs/sprints/<NN>-<slug>/plans/`), status reports (`docs/sprints/<NN>-<slug>/status/`) e ADRs (`docs/decisions/`).
>
> **Última atualização:** 2026-08-16 (sprint 04 em execução — QA-012, QA-013, QA-014, **QA-015** e FIX-008 concluídas; ferramental de qualidade completo e **item #2 do backlog fechado**; **tag do marco zero desbloqueada**; auditoria de alcance de skills fechada com a ADR 0022 e seu risco de assets **confirmado em runtime**; sprint 03 com duas tasks de front não executadas; produção restaurada em 2026-08-09).
> **Fonte:** derivado do frontmatter `estado:` dos status reports, do log do git e do histórico de runs do workflow `Deploy to Production`. Onde uma afirmação foi verificada por comando, o comando está citado. **O frontmatter dos status reports manda** — quando este resumo divergir dele, ele está errado.

---

## Sprint atual

**Sprint 04 — Instrumentação e qualidade** (`docs/sprints/04-instrumentacao-qualidade/README.md`).

**Modo:** 🟠 **em execução** — três tasks de ferramental entregues (QA-012, QA-013, QA-014), mais a QA-015 e a FIX-008. **O ferramental de qualidade está completo e o item #2 fechou**; o que resta é instrumentação de medição, configuração de agentes e as decisões pendentes com o humano.

**Objetivo:** instalar o ferramental de qualidade e a instrumentação de medição que o repositório não tem (JaCoCo, PIT, PMD, coleta de custo por papel), e corrigir a configuração dos subagentes que impede medir modelo por papel.

**Entrega que define "pronto":** um run completo do fluxo planner → backend → reviewer → QA de ponta a ponta, com custo por papel coletado, modelo comprovadamente pinado e métricas de qualidade gerando número real em vez de `na`.

**Backlog vivo:** `docs/sprints/04-instrumentacao-qualidade/backlog-s04.md`. Itens refinados viram planos em `plans/`.

**Branch de integração:** `integration/04-instrumentacao-qualidade` — criada e já usada (PR #124 mergeado em `develop`). **Sincronizada com `develop` em 2026-08-16 pelo PR #132**, que levou até ela o plano da QA-015, o da BE-031, as cinco mudanças de `.claude/agents/` e a ADR 0022. Quem sair dela agora enxerga o próprio plano da task; sem o sync, não enxergava.

---

## Sprint 04 — estado das tasks

### ✅ Concluído

- **QA-012 — Piloto do PIT (mutation testing) em escopo reduzido.** PIT 1.25.9 + `pitest-junit5-plugin` 1.2.3 no `pom.xml`, escopo de 4 classes, 42 mutantes, leitura interpretada dos 5 sobreviventes. Reviewer: **aprovado com ressalvas** (11/11 critérios, 5 achados de prosa corrigidos). PRs #123 e #124.
  - Baseline registrado por classe no status report. Teto realista das 4 classes: **39/42 ≈ 93%** — 2 sobreviventes são equivalentes demonstrados e 1 é inalcançável na prática. **Não tratar 100% como meta.**
  - **Score atualizado pela QA-015: 37/42 → 38/42** (`Test strength 93%`). ⚠️ **O teto de 39/42 NÃO mudou** — o mutante que a QA-015 matou já estava contado como matável dentro dele. Matar mutante aproxima o score do teto; não eleva o teto. O plano da QA-015 mandava elevá-lo para 40/42, foi derrubado pelo Reviewer e está com errata registrada. **Falta 1 para o teto:** `MetaSignatureValidator:28`, o warn de `app-secret`, que exige infra de captura de log que o repo não tem.

- **QA-013 — PMD: ruleset curado e piloto de leitura interpretada.** PRs #127 e #128. Reviewer: **aprovado com observações**, 2 rodadas (1 achado `high` derrubado e corrigido; `F6` `low` segue aberto, custo de duas frases). Absorveu o item #5 do backlog.
  - `maven-pmd-plugin` fora do ciclo de vida + `pmd-ruleset.xml` com **10 regras justificadas uma a uma**. Gate `lint` do backend **deixa de ser `na`** — informativo, não bloqueante.
  - **Baseline congelado — só vale junto com o hash:** `Q7_producao = 22` · `Q7_teste = 1` · `sha256 = 5100b68f387a0f0d4a8a6d8ba6540c715854709869790373df1118acb71755a9`. **Δ medido contra outro ruleset não é comparável.**
  - Achado de maior valor: **2 bugs reais de locale**, um deles **gravando dado corrompido no banco** (`PaymentProofStrategy:81`). Ver registro de pendências.

- **QA-014 — Piloto do JaCoCo: cobertura medida e comparada com PIT e PMD.** PRs #130 e #131. Reviewer: **2 rodadas** — rodada 1 `rejected` por premissa falha (afirmação do plano que não se sustentava), rodada 2 aprovada após 6 correções de texto. **Nenhum número precisou ser refeito.**
  - **Baseline unit-only, com `lombok.config`:** linha **90,2%** (1681/1864) · branch **75,8%** (326/430) · 374 testes executados dos 422 · 148 classes. ⚠️ **Subestima a cobertura real** — a suíte de integração está fora do recorte, e isso é definição da métrica.
  - **As três ferramentas agora descrevem o mesmo código.** PIT e JaCoCo concordam **linha a linha** em 3 das 4 classes; a única divergência (`LegendaParser`, 94% × 100%) é o construtor privado de classe utilitária, que o JaCoCo filtra e o PIT não — e **o número do JaCoCo é o melhor**.
  - **Achado que mais ensinou:** o Lombok inflava o denominador em 604 métodos e 128 desvios, com **126 dos 128 descobertos** — 55% de toda a "cobertura de branch faltante" era `equals`/`hashCode` gerado. A cobertura de **linha mal se moveu** (+0,9 pp) enquanto a de **branch subiu 17 pp**. Argumento concreto para **nunca reportar cobertura de linha sozinha**.
  - `cobertura_pct` **destravado**, com regra provisória: cobertura das classes de produção que a task tocou; `na` quando não toca nenhuma.

- **QA-015 — Fortalecer os testes fracos revelados pelos pilotos.** PRs #133 e #134. Reviewer: **`approved-with-notes`**, zero achado `critical`/`high`, **nenhuma remediação de código** — o Reviewer reexecutou PIT, JaCoCo e build e reproduziu os dois números. Fecha o item **#2** do backlog. 2 testes novos, **zero linha de `src/main/`**, `cobertura_pct: 100`.
  - `LegendaParser` **6/8 → 7/8**; `PaymentRequestStrategy` branch **7/8 → 8/8** (linha 49/49). O sobrevivente restante **continua vivo por projeto** — equivalente demonstrado. **7/8 é teto, não pendência:** nenhum reforço de teste leva a 8/8, e task que prometa isso promete o impossível.
  - **Primeira task sob o `mutation_gate` (ADR 0021): 94,7% de `test strength` ≥ 80% ✅.** Nenhuma classe foi removida do escopo, nenhum teste foi escrito para levantar métrica, o piso não foi mexido.
  - 🔑 **O achado que inverte a lição da sprint:** no alvo 2 o **PIT não se moveu** — dava 11/11 antes de existir teste que atingisse o `throw`, porque nenhum mutante é gerado na construção da exceção. Quem achou o buraco foi o **JaCoCo**. É o espelho exato do `LegendaParser`, onde a cobertura era 100/100 e quem achou foi o PIT. **Nenhuma das duas ferramentas domina a outra**, e agora existe dado deste projeto nas duas direções — argumento para nunca rodar só uma.
  - **A tag do marco zero está desbloqueada** — as duas classes que a feature do experimento toca estão no estado final.

- **FIX-008 — CI aceita `develop` no gate de convenção de branch.** PR #126 mergeado. Destrava o PR de sincronização `develop → integration`, que o gate rejeitava. **Executada pelo planner sob autorização explícita, sem Reviewer** — desvio registrado no status; `territorio: excecao-autorizada`.
  - Débito herdado: a faixa `[a-z0-9]` da regex é **sensível a locale** — sob `LC_COLLATE=en_US`, `feature/QA-013` é **aceita**. Não introduzido pela task; registrado no global.

### 🟢 Derivado das tasks, já materializado

- **ADR 0021 — gate de mutation testing opcional por task.** Critério: `test strength` (mortos ÷ **cobertos**) ≥ **80%**, medido **apenas sobre as classes alteradas pela task**; código pré-existente fica fora do denominador. Sobrevivente classificado como equivalente **com demonstração escrita** não conta contra o piso.
- **Item 11 do `BACKLOG-evolucao-workflow`** (mudanças em `.claude/`) — commit `105d955`, 2026-08-12. Regra de asserção sobre valor na `writing-java-unit-tests`; consistência rótulo × número na `artifact-report-contract`; campo `mutation_gate` no planner/backend/reviewer e nos dois templates de plano; remoção das referências penduradas a `.github/instructions/`.
  - ⚠️ **Config de agente é carregada no spawn** — isso só vale a partir da próxima sessão.
- **`.codex/` deletado pelo humano em 2026-08-13** ("nem uso o codex"), 8 definições de agente fora do repo. `.claude/agents/` é hoje o **único** conjunto de definições. Resíduo: **nenhum agente instruía a medir cobertura** — `.claude/agents/qa-test-specialist.md` nunca mencionou JaCoCo.
  - **Parte 1 aplicada em 2026-08-16** (`96dcbb7`): o `qa-test-specialist` ganhou a seção `Coverage Measurement`, com comando completo, os dois modos de falha silenciosa e a regra de reportar linha **e** branch juntas. O `reviewer` passou a auditar a cobertura reportada (`a99e3e4`).
  - **Parte 2 continua aberta e é decisão do humano:** passo equivalente no `.claude/agents/backend.md` **ou** política de `qa_required: true` sempre que a task tocar classe de produção. Enquanto não fechar, task com `qa_required: false` segue sem medir nada. Detalhe em `docs/sprints/04-instrumentacao-qualidade/pendencias-tecnicas.md`.

- **ADR 0022 — via de entrega declarada para skills em subagentes.** Fecha a auditoria de alcance de skills (2026-08-16, commits `36459bd`, `e8eb931`, `fb5f24b`). O achado: **`skills:` no frontmatter é pré-carga, não permissão** — quem barra o acesso é a ausência da ferramenta `Skill` em `tools:`. Três agentes tinham prosa mandando usar skill que eles **não alcançavam**; a falha é silenciosa, o agente roda e produz saída plausível sem nunca aplicar a skill.
  - Corrigidos: `backend` (preload das duas skills de Java, `ba8f12e`), `planner` e `ai-engineer` (ganharam a ferramenta `Skill`, `36459bd`).
  - `validate_agent.py` da skill `creating-agents` agora **detecta skill inalcançável**, para o defeito não voltar sem aviso.
  - 🔴 **O risco que estava "em aberto" foi CONFIRMADO em runtime pela QA-015, no mesmo dia.** O path relativo dos assets (`references/…`) resolve contra o **diretório de trabalho da sessão**, não contra o diretório da skill: o path que o próprio `SKILL.md` publica **falha**, o absoluto funciona — provado com par falha/sucesso sobre o **mesmo arquivo existente**. É onde mora a maior parte do conteúdo das skills de Java (**todos os exemplos de código**; 10 links numa, 5 na outra). O agente recebe a política **sem os padrões**, em silêncio. Correção candidata: âncora explícita (`${CLAUDE_SKILL_DIR}` ou path da raiz) — ⚠️ **mexe em `.claude/`, aguardando autorização do humano.** Registrado no débito global.

### 🔵 Próximo a refinar

- **Itens #9 e #10 — planejados como `BE-031`** (2026-08-16), `aguardando-decisao-humana`. **É o único item da sprint com plano pronto, e a dependência da QA-015 já caiu.** (#9) A mensagem de erro dentro de `PaymentRequestStrategy.parsePedido` é **inalcançável** — o usuário sempre vê a genérica do dispatcher (`MensagemEntranteService:60-63`); confirmado no fonte pelo Reviewer da QA-015. (#10) O upload ao S3 acontece **antes** da validação e **dentro** da `@Transactional`: se a persistência falhar, o rollback não desfaz o objeto no bucket. Fecha o #9 inteiro e o #10 **em parte** — o órfão por rollback fica para ADR.
  - **O que trava:** `mutation_gate: pendente`. Adotar o gate sobre `MensagemEntranteService` exigiria ampliar `targetClasses` no `pom.xml`, congelado pela QA-012. **Pergunta em aberto com o humano.**
  - A QA-015 mostrou que o `throw` do #9 **agora está protegido por teste de contrato** — o que muda o custo de mexer nele: remover a redundância deixou de ser mudança sem rede.
- **Item #8 — gate da convenção `*IntegrationTest`.** A QA-013 trouxe **segunda evidência a favor do ArchUnit**: nenhuma das três ferramentas da sprint mede conformidade arquitetural. Decisão ArchUnit × reflection puro segue com o humano.
- **Ferramental de qualidade está completo** — PIT, PMD e JaCoCo instalados, com baseline congelado em cada. O que resta da sprint é instrumentação de medição e configuração de agentes.
- Demais frentes não refinadas: mecanismo de "classes tocadas" (#7), hooks de coleta de custo, pinagem de modelo dos agentes.

> ⚠️ **Lição da QA-014, que continua valendo:** uma afirmação do plano ("não existe X no repositório") foi publicada com base em `grep` de **três diretórios** e derrubada por `git grep` na árvore inteira — que devolvia 77 linhas. **Verificação de escopo amplo exige busca de escopo amplo.** O que a derrubou foi o `.codex/agents/`, um segundo conjunto de definições de agente que o planner não sabia existir; **o `.codex/` foi deletado pelo humano em 2026-08-13** e hoje `.claude/agents/` é o único. A lição de método sobrevive ao fato que a originou.

### Decisão pendente da sprint

- **Ordem em relação à tag do marco zero.** Definir se a sprint inteira precede a tag do baseline do experimento, ou só as tasks que tocam código de produção. (A dependência antiga do FIX-006 **caiu** — ver "Deploy" abaixo. O `README.md` do experimento, item 6, ainda registra a versão antiga e precisa ser corrigido.)
  - **Resolvido em parte, 2026-08-16:** o humano autorizou o **item #2 antes da tag**. O escopo geral — se o resto da sprint também precede — segue em aberto.
  - **A QA-015 fechou e a tag está tecnicamente desbloqueada** — as duas classes que a feature do experimento toca estão no estado final. A decisão que resta é de escopo, não de dependência: **criar a tag agora**, ou esperar a BE-031, que toca `PaymentRequestStrategy` (a mesma classe) e reabriria a questão.

- **Autorizar a correção dos paths dos assets nas skills** (`.claude/skills/*/SKILL.md`, 15 links). Débito confirmado em runtime pela QA-015, correção de baixo esforço, **território do humano**.

- **Regra do campo `lint` no `PRE-MERGE-CHECKLIST`** para "tocou Java, mas fora do escopo configurado do linter". Hoje cai em `na`, que passa a significar duas coisas. **Ação do planner**, aguardando só a decisão da regra.

- **Parte 2 do débito de cobertura:** passo no `.claude/agents/backend.md` **ou** política de `qa_required: true` para task que toca produção.

- **`mutation_gate` da BE-031** — adotar exigiria ampliar o `targetClasses` congelado.

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
- **develop → integration (sync):** planner abre **durante** a sprint sempre que publicar em `develop` algo que o implementador precisa enxergar — plano de task, ADR, mudança de agente ou de template. Auto-aceitável: `develop` sempre contém a integration, então não há divergência possível. Último: **PR #132**, 2026-08-16.
- **fix/hotfix → develop:** PR direto.
- Última task usada por prefixo: **QA-015**, **BE-030** (a BE-031 tem plano, ainda não executada), **FE-017**, **FIX-008**.

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
