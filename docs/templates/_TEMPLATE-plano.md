---
# ─── Frontmatter (schema obrigatório — parseável) ───
task: BE-19                                # ^(BE|FE|DEP|FIX|HOTFIX|EVO|CI|WF|QA)-\d+[a-z]?$ (FIX/HOTFIX = 3 dígitos zero-padded a partir de 2026-05-29; QA = tooling de qualidade, a partir de 2026-06-01 — ADR 0017)
titulo: "Adapter de entrada WhatsApp"
sprint: 02-canal-whatsapp                  # NN-slug
data_planejamento: 2026-05-30              # YYYY-MM-DD
branch_alvo: feature/be-19-adapter-entrada-whatsapp
prioridade: alta                           # alta | media | baixa
esforco: alto                              # baixo | medio | alto
territorio: back                           # back | front | plan | infra
estado: pronto-pra-execucao                # rascunho | pronto-pra-execucao | em-execucao | concluido | bloqueado | parqueado
depende_de: []                             # lista de task-ids; [] se nenhuma
bloqueia: []                               # lista de task-ids; [] se nenhuma
skills_dispatched: []                      # skills a carregar nesta task — sinal B do feedback loop
                                           # ex: [arquitetura-hexagonal, qualidade-de-testes]
                                           # [] se nenhuma skill específica além das always-on do role
integration_branch: integration/03-evo-09  # branch intermediária desta sprint — implementador cria feature/* a partir daqui
                                           # null para fix/hotfix (saem direto de develop)
fluxos_qa: []                              # fluxos de teste automatizado que o QA deve executar nesta task
                                           # ex: [integracao-autenticacao, componente-lista-pedidos, e2e-fluxo-compra]
                                           # [] se nenhuma (task documental, infra pura, ou flows já cobertos)
                                           # preenchido pelo planner; QA pode sugerir ajustes na seção 7 da avaliação
mutation_gate: false                       # gate de mutation testing desta task — opcional e por task (ADR 0021)
                                           # true  → test strength (mortos ÷ cobertos) ≥ 80%, medido APENAS sobre as
                                           #         classes alteradas por esta task; código antigo fica fora do denominador
                                           # false → não se aplica nesta task
                                           # planner pergunta ao humano ao escrever o plano; só vale se declarado true aqui
                                           # sobrevivente equivalente com demonstração escrita não conta contra o piso
mutation_rationale: ""                     # obrigatório quando mutation_gate: true — quais classes alteradas entram no
                                           # escopo da medição e por que esta task adota o gate; "" quando false
---

# [TASK-ID] — Título curto da tarefa

> **Não edite este arquivo.** Copie pra `<TASK-ID>-<slug>.md` na pasta da sprint correta (`docs/sprints/<NN>/plans/`), preencha o frontmatter e as seções abaixo.

---

## Intake (resumo executivo do que o frontmatter dispara)

Bloco humano-legível que repete e justifica o frontmatter. Use bullets curtos:

- **Origem:** de onde veio a demanda (spec, ADR, RETRO, dor concreta, item do backlog).
- **Por quê agora:** o que destrava ou bloqueia.
- **Esforço:** estimativa em horas/dias + o que faz ser leve/pesado.
- **Riscos resumidos:** 2-3 linhas. Detalhe vai na seção "Riscos & mitigações".

---

## Contexto

O que já existe no repo que esta task pressupõe. Estado atual da parte do sistema que vai ser tocada. Cite arquivos/pacotes/ADRs com path. Quanto mais o implementador sabe de pré-condições, menor a variância da execução.

---

## Decisão / abordagem

Como atacar. **Não é "o que tem que ficar pronto"** (isso vai nos Critérios) — é **o caminho técnico**: que padrão seguir, que decisões já foram tomadas pelo planner/arquiteto pra reduzir hesitação do implementador, o que deliberadamente NÃO é desta task.

---

## Escopo / arquivos

Lista do que mexe, separado por tipo de mudança:

### Criar
- `path/Arquivo.ext` — propósito + nota crítica.

### Modificar
- `path/Existente.ext` — o que muda e por quê.

### Mover (com `git mv` se preservar histórico importa)
- `de/path.ext` → `pra/path.ext`.

### Não tocar (escopo limitado)
- Pacote/arquivo X — fora desta task; se descobrir que precisa, abrir FIX separada.

---

## Testes

O que precisa ser coberto. Granularidade: unit / integration / e2e / manual. Estimar `testes_total` esperado pós-task e `testes_novos`. Se algum tipo não se aplica, escrever "Não aplicável: <motivo>".

---

## Critérios de aceitação

Checklist verificável. Cada item bate com um gate do PRE-MERGE-CHECKLIST ou é específico desta task.

- [ ] Comportamento X funciona.
- [ ] `mvn test` (ou equivalente) verde com `testes_total ≥ N` e `testes_novos ≥ M`.
- [ ] Branch saiu de `develop` no formato `<prefixo>/<id>-<slug>`.
- [ ] Território respeitado (só `<pasta>`).
- [ ] Status report `docs/sprints/<NN>/status/<TASK-ID>-<slug>.md` com frontmatter válido (`_TEMPLATE-status.md`).

---

## Fora de escopo (explicitamente)

O que **não** entra nesta task mesmo que pareça relacionado. Cita o que entra em qual task futura ou vira pendência. Reduz drift do implementador.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Descrição curta | Baixa\|Média\|Alta | Baixo\|Médio\|Alto | O que reduz/elimina |

---

## Coordenação

- **Pode rodar em paralelo com:** outras tasks ativas que não conflitam.
- **Depende sequencialmente de:** o que precisa estar em `develop` antes.
- **Bloqueia:** o que destrava quando esta mergear.
- **Atenção pro Reviewer:** pontos específicos que o Reviewer deve olhar (smells esperados, áreas sensíveis).
- **Atenção pro QA:** contexto sobre os `fluxos_qa` escolhidos — por que cada flow foi incluído e o que está fora de escopo.
- **Após merge:** ações necessárias (atualizar `STATE.md`, despachar próxima task, abrir pendência).

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido conforme `_TEMPLATE-status.md`, **revisão do Reviewer** (sessão separada — ADR 0005), e — quando `fluxos_qa ≠ []` — **gate QA** com seção 7 da avaliação preenchida e `veredito_qa: aprovado` ou `aprovado_com_ajustes`, e — quando `mutation_gate: true` — **gate de mutação** (ADR 0021) com `test strength` ≥ 80% medido apenas sobre as classes alteradas por esta task, sobreviventes classificados como equivalentes acompanhados de demonstração escrita no status report. Implementador abre o PR da feature para `integration_branch` após todos os gates OK; **não** abre PR direto pra `develop`.

---

## Referências

- Spec/ADR/aprendizado relevantes com path.
- Padrão referencial no código (`adapters/in/telegram/...` etc).
- Tasks adjacentes.
