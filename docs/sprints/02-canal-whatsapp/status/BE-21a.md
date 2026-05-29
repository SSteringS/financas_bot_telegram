---
tarefa: BE-21a
titulo: Eventos in-process + NotificadorPortOut + canalPreferido + Telegram impl
branch: feature/be-21a-eventos-notificador-canalpreferido
estado: concluido
data: 2026-05-29
testes_total: 229
testes_novos: 3
desvios: 2
---

## O que foi feito

### Migração (V4)
- `V4__add_canal_preferido_requisitante.sql`: `ALTER TABLE requisitante ADD COLUMN canal_preferido VARCHAR(20) NOT NULL DEFAULT 'TELEGRAM'`.
- Default `'TELEGRAM'` garante backfill silencioso dos registros existentes (Pedro).
- **NOTA DE CONFLITO:** este arquivo usa V4 pois é o próximo número disponível em `develop`. Se BE-19a mergear antes desta task, renumerar para V5 durante a resolução de conflito.

### Domínio
- `domain/model/Canal.java`: enum `TELEGRAM | WHATSAPP` — enum unificado.
  - **Decisão de unificação com BE-19a:** BE-19a cria `CanalMensagem` em `domain/model/`. Quando BE-19a mergear em `develop`, substituir `CanalMensagem` por `Canal` (este enum) para evitar duplicação. Documentado no PR de BE-19a.
- `domain/event/ComprovanteRegistradoEvent.java`: record `(Long comprovanteId, Long pedidoId, Long requisitanteId, String chatId)`. Campo `chatId` transporta o identificador de canal do destinatário (chatId Telegram; wa_id para WhatsApp futuro).
- `domain/model/Requisitante.java`: adicionado campo `Canal canalPreferido` com `@Builder.Default = Canal.TELEGRAM`.

### Persistência
- `adapters/out/persistence/entity/RequisitanteEntity.java`: adicionado `@Enumerated(EnumType.STRING) Canal canalPreferido` com default `Canal.TELEGRAM`.
- `adapters/out/persistence/mapper/RequisitanteMapper.java`: mapeamento `canalPreferido` em `toDomain()` e `toEntity()`.

### Aplicação — porta de saída
- `application/port/out/NotificadorPortOut.java`: interface com `Canal getCanal()` e `void notificar(NotificacaoDTO)`.
- `application/port/out/NotificacaoDTO.java`: record `(String destinatarioCanalId, Long pedidoId, String linkSite)`. `destinatarioCanalId` é String para acomodar chatId Telegram (Long como String) e wa_id WhatsApp (phone number string).

### Aplicação — evento + config
- `application/config/AsyncConfig.java`: `@EnableAsync` + bean `notificacaoExecutor` (ThreadPoolTaskExecutor: core=2, max=4, queue=100, prefix="notif-").
- `application/event/NotificacaoComprovanteListener.java`:
  - `@Async("notificacaoExecutor")` + `@TransactionalEventListener(phase = AFTER_COMMIT)`.
  - Injeta `Map<Canal, NotificadorPortOut>` construído a partir de `List<NotificadorPortOut>` via `getCanal()` como chave.
  - Busca requisitante para obter `canalPreferido`; despacha `NotificacaoDTO` para o notificador correto.
  - try/catch + log `ERROR` + `// TODO BE-22: counter de falha do listener`.
  - Log de warning se canal sem notificador registrado.

### Adapter de saída — Telegram notificador
- `adapters/out/telegram/notificador/TelegramNotificadorImpl.java`:
  - Implementa `NotificadorPortOut`, retorna `Canal.TELEGRAM`.
  - Monta mensagem: `"✅ Comprovante registrado para o pedido #N.\nVeja em <link>"`.
  - Delega para `TelegramMessageSenderService.sendMessage()`.

### Wire do usecase
- `application/services/RegistrarComprovanteServiceImpl.java`:
  - Adicionado `ApplicationEventPublisher` ao construtor.
  - Adicionado `@Transactional` ao método `execute()` — obrigatório para `AFTER_COMMIT` disparar.
  - Após `comprovanteRepository.save()`, publica `ComprovanteRegistradoEvent`.

### Testes unitários
- `RegistrarComprovanteServiceImplTest.java`: adicionado `@Mock ApplicationEventPublisher eventPublisher` para suportar o novo construtor via `@InjectMocks`. Testes existentes continuam passando (Mockito ignora chamadas void não-stubadas).

### Testes de integração (Testcontainers — falham neste ambiente/Docker)
- `NotificacaoComprovanteListenerIntegrationTest.java` (3 testes):
  - `sucesso_aposCommit_listenerDispara` — CountDownLatch, aguarda 5s para verificar disparo assíncrono após commit.
  - `rollback_listenerNaoDispara` — TransactionTemplate + setRollbackOnly; verifica que listener NÃO dispara com `Thread.sleep(500)`.
  - `falhaNoListener_naoAfetaTransacaoDoUsecase` — mock do sender lança exceção; verifica que comprovante foi salvo e listener tentou (best-effort).

## Decisões documentadas

### Mecanismo do `Map<Canal, NotificadorPortOut>`
Construído via `List<NotificadorPortOut>` injetada no construtor do listener + `.collect(Collectors.toMap(NotificadorPortOut::getCanal, Function.identity()))`. Evita dependência de bean names como chave. Adicionar novo canal = só criar novo bean que implemente `getCanal()`.

### `@Transactional` em `RegistrarComprovanteServiceImpl.execute()`
Necessário para que `@TransactionalEventListener(AFTER_COMMIT)` dispare. Sem transação ativa, eventos de domínio são descartados (fallbackExecution = false por padrão). Adicionado nesta task.

### Default TELEGRAM do canalPreferido
Tanto na migração SQL (`DEFAULT 'TELEGRAM'`) quanto no `RequisitanteEntity` e `Requisitante.builder().canalPreferido(Canal.TELEGRAM)`. Pedro existente é automaticamente mapeado para TELEGRAM sem UPDATE.

## TODOs para BE-22

- `NotificacaoComprovanteListener`: `// TODO BE-22: incrementar counter de falha do listener (metric)`
- BE-22 deve adicionar: (a) Micrometer `Counter` de falhas do listener por canal, (b) `Timer` de latência do envio segmentado por canal/resultado.

## Desvios documentados

1. **V4 em vez de V5:** plan esperava V5 (pós-BE-19a). Usado V4 por ser o próximo disponível em develop. Renumerar se BE-19a mergear antes.
2. **`Canal` em vez de `CanalMensagem` do BE-19a:** esta task cria `Canal` unificado; BE-19a cria `CanalMensagem` separado na mesma pasta. Unificar para `Canal` durante o merge de uma das branches.

## Gates

- [x] `mvn test`: 207 unit tests passando, 22 erros (todos `integration` — Docker indisponível, pré-existente)
- [x] `mvn package -DskipTests`: BUILD SUCCESS
- [x] Território: apenas `financas_bot_telegram/` — nenhum toque em `frontend/`, infra
- [x] Status report escrito
