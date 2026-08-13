# Review - QA-014 Piloto do JaCoCo: cobertura medida e comparada com PIT e PMD

## Metadata
- sprint: `04-instrumentacao-qualidade`
- task_id: `QA-014`
- agent: reviewer
- date: `2026-08-13`
- plan: `docs/sprints/04-instrumentacao-qualidade/plans/QA-014-piloto-jacoco-cobertura.md`
- status: `docs/sprints/04-instrumentacao-qualidade/status/QA-014-piloto-jacoco-cobertura.md`

## Review Scope

Modo: `full-review`. Sessão somente-leitura sobre `feature/qa-014-piloto-jacoco-cobertura`, commits `ff78be3` e `1c1f216`, diff contra `origin/integration/04-instrumentacao-qualidade` (`40877a9`).

Seis arquivos no diff, nenhum deles `.java`:

| Arquivo | Natureza |
|---|---|
| `financas_bot_telegram/pom.xml` | `jacoco-maven-plugin` 0.8.12, property de versão, sem `<executions>` |
| `financas_bot_telegram/lombok.config` | novo |
| `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` | nova §Camada 1.6 |
| `docs/runbooks/PRE-MERGE-CHECKLIST.md` | linha `cobertura_pct` |
| `docs/templates/_TEMPLATE-status.md` | comentário de `cobertura_pct` |
| `docs/sprints/.../status/QA-014-piloto-jacoco-cobertura.md` | status |

Todas as medições do status foram **regeradas nesta sessão** (JaCoCo com e sem `lombok.config`, PIT, PMD, `package`, log do CI). Nada foi aceito por citação.

**O entregável desta task é o relatório**, como na QA-013 — logo a revisão pesa a exatidão das afirmações tanto quanto a do `pom.xml`.

## Premise Checks
- architecture_best_practice: pass — notes: estilo declarado hexagonal; nenhuma classe Java criada ou alterada, logo não há colocação de classe a julgar. O plugin segue o padrão que a sprint vem mantendo (PIT, PMD): declarado em `<build><plugins>` com versão em `<properties>` e **sem `<executions>`**, portanto não amarrado a fase nenhuma. Verificado no diff e por `mvn -q -DskipTests package` (exit 0) — o build de todo mundo não ficou mais caro.
- technical_justifications_true: **fail** — notes: a maioria das justificativas técnicas foi verificada e é verdadeira (ver §What Was Validated), mas a §"Correção de fato registrada no backlog" afirma um fato **falso e verificável como falso em um comando**, e usa esse fato para inverter um item de backlog que estava correto. Detalhe em `high` A1. Uma segunda justificativa (a do parent do surefire) está formalmente correta mas apoiada em evidência mais fraca do que a disponível — `low` B4.
- external_contract_source_verified: pass — notes: a task não consome nem produz campo de sistema externo. Os únicos "contratos" em jogo são os esquemas de relatório das ferramentas (`jacoco.xml`, HTML do PIT, `pmd.xml`) e o log do GitHub Actions; todos foram lidos das fontes autoritativas regeradas nesta sessão, não de transcrição.
- test_fixtures_from_real_source: pass — notes: `testes_novos: 0`, nenhum teste escrito — não há fixture a auditar. O análogo aqui é o risco de os números serem copiados da QA-012/QA-013 em vez de medidos: **não foram**. Rodei PIT e PMD do zero e os baselines saíram idênticos (`123/129 (95%)`, `42 mutações / 37 mortos (88%)`, 22 violações). Os dois números do Lombok também foram reproduzidos independentemente (ver §What Was Validated).
- no_process_doc_references_in_code: pass — notes: os comentários do `pom.xml` e do `lombok.config` explicam o **porquê técnico** (semântica do `argLine`, `@lombok.Generated`, motivo de não haver `jacoco:check`) sem apontar para plano, ID de task ou artefato de processo. Nenhum `QA-014` aparece em arquivo de código.

Um item `not-checked` também apareceria em `Blocked Validations / Uncertainty`. Não há nenhum.

## Architecture Conformance Check
- declared style: hexagonal
- conformance: pass — notes: a arquitetura não foi tocada. Registro adjacente, para o planner: o relatório mostra que a fronteira hexagonal aparece nítida no dado — os pacotes de `adapters/out` e `adapters/in/*/exceptionhandler` concentram a cobertura baixa do recorte unitário, exatamente onde o teste é de integração por natureza. Isso é leitura, não violação.

## What Was Validated

- claim: `jacoco-maven-plugin` fora do ciclo de vida, versão em `<properties>` | evidence consulted: diff do `pom.xml` — bloco `<plugin>` sem `<executions>`; `<jacoco-maven-plugin.version>0.8.12</jacoco-maven-plugin.version>` junto das de PIT e PMD. `./mvnw -q -DskipTests package` → exit **0**.
- claim: o comando documentado roda sem Docker e gera HTML + XML | evidence consulted: `./financas_bot_telegram/mvnw clean jacoco:prepare-agent test jacoco:report -f financas_bot_telegram/pom.xml -Dtest='!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false` → `Tests run: 374, Failures: 0, Errors: 0`, `Analyzed bundle 'financas_bot_telegram' with 148 classes`, `BUILD SUCCESS`, `Total time: 38.365 s` (o status diz "~40 s" — bate). `target/site/jacoco/index.html` e `jacoco.xml` gerados; `git check-ignore` confirma `financas_bot_telegram/.gitignore:2:target/`.
- claim: **a cobertura não veio zero por `argLine`** (o modo de falha silencioso) | evidence consulted: `mvn help:effective-pom` gerado e inspecionado — **zero ocorrências da string `argLine`** no POM efetivo inteiro. E o número positivo: LINE 1681/1864, BRANCH 326/430, com linhas descobertas que batem com a leitura manual do fonte (`MetaSignatureValidator` 57-59 é o `catch`; `PaymentRequestStrategy` 88 e 97 é o `throw`). Instrumentação funcionando, `pass` sem reserva.
- claim: os dois números do Lombok foram **medidos**, não estimados | evidence consulted: reproduzi o run "antes" **sem alterar o repositório**, com `MAVEN_OPTS="-Dlombok.disableConfig=true"` (flag do próprio Lombok que ignora `lombok.config`), `clean` nos dois. Resultado: `Analyzed bundle with 174 classes` (contra 148). Os **doze** valores da tabela §Distorção do Lombok saíram idênticos: LINE 1897/2125=89,3% → 1681/1864=90,2%; BRANCH 328/558=58,8% → 326/430=75,8%; INSTRUCTION 9095/11509=79,0% → 6880/7685=89,5%; METHOD 811/1035=78,4% → 377/431=87,5%; CLASS 162/174=93,1% → 140/148=94,6%; COMPLEXITY 945/1318=71,7% → 510/650=78,5%. A aritmética da frase também fecha: 1035−431=**604** métodos (58,4%), 11509−7685=**3824** instruções (33,2%), 558−430=**128** desvios (22,9%), e 328−326=2 cobertos ⇒ **126** descobertos.
- claim: as 5 classes do piloto têm números **byte a byte idênticos** nos dois runs | evidence consulted: comparei os `<counter>` por classe dos dois `jacoco.xml`. `identical=True` nas cinco. Reforço independente: `grep -ci lombok` nos cinco fontes → **0** em todos.
- claim: tabela por classe (linha e branch) | evidence consulted: parse do `jacoco.xml`, valores exatos — `LegendaParser` LINE 17/17, BRANCH 10/10, INSTR 70/70; `PaymentRequestStrategy` 47/49, 7/8, 178/185; `PaymentProofStrategy` 33/33, 9/10, 139/139; `MetaSignatureValidator` 26/29, 14/14, 121/129; `FecharMesServiceImpl` 75/75, 19/24, 274/276. **Cinco de cinco linhas conferem, incluindo denominadores.**
- claim: baseline do projeto e testes do recorte | evidence consulted: idem acima + `Tests run: 374` e 148 classes analisadas. A tabela de pacotes também confere um a um: `whatsapp/exceptionhandler` 4/53 e 0/18; `adapters/in/rest` 8/28 sem branch; `persistence/idempotencia` 4/10; `adapters/out/persistence` 74/97 e 0/18; `s3/service` 35/48 e 2/2. As **8 classes em 0%** são exatamente as listadas, e todas têm de 1 a 6 linhas.
- claim: divergência `LegendaParser` 17/17 (JaCoCo) × 17/18 (PIT) é a linha 17 | evidence consulted: **os dois lados, medidos**. JaCoCo: a lista de `<line nr>` da classe é `[9,11,12,13,14,15,20,21,23,24,26,27,28,29,30,32,34]` — a **17 não está lá**, é ausente e não descoberta; e os `<method>` são apenas `parseTipo` e `<clinit>`, **sem `<init>`**. PIT: parse do `LegendaParser.java.html` → 17 linhas `covered`, **1 `uncovered`: a linha 17**. Fonte: `LegendaParser.java:17` é `private LegendaParser() {}`. As duas afirmações específicas do status estão corretas na letra.
- claim: PIT e JaCoCo apontam as **mesmas linhas** nas outras classes | evidence consulted: parse do HTML do PIT — `PaymentRequestStrategy` uncovered `[88, 97]`, `MetaSignatureValidator` uncovered `[57, 58, 59]`, `PaymentProofStrategy` uncovered `[]`. Idêntico ao JaCoCo. Confere.
- claim: baseline do PIT reproduzido | evidence consulted: `./mvnw org.pitest:pitest-maven:mutationCoverage` → `Line Coverage (for mutated classes only): 123/129 (95%)`, `Generated 42 mutations Killed 37 (88%)`. Por classe: `LegendaParser` 17/18 · 6/8 · 6/8; `PaymentRequestStrategy` 47/49 · 11/11 · 11/11; `PaymentProofStrategy` 33/33 · 9/9 · 9/9; `MetaSignatureValidator` 26/29 · 11/14 · 11/13. Confere com a tabela de três números, inclusive o "2 de 8 mutantes sobreviveram".
- claim: baseline do PMD (22 violações) inalterado | evidence consulted: `./mvnw pmd:pmd` → `BUILD SUCCESS`; contagem no `target/pmd.xml` → **22**. Idêntico ao congelado na QA-013.
- claim: 5 branches descobertos de `FecharMesServiceImpl` nas linhas 59, 83, 85, 86, 87 | evidence consulted: `jacoco.xml`, linhas com `mb>0` → exatamente `59, 83, 85, 86, 87`, uma branch faltando em cada (19/24). Fonte confere: 59 é o ternário `ajuste != null`, 83/85/86 são `null`-guards nos `.filter(...)`, 87 é `getParcelasPagas() < getNumParcelas()`.
- claim: nenhuma das chamadas a `fechar(...)` passa `null` | evidence consulted: `grep -n "null" FecharMesServiceImplTest.java` → **nenhuma ocorrência da string `null` no arquivo inteiro**. A conclusão está certa e é mais forte do que o status alega. A contagem, porém, está errada — ver `low` B1.
- claim: `MetaSignatureValidator` tem 90% de linha e 100% de branch, e o buraco é bloco sem desvio | evidence consulted: `jacoco.xml` BRANCH 14/14, LINE 26/29, missed `[57,58,59]`; fonte 57-59 é `catch (NoSuchAlgorithmException | InvalidKeyException)` + `logger.error` + `return false` — nenhum desvio dentro. O rótulo "as duas métricas não são ordenáveis" bate com o dado.
- claim: `testes_total: 422` sustentado por CI verificado nesta sessão | evidence consulted: `gh run view 31727999562 --json` → `headSha 15ae35e96bac03bbfc39e082e5ebb52057eb000f`, `conclusion success`, workflow `CI Gate (develop)`; `gh run view --log` → `[INFO] Tests run: 422, Failures: 0, Errors: 0, Skipped: 0`. `git merge-base --is-ancestor 15ae35e HEAD` → verdadeiro. O delta entre `15ae35e` e HEAD não contém `.java`, então o total continua válido.
- claim: `cobertura_pct: na` é a aplicação correta da regra nova | evidence consulted: a regra escrita em `_TEMPLATE-status.md` e no `PRE-MERGE-CHECKLIST.md` diz "`na` quando a task não toca classe de produção"; `git diff --name-only` confirma zero `.java`. Correto — e o frontmatter do template continua parseável como YAML depois do comentário multilinha (validado com `yaml.safe_load`).
- claim: `@lombok.Generated` não tem efeito em runtime (comentário do `lombok.config`) | evidence consulted: `javap -v lombok/Generated.class` do `lombok-1.18.32.jar` → `Retention(value=RetentionPolicy.CLASS)`. Invisível por reflexão. Claim verdadeira.
- claim: `testes_novos = 0` e nenhuma classe de produção ou teste alterada | evidence consulted: `git diff --name-only origin/integration/04-instrumentacao-qualidade...HEAD` → seis arquivos, nenhum `.java`. Confere.
- claim: branch a partir da integration, nome na convenção | evidence consulted: `git rev-parse --abbrev-ref HEAD` → `feature/qa-014-piloto-jacoco-cobertura` (bate `feature/qa-\d{3}-`); `git merge-base --is-ancestor origin/develop HEAD` → verdadeiro; base da branch é `40877a9`, ponta da integration. Confere.
- claim (gate de mutação) | evidence consulted: o plano declara `mutation_gate: false` e a task não altera classe Java — não há denominador possível. Registrado como **`not-applicable`**; nenhuma evidência de PIT foi exigida (a execução do PIT nesta sessão foi para validar a comparação de três números, não para auditar gate).

## Findings (by severity)

- critical: nenhum.

- high:
  - **A1 — §"Correção de fato registrada no backlog" é falsa, e inverte um item de backlog que estava certo.** O status afirma, como fato reconfirmado na execução: *"Antes desta task, a única ocorrência da string `jacoco` no repositório era um comentário no frontmatter de `docs/templates/_TEMPLATE-status.md:16`; o agente `qa-test-specialist` não mencionava JaCoCo"*, e conclui que *"não havia comando algum"* e que a causa registrada no item #6 estava errada.

    Um comando derruba as três afirmações. `git grep -in "jacoco" origin/integration/04-instrumentacao-qualidade` retorna **77 linhas** no commit-base. Entre elas:

    - `.codex/agents/qa-test-specialist.toml:37` — `` - Cobertura: JaCoCo — `./mvnw jacoco:report` ``
    - `.codex/agents/qa-test-specialist.toml:186` — `` | Cobertura da classe principal | `./mvnw jacoco:report` → campo `cobertura_pct` | ``
    - mais 4 menções a JaCoCo no mesmo arquivo (linhas 78, 156, 175, 205)
    - `docs/experiments/models-claude-experiment/05-instrumentacao-e-harness.md:62` — *"O comando `./mvnw jacoco:report` que o agente QA é instruído a rodar **falha**"*, que é a **fonte primária** da redação do item #6

    Existem **dois** arquivos de agente com o mesmo papel, um por harness: `.claude/agents/qa-test-specialist.md` (nenhuma menção a JaCoCo — é o que o plano grepou) e `.codex/agents/qa-test-specialist.toml` (seis menções, duas delas instruindo o comando). O grep do plano cobriu `.claude/`, `docs/templates/` e `docs/runbooks/`; `.codex/` ficou fora, e o status promoveu esse recorte a "no repositório".

    Portanto **o item #6 do backlog está correto como escrito**: havia sim um agente instruído a rodar `./mvnw jacoco:report`, e o comando falhava (prefixo `jacoco` não resolvia sem o plugin no POM). Quem está errada é a "correção".

    Por que isto é `high` e não prosa: o plano manda o planner **fechar o item #6 e registrar o baseline no `STATE.md`** após o merge. Mesclar como está propaga a correção invertida para o backlog e para o `STATE.md`, e coloca o status em contradição direta com `05-instrumentacao-e-harness.md:62` sem sequer citá-lo. É a mesma classe do `high` da QA-013 — afirmação apresentada como verificada que a evidência não sustenta —, agora com o agravante de reescrever um registro correto.

- medium:
  - **M1 — o comando documentado omite `clean`, e o `jacoco.exec` acumula entre runs.** O runbook §Camada 1.6 e a linha `cobertura_pct` do `PRE-MERGE-CHECKLIST.md` documentam `jacoco:prepare-agent test jacoco:report ...` **sem `clean`** e sem `-Djacoco.append=false`. O parâmetro `append` do `prepare-agent` **não tem `default-value` no `plugin.xml`** e cai no default do agente, que é `true`.

    Verificado por execução, não por leitura de doc:

    | Run (sem `clean`, sequenciais) | LINE coberta no relatório |
    |---|---:|
    | `-Dtest='LegendaParserTest'` | 23 |
    | `-Dtest='CookieFactoryTest'` | **42**, e `LegendaParser` ainda aparece **17/17** |

    O segundo relatório credita cobertura a uma classe que o segundo run não exercitou. Consequência prática: quem rodar a suíte completa uma vez (num ambiente com Docker, ou via `mvn test` de outro contexto) e depois rodar o comando documentado obtém um número que **não é unit-only** — contradizendo em silêncio a ressalva central do runbook, a definição de `cobertura_pct` do template e a comparabilidade com o PIT, que é a razão nº 1 do recorte no plano.

    Reforça o achado: os runs de medição do próprio status usaram `clean` (§Decisões tomadas, item 2), ou seja, **o comando da evidência e o comando documentado divergem**. O que foi medido é reprodutível; o que foi documentado não é.

  - **M2 — a rota de falha silenciosa do agente de QA ficou aberta, como consequência de A1.** Como a task concluiu que "não havia comando algum", `.codex/agents/qa-test-specialist.toml` continua instruindo `./mvnw jacoco:report` puro. Verifiquei o efeito **depois** desta task: com `target/` limpo, o comando agora resolve o prefixo (o plugin passou a estar no POM) e responde

    ```
    [INFO] --- jacoco:0.8.12:report (default-cli) @ financas-bot-telegram ---
    [INFO] Skipping JaCoCo execution due to missing execution data file.
    [INFO] BUILD SUCCESS
    ```

    Antes da task o comando **falhava alto**; depois dela ele **passa verde e não produz relatório nenhum**. Para um agente que lê exit code, isso é pior que antes. O runbook novo já descreve exatamente esse modo ("Rodar `jacoco:report` sozinho... ou falha por falta do `.exec` ou reporta um `.exec` velho") — o que falta é a instrução do agente ter sido alinhada, ou o descompasso ter sido registrado.

    **Não peço a correção do arquivo nesta task:** `.codex/` e `.claude/` são território do humano/`ai-engineer` e exigem autorização explícita (CLAUDE.md). O que é exigível aqui é **registrar como débito** e parar de afirmar que o problema não existia.

- low:
  - **B1 — "as 13 chamadas a `service.fechar(...)`" são 12.** `grep -n "\.fechar("` em `FecharMesServiceImplTest.java` → linhas 89, 111, 141, 168, 185, 203, 224, 236, 250, 279, 308, 328. Doze. (A classe tem 15 `@Test`.) A conclusão que o número sustenta — nenhuma passa `null` — está **correta e é mais forte do que o alegado**: a string `null` não aparece uma vez sequer no arquivo. Só o numeral está errado; corrigir para 12 e, se quiser, trocar a evidência pela mais forte.
  - **B2 — "quase toda a cobertura de branch faltante do projeto era `equals`/`hashCode` gerado" não bate com o número ao lado.** Antes do `lombok.config` faltavam **230** desvios (558−328). O Lombok respondia por **126** deles. Isso é **54,8%** — pouco mais da metade, não "quase toda"; sobram 104 desvios descobertos em código escrito à mão. As formulações que a evidência sustenta são "**126 dos 128** desvios removidos estavam descobertos" (98%) e "o Lombok respondia por **55%** dos desvios não cobertos do projeto". Achado exatamente da classe que o plano pediu para caçar: rótulo qualitativo mais forte que o dado ao lado.
  - **B3 — tabela quebrada no runbook por pipe não escapado.** `docs/runbooks/ROTEIRO-TESTES-BACKEND.md:125` contém `` | `BRANCH` | cada saída de `if`/`&&`/`||`/ternário/`switch` | é onde a cobertura de linha mente; ... | ``. Em GFM o `|` delimita célula **mesmo dentro de crase**, então a linha vira 5 células num cabeçalho de 3 e o excedente é descartado: **a coluna "Armadilha" da linha `BRANCH` — o aviso mais importante da tabela — não renderiza**. Escapar como `\|\|`. (O próprio status já escapa corretamente em `NoSuchAlgorithmException \| InvalidKeyException`, então é lapso e não desconhecimento.)
  - **B4 — a justificativa do `argLine` no parent está apoiada na evidência mais fraca das disponíveis.** O status diz que "o parent `spring-boot-starter-parent:3.4.5` também não define nenhum dos dois" (surefire e `argLine`). Literalmente verdadeiro para aquele arquivo — grepei `spring-boot-starter-parent-3.4.5.pom`: 0 e 0. Mas a **cadeia** de parents gerencia sim o `maven-surefire-plugin` (versão 3.5.3, via `spring-boot-dependencies`), e o POM efetivo traz `maven-surefire-plugin` tanto em `pluginManagement` quanto em `build/plugins`. Quem for conferir com `help:effective-pom` vai ver surefire e achar que o status errou. A evidência decisiva — e que confirma a conclusão — é outra: **`argLine` tem zero ocorrências no POM efetivo inteiro**. Mesma observação vale para a frase absoluta do runbook, "hoje **não existe** plugin surefire configurado no `pom.xml`": vale para o POM do módulo, não para o efetivo.

## Required Fixes

1. **Corrigir a §"Correção de fato registrada no backlog"** (A1). Retratar as três afirmações falsas e registrar que **o item #6 estava correto**: `.codex/agents/qa-test-specialist.toml:37` e `:186` instruem `./mvnw jacoco:report`, e `docs/experiments/models-claude-experiment/05-instrumentacao-e-harness.md:62` é a fonte primária dessa redação. Se quiser preservar a nuance real, ela é: existem **duas** definições do agente de QA, uma por harness, e o grep do plano só cobriu a do `.claude/`. Comando de verificação a citar: `git grep -in "jacoco" <commit-base>`.
2. **Acrescentar `clean` (ou `-Djacoco.append=false`) ao comando documentado** (M1), nos três lugares onde ele aparece: `ROTEIRO-TESTES-BACKEND.md` §Camada 1.6, a linha `cobertura_pct` do `PRE-MERGE-CHECKLIST.md` e o comentário do `_TEMPLATE-status.md`. Registrar no runbook, junto da armadilha do `argLine`, que **`append` é `true` por padrão** e que sem `clean` o `.exec` acumula runs anteriores — inclusive runs que incluíram `*IntegrationTest`, o que anula a propriedade unit-only. Alinha o comando documentado com o comando que produziu a evidência.
3. **Registrar M2 como débito** no §Débitos técnicos do status: `.codex/agents/qa-test-specialist.toml` instrui `jacoco:report` puro, que depois desta task retorna `BUILD SUCCESS` **sem gerar relatório** (`Skipping JaCoCo execution due to missing execution data file`) — falha silenciosa em vez da falha alta de antes. Não corrigir o arquivo nesta task (território `.codex/` exige autorização explícita do humano); apenas reportar para o planner decidir.
4. **Corrigir "13 chamadas" para 12** (B1).
5. **Reformular "quase toda a cobertura de branch faltante"** para o que o número sustenta — 126 de 230 desvios descobertos, ou seja ~55% (B2).
6. **Escapar `\|\|`** em `ROTEIRO-TESTES-BACKEND.md:125` (B3).

Itens 1 a 3 são os que bloqueiam; 4 a 6 são correções de uma linha cada que devem entrar no mesmo commit.

## Optional Improvements

- **B4:** trocar a evidência do `argLine` por `mvn help:effective-pom | grep -i argLine` → zero ocorrências. É mais curta, mais forte, imune à cadeia de parents, e não fica falsa quando alguém consultar o POM efetivo e enxergar o surefire lá. Ajustar junto a frase absoluta do runbook para "no `pom.xml` deste módulo".
- Registrar no runbook a flag `-Dlombok.disableConfig=true` como a maneira de reproduzir o número "antes" **sem mexer no repositório**. Foi assim que reproduzi os doze valores da tabela do Lombok nesta revisão; o status sugere mover o arquivo para fora, o que é mais arriscado (fica fácil esquecer de restaurar) e desnecessário.
- A tabela §Baseline por pacote é o material mais acionável do relatório e está enterrada. Vale uma frase dizendo qual dessas linhas o item #7 deve consumir primeiro.

## Technical Debt Identified

- item: `.codex/agents/qa-test-specialist.toml` (linhas 37 e 186) instrui `./mvnw jacoco:report` puro | impact: depois da QA-014 o comando retorna `BUILD SUCCESS` sem produzir relatório (`Skipping JaCoCo execution due to missing execution data file`) — o agente de QA recebe verde e nenhum dado, com risco de preencher `cobertura_pct` com número inexistente ou herdado de `.exec` velho; antes ele falhava alto | suggested_action: atualizar as duas linhas para o comando completo do §Camada 1.6, em task própria e com autorização explícita do humano (território `.claude/`/`.codex/`)
- item: `append=true` é o default do agente JaCoCo e nada no repositório protege contra ele | impact: qualquer comando de cobertura sem `clean` mistura runs; o número reportado deixa de descrever a população de testes que ele diz descrever | suggested_action: além do fix 2, avaliar fixar `<append>false</append>` na configuração do plugin no `pom.xml`, para o default seguro não depender de disciplina de quem digita o comando
- item: o plano da QA-014 (§Contexto, "Correção de um fato que o backlog registra errado") carrega a mesma afirmação falsa de A1 | impact: se só o status for corrigido, o plano continua sendo fonte do erro para quem o reler; e o plano é o artefato que o planner consulta ao fechar o item #6 | suggested_action: planner corrigir a §Contexto do plano junto do fechamento do item #6
- item: `FecharMesServiceImpl` — 5 `null`-guards de `fechar(...)` nunca exercitados com `null` (100% linha / 79% branch, linhas 59, 83, 85, 86, 87) | impact: código defensivo que nunca foi provado defender; confirmado independentemente por JaCoCo nesta revisão | suggested_action: manter como o débito nº 1 do status; alvo natural para o item #2 do backlog junto do `LegendaParser`
- item: `GlobalWhatsAppExceptionHandler` — 4/53 linhas e 0/18 branches no recorte unitário, pior número do projeto | impact: **indeterminado** até a suíte de integração ser mensurável; confirmei que a classe é a única do pacote, então a atribuição do status está certa | suggested_action: reavaliar depois do FIX de Docker; não tratar como buraco de teste antes disso
- item: 8 classes em 0% de linha no recorte (`ApiTelegramClientException`, `WhatsAppContact`, `WhatsAppMetadata`, `WhatsAppProfile`, `InvalidWhatsAppPayloadException`, `WhatsAppMediaDownloadException`, `MensagemProcessadaEntity`, `TelegramFileDownloadException`) | impact: baixo — verifiquei que todas têm de 1 a 6 linhas e são exceções/DTOs; entram aqui só para não sumirem do radar do item #7 | suggested_action: candidatas naturais a exclusão de escopo quando o recorte por diff for automatizado

## Blocked Validations / Uncertainty

- **Cobertura da suíte de integração: não medida, e o status declara isso corretamente.** Não reproduzi porque o Docker/Testcontainers está indisponível nesta máquina — mesmo débito já registrado. Nenhuma afirmação do status depende dessa medição; o único ponto que dependeria (débito nº 4) está explicitamente marcado como indeterminado, o que é a conduta certa.
- **JVM 23-ea, não Temurin 21.** Rodei com a mesma JVM que o implementador (única instalada), então minha reprodução confirma os números **na mesma condição**, não numa condição independente. O `pom.xml` compila com `--release 21` e o CI usa Temurin 21. Ressalva herdada da QA-012/QA-013, declarada no status; não bloqueia nada nesta task porque nenhum número aqui é sensível à JVM de forma plausível.
- **Nota de ambiente, fora do escopo da task.** A árvore de trabalho tem uma alteração **não commitada** em `.claude/agents/backend.md` (`model: inherit` → `model: opus`). Não está em nenhum dos dois commits da branch, não entra no diff revisado e não afeta o veredito — registro só para que não seja arrastada por engano para o commit de correção. Nenhum arquivo do repositório foi modificado por esta revisão; as execuções só escreveram em `target/`, que é gitignored, e o deixei no estado canônico do comando documentado.

## Review Artifact Path
- `docs/sprints/04-instrumentacao-qualidade/avaliacoes/review-QA-014-piloto-jacoco-cobertura.md`

## Plan Deviations

- none — o status declara "Nenhum" e confirmei: todos os itens de §Escopo/arquivos foram entregues, nada fora deles foi tocado, e as 5 decisões de execução registradas ficam dentro da latitude que o plano deixou aberta (versão do plugin, uso de `clean` na comparação, numeração da subseção do runbook). A reprodução do run do PIT (decisão 5) excede o escopo, mas na direção de reforçar um critério de aceitação — não é desvio.

## Verdict (rodada 1)
- rejected

**Por quê, sendo o núcleo de medição impecável.** É preciso separar as duas metades desta entrega.

A metade medida é a mais bem sustentada que revisei nesta sprint. Reproduzi **todos** os números do relatório e nenhum divergiu: os doze valores da tabela do Lombok, as cinco linhas da tabela por classe com denominadores, os baselines do PIT e do PMD, a tabela por pacote, as 8 classes em 0%, a divergência do `LegendaParser` conferida **linha a linha nos dois lados** e a igualdade byte a byte das 5 classes do piloto entre os dois runs. Os três pontos que o plano mandou vigiar passaram: a cobertura não veio zero por `argLine` (zero ocorrências no POM efetivo, e números positivos coerentes com o fonte), os dois números do Lombok foram medidos de verdade (reproduzi o "antes" com `-Dlombok.disableConfig=true`), e a comparação de três números vem com a divergência explicada até a linha e a causa correta.

A metade escrita é onde ela cai. O veredito é `rejected` por dois motivos independentes:

1. O premise check `technical_justifications_true` está **fail**, e a regra é que não se aprova com premissa falha. Não é tecnicalidade: A1 afirma um fato "reconfirmado nesta execução" que um `git grep` derruba, e usa esse fato para **corrigir para errado** um item de backlog que estava certo. O plano manda fechar o item #6 depois do merge — mesclar assim propaga o erro para o backlog e para o `STATE.md`, e deixa o status em contradição não declarada com `05-instrumentacao-e-harness.md:62`.
2. M1 faz o comando documentado — que é entregável desta task, não anexo — produzir, em cenário realista, um número que contradiz a própria definição que o template acabou de escrever. A task instala uma métrica; a métrica precisa ser reprodutível pelo comando publicado, e hoje ela é reprodutível pelo comando da evidência (com `clean`) e não pelo comando do runbook.

Os seis fixes são pontuais: cinco edições de texto e uma palavra (`clean`) em três arquivos. Nenhum exige remedição — todos os números permanecem válidos como estão. Re-revisão pode ser `delta-review` restrita aos itens 1 a 6.

---

# Delta-review — rodada 2 (2026-08-13)

## Delta Scope

Modo: `delta-review`. Diff `1c1f216..HEAD` — commits `6dff57f` (as 6 correções + o relatório desta revisão), `e1742bf` (hash no frontmatter), `606d250` (coerência interna). Cinco arquivos, nenhum `.java`, nenhum `.claude/`, nenhum `.codex/`.

Escopo desta rodada, conforme combinado: **verificar aplicação e lastro das 6 correções**, não reproduzir medição. Os números da rodada 1 foram todos reproduzidos e não mudaram — verificado por diff (ver `Delta Verification`, último item).

## Premise Checks (delta)

- architecture_best_practice: pass — notes: nada de código no delta; o `pom.xml` não foi tocado pelos três commits.
- technical_justifications_true: **pass** (era `fail` na rodada 1) — notes: A1 retratado e a nova redação bate com o `git grep` refeito nesta sessão; B4 trocado pela evidência forte. As três afirmações **novas** (retratação, parágrafo do `append`, texto do `effective-pom`) foram verificadas uma a uma abaixo; nenhuma entrou sem lastro. Resta uma imprecisão de atribuição, não de fato — `low` C3.
- external_contract_source_verified: pass — notes: inalterado; a task não consome campo de sistema externo. As duas linhas do `.codex/` citadas no status foram conferidas **verbatim** contra o commit-base.
- test_fixtures_from_real_source: pass — notes: `testes_novos: 0`, nada a auditar; nenhum número de medição alterado no delta.
- no_process_doc_references_in_code: pass — notes: nenhum arquivo de código no delta.

## Delta Verification

- fix 1 (A1) | **aplicado, e a retratação confere.** A §"Correção de fato registrada no backlog" virou §"Retratação — o item #6 do backlog estava certo" (status linhas 205-218). Refiz as três checagens que a seção agora afirma: `git grep -in "jacoco" origin/integration/04-instrumentacao-qualidade | wc -l` → **77**; `.codex/agents/qa-test-specialist.toml:37` → `` - Cobertura: JaCoCo — `./mvnw jacoco:report` `` e `:186` → `` | Cobertura da classe principal | `./mvnw jacoco:report` → campo `cobertura_pct` | `` (citação **verbatim**, só com indentação cosmética a mais); `git grep -in jacoco -- .claude/agents/qa-test-specialist.md` no commit-base → **vazio**, e `git ls-tree` confirma que os dois arquivos de agente coexistem — logo "duas definições, uma por harness" é fato. A atribuição do erro ("repeti a verificação do plano em vez de refazê-la") bate com `plans/QA-014-...md:49`, que de fato limita o grep a `.claude/`, `docs/templates/` e `docs/runbooks/`. A instrução ao planner ("**não** fechar o item #6 com a 'correção' que estava aqui") é a conduta correta. Não troca um erro por outro.
- fix 2 (M1) | **aplicado nos três documentos, mas há um quarto ponto de publicação que ficou de fora** — ver `medium` C1. `git grep "jacoco:prepare-agent" -- docs/` retorna exatamente três comandos publicados e **os três trazem `clean`**: `ROTEIRO-TESTES-BACKEND.md:107`, `PRE-MERGE-CHECKLIST.md:18`, `_TEMPLATE-status.md:17`. O status também: a tabela §Evidência de execução (linhas 46-47) já usava `clean` desde a rodada 1 e continua. O parágrafo novo do runbook (`:112`) e o do checklist (`:18`) descrevem a armadilha corretamente.
- claim nova: "`Alternativa equivalente: -Djacoco.append=false`" (runbook `:112`) | **verificada na fonte.** Extraí `META-INF/maven/plugin.xml` do `jacoco-maven-plugin-0.8.12.jar`: o mojo `prepare-agent` declara `<append implementation="java.lang.Boolean">${jacoco.append}</append>` — a user property existe e o `-D` funciona. Lastro ok.
- claim nova: "os runs da medição usaram `clean` **por acidente**" (status `:65`) | **coerente e verificável.** Bate com §Decisões tomadas item 2 (recompilação exigida pelo `lombok.config`) e com a tabela de evidência, que já publicava `clean` no comando. A auto-atribuição honesta ("foi encontrado pela revisão (M1), não por mim") é fiel ao que aconteceu.
- fix 3 (M2) | **aplicado como débito 7**, com o comportamento descrito corretamente: `Skipping JaCoCo execution due to missing execution data file` + `BUILD SUCCESS` — idêntico ao que reproduzi na rodada 1, com `target/` limpo. `.codex/` **não** foi tocado (confirmado por `git diff --name-only 1c1f216..HEAD`), e a justificativa citada (território do humano, CLAUDE.md) é a correta. O runbook `:114` documenta o mesmo modo de falha para quem roda à mão. Severidade "média" no débito 7 = `medium` do achado; não suavizou.
- fix 4 (B1) | **aplicado.** Status `:193` — "as **12 chamadas**", e a evidência foi trocada pela mais forte ("a string `null` **não aparece no arquivo inteiro**"), que é exatamente o que medi na rodada 1.
- fix 5 (B2) | **aplicado e aritmeticamente correto.** Status `:82` — "**55% de toda a 'cobertura de branch faltante' do projeto (126 de 230)**", com a ressalva de que "os outros 104 desvios descobertos são código escrito à mão e continuam valendo como buraco real". 126/230 = 54,8% → 55%; 230−126 = 104. O rótulo qualitativo agora é o número.
- fix 6 (B3) | **aplicado.** `ROTEIRO-TESTES-BACKEND.md:129` traz `` `\|\|` ``. Em GFM o `\|` é resolvido antes do parse inline, então a célula volta a ter 3 colunas e a coluna "Armadilha" da linha `BRANCH` renderiza.
- B4 (opcional) | **aplicado, e com a admissão correta.** Status `:61` troca a evidência por `help:effective-pom` + `grep -c argLine` = 0 e **declara explicitamente** que a anterior era frágil porque o surefire *é* gerenciado pela cadeia de parents (3.5.3) — os dois fatos que confirmei na rodada 1. O runbook `:120` deixou de dizer "não existe plugin surefire configurado no `pom.xml`" e passou a dizer "não existe nenhum `<argLine>` no POM efetivo". Correto nos dois lugares.
- §"Revisão independente (ADR 0005)" nova (status `:222-236`) | **fiel, sem suavização.** Confere um a um contra este relatório: veredito `rejected`, "6 correções pontuais e nenhuma remediação de código", severidades `high`/`medium`/`medium`/`low`×4 idênticas às minhas, e o texto de cada linha descreve o achado pelo que ele é (A1 "invertia um item correto do backlog"; M2 "passou a desistir em silêncio"). Não há achado omitido, rebaixado nem reatribuído a terceiros — A1 e M1 são assumidos como erro próprio ("verificação herdada em vez de refeita"). O parágrafo de fecho não reivindica mérito que não existe.
- números de medição | **nenhum alterado.** `git diff 1c1f216..HEAD` filtrado por linhas com números: as únicas mudanças numéricas são `13 → 12` (a contagem errada, que é o próprio fix B1) e a adição de `55% (126 de 230)`. As tabelas do Lombok (12 valores), por classe, por pacote, os baselines de PIT e PMD, as 8 classes em 0% e os totais (374/422/148/174) estão byte a byte iguais.
- `.claude/agents/backend.md` | **não entrou em commit nenhum.** `git diff --name-only 1c1f216..HEAD` lista cinco arquivos, todos em `docs/`; `git status --porcelain` continua mostrando ` M .claude/agents/backend.md` como alteração **não commitada** na árvore de trabalho — mesmo estado da rodada 1. A ressalva de ambiente segue valendo: não arrastar esse arquivo para o commit de merge.

## Findings (delta, by severity)

- critical: nenhum.
- high: nenhum. **A1 está resolvido** e o premise check que estava `fail` passou.

- medium:
  - **C1 — sobrou um quarto lugar publicando o comando sem `clean`: o comentário do `pom.xml`.** `financas_bot_telegram/pom.xml:307` contém

    ```
    ./mvnw jacoco:prepare-agent test jacoco:report -Dtest='!*IntegrationTest' -f financas_bot_telegram/pom.xml
    ```

    sem `clean` e sem `-Djacoco.append=false`. É o mesmo defeito do M1, no arquivo que explica **por que** o plugin não tem `<executions>` — ou seja, o lugar em que alguém que mexe no build vai ler primeiro. O `pom.xml` não foi tocado por nenhum dos três commits de correção.

    **Registro de honestidade:** a lista de "três lugares" é do meu próprio fix 2 da rodada 1, e ela sub-enumerou os pontos de publicação; o implementador cumpriu à risca o que foi pedido. Isso muda a atribuição, não o fato: o comando publicado no `pom.xml` continua produzindo, no cenário do M1, um número que contradiz a definição que esta mesma task escreveu. Efeito colateral menor: a frase do status `:65` ("o comando publicado **nos três documentos** não trazia o `clean`; passou a trazer") é verdadeira sobre os três, mas sugere completude que não há.

    Correção: acrescentar `clean` na linha 307 do `pom.xml` (uma palavra, território do back, risco zero). Enquanto não entrar, o repositório contém duas versões do mesmo comando, uma delas explicitamente desaconselhada pelo runbook ao lado.

- low:
  - **C2 — `commits:` do frontmatter parou em `6dff57f`.** Faltam `e1742bf` e `606d250`. O último é insolúvel por construção (um commit não cita o próprio hash), mas `606d250` poderia ter registrado `e1742bf`. Impacto: rastreabilidade do status, não correção.
  - **C3 — "`append=true` (default do plugin)" atribui o default ao lugar errado.** O `plugin.xml` do `jacoco-maven-plugin` 0.8.12 declara `append` **sem `default-value`** (diferente de `skip` e `destFile`, que têm); o `true` efetivo vem do default do **agente** JaCoCo. O comportamento afirmado está certo — reproduzi a acumulação na rodada 1 —, só a fonte do default está trocada. Vale para `ROTEIRO-TESTES-BACKEND.md:112`. Uma palavra: "default do agente".
  - **C4 — a linha 40 do status ficou fora da varredura de coerência do `606d250`.** "O diff é: `pom.xml`, `lombok.config` (novo), `ROTEIRO-TESTES-BACKEND.md`, `_TEMPLATE-status.md`, `PRE-MERGE-CHECKLIST.md` e este status" — o commit `606d250` acrescentou o relatório de revisão à §Arquivos criados/modificados (linha 303) mas não a esta enumeração, que agora está incompleta em relação ao diff da branch.
  - **C5 — "os quatro goals vão na mesma linha de comando" (runbook `:110`).** `clean` e `test` são **fases** do ciclo de vida, não goals; goals mesmo são dois (`jacoco:prepare-agent` e `jacoco:report`). A imprecisão é herdada da redação anterior ("três goals", que já contava `test`), então o fix só a propagou. Formulação exata: "os quatro itens da linha de comando".

## Required Fixes (delta)

1. **`clean` no comentário do `pom.xml:307`** (C1). Fecha o M1 no último lugar onde o comando é publicado.

Opcionais, todos de uma palavra ou uma linha, e que podem ir no mesmo commit: C2 (acrescentar `e1742bf` ao frontmatter), C3 ("default do agente"), C4 (incluir o relatório de revisão na enumeração da linha 40), C5 ("os quatro itens da linha de comando").

## Technical Debt Identified (delta)

- item: nenhum débito **novo** — os sete da rodada 1 seguem válidos e o status incorporou o M2 como item 7 da própria tabela | impact: — | suggested_action: —
- item: o plano da QA-014 (`plans/QA-014-...md:49`, §Contexto) **continua** carregando a afirmação falsa que o A1 derrubou | impact: o status foi retratado, o plano não; e o plano é o artefato que o planner consulta ao fechar o item #6 do backlog. Reconfirmado nesta rodada: a linha 49 do plano ainda diz "existe **uma única ocorrência no repositório**" | suggested_action: planner corrigir a §Contexto do plano junto do fechamento do item #6, usando `git grep -in "jacoco" <commit-base>` → 77 linhas como verificação. Não é exigível do implementador (o plano é território do planner), por isso permanece débito e não fix
- item: `backlog-s04.md:123` e `README.md:13` da sprint descrevem o item #6 **corretamente** e não precisam de mudança | impact: nenhum — registro para evitar que a "correção" da rodada 1 seja aplicada a eles por engano | suggested_action: nenhuma ação; o item #6 fecha como estava escrito

## Blocked Validations / Uncertainty (delta)

- **Medição não reexecutada nesta rodada, por decisão de escopo.** Os números foram integralmente reproduzidos na rodada 1 e confirmei por diff que nenhum deles mudou; não é validação bloqueada, é validação já feita e verificada como intacta.
- **O relatório de revisão da rodada 1 foi commitado pelo implementador (`6dff57f`) e eu não tenho cópia fora da árvore para diferenciar byte a byte.** O conteúdo em `HEAD` preserva os seis achados, as severidades e o veredito `rejected` — inclusive as passagens mais duras —, então não há sinal de edição; registro a limitação por completude.
- **`.claude/agents/backend.md` continua modificado e não commitado** na árvore de trabalho. Fora do escopo da task e do diff revisado; anotado para não ser arrastado no commit de correção.

## Verdict (rodada 2)
- approved-with-comments

**O que mudou.** As seis correções foram aplicadas, e nenhuma delas trocou um erro por outro. A retratação do A1 é o ponto alto: em vez de apagar a seção, o status registra o que afirmou antes, por que estava errado, qual comando derruba a afirmação, e instrui o planner a **não** fechar o item #6 com a correção invertida. Refiz as três verificações que a seção nova afirma — as 77 linhas do grep, as duas linhas do `.codex/` verbatim, a inexistência de menção a JaCoCo no agente do `.claude/` — e todas conferem. O premise check `technical_justifications_true`, que era o motivo formal do `rejected`, passou. As três afirmações novas do delta têm lastro verificado, incluindo a `-Djacoco.append=false`, que fui conferir no `plugin.xml` do próprio plugin. Nenhum número de medição foi tocado, nenhum achado foi suavizado na §Revisão independente, e `.claude/agents/backend.md` não entrou em commit nenhum.

**O que ainda impede o merge:** um item, de uma palavra — o `clean` no comando publicado em `financas_bot_telegram/pom.xml:307` (C1). Não é premissa falsa nem número errado, e a lista de "três lugares" que deixou esse ponto de fora foi minha, não do implementador. Mas é o mesmo defeito do M1 sobrevivendo no arquivo que explica o desenho do plugin, e mesclar assim deixa o repositório publicando duas versões do mesmo comando, uma delas contrariando o aviso que o runbook ao lado acabou de escrever. Aplicado esse fix, a task está pronta — os quatro `low` (C2 a C5) são cosméticos e podem ir junto ou virar nota para o planner. Não é necessária nova rodada de revisão: a verificação do C1 é `git grep -n "prepare-agent" financas_bot_telegram/pom.xml` mostrando `clean` na linha.
