# BE-04 — DTOs da API REST + anotações OpenAPI (springdoc)

**Data:** 2026-05-11
**Branch:** feature/api-consulta-pedidos-comprovantes
**Responsável (instância):** Claude Code (CLI)

---

## O que foi feito

Criados todos os DTOs como Java records com anotações `@Schema` (springdoc), configurada a integração com springdoc-openapi e apagada a classe vazia `PaymentCategory`.

### Arquivos criados (DTOs — `application/dto/`):
- `PaginaDTO<T>` — wrapper genérico de paginação
- `PedidoResumoDTO` — item da listagem de pedidos
- `PedidoDetalheDTO` — detalhe completo de um pedido (separado do resumo para evolução independente)
- `ResumoStatusDTO` — nested: quantidade + total por status
- `ResumoMesDTO` — resumo do mês (pendentes + pagos)
- `AuthExchangeRequest` — request de exchange do link mágico, com Bean Validation (`@NotBlank`, `@Size`)
- `RequisitanteDTO` — dados do requisitante autenticado
- `AuthMeResponse` — resposta dos endpoints de auth
- `ErroDTO` — estrutura padrão de erro

### Arquivos criados (infra):
- `infra/OpenApiConfig.java` — bean `OpenAPI` com title "Finbot API", version "v1", description

### Arquivos modificados:
- `application.properties` — adicionada config springdoc (path, sorters, tryItOut, show-actuator)
- `application-prod.properties` — adicionado `springdoc.swagger-ui.enabled=false` e `springdoc.api-docs.enabled=false` pra proteger produção
- `application/dto/PaymentMessageDTO.java` — campo `categoria` trocado de `PaymentCategory` (apagada) para `TipoPagamento`

### Arquivo apagado:
- `application/dto/PaymentCategory.java` — classe vazia sem uso, conceito agora coberto por `TipoPagamento`

### Testes criados (7 classes, 12 casos):
- `PedidoResumoDTOTest` — 2 testes (serialização completa + dataPagamento null)
- `PedidoDetalheDTOTest` — 1 teste (todos os campos)
- `ResumoMesDTOTest` — 1 teste (nested ResumoStatusDTO)
- `AuthMeResponseTest` — 1 teste (nested RequisitanteDTO)
- `PaginaDTOTest` — 2 testes (com items + vazia)
- `ErroDTOTest` — 1 teste (código + mensagem)
- `AuthExchangeRequestTest` — 4 testes (token válido, em branco, curto, longo)

---

## Resultado dos testes

```
Tests run: 67, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 18.189 s
```

(55 testes anteriores + 12 novos de serialização/validação dos DTOs)

---

## Desvios do plano

- `PaymentMessageDTO` foi atualizado para substituir `PaymentCategory categoria` por `TipoPagamento categoria` — necessário porque o campo referenciava a classe apagada. Não era previsto no plano mas é consequência direta do passo 0.
- Os testes de `PedidoResumoDTOTest` e `PedidoDetalheDTOTest` precisaram de `.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)` no `ObjectMapper` — sem isso, `LocalDate` serializa como array `[2026,5,3]` em vez de `"2026-05-03"`. Não é um desvio de comportamento da API (Spring configura o `ObjectMapper` do contexto com esse flag desabilitado por padrão), é apenas um ajuste nos testes standalone que instanciam o `ObjectMapper` diretamente.

---

## Decisões tomadas durante a execução

- `PedidoResumoDTO` e `PedidoDetalheDTO` têm os mesmos campos hoje, mas foram mantidos como records separados conforme o plano — o detalhe pode adicionar campos de auditoria ou histórico futuramente sem quebrar o contrato da listagem.
- Não foi criado um `@Schema` global de segurança (Bearer/Cookie) — ainda não há auth implementado; será adicionado em BE-10.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos

- **BE-05** (`GET /api/v1/pedidos`) está liberada — primeiro endpoint REST de listagem.
- **BE-10** (auth_token + endpoint admin de geração de link mágico) também pode rodar em paralelo — as duas não têm dependência entre si.
- A pasta `application/mapper/` ainda está vazia — os mappers `PedidoPagamento → PedidoResumoDTO` e similares entram nas tarefas BE-05+.
