# YearMonth → DATE: como representar "mês" no banco SQL

## Contexto da dúvida

Apareceu na lógica do `FecharMesUseCase` (EVO-09, Sprint 03). O código usa
`primeiroDoMes` e `ultimoDoMes` e a dúvida foi: por que converter o mês de
referência para datas específicas?

## Resumo destilado

Em Java, `YearMonth` representa apenas mês + ano (ex: "maio 2026"). O banco de dados
**não tem esse tipo** — ele trabalha com `DATE` (que inclui dia, mês e ano). A
conversão é um passo de adaptação obrigatória para usar o mês em queries SQL.

```java
YearMonth mesReferencia = YearMonth.of(2026, 5);  // "maio 2026"

LocalDate primeiroDoMes = mesReferencia.atDay(1);          // → 2026-05-01
LocalDate ultimoDoMes   = mesReferencia.atEndOfMonth();    // → 2026-05-31
```

### Para que serve cada um

| Variável | Valor | Uso |
|---|---|---|
| `primeiroDoMes` | `2026-05-01` | Boundary de início nas queries; chave de idempotência no banco |
| `ultimoDoMes` | `2026-05-31` | Boundary de fim na query de vales do mês |

### Os três usos no FecharMesUseCase

**1. Buscar vales do mês:**
```sql
WHERE DATE(data_criacao) BETWEEN '2026-05-01' AND '2026-05-31'
```

**2. Checar quais adiantamentos descontar:**
```sql
WHERE data_inicio <= '2026-05-01'
-- "o adiantamento começou antes ou neste mês → cai neste fechamento"
```
Ex: adiantamento com `data_inicio = 2026-03-01` → 03-01 ≤ 05-01 → ✅ desconta.

**3. Gravar `mes_referencia` no Pedido FOLHA:**
```sql
INSERT ... SET mes_referencia = '2026-05-01'
```
Convenção: o mês é sempre representado como **dia 1** no banco.
Isso permite o `UNIQUE INDEX (funcionario_id, mes_referencia)` identificar meses únicos.

## Por que "dia 1" como convenção?

Porque o MySQL `UNIQUE INDEX` compara valores exatos. Para garantir que
"maio 2026" seja sempre um único valor no índice, precisamos de uma representação
canônica. `2026-05-01` (primeiro dia) é a convenção — poderia ser qualquer dia
fixo, mas o primeiro é o mais intuitivo.

Sem essa convenção, dois fechamentos do mesmo mês poderiam usar `2026-05-10` e
`2026-05-20` respectivamente — o `UNIQUE INDEX` não os veria como duplicata.

## Pontos-chave

- `YearMonth` é um tipo Java; `DATE` é o tipo SQL. São mundos diferentes.
- `atDay(1)` = "dê-me uma `LocalDate` com dia = 1 deste mês". Método da stdlib Java.
- `atEndOfMonth()` = "dê-me o último dia do mês" (considera anos bissextos automaticamente).
- A convenção "primeiro dia = representação do mês no banco" é necessária para que o
  `UNIQUE INDEX` funcione como proteção de idempotência.
- O `primeiroDoMes` serve tanto como boundary de query (limite de início) quanto como
  chave canônica do mês no banco.

## Para aprofundar

- `java.time.YearMonth`, `java.time.LocalDate` — pacote `java.time` (Java 8+)
- `YearMonth.atDay(int)` e `YearMonth.atEndOfMonth()` na Javadoc
- UNIQUE INDEX com NULL em MySQL — `NULL != NULL` em índices, permitindo múltiplos NULLs
