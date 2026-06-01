# STATE — onde o projeto está agora

> **Doc vivo de orientação.** Existe pra uma sessão que começa fria (planner, back, front ou reviewer) se situar em 1 minuto, sem re-derivar contexto. **Curto de propósito.** Detalhe mora nos planos (`docs/plans/`), status reports (`docs/sprints/<NN>/status/`) e ADRs (`docs/decisions/`).
>
> **Última atualização:** 2026-05-31 (sprint 02b kaizen **fechada** — 7/8 WF tasks concluídas, 16 skills, ADR 0015; RETRO-02b publicada; **sprint 03 a abrir**).
> **Fonte:** este resumo é derivado dos status reports em `docs/sprints/<NN>/status/` e `docs/sprints/02-canal-whatsapp/status/`. O **estado real de merge em `develop` é do humano** (ele é o integrador — ADR 0004). Quando um status diz "aguardando revisão / não mergeado", está marcado abaixo.

---

## Sprint atual

**Sprint 03 — Folha de pagamento** (`docs/sprints/03-folha-pagamento/README.md`).

**Modo:** 🟡 **em planejamento** — 10 tasks planejadas (BE-023..BE-029, FE-015..FE-017). Spec técnica e ADR 0016 (`Proposed`) disponíveis. Decisão pendente §7 (vales na lista do Pedro) não bloqueia a maioria das tasks.

**Sprint 02b — Kaizen (workflow/processo)** ✅ **fechada** em 2026-05-31. Retro em `docs/retrospectivas/RETRO-02b-kaizen-workflow.md`. Push de 4 commits locais pendente (humano executa).

**Sprint 02 — Canal WhatsApp** ✅ **fechada** em 2026-05-30 com FE-14 mergeado (PR #80). Retro em `docs/retrospectivas/RETRO-02-canal-whatsapp.md`. Falta abrir PR `develop → main` pro deploy (não bloqueante).

**Escopo revisado em 2026-05-29:** *código completo* do canal WhatsApp em prod (com endpoints inertes por sentinela), observability externalizada, e UX upgrades. **"WhatsApp vivo em prod"** (smoke E2E real contra Meta) **saiu pra próxima sprint** quando o chip dedicado + Business Verification estiverem disponíveis (bloqueio externo).

A Fase 3 (MVP de visualização) está **concluída** desde 2026-05-27 — histórico em `docs/sprints/01-mvp/` e bloco histórico do `docs/plans/BACKLOG-produto.md`.

---

## Onde estamos na sprint 02

### Mergeado em `develop`

- **DEP-09** Observability infra (CloudWatch agent + log metric filter `finbot/app/errors`): ✅
- **BE-17** Refactor porta agnóstica de entrada (`MensagemEntrantePortIn`): ✅
- **BE-18** Adapter de saída WhatsApp (sender + downloader Graph API): ✅
- **BE-19** Adapter de entrada WhatsApp (handshake GET + POST HMAC + mapper + exception handler) — PR #76: ✅
- **BE-19a** Idempotência via tabela `mensagem_processada` (cobre `wamid`): ✅
- **BE-21a** Eventos + notificador roteando por `canalPreferido`: ✅
- **BE-22** Micrometer + CloudWatch custom metrics (~$7/mês) — PR #77: ✅
- **FIX-001-whatsapp-defaults-deploy-safe** — `@Value` defensivos pros 3 secrets WhatsApp — PR #78 (2026-05-30): ✅ **primeiro FIX no padrão novo zero-padded.**
- **BE-17b-renomear-rota-telegram** — `/webhook` → `/webhook/telegram` + `setWebhook` manual coordenado — PR #79 (2026-05-30): ✅
- **FIX-padronizar-restclient-builder** (legacy slug): ✅
- **FIX-idempotencia-porta-application** (legacy slug — porta extraída pra `application/port/out/`): ✅

### Pronto pra dispatch (planos escritos, aguardando despacho)

- **FE-14-botao-ver-arquivo-original** — botão menor destaque no `PedidoCard` pra abrir foto/PDF original. Front; território disjunto do back. **Única task de produto pendente da sprint 02.**

### Parqueado (entra na sprint vigente quando o chip chegar)

- **BE-20-deploy-whatsapp-smoke** — deploy WhatsApp + config webhook na Meta + smoke E2E. Plano escrito; bloqueado por (a) chip dedicado WhatsApp Business não-comprável imediatamente; (b) Test number Meta sofre restrição BR 130497 (Business não-verificada não envia business-initiated). Ver `docs/aprendizado/whatsapp-restricoes-pais-business-nao-verificada.md`.
- **BE-21b** — `WhatsAppNotificadorImpl` + templates de utilidade Meta. Sem plano ainda. Depende de BE-20.
- **EVO-02 completa** — migrar `canalPreferido` do Pedro pra WHATSAPP. Sem plano. Depende de BE-21b.
- **PREP-WA** fases 4 (Business Verification — lead time externo), 8 (configurar webhook Meta), 9 (smoke com chip).

### Pendências de produto registradas

- **EVO-09 — folha de pagamento** (registrada em `docs/plans/BACKLOG-produto.md` por iniciativa do humano em 2026-05-29). 8 perguntas abertas pro PO, sub-frentes identificadas. Entra no escopo de planejamento da sprint 03.

---

## Próximos passos prováveis

1. **Despachar FE-14** pro front (única task de produto pendente na sprint 02).
2. **Abrir PR `develop → main`** pra deploy em prod do canal WhatsApp inerte + BE-17b (rota Telegram simétrica) + BE-22 (Micrometer) + FIX-001 (defaults defensivos). **Já desbloqueado** pelos merges de hoje.
3. **RETRO-02** — escrever retrospectiva da sprint 02 incluindo itens #8/#9/#10 do `docs/plans/BACKLOG-evolucao-workflow.md` + lições aprendidas (sync gremlin do OneDrive, dois worktrees, convenção FIX-NNN).
4. **Abrir sprint 03** — decidir escopo: EVO-09 (folha de pagamento — refinar com PO/arquiteto), BE-22b/c/d (alarmes + timers), itens de workflow não cobertos na retro. Estrutura nova: `docs/sprints/03-<slug>/`.

---

## Pendências abertas (não bloqueantes)

- **DEP-07:** PR #64 aberto, aguarda merge + `terraform apply` (in-place confirmado — sem risco à EC2 atual).
- **DEP-08:** webhook via Caddy/LE — plano escrito, Fase 2 precisa de ADR de topologia TLS. `docs/PENDENCIAS-TECNICAS.md`.
- **Ícones PWA:** placeholders em `frontend/public/` — trocar por arte real.
- Demais débitos: `docs/PENDENCIAS-TECNICAS.md`.

---

## Workflow / processo (meta, não-produto)

Backlog vivo em `docs/plans/BACKLOG-evolucao-workflow.md`. Estado dos itens em voo:

- **FIX-gitattributes-eol:** ✅ mergeado.
- **CI-01** (gate de CI no caminho pra `develop`): ✅ mergeado. Branch protection adiada (decisão do humano) — gate informativo, não bloqueante.
- **Codegen do `tipos.ts`** (FE-13): ✅ mergeado.
- **Script de métricas** dos status reports: ✅ `docs/scripts/metricas_status.py`. **Regex de task-id** aceita `CI-`: ✅.
- **Reviewer:** **adotado pra toda task (2026-05-26)** — revisão independente em sessão separada antes do merge (ADR 0005). Já em uso.
- **Convenção FIX-NNN / HOTFIX-NNN zero-padded:** ✅ adotada forward-only desde 2026-05-29 (CLAUDE.md). FIX-001 inaugurou.
- **Dois worktrees git compartilhando `.git/`:** ✅ adotado (planner em worktree dedicado fixo em `develop`, implementador no worktree principal). Documentado em CLAUDE.md.
- **Governança:** ADR 0004 (taxonomia), ADR 0005 (sessões por papel). Retroativos: ADR 0006 (hostnames), ADR 0007 (reporting com gates), ADR 0008 (Terraform módulo único). ADR 0009 (provisionamento da EC2 codificado).

---

## Mapa rápido de onde mora o quê

| Preciso de… | Vou em… |
|---|---|
| O que construir (spec de task) | `docs/plans/` |
| O que foi feito (execução) | `docs/sprints/<NN>/status/` |
| Decisão arquitetural canônica | `docs/decisions/` (ADRs) |
| Regra que o agente obedece | `CLAUDE.md` |
| Conceito pra revisitar | `docs/aprendizado/` |
| Definição de pronto / gates | `docs/runbooks/PRE-MERGE-CHECKLIST.md` |
| Instruções por papel | `docs/roles/` |
| Débito técnico conhecido | `docs/PENDENCIAS-TECNICAS.md` |
