---
task: QA-012
titulo: "Piloto do PIT — mutation testing em escopo reduzido"
sprint: 04-instrumentacao-qualidade
data_planejamento: 2026-08-10
branch_alvo: feature/qa-012-piloto-pit-mutation-testing
prioridade: alta
esforco: medio
territorio: back
estado: pronto-pra-execucao
depende_de: []
bloqueia: []
skills_dispatched: []
integration_branch: integration/04-instrumentacao-qualidade
fluxos_qa: []
---

# QA-012 — Piloto do PIT (mutation testing) em escopo reduzido

## Intake

- **Origem:** revisão das métricas do experimento de alocação de modelo (2026-08-10). O `04-metricas.md` define **Q3 = mutation score nas classes tocadas**, coletado por PIT. **PIT não existe no `pom.xml`** (`05-instrumentacao-e-harness.md` §5).
- **Por quê agora:** primeira task da sprint 04. Q3 é a métrica de qualidade de teste do experimento e hoje é incalculável. Além disso, o humano declarou objetivo de **aprender a ferramenta em detalhe** — esta task é o veículo.
- **Esforço:** médio. A instalação é curta; **a leitura interpretada é a maior parte do trabalho** e não deve ser tratada como formalidade.
- **Riscos resumidos:** compatibilidade do PIT com Java 21; tempo de execução se o escopo vazar para além das 4 classes; e o risco de a task degenerar em "instalado, verde" sem entregar entendimento.

---

## Contexto

O repositório não tem nenhuma ferramenta de mutation testing. A cobertura também não é medida (JaCoCo ausente — item #6 do backlog), e `cobertura_pct: na` aparece em 100% dos status reports.

**Por que mutation testing importa aqui:** cobertura mede se a linha foi **executada**; não mede se o teste **verifica** algo. Um teste sem asserção dá cobertura idêntica a um teste rigoroso. O failure mode conhecido de código gerado por LLM é exatamente esse — teste que passa, não teste que pega bug. O experimento precisa distinguir os dois.

Conceito destilado em [`docs/aprendizado/teste-mutante-e-pit.md`](../../../aprendizado/teste-mutante-e-pit.md). **Leitura obrigatória antes de começar.**

### Restrição crítica do repositório

Os testes de integração sobem **MySQL 8 real via Testcontainers**. O PIT reexecuta a suíte relevante **uma vez por mutante**. Com um container no caminho, o run não termina.

**Excluir `*IntegrationTest` não é otimização — é requisito de viabilidade.**

---

## Decisão / abordagem

**Escopo fechado em quatro classes**, todas com teste unitário existente, lógica pura, sem contexto Spring e sem Testcontainers:

| Classe | Teste existente | Por que ela |
|---|---|---|
| `domain/service/LegendaParser` | `LegendaParserTest` | regex puro e pequeno — o relatório mais legível para uma primeira leitura |
| `application/strategy/PaymentRequestStrategy` | `PaymentRequestStrategyTest` | `supports()` com regex + construção do pedido |
| `application/strategy/PaymentProofStrategy` | `PaymentProofStrategyTest` | par da anterior; permite comparar duas leituras |
| `adapters/in/whatsapp/security/MetaSignatureValidator` | `MetaSignatureValidatorTest` | lógica de segurança com comparação e early-return; mutantes de fronteira instrutivos |

As três primeiras são **exatamente o código que a feature de complexidade baixa do experimento vai tocar** — o mutation score delas passa a ser dado de baseline, não só exercício.

**Configuração:** `targetClasses` restrito a essas quatro; `excludedTestClasses` cobrindo `*IntegrationTest`; saída em XML e HTML.

**Relatórios não são commitados** — ficam sob `target/`, já ignorado pelo git. Os **números** e a **leitura interpretada** vão para o status report, que é o artefato durável.

---

## Escopo / arquivos

### Modificar
- `financas_bot_telegram/pom.xml` — adicionar `pitest-maven` e a dependência `pitest-junit5-plugin` (**obrigatória**: sem ela o PIT não reconhece JUnit 5), com a configuração acima.
- `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` — documentar o comando de execução e quando usá-lo.

### Criar
- `docs/sprints/04-instrumentacao-qualidade/status/QA-012-piloto-pit-mutation-testing.md` — status report com frontmatter válido **e** a seção de leitura interpretada descrita abaixo.

### Não tocar
- Nenhum teste existente. **Corrigir teste fraco é a task seguinte** (item #2 do backlog). Misturar as duas coisas destrói a leitura limpa do baseline.
- Nenhuma classe de produção.
- JaCoCo e PMD — tasks próprias.

---

## Testes

Não aplicável no sentido usual: a task não adiciona comportamento. A verificação é a **execução bem-sucedida do PIT** e a coerência do relatório.

Obrigatório: `mvn test` continua verde antes e depois. O PIT exige suíte verde para funcionar.

---

## Critérios de aceitação

1. `pitest-maven` + `pitest-junit5-plugin` configurados no `pom.xml` do backend.
2. `targetClasses` restrito às quatro classes listadas; `excludedTestClasses` cobre `*IntegrationTest`.
3. O comando de execução roda até o fim e gera relatório XML + HTML.
4. **Nenhum teste de integração é executado pelo PIT** — verificável no log da execução.
5. Status report registra, **por classe**: mutantes gerados, mortos, sobreviventes, `NO_COVERAGE`, **mutation score** e **test strength**.
6. Status report contém a **leitura interpretada**: cada mutante `SURVIVED` classificado em (a) lacuna real de asserção, (b) mutante equivalente, (c) ruído — **com uma frase de justificativa por item**. **Se não houver sobrevivente**, registrar explicitamente esse fato e o que ele indica sobre a força dos testes existentes.
7. Status report explica por que cobertura e mutation score divergem, **com exemplo concreto destas quatro classes se houver**. Se o material não permitir o exemplo, registrar a limitação em vez de forçar exemplo genérico.
8. Comando documentado no `ROTEIRO-TESTES-BACKEND.md`.
9. `mvn test` verde, sem regressão.
10. Branch `feature/qa-012-piloto-pit-mutation-testing` criada a partir de `integration/04-instrumentacao-qualidade`.
11. Território respeitado — só `financas_bot_telegram/` e `docs/`.

> **Critérios 6 e 7 são o núcleo da task.** A sprint tem objetivo declarado de aprendizado: o entregável é o entendimento, e o relatório é onde ele fica registrado. Um relatório que diz apenas "PIT instalado, X% de mutation score" não entrega o que a sprint precisa.
>
> **Mas eles não são gate de reprovação.** Pode acontecer de as quatro classes não produzirem sobrevivente algum, ou só sobreviventes triviais — e aí não há o que interpretar, sem culpa do implementador. Nesse caso:
>
> - O implementador **registra o fato** e **para**. Não amplia o escopo por conta própria.
> - O Reviewer **anota a lacuna e sugere** que novas classes sejam escolhidas.
> - A escolha das novas classes é feita **junto com o dev humano**, em task ou rodada seguinte.
>
> Nenhum dos dois reprova a entrega por isso.

---

## Fora de escopo

- Corrigir os testes fracos revelados — item #2 do backlog, task separada, **antes da tag do marco zero**.
- Ampliar o escopo para outras classes.
- Instalar JaCoCo ou PMD.
- Construir o mecanismo de "classes tocadas" (item #7) — este piloto usa `targetClasses` fixo de propósito.
- Integrar PIT ao CI ou torná-lo gate de merge.
- Ajustar o conjunto de mutators além do default, salvo se o tempo de execução inviabilizar.

---

## Riscos & mitigações

| Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|
| Versão do PIT incompatível com Java 21 | Média | Alto | Conferir a matriz de compatibilidade antes de fixar a versão; é a primeira coisa a validar |
| Escopo vaza e o run demora demais | Baixa | Médio | `targetClasses` explícito nas quatro classes; `excludedTestClasses` obrigatório |
| Alguma das quatro classes puxa contexto Spring pelo teste | Média | Médio | Se puxar, trocar por outra classe de lógica pura e **registrar a troca no status report** |
| Task degenera em "instalado, verde" | **Média** | **Alto** | Critérios 6 e 7 são explícitos no plano, e o Reviewer avalia e **anota** a qualidade da leitura. Não é gate: a correção vem por nova rodada de classes decidida com o humano, não por reprovação |
| Nenhum mutante sobrevive, ou só sobreviventes triviais | Baixa | Médio | Sem sobrevivente não há o que interpretar, e isso **não é falha do implementador**. Ele registra o fato e **para** — não amplia escopo sozinho. Que os testes existentes sejam fortes também é achado válido, e fica registrado como baseline. Reviewer anota e sugere; o humano decide as próximas classes |

---

## Coordenação

- **Pré-requisito:** a branch `integration/04-instrumentacao-qualidade` **ainda não existe**. Criar antes do dispatch.
- **Paralelo:** não conflita com nada da sprint 03 — territórios disjuntos.
- **Bloqueia:** item #2 do backlog (corrigir testes fracos) e, indiretamente, a tag do marco zero do experimento.
- **Atenção pro Reviewer:**
  1. Confirmar no log que **nenhum teste de integração rodou** sob o PIT. É o erro mais provável e o mais caro.
  2. **Avaliar a leitura interpretada (critérios 6 e 7), mas sem reprovar por ela.** Se estiver rasa ou vazia, **anotar na avaliação** e **sugerir** que novas classes sejam escolhidas junto com o dev humano, para encontrar sobrevivente que renda análise. A decisão de ampliar escopo é do humano, não do Reviewer nem do implementador.
  3. Conferir se a classificação de "mutante equivalente" está justificada — é a saída fácil para não admitir lacuna de teste. Aqui também: **anotar, não reprovar.**
- **Sem gate QA** (`fluxos_qa: []`): a task não altera comportamento de feature e não há fluxo a validar. A verificação substantiva é a leitura interpretada, avaliada pelo Reviewer.
- **Após merge:** atualizar o backlog da sprint; refinar o item #2 com base nos sobreviventes encontrados.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido conforme `_TEMPLATE-status.md` **acrescido da seção de leitura interpretada**, e **revisão do Reviewer** (ADR 0005). PR para `integration/04-instrumentacao-qualidade`.

---

## Referências

- [`docs/aprendizado/teste-mutante-e-pit.md`](../../../aprendizado/teste-mutante-e-pit.md) — conceito, armadilhas, por que excluir Testcontainers. **Leitura obrigatória.**
- [`backlog-s04.md`](../backlog-s04.md) item #1 — origem desta task
- `docs/experiments/models-claude-experiment/04-metricas.md` — Q3
- `docs/experiments/models-claude-experiment/05-instrumentacao-e-harness.md` §5 — ferramental ausente
