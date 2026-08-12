---
adr: 0021
titulo: "Gate de mutation testing opcional por task, medido por test strength sobre o código alterado"
data: 2026-08-10
status: Accepted
decisores: humano
relacionado: [0007, 0017, 0019]
supersedes: null
superseded_by: null
---

# ADR 0021 — Gate de mutation testing opcional por task, medido por test strength sobre o código alterado

---

## Contexto

A QA-012 (sprint 04) instalou o `pitest-maven` no backend e produziu o primeiro baseline de mutation testing do projeto, em escopo reduzido a quatro classes de lógica pura. O plugin roda **sob demanda**, fora do ciclo de vida do Maven, e não está no CI.

O piloto entregou o número, mas deixou uma lacuna deliberada: **não existe critério de aceitação.** Sem um piso declarado, o relatório informa e não decide — qualquer resultado é aceitável, o que torna a ferramenta um instrumento de leitura sem consequência. A revisão humana do status report em 2026-08-10 levantou isso como o primeiro item a fechar antes da próxima implementação.

Três fatos do baseline restringem qual critério é honesto:

1. **O total foi 88% de mutation score sobre 42 mutantes** — mas o teto realista das mesmas quatro classes é **39/42 ≈ 93%**, porque dois sobreviventes são **mutantes equivalentes demonstrados** (mudança sem efeito observável — ex.: capacidade inicial de um `StringBuilder`) e um item é `NO_COVERAGE` inalcançável sem injetar um provider JCE falso. **100% não é meta atingível**, e nunca será.

2. **Mutation score cru mistura dois diagnósticos diferentes.** `MetaSignatureValidator` marca **79% de mutation score** e **85% de test strength**. A diferença inteira é um bloco `catch` de exceção checada que não ocorre na prática. Um piso sobre o score cru reprovaria a classe por ter código defensivo inalcançável — criando incentivo para **remover a defesa** a fim de melhorar a nota.

3. **`targetClasses` hoje é uma lista escrita à mão de quatro classes.** O número medido não é comparável entre escopos: ampliar a lista muda o denominador. Um piso global sobre "o projeto" não tem significado enquanto o escopo for curado manualmente.

Some-se a isso o custo: a fase de cobertura do PIT roda a suíte unitária inteira antes de mutar qualquer coisa (~22 s dos ~48 s do run), independentemente do tamanho de `targetClasses`. Rodar em toda task seria imposto sem retorno proporcional.

**Restrição de território:** parte da materialização desta decisão vive em `.claude/`, que é território do humano e do `ai-engineer` (`CLAUDE.md`, raiz). O planner não implementa essa parte.

---

## Decisão

### 1. A métrica é `test strength`, não mutation score

`test strength` = mutantes mortos ÷ mutantes **cobertos** (exclui `NO_COVERAGE`). Mutation score cru fica como número de leitura no relatório, **nunca como critério**.

### 2. O piso é 80%

Abaixo disso, a entrega não passa no gate — quando o gate estiver ativo (ver §4).

### 3. A medição é sobre as classes alteradas pela task, não sobre a classe inteira nem sobre o projeto

Código pré-existente não entra no denominador. O gate responde *"o teste desta entrega verifica o que ela mudou?"*, não *"esta classe é boa?"*.

### 4. O gate é opcional e decidido por task, com pergunta obrigatória do planner

Não é gate global nem gate de merge por padrão. Ao escrever o plano de uma task de backend, o **planner pergunta ao humano** se aquela task adota o gate de mutação. O gate só vale quando o plano declara explicitamente que adota — mesma mecânica de `qa_required` (ADR 0017, ADR 0019).

A pergunta é obrigatória; a resposta é livre. Sem a obrigatoriedade da pergunta, o gate é opcional na teoria e inexistente na prática.

### 5. Sobrevivente classificado como equivalente **com demonstração escrita** não conta contra o piso

A demonstração precisa estar no status report e ser verificável — argumento de por que nenhuma entrada possível distingue original de mutante. "É equivalente" sem demonstração é lacuna de asserção não admitida, e conta contra.

### 6. Onde cada parte mora

| Parte | Local | Território |
|---|---|---|
| Regra vigente (métrica, piso, escopo) | `financas_bot_telegram/CLAUDE.md` | livre |
| Razão da decisão | esta ADR | livre |
| Como rodar, triagem, leitura do relatório | `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` §Camada 1.5 | livre |
| Obrigação do planner de perguntar | `.claude/agents/planner.md` | **humano + `ai-engineer`** |
| Campo no frontmatter do plano | `_TEMPLATE-plano.md` + `artifact-report-contract` | **humano + `ai-engineer`** |

As duas últimas linhas estão registradas como item #11.3 do `docs/plans/BACKLOG-evolucao-workflow.md`, pendentes de conversa com o `ai-engineer`. **Até que existam, o gate depende de o humano lembrar de pedi-lo** — a decisão está tomada, a automação não.

---

## Razões

- **`test strength` isola o sinal acionável.** O failure mode que interessa é "o teste executa a linha mas não verifica nada". `NO_COVERAGE` é outro problema (falta teste), com outro tratamento, e frequentemente é código inalcançável de propósito. Misturar os dois num número só produz um gate que pune defesa.
- **Medir só o alterado impede que a nota da task dependa de teste que ela não escreveu.** Uma task que toca uma classe com débito histórico seria reprovada por dívida alheia; a inversa também vale — herdar cobertura boa esconderia um teste novo fraco.
- **80% e não 90%+ porque o teto é ~93% e equivalentes são inevitáveis.** Piso alto demais transforma o gate em ritual de justificar exceção, que é como fitness function morre (ver `docs/aprendizado/testes-de-arquitetura-archunit.md`).
- **Opcional porque o custo é real e o valor é desigual.** O PIT paga em classe com lógica de decisão densa — aritmética, fronteiras, datas, agregação. Em CRUD, adapter e mapeamento ele gera pouco mutante e cobra a suíte inteira mesmo assim. Gate universal seria imposto.
- **A pergunta é obrigatória porque o opcional-por-omissão nunca acontece.** Mesmo princípio de ADR 0007: schema válido não garante verdade; aqui, opção disponível não garante uso.
- **Regra e razão em arquivos diferentes** porque têm leitores diferentes. `CLAUDE.md` é consultado por quem está implementando e precisa do número; a ADR é consultada por quem quer contestar o número em seis meses.

---

## Consequências

**Positivas:**

- O relatório do PIT passa a ter consequência. Existe um valor que reprova.
- O incentivo aponta para a direção certa: asserção sobre **valor** de argumento, não sobre ocorrência de chamada — que foi exatamente o que fez `PaymentRequestStrategy` e `PaymentProofStrategy` matarem 20/20 no piloto.
- Código defensivo inalcançável (`catch` de exceção checada que não ocorre) **não** é punido. Ninguém tem motivo para removê-lo em busca de nota.
- O escopo por classe alterada é o mesmo mecanismo que o item #7 do backlog da sprint 04 vai construir para Q2/Q3/Q6/Q7 — as duas coisas se reforçam em vez de duplicar.

**Negativas / custos:**

- **`test strength` é menos conhecido que mutation score.** Quem chegar depois vai ver "88%" no relatório e "80%" na regra e comparar coisas diferentes. Mitigado por nomear a métrica explicitamente em todo lugar; não eliminado.
- **A classificação de "equivalente" vira superfície de manipulação.** É a saída fácil para não admitir lacuna de teste. §5 exige demonstração escrita, e o Reviewer verifica — na QA-012 ele verificou as duas e ambas se sustentaram. É disciplina, não automação.
- **Enquanto o planner não perguntar automaticamente, o gate é letra morta.** Custo assumido conscientemente: a decisão fica registrada agora, a automação depende do `ai-engineer`.
- **"Classes alteradas pela task" ainda não tem mecanismo.** Hoje é `targetClasses` escrito à mão. Até o item #7 existir, a medição é manual e sujeita a erro de escopo.
- **Um piso numérico convida a otimizar para ele.** Teste escrito para matar mutante sem valor de negócio é possível. Contrapeso é o Reviewer, não a métrica.

**Métricas pra avaliar adoção:**

- **Baseline (QA-012, 2026-08-10):** test strength **90%** no total (37/41), por classe: `LegendaParser` 75%, `PaymentRequestStrategy` 100%, `PaymentProofStrategy` 100%, `MetaSignatureValidator` 85%.
- **Alvo:** nenhuma task que adote o gate entrega abaixo de 80%.
- **Critério de parada:** se em 3 tasks consecutivas com o gate ativo o resultado for ≥ 95% sem esforço adicional, o piso está baixo demais e deve subir. Se 2 tasks consecutivas precisarem de exceção justificada, o piso ou o escopo estão errados — revisar esta ADR, não conceder exceção recorrente.

---

## Alternativas consideradas

- **Mutation score cru a 85% (proposta inicial):** descartada pelo humano em 2026-08-10 após o levantamento mostrar que `MetaSignatureValidator` reprovaria com 79% por causa de um `catch` inalcançável. Manteria o número mais conhecido ao custo de punir código defensivo.
- **Gate global no CI:** descartado. O PIT não está no CI, o piso de custo é a suíte inteira, e o número não é comparável enquanto `targetClasses` for curado à mão. Reavaliar se e quando o item #7 do backlog entregar escopo por diff.
- **Piso por classe em vez de por task:** descartado — faz a nota depender de teste pré-existente que a task não escreveu.
- **Sem piso, só relatório (status quo pós-QA-012):** descartado — é a dor que esta ADR endereça. Instrumento de leitura sem consequência não muda comportamento.
- **Piso de 85%:** descartado pelo humano em favor de 80%, "para não travar muito". Coerente com o teto de ~93% das classes do piloto e com o risco de exceção virar rotina.

---

## Referências

- **ADR 0007** — reporting com gates e status report como output contract. Estabelece o princípio de que schema válido não garante verdade; aqui, opção disponível não garante uso.
- **ADR 0017** — prefixo `QA-NNN` para tasks de tooling de qualidade. A QA-012 nasce dele.
- **ADR 0019** — workflow reviewer→QA com loop de correção. Define a mecânica de gate por task que o §4 espelha.
- **Baseline e leitura interpretada:** `docs/sprints/04-instrumentacao-qualidade/status/QA-012-piloto-pit-mutation-testing.md` e a avaliação correspondente em `avaliacoes/`.
- **Substrato conceitual:** `docs/aprendizado/teste-mutante-e-pit.md` (cobertura × mutation score, mutantes equivalentes) e `docs/aprendizado/testes-de-arquitetura-archunit.md` (por que fitness function rígida demais vira allowlist decorativa).
- **Materializado em:** `financas_bot_telegram/CLAUDE.md` §"Critério de mutation testing" · `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` §Camada 1.5 · `docs/plans/BACKLOG-evolucao-workflow.md` §11.3 (pendente, território do `ai-engineer`).
- **Débitos correlatos:** `docs/PENDENCIAS-TECNICAS.md` — "Convenção `*IntegrationTest` não é verificada por nada", "Piso de custo do PIT é a suíte inteira".
