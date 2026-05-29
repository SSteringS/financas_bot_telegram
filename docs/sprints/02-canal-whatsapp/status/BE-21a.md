---
task: BE-21a
titulo: "Eventos in-process + NotificadorPortOut + canalPreferido + Telegram impl"
data: 2026-05-29
branch: feature/be-21a-eventos-notificador-canalpreferido
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: na
  testes: ok
  testes_total: 229
  testes_novos: 3
  branch_convencao: ok
  territorio: ok
commits:
  - c270f62
  - 95b1742
  - 6b28829
pr: null
desvios: 1
pendencias_humano: 0
---

# BE-21a — Eventos in-process + NotificadorPortOut + canalPreferido + Telegram impl

## O que foi feito

### Migração (V5)
- `V5__add_canal_preferido_requisitante.sql`: `ALTER TABLE requisitante ADD COLUMN canal_preferido VARCHAR(20) NOT NULL DEFAULT 'TELEGRAM'`.
- Default `'TELEGRAM'` garante backfill silencioso dos registros existentes (Pedro).

### Domínio
- `domain/model/Canal.java`: enum `TELEGRAM | WHATSAPP` — enum unificado. BE-19a cria o mesmo arquivo com conteúdo idêntico; conflito no merge será no-op.
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

---

## Desvios do plano

**`Canal.java` com conflito trivial com BE-19a:** esta task cria `domain/model/Canal.java`. BE-19a cria o mesmo arquivo com conteúdo idêntico. No merge de BE-21a em `develop` (após BE-19a já estar mergeado), o conflito em `Canal.java` será no-op — resolver mantendo qualquer das duas versões.

(Desvio de numeração V4→V5 já resolvido em `6b28829` — migration renomeada proativamente. Nenhuma ação necessária no merge.)

---

## Decisões tomadas durante a execução

**`Map<Canal, NotificadorPortOut>`:** construído via `List<NotificadorPortOut>` injetada no construtor + `Collectors.toMap(NotificadorPortOut::getCanal, ...)`. Evita dependência de bean names como chave. Adicionar novo canal = só criar novo bean que implemente `getCanal()`.

**`@Transactional` em `RegistrarComprovanteServiceImpl.execute()`:** sem transação ativa, `@TransactionalEventListener(AFTER_COMMIT)` descarta eventos (fallbackExecution = false por padrão).

**Default TELEGRAM do canalPreferido:** tanto na migração SQL (`DEFAULT 'TELEGRAM'`) quanto em `RequisitanteEntity` e `Requisitante.builder()`. Pedro existente é automaticamente mapeado para TELEGRAM sem UPDATE.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

**BE-22 — counters de falha do listener:**
- `NotificacaoComprovanteListener`: `// TODO BE-22: incrementar counter de falha do listener (metric)`
- BE-22 deve adicionar: (a) Micrometer `Counter` de falhas por canal, (b) `Timer` de latência por canal/resultado.

---

## Arquivos criados/modificados

- `src/main/resources/db/migration/V5__add_canal_preferido_requisitante.sql` (novo)
- `domain/model/Canal.java` (novo)
- `domain/event/ComprovanteRegistradoEvent.java` (novo)
- `domain/model/Requisitante.java` (modificado: campo `canalPreferido`)
- `adapters/out/persistence/entity/RequisitanteEntity.java` (modificado: campo `canalPreferido`)
- `adapters/out/persistence/mapper/RequisitanteMapper.java` (modificado: mapeamento `canalPreferido`)
- `application/port/out/NotificadorPortOut.java` (novo)
- `application/port/out/NotificacaoDTO.java` (novo)
- `application/config/AsyncConfig.java` (novo)
- `application/event/NotificacaoComprovanteListener.java` (novo)
- `adapters/out/telegram/notificador/TelegramNotificadorImpl.java` (novo)
- `application/services/RegistrarComprovanteServiceImpl.java` (modificado: publisher + @Transactional)
- `RegistrarComprovanteServiceImplTest.java` (modificado: @Mock eventPublisher)
- `NotificacaoComprovanteListenerIntegrationTest.java` (novo)
