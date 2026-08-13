# Pendências técnicas — Sprint 04 (instrumentação e qualidade)

> **Registro de sprint.** Débitos levantados pelas tasks desta sprint, consolidados pelo planner a partir dos status reports e das avaliações do Reviewer.
>
> **Consolidação lida até:** `QA-012` (status + avaliação) e `QA-013` (status + avaliação, 2 rodadas). Próxima consolidação começa da QA-014.
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

---

## Itens metodológicos — ficam nesta sprint

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
