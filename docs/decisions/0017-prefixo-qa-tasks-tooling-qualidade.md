---
adr: 0017
titulo: "Prefixo QA-NNN para tasks de tooling/infra de qualidade"
data: 2026-06-01
status: Accepted
data_homologacao: 2026-06-01
decisores: humano
relacionado: [0004, 0005, 0007, 0011, 0015]
supersedes: null
superseded_by: null
---

# ADR 0017 - Prefixo QA-NNN para tasks de tooling/infra de qualidade

> Proposto pelo arquiteto em 2026-06-01 como parte do refinamento do desenho de testes automatizados.
> **Homologado pelo humano em 2026-06-01.** Mudancas downstream (Sec. 5) sao tarefas do planner; nao foram executadas pelo arquiteto nesta sessao.

---

## Contexto

O CLAUDE.md raiz define a regex de IDs de task aceitos pelos templates e pelo workflow:

```
^(BE|FE|DEP|FIX|HOTFIX|EVO|CI|WF)-\d+[a-z]?$
```

Os prefixos hoje cobrem:

- BE / FE: produto, por territorio de codigo
- DEP: deploy/infra
- EVO: evolucao de produto que cruza varios territorios
- CI: pipeline de integracao continua
- WF: workflow / processo (ex: kaizen, organizacao de docs)
- FIX / HOTFIX: correcoes fora do fluxo de feature

O desenho da suite E2E (Playwright + a11y + webhook parametrizado) introduz uma classe nova de trabalho: tooling e infraestrutura de qualidade. Nao e feature de produto, nao e deploy, nao e workflow puro, nao e fix. E tooling que suporta features.

Tentar encaixar nos prefixos existentes forca distorcoes:

- FE-NNN para a task que cria frontend/e2e/ parece natural (mora no front), mas a task que decide o mock de download de midia Telegram cai no back. A iniciativa fica fatiada em dois prefixos e perde unidade.
- WF-NNN e workflow no sentido de processo organizacional (kaizen, retro, dispatch). Tooling de teste tem dimensao tecnica diferente: escrever spec Playwright e trabalho de engenharia, nao de processo.
- CI-NNN e especificamente pipeline. A suite E2E roda local sob demanda (decisao arquitetural Sec. 5.1 item 1 do desenho de testes), nao em CI.

A taxonomia de papeis (ADR 0015) ja reconhece o qa-test-specialist como subagent dedicado. Falta o prefixo de task que case com ele.

---

## Decisao

### 1. Adotar prefixo QA-NNN

Adicionar QA a lista de prefixos aceitos. Regra de uso:

> Use QA-NNN quando a task entrega tooling, infraestrutura ou processo cuja funcao primaria e suportar a qualidade do produto (testes, observabilidade de qualidade, analise estatica especializada), e nao modificar comportamento de feature.

Casos de uso tipicos:

- Setup de framework de teste novo (Playwright, Schemathesis, axe-core)
- Helpers/fixtures de teste compartilhados
- Scripts de orquestracao de suite (subir stack, derrubar stack, healthcheck)
- Specs de teste que levantam uma capacidade nova (nao apenas adicionam um caso a uma suite existente)
- Runbooks operacionais de QA (ROTEIRO-E2E.md, etc.)
- Decisoes de mock/stub que afetam so o ambiente de teste

Casos que NAO sao QA:

- Teste unitario/integracao escrito como parte de uma task BE/FE de feature: fica BE-NNN / FE-NNN (testes sao gate da propria task, conforme ADR 0007)
- Pipeline CI rodando testes: CI-NNN
- Ferramenta de observabilidade de producao (CloudWatch alarms, logs): DEP-NNN

### 2. Regex atualizada

```
^(BE|FE|DEP|FIX|HOTFIX|EVO|CI|WF|QA)-\d+[a-z]?$
```

Numeracao de 3 digitos zero-padded a partir do nascimento do prefixo (mesma regra forward-only de FIX/HOTFIX em 2026-05-29). Primeira task: QA-001.

### 3. Padrao de nome de branch

feature/qa-NNN-slug: mesma estrutura dos demais prefixos de feature. Sai de integration/NN-slug quando a task QA pertence a uma sprint regular; sai de develop em casos excepcionais (raros).

### 4. Territorio

Tasks QA-NNN podem tocar:

- frontend/e2e/ (suite Playwright e adjacentes)
- frontend/.env.e2e.example (template versionado)
- frontend/package.json (scripts npm de e2e)
- docs/runbooks/ROTEIRO-E2E.md e similares
- financas_bot_telegram/src/main/resources/application-*.properties APENAS quando a mudanca e exclusivamente para tooling de teste (ex: flag app.telegram.skip-media-download)
- financas_bot_telegram/src/main/java/... APENAS para stubs/mocks ativados por profile de teste, nunca para logica de feature

Quando uma task QA precisar mexer em codigo de producao real (nao-stub), virar BE/FE ou abrir tasks separadas. Manter QA como prefixo que NAO modifica comportamento user-facing.

### 5. Mudancas downstream necessarias (aguardam homologacao)

Quando este ADR virar Accepted:

- CLAUDE.md raiz, secao "Padrao de nome de branch": adicionar QA-NNN na lista de prefixos com regra de uso resumida.
- docs/templates/_TEMPLATE-plano.md, frontmatter: atualizar regex no comentario do campo task:.
- docs/templates/_TEMPLATE-status.md, frontmatter: atualizar regex no comentario do campo task:.
- .claude/agents/qa-test-specialist.md, initialPrompt: mencionar que tasks tipicas levam prefixo QA-NNN.
- docs/aprendizado/ ou secao do desenho-testes-automatizados.md: registrar a convencao pra proximos planners.

Essas mudancas sao tarefas de planner pequenas, todas em docs/ (e .claude/). NAO inclui mudanca em codigo de producao.

---

## Razoes

- Casa com a taxonomia de papeis (ADR 0015). Ja existe qa-test-specialist como subagent. Ter QA-NNN como prefixo paralelo deixa a rastreabilidade obvia: task QA-005 provavelmente foi dispatched com a skill do qa-test-specialist, e a sprint "## Agents & Skills" da retro consegue cruzar a metrica.
- Identifica trabalho que nao modifica feature. Reviewer e planner conseguem ler o prefixo e calibrar expectativa: task QA nao exige exige_e2e_full: true (a suite e o entregavel da propria task), nao exige cobertura_pct no sentido de feature, e o status report fica enxuto.
- Nao conflita com prefixos existentes. WF continua sendo processo organizacional (retro, kaizen, organizacao de docs). CI continua sendo pipeline. QA ocupa um nicho real que hoje e forcado em outros prefixos.
- Custo baixo de adocao. Mudanca em 4-5 arquivos de docs/ e .claude/, todos textuais. Nenhuma migracao de task existente (regra forward-only consistente com FIX/HOTFIX).
- Alinhamento com pratica da industria. "QA" como categoria de trabalho e universalmente entendido; nao exige onboarding adicional pra contribuidor humano externo eventual.

---

## Consequencias

Positivas:

- Iniciativa de tooling de qualidade ganha unidade no backlog (5-7 tasks QA-NNN em vez de mix FE-NNN + BE-NNN + WF-NNN).
- Metricas de sprint conseguem segregar trabalho de feature vs. trabalho de qualidade ("nesta sprint, 70% feature + 30% QA").
- Reviewer recebe sinal claro pelo prefixo: task QA tem checklist especifico (a suite roda? helpers idempotentes? cleanup limpa so o requisitante 99?).
- Subagent qa-test-specialist ganha prefixo natural pra delegacao; planner consegue dizer "dispatch QA-003 ao qa-test-specialist".

Negativas / custos:

- +1 prefixo na regex: pequena complexidade adicional no template e na validacao mental do planner. Mitigado pela regra de uso explicita (Sec. 1).
- Risco de mau uso: planner pode marcar como QA algo que deveria ser BE/FE (teste de feature, nao tooling). Mitigado pela regra "funcao primaria e suportar qualidade, nao modificar feature".
- Dupla manutencao da regex: tres lugares (CLAUDE.md, template de plano, template de status). Mitigado pelo fato de ser change-once: adiciona |QA e fim.

Metricas pra avaliar adocao:

- Baseline: 0 tasks QA-NNN no historico (ADR aprovada hoje).
- Alvo (1 mes): >=5 tasks QA-NNN despachadas, todas com funcao primaria de tooling de qualidade. Auditoria manual confirma encaixe.
- Criterio de parada: se em 1 mes nenhuma task QA-NNN for criada, prefixo e decoracao. Reavaliar (talvez FE/BE simples bastassem).
- Sinal de mau uso: task QA-NNN que tocou em codigo user-facing (nao-stub). Sinal de que o prefixo errado foi escolhido, vira retro item.

---

## Alternativas consideradas

- TEST-NNN: descartado. "TEST" colide semanticamente com "arquivo de teste" na literatura de software, ambiguidade ruim para o planner ("TEST-005 e uma task ou e um arquivo de teste do codigo?"). "QA" tem fronteira mais clara.
- QA-NNN + TEST-NNN (dois prefixos): descartado. Dobra complexidade de decisao do planner sem ganho claro. MVP do prefixo prefere um conceito so.
- Sem prefixo novo, distribuir entre BE/FE/WF: descartado. Perde unidade da iniciativa, e nenhum prefixo existente representa fielmente "tooling de qualidade que nao muda feature".
- QA apenas como sufixo/tag em tasks BE/FE (ex: FE-018-qa-setup-playwright): descartado. Convencao de slug e livre, nao ha gating automatico; sufixo no slug e invisivel pra qualquer ferramenta que filtre por prefixo de ID.
- Manter WF-NNN com sub-categorizacao: descartado. WF e processo organizacional (kaizen, ritual de fechamento, ADR de governance). Tooling de teste e trabalho de engenharia, dimensao diferente. Forcar WF enfraquece ambos os significados.

---

## Referencias

- ADR 0004: governanca/ownership do workflow multiagente.
- ADR 0005: sessoes especializadas por papel (base do qa-test-specialist como sessao).
- ADR 0007: status report como output contract (define o schema do frontmatter onde task: e validado).
- ADR 0011: adocao do arquiteto (este ADR e proposto pelo arquiteto).
- ADR 0015: taxonomia roles x skills (define qa-test-specialist como subagent).
- docs/architecture/desenho-testes-automatizados.md: spec arquitetural que gerou a necessidade do prefixo.
- docs/templates/_TEMPLATE-plano.md e _TEMPLATE-status.md: arquivos que precisam ser atualizados quando este ADR virar Accepted.
- CLAUDE.md raiz, secao "Padrao de nome de branch": outro arquivo a atualizar.
