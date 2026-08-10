---
task: QA-012
titulo: "Piloto do PIT — mutation testing em escopo reduzido"
data: 2026-08-10
branch: feature/qa-012-piloto-pit-mutation-testing
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: na
  testes: ok
  testes_total: 422
  testes_novos: 0
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits:
  - PREENCHER
pr: null
desvios: 1
pendencias_humano: 0
---

# QA-012 — Piloto do PIT (mutation testing) em escopo reduzido

---

## O que foi feito

`pitest-maven` 1.25.9 + `pitest-junit5-plugin` 1.2.3 adicionados ao `pom.xml` do backend, com `targetClasses` fixo nas quatro classes do plano, `excludedTestClasses` em `*IntegrationTest` e saída em XML + HTML. O plugin **não está ligado a nenhuma fase do build** — roda só sob demanda, então `mvn test` / `mvn package` / o CI continuam com o mesmo custo de antes.

Comando documentado na nova **Camada 1.5** do `ROTEIRO-TESTES-BACKEND.md`, junto da tabela de estados do relatório e do porquê de a exclusão dos testes de integração ser requisito de viabilidade, não otimização.

Nenhum teste e nenhuma classe de produção foram tocados — corrigir teste fraco é a task seguinte (item #2 do backlog), e misturar as duas coisas destruiria a leitura limpa do baseline.

### Evidência de execução

| Comando | Resultado |
|---|---|
| `./mvnw test` (antes da mudança) | `Tests run: 422, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS` |
| `./mvnw org.pitest:pitest-maven:mutationCoverage` | `BUILD SUCCESS`, `Completed in 48 seconds`, relatório em `target/pit-reports/` (`index.html` + `mutations.xml`) |
| `./mvnw test` (depois da mudança) | `Tests run: 422, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS` |
| `./mvnw -q -DskipTests package` | exit 0 |

Ambiente da coleta: Maven 3.9.6, **JVM 23-ea** (única instalada na máquina; o `pom.xml` compila com `--release 21` e o CI usa Temurin 21 — ver *Próximos passos*).

### Critério 4 — nenhum teste de integração rodou sob o PIT

Verificado com um segundo run em `-Dverbose=true` (2914 linhas de log):

- O log enumera **70 classes de teste distintas** do pacote do projeto. `target/test-classes` tem **82** classes compiladas, das quais **12** terminam em `IntegrationTest`. 82 − 12 = **70**. Bate exatamente.
- A string `IntegrationTest` aparece **uma única vez** em todo o log, e é o eco da própria configuração: `excludedTestClasses=[^.*IntegrationTest$]`.
- **Zero** ocorrências de `testcontainers`, `mysql` ou `docker` (busca case-insensitive) no log.
- Fase de cobertura: 22 segundos; run inteiro: 46–48 segundos. Só o boot de um MySQL 8 via Testcontainers custa mais que isso.

> Ressalva honesta: o PIT loga `Sending 245 test classes to minion` e `245 tests examined`. Esse contador é maior que as 70 classes que ele de fato enumera, e **não confirmei o que ele conta** (candidatos varridos no classpath de teste, provavelmente incluindo classes de produção dos mesmos pacotes). Não uso esse número como evidência; a evidência é a enumeração das 70 + a ausência total das 12 classes de integração.

---

## Baseline por classe

Run de 2026-08-10, mutators `DEFAULTS`, escopo das quatro classes do plano.

| Classe | Gerados | KILLED | SURVIVED | NO_COVERAGE | Mutation score | Test strength | Line coverage |
|---|---:|---:|---:|---:|---:|---:|---:|
| `LegendaParser` | 8 | 6 | 2 | 0 | **75%** (6/8) | **75%** (6/8) | 94% (17/18) |
| `PaymentRequestStrategy` | 11 | 11 | 0 | 0 | **100%** (11/11) | **100%** (11/11) | 96% (47/49) |
| `PaymentProofStrategy` | 9 | 9 | 0 | 0 | **100%** (9/9) | **100%** (9/9) | 100% (33/33) |
| `MetaSignatureValidator` | 14 | 11 | 2 | 1 | **79%** (11/14) | **85%** (11/13) | 90% (26/29) |
| **Total** | **42** | **37** | **4** | **1** | **88%** (37/42) | **90%** (37/41) | 95% (123/129) |

`Mutation score` = mortos ÷ gerados. `Test strength` = mortos ÷ **cobertos** (exclui os `NO_COVERAGE`). `Line coverage` aqui é a passada de cobertura do próprio PIT, restrita às classes mutadas — **não** é JaCoCo, que continua ausente do projeto (por isso `cobertura_pct: na` no frontmatter).

---

## Leitura interpretada (critério 6)

Quatro sobreviventes e um `NO_COVERAGE`. Um por um.

### 1. `LegendaParser:28` — `pos >= 0` → `pos > 0` · **(a) lacuna real de asserção**

```java
if (pos >= 0 && pos < posicaoMaisCedo) {
```

`ConditionalsBoundaryMutator` troca `>=` por `>`. O mutante só muda comportamento quando a palavra-chave está **no índice 0** da legenda: aí `pos == 0`, a guarda mutada rejeita, e `parseTipo("pix 200")` devolveria `OUTRO` em vez de `PIX`.

Nenhuma das nove legendas de `LegendaParserTest` tem palavra-chave no índice 0 — todas começam pelo valor (`"200 pix maria"`, `"150.00 Almoço boleto"`, `"1500 TED construtora silva"`, ...). Ninguém percebe.

É lacuna real, não equivalente: existe entrada que distingue original de mutante, e é uma entrada legítima do contrato público de `parseTipo(String)`, que é um utilitário estático e não depende do formato de caption do Telegram. **Achado acionável para o item #2 do backlog.**

### 2. `LegendaParser:28` — `pos < posicaoMaisCedo` → `pos <= posicaoMaisCedo` · **(b) mutante equivalente**

Mesmo `if`, outra comparação. Original e mutante só divergem quando `pos == posicaoMaisCedo`. Dois casos:

- **Primeira iteração:** `posicaoMaisCedo` vale `Integer.MAX_VALUE`. `indexOf` devolve no máximo `length - 1`, que nunca é `MAX_VALUE`. Não diverge.
- **Iterações seguintes:** exigiria duas palavras-chave **distintas** começando no **mesmo índice** da legenda. As quatro chaves são `boleto`, `pix`, `ted`, `agendamento` — primeiras letras `b`, `p`, `t`, `a`, todas diferentes. Duas delas não podem começar na mesma posição. Não diverge.

Logo não existe entrada que mate esse mutante: é equivalente por construção. Estou classificando como equivalente **com a demonstração acima**, não como saída fácil — o sobrevivente nº 1 é da mesma linha e está classificado como lacuna real.

### 3. `MetaSignatureValidator:28` (construtor) — condicional negada · **(a) lacuna real, de baixo valor**

```java
if (!secretConfigurado) {
    logger.warn("whatsapp.app-secret nao configurado — canal WhatsApp inerte: ...");
}
```

Negar a condicional inverte **quando o warn sai**: passa a avisar quando o secret está configurado e a ficar calado quando não está.

Não é equivalente — a saída observável muda, e esse warn é sinal operacional deliberado (o comentário do código explica que sem secret o canal fica inerte). O que falta é asserção: o repo não tem captura de appender de log em teste nenhum, então nada verifica logging.

Classifico como **lacuna real de valor baixo**: matar esse mutante exige montar infraestrutura de captura de log para uma linha de aviso. Fica **registrado como conhecido e deliberado**, não silenciosamente ignorado.

### 4. `MetaSignatureValidator:64` (`bytesToHex`) — `bytes.length * 2` → `bytes.length / 2` · **(b) mutante equivalente**

```java
StringBuilder sb = new StringBuilder(bytes.length * 2);
```

É a **capacidade inicial** do `StringBuilder`. Capacidade é dica de alocação: com capacidade menor o `StringBuilder` realoca internamente e produz **exatamente a mesma String**. Nenhum teste possível sobre o contrato público (`isValid` devolve boolean; `bytesToHex` é privado) consegue distinguir original de mutante — só um benchmark de alocação, que não é teste de comportamento.

Equivalente de manual. É, aliás, o exemplo mais didático do run: mostra por que **100% de mutation score não é meta alcançável**.

### 5. `MetaSignatureValidator:59` — `NO_COVERAGE`, não sobrevivente

```java
} catch (NoSuchAlgorithmException | InvalidKeyException e) {
    logger.error("Falha ao calcular HMAC-SHA256: {}", e.getMessage());
    return false;   // ← linha 59, mutante NO_COVERAGE
}
```

O bloco `catch` nunca é executado por teste nenhum: `HmacSHA256` existe em qualquer JDK (`NoSuchAlgorithmException` não ocorre) e `mac.init` não lança `InvalidKeyException` para uma chave HMAC de bytes arbitrários. É defesa contra exceções checadas que, na prática, não acontecem.

**`NO_COVERAGE` não é o mesmo achado que `SURVIVED`:** aqui o problema seria de cobertura, não de asserção — e mesmo assim não é um problema acionável, porque forçar essa linha exigiria injetar um provider JCE falso. Fica registrado como conhecido e deliberado. É exatamente por isso que `test strength` (85%) vale mais que `mutation score` (79%) para essa classe: ela isola a força da asserção do que simplesmente não é alcançável.

### Onde não houve o que interpretar

`PaymentRequestStrategy` (11/11) e `PaymentProofStrategy` (9/9) **não produziram um único sobrevivente**. Isso é achado, não vazio: os testes dessas duas classes usam `ArgumentCaptor` e verificam o conteúdo do objeto construído (valor, descrição, status, `requisitanteId`, `dataPedido`, URL do S3), em vez de só verificar que o mock foi chamado. Toda decisão mutável — os `if` de `supports()`, os early-return, os `TipoUploadS3` passados ao S3, o texto condicional da dica de tipo — tem asserção que a distingue. É o padrão de teste que o item #2 do backlog deveria replicar nas outras classes.

Conforme o plano, **não ampliei o escopo por conta própria**: as quatro classes renderam material suficiente (4 sobreviventes + 1 `NO_COVERAGE`), então não há motivo para escolher classes novas nesta rodada.

---

## Cobertura × mutation score — por que divergem (critério 7)

O run deu três casos diferentes, todos com exemplo concreto destas quatro classes.

### Caso 1 — cobertura alta escondendo asserção fraca (`LegendaParser`)

Line coverage **94%** (17/18). A **única** linha não coberta é `private LegendaParser() {}` — o construtor privado de classe utilitária, inalcançável por design. Lido só pela cobertura, o veredito seria "praticamente perfeito, 100% do que importa".

Mutation score: **75%**. Os dois sobreviventes estão na **linha 28, que é executada por todos os nove testes**. Cobertura respondeu "executou?" — sim. Mutation respondeu "teria percebido se quebrasse?" — em dois casos, não.

**É a divergência que motiva a ferramenta**, e apareceu no primeiro run, na classe mais simples do escopo.

### Caso 2 — cobertura baixa sem lacuna de asserção (`PaymentRequestStrategy`)

O inverso. Line coverage **96%** (47/49): as linhas 88 e 97, o `throw new InvalidMessageFormatException(...)` dentro de `parsePedido`, nunca executam — `process()` só é chamado com caption que `supports()` aceitaria, e `parsePedido` refaz o match por segurança. Cobertura aponta um "buraco".

Mutation score: **100%** (11/11). Os mutators default não geram mutante viável em cima de um `throw` puro (não há retorno, condicional, aritmética ou chamada `void` a mutar), então não há `NO_COVERAGE` correspondente. A lógica que **decide** algo está toda verificada.

Aqui cobertura abaixo de 100% **não** significava teste fraco. Perseguir esses 4% seria trabalho sem retorno.

### Caso 3 — os dois concordando, e o `test strength` desempatando (`MetaSignatureValidator`)

Line coverage **90%** (26/29): as três linhas não cobertas são exatamente o bloco `catch`, e é de lá que sai o único `NO_COVERAGE` do run. As duas métricas apontam para o mesmo lugar.

Mas mutation score (**79%**, 11/14) e test strength (**85%**, 11/13) divergem entre si — e é isso que separa os dois problemas: excluído o mutante inalcançável, ainda sobram **2 sobreviventes em 13 cobertos**. O score cru mistura "não testado" com "testado sem verificar"; o test strength isola o segundo. **Coletar os dois é o que dá a leitura certa.**

### Régua que fica

| Sintoma | Diagnóstico | Ação |
|---|---|---|
| Cobertura alta + mutation score baixo | teste executa mas não verifica | reforçar asserção — é o achado que interessa |
| Cobertura baixa + mutation score alto | código sem decisão (throw, DTO, construtor privado) | normalmente ignorar |
| Cobertura baixa + `NO_COVERAGE` alto | código sem teste algum | escrever teste, se for alcançável |
| `mutation score` << `test strength` | muito código inalcançável no escopo | rever o escopo, não a suíte |

---

## Desvios do plano

1. **`<timestampedReports>false</timestampedReports>` acrescentado à configuração.** O plano enumerou `targetClasses`, `excludedTestClasses` e `outputFormats`; incluí um quarto item. Sem ele o PIT cria uma subpasta com timestamp a cada execução, e o caminho do relatório documentado no runbook mudaria a cada run. Não altera o que é medido nem o resultado — só fixa a saída em `target/pit-reports/`.

Nenhum outro. As quatro classes previstas se sustentaram (nenhuma puxou contexto Spring), então não houve troca de classe; nenhum teste e nenhuma classe de produção foram tocados; JaCoCo e PMD não foram instalados.

---

## Decisões tomadas durante a execução

- **Versões: PIT 1.25.9 (a mais recente no Central) + `pitest-junit5-plugin` 1.2.3 (a mais recente).** O `pitest-junit5-plugin` 1.2.3 é compilado contra o `pitest` 1.15.2 em escopo `provided`, então a combinação com 1.25.9 não é a testada pelo autor — validei **empiricamente**: o run completa, o plugin JUnit 5 é carregado (`Adding org.pitest:pitest-junit5-plugin to SUT classpath`, `Found shared classpath plugin : JUnit 5 test framework support`) e os 42 mutantes rodam contra testes Jupiter reais. Versões em properties (`pitest.version`, `pitest-junit5-plugin.version`) para o bump ser um lugar só.
- **Plugin não amarrado a nenhuma `<execution>`.** Roda por invocação direta do goal. Amarrar a `verify` ou `test` encareceria todo build e o CI sem ninguém ter pedido — e o plano põe "integrar ao CI" explicitamente fora de escopo.
- **Conjunto de mutators mantido no default.** O plano só autorizava mexer se o tempo inviabilizasse; 48 segundos não inviabiliza.
- **Relatórios não commitados.** Ficam em `target/`, já coberto pelo `.gitignore`. Os números e a leitura estão aqui, que é o artefato durável.
- **Comentário no `pom.xml` explicando o porquê do `excludedTestClasses`.** É a linha mais fácil de alguém "limpar" no futuro sem saber que ela é o que impede o run de nunca terminar.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Débitos técnicos encontrados

Para o planner consolidar em `docs/PENDENCIAS-TECNICAS.md` / backlog da sprint:

1. **`LegendaParser` não tem caso com palavra-chave no índice 0.** Sobrevivente nº 1. Insumo direto para o item #2 do backlog.
2. **O warn de `whatsapp.app-secret` não configurado não é verificado por teste algum.** Sobrevivente nº 3. Depende de infraestrutura de captura de log, que o repo não tem.
3. **A exclusão dos testes de integração depende inteiramente da convenção de nome `*IntegrationTest`.** Um teste de integração novo com outro sufixo escapa do filtro e trava o run do PIT (Testcontainers × 1 por mutante). Hoje não há nada que force a convenção — nem lint, nem gate de CI.
4. **PIT roda sem histórico incremental.** `historyInputLocation`/`historyOutputLocation` não configurados: todo run é do zero. Irrelevante em 4 classes, vira problema quando o escopo crescer (item #7 do backlog, "classes tocadas").
5. **Os números foram colhidos em JVM 23-ea, enquanto CI e produção usam Temurin 21.** O `pom.xml` compila com `--release 21`, mas o PIT executou sobre uma JVM diferente da do CI. Não invalida o baseline (é bytecode 21 em ambos os casos), mas se o PIT for para o CI, vale reconferir os números lá antes de tratá-los como comparáveis.
6. **`cobertura_pct` continua `na`.** O `Line Coverage` desta task vem da passada do PIT e cobre só as 4 classes mutadas. Enquanto JaCoCo não entrar (item #6 do backlog), o campo do frontmatter não tem fonte legítima.

---

## Próximos passos / observações pro próximo

- **Item #2 do backlog (corrigir testes fracos)** tem alvo concreto agora: o caso de palavra-chave no índice 0 em `LegendaParser`. Depois de escrever o teste, rodar o PIT de novo e conferir que `LegendaParser` sai de 6/8 para 7/8 — os outros dois sobreviventes (equivalentes) **não devem** ser perseguidos.
- **Não tratar 100% como meta.** Dos 5 achados deste run, 2 são equivalentes demonstrados e 1 é inalcançável na prática. O teto realista destas quatro classes é 39/42 ≈ 93%.
- **Ao ampliar `targetClasses`**, conferir antes se o teste da classe candidata sobe contexto Spring ou Testcontainers — é o que inviabiliza o run.
- **Se o PIT for para o CI**, o runner usa Java 21 e não tem cache do `.m2`: o primeiro run baixa o plugin. E o `excludedTestClasses` continua sendo obrigatório, não opcional.
- **Padrão a replicar:** os testes de `PaymentRequestStrategy`/`PaymentProofStrategy` mataram 20/20 porque capturam o objeto construído e verificam campo a campo, em vez de só verificar que o mock foi chamado. É o contraste com `LegendaParserTest`, que compara só o retorno final.

---

## Padrões técnicos

Não se aplica — a task não adiciona lógica de produção nem altera arquitetura. A mudança é ferramental de build (`pom.xml`) mais documentação.

A única decisão com sabor de design é o plugin ficar **fora do ciclo de vida padrão do Maven**: mutation testing é caro por natureza (reexecuta a suíte relevante uma vez por mutante) e o valor dele é diagnóstico, não regressivo. Ferramenta de diagnóstico que roda em todo build vira imposto; rodada sob demanda, com escopo explícito, ela responde uma pergunta específica quando alguém tem a pergunta.

---

## Arquivos criados/modificados

- `financas_bot_telegram/pom.xml` (modificado: `pitest-maven` + `pitest-junit5-plugin`, versões em properties, `targetClasses` nas 4 classes, `excludedTestClasses=*IntegrationTest`, saída XML+HTML, `timestampedReports=false`)
- `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` (modificado: nova Camada 1.5 — comando, pré-requisito de suíte verde, caminho do relatório, tabela de estados, quando rodar e quando não)
- `docs/sprints/04-instrumentacao-qualidade/status/QA-012-piloto-pit-mutation-testing.md` (novo: este relatório)
