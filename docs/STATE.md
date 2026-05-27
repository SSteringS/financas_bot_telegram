# STATE — onde o projeto está agora

> **Doc vivo de orientação.** Existe pra uma sessão que começa fria (planner, back, front ou reviewer) se situar em 1 minuto, sem re-derivar contexto. **Curto de propósito.** Detalhe mora nos planos (`docs/plans/`), status reports (`docs/status/`) e ADRs (`docs/decisions/`).
>
> **Última atualização:** 2026-05-27 (pelo reviewer — pós DEP-06 E2E).
> **Fonte:** este resumo é derivado dos status reports em `docs/status/`. O **estado real de merge em `develop` é do humano** (ele é o integrador — ADR 0004). Quando um status diz "aguardando revisão / não mergeado", está marcado abaixo.

---

## Fase atual

**Fase 3 — Camada de visualização** (plano-mãe: `docs/plans/FASE-3-VISUALIZACAO.md`).
Adiciona uma camada de leitura pro requisitante (Pedro): API REST + front React (PWA) + deploy em AWS, com auth por link mágico. A operação do bot de Telegram continua igual.

Sub-fases: **3a Backend** → **3b Front** → **3c Deploy** → **3d Evolução pós-MVP** (não fazer agora).

---

## Onde estamos

### 3a — Backend: **concluída** (conforme status reports)
API REST completa: DTOs+OpenAPI (BE-04), listagem/detalhe/resumo (BE-05/06/09), pre-signed URL + endpoints de imagem (BE-07/08), auth completa (token admin → exchange JWT → filtro → CORS, BE-10 a BE-13), testes de integração com Testcontainers (BE-14), handler genérico de exceções do webhook (BE-15, gate de deploy), e o resumo parametrizado por mês/busca (BE-16). Base (Flyway, refatoração de persistência, backfill de testes) em BE-00/00B/01a. Polish + EVO-07 (aceitar document/PDF) na branch `feature/backend-polish-evo07` (212 testes, BUILD SUCCESS).

### 3b — Front: **concluída** (conforme status reports)
Fase 3b inteira (FE-03 a FE-11) na branch `feature/frontend-fase3-completa`. Em cima dela, **FE-12** corrigiu os dois bugs da revisão (contadores que colapsavam ao filtrar; header travado no mês corrente) ligando o front ao contrato `mes`/`busca`/`todos` da BE-16. **FE-12 estava como "aguardando revisão, não mergeado" no seu último status** — confirmar com o humano se já entrou em `develop`.

### 3c — Deploy: **concluída** ✅ (2026-05-27)

E2E completo validado em produção: link mágico → JWT → cookie → listagem de pedidos. Todos os serviços no ar.

- **DEP-01** Route 53 + ACM: ✅
- **DEP-02** S3 + CloudFront do front (`satyansaita.com`): ✅
- **DEP-03** `api.satyansaita.com` via Caddy/Let's Encrypt: ✅
- **DEP-04** Pipeline GitHub Actions (OIDC → S3 sync → CloudFront): ✅ pipeline rodou verde (2026-05-27)
- **DEP-05** CORS + cookie `Domain=satyansaita.com` em prod: ✅ deployado (2026-05-27)
- **DEP-06** E2E em produção: ✅ todos os cenários passaram (2026-05-27)
- **DEP-07** Bootstrap idempotente EC2 (`bootstrap.sh` + `user_data`): PR #64 aberto — aguarda merge + `terraform apply`. Sem urgência (instância atual não afetada — ADR 0009).
- **DEP-08** Webhook → Caddy/LE + aposentar keystore: 📝 plano escrito (`docs/plans/DEP-08-webhook-https-caddy.md`). Fase 2 precisa de ADR de topologia TLS. Item de 3d.

---

## Próximos passos prováveis

1. **DEP-07:** merge PR #64 + `terraform apply` (sem urgência, EC2 atual não afetada).
2. **3d — Evolução pós-MVP:** ver backlog em `docs/plans/BACKLOG-evolucao-workflow.md`. Próximo item relevante: DEP-08 (aposentar keystore self-signed).
3. **Ícones do PWA** ainda são placeholders — trocar antes de divulgar o app.

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
- **Governança:** ADR 0004 (taxonomia), ADR 0005 (sessões por papel). Retroativos: ADR 0006 (hostnames), ADR 0007 (reporting com gates), ADR 0008 (Terraform módulo único). ADR 0009 (provisionamento da EC2 codificado).

---

## Mapa rápido de onde mora o quê

| Preciso de… | Vou em… |
|---|---|
| O que construir (spec de task) | `docs/plans/` |
| O que foi feito (execução) | `docs/status/` |
| Decisão arquitetural canônica | `docs/decisions/` (ADRs) |
| Regra que o agente obedece | `CLAUDE.md` |
| Conceito pra revisitar | `docs/aprendizado/` |
| Definição de pronto / gates | `docs/runbooks/PRE-MERGE-CHECKLIST.md` |
| Instruções por papel | `docs/roles/` |
| Débito técnico conhecido | `docs/PENDENCIAS-TECNICAS.md` |
</content>
</invoke>
