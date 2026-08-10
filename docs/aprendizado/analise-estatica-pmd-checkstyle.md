# Análise estática: PMD, Checkstyle e complexidade ciclomática

## Contexto da dúvida

Apareceu em 2026-08-10, revisando as métricas do experimento de alocação de modelo por papel. O `04-metricas.md` define **Q6 = complexidade ciclomática média dos métodos novos/alterados** (PMD) e **Q7 = violações de ruleset no diff** (PMD / Checkstyle). Nenhuma das duas ferramentas existe no repositório, e nenhuma era conhecida.

## Resumo destilado

**Análise estática** examina o código **sem executá-lo**. Teste roda o programa; análise estática lê a estrutura.

### As duas respondem perguntas diferentes

| | **Checkstyle** | **PMD** |
|---|---|---|
| Pergunta | *"Está escrito no padrão?"* | *"Está bem construído?"* |
| Foco | convenção, consistência, formatação | qualidade, smells, código propenso a bug |
| Exemplos | nome de variável, ordem de import, tamanho de linha, Javadoc ausente | variável não usada, `catch` vazio, método complexo demais, `String` concatenada em loop |
| Métricas | algumas | **calcula complexidade ciclomática** |
| Extra | — | **CPD** — detector de copy-paste |

**Terceira ferramenta do ecossistema, fora do escopo das métricas atuais:** **SpotBugs** analisa *bytecode* e procura bug provável (desreferência de nulo, recurso não fechado). PMD e Checkstyle leem o fonte; SpotBugs lê o compilado.

### Complexidade ciclomática

Conta caminhos independentes de execução num método. Começa em 1 e soma 1 a cada `if`, `else if`, `case`, `for`, `while`, `catch`, `&&`, `||`.

```java
void processar(Pedido p) {          // 1
    if (p == null) return;          // 2
    if (p.getValor() > 0            // 3
        && p.isAtivo()) {           // 4
        for (Item i : p.getItens()) // 5
            salvar(i);
    }
}
```

Complexidade 5 ⇒ **mínimo de 5 casos de teste** para cobrir todos os caminhos. É por isso que a métrica conversa direto com cobertura e mutation score.

**Limitação:** não distingue um `switch` de 10 casos (número alto, leitura fácil) de um aninhamento de 4 níveis (número parecido, ilegível). Métrica mais recente — *cognitive complexity* — tenta corrigir isso. Se a ciclomática for reportada sozinha, a limitação precisa ser declarada.

## Pontos-chave

- **Checkstyle = convenção. PMD = qualidade. SpotBugs = bug provável.** Três camadas distintas, não substitutas.
- **Para avaliar código gerado por LLM, PMD importa mais que Checkstyle.** Modelo formata bem; nome de variável e indentação não discriminam. O que discrimina é `catch` vazio, método inchado, duplicação — território do PMD.
- **Rodar as duas sem curar rulesets produz ruído e dupla contagem.** Cobrem território comum e podem discordar.
- **Ruleset default do PMD é barulhento.** Começar por subconjunto pequeno; as categorias `errorprone` e `design` são as que mais dizem sobre qualidade.
- **Complexidade ciclomática ≥ número mínimo de casos de teste** para cobertura de caminhos. Serve de sanity check contra cobertura reportada.
- **Análise estática não substitui teste.** Ela vê estrutura, não comportamento. Método com complexidade 1 pode estar completamente errado.

## Cuidados quando a métrica alimenta um experimento

- **Congelar o ruleset antes do primeiro run**, com hash no pré-registro. Alterar no meio torna a métrica incomparável entre runs.
- **Medir Δ contra o commit base, não valor absoluto.** O código existente já tem violações; medir o projeto inteiro polui o sinal do diff.
- **Não tornar bloqueante no CI durante o experimento.** Build que falha por violação faz o agente otimizar para o linter — interferência direta no que está sendo medido. Relatório sim, gate não.

## Impacto no workflow deste repositório

O `PRE-MERGE-CHECKLIST` declara hoje `lint: na` como oficial para o backend. Instalar PMD **muda o workflow**, não é só adicionar plugin: exige decidir se passa a existir gate de lint, em que termos, e se é bloqueante.

## Pra aprofundar

- Categorias de ruleset do PMD: `bestpractices`, `codestyle`, `design`, `documentation`, `errorprone`, `multithreading`, `performance`, `security`
- CPD (copy-paste detector) e limiar de tokens duplicados
- Cognitive complexity vs cyclomatic complexity — o que cada uma captura
- SpotBugs e as anotações JSR-305 para suprimir falso positivo
- Baseline de violações: como adotar linter em código legado sem parar o mundo
