---
task: QA-014
titulo: "Piloto do JaCoCo — cobertura medida e comparada com PIT e PMD"
sprint: 04-instrumentacao-qualidade
data_planejamento: 2026-08-13
branch_alvo: feature/qa-014-piloto-jacoco-cobertura
prioridade: alta
esforco: medio
territorio: back
estado: pronto-pra-execucao
depende_de: []
bloqueia: []
skills_dispatched: []
integration_branch: integration/04-instrumentacao-qualidade
fluxos_qa: []
mutation_gate: false
mutation_rationale: ""
---

# QA-014 — Piloto do JaCoCo: cobertura medida e comparada com PIT e PMD

## Intake

- **Origem:** item **#6** de `docs/sprints/04-instrumentacao-qualidade/backlog-s04.md`. Origem primária: `04-metricas.md` **Q2** (cobertura nas classes tocadas) do experimento.
- **Por quê agora:** é a **terceira e última** ferramenta de qualidade da sprint (PIT ✅ QA-012, PMD ✅ QA-013). Fechando-a, as três descrevem o mesmo código e a comparação entre elas fica possível. É também o que **destrava `cobertura_pct`**, hoje `na` em 100% dos status reports.
- **Esforço:** médio. A instalação é pequena; o custo está em duas coisas que não são óbvias — neutralizar a distorção do Lombok antes de congelar o baseline, e explicar as divergências entre três medições do mesmo código.
- **Riscos resumidos:** JaCoCo, ao contrário de PIT e PMD, **precisa dos testes executando** — e nesta máquina os `*IntegrationTest` falham por Docker. Mitigado medindo só a suíte unitária, o que também é o que torna o número comparável com o PIT. O segundo risco é silencioso: Lombok sem configuração infla o código "não coberto".

---

## Contexto

**O que já existe:**

- `financas_bot_telegram/pom.xml` — Java 21, Spring Boot parent. Tem `pitest-maven` 1.25.9 (QA-012) e `maven-pmd-plugin` 3.26.0 (QA-013), ambos **fora do ciclo de vida**. **Não tem JaCoCo.**
- **191 classes de produção**, **82 de teste**, `testes_total: 422`.
- **Nenhum `argLine` de surefire configurado** e nenhum bloco `<plugin>` de surefire no `pom.xml` — verificado. Isso importa: o `jacoco:prepare-agent` funciona **injetando `argLine`**, e um `argLine` pré-existente sem `@{argLine}` o sobrescreveria em silêncio, zerando a cobertura sem erro nenhum.
- **Lombok 1.18.32**, usado em **23 arquivos de produção**, e **não existe `lombok.config`** — verificado por `find`. Ver §Decisão.

**Números já medidos, que esta task vai comparar** (QA-012, passada de cobertura do próprio PIT, restrita às classes mutadas):

| Classe | Line coverage (PIT) | Mutation score | Test strength |
|---|---:|---:|---:|
| `LegendaParser` | 94% (17/18) | **75%** | 75% |
| `PaymentRequestStrategy` | 96% (47/49) | 100% | 100% |
| `PaymentProofStrategy` | 100% (33/33) | 100% | 100% |
| `MetaSignatureValidator` | 90% (26/29) | 79% | 85% |

**Correção de um fato que o backlog registra errado.** O item #6 diz que *"o comando `jacoco:report` que o agente QA é instruído a rodar falha hoje"*. Verificado por `grep -rn "jacoco"` em `.claude/`, `docs/templates/` e `docs/runbooks/`: existe **uma única ocorrência no repositório**, um comentário no frontmatter de `docs/templates/_TEMPLATE-status.md:16`. O agente `qa-test-specialist` **não menciona JaCoCo**. Não há comando falhando — há um campo cujo template aponta uma ferramenta que nunca existiu. O efeito é o mesmo (`cobertura_pct: na` sempre), a causa é outra.

**Estado do ambiente:** 48 testes em 11 classes `*IntegrationTest` falham **localmente** por Docker inacessível ao Testcontainers. O CI roda a mesma suíte verde (`Tests run: 422, Failures: 0, Errors: 0`, run `31727999562`). É débito registrado, **não** é escopo desta task.

---

## Decisão / abordagem

### Medir só a suíte unitária — e por quê isso não é atalho

Aplicar a **mesma exclusão `*IntegrationTest`** que o PIT já usa. Três razões, em ordem de importância:

1. **Comparabilidade.** O PIT excluiu essas classes. Se o JaCoCo as incluísse, cobertura e mutation score descreveriam populações diferentes, e toda comparação desta task viraria ruído.
2. **Qualidade de teste é o que o experimento mede.** Cobertura vinda de teste de integração infla o número sem dizer nada sobre a qualidade dos testes — que é o sinal que o experimento persegue em código gerado por modelo.
3. **Contorna o Docker por completo**, sem a task ficar refém do FIX de ambiente.

**O custo, que precisa estar escrito no relatório:** o número **subestima** a cobertura real do projeto. Quem comparar com número de mercado tem que saber disso. A frase não é ressalva de rodapé — é parte da definição da métrica.

### Neutralizar o Lombok antes de congelar o baseline

Sem `lombok.config`, o JaCoCo conta **getter, setter, `equals`, `hashCode`, `toString` e builder gerados** como linhas não cobertas. O resultado é um baseline que mede a quantidade de Lombok no projeto, não a qualidade dos testes.

O impacto aqui é assimétrico e foi medido:

- **As 5 classes do piloto têm zero Lombok** — a leitura interpretada sai limpa de qualquer jeito.
- **O baseline do projeto é distorcido**, porque Lombok se concentra exatamente onde ele gera mais código: `application/dto` (9 arquivos), `adapters/out` (7), `domain/model` (5), `domain/entity` (2).

**Decisão:** criar `financas_bot_telegram/lombok.config` com `lombok.addLombokGeneratedAnnotation = true`. Isso faz o Lombok anotar o que gera com `@lombok.Generated`, que o JaCoCo **honra e exclui** por padrão. É uma linha, é prática padrão, e sem ela o baseline não significa nada.

**Isso é medição, não conserto** — nenhum teste é escrito, nenhuma classe muda. Para provar o tamanho da distorção, o relatório traz **os dois números**, antes e depois. Custa um run a mais e mostra concretamente quanto o Lombok estava mentindo.

### Fora do ciclo de vida, como as outras duas

`jacoco:prepare-agent` **não precisa** de `<executions>`: invocado na mesma linha de comando, ele define o `argLine` que o surefire consome em seguida. Ou seja, dá para manter o mesmo padrão de PIT e PMD — plugin declarado, versão em `<properties>`, **nada amarrado a fase de build**.

Isso preserva a propriedade que a sprint vem mantendo: **nenhuma ferramenta de diagnóstico encarece o build de todo mundo.**

### Sem gate

Nada de `jacoco:check` com threshold. Build que falha por cobertura faz o agente escrever teste **para a métrica**, não para o comportamento — interferência direta na variável medida. Relatório sim, gate não. Mesma decisão do PMD, mesmo motivo.

### Regra provisória para `cobertura_pct`

O campo hoje diz "cobertura % da classe/componente principal", e "principal" é julgamento de quem escreve. Passa a ser: **cobertura de linha das classes de produção que a task tocou**; `na` quando a task não toca classe de produção.

Alinha com o **D3 do item #7** (`Q2` = produção, não teste). É **provisória** até o #7 automatizar o recorte de "classes tocadas", e o plano declara isso para ninguém tratar como definitiva.

### Conjunto do piloto: as 5 classes do PMD

`LegendaParser`, `PaymentRequestStrategy`, `PaymentProofStrategy`, `MetaSignatureValidator` e `FecharMesServiceImpl`.

As 4 primeiras passam a ter **as três ferramentas medindo o mesmo código**. A quinta tem PMD e JaCoCo, e é a mais complexa das cinco — é onde cobertura de **branch** tende a divergir mais de cobertura de linha.

---

## Escopo / arquivos

### Criar

- `financas_bot_telegram/lombok.config` — `lombok.addLombokGeneratedAnnotation = true`, com comentário explicando que existe para o JaCoCo não contar código gerado. Sem o comentário, é a linha mais fácil de alguém apagar sem saber o que quebra.

### Modificar

- `financas_bot_telegram/pom.xml` — `jacoco-maven-plugin`, versão em `<properties>`, **fora do ciclo de vida**, com comentário explicando por que não está amarrado a `test`.
- `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` — nova subseção na **Camada 1**, no formato da 1.5 (PIT) e da 2.0 (PMD): comando, onde sai o relatório, como ler, e a ressalva de que o número é unit-only.
- `docs/templates/_TEMPLATE-status.md` — comentário do `cobertura_pct` passa a descrever a regra provisória e o comando real.
- `docs/runbooks/PRE-MERGE-CHECKLIST.md` — `cobertura_pct` ganha a mesma definição, declarada **informativa, não bloqueante**.

### Não tocar (escopo limitado)

- **Qualquer classe de produção ou de teste.** Nenhum teste novo, nenhum gap de cobertura corrigido. Se o relatório revelar buraco relevante, **vira pendência técnica**.
- **Configuração de PIT e PMD** no `pom.xml` — blocos vizinhos.
- **`.github/workflows/`** — JaCoCo não entra no CI nesta task.
- **O problema de Docker/Testcontainers** — débito próprio, FIX próprio.

---

## Testes

**Não aplicável no sentido usual:** a task não adiciona nem altera lógica de produção. `testes_total` deve permanecer **422** e `testes_novos` deve ser **0**.

⚠️ **Atenção ao registrar o gate `testes`:** o run desta task é **unit-only**, então o número de testes executados será **menor que 422**. Isso **não** é regressão — é o recorte da medição. O relatório precisa registrar quantos testes rodaram no recorte e deixar claro que os 422 continuam válidos na suíte completa (evidência: CI, run `31727999562`).

---

## Critérios de aceitação

- [ ] `jacoco-maven-plugin` no `pom.xml`, versão em `<properties>`, **não amarrado a nenhuma fase do build**.
- [ ] `financas_bot_telegram/lombok.config` criado com `lombok.addLombokGeneratedAnnotation = true` e comentário explicando o porquê.
- [ ] Comando documentado roda limpo **sem depender de Docker** e gera relatório HTML + XML.
- [ ] **Distorção do Lombok quantificada:** cobertura do projeto **antes** e **depois** do `lombok.config`, com a diferença comentada em uma frase.
- [ ] **Baseline registrado:** cobertura de **linha e de branch** do projeto (produção; teste não entra — o JaCoCo instrumenta só `src/main`), mais o número de testes executados no recorte unit-only.
- [ ] **Tabela por classe** das 5 classes do piloto: cobertura de linha e de branch.
- [ ] **Comparação de três números** nas 4 classes com dado do PIT — cobertura de linha do JaCoCo × cobertura de linha do PIT × mutation score — com **cada divergência explicada**. Divergência entre JaCoCo e PIT no mesmo arquivo é o achado mais instrutivo possível aqui: são duas ferramentas contando a mesma coisa de formas diferentes.
- [ ] **Critério de aprendizado:** o relatório explica, com dado deste projeto, **por que cobertura alta não implica teste bom** — e usa `LegendaParser` (94% de linha no PIT, 75% de mutation score) como caso concreto, agora com o número independente do JaCoCo ao lado.
- [ ] **Cobertura de branch × de linha:** identificar ao menos uma classe onde as duas divergem de forma relevante e explicar o que a de branch enxerga que a de linha não. `FecharMesServiceImpl` é a candidata natural.
- [ ] `ROTEIRO-TESTES-BACKEND.md`, `_TEMPLATE-status.md` e `PRE-MERGE-CHECKLIST.md` atualizados com o comando e a regra provisória de `cobertura_pct`.
- [ ] A limitação **unit-only** está escrita no relatório e no runbook — o número **subestima** a cobertura real.
- [ ] `testes_novos = 0` e nenhuma classe de produção ou teste alterada.
- [ ] Branch `feature/qa-014-piloto-jacoco-cobertura` a partir de `integration/04-instrumentacao-qualidade`.
- [ ] Status report em `docs/sprints/04-instrumentacao-qualidade/status/QA-014-piloto-jacoco-cobertura.md`.

---

## Fora de escopo (explicitamente)

| Item | Onde vive |
|---|---|
| Escrever teste para corrigir gap de cobertura | pendência técnica; item #2 para o caso do `LegendaParser` |
| `jacoco:check` / threshold bloqueante | depois do experimento |
| JaCoCo no CI | idem |
| Cobertura da suíte de integração | requer o FIX de Docker; declarar como não medido |
| FIX do Docker/Testcontainers | débito próprio, FIX próprio |
| Escopo por diff | item **#7** |
| Corrigir os 2 bugs de locale da QA-013 | pendência técnica já registrada |

---

## Riscos & mitigações

| Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|
| **`argLine` do surefire sobrescrever o do JaCoCo** — cobertura sai **zero sem erro nenhum** | Baixa | **Alto** | Verificado que hoje não há `argLine` nem plugin surefire configurado. Critério de sanidade: se a cobertura vier 0%, é isto — não é falta de teste. Registrar no runbook que qualquer `argLine` futuro precisa de `@{argLine}` |
| Lombok distorcer o baseline sem ninguém notar | **Alta** sem o `lombok.config` | Alto | `lombok.config` é critério de aceitação, e a distorção é quantificada com os dois números |
| Docker indisponível bloquear a task | **Alta** se tentar suíte completa | Alto | Recorte unit-only é a decisão central do plano, não contorno improvisado |
| Confundir "menos testes executados" com regressão | Média | Médio | Explicitado em §Testes: o recorte reduz o número, os 422 seguem válidos com evidência de CI |
| JaCoCo e PIT divergirem e a divergência ser varrida para baixo do tapete | Média | Médio | Explicar cada divergência é **critério de aceitação**, não opcional |
| Task extrapolar e escrever teste | Média | Alto | `testes_novos = 0` verificável no diff |

---

## Coordenação

- **Pode rodar em paralelo com:** item #8 (gate `*IntegrationTest`). ⚠️ **Não** rodar em paralelo com o item **#2** (corrigir testes fracos do `LegendaParser`): o #2 muda a cobertura da classe que é o caso central da comparação desta task. Se ambos forem despachados, **esta vai primeiro** — o baseline precisa ser tirado antes.
- **Depende sequencialmente de:** nada. QA-012 e QA-013 já estão em `develop` e fornecem os números de comparação.
- **Bloqueia:** item **#7** (o Q2 precisa de JaCoCo instalado) e o pré-registro do experimento.
- **Atenção pro Reviewer:**
  - **Conferir que a cobertura não veio zero por `argLine`.** É o modo de falha silencioso desta ferramenta: número plausível de 0% parece "sem teste", quando é instrumentação quebrada.
  - Conferir que os dois números do Lombok foram medidos de verdade, e não estimados.
  - **A comparação de três números é o entregável.** Tabela sem explicação de divergência não cumpre o critério — mesmo padrão da QA-013, onde o entregável era a justificativa, não o relatório.
  - QA-012 teve 5 achados de prosa e QA-013 teve 1 `high` de afirmação não sustentada pela evidência. Esta task produz **três medições do mesmo código**, o que multiplica a chance de rótulo contradizer número. **Todo rótulo qualitativo tem que bater com o dado ao lado.**
  - Reproduzir o run e conferir o baseline.
- **Atenção pro QA:** `fluxos_qa: []` — não altera comportamento de produto. **`qa_required: false`.**
- **Após merge:** fechar o item **#6** no `backlog-s04.md`; registrar o baseline de cobertura no `STATE.md` junto do baseline do PMD; abrir pendência para cada gap relevante encontrado; avaliar se o item **#2** ganha alvo novo.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido conforme `_TEMPLATE-status.md`, e **revisão do Reviewer** (sessão separada — ADR 0005). `fluxos_qa: []`, logo **sem gate de QA**. `mutation_gate: false`, logo **sem gate de mutação** — a task não altera código Java de produção, não há classe alterada para entrar no denominador. Implementador abre o PR da feature para `integration/04-instrumentacao-qualidade`; **não** abre PR direto para `develop`.

---

## Referências

- `docs/sprints/04-instrumentacao-qualidade/backlog-s04.md` — item **#6** (este), **#7** (D3/D4 e premissas medidas), **#2** (conflito de ordem).
- `docs/sprints/04-instrumentacao-qualidade/status/QA-012-piloto-pit-mutation-testing.md` §"Baseline por classe" — os números do PIT que esta task compara.
- `docs/sprints/04-instrumentacao-qualidade/status/QA-013-pmd-ruleset-curado-e-piloto.md` — padrão de relatório com leitura interpretada e baseline congelado.
- `docs/sprints/04-instrumentacao-qualidade/pendencias-tecnicas.md` — débito do Docker/Testcontainers e premissas do item #7.
- `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` §"Camada 1.5 (PIT)" e §"2.0 (PMD)" — formato a replicar.
- `financas_bot_telegram/pom.xml` — `pitest-maven` e `maven-pmd-plugin` como referência de plugin fora do ciclo de vida.
