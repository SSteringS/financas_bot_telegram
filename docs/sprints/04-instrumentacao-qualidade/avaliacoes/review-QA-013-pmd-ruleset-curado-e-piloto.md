---
task: QA-013
sprint: 04-instrumentacao-qualidade
data: 2026-08-12
avaliador: claude-reviewer
plano: docs/sprints/04-instrumentacao-qualidade/plans/QA-013-pmd-ruleset-curado-e-piloto.md
status_report: docs/sprints/04-instrumentacao-qualidade/status/QA-013-pmd-ruleset-curado-e-piloto.md
veredito_codigo: aprovado_com_observacoes
veredito_final: aprovado_com_observacoes
achados_count: 6                           # F1..F6 — F1 (high) resolvido na rodada 2; F2..F5 (low) atendidos; F6 (low) novo e aberto
achados_bloqueantes: 0                     # era 1 na rodada 1 (F1)
rodadas: 2                                 # rodada 1 = full-review (veredito rejeitado); rodada 2 = delta-review
data_delta: 2026-08-12
commits_delta:
  - 2b9cdc7
  - 767706b
roteiro_executado: true
gates_verificados_contra_realidade: ok
mutation_gate: nao_aplicavel
veredito_qa: nao_aplicavel
fluxos_qa_executados: []
---

# Review - QA-013 PMD: ruleset curado e piloto de leitura interpretada

## Metadata
- sprint: `04-instrumentacao-qualidade`
- task_id: `QA-013`
- agent: reviewer
- date: `2026-08-12`
- plan: `docs/sprints/04-instrumentacao-qualidade/plans/QA-013-pmd-ruleset-curado-e-piloto.md`
- status: `docs/sprints/04-instrumentacao-qualidade/status/QA-013-pmd-ruleset-curado-e-piloto.md`

> **Duas rodadas.** A rodada 1 (`full-review`) fechou `rejected` por `F1`. A rodada 2 (`delta-review`, commits `2b9cdc7` e `767706b`) está na seção **`Delta Review — rodada 2`**, e é ela que produz o veredito vigente. As seções abaixo preservam a rodada 1 como registro histórico; onde algo mudou, o texto remete à rodada 2.

## Review Scope

Modo rodada 1: `full-review`. Branch `feature/qa-013-pmd-ruleset-curado-e-piloto`, commits `1a8057d` e `b8de250`, diff contra `origin/integration/04-instrumentacao-qualidade`.

Modo rodada 2: `delta-review`. Diff `b8de250..767706b` — 4 arquivos: `financas_bot_telegram/pmd-ruleset.xml`, `financas_bot_telegram/pom.xml`, `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` e o status. Zero classe Java tocada, também no delta.

Arquivos revisados: `financas_bot_telegram/pmd-ruleset.xml` (novo), `financas_bot_telegram/pom.xml`, `docs/runbooks/ROTEIRO-TESTES-BACKEND.md`, `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report.

A revisão foi **executada, não lida**: rodei o PMD por conta própria (run do projeto, run com teste, run de medição de complexidade, e uma reconstrução da rodada 1 com as categorias inteiras), reproduzi os dois bugs de locale em JVM local, reproduzi a suíte de testes, e li o descritor do `maven-pmd-plugin` e os XMLs de categoria dentro do `pmd-java-7.7.0.jar`. As reconstruções que precisaram de ruleset diferente do congelado foram feitas em **projetos-cópia fora do repositório** (scratchpad), sem tocar em nenhum arquivo versionado.

## Premise Checks
- architecture_best_practice: pass — notes: a task não adiciona código Java. `git diff --stat` contra a integration mostra exatamente 5 arquivos: `pom.xml`, `pmd-ruleset.xml` (novo), 2 runbooks e o status. **Zero classes de produção e zero classes de teste alteradas** — o desvio que o plano apontava como mais provável não ocorreu. O bloco do `pitest-maven` (inclusive `targetClasses`) não foi tocado. Estilo declarado: hexagonal; nada no diff cria ou move classe.
- technical_justifications_true: **pass após a rodada 2** (era `fail` na rodada 1) — notes: na rodada 1, **uma** justificativa técnica central era falsa e foi usada para ranquear a severidade de um débito (`F1`). A rodada 2 corrigiu a afirmação nos três lugares onde ela existia (leitura interpretada nº 2, débito 1 e comentário do `pmd-ruleset.xml`) e a nova redação foi **reverificada contra o código, não contra o texto** — rastreamento completo em `Delta Review — rodada 2`. Todas as outras justificativas já reproduziam exatamente na rodada 1 e nenhuma foi alterada pelo delta.
- external_contract_source_verified: pass — notes: não há contrato de sistema externo consumido. Os "contratos externos" desta task são o do PMD e o do `maven-pmd-plugin`, e cada afirmação sobre eles foi rastreada à fonte autoritativa: `category/java/design.xml` e `category/java/errorprone.xml` dentro do `pmd-java-7.7.0.jar` (existência e categoria de cada regra, inclusive a ausência de `ExcessiveMethodLength`), e `META-INF/maven/plugin.xml` dentro do `maven-pmd-plugin-3.26.0.jar` (ausência de user property para `includeTests` e para `rulesets`).
- test_fixtures_from_real_source: pass — notes: a task não adiciona teste (`testes_novos: 0`, correto conforme o plano). O equivalente ao fixture aqui é o relatório do PMD, e ele **não é auto-confirmatório**: regenerei o baseline a partir de uma cópia do ruleset congelado, num projeto separado, e obtive o mesmo conjunto de 22 violações — inclusive com e sem as classes compiladas no auxclasspath.
- no_process_doc_references_in_code: pass — notes: os comentários do `pmd-ruleset.xml` e do `pom.xml` referenciam documentos de processo (`analise-estatica-pmd-checkstyle.md`, backlog #7, QA-013), mas ambos os arquivos **são** configuração de processo, não código de produção. Nenhuma classe Java ganhou referência a documento.

## Architecture Conformance Check
- declared style: hexagonal
- conformance: pass — notes: sem impacto arquitetural. Registro que o próprio status documenta com honestidade o oposto útil (débito 6): as categorias `errorprone`/`design` não têm regra de conformidade arquitetural, e `LoosePackageCoupling` foi removida pelo próprio PMD por falta de configuração — reproduzi o warning literal `[WARNING] Removed misconfigured rule: LoosePackageCoupling cause: No packages or classes specified`.

## What Was Validated

- claim: baseline de 22 violações em produção, tabela por regra, 18 arquivos afetados | evidence consulted: `./financas_bot_telegram/mvnw pmd:pmd -f financas_bot_telegram/pom.xml` → `BUILD SUCCESS`, `target/pmd.xml` parseado: **22 violações, 0 `<error>`, 0 `<configerror>`, 18 arquivos**. Por regra: `AvoidCatchingGenericException` 10, `UseLocaleWithCaseConversions` 5, `MutableStaticState` 2, `CyclomaticComplexity` 2, `AvoidThrowingNewInstanceOfSameException` 1, `NPathComplexity` 1, `CognitiveComplexity` 1, e `EmptyCatchBlock`/`CompareObjectsWithEquals`/`NcssCount` em 0. **Bate linha a linha com a tabela do status.**
- claim: distribuição por categoria = 5 errorprone + 17 design | evidence consulted: atributo `ruleset` de cada `<violation>` no XML: `UseLocaleWithCaseConversions` sai como `Error Prone`; `AvoidCatchingGenericException`, `MutableStaticState` e `AvoidThrowingNewInstanceOfSameException` saem como `Design`. **O status rotula certo** — a suspeita levantada no dispatch não se confirma; o rótulo do status coincide com a categoria real do PMD 7.7.0.
- claim: `Q7_teste = 1`, e o run com teste devolve produção + teste somados | evidence consulted: run com `-Dpmd.includeTests=true` → **23 violações**; comparação de conjuntos `(arquivo, linha, regra)`: o conjunto de produção é **subconjunto estrito** do conjunto com teste e a diferença é exatamente `NotificacaoComprovanteListenerIntegrationTest:69` / `AvoidCatchingGenericException`. Confirmado.
- claim: a flag `-Dpmd.includeTests` só funciona por causa da property de projeto; `-Dpmd.rulesets` não existe | evidence consulted: `META-INF/maven/plugin.xml` do `maven-pmd-plugin-3.26.0.jar`, mojo `pmd`: `<includeTests implementation="boolean" default-value="false"/>` **sem expressão**, e `rulesets` idem; para comparação, `skip` tem `${pmd.skip}` e `targetJdk` tem `${targetJdk}`. A armadilha descrita no status é real e a correção adotada é a correta.
- claim: só 2 violações no piloto, ambas `UseLocaleWithCaseConversions`, ambas `(a)` | evidence consulted: filtro do `target/pmd.xml` pelas 5 classes do piloto → `LegendaParser:21` e `PaymentProofStrategy:81`, nada mais. `MetaSignatureValidator` de fato não produziu violação em nenhuma das rodadas.
- claim: rodada 1 com as categorias inteiras = 94 em produção, 8 no piloto, `LawOfDemeter` 26 (28%) e `MissingSerialVersionUID` 25 (27%) | evidence consulted: reconstruí a rodada 1 (`errorprone` + `design` inteiras) num projeto-cópia com o mesmo classpath → **94 violações em produção, 8 no piloto**, `LawOfDemeter` **26** (27,7%), `MissingSerialVersionUID` **25** (26,6%). As 8 do piloto são exatamente as 8 descritas: `FecharMesServiceImpl:99` ×3 `LawOfDemeter` (com os graus 1, 2 e 2 que o status cita), `PaymentRequestStrategy:73` `LawOfDemeter`, `PaymentProofStrategy:40` e `PaymentRequestStrategy:47` `SimplifyBooleanReturns`, `LegendaParser:21` e `PaymentProofStrategy:81` `UseLocaleWithCaseConversions`. **Nenhuma regra que disparou ficou fora da narrativa** — cruzei a lista de regras que dispararam na rodada 1 contra as 10 mantidas e as 11 discutidas no XML: fecha, não sobrou regra silenciosamente descartada.
- claim: a mensagem de `SimplifyBooleanReturns` sai com placeholders crus | evidence consulted: reproduzido literalmente: `` This if statement can be replaced by `return !{condition} || {elseBranch};` ``. Argumento (a) do status: **confirmado**.
- claim: a sugestão de `SimplifyBooleanReturns`, aplicada ao pé da letra, lançaria NPE | evidence consulted: **confirmado, e mais forte do que o status afirma**. Probei os quatro formatos da regra num arquivo controlado e o PMD 7.7.0 emite: `if(c) return true; return e;` → `{condition} || {elseBranch}` (correto); `if(c) return false; return e;` → `!{condition} || {elseBranch}` (**errado**, o correto seria `&&`); `if(c) return t; return true;` → `!{condition} && {thenBranch}` (**errado**, o correto seria `||`); `if(c) return t; return false;` → `{condition} && {thenBranch}` (correto). Ou seja, o operador está trocado em dois dos quatro formatos, e o formato exato do `supports()` é um deles. O ponto que o implementador declarou como o mais frágil da curadoria **resistiu ao ataque**. Ressalva de precisão em `F2`.
- claim: os dois bugs de locale são reais e só `pix` é afetada entre as 4 palavras-chave | evidence consulted: executei `LocaleCheck.java` no JDK local: `PIX→p?x` (não contém `pix`); `BOLETO`, `TED`, `AGENDAMENTO` inalterados; `"pix".toUpperCase(tr-TR)` → `Enum.valueOf` lança `IllegalArgumentException`. **Os fatos de JDK são verdadeiros e a exclusividade do `pix` também.** O que não se sustenta é o mapeamento de um deles para o código deste repositório — ver `F1`.
- claim: PMD parseou 191 arquivos de produção e 273 com teste, zero erro de parse | evidence consulted: regra XPath `//CompilationUnit` numa passada descartável → **191** (produção) e **273** (produção + teste), com `0 <error>` em ambas; `find src/main/java -name '*.java' | wc -l` = 191 e `src/test/java` = 82. Confirmado.
- claim: a tabela de complexidade do piloto, com as somas por classe 7/10/11/10/16 | evidence consulted: passada de medição com `classReportLevel=1` e `methodReportLevel=1` (`CyclomaticComplexity`) e `reportLevel=1` (`CognitiveComplexity`) restrita às 5 classes. **Todos os 18 pares método/valor batem**, inclusive `PaymentProofStrategy.process` 8/4 e `FecharMesServiceImpl.fechar` 9/9, e as cinco somas por classe fecham em 7, 10, 11, 10 e 16. A inferência "cognitiva ausente = 0" está correta e é observável: os métodos sem entrada cognitiva são exatamente os de ciclomática 1.
- claim: os dois `CyclomaticComplexity` do baseline são `AtualizarFuncionarioServiceImpl.atualizar` = 12 (NPath 2048) e `CadastrarFuncionarioServiceImpl.validarDadosPagamento` = 18 (cognitiva 17) | evidence consulted: mensagens literais do `target/pmd.xml`. Confirmado, inclusive o threshold 200 do NPath e o 15 da cognitiva.
- claim: hash do ruleset congelado | evidence consulted: `sha256sum financas_bot_telegram/pmd-ruleset.xml` = `1b06f4a8ec4358a85561baea89a3435ec6a835b9680f858833c765d35c710a72`. Confere com o status.
- claim: reprodutibilidade / dois formatos de relatório | evidence consulted: meu run gerou `target/pmd.xml` com **15.041 bytes** e `target/reports/pmd.html` com **30.112 bytes** — os mesmos tamanhos registrados no status, em máquina e momento diferentes. O XML sai mesmo com `<format>html</format>`, como descrito.
- claim: `MissingSerialVersionUID` — 25 violações, todas em classe de exceção | evidence consulted: listei as 25 na reconstrução da rodada 1: **todas** em arquivos `*Exception.java`. A justificativa da exclusão ("nada neste stack serializa exceção") é verificável e a exclusão é defensável — ver `Optional Improvements` para a única ressalva.
- claim: `SimplifiedTernary` — 5 violações, todas da forma `x != null ? x : true/false` | evidence consulted: li as 5 linhas apontadas: `FuncionarioController:100`, `AdiantamentoMapper:42`, `FuncionarioMapper:49` e `:52`, `PedidoPagamentoMapper:54`. **Todas** têm exatamente essa forma (4 com `true`, 1 com `false`). A descrição do status é fiel.
- claim: gate `testes` = fail com 48 erros, pré-existente e alheio à task | evidence consulted: `./mvnw test` na minha sessão → `Tests run: 422, Failures: 0, Errors: 48, Skipped: 0`, `BUILD FAILURE`, com 12 ocorrências de `Could not find a valid Docker environment` e `NpipeSocketClientProviderStrategy` no log; 11 classes `*IntegrationTest` mais a `AbstractIntegrationTest`. `testes_total: 422` e `testes_novos: 0` conferem. **Não reexecutei a suíte no commit-base**, mas a causalidade está excluída estruturalmente: o diff não toca nenhuma classe de produção ou de teste, e o único arquivo de build alterado ganha um plugin **sem `<executions>`**, portanto não ligado a nenhuma fase. A afirmação do status se sustenta.
- claim: critério de aprendizado — o PMD não emite nada sobre `LegendaParser:28` | evidence consulted: nem no ruleset congelado nem na reconstrução da rodada 1 há qualquer violação em `LegendaParser` fora da linha 21. Confirmado.
- claim: critério de aprendizado — o PIT não vê o bug de locale | evidence consulted: `QA-012-piloto-pit-mutation-testing.md` registra `LegendaParser` com 8 mutantes / 6 mortos / 2 sobreviventes, ambos na linha 28, sem nenhum achado de locale. Julgamento sobre a força dessa evidência: **suficiente**, e por três razões verificáveis — (1) `LegendaParser.java` não muda desde `c7889ec` (BE-03), muito antes da QA-012, então o relatório reutilizado descreve **o mesmo arquivo byte a byte**; (2) o `pom.xml` não declara `<mutators>`, logo o run foi com o conjunto `DEFAULTS`, e nenhum mutador desse conjunto altera chamada não-`void` como `toLowerCase()`; (3) o argumento estrutural do status ("não existe mutante que expresse 'e se o locale fosse turco?'") não depende do run, é uma propriedade do conjunto de mutadores. Rodar PIT de novo não mudaria a conclusão, e o plano declara `mutation_gate: false`. Não é apoio fraco.

## Delta Review — rodada 2

**Modo:** `delta-review`. **Escopo:** `git diff b8de250..767706b`. **Veredito do delta: aprovado com observações.**

Nada foi aceito por leitura de texto. Reexecutei o PMD duas vezes (com e sem teste), refiz o `sha256sum`, e **re-rastreei o caminho do `tipoPagamento` do zero**, sem me apoiar no rastreamento da rodada 1 nem na narrativa nova.

### D1 — `F1` está corrigido nos três lugares, e a redação nova é verdadeira

| Lugar | Situação |
|---|---|
| Status, leitura interpretada nº 2 (linhas 147–163) | corrigido, com nota explícita de correção |
| Status, débito 1 | corrigido na mesma linha |
| `pmd-ruleset.xml`, comentário de `UseLocaleWithCaseConversions` (linhas 31–46) | corrigido — era o ponto que a rodada 1 exigia decidir |

Re-verifiquei cada afirmação da redação nova contra o código, e todas se sustentam:

- `PaymentProofStrategy:81` → `matcher.group(2).toUpperCase()`; o `COMPROVANTE_PATTERN` é `#(\d+)\s+(.+)`, logo `group(2)` é texto livre — `#123 pix` dá `"pix"`. Confirmado.
- `RegistrarComprovanteServiceImpl.execute(Long, String, ...)` linha 50: `.tipoPagamento(tipoPagamento)` direto no builder, sem conversão. Confirmado.
- `Comprovante.tipoPagamento` = `String` (linha 22); `ComprovanteEntity.tipoPagamento` = `String` (linha 35); `ComprovanteMapper` só copia (linhas 20 e 33). Confirmado.
- `V1__initial_schema.sql:18` = `tipo_pagamento VARCHAR(255),` — contei as linhas, é a 18 mesmo. **E fui além do que a rodada 1 tinha feito:** `grep -rn "tipo_pagamento"` em **todas** as 7 migrations (V1–V7) devolve **essa única linha**. Nenhuma migration posterior adiciona `CHECK`, muda o tipo ou renomeia a coluna. O "sem `CHECK`" é verdadeiro para o schema inteiro, não só para o `CREATE TABLE`.
- "Não há `Enum.valueOf` nesse caminho": `grep -rn "getTipoPagamento\|TipoPagamento.valueOf"` em `src/main/java` devolve **3 ocorrências**, todas benignas — as duas do `ComprovanteMapper` e `PaymentProofStrategy:93`, que só formata a mensagem de sucesso. **Não existe consumidor que converta a string em enum.** Confirmado.
- "`parseTipo` devolve `OUTRO`" (frase nova no XML): `LegendaParser.parseTipo` inicializa `tipoEncontrado = TipoPagamento.OUTRO` e só o substitui dentro do `if`; sem casar palavra-chave, devolve `OUTRO`. Confirmado.

**Conclusão do D1: "corrupção persistida" é a descrição correta, e a descrição do caminho até o banco está certa.** A troca de erro por erro que eu procurava não aconteceu. O ranqueamento "pior que o anterior" saiu, e o texto que o substituiu ("mesma natureza; o nº 2 é o mais difícil de reverter porque o dado fica") é defensável e não reintroduz hierarquia falsa.

Uma nuance de precisão, **não é achado**: `İ` (U+0130) só é gravado intacto se a coluna for `utf8mb4`. A `V1` não declara charset e o default do MySQL 8 é `utf8mb4`, então na prática a afirmação vale; num banco `latin1` o insert falharia com `Incorrect string value` em vez de corromper. O status não precisa dizer isso — registro para quem for corrigir o bug.

### D2 — Hash novo e baseline: conferem

```
$ sha256sum financas_bot_telegram/pmd-ruleset.xml
5100b68f387a0f0d4a8a6d8ba6540c715854709869790373df1118acb71755a9
```

Bate com o hash declarado no status. O descartado (`1b06f4a8…`) aparece **em um único lugar** no repositório inteiro — a linha da tabela que o marca como "descartado, não usar" (`grep -rn "1b06f4a8"` em todo `*.md`/`*.xml`). Não sobrou referência viva ao hash velho, e o `STATE.md` continua sem hash nenhum, como o status afirma.

Baseline reexecutado por mim, **depois** da edição do XML:

```
$ ./financas_bot_telegram/mvnw pmd:pmd -f financas_bot_telegram/pom.xml
total 22 · files 18 · errors 0 · configerrors 0
AvoidCatchingGenericException 10 · UseLocaleWithCaseConversions 5 · MutableStaticState 2
CyclomaticComplexity 2 · AvoidThrowingNewInstanceOfSameException 1 · NPathComplexity 1
CognitiveComplexity 1

$ ./financas_bot_telegram/mvnw pmd:pmd ... -Dpmd.includeTests=true
total 23 — a diferença é NotificacaoComprovanteListenerIntegrationTest:69 (AvoidCatchingGenericException)
```

**22 produção / 1 teste, distribuição idêntica à da rodada 1.** A afirmação "só comentário mudou, nenhuma regra entrou, saiu ou teve propriedade mexida" é verdadeira e verificável de duas formas independentes: pelo `git diff` do XML (só linhas de comentário) e pelo run.

### D3 — `F2`–`F5` e o débito 1b

- **`F2` — atendido no XML.** O bloco agora separa detecção de mensagem ("Defeito da MENSAGEM da regra, nao da deteccao — a forma detectada existe mesmo"), incorpora o experimento dos 4 formatos com o caso exato (`if (c) return false; return e;` → PMD sugere `||`, cabe `&&`) e cria o gatilho `REAVALIAR em upgrade de PMD`. É exatamente o que faltava. **Ressalva em `F6`:** o corpo do status (item 6–7) não recebeu a mesma reescrita.
- **`F3` — atendido.** O absoluto sumiu: "Nenhum dos dois e visivel para o PIT, e um teste comum so pegaria se fosse rodado sob outro locale." Alinhado ao status.
- **`F4` — atendido no runbook, com o número certo.** O `⚠️` novo cita `LawOfDemeter` 2 sem build / 26 com, declara o ruleset congelado insensível (22 nos dois casos) e amarra a ressalva a quem for medir o item #7 com ruleset diferente. A atribuição ("medido pelo Reviewer da QA-013") é honesta. **Ressalva em `F6`.**
- **`F5` — atendido.** O comentário do `pom.xml` agora declara que `minimumPriority=5` é explícito de propósito **apesar de ser o default**, e explica o porquê (não esconder regra sem discutir regra). Não afirma mais estar filtrando algo.
- **Opcional aceito (`SimplifiedTernary`)** — verifiquei a afirmação nova, que é factual e nova no artefato: `PedidoPagamentoMapper:54` é `entity.setFechado(domain.getFechado() != null ? domain.getFechado() : false);` — de fato `: false`, e de fato a versão colapsada seria `!= null && x`, defensável. **Correto.** Das 5 ocorrências, é a única com `false`, como a rodada 1 já tinha apurado.
- **Débito 1b — a afirmação de que `PedidoController:64` está entre as 5 do baseline é verdadeira.** Extraí as 5 `UseLocaleWithCaseConversions` do `target/pmd.xml` do meu próprio run: `PedidoController:64`, `PedidoSpecs:42`, `ResumoMesServiceImpl:38`, `PaymentProofStrategy:81`, `LegendaParser:21`. As 2 do piloto + as 3 do débito 2 fecham em 5, e o ajuste do débito 2 ("restam **2** não lidas uma a uma") está aritmeticamente certo.
- **`StatusPedido` tem exatamente `PENDENTE`, `PAGO`, `CANCELADO`** — nenhum com `i`. O rótulo "inócuo por acidente, não por proteção" está correto; acrescento que há **também** proteção real, o `try/catch (IllegalArgumentException)` de `PedidoController:65-67`, que o status não menciona e que reduz o impacto do 1b de "explode" para "400 com mensagem enganosa".
- **Caracterização nova das 2 ocorrências não lidas** (débito 2: "filtro/agregação, efeito provável é registro sumindo do resultado em silêncio") — li as duas para não deixar passar uma segunda afirmação não verificada: `PedidoSpecs.comBusca` monta `"%" + busca.toLowerCase() + "%"` para um `LIKE`, e `ResumoMesServiceImpl.obter` monta o mesmo pattern para a query de agregação. A caracterização é **fiel**, e o rótulo "provável" está correto — as duas não foram lidas violação a violação, e o status não finge o contrário.

### D4 — Consistência entre status, XML e runbook após as correções

Cruzei os três artefatos linha a linha. **A correção do `F1` está consistente nos três.** Sobraram duas frases antigas que agora divergem do artefato corrigido ao lado — nenhuma é falsa no seu contexto, mas as duas são resíduo do fix, não texto original inocente. Estão em `F6`, severidade `low`, e **não bloqueiam**.

## Findings (by severity)

> Rodada 1: `F1` (high) e `F2`–`F5` (low). **`F1` está resolvido** (ver `D1`); `F2`–`F5` estão atendidos (ver `D3`). `F6` é novo, da rodada 2.

- critical: nenhum.

- high:
  - **F1 — [RESOLVIDO na rodada 2 — ver `D1`] Afirmação falsa de impacto em produção: `PaymentProofStrategy:81` não alimenta `Enum.valueOf` e não "derruba o processamento da mensagem".**
    O status afirma, na leitura interpretada da violação 2: *"Mesma família, direção oposta e consequência mais dura, **porque essa string vira `enum`** (...) Aqui não degrada em silêncio: **explode**. Um comprovante `#123 pix` derrubaria o processamento da mensagem."* E repete no débito 1: *"O segundo lança `IllegalArgumentException` e derruba o processamento da mensagem."*
    O rastreamento do valor mostra o contrário: `matcher.group(2).toUpperCase()` é passado como `String` para `RegistrarComprovanteUsecase.execute(Long, String, ...)`, entra em `Comprovante.tipoPagamento` (`String`), é copiado por `ComprovanteMapper` para `ComprovanteEntity.tipoPagamento`, que é `@Column(name = "tipo_pagamento") private String`, persistido em `V1__initial_schema.sql:18` como **`tipo_pagamento VARCHAR(255)`**, sem `CHECK`. O único consumidor do valor é a mensagem de sucesso (`comprovanteSalvo.getTipoPagamento()` em `PaymentProofStrategy:93`). Um `grep` por `valueOf(` em todo `src/main/java`, descontando os `valueOf` de tipos primitivos, devolve **uma única ocorrência**: `PedidoController:64` (`StatusPedido.valueOf(status.toUpperCase())`), que fica em outro fluxo — REST, não bot — e ainda por cima está dentro de `try/catch (IllegalArgumentException)` que devolve 400.
    Efeito real sob locale turco em `#123 pix`: o comprovante é gravado com `tipo_pagamento = "PİX"` e a mensagem de confirmação mostra `PİX`. **Corrupção silenciosa de dado, não exceção.** A classificação `(a) problema real` continua correta; o que está errado é o mecanismo, a consequência e o ranqueamento ("pior que o anterior"), e é justamente esse ranqueamento que vai para o registro de débitos que o planner consome.
    Por que isso é `high` e não `medium`: é exatamente o modo de falha que o plano mandou caçar (*"evidência que não sustentava a afirmação"*, 5 achados na QA-012), o entregável da task **é** a justificativa escrita, e o erro está no item que o próprio status elege como o achado de maior valor de todo o ruleset.

- medium: nenhum.

- low:
  - **F6 — [NOVO na rodada 2] Duas frases ficaram para trás e agora divergem do artefato que foi corrigido ao lado.** Nenhuma é falsa no seu contexto; as duas são resíduo do fix, e as duas ficam no **status**, que é o artefato que o planner lê.
    1. **`F2` foi corrigido no XML e não no corpo do status.** O `pmd-ruleset.xml` agora diz "Defeito da MENSAGEM da regra, nao da deteccao"; o status, no item 6–7 da leitura interpretada, mantém *"Uma regra cuja remediação está errada para a forma que ela mesma sinaliza produz ruído"* — exatamente a formulação que o `F2` pedia para trocar. Pior: a tabela da seção "Revisão independente" do próprio status declara esse ponto como **"Ajustado"**, o que faz o documento contradizer a si mesmo a 140 linhas de distância. O parágrafo do item 6–7 continua tecnicamente correto (a sugestão *emitida* está mesmo errada), então não é afirmação falsa — é a mesma imprecisão de atribuição, sobrevivendo no artefato mais lido.
    2. **`F4` foi corrigido no runbook e não na linha equivalente do status.** O runbook ganhou o `⚠️` que separa "não precisa de suíte verde" de "não precisa de build"; o status, em "Próximos passos", mantém *"Ao contrário do PIT, o PMD não precisa de suíte verde nem de build — analisa fonte"*. A parte que carrega peso ("roda agora, com os 48 testes vermelhos") é verdadeira e foi verificada; o "nem de build" é a generalização que o `F4` derrubou, e agora o status afirma em geral o que o runbook, no mesmo repositório, ressalva.
    Custo de fechar: duas frases. **Não bloqueia o merge** — o artefato congelado (XML) e o operacional (runbook) estão certos, e o número de baseline não depende de nenhuma das duas.
  - **F2 — [ATENDIDO no XML na rodada 2 — ver `D3` e `F6.1`] O motivo (b) da remoção de `SimplifyBooleanReturns` é verdadeiro, mas está descrito como se fosse um defeito de *remediação* da regra, quando é um defeito da *mensagem*.** A regra detecta uma simplificação legítima, e a simplificação correta (`return caption != null && COMPROVANTE_PATTERN.matcher(caption.trim()).matches();`) é equivalente ao original e não lança NPE. O que está quebrado é o template de mensagem do PMD 7.7.0, que além de não substituir os placeholders **troca o operador** em dois dos quatro formatos (verificado por experimento controlado, acima). Isso não desfaz a decisão — regra cuja saída não é acionável e induz a erro é ruído legítimo de remover —, mas a formulação atual do XML ("regra cuja remediação está errada") atribui à regra um defeito que é da versão. Sugestão de redação e de vínculo com upgrade de PMD em `Optional Improvements`.
  - **F3 — [ATENDIDO na rodada 2 — ver `D3`] Absoluto no XML mais forte do que o do status.** O comentário do `pmd-ruleset.xml` diz que os dois bugs de locale não são visíveis "para teste ou para o PIT". O status é mais preciso e admite que um teste rodado sob `-Duser.language=tr` pegaria. Como o XML é o artefato congelado e citado pelo hash, é ele que vai ser lido daqui a seis meses.
  - **F4 — [ATENDIDO no runbook na rodada 2 — ver `D3` e `F6.2`] O runbook generaliza demais ao dizer que o PMD "não precisa de build prévio".** A frase é verdadeira para o ruleset congelado — verifiquei: rodando o ruleset congelado **sem** as classes compiladas no auxclasspath o resultado continua sendo 22 violações, idênticas. Mas não é verdadeira para o PMD em geral: rodando `LawOfDemeter` **sem** `target/classes` no auxclasspath obtive **2** violações; **com** `target/classes`, **26**. Regras que dependem de resolução de tipo mudam de resultado conforme o estado do build. Como o item #7 vai calcular Δ sobre número de violações, a ressalva importa se alguma regra sensível a tipo entrar numa curadoria futura. (Consequência colateral útil: a rodada 1 do status só é reproduzível com o projeto compilado — foi assim que consegui reproduzir os 94.)
  - **F5 — [ATENDIDO na rodada 2 — ver `D3`] `<minimumPriority>5</minimumPriority>` é o default do plugin** (`default-value="5"` no descritor). Explicitar não é erro e o comentário justifica bem a intenção; registro só para que ninguém leia isso como configuração ativa que mudou algo no número.

## Required Fixes

**Estado após a rodada 2: os 2 `Required Fixes` estão fechados. Nenhum `Required Fix` novo.** O `F6` é `low` e entra em `Optional Improvements`, não aqui.

- ✅ **Fix 1 — fechado.** Corrigido nos dois lugares do status e verificado por rastreamento independente do código (`D1`). O implementador conferiu o achado antes de aceitá-lo, o que é o comportamento certo — e chegou à mesma conclusão por caminho próprio.
- ✅ **Fix 2 — fechado pela saída mais cara e mais correta.** A rodada 1 aceitava as duas saídas (corrigir o XML e refazer o hash, ou registrar a decisão de não mexer). O implementador escolheu corrigir, refez o hash, reexecutou o baseline para provar que o conjunto de violações não mudou, e documentou a troca numa subseção própria com o hash velho marcado como descartado. **Verifiquei os três: hash novo bate, baseline continua 22/1, e o hash velho não sobrevive em lugar nenhum além da linha que o aposenta.**

<details>
<summary>Texto original dos Required Fixes da rodada 1 (histórico)</summary>

1. **Corrigir `F1` no status report**, nos dois lugares (leitura interpretada da violação 2 e débito 1). O texto precisa refletir o que o código faz: `tipoPagamento` trafega e é persistido como `String` (`VARCHAR(255)`, sem `CHECK`), não há `Enum.valueOf` nesse fluxo, e o efeito sob locale turco é **gravar e exibir `PİX`** — corrupção silenciosa, não exceção. A classificação `(a)` se mantém; o ranqueamento "pior que o anterior / explode / derruba o processamento" sai ou é substituído pelo impacto real. Se quiser preservar o exemplo do `Enum.valueOf`, ele existe de verdade em `PedidoController:64` — e vale registrar que hoje é inócuo, porque nenhum valor de `StatusPedido` (`PENDENTE`, `PAGO`, `CANCELADO`) contém a letra `i`.
2. **Decidir e registrar o efeito de `F1` sobre o `pmd-ruleset.xml`.** O comentário do bloco `UseLocaleWithCaseConversions` (linhas 31–39) apoia a justificativa da regra na mesma associação. Se o texto for corrigido, o **sha256 muda** e o hash registrado no status e destinado ao pré-registro do experimento tem de ser refeito — o que é barato agora, porque o hash ainda não foi propagado para o `STATE.md`, e caro depois. Se a decisão for **não** mexer no XML, isso precisa estar escrito no status como decisão consciente, com a ressalva de que a frase vale como fato de JDK e não como descrição do fluxo do comprovante. Qualquer uma das duas saídas fecha o achado; o que não pode é ficar divergente entre status corrigido e XML não corrigido.

</details>

## Optional Improvements

**Da rodada 2 (novos):**

- **`F6.1`** — no status, item 6–7 da leitura interpretada, trocar *"Uma regra cuja remediação está errada para a forma que ela mesma sinaliza produz ruído"* pela mesma formulação que já está no XML (defeito da mensagem, não da detecção). Uma frase; fecha a contradição com a tabela da seção "Revisão independente" do próprio documento.
- **`F6.2`** — no status, em "Próximos passos", cortar *"nem de build"* ou anexar a ressalva do runbook. Uma frase.
- Registrar, junto do débito 1b, que `PedidoController:65-67` **já** tem `try/catch (IllegalArgumentException)` devolvendo 400: o modo de falha ali nunca foi "derruba o processamento", é "400 com mensagem enganosa". O status descreve o 1b como inócuo por acidente e está certo, mas a proteção real existente reforça o argumento e evita que alguém priorize o 1b acima do que ele vale.

**Da rodada 1 (todas atendidas; texto mantido como registro):**

- `F2`: trocar, no `pmd-ruleset.xml`, "regra cuja remediação está errada" por algo como "no PMD 7.7.0 a mensagem sai com placeholder cru e com o operador trocado para este formato (`if (c) return false; return e;` → sugere `!c || e`, quando o correto é `!c && e`) — reavaliar no próximo upgrade de PMD". Ganha precisão e cria o gatilho de reavaliação, que hoje não existe.
- `F3`: alinhar o comentário do XML ao texto do status ("invisível para o PIT; invisível para os testes atuais, que rodam no locale da máquina").
- `F4`: uma linha no `ROTEIRO-TESTES-BACKEND.md` §2.0 registrando que regras que dependem de resolução de tipo podem variar com o estado do `target/classes`, e que o ruleset congelado foi verificado como **insensível** a isso.
- Exclusão de `SimplifiedTernary`: é a mais fraca das nove, e o próprio status admite ("preferência de legibilidade, não sinal de qualidade"). Não peço reversão — a honestidade do rótulo é o que o critério exige —, mas vale marcá-la no XML como candidata explícita a reavaliação, do mesmo jeito que `AvoidDuplicateLiterals` já está.
- Exclusão de `MissingSerialVersionUID`: verifiquei que as 25 são todas em `*Exception.java` e a justificativa procede. Sugiro só acrescentar o gatilho de revisão que o débito 7 já antecipa — hoje ele existe no status, não no XML congelado, e é o XML que sobrevive.

## Technical Debt Identified

> Atualizado após a rodada 2. Os dois primeiros itens da rodada 1 foram absorvidos pelo status e **não precisam mais ser consolidados como débito de revisão** — ficam registrados com o desfecho.

- item: ~~A afirmação de que `PaymentProofStrategy:81` lança `IllegalArgumentException` e derruba o processamento é falsa (ver `F1`)~~ — **resolvido na rodada 2** | impact: era o risco de o planner priorizar o débito 1 como incidente de disponibilidade; o status agora descreve corrupção persistida e os dois bugs de locale como de mesma natureza | suggested_action: nenhuma — o débito 1 do status pode ser consolidado em `pendencias-tecnicas.md` como está. A correção do bug em si continua fora do escopo desta task.
- item: ~~O único `Enum.valueOf` alimentado por `toUpperCase()` sem `Locale` em produção é `PedidoController:64`~~ — **absorvido pelo status como débito 1b, com o mapeamento correto** | impact: nenhum residual | suggested_action: ao consolidar o 1b, acrescentar que `PedidoController:65-67` já captura `IllegalArgumentException` e devolve 400 — o impacto real é mensagem enganosa, não indisponibilidade.
- item: **[NOVO, rodada 2]** O status manteve duas frases da versão anterior que divergem do XML e do runbook já corrigidos (`F6`) | impact: baixo e localizado — o artefato congelado e o runbook estão certos, mas o status é o que o planner lê, e uma delas contradiz a tabela de revisão do próprio documento | suggested_action: duas frases; pode ser fechado no mesmo commit que atender a qualquer outro ajuste, ou ficar como está sem risco para a medição.
- item: `SimplifyBooleanReturns` do PMD 7.7.0 emite mensagem com placeholder não substituído e com operador trocado em 2 dos 4 formatos (bug upstream) | impact: a exclusão da regra é consequência de um defeito de versão, não de uma propriedade permanente da regra; sem gatilho, a exclusão vira definitiva por inércia | suggested_action: amarrar a reavaliação ao próximo upgrade de PMD, junto com a nova curadoria e o novo hash.
- item: Resultado de regras dependentes de resolução de tipo varia conforme `target/classes` esteja populado (`LawOfDemeter`: 2 sem, 26 com) | impact: metodologia do item #7 (Δ de violações) precisa fixar o estado do build se alguma regra sensível a tipo entrar no ruleset; hoje o ruleset congelado é insensível, verificado | suggested_action: registrar como premissa do item #7 no backlog da sprint.
- item: O gate `testes` está vermelho por Docker/Testcontainers indisponível, e a QA-012 registrou `testes: ok` com o mesmo `testes_total: 422` em 2026-08-10 | impact: ninguém sabe ainda se a mudança é de ambiente ou de repositório; enquanto não se souber, toda task da sprint vai fechar `parcial` | suggested_action: é a pendência de humano nº 1 do status e concordo que está fora do escopo desta task; merece FIX próprio, não conserto de carona.

## Blocked Validations / Uncertainty

**Da rodada 2:**

- Não reexecutei `mvn test` no delta. Motivo: o delta não toca nenhuma classe Java (`git diff --stat b8de250..HEAD` = 4 arquivos, 3 de documentação/configuração de PMD e o status), e o gate `testes` continua `fail` pela mesma causa pré-existente — Docker indisponível ao Testcontainers. **Segue como pendência de humano, não como achado desta task**, exatamente como na rodada 1.
- Não reexecutei a reconstrução da rodada 1 (94 violações) nem a passada de medição de complexidade. Motivo: nenhuma delas depende de algo que o delta alterou — o XML só mudou em comentário, e o run confirmado de 22/1 prova que o conjunto de regras é o mesmo. Reexecutar não mudaria conclusão.
- A nuance do charset (`İ` só grava intacto em coluna `utf8mb4`) foi **inferida**, não medida: não subi MySQL sob locale turco para observar o insert. O default do MySQL 8 é `utf8mb4` e a `V1` não declara charset, então a leitura do status vale na configuração real; registro como inferência para não vender como execução.

**Da rodada 1 (mantidas):**

- Não reexecutei a suíte de testes no commit-base (`origin/integration/04-instrumentacao-qualidade`) para provar empiricamente a pré-existência dos 48 erros. Motivo: criar worktree ou trocar de branch mexeria no estado do repositório sob revisão. A causalidade foi excluída por argumento estrutural (nenhum arquivo de código ou teste no diff; plugin adicionado sem `<executions>`), e a evidência de `git stash` do implementador cobre o outro lado. Considero o risco residual desprezível, mas registro que **não** é verificação por execução.
- A reconstrução da rodada 1 e a passada de medição de complexidade foram feitas em **projetos-cópia** (cópia do `pom.xml` do backend apontando `sourceDirectory` para o fonte do repositório), porque `rulesets` não tem user property e eu não podia editar o `pom.xml` sob revisão. Os números conferiram com o status em todos os pontos, o que valida indiretamente o método, mas formalmente é um harness equivalente, não o comando original.
- Mutation gate: `not-applicable`. O plano declara `mutation_gate: false` e a task não altera classe Java. Verifiquei, ainda assim, que o bloco `targetClasses` do `pitest-maven` no `pom.xml` **não foi tocado** pelo diff — não houve encolhimento de escopo de medição.
- QA: `not-applicable`, com a justificativa vinda do plano (`fluxos_qa: []`, task não altera comportamento de produto).

## Review Artifact Path
- `docs/sprints/04-instrumentacao-qualidade/avaliacoes/review-QA-013-pmd-ruleset-curado-e-piloto.md`

## Plan Deviations

- **Rodada 2: nenhum desvio novo.** O delta continua dentro do escopo do plano — corrige prosa e comentários, não altera regra do ruleset, não toca classe de produção ou de teste, e mantém `desvios: 2` no frontmatter do status, coerente com o conteúdo. O `pom.xml` mudou apenas em comentário (o `<minimumPriority>5</minimumPriority>` continua lá, com o mesmo valor).
- Os 2 desvios declarados pelo implementador foram verificados e **procedem**:
  - Desvio 1 (`ExcessiveMethodLength` não existe no PMD 7, substituída por `NcssCount`; três regras que a intuição põe em `errorprone` moram em `design`): confirmado por leitura de `category/java/design.xml` e `category/java/errorprone.xml` dentro do `pmd-java-7.7.0.jar`.
  - Desvio 2 (a classe de contraste não produziu o contraste esperado; `LoosePackageCoupling` removida pelo próprio PMD): confirmado — as 3 violações de `FecharMesServiceImpl` na rodada 1 são todas `LawOfDemeter` na linha 99 e nenhuma sobreviveu à curadoria; reproduzi o warning de remoção da `LoosePackageCoupling` textualmente. A decisão de **não** ampliar o piloto é defensável: o critério de aprendizado foi satisfeito pelo `LegendaParser` com os dois exemplos exigidos, e o plano só obriga a ampliar se as 5 classes não permitirem os dois exemplos.
- Todos os demais critérios de aceitação do plano foram verificados e estão cumpridos: plugin fora do ciclo de vida (sem `<executions>`, sem `pmd:check`), versão em `<properties>`, ruleset com justificativa por regra e por categoria excluída, relatório em XML e HTML, `testes_total = 422` / `testes_novos = 0`, baseline por categoria com produção e teste separados, complexidade das 5 classes com a limitação declarada dentro da tabela, leitura interpretada das 8 violações com uma frase por violação, correspondência `(c)` ↔ ruleset final, hash registrado, runbooks atualizados, branch e território corretos. O único critério que a revisão reprova é o de **veracidade da justificativa**, via `F1`.
- Sobre a ressalva declarada pelo implementador (regras que dispararam fora do piloto decididas por amostra, não violação a violação): **é suficiente e está honestamente demarcada**. Auditei as duas exclusões que o dispatch pediu — `MissingSerialVersionUID` (as 25 são todas em `*Exception.java`, nenhuma serialização real no stack) e `SimplifiedTernary` (as 5 têm exatamente a forma descrita) — e as duas descrições são fiéis ao código. Nenhuma exclusão é injustificada; a mais fraca é `SimplifiedTernary`, e o status já a rotula como preferência de legibilidade em vez de vendê-la como sinal de qualidade.

## Verdict
- approved-with-notes

**Veredito vigente, após o `delta-review` (rodada 2).** As duas observações são o `F6`, `low`, duas frases no status; **nenhuma bloqueia o merge** e nenhuma afeta o número de baseline, o hash ou o artefato congelado.

O que sustenta a aprovação, em ordem de peso:

1. `technical_justifications_true` passou de `fail` para `pass`. O `F1` foi corrigido nos **três** lugares — inclusive no XML, que era o lugar caro — e a redação nova foi verificada contra o código, não contra o texto: o caminho `PaymentProofStrategy:81 → execute(String) → Comprovante(String) → ComprovanteEntity(String) → tipo_pagamento VARCHAR(255)` está certo, não há `Enum.valueOf` em nenhum consumidor, e **nenhuma das 7 migrations** adiciona `CHECK` à coluna. "Corrupção persistida" é a descrição correta.
2. O hash novo (`5100b68f…`) confere por `sha256sum`, o baseline continua **22 produção / 1 teste** com distribuição idêntica, e o hash descartado não sobreviveu em nenhum lugar do repositório além da linha que o aposenta.
3. `F2`–`F5` foram atendidos com precisão maior do que eu havia pedido, e a única sugestão opcional aceita (`SimplifiedTernary`) trouxe uma afirmação nova que também confere (`PedidoPagamentoMapper:54` usa mesmo `: false`).
4. O débito 1b é verdadeiro: `PedidoController:64` está mesmo entre as 5 `UseLocaleWithCaseConversions` do baseline, `StatusPedido` tem mesmo só `PENDENTE`/`PAGO`/`CANCELADO`, e a aritmética do débito 2 (5 = 2 do piloto + 3 fora, das quais 2 seguem não lidas) fecha.

Registro o que me parece o traço mais relevante desta rodada: o implementador **verificou o achado antes de aceitá-lo** e chegou à mesma conclusão por rastreamento próprio, e escolheu a saída mais cara do `Required Fix` 2 — mexer no XML e refazer o hash — quando a barata estava disponível e seria aceita. O `F6` é o preço de ter corrigido em três lugares e ter deixado dois textos vizinhos para trás; é resíduo de correção, não descuido de origem.

**Gates:** `mutation_gate: not-applicable` (o plano declara `false`, o delta não toca classe Java e o bloco `targetClasses` do PIT continua intocado — reconferido no diff do delta). `qa_required: false` → QA `not-applicable`, justificativa vinda do plano. Gate `testes` = `fail` **pré-existente**, por Docker indisponível ao Testcontainers; é pendência de humano e **não** é condição desta aprovação.

<details>
<summary>Veredito da rodada 1 (histórico): rejected</summary>

Rejeitado por **um** achado: `technical_justifications_true` = `fail` (`F1`). A regra de revisão é explícita — não se aprova com premissa reprovada —, e o entregável declarado da task é a justificativa escrita, não o número.

Fora esse ponto, esta é a entrega mais verificável que passou por aqui nesta sprint: **todos** os números do status reproduziram exatamente na minha máquina, do baseline de 22 e da rodada 1 de 94 até os bytes do relatório e as 18 células da tabela de complexidade, e o argumento que o implementador declarou como o mais frágil da curadoria (`SimplifyBooleanReturns`) resistiu a um experimento controlado e saiu mais forte do que estava.

A correção é de prosa, não de configuração: `financas_bot_telegram/pom.xml` e `financas_bot_telegram/pmd-ruleset.xml` estão corretos como estão, com a única ressalva do comentário citado no `Required Fix` 2. Refeitos os dois itens de `Required Fixes`, um `delta-review` fecha — não é preciso repetir a validação de execução.

</details>
