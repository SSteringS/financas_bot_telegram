---
task: QA-013
titulo: "PMD — ruleset curado e piloto de leitura interpretada"
data: 2026-08-12
branch: feature/qa-013-pmd-ruleset-curado-e-piloto
responsavel: claude-back
estado: parcial
gates:
  build: ok
  lint: ok
  testes: fail
  testes_total: 422
  testes_novos: 0
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits:
  - 1a8057d
  - b8de250
  - 2b9cdc7
pr: null
desvios: 2
pendencias_humano: 0
---

# QA-013 — PMD: ruleset curado e piloto de leitura interpretada

> **Por que `estado: parcial` e não `concluido`:** todo o escopo da task foi entregue e a revisão independente aprovou, mas o gate `testes` está **vermelho por causa alheia a esta task** — 48 testes de integração falham nesta máquina por indisponibilidade do Docker para o Testcontainers, e falham **igualmente sem as minhas mudanças** (evidência abaixo). O humano decidiu em 2026-08-13 **mergear assim mesmo**, apoiado no fato de que o diff não tem nenhum arquivo `.java` (ver "Decisões pendentes"). A decisão libera o merge, mas não torna o gate verde: `concluido` exige todos os gates `ok` ou `na`, então o estado correto continua sendo `parcial`.

---

## O que foi feito

`maven-pmd-plugin` 3.26.0 (PMD 7.7.0) no `pom.xml` do backend, com a versão em `<properties>`, **fora do ciclo de vida do build** e sem o goal `pmd:check` — `mvn test`, `mvn package` e o CI continuam com o mesmo custo e não quebram por violação.

O entregável central é `financas_bot_telegram/pmd-ruleset.xml`: **lista explícita de 10 regras**, cada uma com justificativa escrita, mais o registro das regras que **dispararam e ficaram de fora**, com o motivo de cada uma. Não é referência a categoria inteira — a lista foi construída a partir do que o código real disparou.

Nenhuma classe de produção ou de teste foi tocada. O diff é: `pom.xml`, `pmd-ruleset.xml` (novo) e dois runbooks.

### Evidência de execução

| Comando | Resultado |
|---|---|
| `./mvnw pmd:pmd -f financas_bot_telegram/pom.xml` | `BUILD SUCCESS` · 22 violações · `target/pmd.xml` + `target/reports/pmd.html` |
| `./mvnw pmd:pmd ... -Dpmd.includeTests=true` | `BUILD SUCCESS` · 23 violações (produção + teste) |
| `./mvnw -q -DskipTests package` | exit **0** |
| `./mvnw test` | `Tests run: 422, Failures: 0, Errors: 48, Skipped: 0` — `BUILD FAILURE` |
| `./mvnw test` **com as mudanças da task revertidas via `git stash`** | `Tests run: 422, Failures: 0, Errors: 48, Skipped: 0` — idêntico |

Ambiente: Maven 3.9.6, **JVM 23-ea** (única instalada; o `pom.xml` compila com `--release 21` e o CI usa Temurin 21 — mesma ressalva registrada na QA-012).

### Os dois formatos de relatório

`target/pmd.xml` (15.041 bytes) e `target/reports/pmd.html` (30.112 bytes). O XML sai **mesmo com `<format>html</format>`** — o descritor do plugin declara textualmente *"XML is produced in any case, since this format is needed for the check goals"*. Não é configuração minha, é comportamento do plugin, e por isso não depende de eu manter dois formatos configurados.

### Reprodutibilidade

Dois runs consecutivos sem mudança de código, comparados por conjunto `(arquivo, linha, regra)`:

```
run1: 22 run2: 22 conjuntos identicos: True
```

Os **bytes** dos dois XML diferem, mas só no atributo `timestamp` do cabeçalho (`2026-08-12T12:55:05.487` vs `2026-08-12T12:55:15.247`). Registro isso explicitamente porque `sha256sum` do relatório **não** serve como prova de estabilidade aqui — só o conjunto de violações serve.

### O PMD analisou o projeto inteiro — verificado, não presumido

Passada de verificação com uma regra XPath `//CompilationUnit`, que dispara exatamente uma vez por arquivo efetivamente parseado:

| Escopo | Arquivos parseados pelo PMD | `find src/... -name '*.java' \| wc -l` |
|---|---:|---:|
| Produção | **191** | 191 |
| Produção + teste | **273** | 191 + 82 = 273 |

Bate exatamente, e o relatório tem **zero** elementos `<error>` (falha de parse). Isso liquida o risco "`maven-pmd-plugin` puxa versão de PMD incompatível com Java 21" do plano: o parser do PMD 7.7.0 aceitou **todos** os 273 arquivos do projeto sem um único erro.

---

## Baseline do legado (critério: número absoluto, por categoria, separando produção de teste)

Ruleset congelado, run de 2026-08-12.

| Regra | Categoria | Produção | Teste | Total |
|---|---|---:|---:|---:|
| `AvoidCatchingGenericException` | design | 10 | 1 | 11 |
| `UseLocaleWithCaseConversions` | errorprone | 5 | 0 | 5 |
| `MutableStaticState` | design | 2 | 0 | 2 |
| `CyclomaticComplexity` | design | 2 | 0 | 2 |
| `AvoidThrowingNewInstanceOfSameException` | design | 1 | 0 | 1 |
| `NPathComplexity` | design | 1 | 0 | 1 |
| `CognitiveComplexity` | design | 1 | 0 | 1 |
| `EmptyCatchBlock` | errorprone | 0 | 0 | 0 |
| `CompareObjectsWithEquals` | errorprone | 0 | 0 | 0 |
| `NcssCount` | design | 0 | 0 | 0 |
| **Total** | | **22** | **1** | **23** |

**Por categoria:** produção = 5 errorprone + 17 design. Arquivos com pelo menos uma violação: 18 de 191.

**`Q7_producao` = 22 · `Q7_teste` = 1.**

A única violação de teste é `NotificacaoComprovanteListenerIntegrationTest:69` (`AvoidCatchingGenericException`).

> **Como o número de teste foi obtido, e por que isso importa:** o run com `-Dpmd.includeTests=true` devolve **produção + teste somados** (23), não o número de teste. `Q7_teste` é a **diferença** entre os dois runs. Conferi que o conjunto de produção é subconjunto estrito do conjunto com teste — a diferença é exatamente uma violação, e ela está sob `src/test/java`.

### Armadilha que quase produziu um número falso

O primeiro run com `-Dpmd.includeTests=true` devolveu **exatamente os mesmos 22**, e teria sido registrado como "os testes não têm violação nenhuma". Está errado: o mojo `includeTests` do `maven-pmd-plugin` **não tem user property**, verificado no `plugin.xml` dentro do jar do plugin (o bloco `<parameter>` não tem `<expression>`). A flag foi **silenciosamente ignorada**.

Corrigi amarrando `<includeTests>${pmd.includeTests}</includeTests>` a uma property de projeto, que aí sim aceita override de linha de comando. Depois disso o número mudou para 23. **A mesma armadilha vale para `-Dpmd.rulesets`, que também não existe** — trocar de ruleset exige editar o `pom.xml`. Ambas estão documentadas no runbook, porque são do tipo que produz número plausível e errado.

---

## O ciclo de curadoria — 2 rodadas até estabilizar

O critério de parada do plano era o ruleset **estabilizar** (uma rodada sem nova classificação `(c)`), com teto de 3 rodadas.

| Rodada | Ruleset | Violações em produção | Violações no piloto | Novas `(c)` |
|---|---|---:|---:|---|
| 1 | `errorprone` + `design` **inteiras** | **94** | 8 | `LawOfDemeter`, `SimplifyBooleanReturns` |
| 2 | 10 regras explícitas | **22** | 2 | **nenhuma** → estabilizou |

Rodada 1 em números: `LawOfDemeter` sozinha era **26 violações (28% do total)** e `MissingSerialVersionUID` outras **25 (27%)** — mais da metade do relatório bruto eram duas regras que não sobreviveram à leitura.

---

## Leitura interpretada — as 8 violações do piloto, uma a uma

Rodada 1, ruleset amplo, restrito às 5 classes do piloto. Cada uma classificada em **(a) problema real**, **(b) falso positivo**, **(c) regra que não queremos**.

### 1. `LegendaParser:21` — `UseLocaleWithCaseConversions` — **(a) problema real**

```java
String alvo = legenda.toLowerCase();
```

`toLowerCase()` sem `Locale` usa o locale default da JVM. Não é teoria — medido nesta máquina com o JDK instalado:

```
"PIX".toLowerCase(tr-TR)  ->  "pıx"  (i sem ponto)   contains("pix") == false
"BOLETO".toLowerCase(tr-TR) -> "boleto"              (sem 'i', não afetado)
"TED".toLowerCase(tr-TR)    -> "ted"                 (sem 'i', não afetado)
"AGENDAMENTO".toLowerCase(tr-TR) -> "agendamento"    (sem 'i', não afetado)
```

Efeito concreto: numa JVM com locale turco, uma legenda contendo `PIX` deixa de ser classificada como `TipoPagamento.PIX` e cai em `OUTRO` — **silenciosamente**, sem exceção nenhuma. Das quatro palavras-chave, só `pix` tem a letra `i`, então o bug é estreito; mas é real, e o bot roda em EC2 com locale herdado do ambiente.

### 2. `PaymentProofStrategy:81` — `UseLocaleWithCaseConversions` — **(a) problema real**

```java
String tipoPagamento = matcher.group(2).toUpperCase();
```

Mesma família, direção oposta:

```
"pix".toUpperCase(tr-TR)  ->  "PİX"  (I com ponto)
```

> **Correção após revisão (achado F1 do Reviewer).** A primeira versão deste status afirmava que essa string alimenta `Enum.valueOf` e que o efeito seria `IllegalArgumentException` derrubando o processamento da mensagem — e classificava o item como "pior que o anterior". **Estava errado, e eu confirmei o erro por conta própria.** O valor trafega como `String` de ponta a ponta: `RegistrarComprovanteServiceImpl.execute(Long, String, ...)` o repassa direto para `Comprovante.builder().tipoPagamento(...)`, e o schema o persiste em `comprovantes.tipo_pagamento VARCHAR(255)` **sem `CHECK`** (`V1__initial_schema.sql:18`). Não há `Enum.valueOf` nesse caminho. O único `Enum.valueOf` alimentado por `toUpperCase()` em todo `src/main/java` é `PedidoController:64`, que é outro fluxo.

Efeito real sob locale turco: o comprovante é gravado e exibido como `PİX`. **Não explode — corrompe.** É persistido, e persiste depois que o locale for corrigido.

Os dois bugs de locale do piloto, então, têm a **mesma natureza**: degradam em silêncio, nenhum lança exceção. O nº 1 perde a classificação em memória; o nº 2 grava dado errado no banco. Nenhum dos dois é "pior" no sentido de ser mais barulhento — o nº 2 é o mais difícil de reverter, porque o dado fica.

### 3–5. `FecharMesServiceImpl:99` — `LawOfDemeter` ×3 — **(c) regra que não queremos**

```java
BigDecimal valorFinal = funcionario.getSalarioBase()
        .subtract(totalVales)
        .subtract(totalParcelas)
        .add(ajusteEfetivo);
```

Três violações na mesma expressão: `getSalarioBase` em "foreign value" (grau 1) e dois `subtract` (grau 2). `BigDecimal` é **tipo-valor imutável** e sua API é encadeada por construção — não existe forma de escrever aritmética de `BigDecimal` que não dispare essa regra. Não há ação possível derivada do achado, e a regra era 28% do relatório inteiro. **Removida do ruleset.**

### 6–7. `PaymentProofStrategy:40` e `PaymentRequestStrategy:47` — `SimplifyBooleanReturns` — **(c) regra que não queremos**

```java
String caption = dto.getCaption();
if (caption == null) return false;
return COMPROVANTE_PATTERN.matcher(caption.trim()).matches();
```

Dois motivos, ambos verificáveis no `target/pmd.xml`:

1. **A mensagem sai com os placeholders não substituídos.** O texto literal no relatório é ``This if statement can be replaced by `return !{condition} || {elseBranch};` `` — `{condition}` e `{elseBranch}` são placeholders crus, não o código. O achado **não é acionável como sai**.
2. **A sugestão, aplicada ao pé da letra, está errada.** Vira `return caption != null || COMPROVANTE_PATTERN.matcher(caption.trim()).matches();`. Com `caption == null`, o original devolve `false`; a sugestão avalia `null != null` → `false`, segue para o segundo operando e **lança `NullPointerException`** em `caption.trim()`. Não são equivalentes.

O defeito é da **mensagem**, não da detecção — a forma sinalizada existe mesmo. Mas achado que chega com placeholder cru **e** com o operador trocado é ruído, não qualidade. **Removida do ruleset**, com gatilho de reavaliação registrado no XML: se a mensagem for corrigida upstream, a regra volta a ser candidata.

> O Reviewer atacou este ponto — eu o havia sinalizado como o mais frágil da curadoria — e ele saiu **mais forte**: testando os 4 formatos da regra em arquivo controlado, confirmou que o PMD 7.7.0 troca o operador em 2 deles, e que a forma do `supports()` é justamente uma das duas.

### 8. `PaymentRequestStrategy:73` — `LawOfDemeter` — **(c)**, mesma classificação do item 3–5

```java
String dica = pedidoSalvo.getTipo() == TipoPagamento.OUTRO ? ... : "";
```

Um único `get` em objeto retornado por um método. Coberto pela mesma remoção.

### Correspondência entre `(c)` e o ruleset final

Critério de aceitação: *toda classificação `(c)` tem correspondência no ruleset final*.

| Regra classificada `(c)` | Violações no piloto | Situação no ruleset congelado |
|---|---:|---|
| `LawOfDemeter` | 4 | **removida**, justificativa escrita no XML |
| `SimplifyBooleanReturns` | 2 | **removida**, justificativa escrita no XML |

Nenhuma regra classificada `(c)` foi mantida. `MetaSignatureValidator` não produziu violação alguma nas duas rodadas.

### Honestidade sobre o escopo da classificação

As regras que dispararam **fora** do piloto (`MissingSerialVersionUID`, `ImmutableField`, `TooManyMethods`, `TooManyFields`, `DataClass`, `UseUtilityClass`, `AvoidLiteralsInIfCondition`, `SimplifiedTernary`, `AvoidDuplicateLiterals`) **não** receberam a leitura violação-a-violação que o plano exige do piloto. Para cada uma eu li **amostras** e registrei a decisão com a evidência no próprio `pmd-ruleset.xml`. Não estou apresentando essas decisões como classificação `(a)/(b)/(c)` — elas são decisões de inclusão com evidência parcial, e estão separadas no XML exatamente por isso.

Três merecem destaque por serem **falso positivo estrutural do stack**, não questão de gosto — vão disparar de novo em toda classe nova do mesmo tipo:

- `ImmutableField` em `MensagemProcessadaEntity`: campo de `@Entity` **não pode** ser `final` (Hibernate exige construtor sem argumentos e popula por campo/setter).
- `TooManyMethods` nos três `@RestControllerAdvice`: um método por tipo de exceção **é** o design correto de um advice.
- `UseUtilityClass` em `FinancasBotTelegramApplication`: a classe `main` do Spring Boot precisa de construtor público.

---

## Complexidade ciclomática das 5 classes do piloto

**A limitação, primeiro — porque ela muda como a tabela deve ser lida:** complexidade ciclomática conta ramos de decisão e **não distingue um `switch` de 10 casos (número alto, leitura trivial) de um aninhamento de 4 níveis (número parecido, ilegível)**. É contagem, não legibilidade. Por isso a tabela traz **complexidade cognitiva ao lado** — que penaliza aninhamento e quebra de fluxo, não contagem de ramos — e por isso as duas entraram juntas no ruleset.

Números obtidos numa **passada de medição** com `classReportLevel` e `methodReportLevel` em **1** (o ruleset congelado usa o threshold default, 10/80, e só reporta quem estoura):

| Classe | Método | Ciclomática | Cognitiva |
|---|---|---:|---:|
| `LegendaParser` (classe: 7) | `parseTipo(String)` | 6 | 6 |
| | `LegendaParser()` | 1 | 0 |
| `PaymentRequestStrategy` (classe: 10) | `process(PaymentMessageDTO)` | 4 | 2 |
| | `parsePedido(PaymentMessageDTO)` | 3 | 1 |
| | `supports(PaymentMessageDTO)` | 2 | 1 |
| | construtor | 1 | 0 |
| `PaymentProofStrategy` (classe: 11) | `process(PaymentMessageDTO)` | **8** | **4** |
| | `supports(PaymentMessageDTO)` | 2 | 1 |
| | construtor | 1 | 0 |
| `MetaSignatureValidator` (classe: 10) | `isValid(byte[], String)` | **6** | **4** |
| | `bytesToHex(byte[])` | 2 | 1 |
| | construtor | 2 | 1 |
| `FecharMesServiceImpl` (classe: 16) | `fechar(Long, YearMonth, BigDecimal)` | **9** | 9 |
| | `pluralVale(int)` | 2 | 1 |
| | `pluralParcela(int)` | 2 | 1 |
| | `gerarTextoFechamento(...)` | 1 | 0 |
| | `brl(BigDecimal)` | 1 | 0 |
| | construtor | 1 | 0 |

> Os zeros da coluna cognitiva são **inferidos por ausência**: a passada usou `reportLevel=1` para `CognitiveComplexity`, ou seja, reporta tudo que for ≥ 1; método que não aparece na saída tem cognitiva 0. Os valores de ciclomática são todos explícitos, porque `methodReportLevel=1` reporta até os de complexidade 1. As somas por classe fecham com os métodos listados (7, 10, 11, 10 e 16).

**Nenhum método do piloto estoura o threshold default de 10.** Os dois `CyclomaticComplexity` do baseline estão fora do piloto (`AtualizarFuncionarioServiceImpl.atualizar` = 12, `CadastrarFuncionarioServiceImpl.validarDadosPagamento` = 18).

**A limitação não é nota de rodapé — ela aparece dentro desta tabela.** `PaymentProofStrategy.process` tem ciclomática **8** e cognitiva **4**: os oito ramos são *guard clauses sequenciais* (`if (fileBytes == null) throw`, `if (caption == null || isBlank) throw`, `if (!matcher.matches()) throw`), que se leem de cima para baixo sem nenhum aninhamento. Reportar só o 8 sugeriria um método quase no limite; ele é dos mais legíveis do conjunto. No extremo oposto, `FecharMesServiceImpl.fechar` tem **9 e 9** — ali o número alto é alto de verdade. Duas métricas que coincidem num caso e divergem por um fator de 2 no outro, no mesmo piloto de 5 classes.

---

## Critério de aprendizado: o que o PMD viu e o PIT não, e vice-versa

Os dois exemplos saem da **mesma classe** — `LegendaParser`, 36 linhas —, o que torna a comparação limpa: mesmo código, mesmas linhas, duas ferramentas.

### O PMD viu e o PIT não: `LegendaParser:21`, `toLowerCase()` sem `Locale`

A QA-012 rodou o PIT exatamente sobre esta classe e publicou o resultado: **8 mutantes, 6 mortos, 2 sobreviventes, ambos na linha 28**, com leitura interpretada de cada um (`docs/sprints/04-instrumentacao-qualidade/status/QA-012-piloto-pit-mutation-testing.md`). **Nenhum achado relacionado a locale aparece ali** — nem como sobrevivente, nem como ressalva.

E não é acaso de amostragem: **o PIT não tem como ver esse bug**. Ele muta operadores de decisão, valores de retorno e chamadas a métodos `void`. `legenda.toLowerCase()` não é ramo de decisão, não é `void`, e o valor devolvido é correto **sob o locale da máquina que roda o teste**. Não existe mutante no conjunto `DEFAULTS` que expresse "e se o locale fosse turco?". O PIT pergunta *"se eu quebrar esta linha, algum teste percebe?"*; a linha não está quebrada — está **dependendo de estado global do ambiente**, e isso é estrutura, não comportamento.

Vale notar o que isso implica: um teste **novo** para `parseTipo` também não pegaria, a menos que alguém pense em rodá-lo sob `-Duser.language=tr`. Cobertura e mutation score podem ir a 100% com o bug intacto.

### O PIT viu e o PMD não: `LegendaParser:28`, o mutante de fronteira

```java
if (pos >= 0 && pos < posicaoMaisCedo) {
```

O PIT trocou `>=` por `>` e **nenhum teste falhou**. A QA-012 documentou o achado: o mutante só diverge quando a palavra-chave está no **índice 0** da legenda, e nenhuma das nove legendas de `LegendaParserTest` começa por palavra-chave — todas começam pelo valor (`"200 pix maria"`, `"150.00 Almoço boleto"`, ...). É lacuna real de asserção: `parseTipo("pix 200")` está sem cobertura de fato.

O PMD **não emite nada** sobre a linha 28 — nem no ruleset congelado, nem na rodada 1 com as duas categorias inteiras. E também não teria como: a linha está estruturalmente correta. `pos >= 0` é a forma canônica de checar retorno de `indexOf`, a expressão não é complexa, não há literal suspeito. **A pergunta "o teste exercita o caso de fronteira?" não é uma pergunta sobre estrutura**, e o PMD só lê estrutura.

### O que isso diz sobre as três ferramentas da sprint

| | Pergunta que responde | Cego para |
|---|---|---|
| Cobertura (JaCoCo, item #6) | *a linha foi executada?* | se alguém verificou o resultado |
| Mutation (PIT, QA-012) | *o teste perceberia se eu quebrasse?* | dependência de ambiente, estrutura ruim que funciona |
| Estática (PMD, esta task) | *está bem construído?* | se o comportamento está certo |

Nas mesmas 36 linhas do `LegendaParser`, o PIT achou uma lacuna de teste que o PMD não vê, e o PMD achou um bug de produção que o PIT não vê. **As duas ferramentas estavam certas e nenhuma das duas sozinha teria fechado a classe.** É o argumento concreto contra tratar qualquer uma das três como "a" métrica de qualidade.

---

## Ruleset congelado — hash para o pré-registro

| | |
|---|---|
| Arquivo | `financas_bot_telegram/pmd-ruleset.xml` |
| **sha256** | **`5100b68f387a0f0d4a8a6d8ba6540c715854709869790373df1118acb71755a9`** |
| Regras ativas | **10** (3 `errorprone` + 7 `design`) |
| PMD | 7.7.0 · `maven-pmd-plugin` 3.26.0 |
| Baseline no congelamento | produção **22** · teste **1** |

Comando: `sha256sum financas_bot_telegram/pmd-ruleset.xml`.

Alterar o arquivo invalida a comparabilidade das medições — exige nova curadoria, hash novo e registro de a partir de quando a medição nova vale.

### O hash mudou uma vez, durante a revisão — e por quê

| Hash | Situação |
|---|---|
| `1b06f4a8ec4358a85561baea89a3435ec6a835b9680f858833c765d35c710a72` | primeira versão · **descartado, não usar** |
| `5100b68f387a0f0d4a8a6d8ba6540c715854709869790373df1118acb71755a9` | **vale este** |

O achado F1 do Reviewer mostrou que o comentário da regra `UseLocaleWithCaseConversions` dentro do XML repetia a mesma afirmação falsa do status (`Enum.valueOf` lançando exceção). **Optei por corrigir o comentário e refazer o hash**, em vez de deixar o texto errado congelado.

O trade-off era real: refazer o hash agora custa esta tabela; deixar para depois significaria propagar uma justificativa falsa para o `STATE.md` e para o pré-registro do experimento, e aí o custo de corrigir passaria a incluir invalidar medição já publicada. **Nenhuma medição foi publicada ainda** — este status é o primeiro artefato a carregar o hash, e o `STATE.md` ainda não foi atualizado (é do planner). A janela para trocar sem custo é exatamente esta.

**O baseline não mudou:** rodei o PMD depois da edição e o resultado continua **22 em produção**, mesma distribuição por regra. Só comentários foram alterados; nenhuma regra entrou, saiu ou teve propriedade mexida. Isso é o que torna a troca barata — e é verificável reproduzindo o run.

---

## Revisão independente (ADR 0005)

Relatório: `docs/sprints/04-instrumentacao-qualidade/avaliacoes/review-QA-013-pmd-ruleset-curado-e-piloto.md`

**Primeiro veredito: `rejected`**, por **1 achado high** (F1) e 4 low. O Reviewer reproduziu o run por conta própria e conferiu os números um a um.

| Achado | Sev. | O que era | Como ficou |
|---|---|---|---|
| **F1** | **high** | Eu afirmava que `PaymentProofStrategy:81` alimenta `Enum.valueOf` e derruba o processamento da mensagem, e ranqueava o bug como "pior que o anterior" | **Procede.** Confirmei por conta própria: o valor trafega como `String` até `comprovantes.tipo_pagamento VARCHAR(255)` sem `CHECK`. É **corrupção silenciosa**, não exceção. Corrigido na leitura interpretada nº 2, no débito 1, e **no comentário do próprio `pmd-ruleset.xml`** — o que obrigou a refazer o hash |
| F2 | low | Eu escrevia "regra cuja remediação está errada" | Ajustado: o defeito é da **mensagem**, não da detecção. Com gatilho de reavaliação em upgrade de PMD |
| F3 | low | O comentário do XML afirmava de forma mais absoluta que o status que o bug é "invisível para teste" | Alinhado ao texto do status: um teste pegaria, se rodado sob outro locale |
| F4 | low | O runbook generalizava "não precisa de build prévio" | Corrigido com o número que o Reviewer mediu: `LawOfDemeter` dá 2 sem `target/classes` e 26 com; o ruleset congelado é insensível (22 nos dois casos) |
| F5 | low | `minimumPriority=5` já é o default do plugin | Mantido, mas o comentário agora diz que é explícito de propósito, não que está configurando algo |

**O que o Reviewer tentou derrubar e não conseguiu:** o argumento (b) da remoção de `SimplifyBooleanReturns` — que eu havia sinalizado no dispatch como o ponto mais frágil da curadoria — **saiu fortalecido**: testando os 4 formatos da regra em arquivo controlado, ele confirmou que o PMD 7.7.0 sugere `||` onde caberia `&&`. Também resistiram: os 22/23 do baseline, a distribuição por categoria, as 8 violações da rodada 1 no piloto, os 191/273 arquivos parseados, as 18 células da tabela de complexidade e as 5 somas por classe, e a afirmação de que só `pix` é afetada entre as 4 palavras-chave.

**Achado colateral que virou débito:** ao derrubar F1, o Reviewer encontrou o `Enum.valueOf` de verdade — `PedidoController:64` — inócuo hoje só porque nenhum valor de `StatusPedido` (`PENDENTE`, `PAGO`, `CANCELADO`) tem a letra `i`. Verifiquei. Está no item **1b** dos débitos.

### Delta-review: **`approved-with-notes`**

Segunda rodada com os dois `Required Fixes` aplicados. O Reviewer re-rastreou o caminho do F1 **do zero** em vez de conferir minha correção pelo texto, e foi além do que a rodada 1 tinha feito: `grep` em todas as 7 migrations confirma que **nenhuma** posterior à `V1` adiciona `CHECK`, muda o tipo ou renomeia a coluna, e não existe consumidor que converta `tipo_pagamento` em enum. `technical_justifications_true` passou de **fail** para **pass**. Hash, baseline (22/1) e o débito 1b conferidos por execução própria.

**1 achado novo, `low` (F6), já corrigido neste commit:** eu havia aplicado F2 e F4 no XML e no runbook, mas deixei para trás as duas frases equivalentes no corpo deste status — que passaram a contradizer a tabela acima. Corrigidas: a de `SimplifyBooleanReturns` (item 6–7 da leitura interpretada) e a de "não precisa de build" (Próximos passos).

**Duas observações do Reviewer que não eram achados e valem registro:**
- Ele leu `PedidoSpecs.comBusca` e `ResumoMesServiceImpl.obter` para não deixar passar uma afirmação minha não verificada: ambos montam `"%" + busca.toLowerCase() + "%"` para `LIKE`, o que confirma a caracterização "registro sumindo do resultado em silêncio" do débito 2.
- Sobre o débito 1b: `PedidoController:65-67` **já captura** `IllegalArgumentException` e devolve 400 — o impacto real ali é **mensagem de erro enganosa**, não indisponibilidade. Ajustado na tabela de débitos.
- Ressalva declarada por ele como **inferida, não medida**: `İ` só grava intacto em coluna `utf8mb4`; é o default do MySQL 8, mas a `V1` não declara o charset explicitamente. Não medimos o que acontece se o charset for outro.

---

## Desvios do plano

**2 desvios.**

### 1. `ExcessiveMethodLength` não existe no PMD 7 — usei `NcssCount`

O plano não nomeia regras, mas a intenção de cobrir "método inchado" vem de `analise-estatica-pmd-checkstyle.md`. A regra `ExcessiveMethodLength` **não está** em `category/java/design.xml` do PMD 7.7.0 (conferido na listagem do `pmd-java-7.7.0.jar`); foi absorvida por `NcssCount`, que conta **sentenças** em vez de linhas — não infla com comentário nem com quebra de formatação. Descoberto porque o PMD **recusa o ruleset inteiro** com erro de validação XML quando uma referência não resolve; não falha em silêncio.

Na mesma rodada, três regras que a intuição coloca em `errorprone` moram em `design` no PMD 7 (`AvoidCatchingGenericException`, `MutableStaticState`, `AvoidThrowingNewInstanceOfSameException`). Corrigido e anotado no XML.

### 2. A "classe de contraste" não produziu o contraste esperado

O plano escolheu `FecharMesServiceImpl` (192 linhas) para *"produzir violação real e dar matéria à classificação"*, apoiado no acoplamento já registrado em `PENDENCIAS-TECNICAS.md` (importa `DataIntegrityViolationException` na camada de aplicação).

**Não funcionou.** A classe produziu **3 violações, todas `LawOfDemeter`, todas `(c)`** — nenhuma sobreviveu à curadoria. E o PMD **não viu** o acoplamento que motivou a escolha: *"import de tipo do Spring na camada de aplicação"* é regra de **arquitetura**, dependente de convenção de pacote do projeto, e as categorias `errorprone` e `design` não têm nada assim. A regra que existe para isso, `LoosePackageCoupling`, foi **removida pelo próprio PMD** no run da rodada 1 (`[WARNING] Removed misconfigured rule: LoosePackageCoupling cause: No packages or classes specified`) — ela exige a lista de pacotes permitidos como configuração.

Não ampliei o conjunto do piloto porque o critério que dependia de contraste — os dois exemplos PMD-vs-PIT — foi satisfeito com folga pelo `LegendaParser`. Mas o achado é mais útil que o contraste que se esperava dela: **o PMD default não enxerga violação de arquitetura hexagonal**, e o débito conhecido dessa classe continua invisível para a instrumentação da sprint. Registrado como pendência técnica abaixo.

---

## Decisões tomadas durante a execução

- **Lista explícita de regras, não referência a categoria.** `<rule ref="category/java/errorprone.xml"/>` traria ~100 regras sem justificativa individual, o que reprova no critério de aceitação. A lista explícita também torna o congelamento honesto: o hash cobre exatamente o conjunto medido, e não depende de qual versão do PMD define a categoria.
- **Três regras com zero violação entraram no ruleset** (`EmptyCatchBlock`, `CompareObjectsWithEquals`, `NcssCount`). Custo zero no baseline e, portanto, zero distorção no Δ do item #7. Entram como guarda prospectiva: `catch` vazio e método inchado são nominalmente os sinais que `analise-estatica-pmd-checkstyle.md` aponta como discriminantes de código gerado por modelo — deixá-las de fora só porque o legado está limpo desarmaria justamente a medição que o experimento quer fazer. Estão marcadas como tal no XML, para o Reviewer poder discordar de forma dirigida.
- **`<format>html</format>` com o XML saindo por comportamento do plugin**, em vez de configurar dois formatos. É o comportamento documentado no descritor do plugin; configurar redundância daria a impressão de que o XML depende de mim.
- **`minimumPriority` em 5** (aceita todas as prioridades). A curadoria é no ruleset, por regra e com justificativa — filtrar por prioridade numérica no plugin esconderia regra sem discutir regra.
- **`linkXRef` desligado**: o projeto não tem `maven-jxr-plugin`; com o link ligado o relatório HTML aponta para fonte cruzado inexistente.

---

## Decisões pendentes (esperando humano)

**Nenhuma — tarefa fechada.** A única pendência que existia foi decidida pelo humano em 2026-08-13 e está registrada abaixo.

### Decidido: mergear com o gate `testes` vermelho — opção (a)

**Decisão do humano, 2026-08-13:** seguir com o merge sem os testes de integração, *"até porque não teve mexida no código"*. O raciocínio se apoia no fato verificável do diff: **zero arquivos `.java` alterados** (`git diff --name-only origin/integration/04-instrumentacao-qualidade...HEAD | grep '\.java$'` → vazio). Não há comportamento novo que os testes de integração pudessem cobrir, então o que eles deixaram de exercitar nesta task é exatamente o mesmo que exercitavam antes dela.

**O gate continua registrado como `fail`, não como `ok` nem `na`.** A decisão é de aceitar o risco, não de declarar o problema inexistente — `./mvnw test` sai vermelho nesta máquina, e falsear o frontmatter destruiria o valor do checklist como validação. Pelo mesmo motivo `estado` permanece **`parcial`**: a regra do `PRE-MERGE-CHECKLIST.md` é que `concluido` exige todos os gates `ok` ou `na`.

**O que fica em aberto para a sprint:** o ambiente continua sem Docker acessível ao Testcontainers. A próxima task que **mexer em código** não pode herdar esta decisão — ali os 48 testes voltam a ser cobertura relevante, e o problema de ambiente precisa estar resolvido antes.

<details>
<summary>Registro original da pendência (para rastreabilidade)</summary>

1. **Como tratar o gate `testes` vermelho por Docker indisponível.** 48 testes em 11 classes `*IntegrationTest` falham nesta máquina, e falham **igualmente com as mudanças desta task revertidas** — a causa é `Could not find a valid Docker environment` do Testcontainers (`NpipeSocketClientProviderStrategy`), apesar de `docker info` responder normalmente no shell; sem container, o contexto cai em H2 sem schema e os testes morrem em `Table "AUTH_TOKEN" not found`. **Não é regressão desta task e não é código do backend.** A QA-012 registrou `testes_total: 422` com `testes: ok` em 2026-08-10 — não verifiquei se a diferença veio de mudança no repositório desde então ou do ambiente da máquina, e a evidência do `git stash` só prova que **esta** task não é a causa. Decisão do humano: (a) tratar como problema de ambiente local e mergear com o gate vermelho documentado, ou (b) abrir FIX de ambiente antes do merge. Não toquei em nada disso — está fora do escopo declarado da task, que proíbe alterar classe de produção ou de teste.

</details>

---

## Débitos técnicos encontrados

Nenhum corrigido aqui — o plano é explícito em tratar o legado como baseline.

| # | Débito | Evidência | Por que importa |
|---|---|---|---|
| 1 | **Dois bugs reais de locale** em `LegendaParser:21` e `PaymentProofStrategy:81` | violações `(a)` acima, com o comportamento sob `tr-TR` medido | **Corrupção silenciosa, não exceção** (corrigido após F1 do Reviewer). O primeiro classifica `pix` como `OUTRO` em memória; o segundo **grava** `PİX` em `comprovantes.tipo_pagamento` (`VARCHAR(255)`, sem `CHECK`) — dado errado que fica no banco depois de o locale ser corrigido. Nenhum dos dois lança nada. São os únicos `(a)` do piloto |
| 1b | **`PedidoController:64` — `StatusPedido.valueOf(status.toUpperCase())`** | achado do Reviewer ao derrubar F1 | Este **é** um `Enum.valueOf` alimentado por `toUpperCase()` sem `Locale`. Hoje inócuo apenas porque nenhum valor de `StatusPedido` (`PENDENTE`, `PAGO`, `CANCELADO`) contém a letra `i` — é acidente, não proteção. **Impacto real: mensagem enganosa, não indisponibilidade** — as linhas 65-67 já capturam `IllegalArgumentException` e devolvem 400 |
| 1c | **Duas regras do ruleset apontam para as mesmas 3 linhas** — `UseLocaleWithCaseConversions` em `PedidoController:64` e `AvoidThrowingNewInstanceOfSameException` em `PedidoController:66` | baseline, ambas verificadas | Convergência não planejada, e é o melhor argumento a favor da segunda regra: o `catch (IllegalArgumentException e) { throw new IllegalArgumentException(...) }` da linha 66 é exatamente o que **esconde** a causa do bug de locale da linha 64. Uma regra achou o defeito; a outra achou o que impediria de diagnosticá-lo |
| 2 | **`UseLocaleWithCaseConversions`: mais 3 ocorrências fora do piloto** (5 no total) | baseline: `PedidoController:64`, `PedidoSpecs:42`, `ResumoMesServiceImpl:38` | A de `PedidoController:64` é o item 1b acima, lida ao corrigir F1. As de `PedidoSpecs:42` e `ResumoMesServiceImpl:38` **continuam não lidas uma a uma** — as duas são filtro/agregação, onde o efeito provável é registro sumindo do resultado em silêncio |
| 3 | **`AvoidCatchingGenericException`: 10 em produção + 1 em teste** | baseline | Maior bloco do baseline. É o modo de falha que o experimento quer medir em código gerado por modelo |
| 4 | **`AtualizarFuncionarioServiceImpl.atualizar`: ciclomática 12, NPath 2048** (threshold 200) | baseline | 2048 caminhos combinados — a cobertura de caminhos é inalcançável na prática |
| 5 | **`CadastrarFuncionarioServiceImpl.validarDadosPagamento`: ciclomática 18, cognitiva 17** | baseline | As duas métricas concordam: é complexo de verdade, não é artefato de contagem |
| 6 | **O PMD não enxerga violação de arquitetura hexagonal** | desvio 2 | O débito de `DataIntegrityViolationException` na application layer continua invisível. Cobrir exigiria configurar `LoosePackageCoupling` com a lista de pacotes, ou ArchUnit. **Nenhuma das três ferramentas da sprint mede conformidade arquitetural** |
| 7 | **`MissingSerialVersionUID`: 25 ocorrências deliberadamente fora do ruleset** | rodada 1 | Se algum dia este stack serializar exceção, a decisão precisa ser revisitada |

---

## Próximos passos / observações pro próximo

- **`-Dpmd.rulesets` não existe e `-Dpmd.includeTests` só funciona por causa da property que adicionei.** Quem for medir escopo por diff no item #7 vai bater nisso: trocar de ruleset exige editar o `pom.xml`. Documentado no runbook.
- **O baseline para o Δ do item #7 é `Q7_producao = 22` e `Q7_teste = 1`**, com o ruleset de hash `5100b68f…`. Δ medido contra outro ruleset não é comparável.
- **Ao contrário do PIT, o PMD não precisa de suíte verde** — analisa fonte. Ele roda **agora**, com os 48 testes de integração vermelhos, sem prejuízo nenhum ao resultado. **Mas "não precisa de build" não vale para regra nenhuma:** regras com resolução de tipo consultam o `auxclasspath`, e o Reviewer mediu `LawOfDemeter` dando **2** sem `target/classes` e **26** com. O ruleset congelado é insensível (**22 nos dois casos**, verificado) — quem medir com ruleset diferente no item #7 precisa fixar e declarar se compila antes.
- **Não silenciar violação com `@SuppressWarnings("PMD…")`.** A discussão é no ruleset, com justificativa escrita; supressão espalhada pelo código torna o Q7 incomparável entre runs sem deixar rastro no hash.
- **Item #5 do `backlog-s04.md` sai** (absorvido por esta task) e o **#4** fecha, conforme a seção "Após merge" do plano. Falta ainda registrar baseline e hash no `STATE.md` — é do planner.
- Para o item **#6** (JaCoCo): as 4 classes do piloto do PIT já têm mutation score e violações medidos. Fechar com cobertura completa o trio sobre o mesmo código.

---

## Arquivos criados/modificados

- `financas_bot_telegram/pmd-ruleset.xml` (**novo** — entregável central: 10 regras justificadas + as exclusões e seus motivos)
- `financas_bot_telegram/pom.xml` (modificado: `maven-pmd-plugin` fora do ciclo de vida, versão e `pmd.includeTests` em `<properties>`)
- `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` (modificado: nova seção 2.0 na Camada 2, no formato da Camada 1.5)
- `docs/runbooks/PRE-MERGE-CHECKLIST.md` (modificado: gate `lint` do backend deixa de ser `na`; declarado informativo, não bloqueante)
- `docs/sprints/04-instrumentacao-qualidade/status/QA-013-pmd-ruleset-curado-e-piloto.md` (**novo** — este arquivo)

**Nenhuma classe de produção ou de teste foi alterada.**
