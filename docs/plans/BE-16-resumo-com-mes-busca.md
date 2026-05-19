# BE-16 — Resumo com mês e busca opcionais

## Contexto

O endpoint `GET /api/v1/resumo` hoje só devolve o mês corrente e não aceita parâmetros. Isso causa dois bugs visíveis no front (revisão FE-07 e FE-08):

- **Bug A (contadores):** o `FiltroStatus` da Home calcula `pendente`/`pago` em cima de `data.items` da listagem, que já vem filtrada pelo status ativo. Quando o usuário filtra por "Pendente", o contador "Pago" zera — não porque não existe, mas porque sumiu do payload. A solução estrutural é os contadores virem de uma agregação independente do filtro de status.
- **Bug B (header travado):** o cabeçalho da Home mostra "Olá Pedro, 3 pedidos pendentes" mesmo quando o usuário troca pra abril no `SeletorMes`. Causa: o endpoint sempre devolve o mês corrente.

Ambos os bugs pedem o mesmo recurso: **agregação por status, parametrizada por mês e busca**. O endpoint `/api/v1/resumo` já é o ponto natural pra isso — basta parametrizar.

## Branch

Criar nova branch a partir de `develop`:

```
feature/be-16-resumo-com-mes-busca
```

Não usar a `feature/backend-polish-evo07` (que está esperando review separado).

## Contrato novo

```
GET /api/v1/resumo?mes=YYYY-MM&busca=texto
```

### Query params

| Param | Tipo | Obrigatório | Default | Descrição |
|---|---|---|---|---|
| `mes` | string | não | mês corrente (`YearMonth.now()`) | Formato `YYYY-MM`. Filtra `dataPedido` entre o primeiro e o último dia do mês. |
| `busca` | string | não | sem filtro | Texto pra filtrar `descricao` (contém, case-insensitive). Mesma semântica do `busca` em `GET /api/v1/pedidos`. |

### Validação

- `mes` em formato inválido → `400` com erro estruturado (mesmo padrão dos outros endpoints). Aceitar parsing via `YearMonth.parse(mes)`.
- `mes` vazio ou ausente → default = mês corrente.
- `busca` vazia → tratar como ausente (sem filtro).

### Response (200)

```json
{
  "mes": "2026-05",
  "todos":     { "quantidade": 6, "total": 7227.40 },
  "pendentes": { "quantidade": 3, "total": 5239.90 },
  "pagos":     { "quantidade": 3, "total": 1987.50 }
}
```

Mudanças em relação ao contrato atual:
- Campo `mesAtual` renomeado para `mes` (mais correto agora que o endpoint aceita qualquer mês).
- Novo campo `todos: { quantidade, total }` — agregado do recorte inteiro (pendentes + pagos do filtro mes + busca).

Não há mudança de status/erros além do 400 para mês inválido.

## Implementação

### Controller
- Adicionar `@RequestParam(required = false) String mes` e `@RequestParam(required = false) String busca`.
- Parsing/validação de `mes` via `YearMonth.parse(...)` dentro de um try/catch que devolve 400.
- Passar `YearMonth` + `busca` (pode ser null/blank) pro service.

### Service
- Calcular `de` = primeiro dia do mês, `ate` = último dia.
- Repositório agrega: `SUM(valor)` e `COUNT(*)` agrupados por `status`, filtrando `dataPedido BETWEEN de AND ate` e (se busca não for nula/branca) `LOWER(descricao) LIKE %lower(busca)%`. Considerar também o `requisitanteId` autenticado, conforme padrão dos outros endpoints.
- Montar DTO: `pendentes` e `pagos` vêm das linhas do `GROUP BY`; `todos` = soma dos dois.
- Se algum status não tiver linha, mandar `quantidade: 0, total: 0`.

### DTO de resposta
- Atualizar `ResumoMes` (ou nome equivalente) com: `mes`, `todos`, `pendentes`, `pagos`.

### OpenAPI / Swagger
- Anotar `@Operation` no endpoint com descrição dos novos params.
- `@Parameter` em `mes` e `busca` com exemplo e descrição.
- Conferir que a UI do Swagger mostra os params como opcionais.

## Testes

### Unitários (service)
- `default` (sem mes, sem busca) → traz mês corrente sem filtro de texto.
- `com mes específico` → traz dados só do mês pedido.
- `com busca` → filtra `descricao`.
- `com mes + busca` → combina os dois.
- Mês com 0 pedidos → todos os campos devolvem `0` (não null).
- Pedidos com mistura de status → contagens e totais corretos.

### Integração (Testcontainers)
- Sequência: insere 4 pedidos (2 maio pendentes, 1 maio pago, 1 abril pago) → chama `GET /api/v1/resumo?mes=2026-05` → confere payload completo.
- Chama com `busca` específica → confere que filtra.
- Chama com `mes` inválido (`2026-13`) → confere 400.
- Chama sem mes (default) → confere que devolve dados do mês corrente.

## Critérios de aceite

- [ ] Endpoint responde corretamente nos 4 cenários (default / mes / busca / mes+busca)
- [ ] Mês inválido → 400 com erro estruturado
- [ ] Resposta inclui `todos`, `pendentes`, `pagos` com `quantidade` e `total` numéricos (nunca null)
- [ ] Swagger UI mostra params opcionais com descrição
- [ ] Todos os testes (existentes + novos) passam
- [ ] `mvn clean verify` passa local
- [ ] Sem regressão em outros endpoints

## Coordenação com o front

O Claude do front está trabalhando em paralelo na branch `feature/frontend-fase3-completa` consumindo este mesmo contrato via MSW mock. O contrato definido aqui é a fonte de verdade — qualquer alteração precisa ser sincronizada antes de mergear.

## Referências

- Bug A documentado em `docs/aprendizado/react-tanstack-query.md` (FE-07)
- Bug B documentado em `docs/aprendizado/react-estado-derivado-e-composicao-de-hooks.md` (FE-08)
- Endpoint atual: ver `ResumoController` (ou equivalente) — explorar antes de implementar
