# Retrospectiva 02 — Sprint canal WhatsApp + observability + UX

**Data:** 2026-05-30
**Período coberto:** Sprint 02 (2026-05-27 → 2026-05-30, ~4 dias úteis com overnight sessions e dispatches single-task).
**Resultado:** ✅ **Código completo do canal WhatsApp em prod (inerte), observability externalizada, UX upgrade entregue.** A operação do bot Telegram seguiu sem interrupção. "WhatsApp vivo em prod" (smoke E2E real contra Meta) **saiu de escopo** em 2026-05-29 por bloqueio externo (chip dedicado + Business Verification) — re-escopado, não falhado.

Esta é a **segunda** retro do projeto. A primeira (`RETRO-01`) ficou em 2026-05-27 fechando o MVP; as ações dela são checadas na seção 7 abaixo.

---

## 1. O que foi entregue

Sprint 02 fechou **11 tasks de produto** + **1 task de processo** (CI-01 já vinha rolando). Em ordem aproximada de merge:

- **DEP-09** Observability infra (CloudWatch agent + metric filter `finbot/app/errors` + alarmes disk/CPU/error_rate; logback JSON em prod com rotação 7d).
- **BE-17** Refactor porta agnóstica de entrada (`MensagemEntrantePortIn`) — fundação multi-canal.
- **BE-18** Adapter de saída WhatsApp (sender + downloader Graph API, RestClient padronizado).
- **BE-19** Adapter de entrada WhatsApp (handshake GET + POST HMAC + mapper + exception handler) — **PR #76**.
- **BE-19a** Idempotência via tabela `mensagem_processada` (cobre `wamid`).
- **BE-21a** Eventos in-process + notificador roteando por `canalPreferido`.
- **BE-22** Micrometer + CloudWatch custom metrics (~$7/mês) — **PR #77**.
- **BE-17b** Renomear rota Telegram `/webhook` → `/webhook/telegram` (simetria com WhatsApp) — **PR #79**.
- **FIX-001** `@Value` defaults defensivos pros 3 secrets WhatsApp (sentinela `NAO_CONFIGURADO`) — destrava deploy sem chip — **PR #78**. Inaugurou o padrão `FIX-NNN` zero-padded.
- **FIX-padronizar-restclient-builder** + **FIX-idempotencia-porta-application** (legacy slug).
- **FE-14** Botão "ver foto/PDF original" no `PedidoCard` — **PR #80**. Generalizou `ModalArquivo` (refactor leve, +18 testes).

**ADRs registradas (5 novas):** `0010` organização doc por sprint, `0011` adoção do papel arquiteto, `0012` provider WhatsApp Cloud API oficial, `0013` estratégia multi-canal adapters, `0014` notificações via eventos in-process. Densidade técnica subiu.

**Parqueado por bloqueio externo:** BE-20 (deploy + smoke E2E), BE-21b (`WhatsAppNotificadorImpl` + templates Meta), EVO-02 completa (migrar `canalPreferido` do Pedro pra `WHATSAPP`). Saem do gelo quando chip dedicado + Business Verification estiverem prontos.

---

## 2. Dados (medidos pelo `metricas_status.py`)

| Métrica | RETRO-01 | RETRO-02 | Δ |
|---|---|---|---|
| Status reports totais | 43 | **55** | +12 (sprint 02 + DEP-07 ressurgiu) |
| Cobertura schema canônico | 19/43 (44%) | **31/55 (56%)** | **+12 pp** |
| Reports com chave `estado` | 8 | **20** | **2.5×** |
| Reports sem frontmatter (legado) | 24 | **24** | igual — todos pré-RETRO-01; nenhum report novo entrou fora do schema |
| ADRs totais | 7 (0003–0009) | **12** (até 0014) | +5 |
| Aprendizados | 23 | **28** | +5 (whatsapp-modelo-mensagens, whatsapp-restricoes-pais, cloudwatch-metric-dimensions, observability-logs-externalizar, eventos-in-process-spring) |
| Tasks concluídas com status report | 10 | **22** | mais do que dobrou |
| Gate-fails (`gates: fail`) | n/a | **0** | sem regressão |
| Desvios totais | 4 (3 back, 1 front) | **12** (11 back, 1 front) | volume maior porque o volume de tasks também foi maior |
| Pendências humano | n/a | **0** | nada acumulado |
| Soma `testes_total` reportada | ~495 | **3176** | reflete volume agregado entre fases |
| Soma `testes_novos` reportada | n/a | **106** | só do que entrou em sprints novas |

> **Caveat das métricas:** o script `metricas_status.py` foi corrigido nesta retro pra varrer `docs/sprints/<NN>/status/` (era um bug — só via `docs/status/`). Cobertura "real" só ficou visível agora. Provavelmente as métricas da RETRO-01 também estavam subnotificadas — número de comparação serve, magnitude vale.

Entrega aconteceu via mix de **overnight sessions** (2 noites, 5 tasks de back) + **dispatches single-task** (BE-17b, BE-22, FIX-001, FE-14) + **fixes oportunistas** (FIX-padronizar-restclient, FIX-idempotencia, FIX-001 com convenção nova).

---

## 3. O que foi bem (continuar)

- **Reviewer em toda task funcionou** (ação #1 da RETRO-01 cumprida — ADR 0005 adotada 2026-05-26). Os PRs entraram em `develop` sempre após sessão separada de revisão; zero gate-fail nos reports. Acoplado a isso, status reports agora têm campos `desvios` e `pendencias_humano` preenchidos — output contract funcionando.
- **Overnight sessions continuaram entregando volume previsível**: 5 tasks de back em 2 noites (DEP-09 + BE-17 na primeira; BE-19a + BE-18 + BE-21a na segunda), com RESUMO no fim de cada sessão. Modelo ficou consolidado.
- **Adoção do papel arquiteto (ADR 0011)** trouxe profundidade nas decisões técnicas do WhatsApp — ADRs `0012` (provider), `0013` (multi-canal adapters) e `0014` (eventos in-process) foram refinadas antes de virar código. Isso evitou a refatoração de meio de caminho que assombrou a fase 3.
- **Re-escopo em tempo real (2026-05-29)** quando o bloqueio externo (chip + Business Verification BR 130497) ficou claro — sprint pivoteou pra "código completo em prod inerte" em vez de travar. **Parquear ≠ falhar.** Documentado em `docs/aprendizado/whatsapp-restricoes-pais-business-nao-verificada.md`.
- **Convenção `FIX-NNN` zero-padded forward-only** (CLAUDE.md §Fluxo de branches) — inaugurada com FIX-001. Rastreabilidade subiu sem renomear histórico (que sempre custa caro).
- **ADR 0010 (organização por sprint)** deu estrutura nova: `docs/sprints/<NN>/{plans,status,avaliacoes}/` reflete o ciclo, não o catálogo plano. Material da sprint fica colocalizado.
- **Observability não-reativa**: DEP-09 + BE-22 entregaram alarmes + métricas custom **antes** de ter prod WhatsApp vivo. Inverteu o padrão da RETRO-01 ("infra reativa"). Quando o smoke acontecer, já tem instrumentação.
- **Códigos do MVP foram reusados na FE-14** (modal generalizado em `ModalArquivo`) — refator leve, sem duplicação, +18 testes. Sinal de que o front consegue evoluir sem reescrita.

---

## 4. O que pode melhorar

- **Sync gremlin do OneDrive voltou — e ainda morde.** `CLAUDE.md`, `STATE.md`, `BACKLOG-evolucao-workflow.md` e o resumo do EVO-09 chegaram a essa retro **truncados no disco**. A ação #6 da RETRO-01 ("confiabilidade do `STATE.md`") foi só **mitigada** com worktree dedicado pro planner (CLAUDE.md §Worktrees git) — não resolvida. Os 3 commits "documentação recuperada para ser puxada" de 2026-05-30 são prova material: estávamos recuperando pedaços perdidos. **Recorrência clara, vira ação #1 abaixo.**
- **Edit/Write tool do Cowork truncou arquivos em escrita** durante esta sessão (CLAUDE.md original da sessão de planejamento; `metricas_status.py` na primeira tentativa). Bash via `cat > arquivo << EOF` ficou como caminho confiável. **Não é só OneDrive — é interação Cowork ↔ mount.** Hoje contornado caso a caso.
- **Mount FUSE do Cowork não deleta arquivos** (`Operation not permitted` em `rm`/`unlink`) — sobraram `.tmp` e `DISPATCH-FIX-001` lixo, mais um `index.lock` órfão que travou `git add`. Workaround: humano deletou pelo Windows. Funciona, mas vira fricção a cada sessão.
- **ADR 0010 ficou pela metade.** Migração `docs/status/ → docs/sprints/01-mvp/status/` foi feita, mas `docs/status/` continua intacta com **as mesmas 46 cópias** + `DEP-07.md` que ficou pra trás. Sem o fix no script de métricas, contava tudo 2×. Decidir: completar a migração (apagar `docs/status/`) ou manter ambas com regra clara de "onde mora o quê".
- **Métricas só ficaram corretas hoje.** Script só via `docs/status/`; ignorava qualquer report de sprint 02 e 01-mvp. **Métrica errada por 3 dias.** Métrica que não capta entrega nova vira ruído. Bug consertado nesta retro; vale registrar como aprendizado.
- **Branch protection ainda não ligada.** Decisão 2026-05-26 (CLAUDE.md): CI-01 roda mas é **informativo, não bloqueante**. Vale considerar virar bloqueante agora que a disciplina existe.
- **Workflow #8/#9/#10 surgiram durante a sprint e foram empilhados pra retro** em vez de tratados na hora. Backlog viu acumular 3 itens grandes (`checklist arquitetural reviewer`, `zero-padded geral`, `roles × skills × workflows`). Isso motivou a sprint kaizen entre 02 e 03 — mas é sinal de que tarefas de processo precisam de janela própria pra não competir com produto.
- **Pendências de humano-no-loop em DEP-09** (instalar CloudWatch agent via SSH, copiar config, criar diretório de log) ficaram fora do report como "responsabilidade do humano" — o script conta `pendencias_humano: 0`, mas elas existem fora do schema. Vale formalizar (ou colocar `pendencias_humano: <n>` quando há setup manual de infra).
- **EVO-09 estava cortado em "Su" desde o primeiro commit** — o EVO-09 que entrou em 2026-05-29 nunca foi completo. Não é regressão da sprint 02; é dívida da pendência registrada. Discovery refeito nesta retro (BACKLOG-produto.md atualizado).

---

## 5. Aprendizados-chave do ciclo

Em `docs/aprendizado/` (novos desde RETRO-01):

- **`whatsapp-restricoes-pais-business-nao-verificada.md`** — restrição BR 130497 da Meta. **Razão do parqueamento.** Material direto pra próxima sprint onde "WhatsApp vivo" voltar.
- **`whatsapp-modelo-mensagens.md`** — modelo de mensagens utility/marketing/conversation + janela 24h. Base pra BE-21b.
- **`observability-logs-externalizar.md`** — DEP-09. Por que CloudWatch agent + log metric filter > SSH-and-grep.
- **`cloudwatch-metric-dimensions.md`** — BE-22. Dimensões custam $; cardinalidade explode rápido se não controlada.
- **`eventos-in-process-spring.md`** — ADR 0014. Eventos como acoplamento fraco entre BE-21a notificador e o caminho hot do webhook.

A biblioteca está chegando em **28 arquivos** — começou a aparecer como **base de consulta de meio de caminho** (ex.: a sessão anterior consultou `cookies-samesite.md` ao revisar a auth do front). Vale agrupar por tema no `README.md` da pasta (índice).

---

## 6. Incidentes e como foram resolvidos

| Incidente | Causa | Resolução |
|---|---|---|
| `STATE.md` / `CLAUDE.md` / `BACKLOG-evolucao-workflow` chegaram truncados | Sync gremlin do OneDrive (recorrência da RETRO-01 #6) | Worktree dedicado pro planner (CLAUDE.md §Worktrees git); reconstrução manual via bash nesta sessão |
| `index.lock` órfão travou `git add` durante a sessão | `git add` cria lock, mount FUSE do Cowork não deleta no rollback | Humano apagou pelo Windows; tentar de novo |
| Edit/Write tool truncou `metricas_status.py` no meio do patch | Bug Cowork ↔ mount; trunca writes médios | Reescrita via bash heredoc; padrão pra escritas críticas |
| EVO-09 cortado em "Su" desde o primeiro commit | Falha antiga, não-detectada por ninguém | Discovery refeito nesta retro |
| Restrição BR 130497 da Meta (Test number não envia business-initiated) | Política externa da Meta; só descoberta no levantamento da BE-20 | Parqueamento WhatsApp vivo + ADR sobre estratégia de Business Verification |
| Convenção `FIX-NNN` zero-padded retroativa explodiria escopo | Há FIXes legados sem ID numérico | Convenção **forward-only** desde 2026-05-29; FIXes anteriores ficam com slug |
| Script de métricas só via `docs/status/`, ignorando sprints | Default hard-coded num diretório só | Corrigido nesta retro (varre `docs/sprints/*/status/` + dedup por nome) |

---

## 7. Status das ações da RETRO-01

| # | Ação | Status |
|---|---|---|
| 1 | Adotar Reviewer desde o início | ✅ **Cumprida** (ADR 0005, obrigatório desde 2026-05-26; visível nos PRs da sprint 02) |
| 2 | Status report novo no schema canônico + rodar `metricas_status.py` | 🟢 **Parcial** — todos os reports novos entraram no schema; rodada da métrica foi **adiada até esta retro** (e ainda revelou o bug do script). |
| 3 | Single source of truth pra config crítica | 🟢 **Parcial** — codegen `tipos.ts` (FE-13) mergeado; estender pra URLs/config do back continua aberto. |
| 4 | Observabilidade mínima (Actuator + alarme básico) | ✅ **Cumprida e ampliada** — DEP-09 (CloudWatch agent + alarmes) + BE-22 (Micrometer custom metrics). Pendente: BE-22b/c/d (alarmes adicionais, timers). |
| 5 | Rotação do token Telegram | ⏸ **Não tocada** — débito de segurança continua aberto. |
| 6 | Confiabilidade do `STATE.md` | 🟡 **Mitigada, não resolvida** — worktree dedicado reduziu mas não eliminou. Vira ação #1 desta retro. |
| 7 | Sistema de métricas (escolher 2-3 indicadores que mudam comportamento) | 🟢 **Avançou** — script existe + rodado nesta retro; métricas-chave consolidaram (`cobertura_schema`, `gate_fails`, `pendencias_humano`). Falta cadência (rodar fim de cada sprint). |

**Taxa de cumprimento:** 2 cumpridas (29%), 3 parciais (43%), 1 mitigada (14%), 1 não tocada (14%). Direção certa, ritmo aceitável.

---

## 8. Ações pra próxima etapa

> **Contexto:** próximo ciclo começa com uma **sprint kaizen** (workflow puro, intermediária entre 02 e 03) antes da sprint 03 (EVO-09 folha de pagamento, produto pesado). As ações abaixo se distribuem entre as duas.

| # | Ação | Quando | Dono sugerido |
|---|---|---|---|
| 1 | **Resolver o sync gremlin de forma estrutural** — investigar se Cowork+OneDrive tem fix conhecido; alternativas: pasta fora do OneDrive (`C:\src` direto), ou markdown via git em vez de doc vivo no FS | sprint kaizen | planner + humano |
| 2 | **Completar a migração ADR 0010** — decidir entre apagar `docs/status/` (todas as 46 duplicatas migradas) ou manter ambas com regra explícita; mover `DEP-07.md` pra `docs/sprints/01-mvp/status/`; documentar o destino do `_TEMPLATE.md` | sprint kaizen | planner |
| 3 | **#8 BACKLOG-evolucao-workflow** — checklist arquitetural explícito no `reviewer.md` (smells: violação de hexagonal, lógica de domínio no controller, exception handler escopado errado, anti-corruption ausente) | sprint kaizen | planner + Reviewer |
| 4 | **#9 BACKLOG-evolucao-workflow** — adoção do zero-padded geral (BE-NN, FE-NN, DEP-NN, EVO-NN, CI-NN, forward-only); atualizar regex do `PRE-MERGE-CHECKLIST.md` | sprint kaizen | planner |
| 5 | **#10 BACKLOG-evolucao-workflow** — separar config de agentes em `roles × skills × workflows`; extrair boilerplate dos DISPATCH/MASTER-PROMPT; deixar prompts magros | sprint kaizen | planner |
| 6 | **Rodar `metricas_status.py` ao fim de cada sprint** — ritual, não opcional; entra como gate do "fim de sprint" no checklist | a partir da kaizen | planner |
| 7 | **Ligar branch protection** (transformar CI-01 em bloqueante) — agora que a disciplina existe | sprint kaizen | humano |
| 8 | **Refinar EVO-09 com arquiteto** — fechar as 2 decisões pendentes (cálculo do pagamento, comando do bot pra vale) + ADR sobre `tipo`/`categoria` (`VALE`, `FOLHA`); virar 4–6 BE + 2–3 FE | sprint 03 (pré-código) | arquiteto + planner |
| 9 | **Formalizar `pendencias_humano` no schema** quando setup manual de infra fica pendente (DEP-09 deixou pendências fora do report) | sprint kaizen | planner |
| 10 | **Rotação do token Telegram** (carry-over RETRO-01 #5) | quando der | humano |
| 11 | **Estender SSoT pra config do back** (URLs, domínios, secret IDs centralizados) — carry-over RETRO-01 #3 | sprint 03 ou backlog | back + planner |

---

## 8a. Correção pós-retro (2026-05-30, após primeira escrita)

Durante o commit desta retro, descobriu-se que **`C:\\Users\\satya\\src\\` não está no OneDrive** — a coluna de status do OneDrive não aparece nessa pasta. A hipótese "sync gremlin do OneDrive" usada acima e em CLAUDE.md (§Worktrees git) está **parcialmente errada na causa**, embora o fenômeno (arquivos truncados no disco que viraram commits incompletos: `2c20dd9` "documentação recuperada") tenha sido real.

Causa correta provável (a investigar na ação #1 da sprint kaizen):
- **Cowork Write/Edit tools truncam writes médios** (confirmado nesta sessão duas vezes: CLAUDE.md original e `metricas_status.py` no patch). Bash via `cat > file << EOF` ficou como caminho confiável.
- **Mount FUSE do Cowork tem visão defasada do disco** (descoberto após o `git restore` do FE-14: terminal Windows mostrou `git status` clean, mas sandbox Linux ainda via 7 arquivos como "modified" com conteúdo truncado).

Implicação prática: a **ação #1** continua válida (resolver os truncamentos estruturalmente), mas o **escopo certo** é "investigar bug do Cowork Write/Edit + mount FUSE" — **não** "tirar do OneDrive". Atualizar CLAUDE.md §Worktrees git é candidato pra mesma ação.

---

## 9. Nota — sprint kaizen como inovação de processo

Entre RETRO-02 e sprint 03 (EVO-09, produto pesado), o ciclo entra numa **sprint kaizen** dedicada só a processo/tooling. **Por quê:** os itens #8/#9/#10 do `BACKLOG-evolucao-workflow.md` competem mal com sprint de produto — vivem sendo empilhados pra "depois". A sprint kaizen dá janela própria e curta, evita que melhorias de workflow virem dívida permanente. Se funcionar, vira padrão (kaizen entre sprints grandes); se virar overhead, repensar na RETRO-03.

---

> **Como ler esta retro no futuro:** ela é a primeira retro com **comparação direta com a anterior**. As métricas da seção 2 viraram referência pra RETRO-03; as ações da seção 8 são o que carregamos. O ciclo aprende quando a próxima retro mede honestamente o que esta prometeu.
