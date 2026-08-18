---
task: BE-031
titulo: "Validar antes de subir ao S3 e eliminar a mensagem de erro morta"
sprint: 04-instrumentacao-qualidade
data_planejamento: 2026-08-16
branch_alvo: feature/be-031-validacao-antes-do-upload-e-mensagem-morta
prioridade: media
esforco: baixo
territorio: back
estado: pronto-pra-execucao
depende_de: [QA-015]
bloqueia: []
skills_dispatched: []
integration_branch: integration/04-instrumentacao-qualidade
fluxos_qa: []
mutation_gate: true
mutation_rationale: "Adotado por decisão explícita do humano em 2026-08-18, na mesma conversa em que ele confinou o escopo de produto a PaymentRequestStrategy. A amarra descrita em §Gate de mutação dissolveu-se: MensagemEntranteService fica fora da task, e PaymentRequestStrategy — única classe de produção alterada — já está no targetClasses congelado pela QA-012, medindo 11/11 hoje. O gate é medível sem tocar no pom.xml. Critério inalterado: test strength (mortos ÷ cobertos) >= 80% apenas sobre a classe alterada; código pré-existente fora do denominador; sobrevivente equivalente exige demonstração escrita."
---

# BE-031 — Validar antes de subir ao S3 e eliminar a mensagem de erro morta

## Intake

- **Origem:** itens **#9** e **#10** de `docs/sprints/04-instrumentacao-qualidade/backlog-s04.md`, ambos descobertos ao verificar no código o débito 2 da QA-014 durante o planejamento da QA-015.
- **Por quê agora:** o humano pediu um pacote com a QA-015, e as duas tasks precisam caber **antes da tag do marco zero** — depois dela, mudança de produção contamina o experimento.
- **Esforço:** baixo em linhas, **médio em decisão**. O código é pequeno; o que custa é escolher o que fazer com o vazamento que esta task **não** conserta.
- **Riscos resumidos:** o risco principal é a task se convencer de que resolveu o problema do S3. Ela resolve **um** dos dois caminhos. O outro precisa de decisão que não cabe aqui e está em §Fora de escopo, com o motivo.

> ⚠️ **`estado: aguardando-decisao-humana`.** Duas perguntas em aberto — o gate de mutação e o destino da mensagem. Nenhuma bloqueia a escrita do plano; as duas bloqueiam o dispatch. Ver §Pontos de aprovação humana.

---

## Contexto

### O que foi verificado no código (leitura direta, 2026-08-16)

`MensagemEntranteService.processar` (linhas 48-79):

```java
@Transactional                                              // ← 48
public void processar(PaymentMessageDTO dto) {
    ...
    MensagemProcessingStrategy strategy = strategies.stream()
        .filter(s -> s.supports(dto))                       // ← 58
        .findFirst()
        .orElseThrow(() -> new InvalidMessageFormatException(ERROR_MESSAGE, dto.getChatId()));  // ← 62
    ...
    strategy.process(dto);                                  // ← 73
}
```

`PaymentRequestStrategy.process` (linhas 52-81) e `parsePedido` (83-98):

```java
if (dto.getFileBytes() == null) throw new PhotoProcessingException(...);  // ← 56
String s3Url = s3ImageUploadService.uploadFile(...);                      // ← 61  escreve no S3
PedidoPagamento pedido = parsePedido(dto);                                // ← 65  valida
pedido.setImagemUrl(s3Url);                                               // ← 67
salvarPedidoPagamentoUsecase.execute(pedido, chatId);                     // ← 69  grava no banco
```

**Fato 1 — o `throw` de `parsePedido` é inalcançável.** `supports()` (linha 48) e `parsePedido` (linha 87) aplicam o **mesmo `PEDIDO_PATTERN`** (`static final`, determinístico) sobre o **mesmo `caption.trim()`** do **mesmo `dto`**, e nada modifica o DTO entre as duas chamadas. `MensagemEntranteService:73` é o único chamador de `process()` — verificado por busca das implementações de `MensagemProcessingStrategy`. Se `supports()` deu `true`, `matches()` dá `true` de novo.

**Fato 2 — a mensagem boa nunca foi vista.** Quem lança na legenda malformada é o `orElseThrow` da linha 62, com `ERROR_MESSAGE` — texto genérico que cobre pedido **e** comprovante. Os cinco exemplos e a dica de auto-categorização que estão em `PaymentRequestStrategy:88-97` são texto morto.

**Fato 3 — não existe delete de S3 no repositório.** `grep` por `deleteObject|DeleteObject` em `financas_bot_telegram/src`: **zero ocorrências**. `S3ImageUploadService` expõe `uploadImage` e `uploadFile`, e `uploadFile` devolve **URL**, não a key. Qualquer compensação exigiria método novo no adapter e uma forma de recuperar a key — hoje ela só existe dentro de `construirChaveS3`, privado.

**Fato 4 — o upload roda dentro da transação.** `processar` é `@Transactional` e o upload acontece dentro dela. Falha em `salvarPedidoPagamentoUsecase.execute` faz rollback do insert e **não** desfaz o objeto no S3.

### O que isso significa em risco real

| Caminho | Alcançável hoje? | Consequência |
|---|---|---|
| Legenda inválida chega em `parsePedido` | **Não** (fato 1) | Nenhuma hoje. Vira vazamento se alguém afrouxar `supports()` ou chamar `process()` de outro lugar |
| Persistência falha depois do upload | **Sim** | Objeto órfão no bucket, sem pedido apontando para ele, sem nada que o recolha |

**Esta task fecha o primeiro. O segundo continua aberto** — ver §Fora de escopo.

---

## Decisão / abordagem

### Parte 1 — inverter a ordem: validar antes de subir

```java
PedidoPagamento pedido = parsePedido(dto);           // valida primeiro
String s3Url = s3ImageUploadService.uploadFile(...); // só então escreve no S3
pedido.setFileIdTelegram(dto.getMediaId());
pedido.setImagemUrl(s3Url);
```

Três linhas trocadas de lugar. Não altera comportamento observável hoje (o caminho é inalcançável), e **remove a armadilha**: a partir daqui, entrada inválida nunca custa uma escrita no S3, independentemente de quem venha a chamar `process()` no futuro.

**Por que fazer algo que hoje não muda nada:** o custo é três linhas e a alternativa é manter uma ordem que só está correta por acidente de outra classe. `supports()` e `parsePedido` são acoplados por coincidência de regex; a ordem defensiva não depende dessa coincidência.

### Parte 2 — a mensagem morta

O ramo defensivo fica, o texto morto sai. A exceção passa a declarar o que é:

```java
if (!matcher.matches()) {
    // Defesa: supports() já validou este mesmo caption com este mesmo pattern.
    // Se chegou aqui, o dispatcher foi contornado — não é erro de usuário.
    throw new InvalidMessageFormatException(ERROR_MESSAGE_DEFENSIVA, dto.getChatId());
}
```

**O que NÃO fazer:** apagar a validação. Defesa redundante em método público de `@Component` é razoável, e o item #9 do backlog registra isso explicitamente.

**✅ Resolvido em 2026-08-18.** Os cinco exemplos e a dica de auto-categorização **não** sobem para o dispatcher nesta task: `MensagemEntranteService` fica fora. A melhoria da mensagem que o usuário de fato vê vira item de produto em `docs/plans/BACKLOG-produto.md` — o texto bom não se perde, só não entra aqui.

### Gate de mutação — a amarra que a QA-015 não tinha

`targetClasses` do PIT no `pom.xml` contém **as quatro classes do piloto da QA-012**, e o baseline de 42 mutantes está congelado sobre elas.

- Se o escopo desta task ficar **só em `PaymentRequestStrategy`**, ela já está dentro do `targetClasses` e o gate é medível sem tocar em configuração.
- Se a task mexer em **`MensagemEntranteService`** (opção de produto acima), essa classe **não está** no `targetClasses`. Adotar o gate exigiria ampliá-lo — o que muda a configuração congelada, altera o denominador do baseline da QA-012 e precisa de decisão própria. **Ampliar `targetClasses` de carona numa task de fix é exatamente o tipo de mudança silenciosa que o Reviewer é instruído a caçar** (`Mutation Gate Audit`, item 5).

Por isso as duas perguntas estavam amarradas: **escopo de produto define se o gate é barato ou se ele abre outra discussão.**

**✅ Desatado em 2026-08-18.** O humano confinou o escopo de produto a `PaymentRequestStrategy` e adotou o gate. Cai o segundo caso: `MensagemEntranteService` não é tocada, o `targetClasses` **não muda**, o baseline de 42 mutantes da QA-012 fica intacto. O implementador mede o gate sobre uma classe que já está no escopo e que hoje dá 11/11 — qualquer queda é finding, não ruído.

---

## Escopo / arquivos

### Modificar

- `financas_bot_telegram/src/main/java/.../application/strategy/PaymentRequestStrategy.java` — inverter a ordem (validar antes de subir) e substituir o texto morto por mensagem defensiva com comentário explicando por que o ramo existe.
- `financas_bot_telegram/src/test/java/.../application/strategy/PaymentRequestStrategyTest.java` — ajustar o teste do `throw` que a **QA-015** terá adicionado, e acrescentar asserção de que **o S3 não é chamado** quando a legenda é inválida (`verify(s3ImageUploadService, never()).uploadFile(any(), any(), any())`). Essa asserção é a que trava a ordem: sem ela, alguém reverte a inversão e nenhum teste percebe.

### Não tocar

- `MensagemEntranteService` — **fora, por decisão do humano em 2026-08-18.** Tocar nela reabre a discussão de `targetClasses` e é finding do Reviewer nesta task.
- `S3ImageUploadService` — nenhum método novo nesta task (ver §Fora de escopo).
- `pom.xml`, `pmd-ruleset.xml`, `lombok.config` — ferramental congelado.
- `PaymentProofStrategy` — **verificar se tem o mesmo padrão de upload-antes-de-validar e reportar**, mas não alterar. Se tiver, é item de backlog novo.

---

## Verificação de carregamento de skills (coleta de evidência sobre o harness)

> **Não é critério de aceitação da task.** É uma verificação de **uma rodada só**, pedida pelo `ai-engineer`, e não deve virar praxe. Ela não influencia o veredito do Reviewer sobre o código.

**Contexto:** desde 2026-08-16 as skills `developing-java-spring-applications` e `writing-java-unit-tests` são pré-carregadas via `skills:` no frontmatter do `backend.md`. Os `SKILL.md` dessas duas linkam seus assets por **path relativo** (`references/mvc-architecture.md`, por exemplo), e a documentação oficial não confirma como esse path é resolvido em tempo de execução. **Cerca de 90% do conteúdo dessas skills — todos os exemplos de código — mora nos assets**, não no `SKILL.md`. Se a resolução falhar, o remédio é acrescentar uma âncora explícita nos `SKILL.md`.

O agente backend deve incluir no status report uma seção **`Verificação de carregamento de skills`** com três respostas:

1. **O conteúdo das duas skills estava disponível desde o primeiro turno?** Sim ou não, sem suposição — se não souber dizer, escrever que não é possível determinar.
2. **Leu algum arquivo de `references/` ou `examples/` dessas skills?** Em caso positivo: **qual path usou**, e se acertou **na primeira tentativa** ou precisou de `Glob`/tentativa e erro para localizar o arquivo. O path exato e o número de tentativas são o dado que interessa — "consegui ler" sem o path não serve.
3. **Se não leu nenhum asset**, registrar explicitamente qual dos dois casos ocorreu: **não foi necessário** para o trabalho, ou **não conseguiu resolver o path**. São diagnósticos opostos e não podem sair fundidos num "não li".

⚠️ **Não force uma leitura para satisfazer este passo.** Se abrir um asset apenas por causa desta verificação, **declare isso** — "li só para testar a resolução de path, não precisava para o trabalho". Um `sim` obtido por leitura induzida responde uma pergunta diferente da que está sendo feita, e é pior que um `não foi necessário` honesto.

---

## Testes

```bash
./mvnw test
```

Mais o comando de cobertura do §Camada 1.6, obrigatório: esta task **toca classe de produção**, e o Reviewer agora audita o número (`Coverage Audit`, vigente desde 2026-08-16).

```bash
./financas_bot_telegram/mvnw clean jacoco:prepare-agent test jacoco:report -f financas_bot_telegram/pom.xml \
  -Dtest='!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false
```

⚠️ O `clean` apaga `target/`, **inclusive os relatórios do PIT**. Se o gate de mutação for adotado, transcreva os números do PIT **antes** de rodar cobertura.

Os 48 `*IntegrationTest` falham localmente por Docker — débito de ambiente conhecido, verde no CI. Não é regressão desta task.

---

## Critérios de aceitação

1. `parsePedido` roda **antes** de `uploadFile` em `process()`.
2. Existe teste que falha se alguém reverter a ordem: legenda inválida ⇒ `InvalidMessageFormatException` **e** `verify(..., never()).uploadFile(...)`.
3. O texto com os cinco exemplos não existe mais em `PaymentRequestStrategy`, e o ramo defensivo tem comentário dizendo por que existe.
4. A suíte unitária segue verde, e `PaymentRequestStrategy` mantém **100% de mutation score** (11/11 na QA-012). Queda é finding: significa que a mudança criou comportamento sem asserção.
5. `cobertura_pct` traz número real, com o valor de **branch ao lado** e a ressalva unit-only escrita. `na` aqui é inaceitável — a task toca produção.
6. O status declara se `PaymentProofStrategy` tem ou não o mesmo padrão de upload-antes-de-validar. Resposta `não verifiquei` é aceitável e preferível a chute; `não tem` exige dizer como foi verificado.
7. O status traz a seção `Verificação de carregamento de skills` com as três respostas.
8. O status **não afirma** que o vazamento de órfãos no S3 foi resolvido. Ele não foi. Ver §Fora de escopo.

---

## Fora de escopo (explicitamente)

| Item | Por que fica de fora |
|---|---|
| **Órfão no S3 quando a persistência falha** | É o vazamento **alcançável hoje**, e não se resolve com três linhas. Três caminhos possíveis — delete compensatório (exige método novo no adapter e recuperar a key, que hoje só existe em método privado), upload após o commit no padrão `@TransactionalEventListener(AFTER_COMMIT)` que a EVO-02 já usa (mas `imagemUrl` é persistida junto do pedido, então viraria gravação em duas fases), ou regra de lifecycle no bucket. **É decisão de arquitetura: merece ADR, não improviso dentro de um fix.** E antes de dimensionar, alguém precisa medir se já existem órfãos em `bot-financas-pagamentos-satyan` |
| Método de delete em `S3ImageUploadService` | Consequência do item acima. Adicionar API de escrita destrutiva sem a decisão de quando usá-la é pior que não ter |
| Levar os exemplos para o `ERROR_MESSAGE` | Decisão de produto **e** de escopo de medição — ver §Gate de mutação. Só entra com aprovação explícita |
| Remover a validação redundante de `parsePedido` | Defesa em método público é legítima. O problema nunca foi a validação, foi o texto morto pendurado nela |
| `PaymentProofStrategy` | Só inspeção e relato. Alterar exigiria repetir toda esta análise para outra classe |
| Qualquer alteração em `pom.xml` | Ferramental congelado com baseline. Ampliar `targetClasses` é decisão separada |

---

## Riscos & mitigações

| Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|
| Status declarar o problema do S3 como resolvido | **Alta** | **Alto** | Critério 8 proíbe explicitamente. Reviewer confere |
| Inverter a ordem e ninguém travar com teste | Média | Médio | Critério 2 exige o `never()` |
| Conflito com a QA-015 no mesmo arquivo de teste | **Alta** | Baixo | Sequenciamento obrigatório: QA-015 mergeia primeiro; esta sai da `integration/04` já com ela dentro |
| Ampliar `targetClasses` de carona | Baixa | **Alto** | Fora de escopo explícito; é o item 5 do `Mutation Gate Audit` do Reviewer |
| A verificação de skills virar leitura induzida | Média | Médio | O passo exige declarar leitura induzida como tal |
| Escopo crescer para o `PaymentProofStrategy` | Média | Médio | Critério 6 pede **relato**, não conserto |

---

## Coordenação

- **Lane:** `backend`. Arquitetura: **hexagonal**.
- **Depende de:** **QA-015 mergeada**. As duas tocam `PaymentRequestStrategyTest`, e esta ajusta o teste que aquela cria. Fora de ordem, dá conflito e a comparação antes/depois do PIT da QA-015 fica contaminada por mudança de produção.
- **Bloqueia:** a tag do marco zero, junto com a QA-015 — as duas são do mesmo pacote e precisam estar dentro do baseline.
- **Atenção pro Reviewer:**
  - **Cobrar o critério 8.** A afirmação mais provável desta task é "resolvido o vazamento do S3", e ela seria falsa: o caminho consertado é o inalcançável; o alcançável (rollback) continua aberto.
  - Conferir que `targetClasses` no `pom.xml` **não** mudou.
  - Conferir a asserção `never()` — sem ela o critério 1 não tem quem o sustente.
  - `Coverage Audit` se aplica: a task toca produção, logo `cobertura_pct: na` é finding.
  - A seção de verificação de skills **não entra no veredito** do código. Se estiver ausente, é finding de processo, não de qualidade.
- **Atenção pro QA:** `fluxos_qa: []`, **`qa_required: false`** — nenhum comportamento observável pelo usuário muda (o caminho consertado é inalcançável, e a mensagem trocada é a que ninguém vê). Se o humano aprovar levar os exemplos ao dispatcher, **isso muda**: passa a haver texto novo visível ao usuário e o QA volta a fazer sentido.
- **Após merge:** fechar os itens **#9** e **#10** no `backlog-s04.md` — o #10 fecha **parcialmente**, e o resíduo (órfão por rollback) vai para `docs/PENDENCIAS-TECNICAS.md` com o encaminhamento de ADR; consolidar a resposta da verificação de skills e devolvê-la ao `ai-engineer`.

---

## Pontos de aprovação humana

> ✅ **Os três respondidos em 2026-08-18.** O `estado` passou de `aguardando-decisao-humana` para `pronto-pra-execucao`. Nada aqui foi inferido: cada resposta abaixo é do humano, na conversa de planejamento.

1. **Gate de mutação — ✅ ADOTA.** `mutation_gate: true`. Escopo de medição: **apenas `PaymentRequestStrategy`**, única classe de produção alterada, já presente no `targetClasses` congelado. Piso de 80% de `test strength`, sem tocar no `pom.xml`.
2. **Destino da mensagem — ✅ o ramo defensivo FICA, o texto morto SAI.** É exatamente a §Parte 2 deste plano: a exceção passa a declarar que é defesa, com comentário dizendo por que o ramo existe. **`MensagemEntranteService` fica fora da task** — os cinco exemplos não sobem para o dispatcher nesta entrega.
   - ⚠️ **A primeira leitura registrada foi a errada** (apagar o guard inteiro) e o humano corrigiu na sequência. Fica registrado porque a ressalva escrita no plano — "não é para apagar a validação, defesa em método público é razoável" — foi o que expôs a divergência antes do dispatch. O guardrail pagou o próprio custo.
   - **Perda declarada:** o texto bom morre com o ramo. Vai para `docs/plans/BACKLOG-produto.md` como item de produto ("melhorar a mensagem de erro do dispatcher"), para não se perder junto com o código.
3. **Prefixo — ✅ segue `BE-031`.** Mantido o encadeamento com a QA-015 pela `integration/04`.

---

## Referências

- `docs/sprints/04-instrumentacao-qualidade/backlog-s04.md` — itens **#9** e **#10**, com a evidência de código.
- `docs/sprints/04-instrumentacao-qualidade/plans/QA-015-fortalecer-testes-revelados-pelos-pilotos.md` — task predecessora.
- `docs/sprints/04-instrumentacao-qualidade/status/QA-014-piloto-jacoco-cobertura.md` — débito 2, origem do achado.
- `docs/decisions/0021-gate-de-mutation-testing-opcional-por-task.md` — critério do gate.
- `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` §Camada 1.5 e §Camada 1.6.
- `.claude/agents/reviewer.md` §`Coverage Audit (conditional)` — vigente desde 2026-08-16.
