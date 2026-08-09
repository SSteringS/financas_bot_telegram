# RTU — BE-01a: Relatório de Testes Unitários

**Data:** 2026-05-10
**Branch:** feature/api-consulta-pedidos-comprovantes
**Resultado geral:** `Tests run: 55, Failures: 0, Errors: 0, Skipped: 0` — 14s

---

## Sumário por classe

| # | Classe testada | Arquivo de teste | Casos |
|---|---|---|---|
| 1 | `PaymentRequestStrategy` | `strategy/PaymentRequestStrategyTest` | 6 |
| 2 | `PaymentProofStrategy` | `strategy/PaymentProofStrategyTest` | 7 |
| 3 | `UpdateOrchestratorService` | `service/UpdateOrchestratorServiceTest` | 4 |
| 4 | `SalvarPedidoPagamentoServiceImpl` | `services/SalvarPedidoPagamentoServiceImplTest` | 5 |
| 5 | `RegistrarComprovanteServiceImpl` | `services/RegistrarComprovanteServiceImplTest` | 4 |
| 6 | `PedidoPagamentoMapper` | `mapper/PedidoPagamentoMapperTest` | 5 |
| 7 | `ComprovanteMapper` | `mapper/ComprovanteMapperTest` | 5 |
| 8 | `PedidoPagamentoRepositoryAdapter` | `persistence/PedidoPagamentoRepositoryAdapterTest` | 3 |
| 9 | `ComprovanteRepositoryAdapter` | `persistence/ComprovanteRepositoryAdapterTest` | 3 |
| 10 | `TelegramWebhookController` | `controller/TelegramWebhookControllerTest` | 4 |
| 11 | `GlobalTelegramExceptionHandler` | `exceptionhandler/GlobalTelegramExceptionHandlerTest` | 8 |
| 12 | `FinancasBotTelegramApplication` | `FinancasBotTelegramApplicationTests` | 1 (contexto Spring) |
| **Total** | | | **55** |

---

## 1. PaymentRequestStrategy — 6 casos

Regex `^(\d+([.,]\d{1,2})?) (.+)$` aplicada à legenda da foto.

| Método | O que verifica |
|---|---|
| `deveSuportarLegendaComValorEDescricao` | `supports()` retorna `true` para `"150.00 Almoço no restaurante"` |
| `deveSuportarValorComVirgula` | `supports()` retorna `true` para `"150,50 Jantar"` (vírgula como separador decimal) |
| `naoDeveSuportarLegendaDeComprovante` | `supports()` retorna `false` para `"#123 PIX"` (formato de comprovante) |
| `naoDeveSuportarLegendaSemValor` | `supports()` retorna `false` para `"Apenas descrição"` (sem valor numérico) |
| `deveProcessarPedidoComTodosOsDados` | `process()` monta `PedidoPagamento` com valor, descrição, `telegramUserId`, `fileId`, `imagemUrl`, `status=PENDENTE` e delega ao usecase |
| `deveConverterValorComVirgulaParaBigDecimal` | `process()` converte `"99,90"` → `BigDecimal("99.90")` (troca de vírgula por ponto) |

---

## 2. PaymentProofStrategy — 7 casos

Regex `#(\d+)\s+(.+)` aplicada à legenda.

| Método | O que verifica |
|---|---|
| `deveSuportarLegendaDeComprovante` | `supports()` retorna `true` para `"#123 pix"` |
| `deveSuportarComTipoPagamentoComposto` | `supports()` retorna `true` para `"#42 transferencia bancaria"` (tipo com espaços) |
| `naoDeveSuportarLegendaDePedido` | `supports()` retorna `false` para `"150.00 Almoço"` |
| `naoDeveSuportarLegendaSemHashId` | `supports()` retorna `false` para `"123 pix"` (sem `#`) |
| `deveProcessarComprovanteComSucesso` | `process()` faz download, upload S3, chama usecase com `pedidoId=123`, `tipoPagamento="PIX"`, envia mensagem de sucesso |
| `deveLancarInvalidCaptionExceptionQuandoFormatoInvalido` | `process()` lança `InvalidCaptionException` para legenda que não casa com o regex (sem download do Telegram) |
| `deveConverterTipoPagamentoParaMaiusculo` | `process()` envia `"TED"` (maiúsculo) mesmo quando legenda tem `"ted"` |

---

## 3. UpdateOrchestratorService — 4 casos

Percorre a lista de strategies, executa a primeira que retorna `supports() = true`.

| Método | O que verifica |
|---|---|
| `deveExecutarPrimeiraStrategyQueSuporta` | Quando `strategyA.supports()=true`, só `strategyA.process()` é chamado; `strategyB` não é tocado |
| `deveExecutarSegundaStrategyQuandoPrimeiraFalhar` | Quando `strategyA=false` e `strategyB=true`, executa `strategyB` |
| `deveLancarInvalidMessageFormatExceptionQuandoNenhumaStrategySuporta` | Nenhuma strategy aceita → lança `InvalidMessageFormatException` com o `chatId` correto |
| `deveLancarExcecaoComListaDeStrategiesVazia` | Lista vazia → lança `InvalidMessageFormatException` |

---

## 4. SalvarPedidoPagamentoServiceImpl — 5 casos

Valida o pedido antes de delegar ao repository port.

| Método | O que verifica |
|---|---|
| `deveSalvarPedidoValido` | Pedido com todos os campos válidos é persistido; retorna o objeto salvo |
| `deveLancarExcecaoParaValorNulo` | `valor=null` → `BusinessRuleException`; repository **não** é chamado |
| `deveLancarExcecaoParaValorZero` | `valor=0` → `BusinessRuleException` (valor deve ser > 0) |
| `deveLancarExcecaoParaDescricaoEmBranco` | `descricao="   "` → `BusinessRuleException`; repository **não** é chamado |
| `deveLancarExcecaoParaTelegramUserIdNulo` | `telegramUserId=null` → `BusinessRuleException` |

---

## 5. RegistrarComprovanteServiceImpl — 4 casos

Busca o pedido, valida status, atualiza para PAGO, persiste o comprovante.

| Método | O que verifica |
|---|---|
| `deveRegistrarComprovanteEAtualizarStatusDoPedido` | Pedido PENDENTE encontrado → status atualizado para PAGO via `pedidoRepository.save()` → comprovante salvo → retorna comprovante |
| `deveLancarExcecaoQuandoPedidoNaoEncontrado` | `pedidoId` inexistente → `PedidoNaoEncontradoException` com o id na mensagem; `comprovanteRepository` **não** é chamado |
| `deveLancarExcecaoQuandoPedidoJaEstaPago` | Pedido com `status=PAGO` → `BusinessRuleException` com mensagem "pago"; `comprovanteRepository` **não** é chamado |
| `deveSalvarComprovanteComFileIdEImagemUrl` | Verifica que o `Comprovante` passado ao repository tem `fileIdTelegram`, `imagemUrl` e `pedidoId` corretos |

---

## 6. PedidoPagamentoMapper — 5 casos

Converte entre `PedidoPagamento` (domain) e `PedidoPagamentoEntity` (JPA).

| Método | O que verifica |
|---|---|
| `deveMappearEntityParaDomain` | Todos os campos (`id`, `telegramUserId`, `telegramMessageId`, `fileIdTelegram`, `imagemUrl`, `valor`, `descricao`, `status`, `dataCriacao`) são copiados corretamente de entity → domain |
| `deveMappearDomainParaEntity` | Campos copiados de domain → entity (`id`, `telegramUserId`, `valor`, `status`, `descricao`) |
| `deveRetornarNullParaEntityNula` | `toDomain(null)` retorna `null` sem NPE |
| `deveRetornarNullParaDomainNulo` | `toEntity(null)` retorna `null` sem NPE |
| `devePreservarRoundTrip` | `toDomain(toEntity(domain))` preserva todos os campos — conversão de ida e volta é sem perda |

---

## 7. ComprovanteMapper — 5 casos

Converte entre `Comprovante` (domain, usa `pedidoId: Long`) e `ComprovanteEntity` (JPA, usa `@ManyToOne PedidoPagamentoEntity`).

| Método | O que verifica |
|---|---|
| `deveMappearEntityComPedidoParaDomain` | `entity.pedido.id` → `domain.pedidoId`; demais campos (`fileIdTelegram`, `imagemUrl`, `tipoPagamento`, `dataPagamento`) copiados |
| `deveMappearDomainParaEntityComPedidoEntity` | `domain` + `pedidoEntity` passados → entity com `entity.pedido` sendo a mesma instância de `pedidoEntity` |
| `deveTratarEntityComPedidoNuloSemNpe` | Entity com `pedido=null` → `domain.pedidoId=null` (sem NPE) |
| `deveRetornarNullParaEntityNula` | `toDomain(null)` → `null` |
| `deveRetornarNullParaDomainNulo` | `toEntity(null, pedidoEntity)` → `null` |

---

## 8. PedidoPagamentoRepositoryAdapter — 3 casos

Orquestra `mapper` + `jpaRepository` para `save` e `findById`.

| Método | O que verifica |
|---|---|
| `deveSalvarPedidoPassandoPeloMapper` | `save(domain)` → `mapper.toEntity` → `jpaRepository.save` → `mapper.toDomain`; cada chamada verificada com `verify()` |
| `deveBuscarPedidoPorIdExistente` | `findById(id)` existente → `Optional.of(domain)` mapeado pela entity encontrada |
| `deveRetornarVazioQuandoPedidoNaoExiste` | `findById(id)` inexistente → `Optional.empty()` |

---

## 9. ComprovanteRepositoryAdapter — 3 casos

Busca `PedidoPagamentoEntity` pelo id antes de salvar (necessário para a FK JPA).

| Método | O que verifica |
|---|---|
| `deveSalvarComprovanteBuscandoPedidoEntityParaFK` | `save(comprovante)` → busca `pedidoEntity` no `pedidoJpaRepository` → passa para `mapper.toEntity(comprovante, pedidoEntity)` → `jpaRepository.save` → `mapper.toDomain` |
| `deveLancarExcecaoQuandoPedidoNaoExisteAoSalvarComprovante` | `pedidoId` não encontrado → `PedidoNaoEncontradoException` com o id na mensagem |
| `devePassarPedidoEntityCorretaParaMapper` | Verifica que a instância exata do `pedidoEntity` recuperada é passada para `mapper.toEntity` (não outra instância) |

---

## 10. TelegramWebhookController — 4 casos

Valida o payload e autoriza o usuário antes de delegar ao orchestrator. Construído via `new TelegramWebhookController(mock, List.of(...))` — sem Spring.

| Método | O que verifica |
|---|---|
| `deveRetornar200ParaUsuarioAutorizado` | UserId na lista de permitidos → HTTP 200, `orchestratorService.process()` chamado |
| `deveLancarUnauthorizedExceptionParaUsuarioNaoAutorizado` | UserId **fora** da lista → `UnauthorizedUserException` com o `chatId` correto |
| `deveLancarInvalidUpdateExceptionParaUpdateSemMessage` | `update.message=null` → `InvalidUpdateException` |
| `deveLancarInvalidUpdateExceptionParaMessageSemFrom` | `message.from=null` → `InvalidUpdateException` |

---

## 11. GlobalTelegramExceptionHandler — 8 casos

Cada `@ExceptionHandler` chamado diretamente — sem MockMvc, sem Spring.

| Método | O que verifica |
|---|---|
| `deveRetornarOkEEnviarMensagemParaUnauthorizedUserException` | HTTP 200; `sendMessage(chatId, ...)` chamado |
| `deveRetornarOkSemEnviarMensagemParaInvalidUpdateException` | HTTP 200; `sendMessage` **não** é chamado (sem chatId disponível) |
| `deveRetornarOkEEnviarMensagemParaInvalidMessageFormatException` | HTTP 200; `sendMessage(chatId, ...)` chamado |
| `deveRetornarOkEEnviarMensagemParaPedidoNaoEncontradoException` | HTTP 200; `sendMessage(chatId, ...)` chamado |
| `deveRetornarOkEEnviarMensagemParaPhotoProcessingException` | HTTP 200; `sendMessage(chatId, ...)` chamado |
| `deveRetornarOkEEnviarMensagemParaInvalidCaptionException` | HTTP 200; `sendMessage(chatId, ...)` chamado |
| `deveRetornarOkEEnviarMensagemParaBusinessRuleException` | HTTP 200; `sendMessage(chatId, ...)` chamado |
| `deveRetornar500EEnviarMensagemParaDatabaseException` | **HTTP 500**; `sendMessage(chatId, ...)` chamado (única exception que retorna 5xx) |

---

## 12. FinancasBotTelegramApplicationTests — 1 caso (contexto Spring)

| Método | O que verifica |
|---|---|
| `contextLoads` | Contexto Spring sobe sem erro com H2 in-memory (`@ActiveProfiles("test")`), Flyway desligado, `S3Template` mockado via `@MockBean` |

---

## Legenda de cobertura

- **Caminho feliz:** testado em todas as 11 classes
- **Erro / exceção esperada:** testado em 7 de 11 (strategies, orchestrator, services, adapters)
- **Caso borda null-safety:** testado nos 2 mappers e no `ComprovanteRepositoryAdapter`
- **Isolamento:** 0 chamadas reais a banco, S3 ou Telegram — 100% Mockito
