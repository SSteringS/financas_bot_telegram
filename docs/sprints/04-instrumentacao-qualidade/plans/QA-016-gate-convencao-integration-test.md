---
task: QA-016
titulo: "Gate que força a convenção *IntegrationTest, com ArchUnit"
sprint: 04-instrumentacao-qualidade
data_planejamento: 2026-08-18
branch_alvo: feature/qa-016-gate-convencao-integration-test
prioridade: alta
esforco: baixo
territorio: back
estado: pronto-pra-execucao
depende_de: []
bloqueia: []
skills_dispatched: [developing-java-spring-applications, writing-java-unit-tests]
integration_branch: integration/04-instrumentacao-qualidade
fluxos_qa: []
mutation_gate: false
mutation_rationale: ""
---

# QA-016 — Gate que força a convenção `*IntegrationTest`, com ArchUnit

## Intake

- **Origem:** item **#8** de `backlog-s04.md`, que nasceu do débito #3 da QA-012 — endossado pelo Reviewer como **o de maior prioridade daquela task**. Promovido ao registro global como "Convenção `*IntegrationTest` não é verificada por nada", prioridade **alta**.
- **Decisão de implementação:** **ArchUnit**, fechada com o humano em 2026-08-18. Ver §Decisão.
- **Por quê agora:** é a única coisa que hoje segura a viabilidade de todo o ferramental de mutation testing da sprint. Enquanto não existir, a sprint entrega três ferramentas apoiadas numa convenção que ninguém verifica.
- **Esforço:** baixo em linhas, baixo em decisão. A parte que exige cuidado é a **prova negativa** (critério 3), não o código.

---

## Contexto

### O problema, em uma frase

O `excludedTestClasses` do PIT no `pom.xml` filtra por `*IntegrationTest`. Essa exclusão **não é estilo, é requisito de viabilidade**: um teste com Testcontainers dentro do escopo do PIT significa **um container por mutante**, e o run não termina. Hoje **nada força a convenção** — nem lint, nem gate de CI, nem revisão automatizada.

### Por que a falha é perigosa

Ela é **silenciosa e diferida**. Um teste de integração novo com sufixo diferente escapa do filtro e nada acontece — até alguém rodar o PIT, possivelmente meses depois, e o run travar sem explicação óbvia. O custo do diagnóstico é muito maior que o custo do gate.

### Estado atual verificado

- `financas_bot_telegram/src/test/java/.../integration/AbstractIntegrationTest.java` existe e é a superclasse dos testes de integração (container singleton, Flyway real, auth JWT real).
- A suíte tem **11 classes `*IntegrationTest`, 48 testes**, hoje verdes no CI e vermelhas na máquina local por indisponibilidade do Docker ao Testcontainers (débito de ambiente já registrado no global).
- A convenção está **escrita em prosa** em `financas_bot_telegram/CLAUDE.md` §"Convenções que valem hoje", com a observação explícita de que **a convenção é hoje a única coisa que segura essa exclusão**.

---

## Decisão

### ArchUnit, e não reflection puro

Fechada com o humano em 2026-08-18. O item #8 do backlog colocava a pergunta nos termos certos:

> Se o objetivo for só este invariante, reflection puro basta. Se o repo pretende verificar as convenções que hoje só vivem em prosa no `CLAUDE.md`, ArchUnit paga a dependência com folga — **e é a única das duas que escala para isso**.

O humano escolheu **ArchUnit**, e a razão de peso é a segunda: a QA-013 produziu **segunda evidência independente** de que nenhuma das três ferramentas da sprint (PIT, PMD, JaCoCo) mede conformidade arquitetural — está registrado no global como débito próprio. O repo tem uma fila de invariantes candidatos já escritos em prosa: JPA vive no adapter e não no domínio; adapter não conhece outro adapter; nada novo em `application/usecases/`; `domain/model/` e não `domain/entity/` para POJO novo.

**Custo aceito:** uma dependência nova de escopo `test` (`archunit-junit5`) no build durante o experimento.

### O que esta task NÃO faz

**Nenhum outro invariante entra aqui.** O gate da convenção de nome é o que destrava o PIT e **não deve esperar** pela discussão dos outros. Se ArchUnit provar seu valor, cada invariante adicional vira item próprio de backlog — com sua própria decisão sobre falso positivo em código legado.

---

## Escopo / arquivos

### Modificar

- `financas_bot_telegram/pom.xml` — **uma única adição**: a dependência `archunit-junit5`, escopo `test`.
  - ⚠️ **Não tocar em mais nada do `pom.xml`.** `targetClasses`, `excludedTestClasses`, `pmd-ruleset`, JaCoCo e surefire estão **congelados com baseline** pelas QA-012/013/014. Qualquer outra alteração aqui é finding do Reviewer.

### Criar

- `financas_bot_telegram/src/test/java/.../architecture/ConvencaoTesteIntegracaoTest.java` (nome do pacote e da classe a critério do implementador, respeitando as convenções do módulo).
  - ⚠️ **O nome desta classe não pode terminar em `IntegrationTest`.** Ela é teste unitário de convenção, roda em milissegundos e **precisa** estar dentro do escopo do `mvn test` comum. Terminar em `IntegrationTest` a faria ser excluída pelo próprio filtro que ela existe para proteger — armadilha circular.

### Não tocar

- Nenhuma classe de `src/main/` — **esta task não altera código de produção.**
- Nenhum teste existente. Se algum teste hoje **viola** a regra, isso é um **achado**, não um conserto: reportar no status e escalar ao planner antes de renomear qualquer coisa.
- `pmd-ruleset.xml`, `lombok.config`, workflows de CI.

---

## Regra a implementar

Falha o build quando uma classe do classpath de teste satisfaz **qualquer** das condições abaixo **e** não termina em `IntegrationTest`:

1. está anotada com `@Testcontainers`;
2. estende `AbstractIntegrationTest` (direta ou transitivamente).

A mensagem de erro **deve nomear a classe violadora**. É o critério de aceitação do backlog, e é o que separa um gate útil de um gate que só diz "falhou".

**Casos-limite a tratar explicitamente no teste ou a declarar no status:**

- A própria `AbstractIntegrationTest` satisfaz a condição 2 por identidade — e já termina em `IntegrationTest`, então passa. Confirmar, não assumir.
- Classe abstrata intermediária, se existir.
- Classe anotada com `@SpringBootTest` mas **sem** Testcontainers: **fora da regra nesta task**. Ampliar o predicado é decisão nova; se o implementador achar casos assim, reporta como débito e não amplia.

---

## Testes

- O próprio ArchUnit é o teste. Não escrever teste do teste.
- **A prova negativa é obrigatória e é o coração desta entrega** (critério 3). Sem ela, o que existe é uma regra que nunca falhou — e uma regra que nunca falhou não é distinguível de uma regra que não funciona.

---

## Critérios de aceitação

1. `./mvnw test` verde com a regra ativa, e o tempo total da suíte **não cresce de forma perceptível** (a varredura é de classpath, não sobe contexto Spring). Registrar o antes e o depois.
2. A regra cobre **as duas** condições (`@Testcontainers` e herança de `AbstractIntegrationTest`), e o status diz **como** cada uma foi verificada.
3. **Prova negativa executada:** renomear temporariamente uma classe `*IntegrationTest` existente para um nome violador, rodar `./mvnw test`, **observar a falha**, e registrar no status **a mensagem literal** — que precisa nomear a classe violadora. Depois desfazer o rename. O status traz o comando, a saída e a confirmação de que o rename foi revertido (`git status` limpo).
4. A classe nova **não** termina em `IntegrationTest` e **não** foi excluída por `excludedTestClasses`.
5. O `pom.xml` tem **exatamente uma** adição: a dependência do ArchUnit. Diff do `pom.xml` conferido linha a linha no status.
6. A versão do `archunit-junit5` adotada está declarada no status **com a fonte** de onde saiu (não "a mais recente" — o número, e como foi obtido).
7. `cobertura_pct: na`, **com motivo escrito**: a task não altera classe de produção. (Regra do valor `na`, `PRE-MERGE-CHECKLIST` §Regra do valor `na`.)
8. O status declara se **algum teste existente viola a regra hoje**. Se violar, a task **não renomeia** — reporta e escala.

---

## Quality Gates

- **review_required:** `true` — mudança de código, e mexe no `pom.xml`, que está congelado com baseline. Revisão independente obrigatória.
- **qa_required:** `false`
- **qa_rationale:** nenhum comportamento observável pelo usuário muda. A entrega é um invariante de build, e o critério de aceitação já é uma **prova executável** (a prova negativa do critério 3) que o Reviewer reproduz. Acionar o QA duplicaria a mesma verificação. Registrar como `not-applicable`, não como pulado.
- **mutation_gate:** `false`
- **mutation_rationale:** a task **não altera nenhuma classe de produção** — o escopo de medição definido pela ADR 0021 ("apenas as classes alteradas pela task") ficaria vazio, e o PIT não teria o que medir. Adotar o gate aqui exigiria redefinir a regra de escopo, o que é decisão separada. ⚠️ **Se o humano decidir o contrário, este campo muda antes do dispatch** — o plano não presume a resposta.

---

## Riscos & mitigações

| Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|
| ArchUnit não enxergar as classes de teste (importa só `main` por padrão em algumas configurações) | **média** | alto — regra passa vazia e ninguém percebe | **É por isso que a prova negativa é critério de aceitação.** Uma regra que não importa nada passa sempre. Sem a prova, esta task pode ser dada como pronta sem funcionar |
| Falso positivo em classe legada | baixa | médio | Critério 8: reportar, não renomear |
| Ampliar o `pom.xml` além da dependência | baixa | **alto** | Critério 5 + item de auditoria do Reviewer. Ferramental congelado com baseline |
| Nomear a classe nova terminando em `IntegrationTest` | baixa | alto (armadilha circular) | Declarado em §Escopo e no critério 4 |
| Suíte local vermelha por Docker indisponível mascarar o resultado | **alta** | médio | Débito de ambiente conhecido. Rodar o recorte unitário e declarar; a suíte completa é verde no CI e é lá que o número final se confirma |

---

## Dependências

- **Nenhuma.** Não depende da BE-031 nem toca em arquivo que ela toca — as duas podem correr em qualquer ordem.
- Depende de a mudança do `.claude/agents/backend.md` (commit `0305813`) ter chegado à `integration/04` **antes** do dispatch, senão o implementador roda com a config antiga.

---

## Execução — ordem sugerida

1. Criar a branch a partir de `origin/integration/04-instrumentacao-qualidade`.
2. Adicionar a dependência e **rodar a suíte antes de escrever a regra**, para ter o tempo-base do critério 1.
3. Escrever a regra cobrindo as duas condições.
4. **Prova negativa** (critério 3), com a saída literal capturada.
5. Desfazer o rename, conferir `git status` limpo, rodar a suíte de novo.
6. Status report, com o diff do `pom.xml` conferido linha a linha.
7. Reviewer.

---

## Handoff Packet

- **objetivo:** tornar verificável, no `mvn test`, a convenção `*IntegrationTest` da qual todo o ferramental de mutation testing depende.
- **fronteiras:** `financas_bot_telegram/pom.xml` (uma dependência) + uma classe de teste nova. Zero linha de `src/main/`. Nenhum outro invariante.
- **critérios:** §Critérios de aceitação, 8 itens; o de número 3 é o que prova que a entrega funciona.
- **arquitetura:** hexagonal (módulo backend); a classe nova é teste de convenção, não pertence a nenhuma camada de produção.
- **gates:** `review_required: true` · `qa_required: false` · `mutation_gate: false`
- **riscos:** §Riscos, com destaque para o primeiro — regra que não importa classe nenhuma passa sempre.
- **passos:** §Execução.

---

## Pontos de aprovação humana

1. **Gate de mutação** — ✅ perguntado em 2026-08-18. O plano registra `false` com a justificativa de escopo vazio. **Não adotado.**

---

## Fora de escopo (explicitamente)

| Item | Por quê |
|---|---|
| Qualquer outro invariante arquitetural (JPA no adapter, adapter × adapter, `usecases/` legado, `domain/model` × `domain/entity`) | O gate da convenção de nome destrava o PIT e não espera pela discussão dos outros. Cada um vira item próprio, com sua decisão sobre legado |
| Renomear teste que hoje viole a regra | Reportar e escalar. Rename de classe de teste em massa não é decisão de quem implementa o gate |
| Plugar ArchUnit no CI como step separado | Já roda dentro do `mvn test`, que o CI executa. Step novo seria redundante |
| Ampliar `excludedTestClasses` ou `targetClasses` | Ferramental congelado com baseline pelas QA-012/013/014 |

---

## Referências

- `docs/sprints/04-instrumentacao-qualidade/backlog-s04.md` — item **#8**, com a tabela de decisão ArchUnit × reflection.
- `docs/PENDENCIAS-TECNICAS.md` — "Convenção `*IntegrationTest` não é verificada por nada" (prioridade alta) e "Nenhuma ferramenta do repo mede conformidade arquitetural".
- `financas_bot_telegram/CLAUDE.md` — §"Convenções que valem hoje", onde a convenção está escrita e onde consta que ela é hoje a única coisa que segura a exclusão do PIT.
- `docs/runbooks/PRE-MERGE-CHECKLIST.md` — §Regra do valor `na`.
