---
task: QA-015
titulo: "Fortalecer os testes fracos revelados pelos pilotos de PIT e JaCoCo"
sprint: 04-instrumentacao-qualidade
data_planejamento: 2026-08-16
branch_alvo: feature/qa-015-fortalecer-testes-revelados-pelos-pilotos
prioridade: alta
esforco: baixo
territorio: back
estado: pronto-pra-execucao
depende_de: [QA-012, QA-014]
bloqueia: []
skills_dispatched: []
integration_branch: integration/04-instrumentacao-qualidade
fluxos_qa: []
mutation_gate: true
mutation_rationale: "Adotado por decisão explícita do humano em 2026-08-16. Escopo da medição: LegendaParser e PaymentRequestStrategy — as duas classes de produção cujos testes esta task altera. É a primeira task do repositório a rodar sob o gate, e é o caso ideal para estreá-lo: o critério de aceitação já é um número do PIT, então o gate não acrescenta trabalho, só formaliza a verificação. Piso de 80% de test strength conforme ADR 0021; o sobrevivente equivalente de LegendaParser:28 está fora do denominador por já ter demonstração escrita na QA-012."
---

# QA-015 — Fortalecer os testes fracos revelados pelos pilotos de PIT e JaCoCo

## Intake

- **Origem:** item **#2** de `docs/sprints/04-instrumentacao-qualidade/backlog-s04.md` ("corrigir os testes fracos revelados pelo piloto"), que sempre dependeu do #1 e teve o escopo definido pelo que os pilotos mediram. Alvos vindos da QA-012 (sobrevivente nº 1) e da QA-014 (débito 2).
- **Por quê agora:** o backlog exige que este item aconteça **antes da tag do marco zero** do experimento — ele toca exatamente as classes que a feature do experimento vai tocar, e mexer nelas no meio do experimento mudaria o baseline no voo, tornando os runs incomparáveis. O humano autorizou a ordem em 2026-08-16.
- **Esforço:** baixo. Dois testes unitários pequenos, sem Spring, sem Testcontainers, sem mudança em código de produção.
- **Riscos resumidos:** o risco desta task **não é técnico, é de disciplina de escopo** — o convite a "melhorar mais um pouquinho" enquanto se está com o relatório do PIT aberto. Os alvos são dois e estão nomeados; tudo o mais está em §Fora de escopo com o motivo escrito.

---

## Contexto

### O que os pilotos mediram (fatos, não estimativas)

`LegendaParser` — QA-012, run do PIT:

| Métrica | Valor |
|---|---|
| Mutation score | **75%** (6/8) |
| Test strength | **75%** (6/8) |
| Line coverage (PIT) | 94% (17/18) |
| Line/branch (JaCoCo, QA-014) | **100% / 100%** |

É o caso central das três tasks de ferramental: **cobertura perfeita nos dois eixos e 25% dos mutantes sobrevivendo**. A QA-014 confirmou com um terceiro número que o problema não é o que o teste executa, é o que ele verifica.

`PaymentRequestStrategy` — QA-012 deu 11/11 mutantes mortos e 96% de linha (47/49); a QA-014 registrou como **débito 2** que as duas linhas descobertas são o `throw new InvalidMessageFormatException` de `parsePedido`, com branch **7/8**. PIT e JaCoCo concordam nas duas linhas.

### O que está no código hoje — verificado nesta sessão

`LegendaParser.parseTipo` (`domain/service/LegendaParser.java:19-35`) percorre `PALAVRAS_CHAVE` e guarda a ocorrência de menor índice:

```java
int pos = alvo.indexOf(entry.getKey());
if (pos >= 0 && pos < posicaoMaisCedo) {   // ← linha 28
```

`LegendaParserTest` tem **9 casos**, listados por leitura direta do arquivo: `"150.00 Almoço boleto"`, `"200 pix maria"`, `"1500 TED construtora silva"`, `"300 agendamento luz"`, `"100 Almoço"`, `"100 BOLETO pix"`, `""`, `null`, `"500 PIX aluguel"`. **Nenhuma legenda começa pela palavra-chave** — todas começam pelo valor. Por isso `pos == 0` nunca acontece, e o mutante `pos >= 0` → `pos > 0` sobrevive: ele só se distingue do original quando a palavra-chave está no índice 0.

`PaymentRequestStrategy` (`application/strategy/PaymentRequestStrategy.java`) valida a legenda **duas vezes** com o mesmo `PEDIDO_PATTERN` — em `supports()` (linha 48) e de novo dentro de `parsePedido` (linha 87). Isso tem consequência direta para esta task e está tratado em §Decisão.

`PaymentRequestStrategyTest` tem 12 casos; **nenhum** cobre o `throw` de `parsePedido` — verificado por leitura. O caso `naoDeveSuportarLegendaSemValor` exercita `supports()`, que é outro caminho.

### O que a QA-012 registrou como padrão a replicar

Os testes de `PaymentRequestStrategy` e `PaymentProofStrategy` mataram **20/20** mutantes porque asseguram o **valor** dos argumentos que chegam ao colaborador — via `ArgumentCaptor` numa classe, via `eq(...)` na outra —, em vez de só verificar que o mock foi chamado. É o mecanismo que mata mutante, e é o padrão que esta task replica.

---

## Decisão / abordagem

### Alvo 1 — `LegendaParser` com palavra-chave no índice 0

Um teste: `parseTipo("pix 200")` deve devolver `PIX`.

Por que mata o mutante: `indexOf("pix")` devolve `0`; o original aceita (`0 >= 0`), o mutante rejeita (`0 > 0` é falso) e a função cai no `return tipoEncontrado` com `OUTRO`. O teste distingue os dois, que é a definição de mutante morto.

Por que é lacuna legítima e não caso artificial: `parseTipo(String)` é um utilitário estático público do domínio, e nada no contrato dele exige que o valor venha antes. A entrada é legítima.

### Alvo 2 — o `throw` de `parsePedido`, e a ressalva que o implementador precisa ler

Um ou dois testes chamando `process()` diretamente com um DTO que tenha `fileBytes` e uma legenda que **não** case com `PEDIDO_PATTERN`, esperando `InvalidMessageFormatException`.

**A ressalva, que precisa estar no status report:** `supports()` e `parsePedido` aplicam **exatamente o mesmo `PEDIDO_PATTERN`** sobre o mesmo `caption.trim()`. Pelo caminho normal do dispatcher, `process()` só é chamado depois de `supports()` devolver `true` — logo o `throw` da linha 88 é **inalcançável em produção**. Ele é redundância defensiva, não um caminho vivo.

Isso não invalida o teste, mas muda o que ele significa, e a diferença precisa estar escrita: ele é **teste de contrato do método público** `process()`, que garante que a classe não engole entrada inválida se for chamada fora do dispatcher. Não é a reprodução de um cenário de usuário. Descrever esse teste como "cobre o erro que o usuário vê" seria falso — e é o tipo de afirmação que o Reviewer derrubou na QA-014.

**Alternativa considerada e descartada:** classificar o gap como defensivo-e-inalcançável, do jeito que a QA-012 fez com o `catch` de `MetaSignatureValidator`, e não escrever teste. Descartada porque o custo aqui é diferente: aquele exigia injetar um provider JCE falso, este são dez linhas com os mocks que o teste já tem montados. Quando o teste é barato, "inalcançável hoje" não é motivo para deixar sem asserção — a redundância pode ser removida amanhã por alguém que a ache gratuita.

### Ordem de execução dentro da task

1. Rodar o PIT **antes** de qualquer alteração, confirmando 6/8 em `LegendaParser`. Sem o "antes" medido na mesma máquina e na mesma JVM, o "depois" não prova nada.
2. Escrever os testes.
3. Rodar PIT e JaCoCo de novo e registrar os dois pares de números.

O passo 1 não é burocracia: a QA-012 colheu os números originais em **JVM 23-ea**, enquanto CI e produção usam Temurin 21 — está registrado como débito. Comparar o "depois" desta task contra aquele "antes" seria comparar medições de ambientes diferentes.

### Nada de código de produção

Esta task escreve teste. Se algum teste falhar contra o código atual, isso é **achado**, não convite a corrigir: para, registra e reporta. Mudança de produção nesta task tornaria o "depois" do PIT incomparável com o "antes", que é justamente o que ela precisa provar.

---

## Escopo / arquivos

### Modificar

- `financas_bot_telegram/src/test/java/.../domain/service/LegendaParserTest.java` — acrescentar o caso de palavra-chave no índice 0. Nome sugerido: `detectaPalavraChaveNoInicioDaLegenda`. O `@DisplayName`, ou um comentário de uma linha, deve dizer que o caso existe por causa de um mutante `ConditionalsBoundary` da QA-012 — sem isso, é o teste mais fácil de alguém apagar por parecer redundante com `detectaPix`.
- `financas_bot_telegram/src/test/java/.../application/strategy/PaymentRequestStrategyTest.java` — acrescentar o(s) caso(s) do `throw` de `parsePedido`, usando os helpers de DTO que já existem no arquivo (`dtoCompleto`, `dtoComExtensao`).

### Não tocar

- **Nenhum arquivo em `src/main/`.** `testes_novos > 0` e zero linha de produção no diff.
- `pom.xml`, `pmd-ruleset.xml`, `lombok.config` — o ferramental está congelado e com baseline registrado.
- Os testes das outras três classes do piloto.

---

## Testes

Rodar, com os comandos canônicos do `docs/runbooks/ROTEIRO-TESTES-BACKEND.md`:

```bash
# suíte unitária
./mvnw test

# PIT — §Camada 1.5
./financas_bot_telegram/mvnw org.pitest:pitest-maven:mutationCoverage -f financas_bot_telegram/pom.xml

# JaCoCo — §Camada 1.6 (o `clean` não é opcional: append=true é o default)
./financas_bot_telegram/mvnw clean jacoco:prepare-agent test jacoco:report -f financas_bot_telegram/pom.xml \
  -Dtest='!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false
```

**Sobre o gate `testes`:** os 48 testes `*IntegrationTest` falham nesta máquina por Docker inacessível ao Testcontainers — débito de ambiente registrado, verificado verde no CI (`Tests run: 422`, run `31727999562`). Não é regressão desta task e **não deve** ser reportado como se fosse. Relatar o resultado da suíte unitária e citar o débito.

---

## Coleta de evidência — verificação de carregamento de skills

> ⚠️ **Isto NÃO é critério de aceitação da task.** É observação sobre o harness, aproveitando que esta é a primeira task Java depois da ADR 0022. Nada aqui reprova a entrega: uma resposta "não consegui" é resultado válido e é justamente o dado que se quer. **Rodada única** — não vira praxe, não entra em plano futuro.

**Por que agora:** desde 2026-08-16 o `.claude/agents/backend.md` pré-carrega `developing-java-spring-applications` e `writing-java-unit-tests` via campo `skills:` do frontmatter. Mas os dois `SKILL.md` linkam seus arquivos de apoio por **path relativo** (`references/mvc-architecture.md`, `examples/...`), e a documentação oficial **nunca diz** como esse path é resolvido em disco — a própria existência da substituição `${CLAUDE_SKILL_DIR}` sugere que caminho relativo **não** é ancorado automaticamente no diretório da skill. Isso importa porque a maior parte do conteúdo dessas duas skills mora nos assets: **todos os exemplos de código estão lá** (~3/4 dos bytes em `developing-java-spring-applications`, ~60% em `writing-java-unit-tests`). Se o path não resolver, o agente recebe a política **sem os padrões** — e a falha é silenciosa, ninguém percebe. Detalhe em `docs/aprendizado/skills-em-subagentes-preload-vs-sob-demanda.md` §"Risco em aberto".

O agente `backend` deve registrar no status report uma seção **`Verificação de carregamento de skills`** com estes três pontos:

1. **Pré-carga.** Se o conteúdo de `developing-java-spring-applications` e `writing-java-unit-tests` estava disponível **desde o primeiro turno**, sem precisar invocar nada. Responder por skill, não em bloco.
2. **Assets.** Se leu algum arquivo de `references/` ou `examples/` dessas duas skills. Em caso positivo: **qual path exato** usou, e se **acertou na primeira tentativa** ou precisou de `Glob`/tentativa e erro para localizar o arquivo. O path errado que falhou, quando houver, é mais informativo que o certo — registrar os dois.
3. **Se não leu nenhum asset**, dizer explicitamente qual dos dois casos ocorreu: **não foi necessário** (a task não exigiu consultar exemplo), ou **não conseguiu resolver o path**.

**Como isso vai ser usado:** se a resolução de path falhar, acrescentamos âncora explícita nos dois `SKILL.md`. Se funcionar, o risco em aberto da ADR 0022 fecha com evidência de runtime em vez de leitura de doc.

> Observação de honestidade da coleta: esta task é de **teste puro**, então é plausível que o ponto 2 caia legitimamente no "não foi necessário" — a `writing-java-unit-tests` é a skill relevante aqui, e a de Spring provavelmente não será consultada. Isso é resultado, não falha da coleta. **Não** consultar um asset artificialmente só para produzir o dado: um `Read` forçado responde se o path resolve, mas mente sobre o comportamento natural do agente. Se o implementador quiser testar o path sem ter necessidade real, deve dizer no status que a leitura foi **deliberada para esta verificação**.

---

## Critérios de aceitação

1. `LegendaParserTest` tem caso com palavra-chave no índice 0, e ele **passa** contra o código atual.
2. O PIT reporta `LegendaParser` em **7/8** — mutation score e test strength de **87,5%**. O número vem do relatório, não de inferência.
3. O relatório do PIT é rodado **antes e depois**, na mesma máquina e mesma JVM, e o status registra os dois valores com a versão da JVM usada.
4. O mutante sobrevivente restante de `LegendaParser:28` (`pos < posicaoMaisCedo` → `pos <=`) continua vivo e é **declarado como equivalente**, referenciando a demonstração da QA-012 (baseada em prefixo). Persegui-lo é violação de escopo, não zelo.
5. `PaymentRequestStrategyTest` cobre o `throw` de `parsePedido`; o JaCoCo mostra a classe em **8/8 branches** e **49/49 linhas**.
6. O status registra que o `throw` é **inalcançável pelo caminho do dispatcher**, com a razão (mesmo `PEDIDO_PATTERN` em `supports()` e em `parsePedido`), e que o teste é de contrato do método público.
7. `testes_novos` no frontmatter bate com o número real de métodos `@Test` adicionados, e o diff **não contém nenhuma linha de `src/main/`**.
8. **Gate de mutação satisfeito:** `test strength` ≥ **80%** sobre `LegendaParser` e `PaymentRequestStrategy`. Esperado: 7/8 e 11/11, ou seja 18/19 ≈ 94,7% — 100% descontando o equivalente demonstrado. Se o número real vier abaixo de 80%, **para e reporta**; não escreve teste extra fora do escopo para levantar a métrica.
9. Nenhum número novo contradiz um número já congelado pelas QA-012/013/014 sem explicação escrita da divergência.

---

## Fora de escopo (explicitamente)

| Item | Por que fica de fora |
|---|---|
| `LegendaParser:28` — mutante `pos <= posicaoMaisCedo` | **Equivalente demonstrado** na QA-012: duas chaves distintas só teriam o mesmo índice se uma fosse prefixo da outra, e nenhuma de `boleto`/`pix`/`ted`/`agendamento` é. Não existe entrada que o mate |
| `MetaSignatureValidator:64` — `bytes.length * 2` | Equivalente de manual — é capacidade inicial de `StringBuilder`, indistinguível pelo contrato público |
| `MetaSignatureValidator:28` — warn de `app-secret` | Exige infraestrutura de captura de appender de log, que o repo não tem. Débito registrado |
| `MetaSignatureValidator:57-59` — `catch` sem cobertura | Exigiria injetar provider JCE falso. `NO_COVERAGE`, não sobrevivente — diagnóstico diferente |
| `FecharMesServiceImpl` — 5 guards de `null` (débito 1 da QA-014) | Classe fora do conjunto que a feature do experimento toca; não pressiona a tag do marco zero. Fica no registro de débitos |
| `GlobalWhatsAppExceptionHandler` — 0/18 branches | Severidade **indeterminada** até a cobertura da suíte de integração ser medida. Agir sem esse dado é chutar |
| `toLowerCase()` sem `Locale` em `LegendaParser:21` | É **código de produção** e pertence ao débito de locale da QA-013, que tem 5 ocorrências e precisa de tratamento único. Corrigir aqui contaminaria a comparação antes/depois do PIT |
| Ampliar `targetClasses` do PIT | O escopo de 4 classes é o baseline congelado. Ampliar é decisão do humano, sugerida pelo Reviewer da QA-012 e ainda não tomada |

---

## Riscos & mitigações

| Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|
| Escopo crescer com o relatório do PIT aberto | **Alta** | Alto | Os alvos estão nomeados; a tabela de §Fora de escopo dá o motivo de cada exclusão. Reviewer confere o diff contra ela |
| O "depois" ser comparado contra o "antes" da QA-012, medido em JVM diferente | Média | Médio | Critério 3 exige remedir o "antes" na mesma máquina e registrar a JVM |
| Descrever o teste do `throw` como cenário de usuário | Média | Médio | Critério 6 exige a declaração de inalcançabilidade. É o mesmo tipo de afirmação não sustentada que derrubou a rodada 1 da QA-014 |
| Perseguir o equivalente para "fechar em 8/8" | Média | Alto | Critério 4 torna a sobrevivência dele um **resultado esperado**, não uma falha |
| `testes_novos` reportado errado | Baixa | Baixo | Critério 7, conferível no diff |
| Teste novo falhar contra o código atual | Baixa | **Alto se mal tratado** | Se acontecer, é bug de produção descoberto: **para, registra, reporta**. Não corrige nesta task |

---

## Coordenação

- **Lane:** `backend`. Arquitetura: **hexagonal** (`financas_bot_telegram/CLAUDE.md`). A task vive inteira em `src/test/`.
- **Depende de:** QA-012 (números do PIT) e QA-014 (números do JaCoCo), **ambas em `develop`**.
- **Bloqueia:** a tag do marco zero do experimento. Enquanto não fechar, a tag não deve ser criada.
- **Não rodar em paralelo com** nada que toque `LegendaParser`, `PaymentRequestStrategy` ou o ferramental do `pom.xml`.
- **Atenção pro Reviewer:**
  - **Reproduzir o PIT** e conferir o 7/8 no relatório. O número é o entregável; afirmação sem relatório não passa.
  - Conferir que o diff **não tem linha de `src/main/`**.
  - Conferir que o mutante equivalente continua vivo e **declarado** — se ele morreu, alguma coisa mudou que não deveria ter mudado.
  - Cobrar a declaração de inalcançabilidade do `throw`. É a afirmação mais fácil de exagerar nesta task.
  - `mutation_gate: true` — **esta é a primeira task do repositório sob o gate.** Verificar o cálculo do `test strength` (mortos ÷ **cobertos**, não ÷ gerados) e que o denominador contém só as duas classes alteradas.
  - §"Verificação de carregamento de skills" **não é critério de aceitação** — conferir que a seção existe no status e responde aos três pontos, e **não** reprovar a task pelo conteúdo da resposta. "Não consegui resolver o path" é resultado válido. O que reprova é a seção faltar ou responder de forma vaga ("as skills funcionaram bem") em vez de dizer o path usado.
- **Atenção pro QA:** `fluxos_qa: []`, **`qa_required: false`** — a task não altera comportamento de produto e seu critério de aceitação já é um número medido que o Reviewer reproduz. Acionar o QA duplicaria a mesma verificação.
- **Após merge:** fechar o item **#2** no `backlog-s04.md` com os números; atualizar o baseline de `LegendaParser` no `STATE.md` (de 6/8 para 7/8); registrar no `pendencias-tecnicas.md` qualquer achado novo.

  > ❌ **ERRATA — 2026-08-16, pós-execução.** Este item dizia que "o teto de 39/42 das quatro classes muda para 40/42". **Está errado e não deve ser executado.** O `39/42` do `STATE.md:31` é **teto**, não score: `42 − 2 equivalentes demonstrados − 1 inalcançável na prática`. O mutante que esta task matou **já estava contado como matável dentro dos 39** — matá-lo aproxima o score do teto, não eleva o teto. O que muda é o **score agregado: 37/42 → 38/42**. Derrubado pelo Reviewer (achado **M1** da QA-015) e verificado no run: dos 4 não-mortos, dois são equivalentes, um é `NO_COVERAGE` inalcançável, e sobra `MetaSignatureValidator:28` como o único matável ainda vivo — exatamente o `38 + 1 = 39`. Mantido aqui riscado, e não apagado, porque o erro chegou a ser copiado para a primeira versão do status: quem reler o plano precisa ver que ele foi a origem.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido conforme `_TEMPLATE-status.md`, e **revisão do Reviewer** (sessão separada — ADR 0005). `fluxos_qa: []`, logo **sem gate de QA**. **`mutation_gate: true`** — `test strength` ≥ 80% sobre `LegendaParser` e `PaymentRequestStrategy`, conforme ADR 0021. `cobertura_pct` deve trazer número real, não `na`: a task toca classes de produção (pela via dos testes) e o JaCoCo está instalado. Implementador abre PR da feature para `integration/04-instrumentacao-qualidade`; **não** abre PR direto para `develop`.

---

## Referências

- `docs/sprints/04-instrumentacao-qualidade/backlog-s04.md` — item **#2** (este), **#1** (origem dos números), **#6** (débito 2).
- `docs/sprints/04-instrumentacao-qualidade/status/QA-012-piloto-pit-mutation-testing.md` — §"Leitura interpretada", sobreviventes nº 1 e nº 2; §"Padrão a replicar".
- `docs/sprints/04-instrumentacao-qualidade/status/QA-014-piloto-jacoco-cobertura.md` — tabela de débitos, item 2; §"Branch × linha".
- `docs/decisions/0021-gate-de-mutation-testing-opcional-por-task.md` — critério do gate.
- `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` §Camada 1.5 (PIT) e §Camada 1.6 (JaCoCo).
- `docs/aprendizado/teste-mutante-e-pit.md` — conceito de mutante equivalente e por que 100% não é meta.
- `docs/decisions/0022-skills-em-subagentes-via-de-entrega-declarada.md` e `docs/aprendizado/skills-em-subagentes-preload-vs-sob-demanda.md` — origem da §"Verificação de carregamento de skills" e do risco de resolução de path dos assets.
