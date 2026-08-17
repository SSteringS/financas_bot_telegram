# Backlog — Sprint 04 (instrumentação e qualidade)

> **Backlog vivo.** Itens entram aqui conforme o humano revisa os docs do experimento.
>
> Objetivo, fluxo de git e decisões pendentes da sprint moram em [`README.md`](README.md) — **não duplicar aqui**. Este arquivo guarda o detalhamento dos itens até virarem planos em `plans/`.
>
> **Origem:** revisão das métricas do experimento em [`../../experiments/models-claude-experiment/`](../../experiments/models-claude-experiment/).

## Por que esta sprint existe

A revisão das métricas mostrou que a maior parte do que o experimento exige **não existe no repositório**: ferramental de qualidade (JaCoCo, PIT, PMD), instrumentação de custo por papel, e correção da configuração dos subagentes. Sem isso, os runs medem a coisa errada ou não medem nada.

Objetivo secundário, declarado pelo humano: **várias dessas ferramentas serão de primeiro uso.** As tarefas devem produzir entendimento, não só artefato. Onde couber, o entregável inclui leitura interpretada do resultado, não apenas o resultado.

---

## 1. Piloto do PIT — mutation testing em escopo reduzido

> ✅ **Refinado como [`QA-012`](plans/QA-012-piloto-pit-mutation-testing.md)** em 2026-08-10. O plano é a fonte da verdade; o detalhamento abaixo fica como registro da origem.

**Origem:** `04-metricas.md` Q3 · `05-instrumentacao-e-harness.md` §5 (PIT não existe no `pom.xml`).

**Escopo — quatro classes, todas com teste unitário existente, sem Spring e sem Testcontainers:**

| Classe | Motivo |
|---|---|
| `domain/service/LegendaParser` | regex puro e pequeno — o relatório mais legível para uma primeira leitura |
| `application/strategy/PaymentRequestStrategy` | `supports()` com regex + construção do pedido |
| `application/strategy/PaymentProofStrategy` | par da anterior, permite comparar duas leituras |
| `adapters/in/whatsapp/security/MetaSignatureValidator` | lógica de segurança com comparação e early-return; mutantes de fronteira instrutivos |

As três primeiras são **exatamente o código que a feature Baixa do experimento vai tocar** — o baseline de mutation score delas vira dado do experimento.

**Entrega:**

1. `pitest-maven` + `pitest-junit5-plugin` no `pom.xml` do backend.
2. `targetClasses` restrito às quatro classes e `excludedTestClasses` cobrindo `*IntegrationTest` — **exclusão obrigatória**, não otimização: o PIT reexecuta a suíte relevante uma vez por mutante, e Testcontainers subindo MySQL real torna o run inviável.
3. Relatório HTML + XML gerado e commitado (ou o caminho documentado, se pesado demais).
4. **Leitura interpretada** — para cada mutante `SURVIVED`, classificar em (a) lacuna real de asserção, (b) mutante equivalente, (c) ruído, com uma frase de justificativa.
5. Registro do **mutation score e do test strength** por classe, como baseline.
6. Comando documentado no runbook de testes.

**Não faz:** corrigir os testes fracos que o relatório apontar — ver #2.

**Critério de aprendizado:** ao fim deve estar claro, no relatório da task, por que cobertura e mutation score divergem, com exemplo concreto tirado do próprio código do projeto.

**Referência conceitual:** [`../../aprendizado/teste-mutante-e-pit.md`](../../aprendizado/teste-mutante-e-pit.md)

---

## 2. ~~Corrigir os testes fracos revelados pelo piloto~~ — ✅ **CONCLUÍDO**

> ✅ **ENTREGUE pela [`QA-015`](plans/QA-015-fortalecer-testes-revelados-pelos-pilotos.md)** — mergeada em 2026-08-16 (PR #133 → integration, #134 → develop). Reviewer: `approved-with-notes`, zero achado `critical`/`high`, nenhuma remediação de código. [Status](status/QA-015-fortalecer-testes-revelados-pelos-pilotos.md) · [Revisão](avaliacoes/review-QA-015-fortalecer-testes-revelados-pelos-pilotos.md).
>
> **Números finais, reproduzidos pelo Reviewer:**
>
> | Alvo | Antes | Depois |
> |---|---|---|
> | `LegendaParser` — PIT | 6/8 = 75% | **7/8 = 87,5%** |
> | `PaymentRequestStrategy` — JaCoCo branch | 7/8 = 88% | **8/8 = 100%** (linha 49/49) |
> | Gate de mutação (ADR 0021) | — | **94,7%** de `test strength` ≥ 80% ✅ |
>
> O sobrevivente restante de `LegendaParser:28` **continua vivo e é o resultado esperado** — equivalente com demonstração escrita. **7/8 é teto, não pendência:** nenhum reforço futuro de teste leva a 8/8, e uma task que prometa isso está prometendo o impossível.
>
> ⚠️ **Errata sobre o número agregado.** O plano mandava elevar o teto do `STATE.md` de 39/42 para 40/42. **Errado** — o `39/42` é teto (`42 − 2 equivalentes − 1 inalcançável`) e **não muda**; o mutante morto já estava contado dentro dele. O que mudou é o **score: 37/42 → 38/42**. Derrubado pelo Reviewer (M1); errata registrada no plano.
>
> **Dois achados que valem mais que os números** (detalhe em §Débitos de `pendencias-tecnicas.md`): (a) o PIT **não se moveu** no alvo 2 — ele dava 11/11 antes de existir teste que atingisse o `throw`, porque nenhum mutante é gerado na construção da exceção; foi o **JaCoCo** que achou o buraco, o espelho exato do caso `LegendaParser`, onde foi o PIT. Nenhuma das duas ferramentas domina a outra, e agora há dado deste projeto nas duas direções. (b) O `throw` de `parsePedido` é **inalcançável pelo dispatcher**, o que **superestimava** a severidade do débito 2 da QA-014.
>
> **A tag do marco zero está desbloqueada** — as duas classes que a feature do experimento toca estão no estado final.
>
> Registro da origem abaixo.
>
> **Escopo fechado com o humano — dois alvos:** (1) `LegendaParser`, caso com palavra-chave no **índice 0**, matando o sobrevivente nº 1 da QA-012 (6/8 → **7/8**); (2) `PaymentRequestStrategy`, o `throw` de `parsePedido` que a QA-014 registrou como débito 2 (branch 7/8 → **8/8**).
>
> **Primeira task do repositório com `mutation_gate: true`** — adotado por decisão explícita do humano. Piso de 80% de `test strength` sobre as duas classes, conforme ADR 0021.
>
> **Ordem em relação ao marco zero: resolvida.** O humano autorizou esta task **antes** da tag.

**Depende de:** #1. Escopo definido pelo resultado do piloto.

**Atenção:** melhorar os testes de `LegendaParser` e das strategies **antes do congelamento do baseline** é desejável — todos os runs partem do mesmo ponto, e um baseline melhor mede melhor. Mas precisa acontecer **antes** da tag do marco zero, nunca no meio do experimento.

---

## 3. Coletar `test strength` além do mutation score

O Q3 sozinho mistura dois diagnósticos: score baixo por `NO_COVERAGE` significa "não testou"; score baixo com cobertura alta significa "testou sem verificar". Para avaliar qualidade de teste gerado por modelo, o segundo é o sinal que interessa.

O PIT já reporta as duas. **Decidir se `test strength` vira métrica de primeira classe no pré-registro** ou fica como secundária.

---

## 4. PMD — ruleset e escopo, sem Checkstyle

> ✅ **Concluído como [`QA-013`](plans/QA-013-pmd-ruleset-curado-e-piloto.md)** em 2026-08-12 (PRs #127 e #128). **Absorveu o item #5** — a curadoria de ruleset é empírica e não fechava sem o piloto. Reviewer: `aprovado_com_observacoes`, 2 rodadas.
>
> **Entregue:** `maven-pmd-plugin` fora do ciclo de vida, `pmd-ruleset.xml` com 10 regras justificadas uma a uma, baseline do legado e leitura interpretada das 8 violações do piloto.
>
> **Baseline congelado — só vale junto com o hash:**
> ```
> Q7_producao = 22    Q7_teste = 1
> sha256 = 5100b68f387a0f0d4a8a6d8ba6540c715854709869790373df1118acb71755a9
> ```
> **Δ medido contra outro ruleset não é comparável.**
>
> O detalhamento abaixo fica como registro da origem. Débitos em [`pendencias-tecnicas.md`](pendencias-tecnicas.md).

**Origem:** `04-metricas.md` Q6 e Q7 · `05-instrumentacao-e-harness.md` §5 (PMD não existe).

**Decidido com o humano em 2026-08-10: instalar apenas PMD, não instalar Checkstyle.**

1. **Sobreposição** — as duas cobrem território comum e podem discordar; sem curar rulesets, o Q7 conta a mesma violação duas vezes.
2. **O alvo é qualidade, não formatação** — modelo de linguagem formata bem; nome de variável e indentação não discriminam. O que discrimina é `catch` vazio, método inchado, duplicação: território do PMD.
3. **Uma ferramenta a menos** num objetivo declarado de aprendizado detalhado.

**Decisões da task:**

- Subconjunto inicial de regras. O default do PMD é barulhento; `errorprone` e `design` são as categorias que mais dizem sobre qualidade. Escolher regra a regra é parte do trabalho.
- **Escopo por diff, não projeto inteiro** — consome o mecanismo do item #7.
- **Não bloqueante no CI durante o experimento.** Build que falha por violação faz o agente otimizar para o linter, interferindo no que está sendo medido. Relatório sim, gate não.
- **Ruleset congelado com hash no pré-registro** antes do primeiro run.

**Impacto no workflow:** o `PRE-MERGE-CHECKLIST` declara hoje `lint: na` como oficial para o backend. Instalar PMD muda isso — exige decidir se passa a existir gate de lint e em que termos.

**Limitação a declarar no artigo:** complexidade ciclomática não distingue `switch` de 10 casos (alto, legível) de aninhamento de 4 níveis (parecido, ilegível).

**Referência conceitual:** [`../../aprendizado/analise-estatica-pmd-checkstyle.md`](../../aprendizado/analise-estatica-pmd-checkstyle.md)

---

## 5. ~~Piloto do PMD — escopo reduzido~~ — absorvido pelo #4

> ⬛ **Não vira task própria.** Absorvido pela [`QA-013`](plans/QA-013-pmd-ruleset-curado-e-piloto.md) por decisão do humano em 2026-08-12.
>
> **Por que a separação não se sustentava:** a curadoria de ruleset é **empírica** — não se escolhe regra a regra sem ver o que cada uma dispara no código real —, e este item declarava que a classificação "(c) regra que não queremos" alimenta a curadoria do #4. O #4 dependia de um output que só o #5 produzia.
>
> **Executado dentro da QA-013:** as 4 classes do piloto do PIT mais `FecharMesServiceImpl` como contraste, com as 8 violações classificadas em (a)/(b)/(c) e justificativa por violação. A curadoria levou **2 rodadas** até o ruleset estabilizar.
>
> Resultado da classificação: **2 problemas reais** (bugs de locale), o resto falso positivo ou regra descartada. Os dois `(a)` estão no registro global de pendências.

---

## 6. Piloto do JaCoCo — escopo reduzido

> ✅ **Concluído como [`QA-014`](plans/QA-014-piloto-jacoco-cobertura.md)** em 2026-08-13 (PRs #130 e #131). Reviewer: **2 rodadas** — rodada 1 `rejected` por premissa falha, rodada 2 aprovada após 6 correções. Nenhum número precisou ser refeito.
>
> **O texto deste item estava correto e não foi alterado.** O plano da QA-014 afirmou que ele registrava um fato errado; **a afirmação do plano é que era falsa** — o `.codex/agents/qa-test-specialist.toml` instruía `./mvnw jacoco:report` mesmo (arquivo deletado pelo humano em 2026-08-13, depois do fato). Plano retratado; este item fecha como foi escrito.
>
> **Baseline do projeto — unit-only, com `lombok.config`:**
> ```
> Linha 1681/1864 = 90,2%    Branch 326/430 = 75,8%
> Instrução 89,5%  ·  Método 87,5%  ·  Classe 94,6%  ·  Complexidade 78,5%
> 374 testes executados (dos 422 da suíte completa) · 148 classes
> ```
> ⚠️ **Subestima a cobertura real** — a suíte de integração está fora do recorte, e isso é definição da métrica, não rodapé.
>
> **O achado que mais ensinou:** o Lombok inflava o denominador em 604 métodos e 128 desvios, e **126 dos 128 desvios estavam descobertos** — 55% de toda a "cobertura de branch faltante" do projeto era `equals`/`hashCode` gerado. E a cobertura de **linha mal se moveu** (+0,9 pp) enquanto a de **branch subiu 17 pp**. Primeiro argumento com dado deste projeto para **nunca reportar cobertura de linha sozinha**.
>
> Débitos em [`pendencias-tecnicas.md`](pendencias-tecnicas.md).

**Escopo sugerido:** as mesmas quatro classes do piloto do PIT — assim os três relatórios (cobertura, mutation score, violações) descrevem **o mesmo código**, e dá para comparar o que cada ferramenta enxerga e o que nenhuma enxerga.

**Entregável:** relatório + leitura comparada. O ponto pedagógico é encontrar, no código do próprio projeto, **uma classe com cobertura alta e mutation score baixo** — a demonstração concreta de por que cobertura sozinha engana. Se nenhuma das quatro exibir o padrão, ampliar o escopo até encontrar uma que exiba.

**Nota:** JaCoCo é pré-requisito de Q2, e o comando `jacoco:report` que o agente QA é instruído a rodar **falha hoje**. Este piloto resolve a lacuna e destrava `cobertura_pct` nos status reports.

---

## 7. Mecanismo de "classes tocadas" — escopo por diff compartilhado

**Origem:** revisão de métricas, 2026-08-10. **Decisões fechadas com o humano na mesma data.**

**Problema:** quatro métricas são definidas sobre o conjunto alterado — **Q2** (cobertura), **Q3** (mutation score), **Q6** (complexidade), **Q7** (violações) — e **nenhuma tem mecanismo**. Infraestrutura única, não três soluções paralelas.

### Decisões

| # | Decisão |
|---|---|
| **D1** | Base de comparação = **a tag do marco zero, fixa**. Não `develop`, não merge-base. Todo run parte do mesmo commit e nenhum branch é mergeado, então `git diff <TAG_BASELINE>..HEAD` é estável e idêntico entre runs |
| **D2** | Granularidade = **classe**, para todas as métricas. Custo aceito: classe alterada em uma linha entra inteira |
| **D3** | Medir **produção e teste separadamente**, conforme a tabela abaixo |
| **D4** | **PIT restringe na entrada** (`targetClasses`) — obrigatório, mutation testing no projeto inteiro é inviável. **JaCoCo e PMD rodam completo e filtram na saída** — são rápidos, e assim existe **uma implementação de filtro só** |
| **D5** | Durante o run, os artefatos são escritos em **`docs/runs/<RUN_ID>/`** — caminho neutro, fora da pasta do experimento. **Ao fim do experimento**, arquivar em `docs/experiments/models-claude-experiment/runs/`. Evita que o agente trabalhe dentro da pasta que contém o desenho experimental |
| **D6** | Classe deletada = **não entra**. Classe renomeada = **entra**. Arquivo tocado só em import ou comentário = **entra**. Run sem nenhuma classe de produção = valor **`na`**, nunca `0` |

### D3 — o que se mede onde

| Métrica | Produção | Teste | Motivo |
|---|---|---|---|
| Q2 cobertura | sim | não | teste executa a si mesmo 100%; a medida não significa nada |
| Q3 mutation score | sim | não | o PIT muta produção e verifica se o teste mata |
| Q6 complexidade | sim | **sim** | método de teste com complexidade alta é teste ruim |
| Q7 violações PMD | sim | **sim** | teste com `catch` vazio, duplicação ou método gigante é teste ruim |

Reportar como `Q6_producao` / `Q6_teste` e `Q7_producao` / `Q7_teste`. **Num número só, um dilui o outro.**

Justificativa: o failure mode conhecido de LLM é escrever teste fraco. Medindo só produção, o único sinal seria o Q3, indireto.

### D6 — por que `na` e não `0`

Run sem classe de produção tocada torna o mutation score `0 ÷ 0`, indefinido. Gravar `0` produz três estragos: o run parece ter qualidade péssima quando não foi medível; a média entre runs é puxada para baixo; e ninguém distingue "não matou nenhum mutante" de "não havia mutante". O sentinela `na` **já é convenção do repositório**.

### Regra de leitura conjunta — obrigatória

**Q3, Q6 e Q7 não podem ser lidos isoladamente.**

Runs diferentes tocam conjuntos diferentes de classes, logo têm denominadores diferentes. Sem essa regra, cria-se incentivo perverso: um run que escreve **uma classe pequena e bem testada, deixando a feature pela metade**, pontua melhor que um run que entrega tudo.

Portanto: **Q3, Q6 e Q7 só são interpretados junto com Q4** (critérios de aceitação satisfeitos) **e Q5** (tamanho do diff). Run que falha Q4 sai da comparação de qualidade ou entra sinalizado.

**Ação:** emendar no `04-metricas.md` e incluir no pré-registro — **não como nota de rodapé no artigo**.

### Premissas medidas pelos pilotos — ler antes de estimar

Três fatos que os pilotos do PIT e do PMD produziram e que **mudam o desenho deste item**:

1. **O piso de custo do PIT é a suíte unitária inteira, não o tamanho de `targetClasses`** (QA-012). A fase de cobertura roda as 70 classes de teste uma vez antes de mutar qualquer coisa — 22s dos 48s do piloto. **Restringir `targetClasses` reduz a fase de mutação, não a de cobertura.** Mitigação a avaliar: `targetTests` explícito, e/ou `historyInputLocation`.

2. **Regras sensíveis a resolução de tipo mudam de resultado com o estado do build** (QA-013, medido pelo Reviewer). `LawOfDemeter` deu **2** violações sem `target/classes` e **26** com. O ruleset congelado é **insensível** — 22 nos dois casos, verificado —, mas isso é propriedade deste ruleset, não do PMD. **O Δ precisa fixar e declarar se mede com o projeto compilado.**

3. **`-Dpmd.rulesets` não existe** (QA-013). Trocar de ruleset exige editar o `pom.xml` — não é flag de linha de comando, e a edição muda o hash. O Reviewer precisou montar projetos-cópia para reproduzir números com ruleset diferente.

**Baseline já congelado, disponível para o Δ:**

```
Q7_producao = 22    Q7_teste = 1
sha256(pmd-ruleset.xml) = 5100b68f387a0f0d4a8a6d8ba6540c715854709869790373df1118acb71755a9
```

⚠️ **Não silenciar violação com `@SuppressWarnings("PMD…")`** — supressão espalhada pelo código torna o `Q7` incomparável entre runs **sem deixar rastro no hash**.

### Entrega

1. Script único que produz a **lista canônica** de classes tocadas a partir de `git diff <TAG_BASELINE>..HEAD`, separando produção de teste.
2. Lista **persistida por run**, para que a análise seja reproduzível meses depois sem re-rodar git, e para que o auditor cego confira que a métrica foi calculada sobre o conjunto declarado.
3. Integração com as três ferramentas conforme D4.
4. Tratamento dos casos-limite de D6, com `na` propagado até a tabela final.

---

## 8. Gate que força a convenção `*IntegrationTest`

**Origem:** QA-012, débito #3 do implementador — endossado pelo Reviewer como o de maior prioridade da task. Decidido com o humano em 2026-08-10 que vira task de código.

**Problema:** o `excludedTestClasses` do PIT filtra por `*IntegrationTest`, e essa exclusão é **requisito de viabilidade** (Testcontainers × 1 container por mutante = run que não termina). Hoje **nada força a convenção** — nem lint, nem gate de CI. Um teste de integração novo com outro sufixo escapa do filtro, e a falha só aparece quando alguém rodar o PIT, possivelmente meses depois.

Isso torna todo o ferramental de mutation testing da sprint dependente de uma convenção não verificada.

**Entrega:** teste que varre o classpath de teste e **falha** quando uma classe anotada com `@Testcontainers` — ou que estenda `AbstractIntegrationTest` — não termina em `IntegrationTest`. Rodando dentro do `mvn test`, já cai no CI sem tooling novo.

**Decisão de implementação a fechar no planejamento — ArchUnit vs. reflection puro:**

| | ArchUnit | Reflection puro |
|---|---|---|
| Dependência | nova (`archunit-junit5`, escopo `test`) | nenhuma |
| Tamanho | ~10 linhas declarativas | ~30 linhas imperativas |
| Mensagem de erro | boa por padrão (lista as classes violadoras) | por conta de quem escreve |
| Extensibilidade | alta — o repo tem outros invariantes candidatos (JPA fora do domínio, adapter não conhece adapter, `usecases/` legado sem código novo) | nenhuma; resolve só este caso |
| Risco | uma dependência a mais no build durante o experimento | reimplementar varredura de classpath, que é onde mora o bug chato |

**Ponto de decisão real:** se o objetivo for só este invariante, reflection puro basta. Se o repo pretende verificar as convenções que hoje só vivem em prosa no `financas_bot_telegram/CLAUDE.md` (JPA vive no adapter, adapters não se conhecem, nada novo em `usecases/`), ArchUnit paga a dependência com folga — **e é a única das duas que escala para isso**. Levar as duas opções ao humano no planejamento, com essa pergunta explícita.

**Não faz:** verificar as demais convenções nesta task. Se ArchUnit for escolhido, elas viram itens separados — o gate da convenção de nome é o que destrava o PIT e não deve esperar pela discussão dos outros invariantes.

**Critério de aceitação:** um teste renomeado de propósito para violar a regra faz o `mvn test` falhar, com mensagem que nomeia a classe violadora.

**Regra já declarada (mas não verificada):** `financas_bot_telegram/CLAUDE.md` §"Convenções que valem hoje" · `docs/PENDENCIAS-TECNICAS.md` §"Convenção `*IntegrationTest` não é verificada por nada".

---

## 9. Mensagem de erro morta em `PaymentRequestStrategy`

**Origem:** planejamento da QA-015, 2026-08-16. Descoberto ao verificar o débito 2 da QA-014 no código, em vez de aceitar o relatório da ferramenta.

**O fato, verificado por leitura de `MensagemEntranteService:57-73` e `PaymentRequestStrategy:44-98`:** `supports()` (linha 48) e `parsePedido` (linha 87) aplicam **o mesmo `PEDIDO_PATTERN`, sobre o mesmo `caption.trim()`, do mesmo `dto`**, e nada modifica o DTO entre as duas chamadas. O dispatcher só chama `process()` depois de `supports()` devolver `true` — logo `!matcher.matches()` nunca é verdadeiro em produção e o `throw` da linha 88 é **inalcançável pelo caminho real**.

**A consequência que interessa:** a mensagem que o usuário vê quando manda legenda malformada vem do `orElseThrow` do dispatcher (`MensagemEntranteService:60-63`, constante `ERROR_MESSAGE`) — genérica, cobrindo pedido e comprovante. A mensagem que está dentro do `parsePedido`, com **cinco exemplos** e a explicação de que o tipo é detectado pela palavra-chave, **nunca chegou a ninguém**. Alguém escreveu uma mensagem de erro melhor que a de produção e ela ficou num ramo morto.

⚠️ **A QA-014 registrou este débito com a justificativa "é a mensagem de erro que o usuário final vê". Isso é falso**, e o registro fica aqui para corrigir o dado, não para culpar a task — a QA-014 tinha proibição de escrever teste e leu o relatório do JaCoCo, que não tem como enxergar alcançabilidade entre classes.

**Decisão de produto embutida, a tomar:** ou a mensagem boa sobe para o dispatcher (e aí o usuário passa a ver os exemplos), ou o ramo defensivo vira uma exceção seca declarando que é defesa e não caminho de usuário. **Não** é para simplesmente apagar a validação redundante — defesa em método público de `@Component` é razoável.

**Severidade:** baixa em risco, média em valor de produto. **Território:** código de produção.

---

## 10. Upload ao S3 antes da validação, e dentro da transação

**Origem:** planejamento da QA-015, 2026-08-16, mesma leitura de código do item #9.

**A ordem em `PaymentRequestStrategy.process`:** guarda de `fileBytes` (56) → **upload ao S3** (61) → `parsePedido`, que valida (65) → persistência (69).

Pelo caminho da legenda inválida não vaza nada hoje, porque o `throw` é inalcançável (item #9). **Mas existe um caminho alcançável agora:** `MensagemEntranteService.processar` é `@Transactional` (linha 48) e o upload acontece dentro dessa transação. Se `salvarPedidoPagamentoUsecase.execute` falhar — constraint, banco fora, qualquer exceção —, o rollback desfaz o insert e **não desfaz o objeto no S3**. Não há compensação. Sobra arquivo órfão no bucket, sem pedido apontando para ele e sem nada que o recolha.

É o problema clássico de efeito colateral externo dentro de fronteira transacional: o banco tem rollback, o S3 não.

**Por que nenhuma ferramenta desta sprint pegaria isso:** PIT, PMD e JaCoCo olham uma classe por vez. "Escrita externa não compensada dentro de transação" é propriedade de **relação entre classes** — o mesmo tipo de invariante que o item #8 discute com o ArchUnit, e mais uma evidência a favor dele.

**Antes de dimensionar:** medir se já existem órfãos em `bot-financas-pagamentos-satyan`. O tamanho do problema real é desconhecido — não estimar.

**Caminhos possíveis:** inverter a ordem (validar antes de subir) resolve o caso da legenda; o caso do rollback exige mais — upload após o commit (`@TransactionalEventListener(AFTER_COMMIT)`, padrão que o repo já usa na EVO-02) ou rotina de limpeza de órfãos.

**Severidade:** média. **Território:** código de produção.

---

> 📦 **Sequenciamento resolvido (2026-08-16).** Os itens **#9** e **#10** foram refinados juntos como [`BE-031`](plans/BE-031-validacao-antes-do-upload-e-mensagem-morta.md), **depois** da `QA-015` e não dentro dela: a QA-015 exige zero linha de `src/main/` no diff e seu critério de aceitação é a comparação antes/depois do PIT, que mudança de produção no mesmo commit destrói. O pacote é a **sequência** QA-015 → BE-031, as duas antes da tag do marco zero.
>
> ⚠️ **O `BE-031` fecha o #9 inteiro e o #10 apenas em parte.** Ele conserta o caminho **inalcançável** (legenda inválida não escreve mais no S3) e **não** conserta o alcançável (rollback deixa órfão no bucket) — esse precisa de ADR, porque as três saídas possíveis são todas decisões de arquitetura e o repositório **não tem nenhum delete de S3** hoje.

---

## Itens ainda não detalhados

Levantados na revisão, aguardando o humano chegar neles:

- **⚠️ Revalidar o `05-instrumentacao-e-harness.md` inteiro antes de agir sobre ele.** Verificação em 2026-08-10 mostrou que o doc **está desatualizado**: os arquivos de agente foram renomeados (`planner.agent.md` → `planner.md`), o `model` inválido `GPT-5.4 (copilot)` **não existe mais**, e o formato de `tools` foi corrigido. O doc é de 2026-08-08 e a migração veio depois. Qualquer task derivada dele precisa reconferir o disco primeiro.

- **B1 — pinagem de modelo por papel.** Não resolvido, mas **diferente do documentado**. Estado real em 2026-08-10: `planner: opus` (pinado); `backend`, `reviewer` e `qa-test-specialist`: **`inherit`** — três dos quatro papéis da cadeia herdam o modelo da sessão. O risco original sobrevive: sem pinagem por papel, os runs medem a mesma célula.

- **Territórios de tool por agente — auditar.** Descoberto em 2026-08-10: `planner` não declarava `Bash`, o que contradiz a seção "Acesso ao git pelo Cowork (planner)" do `CLAUDE.md`, que especifica quais comandos git o planner pode rodar. Corrigido no mesmo dia. **Vale auditar os seis agentes** contra o que cada role doc promete — pode haver outras divergências do mesmo tipo.
- **B4 — `effort` explícito** em todos os papéis; hoje herda da sessão e é segunda variável livre.
- **B6 — allowlist de permissões fixada**, para um run não falhar por permissão e outro não.
- **B7 — hooks `SubagentStart`/`SubagentStop`** e `run_events.jsonl`.
- **`collect_usage.py`** + teste de regressão do schema JSONL + verificador de pin.
- **Spec de dispatch** — gerador que remove score e notas metodológicas das specs (`07-prompts-padronizados.md` §5).
- **FIX-006 + deploy `develop → main`** antes da tag do marco zero.
