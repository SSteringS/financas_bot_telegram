---
task: QA-012
sprint: 04-instrumentacao-qualidade
data: 2026-08-10
avaliador: claude-reviewer
status_report: docs/sprints/04-instrumentacao-qualidade/status/QA-012-piloto-pit-mutation-testing.md
veredito_codigo: aprovado_com_observacoes
veredito_final: aprovado_com_observacoes
observacoes_count: 5
roteiro_executado: false
gates_verificados_contra_realidade: ok
skills_eficazes: []
skills_gaps: []
veredito_qa: nao_aplicavel
fluxos_qa_executados: []
fluxos_qa_adicionar: []
fluxos_qa_remover: []
---

# Avaliação — QA-012 (Piloto do PIT — mutation testing em escopo reduzido)

**Branch:** `feature/qa-012-piloto-pit-mutation-testing`
**Implementador:** claude-back
**Plano:** `docs/sprints/04-instrumentacao-qualidade/plans/QA-012-piloto-pit-mutation-testing.md`
**Status report:** `docs/sprints/04-instrumentacao-qualidade/status/QA-012-piloto-pit-mutation-testing.md`
**Modo de revisão:** `full-review`
**Base do diff:** `integration/04-instrumentacao-qualidade` (`c6c9cf2`) → `HEAD` (`1a40e2f`)

---

## 0. Checagens de premissa

| Premissa | Resultado | Como foi checada |
|---|---|---|
| Arquitetura / camadas | `pass` | O diff não toca código de produção nem de teste. Só `pom.xml` (build tooling) e `docs/`. Nenhum dos 4 smells arquiteturais se aplica. |
| Afirmação técnica central ("nenhum teste de integração rodou sob o PIT") | `pass` | Reproduzida por execução própria — ver §2 e §3. Não aceita do relato. |
| Contrato externo | `n/a` | A task não consome nem publica contrato externo. A única dependência de terceiros (PIT 1.25.9 × `pitest-junit5-plugin` 1.2.3, combinação não testada pelo autor do plugin) foi validada empiricamente pelo implementador **e** reproduzida por mim em duas execuções independentes. |
| Independência da evidência em relação ao código sob teste | `pass` | Não usei o log nem os relatórios do implementador. Gerei log verbose próprio (2919 linhas), enumerei as classes de teste a partir dele, e conferi `target/test-classes` no disco de forma independente. |

Nenhuma premissa em `fail`. Nenhuma checagem ficou como `not-checked`.

---

## 1. Análise de código (Reviewer lê o diff)

### Veredito de código: **aprovado com observações**

O diff é pequeno, cirúrgico e faz exatamente o que o plano pediu. Três arquivos, 319 linhas, todas de adição:

- `financas_bot_telegram/pom.xml` — `pitest-maven` + `pitest-junit5-plugin`, versões em `properties`, `targetClasses` com os 4 FQCNs, `excludedTestClasses` = `*IntegrationTest`, `outputFormats` XML+HTML, `timestampedReports=false`. **Nenhum `<execution>`** — o plugin não está amarrado a fase alguma do ciclo de vida.
- `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` — nova "Camada 1.5", encaixada entre a camada 1 (unitários) e a 2 (estática), coerente com a estrutura em camadas do documento.
- Status report novo.

Pontos bem resolvidos, confirmados contra a realidade e não só contra o relato:

- **Custo do build não mudou.** Rodei `./mvnw test` completo: `pitest` aparece **0 vezes** no log. `mvn test` / `package` / CI seguem com o custo de antes. A decisão de deixar o plugin fora do ciclo de vida está certa e está justificada no `pom.xml` e no status report.
- **O comentário no `pom.xml` sobre `excludedTestClasses`** é a coisa mais valiosa do diff. É a linha que impede alguém de "limpar" a configuração no futuro e travar o run. O status report identifica isso corretamente como o débito #3.
- **Relatórios não commitados.** `git check-ignore` confirma: `financas_bot_telegram/.gitignore:2:target/` cobre `target/pit-reports/`. Nada de `pit-reports` no diff.
- **Nenhum teste e nenhuma classe de produção alterados** — o plano proíbe explicitamente e a proibição foi respeitada (`git diff --name-only` retorna exatamente 3 arquivos).
- **Comando documentado funciona.** Não aceitei o comando do runbook como correto: executei **o comando exatamente como documentado**, a partir da raiz do repositório (`./financas_bot_telegram/mvnw org.pitest:pitest-maven:mutationCoverage -f financas_bot_telegram/pom.xml`), que **não** é o comando da tabela de evidência do status report. Rodou até o fim, `BUILD SUCCESS`, mesmos números. Isso importa porque `.mvn/` só existe em `financas_bot_telegram/`, e um wrapper invocado da raiz podia não achar a configuração — não é o caso.

### Observações materiais

**Observação 1 — "Zero ocorrências de `testcontainers`, `mysql` ou `docker` no log" é falso, e se contradiz com a evidência vizinha**

- **O quê:** o status report, na seção "Critério 4", terceiro bullet, afirma: *"**Zero** ocorrências de `testcontainers`, `mysql` ou `docker` (busca case-insensitive) no log."* Na minha reprodução (`-Dverbose=true`, 2919 linhas) a busca case-insensitive por `testcontainer|mysql|docker` retorna **5 linhas**, não zero.
- **Onde:** `docs/.../status/QA-012-piloto-pit-mutation-testing.md`, seção "Critério 4 — nenhum teste de integração rodou sob o PIT".
- **Detalhe:** a linha 105 do log (o dump de `ReportOptions`) contém `testcontainers-1.20.6.jar`, `org/testcontainers/mysql/1.20.4`, `docker-java-api-3.4.1.jar` e `mysql-connector-j-9.1.0.jar` dentro de `classPathElements`. **É a mesmíssima linha** que o bullet anterior do relatório cita como evidência (`excludedTestClasses=[^.*IntegrationTest$]`). As outras 4 linhas são avisos do Hibernate mencionando `MySQLDialect`.
- **Por quê importa:** a conclusão está certa — verifiquei por outro caminho que nenhum teste de integração rodou e nenhum container subiu — mas a evidência, **como escrita**, é falsa e autocontraditória. O status report é o artefato durável; quem reproduzir a busca no futuro vai concluir que o relatório mente, e não vai saber quais dos outros números confiar. O plano marcou o critério 4 como "o erro mais provável e o mais caro"; a evidência dele precisa ser exata.
- **Sugestão:** corrigir o bullet. O enunciado verdadeiro e ainda mais forte é: *"nenhuma linha do log indica atividade de Testcontainers ou de container Docker — as únicas ocorrências dos termos são (a) nomes de JAR no `classPathElements` e (b) avisos do Hibernate sobre `MySQLDialect` vindos de um contexto Spring com H2. Docker estava disponível na máquina, então um vazamento teria executado, não falhado."*

**Observação 2 — um contexto Spring Boot sobe durante o run do PIT, e isso não está registrado em lugar nenhum**

- **O quê:** o log verbose mostra banner do Spring Boot, `SpringBootTestContextBootstrapper`, criação de `SessionFactory` do Hibernate e stack traces de `SchemaDropperImpl.dropConstraintsTablesSequences` com `CommandAcceptanceException: Error executing DDL "alter table auth_token drop foreign key ..." [Table "AUTH_TOKEN" not found]`. O próprio PIT emite, ao fim: `Project uses Spring, but the Arcmutate Spring plugin is not present.`
- **Onde:** log do `mutationCoverage`, linhas ~115–330 e ~1960–2130; mensagem de build na saída final.
- **Diagnóstico (para evitar leitura errada):** isso **não** é vazamento de teste de integração e **não** é uma das 4 classes-alvo puxando Spring. O PIT roda a suíte inteira não-excluída **uma vez** na fase de cobertura — e entre as 70 classes há teste de contexto (`FinancasBotTelegramApplicationTests` e afins) que sobe Spring sobre **H2**, não sobre MySQL/Testcontainers. Viabilidade preservada; custo é a fase de cobertura de ~20 s.
- **Por quê importa:** (a) o plano listou "alguma das quatro classes puxa contexto Spring pelo teste" como risco de probabilidade **média**, e o status report declara "as quatro classes previstas se sustentaram (nenhuma puxou contexto Spring)" sem mencionar que Spring sobe assim mesmo, por outro motivo; (b) os stack traces de DDL no log são ruído fácil de confundir com falha pelo próximo que rodar; (c) tem consequência de custo real para o item #7 do backlog ("classes tocadas"): a fase de cobertura roda a suíte toda **independentemente** do tamanho de `targetClasses`, então o piso de tempo do PIT já é a suíte unitária inteira.
- **Sugestão:** acrescentar duas linhas ao status report distinguindo "fase de cobertura roda toda a suíte não-excluída" de "as classes-alvo puxam Spring", e registrar o piso de custo como débito técnico para o item #7. Não bloqueia.

**Observação 3 — `PaymentProofStrategyTest` não usa `ArgumentCaptor`; a lição "padrão a replicar" está errada em metade da amostra**

- **O quê:** o status report afirma, em "Onde não houve o que interpretar": *"os testes dessas duas classes usam `ArgumentCaptor` e verificam o conteúdo do objeto construído (valor, descrição, status, `requisitanteId`, `dataPedido`, URL do S3)"*, e repete em "Próximos passos": *"os testes de `PaymentRequestStrategy`/`PaymentProofStrategy` mataram 20/20 porque **capturam o objeto construído e verificam campo a campo**"*.
- **Onde:** `financas_bot_telegram/src/test/.../strategy/PaymentProofStrategyTest.java` — **zero** ocorrências de `ArgumentCaptor` (contagem via `grep -c`; `PaymentRequestStrategyTest` tem 3). `PaymentProofStrategyTest` verifica por matchers dentro do `verify`, ex.: `verify(registrarComprovanteUsecase).execute(eq(123L), eq("PIX"), eq("file_xyz"), any(), eq(TipoArquivo.IMAGEM), eq(12345L));` — e não constrói objeto de domínio algum a capturar (a strategy chama um usecase com parâmetros soltos).
- **Por quê importa:** a conclusão de fundo continua verdadeira e é a lição certa — *esses testes verificam o **valor** dos argumentos, não só que o mock foi chamado*. Mas o **mecanismo** atribuído está errado em metade da amostra, e essa é exatamente a passagem que o relatório oferece como insumo do item #2 do backlog. Quem for replicar o padrão vai procurar um captor que não existe naquela classe. O plano diz que o entregável desta task é o entendimento; imprecisão factual justamente aí é a que mais custa.
- **Sugestão:** reescrever para "verificam o valor de cada argumento — `ArgumentCaptor` + asserção campo a campo em `PaymentRequestStrategyTest`, matchers `eq(...)` por argumento em `PaymentProofStrategyTest` — em vez de `verify(mock).metodo(any())`". Correção de texto, sem mexer em código.

**Observação 4 — o rótulo "cobertura baixa" do Caso 2 contradiz os próprios números**

- **O quê:** o Caso 1 rotula `LegendaParser` como "cobertura **alta** escondendo asserção fraca" com **94 %** (17/18); o Caso 2 rotula `PaymentRequestStrategy` como "cobertura **baixa** sem lacuna de asserção" com **96 %** (47/49). A classe rotulada "baixa" tem cobertura **maior** que a rotulada "alta".
- **Onde:** status report, seção "Cobertura × mutation score — por que divergem (critério 7)".
- **Por quê importa:** o conteúdo do Caso 2 está correto e eu o verifiquei linha a linha (as duas linhas não cobertas são **exatamente** 88 e 97, o `throw new InvalidMessageFormatException(...)` de `parsePedido`; e o PIT gera **zero** `NO_COVERAGE` ali, confirmando que mutator default não produz mutante viável sobre `throw` puro). Mas o rótulo enfraquece um argumento que estava certo, e a "régua que fica" no fim da seção é indexada por esses mesmos rótulos.
- **Sugestão:** trocar o título do Caso 2 para algo como "linhas não cobertas **sem** lacuna de asserção" e deixar a comparação de percentuais fora do rótulo.

**Observação 5 — `commits:` no frontmatter lista só um dos dois commits da branch**

- **O quê:** `commits: [a713acb]`. A branch tem dois: `a713acb` (implementação) e `1a40e2f` (`docs(QA-012): registra hash do commit de implementacao no status report`, que trocou `PREENCHER` por `a713acb` — só docs).
- **Por quê importa:** cosmético do ponto de vista de risco, mas o frontmatter é o insumo do painel agregado descrito no `PRE-MERGE-CHECKLIST.md`, e o precedente recente do repo (FIX-007, commit `bcc756b`, "frontmatter com os 3 commits") é listar todos.
- **Sugestão:** acrescentar `1a40e2f` (ou aceitar como convenção de que commits docs-only de fixup não entram — mas então vale escrever a convenção em algum lugar).

---

## 2. Gates verificados contra a realidade

Todos os gates foram **executados**, não lidos.

| Gate | Status diz | Reviewer reproduziu | Divergência? |
|---|---|---|---|
| `build` | ok | ok — `./mvnw -q -DskipTests package`, exit **0** | não |
| `lint` | na | na — backend não tem linter configurado (`PRE-MERGE-CHECKLIST.md`) | não |
| `testes` / `testes_total` | ok / 422 | ok — `./mvnw test`: `Tests run: 422, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS` (49,7 s) | não |
| `testes_novos` | 0 | ok — nenhum arquivo de teste no diff (3 arquivos, nenhum sob `src/test`) | não |
| `cobertura_pct` | na | ok — JaCoCo continua ausente; o `Line Coverage` do relatório é a passada do próprio PIT, restrita às 4 classes. A recusa em usar esse número como `cobertura_pct` está correta | não |
| `branch_convencao` | ok | ok — `feature/qa-012-piloto-pit-mutation-testing` bate `^feature/(be\|fe\|dep\|evo\|ci\|qa)-\d{3}[a-z]?-`; `git merge-base --is-ancestor integration/04-instrumentacao-qualidade HEAD` e `origin/develop HEAD` ambos verdadeiros | não |
| `territorio` | ok | ok — `financas_bot_telegram/pom.xml` + 2 arquivos em `docs/`; nada fora do território de `claude-back` | não |
| `estado: concluido` | — | coerente: todos os gates `ok`/`na`, `pendencias_humano: 0`, `desvios: 1` batendo com a seção em prosa | não |
| Relatórios do PIT fora do diff | — | ok — `git check-ignore -v` → `financas_bot_telegram/.gitignore:2:target/`; `pit-reports` ausente do diff | não |
| Plugin fora do ciclo de vida | — | ok — nenhum `<execution>` no `pom.xml`; `pitest` aparece **0 vezes** no log de `./mvnw test` | não |

`gates_verificados_contra_realidade: ok`.

**Nota lateral (fora do escopo desta task):** a árvore de trabalho tem `.claude/agents/backend.md` e `bash.exe.stackdump` modificados e não commitados. Nenhum dos dois está no diff da branch nem tem relação com QA-012. Registro só para que não sejam atribuídos a esta entrega. (`bash.exe.stackdump` estar versionado apesar de `*.stackdump` no `.gitignore` é higiene de repo pré-existente — assunto do planner, não desta task.)

---

## 3. Reprodução independente do critério 4 (o item que o plano manda checar)

Executei `./mvnw org.pitest:pitest-maven:mutationCoverage -Dverbose=true` do zero, na branch sob revisão, com **Docker disponível na máquina** (`docker info` → server 26.1.4) — ou seja, um vazamento de teste de integração teria **rodado**, não falhado.

**Resultado: critério 4 confirmado.**

1. Extraí do **meu** log todos os FQCNs do pacote do projeto terminados em `Test`/`Tests`: **70 classes distintas**.
2. Contei no disco: `target/test-classes` tem **82** classes de topo, das quais **12** terminam em `IntegrationTest` (`AbstractIntegrationTest`, `AdiantamentoIntegrationTest`, `AuthFlowIntegrationTest`, `FecharMesIntegrationTest`, `FuncionarioCRUDIntegrationTest`, `IsolamentoRequisitanteIntegrationTest`, `MensagemProcessadaIntegrationTest`, `NotificacaoComprovanteListenerIntegrationTest`, `PedidoDetalheIntegrationTest`, `PedidosListIntegrationTest`, `ResumoIntegrationTest`, `ValeIntegrationTest`). 82 − 12 = **70**.
3. Comparei os dois conjuntos elemento a elemento: **as 70 enumeradas são exatamente as 82 menos as 12**. Zero `IntegrationTest` no conjunto enumerado.
4. Nenhuma linha do log indica start de container. As 5 ocorrências de `testcontainers|mysql|docker` são nomes de JAR no `classPathElements` e avisos de `MySQLDialect` do Hibernate sobre H2 (ver Observação 1 e Observação 2).
5. Tempos do meu run: `coverage and dependency analysis: 20 seconds`, total 46 s (relatado: 22 s / 46–48 s — variação normal entre execuções). Só o boot de um MySQL 8 via Testcontainers custaria mais.

**Ressalva declarada pelo implementador sobre o contador `245` — é honesta.** Meu log também traz `Sending 245 test classes to minion` e `>> 245 tests examined`. Também não consegui reconciliar 245 com nenhuma contagem óbvia (191 classes de produção de topo + 82 de teste = 273; menos as 12 = 261; com classes internas, 294 / 282). A declaração "não confirmei o que esse contador conta e não o uso como evidência" está correta, e a decisão de não apoiar a conclusão nele foi a decisão certa.

---

## 4. Veracidade dos números do status report

Reparsei **o meu** `target/pit-reports/mutations.xml` e `index.html`. Todos os números do status report reproduzem.

| Classe | Report: gerados / K / S / NC | Reviewer: gerados / K / S / NC | Report: score / strength / line cov | Reviewer | Confere |
|---|---|---|---|---|---|
| `LegendaParser` | 8 / 6 / 2 / 0 | 8 / 6 / 2 / 0 | 75 % / 75 % / 94 % (17/18) | idem | sim |
| `PaymentRequestStrategy` | 11 / 11 / 0 / 0 | 11 / 11 / 0 / 0 | 100 % / 100 % / 96 % (47/49) | idem | sim |
| `PaymentProofStrategy` | 9 / 9 / 0 / 0 | 9 / 9 / 0 / 0 | 100 % / 100 % / 100 % (33/33) | idem | sim |
| `MetaSignatureValidator` | 14 / 11 / 2 / 1 | 14 / 11 / 2 / 1 | 79 % / 85 % / 90 % (26/29) | idem (11/14 = 78,6 %; 11/13 = 84,6 %, arredondamento do próprio PIT) | sim |
| **Total** | 42 / 37 / 4 / 1 | 42 / 37 / 4 / 1 | 88 % / 90 % / 95 % (123/129) | idem | sim |

Identidade dos 5 achados, extraída do `mutations.xml`:

| Status | Classe | Linha | Método | Mutator | Bate com o relatório? |
|---|---|---|---|---|---|
| SURVIVED | `LegendaParser` | 28 | `parseTipo` | `ConditionalsBoundaryMutator` | sim (survivor nº 1) |
| SURVIVED | `LegendaParser` | 28 | `parseTipo` | `ConditionalsBoundaryMutator` | sim (survivor nº 2) |
| SURVIVED | `MetaSignatureValidator` | 28 | `<init>` | `NegateConditionalsMutator` | sim (survivor nº 3) |
| SURVIVED | `MetaSignatureValidator` | 64 | `bytesToHex` | `MathMutator` (mult → div) | sim (survivor nº 4) |
| NO_COVERAGE | `MetaSignatureValidator` | 59 | `isValid` | `BooleanTrueReturnValsMutator` | sim (item nº 5) |

Linhas não cobertas, extraídas das páginas HTML — todas conferem com o que o relatório afirma:

- `LegendaParser`: **[17]** → `private LegendaParser() {}`, o construtor privado. Relatório: correto.
- `PaymentRequestStrategy`: **[88, 97]** → o `throw new InvalidMessageFormatException(...)` de `parsePedido`. Relatório: correto.
- `MetaSignatureValidator`: **[57, 58, 59]** → o bloco `catch` inteiro. Relatório: correto.
- `PaymentProofStrategy`: **[]**. Relatório: correto.

O `ConditionalsBoundaryMutator` do PIT faz `>=` → `>` e `<` → `<=`. A linha 28 de `LegendaParser` é `if (pos >= 0 && pos < posicaoMaisCedo) {` — logo os dois mutantes de fronteira nessa linha são exatamente `pos > 0` e `pos <= posicaoMaisCedo`, como o relatório identifica. Identificação correta.

---

## 5. Avaliação da leitura interpretada (critérios 6 e 7) — **anotação, não reprovação**

Conforme §Coordenação do plano, esta seção **não** é gate.

### Qualidade da leitura: substantiva, não formalidade

A leitura entrega o que a sprint pediu. Ela não para em "88 % de mutation score": classifica cada sobrevivente, separa `NO_COVERAGE` de `SURVIVED` (distinção que muita gente confunde), explica por que `test strength` desempata para `MetaSignatureValidator`, e transforma um dos achados em insumo acionável e específico para o item #2 do backlog. A seção "Onde não houve o que interpretar" trata 20/20 como achado positivo em vez de vazio — postura correta. A "régua que fica" é reutilizável.

O critério 7 foi cumprido com **três** exemplos concretos das próprias quatro classes, e o Caso 1 é genuinamente o exemplo didático que a ferramenta existe para produzir: 94 % de cobertura de linha, 75 % de mutation score, e os dois sobreviventes numa linha executada por **todos** os nove testes.

### Mas a amostra é fina — e isso é o achado a levar ao humano

Dos 5 itens: 2 são equivalentes demonstrados, 1 é `NO_COVERAGE` inalcançável, 1 é uma linha de log de valor reconhecidamente baixo. Sobra **um único achado acionável** (palavra-chave no índice 0 em `LegendaParser`). É pouco material para uma sprint cujo objetivo declarado é aprendizado — e, como o plano previu, **isso não é culpa do implementador**: ele registrou o fato e parou, sem ampliar escopo por conta própria, exatamente como mandado.

**Sugestão ao humano (a decisão é sua, não minha nem do implementador):** escolher uma segunda leva de `targetClasses` com viés para lógica de decisão de verdade — aritmética, datas, fronteiras, agregação. Candidatos que já têm teste unitário puro no repo e valem a triagem: `ResumoMesServiceImpl`, `FecharMesServiceImpl`, `CadastrarAdiantamentoServiceImpl`, `JwtService`, `Sha256HashService`, `TelegramMessageMapper` / `WhatsAppMessageMapper`. Antes de incluir qualquer uma, conferir que o teste correspondente não sobe contexto Spring nem Testcontainers — a triagem que o próprio status report recomenda.

### Classificação de "mutante equivalente" — **as duas se sustentam**

O plano avisa que "equivalente" é a saída fácil para não admitir lacuna de teste. Verifiquei as duas.

**Equivalente 1 — `LegendaParser:28`, `pos < posicaoMaisCedo` → `pos <= posicaoMaisCedo`: a demonstração se sustenta.**
Original e mutante só divergem quando `pos == posicaoMaisCedo`. Primeira iteração: `posicaoMaisCedo` é `Integer.MAX_VALUE` e `indexOf` devolve no máximo `length - 1` — não diverge. Iterações seguintes: exigiria duas palavras-chave distintas começando no mesmo índice. Correto. Só sugiro **apertar o argumento**: o relatório apoia a conclusão em "as primeiras letras são `b`, `p`, `t`, `a`, todas diferentes". O enunciado geral é mais simples e mais robusto — *duas strings distintas só podem começar no mesmo índice se uma for prefixo da outra*; nenhuma das quatro é prefixo de outra. Mesma conclusão, com um passo a menos de dependência do conteúdo específico das chaves (que pode mudar quando alguém acrescentar uma quinta palavra-chave).

**Equivalente 2 — `MetaSignatureValidator:64`, `bytes.length * 2` → `bytes.length / 2`: se sustenta, é equivalente de manual.**
`MathMutator` sobre a capacidade inicial do `StringBuilder`. Capacidade é dica de alocação: com capacidade menor o buffer realoca e produz **a mesma String**. `bytesToHex` é privado e o contrato público é `isValid` → `boolean`. Acrescento uma verificação que o relatório não fez e que fecha o caso: `bytes.length / 2` nunca é negativo para nenhum `bytes`, então o mutante também não consegue divergir via `NegativeArraySizeException`. É equivalente para toda entrada possível.

**Evidência de que não é saída fácil.** O sinal mais forte não é a qualidade de cada demonstração — é o que o implementador **não** classificou como equivalente:

- O outro sobrevivente da **mesma linha 28** de `LegendaParser` foi classificado como lacuna **real**. Verifiquei: `LegendaParserTest` tem exatamente 9 testes e **nenhum** com palavra-chave no índice 0 — todas as legendas começam por dígito (`"200 pix maria"`, `"150.00 Almoço boleto"`, `"1500 TED construtora silva"`, `"100 BOLETO pix"`, ...). `parseTipo("pix 200")` com o mutante devolveria `OUTRO` em vez de `PIX`. Classificação correta, e o achado é acionável de verdade.
- O sobrevivente do `logger.warn` do construtor foi classificado como lacuna real "de valor baixo" — e **não** escondido atrás de "logging é intestável". A honestidade aqui é o que dá crédito às duas classificações de equivalente.

Sobre o item nº 5 (`NO_COVERAGE` no `catch`): a justificativa está certa na conclusão. Um detalhe de precisão, sem consequência: a inalcançabilidade de `InvalidKeyException` não vem só de "chave HMAC de bytes arbitrários sempre serve" — o caminho degenerado (`app-secret` vazio) morre antes, no construtor de `SecretKeySpec`, com `IllegalArgumentException`, que nem é capturada por esse `catch`. A conclusão "inalcançável sem injetar provider JCE falso" continua válida.

---

## 6. Desvio declarado — avaliação

**Desvio 1 — `<timestampedReports>false</timestampedReports>` acrescentado à configuração enumerada pelo plano. Justificativa se sustenta; aceito.**

Verifiquei o efeito real: o dump de `ReportOptions` do meu run mostra `shouldCreateTimestampedReports=false`, e a saída caiu direto em `target/pit-reports/index.html` — **sem** subpasta de timestamp. O default do `pitest-maven` é `true`; sem esse ajuste, o caminho documentado na Camada 1.5 do runbook estaria errado a cada execução, e cada run acumularia uma pasta nova em `target/`. Confirmei também que não altera **o que** é medido: os 42 mutantes, os 37 mortos, o `NO_COVERAGE` e as linhas cobertas são idênticos aos que o relatório declara. É um desvio de saída, não de medição, foi declarado, e a razão está escrita. Correto ter declarado em vez de silenciar.

---

## 7. Critérios de aceitação do plano — um a um

| # | Critério | Resultado | Verificação |
|---|---|---|---|
| 1 | `pitest-maven` + `pitest-junit5-plugin` no `pom.xml` | **atendido** | 1.25.9 / 1.2.3, versões em `properties`; log confirma `Adding org.pitest:pitest-junit5-plugin to SUT classpath` e `Found shared classpath plugin : JUnit 5 test framework support` |
| 2 | `targetClasses` nas 4 classes; `excludedTestClasses` cobre `*IntegrationTest` | **atendido** | `ReportOptions` do meu run ecoa os 4 FQCNs e `excludedTestClasses=[^.*IntegrationTest$]` |
| 3 | Comando roda até o fim; gera XML + HTML | **atendido** | `BUILD SUCCESS` em 2 execuções independentes (49,3 s e 49,5 s); `mutations.xml` (39.728 bytes) + `index.html` + páginas por pacote/classe |
| 4 | **Nenhum teste de integração executado pelo PIT** | **atendido** | Reproduzido — §3. 70 classes enumeradas = 82 − 12; zero `IntegrationTest`; nenhum container, com Docker disponível |
| 5 | Números por classe no status report | **atendido** | Todos reproduzidos exatamente — §4 |
| 6 | Leitura interpretada, cada `SURVIVED` classificado com justificativa | **atendido** | 4 sobreviventes + 1 `NO_COVERAGE`, um a um, com justificativa. Qualidade avaliada em §5; imprecisão factual na Observação 3 |
| 7 | Cobertura × mutation score com exemplo concreto destas classes | **atendido** | 3 casos, todos com dados reais das 4 classes, todos verificados linha a linha. Rótulo do Caso 2 impreciso — Observação 4 |
| 8 | Comando documentado no `ROTEIRO-TESTES-BACKEND.md` | **atendido** | Camada 1.5 criada; **executei o comando exatamente como documentado**, a partir da raiz — funciona |
| 9 | `mvn test` verde, sem regressão | **atendido** | 422 testes, 0 falhas, `BUILD SUCCESS` |
| 10 | Branch criada a partir de `integration/04-instrumentacao-qualidade` | **atendido** | `merge-base --is-ancestor` verdadeiro; `HEAD` = `1a40e2f`, base = `c6c9cf2` |
| 11 | Território — só `financas_bot_telegram/` e `docs/` | **atendido** | 3 arquivos, nenhum fora |

**11 de 11 atendidos.** Nada em aberto e nada bloqueante.

---

## 8. Validações bloqueadas

Nenhuma. Todos os comandos relevantes (`package`, `test`, `mutationCoverage` em duas variantes de invocação) foram executados nesta máquina, e todos os relatórios foram parseados a partir de artefatos gerados pelo meu próprio run.

---

## 9. Débitos técnicos encontrados (para o planner consolidar)

Os **6 itens já registrados pelo implementador** são legítimos e eu os endosso — em particular o #3 (a exclusão depende inteiramente da convenção de nome `*IntegrationTest`, sem nada que a force) e o #5 (números colhidos em JVM 23-ea enquanto CI/prod usam Temurin 21). Acrescento três:

7. **A fase de cobertura do PIT roda a suíte unitária inteira, independentemente de `targetClasses`.** Piso de custo de ~20 s hoje, e não diminui ao reduzir o escopo de mutação. Relevante para dimensionar o item #7 do backlog ("classes tocadas") — encolher `targetClasses` acelera a fase de mutação, não a de cobertura.
8. **Um contexto Spring Boot (com H2) sobe dentro do run do PIT** e emite stack traces de DDL do Hibernate no log. Ruído que pode ser confundido com falha; e é mais um vetor de lentidão/instabilidade quando o escopo crescer. Ver Observação 2.
9. **Três afirmações do status report precisam de correção textual** (Observações 1, 3 e 4). O status report é o artefato durável desta task — o valor entregue é justamente o registro escrito.

---

## 10. Resultado consolidado

| Item | Resultado |
|---|---|
| Checagens de premissa | 4/4 sem `fail` (1 `n/a`) |
| Análise de código | aprovado com observações |
| Gates contra a realidade | **ok** — todos reproduzidos por execução, zero divergência |
| Critérios de aceitação | 11/11 atendidos |
| Números do status report | **conferem integralmente** contra run independente |
| Critério 4 (o crítico) | **confirmado por reprodução própria**, com Docker disponível |
| Classificação de equivalentes | **ambas se sustentam logicamente** |
| Leitura interpretada (6 e 7) | substantiva; amostra fina — sugestão de nova leva de classes ao humano |
| Desvio declarado (1) | justificado e verificado |
| Roteiro manual | não aplicável — task de ferramental de build, sem comportamento observável por humano |
| **Veredito final** | **aprovado com ressalvas — mergear** |

**Justificativa do veredito.** Tudo o que o plano marcou como crítico foi verificado por execução independente e passou: nenhum teste de integração rodou sob o PIT, os 42 mutantes e os 5 achados reproduzem exatamente, o plugin não encareceu nenhum build, nenhum teste ou classe de produção foi tocado, e os relatórios não estão no diff. As 5 observações são de **precisão do texto do status report** e de **omissão de uma observação do log** — nenhuma altera uma conclusão, nenhuma bloqueia o merge. Como o entregável declarado desta sprint é o entendimento registrado, recomendo que as Observações 1 e 3 (afirmações factualmente falsas) sejam corrigidas no status report antes ou logo após o merge; as Observações 2, 4 e 5 podem ir junto no mesmo commit de texto.

**Nenhuma correção de código é necessária.**

---

## 11. Skills — feedback loop

Sem observação de skills nesta task: o dispatch veio com `skills_dispatched: []` e a task não produz código de produção, então não há sinal de aderência a padrão de código a coletar. `skills_eficazes: []`, `skills_gaps: []`.

---

## 12. Para o planner (próximos passos)

- **Merge:** liberado para PR `feature/qa-012-piloto-pit-mutation-testing` → `integration/04-instrumentacao-qualidade`.
- **Correção de texto no status report** (Observações 1 e 3 são as que importam): não bloqueia o merge, mas o relatório é o artefato durável e hoje contém duas afirmações falsas.
- **Item #2 do backlog** tem alvo concreto e verificado: caso com palavra-chave no índice 0 em `LegendaParserTest`. Após escrever, `LegendaParser` deve ir de 6/8 para 7/8 — os outros dois sobreviventes são equivalentes demonstrados e **não** devem ser perseguidos. O teto realista das 4 classes é 39/42 ≈ 93 %.
- **Decisão do humano — segunda leva de `targetClasses`:** a amostra atual rendeu um único achado acionável. Candidatos triados em §5.
- **Débito #3 do implementador merece prioridade** (`PENDENCIAS-TECNICAS.md`): a viabilidade do PIT depende de uma convenção de nome não verificada por nada. Um teste de integração novo com outro sufixo trava o run.
- **Débitos novos 7, 8 e 9** desta avaliação, para consolidar.
- **Pasta `docs/sprints/04-instrumentacao-qualidade/avaliacoes/` foi criada por esta avaliação** — não existia.

---

## 13. QA — Fluxos automatizados

Não aplicável: task sem fluxos QA definidos (`fluxos_qa: []` no plano, com a justificativa registrada em §Coordenação — a task não altera comportamento de feature e a verificação substantiva é a leitura interpretada, avaliada em §5 desta avaliação). `veredito_qa: nao_aplicavel`.
