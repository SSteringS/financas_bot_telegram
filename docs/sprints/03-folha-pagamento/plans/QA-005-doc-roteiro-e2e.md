---
task: QA-005
titulo: "Doc docs/runbooks/ROTEIRO-E2E.md"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-01
branch_alvo: null
integration_branch: null
prioridade: media
esforco: baixo
territorio: plan
estado: pronto-pra-execucao
depende_de: []
bloqueia: []
skills_dispatched: []
exige_e2e_full: false
lote: A
fluxos_qa: []
---

# QA-005 — Doc `docs/runbooks/ROTEIRO-E2E.md`

## Intake

- **Origem:** spec QA Fase 1 §4 (QA-005). Lote A — independente. **Território: plan (planner executa diretamente em `develop`).**
- **Por quê agora:** sem o roteiro, o desenvolvedor não sabe como rodar a suíte. Pode ir em paralelo com as tasks de implementação.
- **Esforço:** baixo (~1h de escrita).

---

## Escopo

Criar `docs/runbooks/ROTEIRO-E2E.md` cobrindo 7 seções (conforme spec §4.QA-005):

1. **Pré-requisitos** — MySQL local rodando, `.env.e2e` preenchido, Java 21, Node 20+. Docker NÃO necessário.
2. **Como rodar** — `cd frontend && npm run e2e:full`.
3. **Como interpretar resultado** — relatório HTML, trace viewer, screenshot/video em falhas.
4. **Troubleshooting** — MySQL não rodando, `.env.e2e` ausente, porta ocupada, suíte travada em healthcheck.
5. **Relação com ROTEIRO-INTEGRACAO-FRONT-BACK.md** — este runbook substitui o manual para cenários cobertos; manual permanece como fallback e para Camada 5 (Telegram real, ngrok).
6. **Como adicionar cenário novo** — apontar tabela parametrizada de `webhook-cenarios.spec.ts`.
7. **Aviso sobre dados** — cleanup limpa SOMENTE `requisitante_id=99`. NUNCA rodar com DB de produção.

---

## Critérios de aceitação

- [ ] Arquivo `docs/runbooks/ROTEIRO-E2E.md` criado com as 7 seções.
- [ ] Referenciado no PRE-MERGE-CHECKLIST (relacionado com QA-006).
- [ ] `exige_e2e_full: false` no frontmatter desta task.

---

## Definição de pronto

Planner commita direto em `develop`. Sem PR, sem Reviewer (território exclusivo do planner).

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md` §4 (QA-005)
- `docs/architecture/desenho-testes-automatizados.md` §14.2
