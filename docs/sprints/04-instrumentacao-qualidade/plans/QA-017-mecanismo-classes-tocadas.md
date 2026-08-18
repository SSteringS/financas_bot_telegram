---
task: QA-017
titulo: "Mecanismo de classes tocadas — escopo por diff compartilhado"
sprint: 04-instrumentacao-qualidade
data_planejamento: 2026-08-18
branch_alvo: feature/qa-017-mecanismo-classes-tocadas
prioridade: alta
esforco: medio
territorio: back
estado: pronto-pra-execucao
depende_de: []
bloqueia: [B7, collect_usage]
skills_dispatched: []
integration_branch: integration/04-instrumentacao-qualidade
fluxos_qa: [lista-canonica-de-classes-tocadas]
mutation_gate: false
mutation_rationale: ""
---

# QA-017 — Mecanismo de "classes tocadas": escopo por diff compartilhado

## Intake

- **Origem:** item **#7** de `backlog-s04.md`. As decisões **D1 a D6 já foram fechadas com o humano em 2026-08-10** — este plano **não as reabre**, apenas as implementa.
- **Por quê agora:** quatro métricas do experimento são definidas sobre o conjunto de classes alteradas — **Q2** (cobertura), **Q3** (mutation score), **Q6** (complexidade), **Q7** (violações) — e **nenhuma tem mecanismo**. É pré-requisito das quatro, e por isso é a próxima peça da frente de instrumentação.
- **Esforço:** médio. O script é pequeno; o que custa são os casos-limite e **uma incógnita real** na integração com o PIT, descrita em §Incógnita bloqueante parcial.
- **Risco principal:** uma lista errada produz número **plausível e errado**, e ninguém percebe. É o mesmo modo de falha que o `backend.md` acabou de ganhar guardrail para evitar na cobertura — só que aqui ele fica automatizado e passa a valer para quatro métricas de uma vez.

---

## Contexto

### O que o mecanismo resolve

Hoje o recorte de "classes que a task alterou" é feito **na mão**, tanto no gate de mutação quanto na medição de cobertura. Isso está escrito, com todas as letras, no `.claude/agents/backend.md`:

> That recorte is manual today and is the most fragile step of this procedure — a wrong class list produces a plausible and wrong number.

O mecanismo transforma isso em **uma lista canônica, derivada do git, persistida e auditável**. Consequência direta: os dois gates passam a usar **a mesma lista**, e ninguém consegue encolher o denominador para melhorar o próprio número.

### Decisões já fechadas (2026-08-10) — implementar, não rediscutir

| # | Decisão |
|---|---|
| **D1** | Base de comparação = **a tag do marco zero, fixa**. Não `develop`, não merge-base |
| **D2** | Granularidade = **classe**, para todas as métricas. Classe alterada em uma linha entra inteira |
| **D3** | Produção e teste medidos **separadamente** — `Q6_producao`/`Q6_teste`, `Q7_producao`/`Q7_teste` |
| **D4** | **PIT restringe na entrada** (`targetClasses`). **JaCoCo e PMD rodam completo e filtram na saída** — uma implementação de filtro só |
| **D5** | Artefatos do run em **`docs/runs/<RUN_ID>/`**; arquivamento em `docs/experiments/.../runs/` só ao fim do experimento |
| **D6** | Deletada **não entra**; renomeada **entra**; tocada só em import ou comentário **entra**; run sem classe de produção = **`na`**, nunca `0` |

---

## Incógnita bloqueante parcial — ler antes de começar

### A base de comparação da D1 ainda não existe

A D1 fixa a base na **tag do marco zero**. O humano decidiu em **2026-08-18** que essa tag só é criada **depois de encerrar a sprint 04** — ou seja, **depois desta task**.

**Consequência de design, não de cronograma:** o script **não pode** ter a tag embutida. Ele recebe a base como **parâmetro obrigatório** e é exercitado, nesta task, contra qualquer ref (um commit antigo qualquer serve). Quando a tag existir, ela vira o valor passado — nada no script muda.

⚠️ **Um script que assume que a tag existe não é entregável hoje e falharia em silêncio ao ser exercitado.**

### O `targetClasses` do PIT está congelado — e a D4 exige mexer nele por run

A D4 manda o PIT restringir **na entrada**, via `targetClasses`. Mas o `targetClasses` do `pom.xml` está **congelado com baseline** pela QA-012 (4 classes, 42 mutantes), e a BE-031 acabou de ser desenhada explicitamente para **não** ampliá-lo.

Existe aqui uma incógnita que **precisa ser resolvida por verificação, antes de escrever a integração com o PIT**:

> O `pitest-maven` aceita sobrescrever `targetClasses` **por linha de comando**, ou a única via é editar o `pom.xml`?

⚠️ **Não assumir que aceita.** Precedente medido neste repo: a QA-013 descobriu que **`-Dpmd.rulesets` não existe** — trocar de ruleset exige editar o `pom.xml`, e o Reviewer precisou montar projetos-cópia para reproduzir números. O mesmo pode valer para o PIT.

**Encaminhamento obrigatório:**
- **Se aceitar por linha de comando:** implementar a integração, registrando o comando exato e como a aceitação foi verificada.
- **Se NÃO aceitar:** **parar a integração com o PIT e escalar ao planner.** Editar o `pom.xml` a cada run significa mudar, entre runs, um arquivo que o experimento precisa manter constante — é decisão de desenho experimental, não de implementação. Os itens 1, 2 e a integração com JaCoCo/PMD continuam entregáveis e a task fecha `parcial` com o motivo escrito.

Esta é a razão de `esforco: medio` e não `baixo`.

---

## Escopo / arquivos

### Criar

- Script único (Python, para casar com o `collect_usage.py` já previsto na mesma frente), que a partir de `git diff <BASE_REF>..HEAD` produz a **lista canônica** de classes tocadas, **separando produção de teste** (D3).
- Persistência da lista por run em `docs/runs/<RUN_ID>/` (D5), em formato parseável, contendo no mínimo: `BASE_REF` usado, `HEAD` resolvido em SHA, data/hora, lista de produção, lista de teste, e o marcador `na` quando a lista de produção for vazia (D6).
- Testes do script cobrindo **cada caso-limite da D6**, sobre históricos git construídos para o teste — não sobre o histórico real do repositório, que muda embaixo do teste.

### Modificar

- Nada em `src/main/`. Nada em `src/test/`. Nada no `pom.xml` **até** a incógnita do PIT ser resolvida.

### Decisão de território a fechar (ação do planner, não do implementador)

O script não mora em `financas_bot_telegram/` nem em `frontend/`. A tabela de territórios do `PRE-MERGE-CHECKLIST` **não lista `scripts/`** — o gate `territorio` não sabe classificar esta task. O `README.md` da sprint já define o fluxo ("scripts do repositório → branch + PR + Reviewer"), mas a tabela não foi atualizada.

**Encaminhamento:** o planner acrescenta `scripts/` ao território de `claude-back` na tabela **antes do dispatch**. O implementador declara `territorio: ok` apoiado nessa linha; se ela não estiver lá quando ele for escrever o status, ele reporta em vez de improvisar.

---

## Critérios de aceitação

1. O script recebe a **base como parâmetro obrigatório** e falha com mensagem clara se ela não for passada ou não resolver para um commit válido. **Não** existe tag embutida no código.
2. A saída separa **produção** de **teste** (D3), e a separação é declarada — como o script decide o que é teste, e por qual regra.
3. Cada caso-limite da **D6** tem teste próprio, e o status mostra o resultado de cada um: **deletada** (não entra), **renomeada** (entra), **tocada só em import/comentário** (entra), **run sem classe de produção** (`na`, nunca `0`).
4. A lista é **persistida** em `docs/runs/<RUN_ID>/`, com `BASE_REF` e `HEAD` em SHA resolvido, para que a análise seja reproduzível meses depois **sem re-rodar git**.
5. **Filtro de saída único** (D4) usado tanto por JaCoCo quanto por PMD — duas chamadas ao mesmo filtro, não duas implementações. O status demonstra que é a mesma.
6. **Integração com o PIT:** ou implementada, com o comando e a verificação registrados; ou **não implementada**, com a resposta da incógnita escrita e a task fechando `parcial`. As duas saídas são aceitáveis; **inventar um comando que não foi verificado, não**.
7. O status registra o valor produzido pelo script para o **próprio diff desta task** — que deve ser uma lista de produção **vazia** (`na`), já que a task não altera `src/main/`. É o primeiro uso real do mecanismo, e é um teste honesto: se der diferente de `na`, o script está errado.
8. `cobertura_pct: na`, **com motivo escrito**: a task não altera classe de produção (regra do valor `na`).
9. O status **não afirma** que as quatro métricas passaram a existir. Elas passam a **ter mecanismo**; calculá-las é trabalho de outra task.

---

## Quality Gates

- **review_required:** `true` — código novo, e código do qual dependem quatro métricas.
- **qa_required:** `true`
- **qa_rationale:** o modo de falha desta entrega é **silencioso e caro**: uma lista errada produz número plausível e errado em quatro métricas ao mesmo tempo, e nenhuma das ferramentas tem como perceber. Os casos-limite da D6 são exatamente onde esse erro mora. O QA projeta a matriz de casos **independentemente de quem escreveu o script** — que é o ponto: quem escreveu o filtro é a pior pessoa para adivinhar o que ele não cobre. Fluxo: `lista-canonica-de-classes-tocadas`.
- **mutation_gate:** `false`
- **mutation_rationale:** a task não altera código Java — o PIT não tem o que medir. Gate não se aplica; não foi perguntado ao humano por esse motivo.

---

## Riscos & mitigações

| Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|
| Script assumir a tag do marco zero, que ainda não existe | **alta** se o plano não for lido | alto | Critério 1, e §Incógnita. Base é parâmetro obrigatório |
| PIT não aceitar `targetClasses` por linha de comando | **média** | alto | Critério 6: escalar, fechar `parcial`, não editar `pom.xml` por conta própria. Precedente do `-Dpmd.rulesets` |
| Teste do script escrito sobre o histórico real do repo | média | médio | Histórico real muda embaixo do teste. Construir históricos de teste |
| Duas implementações de filtro (uma para JaCoCo, outra para PMD) | média | médio | Critério 5. A D4 escolheu "filtrar na saída" **exatamente** para ter uma implementação só |
| `0` gravado onde deveria ser `na` | média | **alto** | D6 explica os três estragos: run parece péssimo sem ter sido medível, média puxada para baixo, e ninguém distingue "não matou mutante" de "não havia mutante" |
| Task se convencer de que entregou as métricas | média | médio | Critério 9 |

---

## Dependências

- **Não depende** da BE-031 nem da QA-016 — arquivos disjuntos, pode correr em qualquer ordem em relação às duas.
- **Bloqueia** a frente de custo (hooks `SubagentStart`/`SubagentStop` e `collect_usage.py`): as duas escrevem em `docs/runs/<RUN_ID>/` e precisam do mesmo `RUN_ID` e do mesmo layout. Fechar este primeiro evita duas convenções paralelas.
- Depende de a mudança do `.claude/agents/backend.md` (commit `0305813`) ter chegado à `integration/04` antes do dispatch.

---

## Execução — ordem sugerida

1. Branch a partir de `origin/integration/04-instrumentacao-qualidade`.
2. **Resolver a incógnita do PIT antes de escrever qualquer integração** e registrar a resposta com o comando usado.
3. Script + separação produção/teste (D3), com a base como parâmetro.
4. Testes dos casos-limite da D6, sobre históricos construídos.
5. Persistência em `docs/runs/<RUN_ID>/` (D5).
6. Filtro de saída único, plugado em JaCoCo e PMD (D4).
7. Integração com o PIT **ou** o registro de por que ela não entrou.
8. Rodar o script sobre o próprio diff da task (critério 7).
9. Status, QA, Reviewer.

---

## Handoff Packet

- **objetivo:** produzir a lista canônica, persistida e auditável, de classes tocadas por um run, separando produção de teste, e plugá-la nas ferramentas conforme a D4.
- **fronteiras:** script novo + seus testes + persistência em `docs/runs/`. Zero linha de `src/main/` e de `src/test/`. `pom.xml` só se a incógnita do PIT autorizar, e nem assim sem escalar.
- **critérios:** §Critérios de aceitação, 9 itens.
- **arquitetura:** fora do módulo Java; script de repositório. Sem impacto em camada de produção.
- **gates:** `review_required: true` · `qa_required: true` (fluxo `lista-canonica-de-classes-tocadas`) · `mutation_gate: false`
- **riscos:** §Riscos; os dois primeiros são os que decidem se a entrega presta.
- **passos:** §Execução — com o passo 2 antes de tudo.

---

## Fora de escopo (explicitamente)

| Item | Por quê |
|---|---|
| Calcular Q2, Q3, Q6, Q7 | Esta task entrega o **mecanismo de recorte**, não as métricas |
| Emendar a "regra de leitura conjunta" no `04-metricas.md` e no pré-registro | É edição de doc do experimento — **ação do planner**, registrada como follow-up |
| Registrar `test strength` como métrica de primeira classe no pré-registro | Idem — decisão do humano em 2026-08-18, ação do planner |
| Hooks de custo e `collect_usage.py` | Frente vizinha, task própria. Esta apenas fixa o layout de `docs/runs/<RUN_ID>/` que eles vão reusar |
| Editar `pom.xml` para ampliar `targetClasses` por run | Muda entre runs um arquivo que o experimento precisa manter constante. Decisão de desenho experimental |
| Arquivar runs em `docs/experiments/.../runs/` | A D5 coloca isso **ao fim do experimento**, não agora |

---

## Referências

- `docs/sprints/04-instrumentacao-qualidade/backlog-s04.md` — item **#7**, com D1–D6, a tabela da D3, a justificativa da D6 e a regra de leitura conjunta.
- `docs/sprints/04-instrumentacao-qualidade/status/QA-012-piloto-pit-mutation-testing.md` — piso de custo do PIT e `targetClasses` congelado.
- `docs/sprints/04-instrumentacao-qualidade/status/QA-013-pmd-ruleset-curado-e-piloto.md` — o precedente do `-Dpmd.rulesets` inexistente e o baseline congelado com hash.
- `.claude/agents/backend.md` — §`Coverage Measurement`, onde consta que o recorte é manual hoje e é o passo mais frágil do procedimento.
- `docs/runbooks/PRE-MERGE-CHECKLIST.md` — §Regra do valor `na`.
