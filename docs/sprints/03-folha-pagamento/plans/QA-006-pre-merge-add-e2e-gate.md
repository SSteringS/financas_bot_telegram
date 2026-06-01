---
task: QA-006
titulo: "Item E2E no PRE-MERGE-CHECKLIST (gate condicional)"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-01
branch_alvo: null
integration_branch: null
prioridade: media
esforco: baixo
territorio: plan
estado: pronto-pra-execucao
depende_de: [QA-005]
bloqueia: []
skills_dispatched: []
exige_e2e_full: false
lote: A
---

# QA-006 — Item E2E no PRE-MERGE-CHECKLIST

## Intake

- **Origem:** spec QA Fase 1 §4 (QA-006). Lote A. **Território: plan (planner executa diretamente em `develop`).**
- **Por quê agora:** o checklist precisa conter o gate antes que qualquer task declare `exige_e2e_full: true` (o que acontecerá a partir da sprint 04, quando a infra estiver pronta).
- **Esforço:** baixo (~15-30 min).

---

## Escopo

Editar `docs/runbooks/PRE-MERGE-CHECKLIST.md` adicionando:

**Na seção do implementador (gate condicional, não bloqueante por padrão):**
```
- [ ] Se o plano declara `exige_e2e_full: true`: `npm run e2e:full` verde, e campo `e2e_full` preenchido no status report (ver `docs/architecture/desenho-testes-automatizados.md` §9.2 para schema).
```

**Na seção do Reviewer (auditoria, bloqueante se inconsistente):**
```
- [ ] Se `exige_e2e_full: true` no plano: confirmar que `e2e_full.executado: true` e `e2e_full.status: verde` no status report. Caso contrário, **rejeitar** com pendência bloqueante.
```

**Não atualizar** `_TEMPLATE-status.md` com bloco `e2e_full` agora — esse campo só entra em uso quando a infra (QA-001..004) estiver mergeada em develop. Decisão deliberada: evita que tasks da sprint 03 (Folha) sejam obrigadas a preencher um campo sem infra de suporte.

---

## Critérios de aceitação

- [ ] PRE-MERGE-CHECKLIST contém os 2 novos itens nas seções corretas (implementador + Reviewer).
- [ ] Cada item referencia `docs/architecture/desenho-testes-automatizados.md` §9.2 por path completo.
- [ ] Item do Reviewer descreve ação de "rejeitar com pendência bloqueante" em caso de inconsistência.
- [ ] `_TEMPLATE-status.md` **não** foi alterado nesta task.

---

## Definição de pronto

Planner commita direto em `develop`. Sem PR, sem Reviewer.

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md` §4 (QA-006), §9.3
- `docs/architecture/desenho-testes-automatizados.md` §9.2 (schema e2e_full)
