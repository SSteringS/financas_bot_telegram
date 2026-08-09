# ADR 0007 — Reporting com gates: status report como output contract verificável

**Data:** 2026-05-26 (registro retroativo de decisão já em vigor)
**Status:** Accepted
**Decisores:** humano (com ajuda do Claude planejador)
**Relacionado:** estende o ADR 0004 (taxonomia de persistência define *onde* o status report mora; este define *qual a forma* e *como se valida*). Base conceitual em `docs/aprendizado/structured-outputs.md`.

---

## Contexto

No workflow multiagente, cada task implementada produz um relatório de execução. Sem forma garantida, esses relatórios seriam texto livre: difíceis de agregar, fáceis de omitir o essencial, e impossíveis de validar de forma consistente. O incidente da FE-12 (branch errada por campo ambíguo no plano) mostrou que **input/output mal-especificado gera saída errada** — o mesmo princípio de structured output em LLMs (a forma reduz a variância).

A solução já estava implementada na prática — `docs/templates/_TEMPLATE-status.md` (frontmatter YAML obrigatório) + `docs/runbooks/PRE-MERGE-CHECKLIST.md` (gates verificáveis) — mas a **decisão** por trás dela nunca foi registrada como ADR. Este registro a torna canônica.

---

## Decisão

O **status report é um output contract com schema obrigatório e gates verificáveis**, e a **definição de pronto de qualquer task** é passar nesses gates. Concretamente:

1. **Schema obrigatório (frontmatter YAML parseável)** em todo `docs/sprints/<NN>/status/<TASK-ID>-*.md`, conforme `docs/templates/_TEMPLATE-status.md`: `task`, `branch`, `responsavel`, `estado` (`concluido`|`parcial`|`bloqueado`), bloco `gates:` (`build`, `lint`, `testes`, `testes_total`, `testes_novos`, `branch_convencao`, `territorio`), `desvios`, `pendencias_humano`.
2. **Gates machine-checkable**, cada um com comando e condição de passagem, definidos em `docs/runbooks/PRE-MERGE-CHECKLIST.md` e mapeados 1:1 com o bloco `gates:`.
3. **Regra de "concluído":** `estado: concluido` só é válido com **todos** os gates relevantes `ok` (ou `na`) **e** `pendencias_humano: 0`. Caso contrário, `parcial` ou `bloqueado`.
4. **Verificação independente sobre o report:** schema válido garante a *sintaxe*, não a *semântica* — um report pode dizer `testes: ok` sem ser verdade. Por isso os gates são conferidos **contra a realidade** (rodar o build/testes, ler o diff) antes do merge, não tomados como fé. Em tasks de dinheiro/infra, isso é feito por verificação independente (ADR 0004 §4; mecanismo no ADR 0005, o Reviewer).

---

## Razões

- **Agregável:** frontmatter YAML deixa um script ler todos os `docs/sprints/<NN>/status/*.md` e montar um painel do projeto (tasks por estado, desvios por área, soma de testes, gate-fails) sem esforço.
- **Checklist forçado:** campo obrigatório faltando = violação visível. O schema obriga o implementador a reportar o essencial.
- **Reduz variância:** plano (input contract) + status report (output contract) bem-especificados reduzem a variância do agente, igual a um JSON schema reduzir a variância de um LLM.
- **Sintaxe ≠ semântica:** separar conformidade de processo (branch certa? report presente? = sintaxe) de qualidade (arquitetura, testes corretos = semântica) torna a revisão mais clara e expõe que report válido ainda pode mentir — daí a verificação independente.
- **Enforced > disciplina:** os mesmos gates serão enforced por CI no caminho pra `develop` (plano CI-01), tirando o escorregão da disciplina manual conforme o volume cresce.

---

## Consequências

**Positivas:**
- Estado do projeto fica parseável e auditável; base pra métricas (backlog de workflow, item #3).
- "Pronto" deixa de ser opinião e vira condição verificável.
- A porta fica aberta pro enforcement por CI (CI-01) sem mudar o schema.

**Negativas:**
- Custo por task de preencher o frontmatter corretamente (mitigado pelo template).
- Schema válido **não** garante verdade — exige a camada de verificação independente pra não virar teatro de conformidade.
- Mais um artefato a manter em sincronia (template ↔ checklist ↔ campos do CI).

---

## Alternativas consideradas

- **Status report em texto livre:** descartado. Não agrega, não força o essencial, não dá pra validar de forma consistente.
- **Confiar no autorrelato (sem conferir gates contra a realidade):** descartado. É exatamente o "sintaxe ≠ semântica" — um report schema-válido pode mentir; sem verificação o gate é decorativo.
- **Enforcement só por disciplina manual, sem CI:** insuficiente a longo prazo (escorrega com volume) — por isso o CI-01 evolui pra enforcement.

---

## Referências

- `docs/templates/_TEMPLATE-status.md` (o schema)
- `docs/runbooks/PRE-MERGE-CHECKLIST.md` (os gates verificáveis)
- `docs/aprendizado/structured-outputs.md` (output schema, sintaxe ≠ semântica, aplicação ao workflow)
- ADR 0004 (taxonomia e verificação independente) e ADR 0005 (Reviewer como mecanismo)
- `docs/plans/CI-01-gate-pr-develop.md` (enforcement por CI)
</content>
