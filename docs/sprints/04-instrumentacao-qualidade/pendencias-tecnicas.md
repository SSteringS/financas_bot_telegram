# Pendências técnicas — Sprint 04 (instrumentação e qualidade)

> **Registro de sprint.** Débitos levantados pelas tasks desta sprint, consolidados pelo planner a partir dos status reports e das avaliações do Reviewer.
>
> **Consolidação lida até:** `QA-012`, `QA-013` e `QA-014` — status + avaliação de cada, incluindo as rodadas de delta-review. Próxima consolidação começa da QA-015.
>
> **O que fica aqui e o que vai para o global:** débito de **código ou de repositório** — que sobrevive ao fim da sprint — é promovido para [`../../PENDENCIAS-TECNICAS.md`](../../PENDENCIAS-TECNICAS.md) e aqui fica só o ponteiro. O que é **metodológico ou específico do ferramental desta sprint** fica aqui.

---

## Promovidos ao registro global

Ver [`docs/PENDENCIAS-TECNICAS.md`](../../PENDENCIAS-TECNICAS.md):

| Débito | Origem | Prioridade |
|---|---|---|
| `toUpperCase()`/`toLowerCase()` sem `Locale` — 5 ocorrências, **2 com bug real de corrupção silenciosa** | QA-013 débitos 1, 1b, 2 | **alta** |
| `AvoidCatchingGenericException` — 10 em produção + 1 em teste | QA-013 débito 3 | média |
| Dois métodos com complexidade alta confirmada por duas métricas | QA-013 débitos 4, 5 | média |
| Nenhuma ferramenta do repo mede conformidade arquitetural | QA-013 débito 6 | média |
| `MissingSerialVersionUID` — 25 ocorrências fora do ruleset por decisão | QA-013 débito 7 | baixa |
| Testcontainers não alcança o Docker na máquina de desenvolvimento | QA-013 débito 8 | **alta** |
| `SimplifyBooleanReturns` excluída por bug de versão do PMD 7.7.0 | Reviewer QA-013 | baixa |
| Convenção `*IntegrationTest` não é verificada por nada | QA-012 débito 3 | alta |
| Piso de custo do PIT é a suíte inteira | QA-012 débito 7 | média |
| PIT sem histórico incremental; medido em JVM diferente da do CI | QA-012 débitos 4, 5 | baixa |
| Repo não tem infraestrutura de captura de log em teste | QA-012 débito 2 | baixa |
| Código defensivo nunca exercitado — 3 gaps confirmados por PIT **e** JaCoCo | QA-014 débitos 1, 2, 3 | média |
| Cobertura da suíte de integração nunca medida — pacotes de infra **indeterminados** | QA-014 débitos 4, 5 | média |
| 8 classes em 0% no recorte unitário (exceções e DTOs de 1 a 6 linhas) | QA-014 | baixa |

---

## Itens metodológicos — ficam nesta sprint

### Nenhum agente instrui a medir cobertura — `cobertura_pct` pode continuar `na` por omissão

**Origem:** resíduo da deleção do `.codex/` pelo humano em 2026-08-13.

O `.codex/agents/qa-test-specialist.toml` era o **único** lugar do repositório que mandava um agente rodar cobertura (`./mvnw jacoco:report`, linhas 37 e 186). Com ele deletado, o comando enganoso sumiu — mas sobrou o buraco: **`.claude/agents/qa-test-specialist.md` nunca mencionou JaCoCo**, verificado por `grep -rn "jacoco" .claude/`, zero ocorrências.

**O que ainda funciona:** `docs/templates/_TEMPLATE-status.md:16` define a regra do campo, e o `ROTEIRO-TESTES-BACKEND.md` §Camada 1.6 tem o comando completo, com `clean`. Quem ler o template chega ao número.

**O que não funciona:** nada **instrui** o agente de QA a medir. A QA-014 instalou a ferramenta e destravou o campo, mas a causa original de `cobertura_pct: na` — ausência de instrução no agente que a Claude usa — **não mudou**.

**Fix aplicado em parte — 2026-08-16.** O humano autorizou explicitamente a mudança; o `ai-engineer` especificou e o planner aplicou. O `.claude/agents/qa-test-specialist.md` ganhou a seção `Coverage Measurement (conditional)` com o comando completo (com `clean`), os dois modos de falha silenciosa, a regra de reportar linha **e** branch juntas, e a política de publicar o número para o implementador copiar em vez de editar o status. Mais 4 guardrails e 3 itens de checklist. ⚠️ Config de agente é lida no spawn — **vale a partir da próxima sessão**.

**O que a mudança NÃO resolve — e é o ponto que decide o débito:** o `qa-test-specialist` só roda quando o plano declara `qa_required: true`. Task com `qa_required: false` continua sem medir nada. Ou seja, a correção acima vale **onde o QA roda**, e não é sozinha a correção de "`cobertura_pct` sai `na` em 100% dos reports".

**Parte 2, ainda aberta — escolher uma das duas:**

1. Passo equivalente em `.claude/agents/backend.md`. É o implementador quem preenche o campo e quem já roda o gate de mutação no mesmo padrão, então encaixa na estrutura existente. Se as duas cópias do texto divergirem, extrair para skill compartilhada.
2. Política do planner: `qa_required: true` obrigatório sempre que a task tocar classe de produção.

A opção 1 é mais barata. **Decisão do humano** — e a opção 1 mexe em `.claude/`, logo exige autorização nova.

**Fecha também, em parte, o débito 7 da QA-014.** O arquivo que ele citava (`.codex/agents/qa-test-specialist.toml`) já não existe; o que restava dele era exatamente a ausência coberta aqui.

**Prioridade:** média.

---

### Duas incoerências do `_TEMPLATE-status.md` reveladas pelo fix acima

**Origem:** `ai-engineer`, 2026-08-16, ao especificar a seção de cobertura. Nenhuma foi corrigida — as duas mudam schema ou comando de outra stack e merecem decisão própria.

1. **O schema do `cobertura_pct` não tem valor para "tentei medir e não deu".** `_TEMPLATE-status.md:16` admite `number | na`, e `na` significa "não tocou classe de produção". Suíte vermelha, instrumentação quebrada ou stack sem procedimento definido não têm como ser reportados sem escolher entre um número inventado e um `na` falso. A instrução do agente foi escrita usando `na` + motivo escrito ao lado, justamente para **não** introduzir um terceiro valor à revelia. Se o valor `unknown` for aceito, o template e o `artifact-report-contract` mudam juntos. **Decisão pendente.**

2. **`_TEMPLATE-status.md:20` manda o front rodar `jest --coverage`, e o front usa vitest.** Verificado em `frontend/package.json`: `"test": "vitest run"`, com `@vitest/coverage-v8`. É a mesma classe de erro que o `.codex` cometia com o `jacoco:report` — instrução que aponta para comando inexistente. **Prioridade:** média, e é uma linha.

---

### ~~`.codex/agents/qa-test-specialist.toml` manda rodar comando que falha em silêncio~~ ✅ resolvido

**Resolvido em 2026-08-13** — o humano **deletou o `.codex/` inteiro** ("nem uso o codex"). Os 8 arquivos de definição de agente saíram do repositório.

O débito era: depois da QA-014, `./mvnw jacoco:report` puro deixou de falhar e passou a devolver `BUILD SUCCESS` **sem gerar relatório** (`Skipping JaCoCo execution due to missing execution data file`) — pior que falhar alto. Sem o arquivo, não há mais instrução enganosa. O resíduo virou o item acima.

---

### ~~A regra de território do `CLAUDE.md` não cobre `.codex/`~~ ✅ resolvido por remoção

**Resolvido em 2026-08-13** pela mesma deleção. A regra do `CLAUDE.md` continua citando só `.claude/`, e agora isso está **correto** — é o único diretório de configuração de agente que existe.

> ⚠️ **Se um segundo harness voltar** (`.codex/`, `.cursor/`, `.github/copilot-instructions.md` ou equivalente), a lacuna reabre: a regra é escrita por caminho literal, não por categoria. Registrado para que a decisão seja consciente na volta, e não descoberta de novo por acidente.

---

### `append=true` é o default do JaCoCo e nada no repositório protege contra ele

**Origem:** QA-014, achado `M1` do Reviewer.

O agente do JaCoCo tem `append=true` por padrão: comando de cobertura **sem `clean`** mistura o `.exec` do run atual com os anteriores. O número reportado deixa de descrever a população de testes que ele diz descrever.

Foi o que derrubou a rodada 1 da revisão: o comando publicado no runbook produzia, em cenário realista, número que contradizia a definição que o próprio template acabara de escrever. Corrigido acrescentando `clean` ao comando documentado.

**Débito residual:** o default seguro continua dependendo de **disciplina de quem digita**. Avaliar fixar `<append>false</append>` na configuração do plugin — aí o comando errado passa a ser inofensivo em vez de silenciosamente errado.

**Prioridade:** média — é o segundo modo de falha silencioso desta ferramenta, junto com o `argLine`.

---

### Premissa obrigatória do item #7: regras sensíveis a resolução de tipo mudam com o estado do build

**Origem:** Reviewer da QA-013, achado `F4`, **medido**.

O item #7 do backlog vai calcular **Δ de violações** entre a tag do marco zero e o `HEAD`. Isso pressupõe que o número de violações depende só do código — e **não depende**: regras que fazem resolução de tipo consultam o `auxclasspath`, então o resultado muda conforme `target/classes` esteja populado.

Medição do Reviewer com `LawOfDemeter`:

| `target/classes` | Violações |
|---|---:|
| vazio | **2** |
| populado | **26** |

**O ruleset congelado é insensível a isso** — verificado: 22 violações nos dois casos. Mas a insensibilidade é propriedade **deste** ruleset, não do PMD.

**Consequência para quem desenhar o #7:** fixar e **declarar** se a medição roda com o projeto compilado. Se uma curadoria futura incluir regra sensível a tipo, um Δ medido com build sujo é ruído puro.

---

### `-Dpmd.rulesets` não existe: trocar de ruleset exige editar o `pom.xml`

**Origem:** QA-013, §Próximos passos.

O `maven-pmd-plugin` não expõe user property para `rulesets`. O `-Dpmd.includeTests` só funciona porque a task adicionou a property no `pom.xml`.

**Impacto no item #7:** medir com ruleset alternativo — para comparação metodológica, por exemplo — não é uma flag de linha de comando; é edição de arquivo versionado, que muda o hash. O Reviewer bateu nisso durante a revisão e teve que montar projetos-cópia para reproduzir os números.

---

### Baseline e hash só valem juntos

**Origem:** QA-013, §Ruleset congelado.

```
Q7_producao = 22    Q7_teste = 1
sha256(pmd-ruleset.xml) = 5100b68f387a0f0d4a8a6d8ba6540c715854709869790373df1118acb71755a9
```

**Δ medido contra outro ruleset não é comparável.** O hash mudou uma vez durante a revisão (correção do comentário após o achado `F1`); o valor acima é o que vale. Qualquer alteração no XML exige nova curadoria, hash novo e registro de a partir de quando a medição nova passa a valer.

**Regra que acompanha:** não silenciar violação com `@SuppressWarnings("PMD…")`. Supressão espalhada pelo código torna o `Q7` incomparável entre runs **sem deixar rastro no hash** — o ruleset continuaria idêntico enquanto a medição muda.

---

### `F6` do Reviewer: duas frases do status divergem do XML e do runbook já corrigidos

**Origem:** Reviewer da QA-013, rodada 2. Severidade **low**, **aberto**.

Nenhuma das duas é falsa no próprio contexto; as duas são resíduo do fix do `F1`, e as duas ficam no **status**, que é o artefato que o planner lê.

1. Item 6–7 da leitura interpretada mantém *"regra cuja remediação está errada"*, formulação que o `F2` pediu para trocar e que **já foi trocada no XML**. A tabela §Revisão independente do mesmo documento declara esse ponto como "Ajustado" — o documento se contradiz a 140 linhas de distância.
2. §Próximos passos mantém *"não precisa de suíte verde **nem de build**"*; o runbook já ganhou a ressalva do `F4`.

**Custo de fechar:** duas frases. **Não bloqueia nada** — o artefato congelado (XML) e o operacional (runbook) estão corretos, e o baseline não depende de nenhuma das duas. Fica registrado para não sumir; pode ser fechado de carona no próximo commit que tocar o arquivo.

---

### Encerrado nesta sprint

- ~~**Gate `testes` vermelho por Docker indisponível — causa desconhecida.**~~ ✅ **Respondido em 2026-08-13.** O CI rodou a suíte completa no runner (`Tests run: 422, Failures: 0, Errors: 0`, run `31727999562`) sobre o mesmo commit. **A causa é a máquina local, não o repositório** — os `*IntegrationTest` estão sãos. O que sobra é o débito de ambiente, promovido ao registro global.
