# Review - QA-015 Fortalecer os testes fracos revelados pelos pilotos de PIT e JaCoCo

## Metadata
- sprint: `04-instrumentacao-qualidade`
- task_id: `QA-015`
- agent: reviewer
- date: `2026-08-16`
- plan: `docs/sprints/04-instrumentacao-qualidade/plans/QA-015-fortalecer-testes-revelados-pelos-pilotos.md`
- status: `docs/sprints/04-instrumentacao-qualidade/status/QA-015-fortalecer-testes-revelados-pelos-pilotos.md`

## Review Scope

Modo: `full-review`. Branch `feature/qa-015-fortalecer-testes-revelados-pelos-pilotos`, commits `db25d1c` (testes) e `a9a9369` (status), base `origin/integration/04-instrumentacao-qualidade`.

Diff revisado (`git diff origin/integration/04-instrumentacao-qualidade...HEAD`), 3 arquivos / 293 inserções / 0 remoções:

- `financas_bot_telegram/src/test/java/.../domain/service/LegendaParserTest.java` (+9)
- `financas_bot_telegram/src/test/java/.../application/strategy/PaymentRequestStrategyTest.java` (+22)
- `docs/sprints/04-instrumentacao-qualidade/status/QA-015-...md` (+262)

Ambiente do revisor: Windows 11, Maven 3.9.9, `openjdk version "23-ea" 2024-09-17` (build 23-ea+13-981) — **mesma máquina e mesma JVM** declaradas no status, o que torna os números comparáveis.

Tudo o que segue foi reexecutado nesta sessão; nenhum número foi aceito por citação.

## Premise Checks
- architecture_best_practice: pass — notes: estilo declarado **hexagonal**. A task vive inteira em `src/test/`; `git diff --name-only ...HEAD | grep src/main` não devolve nada. Nenhuma fronteira foi movida, nada foi tornado visível para viabilizar teste (`parsePedido` continua `private`, o teste entra por `process()`, método da interface `MensagemProcessingStrategy`). O `LegendaParserTest` exercita `parseTipo` como utilitário estático de domínio, sem Spring.
- technical_justifications_true: pass — notes: as três afirmações técnicas que sustentam a entrega foram verificadas no fonte e no relatório, não aceitas por plausibilidade. (1) Inalcançabilidade do `throw`: `supports()` (`PaymentRequestStrategy:45-49`) aplica `PEDIDO_PATTERN.matcher(caption.trim()).matches()` e `parsePedido` (`:84-87`) aplica **o mesmo pattern sobre o mesmo `dto.getCaption().trim()`**; `process()` tem **um único call site em produção** (`MensagemEntranteService:73`), imediatamente depois do filtro `.filter(s -> s.supports(dto))` da linha 58, sem mutação do DTO entre as duas chamadas — confirmado por `grep -rn "\.process(" src/main`. (2) Equivalência do sobrevivente de `LegendaParser:28` — ver §What Was Validated. (3) "O PIT não se moveu em `PaymentRequestStrategy`": o `mutations.xml` mostra os 11 mutantes `KILLED` e **nenhum deles é morto pelo teste novo** — o da linha 87 morre por `deveProcessarPedidoComTodosOsDados`, isto é, pelo lado oposto do desvio, exatamente como o status descreve; não há mutante gerado nas linhas 88-97.
- external_contract_source_verified: not-applicable — notes: a task não consome nem produz campo de sistema externo. Os dois testes usam entradas do contrato público de duas classes internas. Registrado também em §Blocked Validations.
- test_fixtures_from_real_source: pass — notes: `"pix 200"` e `"Apenas descricao sem valor"` derivam do contrato público (posição livre da palavra-chave; legenda que não começa por valor), não da implementação — não copiam a regex nem a ordem do `LinkedHashMap`. O ponto que importa: a fixture **não compartilha a premissa do código sob teste** — se `parseTipo` deixasse de aceitar `pos == 0`, o teste quebraria, e é precisamente isso que o mutante morto demonstra empiricamente (ver evidência do `killingTest` abaixo). O `hasMessageContaining("<valor> <descrição>")` do alvo 2 é acoplamento a texto da mensagem de produção, mas é asserção sobre a saída observável do método público, não sobre estado interno.
- no_process_doc_references_in_code: pass — notes: nenhum **nome** de teste, constante ou identificador cita documento de processo. O comentário de `LegendaParserTest` termina com "Lacuna medida pelo mutation testing na QA-012" — referência a artefato de `docs/` dentro de código, **exigida explicitamente pelo plano** (§Escopo/arquivos) e mantida por decisão de plano, não por descuido. O comentário é auto-contido (descreve o mutante e o desvio; a citação é só proveniência), então o dano que a regra endereça — referência sem significado para quem lê só o repo — não se materializa. Registrado como achado `low` L1, não como `fail`.

## Architecture Conformance Check
- declared style: hexagonal
- conformance: pass — notes: zero linha de `src/main/` no diff; nenhuma dependência nova; `pom.xml` intocado (`git diff --stat ...HEAD -- financas_bot_telegram/pom.xml` vazio). A violação hexagonal pré-existente da `PaymentRequestStrategy` (importa de `adapters/in/telegram/exception` e `adapters/out/s3/service`, com nota arquitetural no próprio fonte, linhas 22-25) **não foi introduzida nem agravada** por esta task — o teste novo importa `InvalidMessageFormatException` do mesmo pacote que a produção já importa. Não é achado desta entrega; segue como débito herdado.

## What Was Validated

- claim: **o PIT reporta `LegendaParser` em 7/8 depois da task** | evidence consulted: PIT reexecutado pelo revisor — `./financas_bot_telegram/mvnw org.pitest:pitest-maven:mutationCoverage -f financas_bot_telegram/pom.xml` → `BUILD SUCCESS`, `Line Coverage (for mutated classes only): 125/129 (97%)`, `Generated 42 mutations Killed 38 (90%)`, `Mutations with no coverage 1. Test strength 93%`. Parse do `target/pit-reports/mutations.xml`: `LegendaParser` **7 KILLED / 1 SURVIVED** de 8 gerados. Bate com o run 3 do status **em todos os dígitos**.
- claim: **o mutante que o teste novo mata é o da fronteira `pos >= 0`** | evidence consulted: os dois `ConditionalsBoundaryMutator` da linha 28 no `mutations.xml` são `index 52` (bloco 13) e `index 55` (bloco 14). O `index 52` está `KILLED` e seu `<killingTest>` é **exatamente** `LegendaParserTest.detectaPalavraChaveNoInicioDaLegenda()` — e é o único teste que o mata. O `index 55` está `SURVIVED` com `<killingTest/>` vazio. Isso confirma ao mesmo tempo (a) que o teste novo é o que produz o ganho e (b) que o "antes" era 6/8, sem precisar reexecutar o run anterior.
- claim: **o sobrevivente restante é `pos < posicaoMaisCedo` → `pos <=` e continua vivo** | evidence consulted: único não-`KILLED` de `LegendaParser` no `mutations.xml` — linha 28, `ConditionalsBoundaryMutator`, `index 55`, bloco 14 (o segundo operando relacional da linha). Como o `index 52` (`>=`) foi morto, o remanescente é necessariamente o `<`.
- claim: **a demonstração de equivalência escrita no status (§Alvo 1) se sustenta** | evidence consulted: reconstruí o argumento contra o fonte `LegendaParser.java:19-35`. (i) `<` e `<=` só divergem quando `pos == posicaoMaisCedo`; (ii) contra o valor inicial `Integer.MAX_VALUE` a igualdade exigiria `indexOf` devolver `Integer.MAX_VALUE`, o que exigiria string de comprimento **maior** que `Integer.MAX_VALUE` — impossível na JVM; (iii) contra um índice já gravado, exigiria duas chaves **distintas** ocorrendo no mesmo índice da mesma string, o que implica uma ser prefixo da outra; (iv) as chaves do `LinkedHashMap` são `boleto`/`pix`/`ted`/`agendamento`, com **iniciais distintas** (`b`,`p`,`t`,`a`), logo nenhuma é prefixo de outra; as chaves são únicas no mapa, então nem a repetição da mesma chave produz a igualdade. **Não derrubei a demonstração: ela é válida e é reconstruída de forma independente, não copiada da QA-012.** A ressalva escrita no status — a equivalência é condicional ao conteúdo de `PALAVRAS_CHAVE` — é a ressalva certa e está presente.
- claim: **`PaymentRequestStrategy` 8/8 branches e 49/49 linhas** | evidence consulted: JaCoCo reexecutado — `./financas_bot_telegram/mvnw clean jacoco:prepare-agent test jacoco:report -f financas_bot_telegram/pom.xml -Dtest='!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false` → `BUILD SUCCESS`, `Tests run: 376, Failures: 0, Errors: 0, Skipped: 0`. Parse de `target/site/jacoco/jacoco.xml`: `PaymentRequestStrategy` LINE **49/49**, BRANCH **8/8**, INSTRUCTION **185/185**; `LegendaParser` LINE **17/17**, BRANCH **10/10**, INSTRUCTION **70/70**. Critério 5 satisfeito e tabela do status confirmada nos três eixos.
- claim: **`cobertura_pct: 100`** | evidence consulted: (17 + 49) / (17 + 49) = 66/66 = 100% pela regra provisória da QA-014 (cobertura de linha das classes de produção tocadas). Números do parágrafo acima, medidos pelo revisor.
- claim: **`testes_total: 376` e `testes_novos: 2`** | evidence consulted: `Tests run: 376` no run do JaCoCo acima. `testes_novos`: o diff adiciona exatamente **dois** métodos `@Test` (`detectaPalavraChaveNoInicioDaLegenda`, `deveLancarInvalidMessageFormatExceptionQuandoLegendaNaoTemValor`), nenhum removido.
- claim: **a suíte completa é verde no CI e o commit citado é ancestral** | evidence consulted: `gh run view 31727999562 --json headSha,conclusion` → `success`, `headSha 15ae35e96...`; `gh run view 31727999562 --log | grep -o "Tests run: ..."` → `Tests run: 422, Failures: 0, Errors: 0, Skipped: 0`; `git merge-base --is-ancestor 15ae35e HEAD` → verdadeiro. A ressalva do status (48 testes `*IntegrationTest` não executáveis localmente por Docker/Testcontainers, débito de ambiente pré-existente) está corretamente declarada como **não regressão** e não é atribuída a esta task.
- claim: **gate `build`** | evidence consulted: `./financas_bot_telegram/mvnw -q -DskipTests package -f financas_bot_telegram/pom.xml` → exit `0`, sem saída de erro. Gate `build: ok` confirmado pelo comando canônico do checklist (que o status não registrou — ver L3).
- claim: **zero linha de `src/main/` no diff** | evidence consulted: `git diff --name-only origin/integration/04-instrumentacao-qualidade...HEAD` devolve só os 3 arquivos listados em §Review Scope; o filtro por `src/main` sai vazio (exit 1). Critério 7 satisfeito.
- claim: **`.claude/agents/backend.md` não entrou em commit da task** | evidence consulted: `git status --porcelain` mostra ` M .claude/agents/backend.md` (não-staged) e o diff do arquivo é de uma linha (`model: inherit` → `model: opus`); o arquivo **não aparece** no diff da branch. A declaração do status está correta e a regra de território de `.claude/` foi respeitada.
- claim: **§"Verificação de carregamento de skills" existe e responde aos três pontos** | evidence consulted: seção presente no status (linhas 169-193), com resposta **por skill** no ponto 1, tabela de três tentativas com **paths concretos** no ponto 2 (inclusive o path que falhou, que é o dado informativo) e ponto 3 respondido. Conferi por amostragem os fatos verificáveis que ela alega: `.claude/skills/writing-java-unit-tests/references/test-doubles-guidelines.md` **existe**; `grep -o -E "\]\((references|examples)/[^)]+\)"` devolve **5** ocorrências em `writing-java-unit-tests/SKILL.md` (linhas 50-74) e **10** em `developing-java-spring-applications/SKILL.md` (linhas 52-78) — os dois números batem com o status. Conforme o plano, **não avalio o conteúdo da resposta como critério de aceitação**; avalio que a seção existe e é específica. É.
- claim: **critério 9 — nenhum número novo contradiz número congelado** | evidence consulted: satisfeito para todos os números **medidos** (o "antes" 6/8, `123/129`, `37/42` reproduz QA-012 e QA-014; o "depois" que reexecutei bate com o status). **Uma exceção**, em §Próximos passos: o "teto" de `42` mutantes. Ver achado M1.

### Gate de mutação (ADR 0021) — auditoria da evidência

Auditado a partir da evidência registrada **e** confirmado contra o `mutations.xml` que eu mesmo gerei. Não reexecutei "o gate" — reexecutei o PIT, que é a fonte do número.

| Item auditado | Resultado |
|---|---|
| Evidência existe (comando + saída) | **ok** — comando e saída registrados no status (run 3) e reproduzidos por mim com resultado idêntico |
| Denominador correto (mortos ÷ **cobertos**, só as classes alteradas) | **ok** — `LegendaParser` 7/8 + `PaymentRequestStrategy` 11/11 = **18/19 = 94,7%**. Nenhum `NO_COVERAGE`/`TIMED_OUT` nessas duas classes, então cobertos = gerados aqui. `PaymentProofStrategy` (9/9) e `MetaSignatureValidator` (11 KILLED / 2 SURVIVED / 1 NO_COVERAGE) aparecem no run e **estão fora da conta**, como o critério manda |
| Piso de 80% | **ok** — 94,7% ≥ 80%. Contando também o desconto do equivalente demonstrado: 18/18 = 100% |
| Equivalentes demonstrados | **ok** — o único sobrevivente do escopo tem demonstração escrita no status e ela se sustenta sob verificação independente (acima). Observação favorável: o status manteve o equivalente **dentro** do denominador (18/19), que é a leitura conservadora — mais rigorosa que a autorizada pelo `mutation_rationale` do plano |
| Escopo não adulterado | **ok** — `targetClasses` do `financas_bot_telegram/pom.xml` continua com as **mesmas 4 classes** congeladas (`LegendaParser`, `PaymentRequestStrategy`, `PaymentProofStrategy`, `MetaSignatureValidator`); `git diff ...HEAD -- financas_bot_telegram/pom.xml` é **vazio**. As duas classes exercitadas pela task estão ambas medidas; nenhuma classe alterada ficou fora do escopo |

**Gate satisfeito.** Nenhum sinal de teste escrito para inflar métrica: o único teste que move o número é o do alvo 1, e ele mata um mutante que o próprio piloto havia classificado como **lacuna real, não equivalente**.

### Cobertura dos critérios de aceitação do plano

| # | Critério | Situação |
|---|---|---|
| 1 | Caso com palavra-chave no índice 0, passando | **ok** — `detectaPalavraChaveNoInicioDaLegenda`, verde no run de 376 testes |
| 2 | PIT reporta `LegendaParser` 7/8 (87,5%), vindo do relatório | **ok** — reproduzido pelo revisor via parse do `mutations.xml` |
| 3 | PIT antes e depois, mesma máquina/JVM, JVM registrada | **ok** — JVM `23-ea+13-981` registrada e confirmada por `java -version` nesta sessão; o "antes" é ainda corroborado pelo `killingTest` exclusivo do mutante `index 52` |
| 4 | Sobrevivente de `:28` vivo e declarado equivalente | **ok** — vivo no meu run, com demonstração verificada |
| 5 | `throw` coberto; 8/8 branches e 49/49 linhas | **ok** — medido pelo revisor |
| 6 | Status registra inalcançabilidade e natureza de teste de contrato | **ok** — §Alvo 2 do status e o comentário no próprio teste dizem as duas coisas, e a razão (mesmo `PEDIDO_PATTERN`) foi verificada no fonte. Nada no status descreve o teste como cenário de usuário; ao contrário, o status **corrige** a severidade herdada da QA-014 por esse exato motivo |
| 7 | `testes_novos` bate e diff sem `src/main/` | **ok** |
| 8 | Gate de mutação ≥ 80% | **ok** — 94,7% |
| 9 | Nenhum número novo contradiz número congelado sem explicação | **parcial** — ver M1 (o "teto 40/42" de §Próximos passos) |

## Findings (by severity)
- critical: nenhum.
- high: nenhum.
- medium:
  - **M1 — o "teto" que o status manda gravar no `STATE.md` está errado (40/42).** §Próximos passos do status diz: *"o teto das quatro classes passa de 39/42 para 40/42"*. O 39/42 do `docs/STATE.md:31` **não é o score atual, é o teto realista**, definido pela QA-012 (linha 264) como `42 − 2 equivalentes demonstrados − 1 inalcançável na prática`. Matar o mutante `LegendaParser:28 (>=)` **não move o teto**, porque esse mutante já estava contado como matável dentro dos 39; move o score real, de `37/42` para `38/42` — que é exatamente o `Killed 38` do meu run. Depois desta task os não-mortos são 4: `LegendaParser:28 <=` (equivalente), `MetaSignatureValidator:64` (equivalente), `MetaSignatureValidator:59` (`NO_COVERAGE`, inalcançável sem provider JCE falso) e `MetaSignatureValidator:28` (o warn de `app-secret`, **matável** com infra de captura de log que o repo não tem). Logo: **teto continua 39/42**, score atual **38/42**, e resta **um** mutante matável. Aplicar a instrução como está faria o `STATE.md` mentir — precisamente o risco que a instrução tentava evitar. Origem do erro: o próprio plano (§Coordenação > "Após merge") afirma 40/42; o status repetiu. Não afeta o gate de mutação nem o código.
- low:
  - **L1 — referência a artefato de `docs/` dentro de código.** O comentário de `LegendaParserTest` cita "QA-012". Foi **exigido pelo plano**, e o comentário é auto-contido, então não reprova. Há, porém, uma inconsistência interna: a decisão 4 do status afirma que os comentários descrevem o mutante *"sem citar seção de processo"* — o de `LegendaParser` cita a task. Sem ação obrigatória; se o planner quiser aderência estrita à regra de referências de processo em código, a citação sai sem perda (o resto do comentário já explica tudo).
  - **L2 — `commits:` no frontmatter lista só `db25d1c`.** O commit `a9a9369` (o do próprio status) não está lá. É a limitação óbvia do auto-referenciamento e o repo já tem o hábito de acrescentar o hash num commit posterior (ver `bf25f56` da QA-014). Vale fechar do mesmo jeito ao registrar o resultado desta revisão.
  - **L3 — `build: ok` sem o comando canônico registrado.** A §Evidência de execução tem PIT, `test` e JaCoCo (todos `BUILD SUCCESS`, o que implica compilação), mas não o `./mvnw -q -DskipTests package` do `PRE-MERGE-CHECKLIST`. Eu rodei: exit `0`. O gate está correto; falta só o registro.
  - **L4 — `lint: na` estica a letra do checklist.** O checklist define `na` como *"quando a task não toca código Java"*, e esta task toca (código de teste). A justificativa escrita — PMD congelado com `pmd.includeTests=false`, logo o run devolveria só as 22 violações de produção pré-existentes — é **correta e verificável no `pom.xml`** (`<pmd.includeTests>false</pmd.includeTests>`), e o gate é declaradamente informativo. Aceito como está; o que falta é regra escrita para o caso "tocou Java, mas fora do escopo configurado do linter" (ver §Technical Debt, D1).
  - **L5 — ambiguidade menor na §Evidência de execução.** O run 2 registra `Tests run: 23 ... (13 + 10)` para `-Dtest='LegendaParserTest,PaymentRequestStrategyTest'`; a ordem do parêntese é a inversa da ordem dos nomes no comando (`LegendaParserTest` = 10, `PaymentRequestStrategyTest` = 13). A soma está certa e confirmei as contagens por classe no fonte; é só leitura.

## Required Fixes

1. **(M1, obrigatório antes do merge — correção de uma linha no status)** Corrigir a §Próximos passos do status para: `LegendaParser` **6/8 → 7/8**; agregado das quatro classes **37/42 → 38/42** (`Killed 38`, `Test strength 93%`); **teto realista permanece 39/42**, com um mutante matável restante (`MetaSignatureValidator:28`, o warn de `app-secret`, dependente de infra de captura de log). Não instruir mudança do teto no `docs/STATE.md`. Registrar junto, para o planner, que **o plano também carrega o 40/42** em §Coordenação > "Após merge" e precisa da mesma correção — senão o erro sobrevive à correção do status.

Nenhuma outra correção é exigida. Nenhuma correção exigida toca código; **não é necessária nova rodada de revisão** depois de aplicar a fix 1 — basta o planner conferir o texto ao fechar o item #2 do backlog.

## Optional Improvements

- Acrescentar ao status o `./mvnw -q -DskipTests package` com sua saída, fechando o gate `build` pela evidência do próprio artefato em vez de por implicação (L3).
- Ao registrar o resultado desta revisão, incluir `a9a9369` em `commits:` (L2).
- O comentário do teste do alvo 2 é longo (6 linhas) para o padrão do arquivo, mas cada linha carrega informação não recuperável do código — mantenho como está. Se algum dia encolher, a frase que **não** pode sair é a da inalcançabilidade pelo dispatcher.

## Technical Debt Identified
- item: `PRE-MERGE-CHECKLIST.md` não tem valor definido para "a task tocou código Java, mas só fora do escopo configurado do linter" (aqui: só teste, com `pmd.includeTests=false`). | impact: força `na` com justificativa em prosa, e `na` passa a significar duas coisas diferentes — o campo perde poder de agregação, que é justamente o ganho que o schema do ADR 0007 promete. | suggested_action: planner define a regra (ex.: `na` vale também quando o diff está inteiro fora do escopo do linter, exigindo a justificativa por escrito) e escreve no checklist.
- item: o "teto 40/42" está **no plano** da QA-015 (§Coordenação > "Após merge"), não só no status. | impact: corrigir apenas o status deixa a fonte do erro viva, e o próximo que abrir o plano reintroduz o número no `STATE.md`. | suggested_action: planner corrige o plano junto, ou registra a errata ao fechar o item #2 do `backlog-s04.md`.
- item: os quatro débitos que o implementador registrou (resolução de path relativo dos assets de skill; `clean` do JaCoCo apagando `target/pit-reports/`; severidade superestimada do débito 2 da QA-014; redundância de validação em `PaymentRequestStrategy`). | impact: confirmo os quatro como reais — o terceiro eu verifiquei no fonte (o erro que o usuário final vê vem de `MensagemEntranteService:60-63`, não deste `throw`), e o quarto é o correlato estrutural do terceiro. | suggested_action: consolidar no `pendencias-tecnicas.md` da sprint sem revalidação; nenhum deles é ação desta task.
- item: `MetaSignatureValidator:28` (warn de `app-secret`) é hoje o **único** mutante matável ainda vivo nas quatro classes do `targetClasses`. | impact: é o próximo alvo natural de qualquer task que queira mover o número agregado, e depende de infraestrutura de captura de appender de log que o repo não tem. | suggested_action: planner decide se vale uma task de infra de teste de log; enquanto não valer, é o motivo pelo qual 38/42 e não 39/42.

## Blocked Validations / Uncertainty

- `external_contract_source_verified` está como **not-applicable**: a task não toca contrato de sistema externo (nenhum campo consumido ou produzido para Telegram, WhatsApp/Meta, S3 ou banco). Não é `not-checked` disfarçado — é ausência de superfície a checar.
- **Não reexecutei literalmente o run "antes"** (PIT sobre a base sem os dois testes). Verifiquei o "antes" por duas vias independentes que considero mais fortes que a repetição: o `<killingTest>` do mutante `index 52` é **exclusivamente** o teste novo (logo ele sobrevivia antes ⇒ 6/8), e a QA-014 já havia reproduzido `123/129` e `37/42` na base. Registro a diferença de método por transparência.
- **Não rodei a suíte de integração** (48 testes `*IntegrationTest`): Docker/Testcontainers indisponível nesta máquina, débito de ambiente pré-existente e declarado. Cobri o buraco pela via do CI: run `31727999562` = `success` em `15ae35e`, `Tests run: 422`, e `15ae35e` é ancestral de `HEAD`. Como esta task não altera `src/main`, o risco de regressão de integração é estruturalmente nulo.
- **Não rodei o PMD.** Aceitei `lint: na` com a justificativa do status, verificada contra `pom.xml` (`pmd.includeTests=false`, e o diff só tem teste). Ver L4.
- `.claude/agents/backend.md` está modificado na árvore de trabalho e **fora** dos commits da task. Não é achado desta entrega; é território do humano e fica anotado só para não ser lido como vazamento de escopo em auditoria futura.

## Review Artifact Path
- `docs/sprints/04-instrumentacao-qualidade/avaliacoes/review-QA-015-fortalecer-testes-revelados-pelos-pilotos.md`

## Plan Deviations
- Nenhum desvio de execução. O status declara `desvios: 0` e o diff confirma: alvos, arquivos e proibições do plano foram respeitados à risca, inclusive a mais fácil de violar — **nenhuma linha de produção** e nenhum item da tabela §Fora de escopo foi tocado (conferi o diff contra os oito itens dela). A decisão de escrever **um** teste no alvo 2 em vez de dois está dentro do que o plano autorizava ("um ou dois") e está justificada.
- Observação, não desvio: o único ponto em que o status **diverge do plano** é a favor do rigor — o plano autorizava deixar o equivalente fora do denominador do gate (`mutation_rationale`), e o status o manteve dentro (18/19 em vez de 18/18).

## Verdict
- approved-with-notes

Aprovado com uma correção obrigatória de texto (M1) e quatro observações `low`. A entrega é sólida no que importa: os dois números que eram o entregável — `LegendaParser` **7/8** no PIT e `PaymentRequestStrategy` **8/8 branches / 49/49 linhas** no JaCoCo — foram **reproduzidos por mim, não aceitos por citação**; o diff não tem uma linha de `src/main/`; o sobrevivente equivalente continua vivo, declarado, e sua demonstração resiste a verificação independente; o gate de mutação da ADR 0021 fecha em **18/19 = 94,7%** com denominador correto e sem qualquer sinal de adulteração de escopo. A declaração de inalcançabilidade do `throw` — o ponto onde era mais fácil exagerar — foi **verificada no fonte e no único call site de `process()`**, e o status foi mais longe do que o exigido, corrigindo a severidade herdada da QA-014 em vez de se apoiar nela. O achado M1 não contamina nenhum desses números: é uma instrução de atualização de artefato compartilhado que confunde "score" com "teto", e sai com uma linha.
