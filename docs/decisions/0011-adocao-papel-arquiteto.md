# ADR 0011 — Adoção do papel de Arquiteto

**Data:** 2026-05-27
**Status:** Accepted
**Decisores:** humano (com ajuda do Claude planejador)
**Relacionado:** evolui o ADR 0005 (que adiava o `architect.md`) e refina o ADR 0004 (ownership de arquitetura). Materializado em `docs/roles/architect.md`.

---

## Contexto

O ADR 0005 **adiou** o `architect.md` ("arquitetura cabe no planner") e o ADR 0004 pôs o **planner como dono da arquitetura**. Foram decisões corretas no estágio do MVP, onde as decisões de arquitetura eram poucas e cabiam na sessão do planner.

Pós-MVP, a **sprint 02** (Canal WhatsApp + notificação + observability) traz decisões técnicas densas: escolha de **provider de WhatsApp** (Cloud API oficial vs Z-API/Evolution, com trade-offs de custo/ToS/esforço), redesenho do **adapter de entrada**, e topologia de **observabilidade**. Acumular isso na sessão do planner dilui o foco de coordenação e mistura "o que coordenar" com "como desenhar tecnicamente" — a mesma diluição/viés que motivou o Reviewer e os papéis especializados (ADR 0005). O gatilho que o próprio 0005 previa ("até a dor justificar") chegou.

---

## Decisão

1. **Adotar o papel de Arquiteto** (`docs/roles/architect.md`), acionado **sob demanda** — quando o ciclo tem decisões técnicas densas. Não é sessão permanente.
2. **Fronteira planner ↔ arquiteto ↔ humano:**
   - **Arquiteto desenha e propõe:** comparativos de trade-off, specs em `architecture/`, e **ADRs em status `Proposed`**.
   - **Humano homologa:** o ADR vira `Accepted` (o arquiteto **não** homologa a própria proposta — independência, igual ao Reviewer).
   - **Planner integra:** quebra a decisão homologada em tasks (`plans/`), coordena o ciclo, mantém docs/processo.
3. **Refina o ADR 0004:** "planner é dono da arquitetura" passa a — *o planner é dono da arquitetura **na ausência de um arquiteto acionado**; quando acionado, o arquiteto desenha/propõe e o humano homologa. Decisões pequenas seguem com o planner.* A decisão canônica continua sendo o ADR homologado pelo humano.
4. **Supersede** o ponto específico do ADR 0005 que adiava o `architect.md`. O resto do 0005 (estrutura de papéis, Reviewer, regra delta-não-cópia) **segue vigente**. `qa.md` continua adiado.

---

## Razões

- **Separação cognitiva por sessão** ataca diluição de contexto e viés — mesmo princípio do Reviewer (0005).
- **Sob demanda** evita overhead: o arquiteto não é papel fixo, só entra quando a densidade técnica paga o custo de orquestrar mais uma sessão.
- **Humano como homologador** preserva o "quem propõe não é juiz da própria proposta" e mantém a decisão canônica via ADR (0004).
- **Sem disputa de território:** arquiteto = desenho técnico; planner = coordenação/processo; implementadores = código; reviewer = verificação. Cada um na sua fase.

---

## Consequências

**Positivas:**
- Decisões técnicas densas (ex.: provider de WhatsApp) ganham desenho dedicado e trade-offs explícitos **antes** do código.
- O planner volta a focar em coordenação/planejamento, sem carregar o desenho técnico pesado.
- O roadmap de papéis amadurece conforme a dor, como o 0005 previa.

**Negativas / custos:**
- Mais uma sessão pra orquestrar — depende da disciplina do humano de **abrir a sessão certa pro trabalho certo**.
- Risco de sobreposição planner/arquiteto se a fronteira não for respeitada — mitigado pela seção "Fronteira com o planner" no `architect.md`.
- `architect.md` é novo, em amadurecimento — refinar com o uso.

---

## Alternativas consideradas

- **Manter tudo no planner (status quo do 0005):** descartado — é exatamente a diluição que a sprint 02 expõe.
- **Arquiteto como sessão permanente:** descartado — overhead sem retorno; sob demanda basta.
- **Usar o Reviewer pra validar arquitetura:** descartado — o Reviewer verifica a **entrega contra a realidade** (pós-implementação); o arquiteto desenha o **"como"** (pré-implementação). São fases diferentes, não substitutas.

---

## Referências

- `docs/roles/architect.md` (o papel materializado por esta decisão)
- ADR 0004 (ownership/taxonomia — refinado aqui) · ADR 0005 (papéis; o adiamento do architect é superseded por este ADR)
- `docs/sprints/02-canal-whatsapp/README.md` (primeiro ciclo a acionar o arquiteto)
</content>
