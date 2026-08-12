---
task: QA-013
titulo: "PMD — ruleset curado e piloto de leitura interpretada"
sprint: 04-instrumentacao-qualidade
data_planejamento: 2026-08-12
branch_alvo: feature/qa-013-pmd-ruleset-curado-e-piloto
prioridade: alta
esforco: medio
territorio: back
estado: pronto-pra-execucao
depende_de: []
bloqueia: []
skills_dispatched: []
integration_branch: integration/04-instrumentacao-qualidade
fluxos_qa: []
mutation_gate: false
mutation_rationale: ""
---

# QA-013 — PMD: ruleset curado e piloto de leitura interpretada

## Intake

- **Origem:** itens **#4 e #5** de `docs/sprints/04-instrumentacao-qualidade/backlog-s04.md`, **fundidos nesta task por decisão do humano em 2026-08-12** (ver §Decisão / abordagem, "Por que #4 e #5 viraram uma task só"). Origem primária: `04-metricas.md` Q6 (complexidade ciclomática) e Q7 (violações de ruleset) do experimento — nenhuma das duas métricas tem ferramenta no repositório.
- **Por quê agora:** PMD é pré-requisito de Q6 e Q7. É também a segunda das três ferramentas de qualidade da sprint (PIT ✅ QA-012, PMD aqui, JaCoCo no item #6), e a única que produz o **baseline de violações do código legado** que o item #7 vai precisar para calcular Δ.
- **Esforço:** médio. A instalação é pequena; o custo real é a **curadoria do ruleset**, que é iterativa e empírica — roda, lê, classifica, ajusta, roda de novo.
- **Riscos resumidos:** o ruleset default do PMD é barulhento e 191 classes de produção nunca passaram por análise estática, então o primeiro run produz um volume que pode soterrar o sinal. Mitigado por começar em subconjunto pequeno de regras e por tratar o legado como baseline documentado, nunca como dívida a corrigir aqui.

---

## Contexto

**O que já existe:**

- `financas_bot_telegram/pom.xml` — Java 21, `spring-boot-starter-parent`. Tem `pitest-maven` 1.25.9 (QA-012), **não tem PMD nem JaCoCo**. Verificado por leitura direta do arquivo em 2026-08-12.
- **191 classes de produção** e **82 classes de teste** (`find src/main/java -name '*.java' | wc -l`). Nenhuma jamais foi analisada estaticamente.
- `docs/runbooks/PRE-MERGE-CHECKLIST.md:16` declara hoje `lint` como **`Back: na (não há linter configurado)`**. Esta task torna essa linha falsa e por isso a atualiza.
- `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` tem a "Camada 2 — Verificação estática (humano + ferramentas)", hoje sem ferramenta automatizada, e a "Camada 1.5 — Teste mutante (PIT), sob demanda", que é o **padrão de documentação de comando a replicar** aqui.
- `docs/aprendizado/analise-estatica-pmd-checkstyle.md` — base conceitual já escrita e homologada. **O implementador deve lê-la antes de começar**; ela contém a decisão de não instalar Checkstyle, as categorias de ruleset do PMD, e a limitação da complexidade ciclomática.

**Decisões já tomadas pelo humano, que esta task não reabre:**

| Decisão | Onde foi registrada |
|---|---|
| Instalar **apenas PMD**, sem Checkstyle | backlog-s04 #4 (2026-08-10) |
| **Não bloqueante** no CI nem no pré-merge durante o experimento | backlog-s04 #4; `analise-estatica-pmd-checkstyle.md` §Cuidados |
| Ruleset **congelado com hash** antes do primeiro run do experimento | backlog-s04 #4 |
| Escopo por diff fica no item **#7**, não aqui | decisão do humano, 2026-08-12 |

---

## Decisão / abordagem

### Por que #4 e #5 viraram uma task só

O backlog separava "#4 — ruleset e escopo" de "#5 — piloto em escopo reduzido". A separação **não se sustenta na execução**: a curadoria de ruleset é empírica — não se escolhe regra a regra sem ver o que cada uma dispara no código real — e o próprio #5 declarava que a classificação "(c) regra que não queremos" **alimenta a curadoria do #4**. Como escrito, o #4 dependia de um output que só o #5 produzia.

Além disso, o README da sprint é explícito: *"uma task que instala PIT e reporta 'instalado, verde' não cumpre o objetivo desta sprint"*. Um #4 isolado entregaria exatamente isso.

**Consequência:** esta task absorve o #5. O item #5 sai do backlog quando esta mergear.

### O ciclo de curadoria — é este o trabalho

Não é "instalar plugin e rodar". O entregável central é um ruleset **justificado regra a regra**:

1. **Partir de um subconjunto pequeno**, não do default. Categorias `errorprone` e `design` são as que mais dizem sobre qualidade de código gerado por modelo; `codestyle` e `documentation` ficam de fora (formatação não discrimina LLM, e é o território que o repo decidiu não policiar).
2. **Rodar no conjunto do piloto** (abaixo), ler as violações uma a uma e classificar cada uma em **(a) problema real**, **(b) falso positivo**, **(c) regra que não queremos**.
3. **Ajustar o ruleset** com base em (c) — cada regra removida vira uma linha de justificativa no relatório. Repetir até o ruleset estabilizar.
4. **Congelar** e registrar o hash.

### O conjunto do piloto — o contraste é o ponto pedagógico

As **quatro classes do piloto do PIT**, mais **uma classe reconhecidamente complexa** para contraste:

| Classe | Linhas | Papel no piloto |
|---|---|---|
| `domain/service/LegendaParser` | 36 | regex puro e pequeno |
| `application/strategy/PaymentRequestStrategy` | 115 | strategy com construção de objeto de domínio |
| `application/strategy/PaymentProofStrategy` | 96 | par da anterior |
| `adapters/in/whatsapp/security/MetaSignatureValidator` | 70 | lógica de segurança, early-returns |
| `application/services/FecharMesServiceImpl` | **192** | **classe de contraste** — a maior das cinco; `PENDENCIAS-TECNICAS.md` já aponta acoplamento questionável nela (importa `DataIntegrityViolationException` na camada de aplicação) |

**Rodar só nas classes limpas não ensina nada** — sem violação, não se vê a ferramenta trabalhar. `FecharMesServiceImpl` entra justamente para produzir violação real e dar matéria à classificação.

Usar as mesmas quatro classes do PIT é deliberado: quando o JaCoCo entrar (item #6), os três relatórios — cobertura, mutation score, violações — vão descrever **o mesmo código**, e será possível comparar o que cada ferramenta enxerga e o que **nenhuma** enxerga.

### Escopo de execução: projeto inteiro, relatório apenas

O backlog mandava escopo por diff, consumindo o mecanismo do item **#7** — que está **bloqueado pela tag do marco zero e não existe**. Nesta task o PMD roda no **projeto inteiro**, em modo relatório, e o entregável inclui o **número absoluto de violações do legado**, que é exatamente o baseline de que o #7 precisa para calcular Δ depois.

### Não bloqueante — e o motivo não é conveniência

O plugin **não é amarrado a nenhuma fase do build** (mesmo padrão adotado para o PIT na QA-012): roda por invocação direta do goal. Build que falha por violação faz o agente **otimizar para o linter**, o que é interferência direta na variável que o experimento mede. **Relatório sim, gate não.**

### O legado é baseline, não dívida

Nenhuma violação pré-existente é corrigida nesta task. O número entra no relatório como medida; corrigir é decisão futura e separada.

---

## Escopo / arquivos

### Criar

- `financas_bot_telegram/pmd-ruleset.xml` — ruleset curado. **Cada regra incluída leva um comentário XML dizendo por que está lá**; cada categoria inteira excluída leva um comentário dizendo por que não está. Este arquivo é o entregável central da task — não é configuração acessória.

### Modificar

- `financas_bot_telegram/pom.xml` — adicionar `maven-pmd-plugin` com a versão em `<properties>` (padrão já usado por `pitest.version`), apontando para o ruleset, **fora do ciclo de vida padrão**, com formatos XML + HTML. Comentário no bloco explicando por que não está amarrado a `verify`.
- `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` — nova subseção na **Camada 2**, no mesmo formato da Camada 1.5 do PIT: comando, onde sai o relatório, como ler.
- `docs/runbooks/PRE-MERGE-CHECKLIST.md:16` — a linha do `lint` deixa de dizer `Back: na (não há linter configurado)`. Passa a descrever o comando do PMD e a registrar explicitamente que o gate é **informativo, não bloqueante**.

### Não tocar (escopo limitado)

- **Qualquer classe de produção ou de teste.** Nenhuma violação existente é corrigida aqui. Se o relatório revelar algo que pareça urgente, **vira pendência técnica**, não conserto nesta task.
- **`.github/workflows/`** — PMD não entra no CI nesta task.
- **Configuração do PIT** no `pom.xml` — bloco vizinho, não é para ser tocado.
- **CPD (copy-paste detector)** — faz parte do PMD, mas é ferramenta com pergunta própria e limiar próprio. Fora.

---

## Testes

**Não aplicável no sentido usual: a task não adiciona nem altera lógica de produção**, logo não há comportamento novo a cobrir com teste unitário. `testes_total` deve permanecer **422** e `testes_novos` deve ser **0** — qualquer desvio disso indica que a task saiu do escopo.

A verificação desta task é a **reprodutibilidade do relatório**, coberta pelos critérios de aceitação: o comando roda a partir de um clone limpo, produz relatório nos dois formatos, e o número de violações é estável entre execuções consecutivas sem mudança de código.

---

## Critérios de aceitação

- [ ] `maven-pmd-plugin` no `pom.xml`, versão em `<properties>`, **não amarrado a nenhuma fase do build**.
- [ ] `financas_bot_telegram/pmd-ruleset.xml` existe, e **cada regra ativa tem comentário justificando sua presença**; cada categoria excluída tem comentário justificando a ausência.
- [ ] O comando documentado roda limpo e gera relatório em **XML e HTML**.
- [ ] `mvn test` continua verde, com `testes_total = 422` e `testes_novos = 0`.
- [ ] **Baseline registrado:** número total de violações do projeto inteiro, quebrado **por categoria de regra** e **separando produção de teste** (a separação é exigência do D3 do item #7 — `Q7_producao` e `Q7_teste` num número só diluem um ao outro).
- [ ] **Complexidade ciclomática** das 5 classes do piloto reportada por método, com a **limitação declarada** — a métrica não distingue `switch` de 10 casos (alto e legível) de aninhamento de 4 níveis (parecido e ilegível).
- [ ] **Leitura interpretada:** cada violação nas 5 classes do piloto classificada em **(a) problema real**, **(b) falso positivo** ou **(c) regra que não queremos**, com **uma frase de justificativa por violação**.
- [ ] Toda classificação **(c)** tem correspondência no ruleset final — a regra foi removida, ou há justificativa escrita de por que foi mantida mesmo assim.
- [ ] **Critério de aprendizado:** o relatório demonstra, com exemplo concreto tirado do código deste projeto, **uma coisa que o PMD viu e o PIT não viu, e uma que o PIT viu e o PMD não viu**. Se as 5 classes não permitirem os dois exemplos, ampliar o conjunto até permitirem, e registrar a ampliação.
- [ ] **Hash do ruleset congelado** registrado no status report (`sha256` do `pmd-ruleset.xml`), para o pré-registro do experimento.
- [ ] `ROTEIRO-TESTES-BACKEND.md` documenta o comando na Camada 2, no formato da Camada 1.5.
- [ ] `PRE-MERGE-CHECKLIST.md` atualizado: `lint` do backend deixa de ser `na` e fica declarado como **informativo, não bloqueante**.
- [ ] Branch `feature/qa-013-pmd-ruleset-curado-e-piloto`, criada a partir de `integration/04-instrumentacao-qualidade`.
- [ ] Território respeitado — só `financas_bot_telegram/` e `docs/`.
- [ ] Status report em `docs/sprints/04-instrumentacao-qualidade/status/QA-013-pmd-ruleset-curado-e-piloto.md` com frontmatter válido.

---

## Fora de escopo (explicitamente)

| Item | Onde vive |
|---|---|
| Corrigir qualquer violação encontrada | pendência técnica; decisão futura |
| Escopo por diff / filtro de "classes tocadas" | item **#7** do backlog-s04 |
| PMD no CI, bloqueante ou não | depois do experimento; hoje contamina a medição |
| Checkstyle | **decidido: não entra**, 2026-08-10 |
| SpotBugs | não está nas métricas do experimento |
| CPD (copy-paste detector) | pergunta própria, limiar próprio |
| JaCoCo | item **#6** do backlog-s04 |
| Corrigir os testes fracos do piloto do PIT | item **#2** do backlog-s04 |

---

## Riscos & mitigações

| Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|
| Volume de violações do legado soterra o sinal — 191 classes nunca linteadas | **Alta** | Médio | Subconjunto pequeno de regras desde o início; leitura interpretada restrita às 5 classes do piloto; legado entra como número agregado, não como lista a percorrer |
| Curadoria vira caça a falso positivo e a task não fecha | Média | Médio | O critério de parada é o ruleset **estabilizar** (uma rodada sem nova classificação `(c)`), não "zero violações". Se após 3 rodadas não estabilizar, parar e registrar o estado como decisão pendente |
| Regra removida por ser barulhenta escondia problema real | Média | Médio | Toda remoção exige justificativa escrita no XML; o Reviewer valida as justificativas, não só o resultado |
| `maven-pmd-plugin` puxa versão de PMD incompatível com Java 21 | Baixa | Alto | Fixar a versão explicitamente e conferir no primeiro run que o parser aceita a sintaxe de Java 21 usada no projeto (records, switch expressions, text blocks). Se falhar, é bloqueio real — reportar, não contornar baixando `--release` |
| Complexidade ciclomática reportada sem a limitação vira número enganoso | Média | Médio | A limitação é **critério de aceitação**, não nota de rodapé |
| Task extrapola e corrige código | Média | Alto | `testes_total = 422` e `testes_novos = 0` são critério verificável; qualquer mudança em `src/main` aparece no diff e o Reviewer barra |

---

## Coordenação

- **Pode rodar em paralelo com:** item #2 (corrigir testes fracos do `LegendaParser`) e item #8 (gate `*IntegrationTest`). Nenhum toca o mesmo bloco do `pom.xml`. ⚠️ Se o #2 rodar junto, haverá conflito em `ROTEIRO-TESTES-BACKEND.md` — coordenar quem mergeia primeiro.
- **Depende sequencialmente de:** nada. A QA-012 já está em `develop`.
- **Bloqueia:** item **#7** (o baseline de violações e a separação produção/teste são insumo dele) e o pré-registro do experimento (precisa do hash do ruleset).
- **Atenção pro Reviewer:**
  - **O entregável é a justificativa, não o número.** Um ruleset sem comentário por regra reprova, mesmo que o build passe.
  - Conferir que **nenhuma classe de produção ou teste foi alterada** — é o desvio mais provável desta task.
  - Checar que a classificação `(c)` bate com o ruleset final: regra classificada como indesejada e mantida sem justificativa é inconsistência.
  - A QA-012 teve **5 achados, todos em prosa do relatório** (número contradizendo rótulo, evidência que não sustentava a afirmação). Esta task produz ainda mais prosa com número ao lado. **Rótulo qualitativo tem que bater com o dado quantitativo** ao lado dele.
  - Reproduzir o run por conta própria e conferir que o total de violações bate.
- **Atenção pro QA:** `fluxos_qa: []` — a task não altera comportamento de produto, não há fluxo a exercitar. **`qa_required: false`.**
- **Após merge:** remover o item **#5** do `backlog-s04.md` (absorvido); atualizar o item **#4** como concluído; registrar o baseline de violações e o hash do ruleset no `STATE.md`; abrir pendência técnica para cada violação `(a) problema real` que não for corrigida.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido conforme `_TEMPLATE-status.md`, e **revisão do Reviewer** (sessão separada — ADR 0005). `fluxos_qa: []`, logo **sem gate de QA**. `mutation_gate: false`, logo **sem gate de mutação** — a task não altera código Java de produção, não há classe alterada para entrar no denominador da medição. Implementador abre o PR da feature para `integration/04-instrumentacao-qualidade`; **não** abre PR direto para `develop`.

---

## Referências

- `docs/sprints/04-instrumentacao-qualidade/backlog-s04.md` — itens **#4** e **#5** (fundidos aqui), **#6** (JaCoCo), **#7** (classes tocadas, D3 e D4).
- `docs/aprendizado/analise-estatica-pmd-checkstyle.md` — **leitura obrigatória antes de começar**.
- `docs/sprints/04-instrumentacao-qualidade/plans/QA-012-piloto-pit-mutation-testing.md` — padrão de plano-piloto com leitura interpretada.
- `docs/sprints/04-instrumentacao-qualidade/status/QA-012-piloto-pit-mutation-testing.md` — padrão de relatório com leitura interpretada; ver §"Cobertura × mutation score" para o nível de argumentação esperado.
- `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` §"Camada 1.5 — Teste mutante (PIT)" — formato a replicar na Camada 2.
- `docs/PENDENCIAS-TECNICAS.md` §"`DataIntegrityViolationException` importada na application layer" — acoplamento já conhecido em `FecharMesServiceImpl`, a classe de contraste.
- `financas_bot_telegram/pom.xml` — bloco do `pitest-maven` como referência de plugin fora do ciclo de vida.
