# BE-00B — Refatorar camada de persistência

**Data:** 2026-05-10
**Branch:** feature/api-consulta-pedidos-comprovantes
**Commit/PR:** (ver commit desta tarefa)
**Responsável (instância):** Claude Code (CLI) — back

---

## O que foi feito

- Criados POJOs puros `domain/model/PedidoPagamento.java` e `domain/model/Comprovante.java` (zero anotações de framework; Lombok apenas)
- Criadas entities JPA `adapters/out/persistence/entity/PedidoPagamentoEntity.java` e `ComprovanteEntity.java`
- Criados mappers `adapters/out/persistence/mapper/PedidoPagamentoMapper.java` e `ComprovanteMapper.java`
- `PedidoPagamentoJpaRepository` e `ComprovanteJpaRepository` refatorados para estender `JpaRepository<*Entity, Long>`
- `PedidoPagamentoRepositoryAdapter` e `ComprovanteRepositoryAdapter` refatorados para usar mappers
- Ports `PedidoPagamentoRepositoryPort` e `ComprovanteRepositoryPort` atualizados para `domain.model.*`
- Usecases `SalvarPedidoPagamentoUsecase` e `RegistrarComprovanteUsecase` atualizados para `domain.model.*`
- Services `SalvarPedidoPagamentoServiceImpl` e `RegistrarComprovanteServiceImpl` atualizados; `RegistrarComprovanteServiceImpl` usa builder do Lombok no lugar de `new Comprovante()` + `setPedido()`
- `PaymentRequestStrategy`: import atualizado + `new PedidoPagamento()` convertido para builder
- `PaymentProofStrategy`: import atualizado + `comprovanteSalvo.getPedido().getId()` → `comprovanteSalvo.getPedidoId()`
- Removidos: `domain/entity/PedidoPagamento.java`, `domain/entity/Comprovante.java`, `application/port/out/PedidoPagamentoRepository.java`, `application/port/out/ComprovanteRepository.java`

---

## Desvios do plano

- `ComprovanteRepositoryAdapter.orElseThrow` usa `new PedidoNaoEncontradoException(msg, null)` em vez de apenas `(msg)` — a exception exige `(String, Long chatId)` e o adapter não tem contexto de chat. Passado `null` explicitamente. Desvio inofensivo e correto.

---

## Decisões tomadas durante a execução

- `RegistrarComprovanteServiceImpl` usava `comprovante.setPedido(pedido)` passando o objeto inteiro. Com o POJO novo tendo apenas `pedidoId`, a linha virou `Comprovante.builder().pedidoId(pedidoId)...build()`. A busca do `PedidoPagamentoEntity` para montar a FK ficou encapsulada no `ComprovanteRepositoryAdapter`, conforme o plano.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- A pasta `domain/entity/` foi removida. A pasta `domain/model/` agora tem: `TelegramMediaGroup.java`, `PedidoPagamento.java`, `Comprovante.java`.
- BE-01 está liberada — pode criar `V2__add_requisitante_dates_categoria_auth.sql` e as entities correspondentes sem nenhum conflito.
- Smoke test via Telegram não realizado nesta sessão — app não foi reiniciado. Recomendo validar manualmente antes do próximo PR para main.

---

## Verificação `jakarta.persistence` fora de `adapters/out/persistence/`

```
$ grep -rl "jakarta.persistence" src/main/java/ | grep -v "adapters/out/persistence"
(nenhum resultado)
```

---

## Arquivos criados/modificados

**Criados:**
- `domain/model/PedidoPagamento.java`
- `domain/model/Comprovante.java`
- `adapters/out/persistence/entity/PedidoPagamentoEntity.java`
- `adapters/out/persistence/entity/ComprovanteEntity.java`
- `adapters/out/persistence/mapper/PedidoPagamentoMapper.java`
- `adapters/out/persistence/mapper/ComprovanteMapper.java`

**Modificados:**
- `adapters/out/persistence/PedidoPagamentoJpaRepository.java`
- `adapters/out/persistence/ComprovanteJpaRepository.java`
- `adapters/out/persistence/PedidoPagamentoRepositoryAdapter.java`
- `adapters/out/persistence/ComprovanteRepositoryAdapter.java`
- `application/port/out/PedidoPagamentoRepositoryPort.java`
- `application/port/out/ComprovanteRepositoryPort.java`
- `application/usecases/SalvarPedidoPagamentoUsecase.java`
- `application/usecases/RegistrarComprovanteUsecase.java`
- `application/services/SalvarPedidoPagamentoServiceImpl.java`
- `application/services/RegistrarComprovanteServiceImpl.java`
- `adapters/in/telegram/strategy/PaymentRequestStrategy.java`
- `adapters/in/telegram/strategy/PaymentProofStrategy.java`

**Removidos:**
- `domain/entity/PedidoPagamento.java`
- `domain/entity/Comprovante.java`
- `application/port/out/PedidoPagamentoRepository.java`
- `application/port/out/ComprovanteRepository.java`
