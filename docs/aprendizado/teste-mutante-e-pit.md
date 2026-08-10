# Teste mutante e PIT (pitest)

## Contexto da dúvida

Apareceu em 2026-08-10, na revisão das métricas do experimento de alocação de modelo por papel. O `04-metricas.md` define **Q3 = mutation score nas classes tocadas (mutantes mortos ÷ gerados)**, coletado por **PIT**, e descreve a métrica como *"proxy de qualidade de teste muito superior a cobertura"*. A dúvida foi: o que é teste mutante, e o que é essa ferramenta que precisa ser instalada.

## Resumo destilado

### O problema

Cobertura mede **qual código foi executado** pelos testes. Não mede se o teste **verifica** alguma coisa. Um teste sem nenhuma asserção dá cobertura idêntica a um teste rigoroso:

```java
@Test
void calculaTotal() {
    calculadora.calcular(pedidos);   // 100% de cobertura
}                                     // zero verificação
```

Trocar `+` por `-` no código de produção não quebra esse teste.

### Como o teste mutante resolve

A ferramenta **altera o código de propósito** e verifica se algum teste percebe.

1. Gera **mutantes** — cópias do código com exatamente uma alteração pequena cada: `+`→`-`, `>`→`>=`, `return x`→`return 0`, `if (a)`→`if (!a)`, remoção de chamada `void`.
2. Roda a suíte contra cada mutante.
3. Algum teste falhou → mutante **morto** (`KILLED`). Bom sinal.
4. Todos passaram → mutante **sobreviveu** (`SURVIVED`). Existe mudança de comportamento que ninguém detecta.

```
mutation score = mortos ÷ gerados
```

Cada sobrevivente aponta uma linha que pode ser alterada sem nenhum teste reclamar.

### Estados no relatório

- **`SURVIVED`** — o achado que interessa: asserção ausente ou fraca.
- **`NO_COVERAGE`** — linha não executada por teste nenhum. Problema de cobertura, não de asserção.
- **`TIMED_OUT`** — conta como morto; a mutação gerou loop infinito, e isso é detecção.
- **Mutante equivalente** — a mutação não altera comportamento observável, logo é **impossível de matar**. Infla o denominador. Por isso **100% é inalcançável e não deve virar meta**.
- **Test strength** — métrica irmã: mortos ÷ mutantes **cobertos**. Separa "asserção fraca" de "código não testado". Vale coletar junto.

## PIT (pitest)

Ferramenta madura de mutation testing na JVM. Duas decisões de projeto explicam a adoção:

- **Muta bytecode**, não código-fonte — não recompila, muito mais rápido que as gerações anteriores.
- **Roda só os testes que cobrem a linha mutada** — faz uma passada de cobertura antes. Sem isso o custo seria `mutantes × suíte inteira`, inviável.

### Configuração Maven

```xml
<plugin>
  <groupId>org.pitest</groupId>
  <artifactId>pitest-maven</artifactId>
  <dependencies>
    <dependency>
      <groupId>org.pitest</groupId>
      <artifactId>pitest-junit5-plugin</artifactId>
    </dependency>
  </dependencies>
  <configuration>
    <targetClasses><param>br.com.satyan...*</param></targetClasses>
    <excludedTestClasses><param>*IntegrationTest</param></excludedTestClasses>
    <outputFormats><param>XML</param><param>HTML</param></outputFormats>
  </configuration>
</plugin>
```

Execução: `mvn org.pitest:pitest-maven:mutationCoverage`.
O `pitest-junit5-plugin` é **obrigatório** — sem ele o PIT não reconhece JUnit 5.

## Pontos-chave

- **Cobertura responde "executou?"; mutation score responde "teria percebido se quebrasse?".**
- **Excluir testes de integração do PIT é o uso correto, não atalho.** Neste repo os `*IntegrationTest` sobem MySQL real via Testcontainers; o PIT reexecuta a suíte relevante uma vez por mutante, e um container no caminho torna o run inviável. Mutation testing serve pra medir força de asserção em **lógica**, não integração com banco.
- **Escopar por classes tocadas.** Rodar no projeto inteiro é caro e dilui o sinal com código não alterado. Derivar de `git diff --name-only` contra o commit base e injetar em `targetClasses`.
- **100% não é meta** — mutantes equivalentes garantem que não dá.
- **É lento por natureza.** Defesas: `timeoutConstant`, escopo reduzido, conjunto menor de mutators.
- **Por que importa em avaliação de código gerado por LLM:** modelo de linguagem tem viés de escrever teste que **passa**, não teste que **pega bug** — asserção tautológica, mock que verifica o próprio mock, `assertNotNull` em objeto recém-construído. Cobertura dá nota alta pra tudo isso; mutation score não. É exatamente o failure mode que o Q3 existe pra capturar.

## Pra aprofundar

- Conjuntos de mutators do PIT: `DEFAULTS` vs `STRONGER` vs `ALL`, e o custo de cada um
- Análise incremental do PIT (histórico entre execuções) para reduzir tempo em CI
- Relação entre mutation score e *test smells* — asserção roulette, mystery guest
- Por que mutantes equivalentes são indecidíveis no caso geral (redução ao problema da parada)
