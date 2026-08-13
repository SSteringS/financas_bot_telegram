---
task: QA-014
titulo: "Piloto do JaCoCo — cobertura medida e comparada com PIT e PMD"
data: 2026-08-13
branch: feature/qa-014-piloto-jacoco-cobertura
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: ok
  testes: ok
  testes_total: 422
  testes_novos: 0
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits:
  - ff78be3   # feat: jacoco + lombok.config + runbooks/template/checklist
  - 1c1f216   # docs: status report
  - 6dff57f   # fix: 6 achados da revisao (A1 retratacao, M1 clean/append, B1-B4)
  - e1742bf   # docs: hash do commit de correcao no frontmatter
  - 606d250   # docs: coerencia interna do status
  - 33ca245   # fix: C1-C5 do delta-review (clean no comentario do pom, fonte do append)
pr: null
desvios: 0
pendencias_humano: 0
---

# QA-014 — Piloto do JaCoCo: cobertura medida e comparada com PIT e PMD

> **Sobre `testes_total: 422` e o run desta task.** A medição de cobertura é **unit-only** por decisão do plano, então o run que gerou os números executou **374 testes** — os 48 dos 11 `*IntegrationTest` ficaram de fora do recorte. Isso **não é regressão**: os 422 continuam válidos na suíte completa, verde no CI. Verificado nesta sessão (não copiado do plano): `gh run view 31727999562` → `Tests run: 422, Failures: 0, Errors: 0, Skipped: 0`, `conclusion: success`, commit `15ae35e` — e `git merge-base --is-ancestor 15ae35e HEAD` responde **sim**, ou seja, é a mesma base de código desta branch.
>
> **Sobre `cobertura_pct: na`.** A regra provisória que esta própria task escreveu diz: cobertura de linha das **classes de produção que a task tocou**, `na` quando não toca nenhuma. Esta task não tocou uma linha de Java. `na` é a aplicação correta da regra nova, não omissão.

---

## O que foi feito

`jacoco-maven-plugin` 0.8.12 no `pom.xml` do backend, versão em `<properties>`, **fora do ciclo de vida** e sem `jacoco:check` — mesmo padrão de PIT (QA-012) e PMD (QA-013). `mvn test`, `mvn package` e o CI continuam com o mesmo custo e não quebram por cobertura.

`financas_bot_telegram/lombok.config` criado com `lombok.addLombokGeneratedAnnotation = true`. Não é detalhe de build: sem ele o relatório mede **quanto Lombok o projeto usa**, não quanto os testes cobrem. Os dois números foram medidos, não estimados (§Distorção do Lombok).

Nenhuma classe de produção ou de teste foi tocada. O diff é: `pom.xml`, `lombok.config` (novo), `ROTEIRO-TESTES-BACKEND.md`, `_TEMPLATE-status.md`, `PRE-MERGE-CHECKLIST.md`, este status e o relatório da revisão em `avaliacoes/`.

### Evidência de execução

| Comando | Resultado |
|---|---|
| `./financas_bot_telegram/mvnw clean jacoco:prepare-agent test jacoco:report -f financas_bot_telegram/pom.xml -Dtest='!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false` (**sem** `lombok.config`) | `BUILD SUCCESS` · `Tests run: 374, Failures: 0, Errors: 0` · `Analyzed bundle with 174 classes` |
| idem, **com** `lombok.config` | `BUILD SUCCESS` · `Tests run: 374, Failures: 0, Errors: 0` · `Analyzed bundle with 148 classes` |
| `./financas_bot_telegram/mvnw org.pitest:pitest-maven:mutationCoverage -f financas_bot_telegram/pom.xml` | `BUILD SUCCESS` · `Line Coverage (for mutated classes only): 123/129 (95%)` · `Generated 42 mutations Killed 37 (88%)` — **reprodução exata do baseline da QA-012** |
| `./financas_bot_telegram/mvnw pmd:pmd -f financas_bot_telegram/pom.xml` | `BUILD SUCCESS` · **22 violações** — idêntico ao baseline congelado da QA-013 |
| `./financas_bot_telegram/mvnw -q -DskipTests package -f financas_bot_telegram/pom.xml` | exit **0** |
| `gh run view 31727999562` (CI, commit `15ae35e`, ancestral desta branch) | `Tests run: 422, Failures: 0, Errors: 0, Skipped: 0` · `success` |

Saída do JaCoCo: `financas_bot_telegram/target/site/jacoco/index.html` (34.160 bytes) e `jacoco.xml` (368.792 bytes). Sob `target/`, **não commitado** — confirmado por `git check-ignore`.

Ambiente: Maven 3.9.9, **JVM 23-ea** (única instalada; o `pom.xml` compila com `--release 21` e o CI usa Temurin 21 — mesma ressalva registrada na QA-012 e QA-013), locale `pt_BR`.

### Por que os goals vão todos na mesma linha de comando

`jacoco:prepare-agent` não precisa de `<executions>`: ele define a property `argLine`, o `test` seguinte **na mesma sessão do Maven** a consome, e o `jacoco:report` lê o `jacoco.exec` resultante. É isso que permite manter o plugin fora do ciclo de vida sem perder função — a mesma propriedade que a sprint vem preservando em PIT e PMD.

**O modo de falha silencioso foi verificado, não assumido** — e a evidência foi **trocada por uma mais forte após o achado B4 da revisão**. A original olhava só o `pom.xml` do módulo e o POM do `spring-boot-starter-parent:3.4.5`, o que é frágil: o surefire **é** gerenciado pela cadeia de parents (versão 3.5.3), então "o parent não menciona surefire" era falso como generalização. A verificação correta é o POM efetivo, que resolve a cadeia inteira: `./mvnw help:effective-pom -Doutput=...` seguido de `grep -c argLine` devolve **0**. Não existe `<argLine>` em lugar nenhum da configuração resolvida, logo não há nada para sobrescrever o agente hoje.

A cobertura **não veio zero** — veio 89,5% de instrução, com números por linha coerentes com a leitura manual do fonte, o que é a evidência positiva de que a instrumentação funcionou.

⚠️ **`append=true` é o segundo modo de falha silencioso, e foi encontrado pela revisão (M1), não por mim.** O `prepare-agent` soma ao `target/jacoco.exec` existente em vez de substituí-lo. Sem `clean`, um run anterior que tenha incluído os `*IntegrationTest` continua contando e o resultado **deixa de ser unit-only sem nada no log avisando**. Os dois runs desta medição usaram `clean` — por causa da recompilação exigida pelo `lombok.config`, e não porque eu tivesse identificado o risco —, então **os números publicados aqui não estão contaminados**. O comando publicado não trazia o `clean` em nenhum dos **quatro** pontos onde aparece (runbook, checklist, template e o comentário do `pom.xml`); passou a trazer nos quatro — o quarto só foi localizado no delta-review (achado C1).

---

## Distorção do Lombok — medida, não estimada

Dois runs limpos (`clean` nos dois), idênticos exceto pela presença do `lombok.config`. Mesmos 374 testes, mesmo código.

| Métrica | Sem `lombok.config` | Com `lombok.config` | Δ |
|---|---:|---:|---:|
| **Linha** | 1897/2125 = **89,3%** | 1681/1864 = **90,2%** | **+0,9 pp** |
| **Branch** | 328/558 = **58,8%** | 326/430 = **75,8%** | **+17,0 pp** |
| Instrução | 9095/11509 = 79,0% | 6880/7685 = 89,5% | +10,5 pp |
| Método | 811/1035 = 78,4% | 377/431 = 87,5% | +9,1 pp |
| Classe | 162/174 = 93,1% | 140/148 = 94,6% | +1,5 pp |
| Complexidade | 945/1318 = 71,7% | 510/650 = 78,5% | +6,8 pp |

**A frase:** o Lombok inflava o denominador em **604 métodos (58% do total), 3.824 instruções (33%) e 128 desvios (23%)**, e **126 desses 128 desvios estavam descobertos** — isto é, **55% de toda a "cobertura de branch faltante" do projeto (126 de 230)** era `equals`/`hashCode` gerado que ninguém escreveu e ninguém deveria testar. Os outros 104 desvios descobertos são código escrito à mão e continuam valendo como buraco real.

**O detalhe que quase engana:** a métrica de **linha mal se move** (+0,9 pp). Quem tivesse medido só linha concluiria que o Lombok não distorce nada. Ele distorce — o código gerado é atribuído às **linhas da anotação e dos campos**, que já estavam cobertas por outra coisa, então some da contagem de linha e aparece inteiro na de branch e de método. É o primeiro argumento concreto, com dado deste projeto, para **nunca reportar cobertura de linha sozinha**.

Todos os números abaixo usam o run **com** `lombok.config`.

---

## Baseline do projeto (unit-only, com `lombok.config`)

| Métrica | Coberto/Total | % |
|---|---:|---:|
| **Linha** | 1681/1864 | **90,2%** |
| **Branch** | 326/430 | **75,8%** |
| Instrução | 6880/7685 | 89,5% |
| Método | 377/431 | 87,5% |
| Classe | 140/148 | 94,6% |
| Complexidade | 510/650 | 78,5% |

Testes executados no recorte: **374** (dos 422 da suíte completa). Classes analisadas: **148**. Só `src/main` — o JaCoCo instrumenta produção; teste não entra na conta.

⚠️ **Este número SUBESTIMA a cobertura real do projeto** e a frase é parte da definição da métrica, não ressalva de rodapé. A suíte de integração (48 testes, 11 classes) está fora do recorte por decisão do plano — comparabilidade com o PIT e independência do Docker. Os pacotes mais penalizados são justamente os que existem para conversar com infraestrutura:

| Pacote | Linha | Branch |
|---|---:|---:|
| `adapters/in/whatsapp/exceptionhandler` | 4/53 = 8% | 0/18 = 0% |
| `adapters/in/rest` (`RestExceptionHandler`) | 8/28 = 29% | — |
| `adapters/out/persistence/idempotencia` | 4/10 = 40% | — |
| `adapters/out/persistence` | 74/97 = 76% | 0/18 = 0% |
| `adapters/out/s3/service` | 35/48 = 73% | 2/2 = 100% |

**Não afirmo que a suíte de integração cobre esses pacotes** — isso não foi medido e não pode ser medido nesta máquina hoje (Docker). O que está medido é que **no recorte unitário** eles estão baixos. Cobertura da suíte completa fica **declarada como não medida** até o FIX de ambiente.

Oito classes ficaram em **0% de linha** no recorte: `ApiTelegramClientException`, `WhatsAppContact`, `WhatsAppMetadata`, `WhatsAppProfile`, `InvalidWhatsAppPayloadException`, `WhatsAppMediaDownloadException`, `MensagemProcessadaEntity`, `TelegramFileDownloadException` — todas de 1 a 6 linhas, majoritariamente exceções e DTOs.

---

## Tabela por classe — as 5 do piloto

| Classe | Linha | Branch | Instrução |
|---|---:|---:|---:|
| `LegendaParser` | 17/17 = **100%** | 10/10 = **100%** | 70/70 = 100% |
| `PaymentRequestStrategy` | 47/49 = **96%** | 7/8 = **88%** | 178/185 = 96% |
| `PaymentProofStrategy` | 33/33 = **100%** | 9/10 = **90%** | 139/139 = 100% |
| `MetaSignatureValidator` | 26/29 = **90%** | 14/14 = **100%** | 121/129 = 94% |
| `FecharMesServiceImpl` | 75/75 = **100%** | 19/24 = **79%** | 274/276 = 99% |

**As 5 classes têm zero Lombok — e isso foi verificado, não suposto:** os números dessas classes são **byte a byte idênticos** nos dois runs (antes e depois do `lombok.config`). A distorção do baseline não contamina nenhuma leitura interpretada abaixo.

---

## Comparação de três números — JaCoCo × PIT × mutação

| Classe | Linha (JaCoCo) | Linha (PIT) | Mutation score | Test strength | Divergência JaCoCo×PIT |
|---|---:|---:|---:|---:|---|
| `LegendaParser` | **100%** (17/17) | 94% (17/18) | **75%** | 75% | **sim, +1 linha no PIT** |
| `PaymentRequestStrategy` | 96% (47/49) | 96% (47/49) | 100% | 100% | não |
| `PaymentProofStrategy` | 100% (33/33) | 100% (33/33) | 100% | 100% | não |
| `MetaSignatureValidator` | 90% (26/29) | 90% (26/29) | 79% | 85% | não |

Números do PIT reproduzidos nesta sessão (`123/129 (95%)`, `42 mutações, 37 mortos (88%)` — idêntico à QA-012), não copiados do status anterior.

### A única divergência, explicada até a linha

`LegendaParser`: o PIT conta **18 linhas** e o JaCoCo conta **17**. A linha extra é a **17** — `private LegendaParser() {}`, o construtor privado da classe utilitária.

Evidência dos dois lados, não inferência:

- **PIT** (`target/pit-reports/.../LegendaParser.java.html`, parseado): a única linha marcada `uncovered` é a **17**. As outras 17 estão `covered`.
- **JaCoCo** (`jacoco.xml`): a linha 17 **não aparece** na lista de `<line>` da classe — não é "descoberta", é **ausente**. E a lista de `<method>` traz apenas `parseTipo` e `<clinit>`; **não há entrada `<init>`**.

A causa é um filtro nativo do JaCoCo (desde a 0.8.0) para **construtor privado vazio e sem argumentos de classe que só tem membros estáticos** — o idioma padrão de classe utilitária, que existe para *impedir* instanciação e portanto não deve ser exigido em teste. O PIT não tem esse filtro e conta a linha como código não executado.

**Não é bug de nenhum dos dois, e o "melhor" número é o do JaCoCo.** 94% no PIT sugere um buraco de teste que não existe: escrever um teste que instancia por reflexão o construtor privado subiria a métrica sem acrescentar uma asserção sobre comportamento algum. É exatamente o tipo de teste que gate de cobertura produz — e o argumento de por que `jacoco:check` não está configurado.

### As não-divergências valem tanto quanto

Nas outras três classes as ferramentas concordam **linha a linha**, não só no total — o que é uma checagem bem mais forte que dois percentuais coincidirem:

| Classe | Linhas não cobertas (PIT) | Linhas não cobertas (JaCoCo) |
|---|---|---|
| `PaymentRequestStrategy` | 88, 97 | 88, 97 |
| `PaymentProofStrategy` | — | — |
| `MetaSignatureValidator` | 57, 58, 59 | 57, 58, 59 |

---

## Por que cobertura alta não implica teste bom — com dado deste projeto

O caso é `LegendaParser`, e ele ficou **mais forte** com o JaCoCo do que era com o PIT sozinho:

| Ferramenta | O que ela responde | `LegendaParser` |
|---|---|---|
| JaCoCo (linha) | as linhas foram executadas? | **100%** — todas |
| JaCoCo (branch) | os dois lados de cada desvio foram executados? | **100%** — todos |
| PIT | se eu quebrar o código, algum teste reclama? | **75%** — 2 de 8 mutantes sobreviveram |

**Cobertura perfeita pelas duas métricas do JaCoCo, e um quarto das mutações passa despercebido.** Não é margem de erro: os testes executam cada linha e cada desvio de `parseTipo`, e ainda assim existe alteração de comportamento que nenhum deles detecta. A QA-012 já havia identificado o sobrevivente: `pos >= 0` → `pos > 0` na linha 28 só muda o resultado quando a palavra-chave está **no índice 0** da legenda, e nenhuma das nove legendas de teste começa pela palavra-chave — todas começam pelo valor (`"200 pix maria"`).

O ponto pedagógico é a **ordem dos números**: 100% de linha, 100% de branch, 75% de mutação. Cobertura mede o que o teste **percorre**; mutação mede o que o teste **verifica**. Um teste que chama `parseTipo("200 pix maria")` e não olha o retorno dá 100% nas duas primeiras e 0% na terceira. **A cobertura é condição necessária e não suficiente**, e este projeto agora tem o par de números que prova isso no próprio código, sem apelo a exemplo de livro.

Corolário direto para o experimento: **`cobertura_pct` sozinho não é medida de qualidade de teste.** Ele responde "o que nunca foi executado" — uma pergunta útil e mais barata que a do PIT, mas outra pergunta.

---

## Branch × linha — o que a de branch enxerga

`FecharMesServiceImpl`, como o plano previu: **100% de linha (75/75) e 79% de branch (19/24)**. Cinco desvios nunca exercitados, e a métrica de linha não pisca. Os cinco, do `jacoco.xml`:

| Linha | Código | Desvio não exercitado |
|---:|---|---|
| 59 | `BigDecimal ajusteEfetivo = ajuste != null ? ajuste : BigDecimal.ZERO;` | o lado `null`. Verificado: as **12 chamadas** a `service.fechar(...)` em `FecharMesServiceImplTest` passam `BigDecimal.ZERO`, `new BigDecimal("200.00")` ou `new BigDecimal("-150.00")`; a string `null` **não aparece no arquivo inteiro** |
| 83 | `.filter(a -> a.getDataInicio() != null` | o lado `null` |
| 85 | `.filter(a -> a.getParcelasPagas() != null` | o lado `null` |
| 86 | `&& a.getNumParcelas() != null` | o lado `null` |
| 87 | `&& a.getParcelasPagas() < a.getNumParcelas())` | um dos lados da comparação |

O padrão é único e nítido: **todo guard de `null` do método está escrito e nenhum foi testado com `null`**. Como cada guard divide a linha em dois caminhos e o caminho feliz executa a linha inteira, a cobertura de linha marca 100% em todos os cinco. É a definição operacional da diferença entre as duas métricas — e o motivo de o runbook e o `PRE-MERGE-CHECKLIST` passarem a pedir as duas.

Vale notar o inverso na mesma tabela do piloto: `MetaSignatureValidator` tem **90% de linha e 100% de branch**. Lá o buraco é bloco inteiro não executado (o `catch` de `NoSuchAlgorithmException | InvalidKeyException`, linhas 57-59), que não contém desvio nenhum. As duas métricas **não são ordenáveis** — cada uma vê um tipo de buraco.

---

## Retratação — o item #6 do backlog estava certo

**Esta seção começou afirmando o contrário e foi corrigida após o achado A1 da revisão.** O que foi publicado antes: que o item #6 do backlog errava ao dizer que *"o comando `jacoco:report` que o agente QA é instruído a rodar falha hoje"*, porque a única ocorrência de `jacoco` no repositório seria um comentário em `_TEMPLATE-status.md:16`.

**Está errado, e o erro é meu:** repeti a verificação do plano em vez de refazê-la. `git grep -in "jacoco" origin/integration/04-instrumentacao-qualidade` devolve **77 linhas**. O grep original (do plano) varreu `.claude/`, `docs/templates/` e `docs/runbooks/` — e o projeto tem **duas** definições do agente de QA, uma por harness:

```
.codex/agents/qa-test-specialist.toml:37:  - Cobertura: JaCoCo — `./mvnw jacoco:report`
.codex/agents/qa-test-specialist.toml:186: | Cobertura da classe principal | `./mvnw jacoco:report` → campo `cobertura_pct` |
```

Ou seja: **existia sim um agente instruído a rodar um comando de JaCoCo em um projeto sem JaCoCo.** O item #6 descreveu a causa corretamente; quem errou o diagnóstico foi o plano, e eu o repeti. O planner **não** deve fechar o item #6 com a "correção" que estava aqui.

**Débito aberto por esta constatação (M2 da revisão):** com o plugin instalado mas **sem `<executions>`**, `./mvnw jacoco:report` puro deixou de falhar e passou a **desistir em silêncio** — verificado nesta sessão, com `target/` limpo: `Skipping JaCoCo execution due to missing execution data file` seguido de `BUILD SUCCESS`. A instrução do `.codex/agents/qa-test-specialist.toml` antes falhava alto; agora passa verde sem produzir relatório, o que é pior. **Não corrigi o arquivo**: `.claude/` e `.codex/` são território do humano e do `ai-engineer`, e alterá-los exige autorização explícita, que não foi pedida nem dada. Está na tabela de débitos como item 7.

---

## Revisão independente (ADR 0005)

Primeira rodada: **`rejected`**, com 6 correções pontuais e **nenhuma remediação de código** — o Reviewer reproduziu os 12 valores da tabela do Lombok, as 5 linhas da tabela por classe, os baselines de PIT e PMD, a tabela por pacote e a igualdade byte a byte das 5 classes do piloto, e **nenhum número divergiu**. O relatório está em `docs/sprints/04-instrumentacao-qualidade/avaliacoes/review-QA-014-piloto-jacoco-cobertura.md`.

| Achado | Severidade | O que foi feito |
|---|---|---|
| **A1** — a §"Correção de fato" invertia um item correto do backlog | high | Seção **retratada**. `.codex/agents/qa-test-specialist.toml` de fato instrui `jacoco:report`; o grep do plano não cobria `.codex/` |
| **M1** — comando publicado sem `clean`; `prepare-agent` roda com `append=true` | medium | `clean` acrescentado nos **3** documentos + a armadilha registrada no runbook e no checklist |
| **M2** — `jacoco:report` puro passou a **desistir em silêncio** em vez de falhar | medium | Registrado como **débito 7**; não corrigido porque `.codex/` exige autorização do humano |
| **B1** — "13 chamadas" a `service.fechar(...)` | low | Corrigido para **12**; a conclusão ficou mais forte (a string `null` não existe no arquivo) |
| **B2** — "quase toda a cobertura de branch faltante" era Lombok | low | Quantificado: **55%** (126 de 230) |
| **B3** — `\|\|` não escapado quebrava uma célula da tabela do runbook | low | Escapado |
| **B4** — evidência fraca sobre `argLine` (olhava o parent, não o POM efetivo) | low | Trocada por `help:effective-pom` + `grep -c argLine` = **0** |

Segunda rodada (delta-review): **`approved-with-comments`**, 1 achado `medium` e 4 cosméticos, todos aplicados.

| Achado | Severidade | O que foi feito |
|---|---|---|
| **C1** — sobrou um **quarto** lugar publicando o comando sem `clean`: o comentário do `pom.xml` | medium | `clean` acrescentado, com a explicação do `append` junto. A lista de "três documentos" do fix M1 sub-enumerou os pontos de publicação — o `pom.xml` é justamente onde quem mexe no build lê primeiro |
| **C2** — `commits:` parou em `6dff57f` | low | Frontmatter completo |
| **C3** — "`append=true` (default do plugin)" tem a fonte trocada: o mojo declara `append` **sem `default-value`**; o `true` vem do default do agente | low | Corrigido no runbook |
| **C4** — a enumeração do diff em §O que foi feito ficou fora da varredura de coerência | low | Corrigida |
| **C5** — "os quatro goals": `clean` e `test` são fases, não goals | low | Reescrito como duas fases + dois goals |

**Débito que não é meu e o planner precisa ver:** `plans/QA-014-piloto-jacoco-cobertura.md:49` **ainda** contém a frase que o achado A1 derrubou ("existe uma única ocorrência no repositório"). O status foi retratado, o plano não — e é o plano que o planner consulta ao fechar o item #6. `backlog-s04.md:123` e o `README.md` da sprint descrevem o item corretamente e **não** devem ser mexidos.

Dois dos três achados de substância (**A1** e **M1**) têm a mesma raiz: **verificação herdada em vez de refeita**. A1 repetiu o grep do plano; M1 usou `clean` por acidente (o `lombok.config` exige recompilação) sem perceber que ele era condição de validade do número. Nenhum dos dois teria aparecido em revisão de estilo — os dois exigiram reexecução.

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

1. **Versão 0.8.12 do `jacoco-maven-plugin`.** O plano não fixou versão. `spring-boot-starter-parent:3.4.5` **não** gerencia o JaCoCo (verificado no POM do parent), então a versão é obrigatoriamente explícita — foi para `<properties>` junto das de PIT e PMD.
2. **`clean` nos dois runs da comparação do Lombok.** `lombok.config` só surte efeito na **recompilação**, e o `maven-compiler-plugin` não recompila por mudança em arquivo de configuração. Sem `clean`, o run "depois" leria bytecode velho e a distorção apareceria como zero — resultado plausível e errado. O run "antes" foi refeito com `clean` também, para os dois números diferirem por uma variável só.
3. **`-Dsurefire.failIfNoSpecifiedTests=false` no comando documentado.** Blinda o comando contra um futuro em que o filtro `!*IntegrationTest` não case com nada; sem ele o surefire falharia por "nenhum teste especificado".
4. **Nova subseção numerada `Camada 1.6`**, entre a 1.5 (PIT) e a Camada 2 (PMD). Mantém a leitura em ordem de custo e deixa as três ferramentas de diagnóstico adjacentes no runbook.
5. **Reprodução do run do PIT.** Não estava no escopo, mas a comparação de três números perde o sentido se um dos lados for citação de status anterior. O baseline da QA-012 saiu idêntico, e daí veio a evidência linha a linha da divergência do `LegendaParser`.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Débitos técnicos encontrados (para o planner consolidar)

Nenhum foi corrigido: o escopo do plano proíbe escrever teste nesta task, e a proibição foi respeitada (`testes_novos: 0`).

| # | Achado | Evidência | Severidade sugerida |
|---|---|---|---|
| 1 | `FecharMesServiceImpl` — os 5 guards de `null` de `fechar(...)` nunca exercitados com `null` (100% linha / 79% branch) | tabela §Branch × linha; `jacoco.xml` linhas 59, 83, 85, 86, 87 | média — código defensivo que nunca foi provado defender |
| 2 | `PaymentRequestStrategy` — o `throw new InvalidMessageFormatException` de `parsePedido` (linhas 88 e 97) nunca é atingido no recorte unitário; branch 7/8 | PIT e JaCoCo concordam nas duas linhas | média — é a mensagem de erro que o usuário final vê |
| 3 | `MetaSignatureValidator` — `catch (NoSuchAlgorithmException \| InvalidKeyException)` (linhas 57-59) sem cobertura | já era o `NO_COVERAGE` da QA-012; JaCoCo confirma independentemente | baixa — exige HMAC-SHA256 ausente da JVM |
| 4 | `GlobalWhatsAppExceptionHandler` — 4/53 linhas e **0/18 branches** no recorte unitário; pior número do projeto | tabela §Baseline | **indeterminada até medir com integração** — pode ser coberto por `*IntegrationTest`, o que **não foi medido** |
| 5 | Cobertura da suíte de integração **não medida** e não mensurável nesta máquina (Docker/Testcontainers) | débito já registrado em `pendencias-tecnicas.md` | herda a severidade do débito de ambiente |
| 6 | `LegendaParser` continua com mutation score 75% apesar de 100% de linha **e** de branch | §Por que cobertura alta… | é o item **#2** do backlog da sprint — agora com terceiro número confirmando |
| 7 | `.codex/agents/qa-test-specialist.toml` (linhas 37 e 186) instrui `./mvnw jacoco:report` puro, que agora **passa verde sem gerar relatório** em vez de falhar | §Retratação; verificado com `target/` limpo | **média** — corrigir exige autorização do humano (`.codex/` é território dele). Ou o arquivo passa a citar o comando completo do runbook, ou o plugin ganha `<executions>` — e a segunda opção contraria a decisão de manter o JaCoCo fora do ciclo de vida |

---

## Próximos passos / observações pro próximo

- **Não apague `lombok.config`.** Ele é parte da definição da métrica: sem ele a cobertura de branch do projeto cai 17 pontos por motivo nenhum, e todo baseline registrado aqui deixa de ser comparável.
- **Ao adicionar um plugin surefire com `<argLine>`, inclua `@{argLine}`.** É o único modo de falha silencioso desta ferramenta: cobertura 0% sem erro. Registrado no runbook §Camada 1.6 e no `PRE-MERGE-CHECKLIST`.
- **A regra de `cobertura_pct` é provisória e está declarada como tal** no template e no checklist. O recorte "classes tocadas" é manual até o item **#7** do backlog automatizar por diff.
- **Para o item #7:** o `jacoco.xml` já traz contadores por `package`, por `class`, por `method` e por linha (`<line nr ci mi cb mb>`), o que torna o recorte por diff um cruzamento de nomes de classe — não precisa reprocessar nada. O par de arquivos usado aqui foi mantido fora do repositório (`target/`, gitignored); quem for automatizar reproduz com um run.
- **Piso de custo:** o run completo levou **~40 s** (374 testes + instrumentação + relatório). A instrumentação do JaCoCo é barata; o custo é a suíte.
- **Ordem com o item #2 respeitada:** o baseline foi tirado **antes** de qualquer alteração nos testes do `LegendaParser`, como o plano exigia. Se o #2 rodar agora, os números desta task viram o "antes" da comparação.

---

## Padrões técnicos

Omitido — task de instrumentação (QA-\*), sem lógica de produção. Nenhuma classe Java criada ou alterada.

---

## Arquivos criados/modificados

- `financas_bot_telegram/lombok.config` (novo: `addLombokGeneratedAnnotation`, com comentário explicando que existe para a medição de cobertura — sha256 `1aea8ee7…`)
- `financas_bot_telegram/pom.xml` (modificado: `jacoco-maven-plugin` 0.8.12 fora do ciclo de vida + property de versão)
- `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` (modificado: nova §Camada 1.6 — comando, saída, leitura das métricas, armadilha do `argLine`, ressalva unit-only)
- `docs/templates/_TEMPLATE-status.md` (modificado: `cobertura_pct` ganha a regra provisória e o comando real)
- `docs/runbooks/PRE-MERGE-CHECKLIST.md` (modificado: `cobertura_pct` vira linha da tabela de gates, informativa e não bloqueante, + item no checklist operacional)
- `docs/sprints/04-instrumentacao-qualidade/status/QA-014-piloto-jacoco-cobertura.md` (novo: este arquivo)
- `docs/sprints/04-instrumentacao-qualidade/avaliacoes/review-QA-014-piloto-jacoco-cobertura.md` (novo: relatório da revisão independente, escrito pelo Reviewer)
