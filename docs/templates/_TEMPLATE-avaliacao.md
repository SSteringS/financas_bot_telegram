---
# ─── Frontmatter (schema obrigatório — parseável) ───
task: BE-19                                # task-id da task avaliada
sprint: 02-canal-whatsapp                  # NN-slug
data: 2026-05-30                           # YYYY-MM-DD
avaliador: claude-reviewer                 # claude-reviewer | claude-plan (legado pré-ADR 0005)
status_report: docs/sprints/02-canal-whatsapp/status/BE-19-adapter-entrada-whatsapp.md  # path do status que esta avaliação valida
veredito_codigo: aprovado_com_observacoes  # aprovado | aprovado_com_observacoes | reprovado
veredito_final: pendente                   # pendente | aprovado | aprovado_com_observacoes | reprovado
observacoes_count: 2                       # int — observações materiais (não cosméticas)
roteiro_executado: false                   # bool — humano já preencheu a tabela (quando aplicável)
gates_verificados_contra_realidade: ok     # ok | divergente — Reviewer reproduziu os gates do status?
skills_eficazes: []          # skills cujo efeito foi observado no código — sinal C positivo
                             # ex: [arquitetura-hexagonal] quando não há drift em task que toca application/
skills_gaps: []              # skills ausentes no dispatch cuja falta causou problema observável — sinal C negativo
                             # ex: [qualidade-de-testes] quando testes cobrem só happy path
veredito_qa: pendente        # pendente | aprovado | aprovado_com_ajustes | reprovado | nao_aplicavel
                             # nao_aplicavel quando fluxos_qa: [] no plano; preenchido pelo qa-test-specialist
fluxos_qa_executados: []     # fluxos que o QA efetivamente executou nesta avaliação
fluxos_qa_adicionar: []      # fluxos que QA recomenda adicionar ao plano (pro planner ajustar)
fluxos_qa_remover: []        # fluxos desnecessários que QA recomenda remover do plano
---

# Avaliação — [TASK-ID] (título curto)

> **Não edite este arquivo.** Copie pra `<TASK-ID>-<slug>.md` na pasta `docs/sprints/<NN>/avaliacoes/`, preencha o frontmatter e as seções abaixo.

**Branch:** `feature/...`
**Implementador:** claude-back | claude-front
**Plano:** `docs/sprints/<NN>/plans/<TASK-ID>-<slug>.md`
**Status report:** `docs/sprints/<NN>/status/<TASK-ID>-<slug>.md`

---

## 1. Análise de código (Reviewer lê o diff)

Reviewer abre o diff, lê o código real (não o status), e dá veredito **da implementação**:

### Veredito de código: aprovado / aprovado com observações / reprovado

Prosa curta com a leitura: o que está bem desenhado, o que está fora do padrão, o que viola arquitetura. Cita arquivos/linhas quando crítico.

### Observações materiais

Pra cada observação, um bloco:

**Observação N — [título curto]**
- **O quê:** descrição factual.
- **Onde:** path/arquivo:linha.
- **Por quê importa:** impacto (regressão, drift de contrato, smell arquitetural, edge case).
- **Sugestão:** o que fazer (corrigir agora / abrir FIX / registrar como pendência / aceitar).

Se zero observações: "Nenhuma observação material." e `observacoes_count: 0`.

---

## 2. Gates verificados contra a realidade

Reviewer **roda** (não lê) os gates que o status afirma estarem `ok`. Tabela:

| Gate | Status diz | Reviewer reproduziu | Divergência? |
|---|---|---|---|
| build | ok | ok | não |
| testes | ok (226 total, 18 novos) | ok (226 total) | não |
| lint | ok | ok | não |
| branch_convencao | ok | ok (`feature/be-19-...`) | não |
| territorio | ok | ok (só `financas_bot_telegram/`) | não |

Se algum divergente: `gates_verificados_contra_realidade: divergente` no frontmatter + bloco "Divergência" detalhando.

---

## 3. Roteiro de validação manual (opcional)

> **Quando preencher esta seção:** task tem UI visual, integração com bot real, console de cloud (S3/AWS), ou qualquer comportamento que só humano consegue verificar. Tasks puramente de backend interno (refactor, lib, bean) podem pular — anotar "Não aplicável: <motivo>".

Reviewer monta o roteiro, humano executa, Reviewer fecha o veredito final.

### Pré-condições

- [ ] Ambiente dev rodando com profile X
- [ ] Banco de dados com dados de fixture Y
- [ ] Acesso ao console Z

### Casos

| # | Ação | Esperado | Resultado | Observação |
|---|---|---|---|---|
| 1.1 | descrição da ação | comportamento esperado | (humano preenche ✅/❌/⚠️) | |
| 1.2 | ... | ... | | |

---

## 4. Resultado consolidado

| Item | Resultado |
|---|---|
| Análise de código | aprovado / aprovado com observações / reprovado |
| Gates contra a realidade | ok / divergente |
| Roteiro manual | n/a / aprovado / falhou no caso X.Y |
| **Veredito final** | mergear / ajustar antes de mergear / bloquear |

---

## 5. Skills — feedback loop

> Preencher sempre, mesmo que as listas fiquem vazias — é o único ponto de coleta do sinal C.
> Basta uma linha por skill; o frontmatter é o que o script agrega.

**Skills eficazes** (confirma efeito observável no código):
- `<nome-da-skill>` — evidência: ex. "nenhum drift hexagonal detectado; adapter não importa JdbcTemplate diretamente"

**Skills com gap** (ausentes no dispatch, falta foi causa observável de problema):
- `<nome-da-skill>` — problema observado + por que essa skill teria prevenido

Se nenhum signal: "Sem observação de skills nesta task." e `skills_eficazes: []`, `skills_gaps: []`.

---

## 6. Para o planner (próximos passos)

Decisões/ações que o planner precisa integrar após esta avaliação:

- Ordem de merge se há dependência.
- Observação que vira ADR / pendência técnica / FIX separada.
- Sincronização front/back de algum drift de contrato.
- Atualização do STATE.md.

Se nada: "Sem ações pendentes pro planner — pronto pra merge."

---

## 7. QA — Fluxos automatizados

> Preenchido pelo **qa-test-specialist** após o Reviewer dar OK no veredito_final.
> Se `fluxos_qa: []` no plano, preencher como "Não aplicável: task sem fluxos QA definidos." e `veredito_qa: nao_aplicavel`.

### 7.1 Avaliação do plano de testes

O plano especifica os seguintes fluxos em `fluxos_qa`: (listar do frontmatter do plano)

**Concordância com o plano:**
- ✅ **Manter:** (fluxo — rationale: risco coberto)
- ➕ **Adicionar:** (fluxo — rationale: risco não coberto pelo plano)
- ➖ **Remover:** (fluxo — rationale: desnecessário dado o contexto)

Se concorda integralmente: “Concordo com todos os fluxos especificados — nenhuma sugestão de ajuste.”

### 7.2 Fluxos executados

| Fluxo | Comando executado | Resultado | Observações |
|---|---|---|---|
| nome-do-flow | `./mvnw test -Dtest=...` | ✅ Verde / ❌ Vermelho | |

### 7.3 Issues encontrados

🔴 **Críticos** (bloqueiam merge):
- [ISSUE] Descrição factual — risco: o que pode dar errado em produção.

🟡 **Recomendados** (não bloqueiam, mas são dívida):
- [ISSUE] ...

🟢 **Oportunidades** (cobertura adicional):
- [ISSUE] ...

Se nenhum: “Nenhum issue encontrado — todos os fluxos passaram.”

### 7.4 Bloqueio para humano (se aplicável)

> Preencher apenas se há bloqueio que exige decisão ou ação humana antes de prosseguir.
> Se não há bloqueio: “Não aplicável.”

(Descrição do bloqueio + contexto mínimo para o humano decidir)

### 7.5 Veredito QA

**aprovado** / **aprovado_com_ajustes** / **reprovado**

Prosa de 1-2 frases justificando.
- Se aprovado: "APROVADO: pode abrir PR para `integration_branch`."
- Se reprovado: “AÇÃO NECESSÁRIA: corrigir issues 🔴 antes de abrir PR — retornar ao reviewer após correções.”
