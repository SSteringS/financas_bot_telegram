---
task: FIX-005-front
titulo: "Atualizar folha.ts e types/folha.ts — /api/funcionarios → /api/v1/funcionarios"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-06
branch_alvo: fix/005-padronizar-api-v1-front
prioridade: alta
esforco: baixo
territorio: front
estado: pronto-pra-execucao
depende_de: [FIX-005-back]
bloqueia: [QA-011-A]
skills_dispatched: []
integration_branch: null
fluxos_qa: []
---

# FIX-005 front — Atualizar `/api/funcionarios` → `/api/v1/funcionarios` no frontend

---

## Intake

- **Origem:** FIX-005 back (PR #109) padronizou os controllers Java para `/api/v1/funcionarios/**`. O frontend nunca foi atualizado — toda chamada da área de folha retorna 404 em produção.
- **Por quê agora:** produto quebrado. Tela de funcionários e folha de pagamento estão inacessíveis.
- **Esforço:** baixo — search & replace em dois arquivos; ~10 min de execução.
- **Riscos:** praticamente zero — é substituição de string de URL sem alteração de lógica.

---

## Contexto

FIX-005 back (PR #109, commit `3bd3a5d`) moveu os controllers:
- `FuncionarioController` → `@RequestMapping("/api/v1/funcionarios")`
- `FolhaController` → `@RequestMapping("/api/v1/funcionarios")` (subrecursos: vales, adiantamentos, fechamentos)

O `JwtAuthenticationFilter` agora protege qualquer path sob `/api/` que não esteja na allowlist explícita — o antigo `/api/funcionarios/**` **não está** na allowlist, então retorna 401 mesmo que o path existisse.

O frontend ainda usa os paths antigos em dois arquivos:

| Arquivo | Ocorrências funcionais | Ocorrências em JSDoc |
|---|---|---|
| `frontend/src/api/folha.ts` | 10 | 6 |
| `frontend/src/types/folha.ts` | 0 | 8 |

---

## Decisão / abordagem

Search & replace global: `/api/funcionarios` → `/api/v1/funcionarios` nos dois arquivos.

Não há lógica condicional, não há construção dinâmica de URL — todas as ocorrências são strings literais. A substituição é segura e mecânica.

Branch: `fix/005-padronizar-api-v1-front`, saindo de `develop` (padrão FIX).

---

## Escopo / arquivos

### Modificar

- `frontend/src/api/folha.ts` — substituir todas as ocorrências de `/api/funcionarios` por `/api/v1/funcionarios` (10 chamadas funcionais + 6 em JSDoc de cabeçalho).
- `frontend/src/types/folha.ts` — substituir todas as ocorrências de `/api/funcionarios` por `/api/v1/funcionarios` (8 ocorrências, todas em JSDoc — sem impacto funcional, mas mantém docs consistentes).

### Não tocar

- Qualquer outro arquivo de frontend — fora do escopo desta task.
- Nenhum arquivo de backend — FIX-005 back já está concluído.

---

## Testes

**Não aplicável — testes automatizados:** não existem testes unitários de `folha.ts` ainda (são criados em QA-010). A correção é verificável via smoke test manual ou pelos testes E2E existentes (QA-004/QA-008).

**Verificação obrigatória antes do merge:**
- `npm run build` (ou `tsc --noEmit`) verde — sem erros de compilação TypeScript.
- Smoke manual: abrir tela de funcionários e confirmar que a chamada de rede vai para `/api/v1/funcionarios` (DevTools → Network).

`testes_novos: 0` — nenhum teste novo criado nesta task (cobertura de `folha.ts` é escopo de QA-010).

---

## Critérios de aceitação

- [ ] `frontend/src/api/folha.ts`: zero ocorrências de `/api/funcionarios` sem o prefixo `/v1/`.
- [ ] `frontend/src/types/folha.ts`: zero ocorrências de `/api/funcionarios` sem o prefixo `/v1/`.
- [ ] `npm run build` (ou `tsc --noEmit`) verde.
- [ ] Branch saiu de `develop` no formato `fix/005-padronizar-api-v1-front`.
- [ ] Território respeitado: apenas `frontend/src/api/folha.ts` e `frontend/src/types/folha.ts`.
- [ ] Status report `docs/sprints/03-folha-pagamento/status/FIX-005-front-padronizar-api-v1.md` com frontmatter válido.

---

## Fora de escopo

- **Testes unitários de `folha.ts`** — escopo de QA-010.
- **E2E de cenários 401** — escopo de QA-011 Sub-área A (desbloqueia com este fix).
- **Outros arquivos de frontend** — nenhum outro arquivo referencia `/api/funcionarios`.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Arquivo adicional com `/api/funcionarios` não mapeado | Baixa | Baixo | Rodar `grep -rn "api/funcionarios" frontend/src/` antes do commit e confirmar zero resultados |
| Build quebrado por erro de digitação | Baixa | Médio | `tsc --noEmit` obrigatório antes do commit |

---

## Coordenação

- **Pode rodar em paralelo com:** QA-010, QA-011 Sub-áreas B/C/D.
- **Depende sequencialmente de:** FIX-005 back (PR #109 — já mergeado em develop ✅).
- **Bloqueia:** QA-011 Sub-área A (testes E2E de cenários foto+caption e JWT 401 na área de folha).
- **Atenção pro Reviewer:** confirmar que `grep -rn "api/funcionarios" frontend/src/` retorna zero após a mudança.
- **Atenção pro QA:** `fluxos_qa: []` — não há fluxo QA automatizado nesta task; a cobertura de `folha.ts` é escopo de QA-010.
- **Após merge:** atualizar README sprint 03 marcando FIX-005 front como ✅; despachar QA-011 Sub-área A.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido conforme `_TEMPLATE-status.md`, **revisão do Reviewer** (sessão separada — ADR 0005). PR direto para `develop` (task FIX, não passa por integration branch).

---

## Referências

- FIX-005 back: `docs/sprints/03-folha-pagamento/status/FIX-005-back-padronizar-api-v1.md`
- Avaliação FIX-005 back: `docs/sprints/03-folha-pagamento/avaliacoes/FIX-005-back-padronizar-api-v1.md`
- `frontend/src/api/folha.ts` — arquivo alvo (10 chamadas funcionais)
- `frontend/src/types/folha.ts` — arquivo alvo (8 JSDoc)
- QA-011: `docs/sprints/03-folha-pagamento/plans/QA-011-expansao-e2e-cenarios-positivos.md` — desbloqueia Sub-área A
