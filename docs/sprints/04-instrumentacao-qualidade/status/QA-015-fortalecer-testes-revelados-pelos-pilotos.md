---
task: QA-015
titulo: "Fortalecer os testes fracos revelados pelos pilotos de PIT e JaCoCo"
data: 2026-08-16
branch: feature/qa-015-fortalecer-testes-revelados-pelos-pilotos
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: na
  testes: ok
  testes_total: 376
  testes_novos: 2
  cobertura_pct: 100
  branch_convencao: ok
  territorio: ok
commits:
  - db25d1c   # test: os dois casos novos
  - a9a9369   # docs: status report
  - a0927da   # docs: M1 da revisao + secao de revisao independente + relatorio do Reviewer
pr: null
desvios: 0
pendencias_humano: 0
---

# QA-015 — Fortalecer os testes fracos revelados pelos pilotos de PIT e JaCoCo

> **`estado: concluido`:** Reviewer deu `approved-with-notes` (§Revisão independente), a única correção obrigatória foi aplicada, todos os gates estão `ok`/`na` e `pendencias_humano: 0`. **Sem gate de QA** — o plano declara `fluxos_qa: []` e `qa_required: false`, com a justificativa de que o critério de aceitação já é um número medido que o Reviewer reproduz; acionar o QA duplicaria a mesma verificação. Registrado como **`not-applicable`**, não como pulado.
>
> **Sobre `testes_total: 376`.** É o número de testes que **efetivamente rodaram** nesta máquina, no recorte unit-only (`-Dtest='!*IntegrationTest'`) — 374 da QA-014 mais os 2 desta task. Os 48 testes das 11 classes `*IntegrationTest` ficam fora porque o Docker não está acessível ao Testcontainers aqui (débito de ambiente já registrado). **Não é regressão.** A suíte completa nesta base é verde no CI e a verificação foi **refeita nesta sessão**, não herdada da QA-014: `gh run view 31727999562 --log | grep "Tests run:"` devolve `Tests run: 422, Failures: 0, Errors: 0, Skipped: 0`, e `git merge-base --is-ancestor 15ae35e HEAD` responde **sim**. A suíte completa com esta task deveria dar **424**; esse número **não foi executado localmente** e está declarado como não verificado.
>
> **Sobre `cobertura_pct: 100`.** A regra provisória escrita pela QA-014 pede cobertura de **linha das classes de produção que a task tocou**. Esta task não altera uma linha de `src/main` — mas exercita duas classes de produção, e o plano exige número real em vez de `na`. As duas juntas: `LegendaParser` 17/17 + `PaymentRequestStrategy` 49/49 = **66/66 = 100%**.
>
> **Sobre `lint: na`.** O PMD não foi rodado: seu escopo está em `pmd.includeTests=false` (baseline congelado da QA-013) e o diff **só tem arquivo de teste**. Rodá-lo devolveria as mesmas 22 violações de produção, que nada têm a ver com esta task.

---

## O que foi feito

Dois métodos `@Test`, em dois arquivos, ambos em `src/test/`. Nenhuma linha de produção — verificado por `git diff --name-only | grep src/main`, que não devolve nada.

O que os dois casos têm em comum é a natureza da lacuna: em nenhum dos dois o problema era **falta de execução do código**. Em `LegendaParser` o código era executado inteiro e nada era verificado no limite; em `PaymentRequestStrategy` o mutante da linha do `if` já morria, mas o lado verdadeiro do desvio nunca era percorrido. São os dois erros opostos, e cada um foi encontrado por uma ferramenta diferente — que é a tese que a sprint vinha tentando demonstrar.

### Evidência de execução

Ambiente: Maven 3.9.9, **JVM 23-ea** (`openjdk version "23-ea" 2024-09-17`, build 23-ea+13-981), locale `pt_BR`, Windows 11. **É a mesma JVM da QA-012, QA-013 e QA-014** — e continua sendo a ressalva conhecida: o `pom.xml` compila com `--release 21` e o CI usa Temurin 21. O "antes" e o "depois" desta task foram medidos na **mesma máquina e mesma JVM, com minutos de intervalo**, que é o que o critério 3 pede.

| # | Comando | Resultado |
|---|---|---|
| 1 | `./financas_bot_telegram/mvnw org.pitest:pitest-maven:mutationCoverage -f financas_bot_telegram/pom.xml` (**antes**) | `BUILD SUCCESS` · `Line Coverage (for mutated classes only): 123/129 (95%)` · `Generated 42 mutations Killed 37 (88%)` · `Test strength 90%` |
| 2 | `./financas_bot_telegram/mvnw test -f financas_bot_telegram/pom.xml -Dtest='LegendaParserTest,PaymentRequestStrategyTest' -Dsurefire.failIfNoSpecifiedTests=false` | `BUILD SUCCESS` · `Tests run: 23, Failures: 0, Errors: 0, Skipped: 0` (13 + 10) |
| 3 | `./financas_bot_telegram/mvnw org.pitest:pitest-maven:mutationCoverage -f financas_bot_telegram/pom.xml` (**depois**) | `BUILD SUCCESS` · `Line Coverage: 125/129 (97%)` · `Generated 42 mutations Killed 38 (90%)` · `Test strength 93%` |
| 4 | `./financas_bot_telegram/mvnw clean jacoco:prepare-agent test jacoco:report -f financas_bot_telegram/pom.xml -Dtest='!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false` | `BUILD SUCCESS` · `Tests run: 376, Failures: 0, Errors: 0, Skipped: 0` |
| 5 | `gh run view 31727999562 --log \| grep "Tests run:"` (CI, commit `15ae35e`, ancestral de HEAD) | `Tests run: 422, Failures: 0, Errors: 0, Skipped: 0` |
| 6 | `./financas_bot_telegram/mvnw -q -DskipTests package -f financas_bot_telegram/pom.xml` | exit **0** — é a evidência de `build: ok` (acrescentada após o achado L3 da revisão) |

Os números por classe saíram de **parse programático** dos relatórios (`target/pit-reports/mutations.xml` e `target/site/jacoco/jacoco.xml`), não de leitura de percentual arredondado no HTML. Os dois arquivos estão sob `target/` e não são commitados.

⚠️ **Armadilha operacional encontrada nesta sessão, na ordem dos comandos.** O comando do JaCoCo (§Camada 1.6) começa com `clean` — obrigatório por causa do `append=true`, como a QA-014 estabeleceu. Só que `clean` apaga `target/` **inteiro**, e `target/pit-reports/` mora lá dentro: rodar JaCoCo depois do PIT **destrói o relatório do PIT**. Aconteceu aqui; os números do run 3 estavam capturados antes, então nada se perdeu, mas quem rodar na ordem inversa e for reler o `mutations.xml` encontra a pasta ausente. Sugestão para o runbook em §Débitos.

---

## Alvo 1 — `LegendaParser`: o mutante que exigia a palavra-chave no índice 0

### Antes e depois, medidos

| Momento | Mutation score | Test strength | Sobreviventes |
|---|---:|---:|---|
| **Antes** (run 1) | 6/8 = **75%** | 6/8 = **75%** | 2, ambos `ConditionalsBoundary` na linha 28 |
| **Depois** (run 3) | 7/8 = **87,5%** | 7/8 = **87,5%** | 1, `ConditionalsBoundary` na linha 28 |

O "antes" reproduz **exatamente** o baseline da QA-012 (6/8) e o da QA-014 (`123/129`, `37/42`). Nenhum número congelado foi contrariado.

### O teste

```java
@Test
void detectaPalavraChaveNoInicioDaLegenda() {
    assertThat(LegendaParser.parseTipo("pix 200")).isEqualTo(TipoPagamento.PIX);
}
```

**Por que ele mata o mutante, passo a passo.** `PALAVRAS_CHAVE` é iterado na ordem `boleto, pix, ted, agendamento`. Para `"pix 200"`: `indexOf("boleto")` = -1 (rejeitado pelos dois); `indexOf("pix")` = **0**. O original avalia `0 >= 0 && 0 < Integer.MAX_VALUE` → verdadeiro → `tipoEncontrado = PIX`. O mutante avalia `0 > 0` → falso → não entra, e as chaves seguintes também dão -1, então a função retorna `OUTRO`. Original `PIX`, mutante `OUTRO`: o teste distingue os dois, que é a definição de mutante morto.

**Por que a entrada é legítima e o teste não é artificial.** `parseTipo(String)` é utilitário estático público do domínio e o contrato dele não exige que o valor venha antes da palavra-chave. As nove legendas que existiam começavam **todas** pelo valor (`"200 pix maria"`, `"1500 TED construtora silva"`…), o que fazia `pos == 0` ser um estado inalcançável pela suíte — não pelo código.

O caso tem comentário no arquivo dizendo por que existe. Sem isso ele é o teste mais fácil de alguém apagar por parecer duplicata de `detectaPix`, e a lacuna voltaria em silêncio.

### O sobrevivente que continua vivo — e por que isso é o resultado esperado

`LegendaParser:28`, `ConditionalsBoundaryMutator`: `pos < posicaoMaisCedo` → `pos <= posicaoMaisCedo`. Ele sobreviveu no "antes" e sobrevive no "depois", como o critério 4 previa.

**Demonstração de equivalência** (reescrita aqui, não só referenciada — a QA-012 chegou à mesma conclusão pela via do prefixo):

1. Original e mutante só divergem quando `pos == posicaoMaisCedo`. Em qualquer outro caso, `<` e `<=` decidem igual.
2. `posicaoMaisCedo` começa em `Integer.MAX_VALUE`. Um `indexOf` só devolveria esse valor numa string de comprimento maior que `Integer.MAX_VALUE`, o que a JVM não admite. Logo a igualdade nunca acontece contra o valor inicial.
3. Sobra a igualdade contra um índice já gravado: exigiria **duas palavras-chave distintas ocorrendo no mesmo índice da mesma string**. Duas strings distintas que começam na mesma posição implicam que a mais curta é prefixo da mais longa.
4. As chaves são `boleto`, `pix`, `ted`, `agendamento` — **iniciais distintas** (`b`, `p`, `t`, `a`), logo nenhuma é prefixo de outra.

Não existe entrada que faça as duas versões divergirem ⇒ **mutante equivalente**, impossível de matar por construção. Persegui-lo produziria teste sem valor. Por isso ele fica **fora do denominador** do gate, conforme `financas_bot_telegram/CLAUDE.md` §"Critério de mutation testing" e ADR 0021.

> A demonstração depende do **conteúdo** de `PALAVRAS_CHAVE`. Acrescentar uma chave que seja prefixo de outra (ex.: `pix` e `pixel`) torna o mutante matável e a classificação obsoleta.

---

## Alvo 2 — o `throw` de `parsePedido`, e o que ele significa de verdade

### O que mudou nos números

| Ferramenta | Antes | Depois |
|---|---|---|
| PIT — mutation score / test strength | 11/11 = 100% | 11/11 = **100%** (inalterado) |
| JaCoCo — branch | 7/8 = 88% | **8/8 = 100%** |
| JaCoCo — linha | 47/49 = 96% | **49/49 = 100%** |
| JaCoCo — instrução | 178/185 = 96% | **185/185 = 100%** |

### O achado que inverte a lição da sprint

**O PIT não se moveu.** Ele já dava 11/11 antes de existir qualquer teste que atingisse o `throw`. A razão está no relatório: o único mutante da linha 87 é o `NegateConditionalsMutator` sobre `if (!matcher.matches())`, e ele morre pelo **lado oposto** do desvio — negar a condição faz as legendas válidas passarem a lançar exceção, e os testes de caminho feliz quebram na hora. Nenhum mutante foi gerado nas linhas 88-97 (a construção da exceção), então **o buraco era invisível ao PIT**.

Isso é o **espelho** do caso `LegendaParser`, que a QA-014 usou como peça central:

| Classe | O que o PIT via | O que o JaCoCo via | Quem achou o buraco |
|---|---|---|---|
| `LegendaParser` | 75% — buraco de **asserção** | 100% linha / 100% branch | **PIT** |
| `PaymentRequestStrategy` | 100% — nada | 88% branch — buraco de **execução** | **JaCoCo** |

A conclusão prática: mutation score alto **não** implica cobertura completa, do mesmo jeito que cobertura completa não implica mutation score alto. Nenhuma das duas domina a outra, e as duas juntas acharam mais que qualquer uma sozinha — com dado deste projeto, nas duas direções.

### O teste, e a ressalva que o plano exigiu por escrito

```java
assertThatThrownBy(() -> strategy.process(dto))
    .isInstanceOf(InvalidMessageFormatException.class)
    .hasMessageContaining("<valor> <descrição>")
    .extracting(e -> ((InvalidMessageFormatException) e).getChatId())
    .isEqualTo(12345L);

verifyNoInteractions(salvarPedidoPagamentoUsecase, canalNotificadorPort);
```

⚠️ **Este `throw` é inalcançável pelo caminho do dispatcher, e o teste não é a reprodução de um cenário de usuário.** A razão, verificada no fonte e não suposta: `supports()` (linha 48) e `parsePedido` (linha 85) aplicam o **mesmo `PEDIDO_PATTERN`** sobre o **mesmo `dto.getCaption().trim()`**. Como `process()` só é chamado depois de `supports()` devolver `true`, a condição `!matcher.matches()` da linha 87 é sempre falsa nesse caminho. É redundância defensiva.

Descrever este teste como "cobre a mensagem de erro que o usuário final vê" seria **falso** — e a `PaymentRequestStrategy` é justamente a classe onde essa frase apareceria com mais naturalidade, porque a mensagem tem exemplos formatados para o Telegram. O que o teste é: **teste de contrato do método público `process()`**, garantindo que a classe rejeita entrada malformada quando chamada fora do dispatcher, em vez de persistir lixo. É o que o justifica: a redundância defensiva deixa de ser removível sem que nada quebre.

> Nota para quem for mexer aqui: o débito 2 da QA-014 classificou o gap como *"média — é a mensagem de erro que o usuário final vê"*. Essa severidade **está superestimada**, pela razão acima. O gap era real e vale ter fechado; a justificativa registrada lá é que precisa de correção quando o planner consolidar.

**O que a asserção verifica, e por que não é só o tipo da exceção.** Seguindo o padrão que a QA-012 registrou (assegurar **valor**, não ocorrência de chamada): confere o tipo, o texto que orienta o usuário, o `chatId` propagado (é o que faz a resposta chegar ao chat certo) e — o mais forte — que **nenhum** dos dois colaboradores de efeito foi tocado. `verifyNoInteractions` transforma "lançou exceção" em "abortou antes de persistir e antes de notificar", que é a propriedade que realmente importa.

**Sobre o mock não-stubado:** `s3ImageUploadService.uploadFile(...)` é chamado antes do `parsePedido` e devolve `null` por default do Mockito. Não foi stubado de propósito — stub sem uso quebraria o `MockitoExtension` em modo estrito. O `null` só vai para um `logger.info`, e o teste não depende dele.

---

## Gate de mutação (ADR 0021) — primeira task do repositório sob o gate

**Escopo da medição:** as duas classes de produção que os testes desta task exercitam. Nenhuma outra entra no denominador. As duas já estavam em `targetClasses` do `pom.xml` — **nenhuma alteração de escopo do PIT foi necessária**, e portanto a triagem do runbook (§Camada 1.5) não precisou ser acionada.

| Classe | Mortos | Cobertos | Gerados | **Test strength** (mortos ÷ cobertos) | Mutation score (mortos ÷ gerados) |
|---|---:|---:|---:|---:|---:|
| `LegendaParser` | 7 | 8 | 8 | **87,5%** | 87,5% |
| `PaymentRequestStrategy` | 11 | 11 | 11 | **100%** | 100% |
| **Conjunto da task** | **18** | **19** | **19** | **94,7%** | 94,7% |

**94,7% ≥ 80% — gate satisfeito.** Descontando o único sobrevivente, que é equivalente **com demonstração escrita** nesta página: **18/18 = 100%**, que é o teto real desta medição. Nenhum `NO_COVERAGE` e nenhum `TIMED_OUT` nas duas classes, então aqui `test strength` e mutation score coincidem — a distinção entre as duas métricas não é decorativa, só não morde neste caso.

Status dos 19 mutantes do escopo: **18 `KILLED`**, **1 `SURVIVED`** (o equivalente da linha 28). As duas classes fora do escopo (`PaymentProofStrategy` 9/9, `MetaSignatureValidator` 11/14) aparecem no run porque estão no `targetClasses` congelado, e **não entram na conta do gate**: são código pré-existente que esta task não tocou.

Nenhum teste foi escrito para levantar métrica, nenhuma classe foi removida do escopo e o piso não foi mexido.

---

## Verificação de carregamento de skills (coleta de evidência — **não é critério de aceitação**)

> Rodada única, pedida pelo plano §"Coleta de evidência". O que segue é observação sobre o harness.

**1. Pré-carga.** O `.claude/agents/backend.md` declara **quatro** skills no campo `skills:` — `workflow-gates-core`, `artifact-report-contract`, `developing-java-spring-applications` e `writing-java-unit-tests`. Respondendo por skill, e limitando-me ao que é observável de dentro da sessão:

- `writing-java-unit-tests`: **não consigo afirmar que o corpo do `SKILL.md` estava no meu contexto.** Até o probe deliberado descrito no ponto 2, eu não tinha acesso citável ao texto dele. Escrevi os dois testes a partir das instruções do agente, do plano e dos padrões já presentes nos próprios arquivos de teste (`ArgumentCaptor`, `eq(...)`, AssertJ).
- `developing-java-spring-applications`: idem, e **não foi consultada** — a task não escreve produção.
- `workflow-gates-core` e `artifact-report-contract` (as duas que a definição do agente afirma estarem "in context from the first turn"): mesma observação. O sinal mais concreto é que **precisei ler `docs/templates/_TEMPLATE-status.md` do disco** para conhecer o schema do frontmatter; se o contrato de artefato estivesse disponível como texto, essa leitura teria sido redundante.

Registro isso como **incerteza honesta**, não como afirmação de que a injeção falhou: não tenho como inspecionar meu próprio contexto para provar ausência. O que está provado é o ponto 2.

**2. Assets — probe deliberado, declarado como tal.** Nenhuma leitura de asset foi **necessária**: a task é de teste puro e os padrões estavam nos arquivos sendo editados. Como o plano autoriza o probe desde que declarado, rodei-o **depois** de os testes estarem escritos e verdes, para não contaminar o comportamento natural.

| Tentativa | Path usado | Resultado |
|---|---|---|
| 1 (inválida como evidência) | `references/mocking-guidelines.md` | `File does not exist. Note: your current working directory is C:\Users\satya\src\financas_bot_telegram`. **Descartada**: eu havia chutado o nome do arquivo, então o erro é ambíguo entre "path não resolve" e "arquivo não existe" |
| 2 (a que vale) | `references/test-doubles-guidelines.md` — o path **exatamente como o `SKILL.md` o escreve** (linhas 52 e 73), e o arquivo **existe** | **Falhou**, com a mesma mensagem apontando o cwd |
| 3 (controle) | `C:\Users\satya\src\financas_bot_telegram\.claude\skills\writing-java-unit-tests\references\test-doubles-guidelines.md` | **Sucesso**, 29 linhas lidas |

**Conclusão, com o par falha/sucesso sobre o mesmo arquivo:** o path relativo dos `SKILL.md` **não é ancorado no diretório da skill** — ele é resolvido contra o diretório de trabalho da sessão (a raiz do repo). O arquivo existe; o path publicado não chega nele. **O risco em aberto da ADR 0022 se confirma em runtime**, e não como leitura de documentação. A localização exigiu `ls`/`find` em `.claude/skills/`.

**3.** Não se aplica — houve leitura de asset (deliberada, item 2).

**Encaminhamento sugerido (decisão não é minha):** os `SKILL.md` de `developing-java-spring-applications` (**10** links relativos, linhas 52-78) e `writing-java-unit-tests` (**5** links, linhas 50-74) precisam de âncora explícita — contagem por `grep -o -E "\]\((references|examples)/[^)]+\)"` — `${CLAUDE_SKILL_DIR}` ou path a partir da raiz do repo. **Não alterei nenhum arquivo em `.claude/`**: é território do humano e do `ai-engineer`, e exige autorização explícita que não foi pedida nem dada.

---

## Revisão independente (ADR 0005)

Veredito: **`approved-with-notes`**. Relatório em `docs/sprints/04-instrumentacao-qualidade/avaliacoes/review-QA-015-fortalecer-testes-revelados-pelos-pilotos.md`. **Zero achado `critical` ou `high`; nenhuma remediação de código.**

O Reviewer **reexecutou** PIT, JaCoCo e build no mesmo ambiente e reproduziu os dois números que eram o entregável (`LegendaParser` 7/8, `PaymentRequestStrategy` 8/8 branch), o denominador do gate e a igualdade do frontmatter com o diff. Além disso trouxe **duas evidências mais fortes que as minhas**, que vale registrar:

- Dos dois `ConditionalsBoundaryMutator` da linha 28, o `mutations.xml` traz o de `index 52` como `KILLED` com `<killingTest>` **exclusivo** apontando `LegendaParserTest.detectaPalavraChaveNoInicioDaLegenda()`, e o de `index 55` como `SURVIVED` com `<killingTest/>` vazio. Isso prova, num só campo, o ganho do teste novo, que o "antes" era 6/8, e que o sobrevivente remanescente é o `pos <=`. Eu havia provado as três coisas separadamente.
- A inalcançabilidade do `throw` foi verificada não só nas duas aplicações do `PEDIDO_PATTERN`, mas no **único call site de `process()` em produção** — `MensagemEntranteService:73`, logo após o `.filter(s -> s.supports(dto))` da linha 58, sem mutação do DTO no meio. É a verificação que fecha o argumento: eu havia checado que as duas validações são idênticas, não que não existe outro caminho até `process()`.

A demonstração de equivalência do sobrevivente foi reconstruída passo a passo pelo Reviewer contra o fonte e **não caiu**.

| Achado | Severidade | O que foi feito |
|---|---|---|
| **M1** — §Próximos passos mandava passar o teto do `STATE.md` de 39/42 para **40/42**. O 39/42 é **teto** (`42 − 2 equivalentes − 1 inalcançável`), não score; o mutante morto aqui já estava contado como matável dentro dele. O que muda é o score: **37/42 → 38/42** | medium | Corrigido em §Próximos passos, com a conta dos 4 não-mortos. **A raiz está no plano**, que carrega o mesmo `40/42` — sinalizado ao planner ali e no débito 6 |
| **L1** — o comentário do teste cita "QA-012", o que roça a regra de não referenciar processo em código | low | Mantido: a citação foi **exigida pelo plano** (§Escopo/arquivos) e é auto-contida — o comentário explica o mutante em termos técnicos e usa "QA-012" só como origem da medição, sem número de seção |
| **L2** — `commits:` não listava o commit do status | low | Frontmatter completo |
| **L3** — `build: ok` sem o comando canônico registrado | low | `mvnw -q -DskipTests package` → exit 0, na linha 6 da tabela de evidência |
| **L4** — `lint: na` estica a letra do checklist | low | Justificativa mantida (o `pmd.includeTests=false` deixa um diff só-de-teste fora do escopo do linter); o buraco do checklist virou **débito 7** |
| **L5** — `Tests run: 23 (13 + 10)` com a ordem invertida em relação ao comando | low | A ordem no comando é `LegendaParserTest,PaymentRequestStrategyTest`; a saída do surefire veio na ordem inversa (13 da `PaymentRequestStrategyTest`, 10 da `LegendaParserTest`). Registrado aqui em vez de reescrito, porque o número por classe é o que importa |

O único achado de substância (M1) tem a mesma raiz dos achados A1/M1 da QA-014: **verificação herdada em vez de refeita**. Copiei do plano um número que não conferi contra o `STATE.md`, e o `39/42` está a uma linha de distância (`STATE.md:31`), com a fórmula escrita ao lado.

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

1. **Um único teste para o alvo 2, não dois.** O plano admitia "um ou dois". Um basta: ele leva o branch a 8/8 e a linha a 49/49, que é o critério 5 inteiro. Um segundo caso (outra legenda inválida) percorreria exatamente o mesmo caminho e não acrescentaria asserção nova.
2. **`verifyNoInteractions` em vez de só `assertThatThrownBy`.** A propriedade que interessa não é "lançou", é "abortou sem persistir e sem notificar". É a aplicação do padrão que a QA-012 registrou: assegurar valor/efeito, não ocorrência.
3. **Não stubar `s3ImageUploadService` no teste do `throw`.** O default `null` do Mockito serve, e um stub não usado quebraria o modo estrito do `MockitoExtension`.
4. **Comentário de motivo em cada um dos dois casos novos.** Ambos parecem redundantes com testes vizinhos para quem lê sem o contexto da medição — e um teste que parece redundante é um teste que alguém apaga. Os comentários descrevem o mutante e o desvio em termos técnicos, sem citar seção de processo.
5. **Números por classe extraídos por parse de `mutations.xml` e `jacoco.xml`.** O percentual do HTML é arredondado; o critério pede 7/8 e 8/8, que são contagens. Parse elimina a chance de "87%" ser lido como confirmação de algo que não foi contado.
6. **PMD não executado** (`lint: na`) — justificado no bloco do topo.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada. (O merge do PR `feature → integration` é do implementador, conforme o `CLAUDE.md` da raiz.)

---

## Débitos técnicos encontrados (para o planner consolidar)

| # | Achado | Evidência | Severidade sugerida |
|---|---|---|---|
| 1 | **Resolução de path relativo nos assets das skills falha.** `references/<arquivo>.md` dos `SKILL.md` resolve contra o cwd do repo, não contra o diretório da skill. Confirmado com o par falha/sucesso sobre o **mesmo arquivo existente** | §Verificação de carregamento de skills, tentativas 2 e 3 | **média** — a maior parte do conteúdo das duas skills Java está nos assets, e a falha é silenciosa. Correção exige mexer em `.claude/`, que precisa de autorização do humano |
| 2 | **`clean` do comando do JaCoCo apaga `target/pit-reports/`.** Rodar §Camada 1.6 depois de §Camada 1.5 destrói o relatório do PIT sem aviso | Observado nesta sessão | **baixa** — sugestão: uma linha no runbook mandando capturar os números do PIT antes, ou rodar JaCoCo primeiro |
| 3 | **A severidade do débito 2 da QA-014 está superestimada.** Ele descreve o `throw` de `parsePedido` como "a mensagem de erro que o usuário final vê"; o caminho é inalcançável pelo dispatcher. O Reviewer confirmou no fonte e localizou a origem real da mensagem que o usuário vê: **`MensagemEntranteService:60-63`**, não este `throw` | §Alvo 2; `PaymentRequestStrategy` linhas 48 e 85-87; `MensagemEntranteService:58` e `:73` | **baixa** — é correção de registro, não de código. Vale ajustar ao fechar o item |
| 4 | **Redundância de validação em `PaymentRequestStrategy`.** O mesmo `PEDIDO_PATTERN` é aplicado em `supports()` e em `parsePedido`. Não é bug e **não foi mexido** (a task proíbe tocar produção); é decisão a tomar com calma — ou o `throw` vira genuinamente alcançável, ou a duplicação sai | §Alvo 2 | **baixa** — agora protegida por teste, que era o objetivo |
| 5 | **O plano da QA-015 manda elevar o teto do `STATE.md` de 39/42 para 40/42** (§Coordenação > "Após merge"). Está errado pela mesma conta do achado M1: 39/42 é teto, e o mutante morto aqui já estava dentro dele. O status foi corrigido; **o plano não** | §Revisão independente, M1; `STATE.md:31` | **média** — é o documento que o planner consulta ao fechar o item #2, e o erro vai direto para o `STATE.md` se ninguém corrigir |
| 6 | **`MetaSignatureValidator:28` é o único mutante matável ainda vivo** nas quatro classes do `targetClasses` (o warn de `app-secret` ausente). Matá-lo exige infra de captura de appender de log, que o repo não tem | run desta sessão: 4 não-mortos = 2 equivalentes + 1 `NO_COVERAGE` + este | **baixa** — já registrado pela QA-012; a novidade é que agora ele é o **último**, o que torna o custo/benefício da infra de log decidível |
| 7 | **O `PRE-MERGE-CHECKLIST` não cobre "tocou Java, mas fora do escopo configurado do linter"** (achado L4 do Reviewer). Hoje o campo `lint` só admite `ok`/`fail`/`na`, e um diff só-de-teste com `pmd.includeTests=false` cai em `na` — que passa a significar duas coisas diferentes | §bloco do topo; `pom.xml`, property `pmd.includeTests` | **baixa** — ambiguidade de registro, não de qualidade |
| 8 | Débitos herdados **não tocados**, conforme §Fora de escopo do plano: 5 guards de `null` em `FecharMesServiceImpl`; `GlobalWhatsAppExceptionHandler` 0/18 branches; `catch` do `MetaSignatureValidator`; `toLowerCase()` sem `Locale`; JVM 23-ea local × Temurin 21 no CI; suíte de integração não executável nesta máquina | registros da QA-012/013/014 | herdadas |

---

## Próximos passos / observações pro próximo

- **Atualizar o `STATE.md`:** baseline de `LegendaParser` passa de **6/8 para 7/8**, e o **score agregado** das quatro classes do `targetClasses` passa de **37/42 para 38/42** (`Generated 42 mutations Killed 38 (90%)`, `Test strength 93%`, run desta sessão).

  ⚠️ **O teto de `39/42` do `STATE.md:31` NÃO muda — e a instrução em contrário está errada.** O erro nasceu no plano (§Coordenação > "Após merge", que manda passar o teto "de 39/42 para 40/42"), foi repetido aqui na primeira escrita deste status, e foi derrubado pelo Reviewer (achado M1). O `39/42` é **teto**, não score: `42 − 2 equivalentes demonstrados − 1 inalcançável na prática` (`STATE.md:31` e QA-012 §Próximos passos, linha 264). O mutante que esta task matou **já estava contado como matável dentro dos 39** — matá-lo aproxima o score do teto em vez de elevá-lo. Verificado no run: dos 4 mutantes não mortos, `LegendaParser:28` e `MetaSignatureValidator:64` são equivalentes, `MetaSignatureValidator:59` é `NO_COVERAGE` inalcançável, e sobra **`MetaSignatureValidator:28` como o único matável ainda vivo** nas quatro classes — exatamente o `38 + 1 = 39`.

  **Para o planner:** o `40/42` continua vivo no **plano** da QA-015. Corrigir só este status deixa a fonte do erro de pé, e é o plano que se consulta ao fechar o item #2.
- **Fechar o item #2 do `backlog-s04.md`** com: `LegendaParser` 6/8 → 7/8; `PaymentRequestStrategy` branch 7/8 → 8/8; sobrevivente restante declarado equivalente.
- **A tag do marco zero do experimento está desbloqueada** por esta task e deve ser criada **depois** do merge — as duas classes que a feature do experimento toca agora estão no estado final.
- **O que sobrou em `LegendaParser` é teto, não pendência.** 7/8 é o máximo alcançável; nenhum reforço futuro de teste vai levar a 8/8, e uma task que prometa isso está prometendo o impossível.
- **`.claude/agents/backend.md` está modificado na árvore de trabalho** (`model: inherit` → `model: opus`) e **não entrou em nenhum commit meu**: é território do humano.

---

## Padrões técnicos

Task exclusivamente de teste, sem lógica de produção nova. O que se aplica:

- **Arquitetura hexagonal preservada por omissão** — nenhum arquivo de `src/main/` foi tocado, logo nenhuma fronteira foi movida. O teste do alvo 2 exercita a `PaymentRequestStrategy` pela sua interface pública (`process`), sem alcançar detalhe interno: `parsePedido` continua privado e não foi exposto para viabilizar teste.
- **Independência entre fixture e código sob teste.** As entradas (`"pix 200"`, `"Apenas descricao sem valor"`) foram derivadas do **contrato público** das duas classes, não da implementação: a primeira sai de "a palavra-chave pode aparecer em qualquer posição", a segunda de "a legenda precisa começar por um valor". Nenhuma delas foi construída copiando a regex ou a ordem do `LinkedHashMap`.
- **Asserção sobre valor, não sobre chamada** — o padrão que a QA-012 identificou como o que mata mutante, aplicado no alvo 2 via `verifyNoInteractions` mais asserção sobre tipo, mensagem e `chatId`.

---

## Arquivos criados/modificados

- `financas_bot_telegram/src/test/java/.../domain/service/LegendaParserTest.java` (modificado: 1 `@Test` novo — palavra-chave no índice 0, com comentário do motivo)
- `financas_bot_telegram/src/test/java/.../application/strategy/PaymentRequestStrategyTest.java` (modificado: 1 `@Test` novo — contrato de `process()` com legenda inválida; 2 imports)
- `docs/sprints/04-instrumentacao-qualidade/status/QA-015-fortalecer-testes-revelados-pelos-pilotos.md` (novo: este arquivo)
- `docs/sprints/04-instrumentacao-qualidade/avaliacoes/review-QA-015-fortalecer-testes-revelados-pelos-pilotos.md` (novo: relatório da revisão independente, escrito pelo Reviewer)
