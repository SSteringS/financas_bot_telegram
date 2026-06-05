---
name: Checklist para refactor de renomeação/movimentação de pacote
description: Padrão de análise de gaps para refactors puros de pacote — quais checks empíricos bastam e por que não há gaps novos a preencher
type: project
---

Refactor puro de pacote (mover classe X de `a.b.c` para `a.b.d` sem mudar comportamento) não introduz gaps de cobertura novos. A análise de QA correta é puramente verificacional, não prescritiva.

**Why:** O caso de FIX-004 (mover `CategoriaPedido` e `FormaPagamento` de `domain.vo` para `domain.enums`) provou que para esta classe de refactor, propor testes novos é ruído. A própria compilação do test suite é o teste de regressão — se um import ficasse defasado, o build quebraria.

**How to apply:** Quando a task for refactor puro de pacote, o relatório de QA deve focar em **4 verificações empíricas**, todas mostrando zero pendência:

1. `Glob` no pacote antigo — deve retornar zero arquivos (pacote eliminado)
2. `Grep` por `<pacote-antigo>` em main e test — deve retornar zero matches (nenhum import esquecido)
3. `Grep` por string literal do pacote antigo (config, properties) — zero matches
4. `./mvnw test` (ou equivalente) — `Failures: 0, Errors: 0, Skipped: 0`

Se as 4 passarem, o veredito é "zero gaps novos, aprovar". Propor tipo novo de teste (unitário, integração, etc) é desperdício. O risco a verificar é puramente de import/classpath defasado, e a compilação cobre isso.

**Sinal de alerta:** Se algum teste foi marcado como `Skipped` ou se aparece NoClassDefFoundError em runtime de integração, aí sim há gap — provavelmente o refactor não pegou um arquivo de configuração ou uso por reflexão (ex: SpEL, `@Value` com classpath, jackson registerSubtypes).
