# BE-21a — Eventos in-process + `NotificadorPortOut` + `canalPreferido` + Telegram impl

> **Intake (contrato de entrada da task)**
>
> - **Origem:** **ADR 0014** (notificações via Spring `ApplicationEvents`) + `docs/architecture/adapter-whatsapp-cloud-api.md` §3 e §6 atualizadas + ADR 0013 (multi-canal).
> - **Prioridade:** alta — **entrega a EVO-02 parcial via Telegram já amanhã**, sem depender da verificação Meta (que bloqueia o canal WhatsApp por causa do erro 130497 BR). Pedro começa a ser notificado proativamente.
> - **Esforço:** médio-alto (sistema novo de eventos + listener + porta + migração + entity update + impl Telegram + wire usecase + testes com transação real).
> - **Território / quem executa:** `financas_bot_telegram/src/main/java/` + `src/main/resources/db/migration/` → **Claude do back**.
> - **Branch:** `feature/be-21a-eventos-notificador-canalpreferido`, a partir de `develop`.
> - **Dependências:** **BE-17 em `develop`** ✅ (precisa de `application/services/` e da estrutura hexagonal pós-refactor).
> - **Riscos:**
>   1. **`@TransactionalEventListener(AFTER_COMMIT)` não dispara em testes com transação mockada** (gotcha do ADR 0014 §riscos). Mitigação: testes do listener usam `@SpringBootTest` + Testcontainers — transação real.
>   2. **`@Async` não propaga exceção** → sem `try/catch` dentro do listener, falha vira silêncio absoluto. Mitigação: tratamento obrigatório + log `ERROR` + métrica de falha (deixar TODO marcado pra BE-22 instrumentar).
>   3. **`AsyncTaskExecutor` não-bounded** cresce em incidente (Graph API lenta = thread pool explode). Mitigação: pool explícito (core 2, max 4, queue 100).
>   4. **Acoplamento do usecase ao `ApplicationEventPublisher`** (acoplamento Spring no `application/`). ADR 0014 aceita pragmatic — não criar `EventPublisherPort` agora.
>   5. **Migração `canal_preferido`** + backfill do Pedro existente. Default `'TELEGRAM'` no DDL garante.
>   6. **Texto bonito da notificação Telegram** — decisão pequena que pode virar discussão; **fixar agora**: "Comprovante registrado pra o pedido #X. Veja em https://satyansaita.com".

---

## Contexto

A spec original tinha `RegistrarComprovanteUsecase` chamando `NotificadorPortOut` direto. Isso virou problema com multi-canal (ADR 0013): usecase precisaria conhecer roteamento. **ADR 0014 inverte:** usecase publica um **evento de domínio**; um **listener** assíncrono resolve canal preferido e despacha.

### Por que isso é especialmente bom agora

A **restrição da Meta pra BR não-verificada** (`docs/aprendizado/whatsapp-restricoes-pais-business-nao-verificada.md`) bloqueia o canal WhatsApp até a verificação aprovar. Mas o roteamento por `canalPreferido` + `Map<Canal, NotificadorPortOut>` permite que **o Telegram receba as notificações já agora** — Pedro fica notificado proativamente enquanto a Meta processa a verificação. Quando BE-21b adicionar o bean do canal WhatsApp e a verificação sair, o roteamento já existe.

### Cadeia pós-BE-21a

```
RegistrarComprovanteUsecase
  ├── salvar(comprovante)                              [@Transactional]
  └── eventPublisher.publish(ComprovanteRegistradoEvent)

[AFTER_COMMIT — async via AsyncTaskExecutor]
NotificacaoComprovanteListener.onComprovanteRegistrado(ev)
  ├── canal = requisitanteRepo.find(ev.requisitanteId).canalPreferido
  ├── notificadores.get(canal).notificar(NotificacaoDTO)   ← Map<Canal, NotificadorPortOut>
  └── try/catch → log ERROR + TODO métrica de falha (BE-22)
```

## Decisão / abordagem

Implementação **completa do sistema de eventos** + **canal Telegram funcional**. WhatsApp impl fica pra BE-21b (espera template + verificação Meta).

### Pontos arquiteturais críticos

- **`AFTER_COMMIT` não-negociável** — evento dentro da transação dispararia o listener antes do commit; rollback geraria "notificado mas não registrado". (ADR 0014 §razões.)
- **`@Async`** desacopla latência — registro do comprovante não espera Telegram API (ou Graph API no futuro). Impacta a métrica fim-a-fim por tipo (§8.1).
- **Listener faz best-effort + log + métrica**. Sem retry estruturado por ora (durabilidade aceita como negativa no ADR 0014 — fonte da verdade é o front).
- **`Map<Canal, NotificadorPortOut>`** — Spring auto-injeta map de beans cuja key é o `@Qualifier` (ou nome do bean). Adicionar canal no futuro = só adicionar bean. Listener e usecase **não tocam**.

## Escopo / arquivos

### Migração + dados

- `src/main/resources/db/migration/Vx__add_canal_preferido_requisitante.sql` (próximo número disponível depois da BE-19a; provavelmente V5):
  ```sql
  ALTER TABLE requisitante
      ADD COLUMN canal_preferido VARCHAR(20) NOT NULL DEFAULT 'TELEGRAM';
  ```
  Default `'TELEGRAM'` garante backfill do Pedro existente sem update explícito.

### Domínio

- `domain/event/ComprovanteRegistradoEvent.java` (criar) — record canal-agnóstico:
  ```java
  public record ComprovanteRegistradoEvent(
      Long comprovanteId,
      Long pedidoId,
      Long requisitanteId
      // outros metadados se o listener precisar (ex.: valor, descrição do pedido) —
      // mas evitar puxar tudo: prefer LISTENER fazer lookup se precisar.
  ) {}
  ```
  Mantém o evento **pequeno e estável** (ADR 0014 §evolução — contrato sobrevive a futuras migrações pra outbox/broker).
- `domain/model/CanalNotificacao.java` (criar) — enum `{ TELEGRAM, WHATSAPP }`. **Decisão de unificação:** se BE-19a já criou `CanalMensagem`, **unificar num único enum `Canal`** em `domain/model/`. Se BE-19a foi mergeada antes, refatorar o que ela criou (renomeando) — coordenar no PR. Se BE-19a ainda não mergeou, esta task **cria `Canal` unificado** e BE-19a se ajusta.
- `domain/model/Requisitante.java` (modificar) — adicionar campo `canalPreferido` do tipo `Canal`.

### Aplicação

- `application/port/out/NotificadorPortOut.java` (criar):
  ```java
  public interface NotificadorPortOut {
      void notificar(NotificacaoDTO notificacao);
  }
  ```
- `application/port/out/NotificacaoDTO.java` (criar) — record com o mínimo pro adapter montar a mensagem: `destinatarioCanalId` (o ID do canal — `chatId` do Telegram, `wa_id` do WhatsApp), `pedidoId`, `linkSite` (já formatado). Manter o port simples; cada adapter sabe como formatar.
- `application/event/NotificacaoComprovanteListener.java` (criar):
  ```java
  @Component
  public class NotificacaoComprovanteListener {
      private final Map<Canal, NotificadorPortOut> notificadores;
      private final RequisitanteRepository requisitanteRepo;
      private final ComprovanteRepository comprovanteRepo; // pra ler dados do comprovante se precisar
      // (Spring injeta Map<Canal, NotificadorPortOut> automaticamente
      // usando @Qualifier ou nome do bean como key — confirmar o padrão)

      @Async
      @TransactionalEventListener(phase = AFTER_COMMIT)
      public void onComprovanteRegistrado(ComprovanteRegistradoEvent ev) {
          try {
              var req = requisitanteRepo.findById(ev.requisitanteId()).orElseThrow();
              var notificador = notificadores.get(req.getCanalPreferido());
              if (notificador == null) {
                  log.warn("Sem notificador pro canal {}; skip", req.getCanalPreferido());
                  return;
              }
              var dto = new NotificacaoDTO(...);  // montar com os dados
              notificador.notificar(dto);
          } catch (Exception e) {
              log.error("Falha ao notificar comprovanteId={}", ev.comprovanteId(), e);
              // TODO BE-22: counter de falha do listener (metric)
          }
      }
  }
  ```
- `application/config/AsyncConfig.java` (criar — ou ajustar config existente):
  ```java
  @Configuration
  @EnableAsync
  public class AsyncConfig {
      @Bean(name = "notificacaoExecutor")
      public Executor notificacaoExecutor() {
          var ex = new ThreadPoolTaskExecutor();
          ex.setCorePoolSize(2);
          ex.setMaxPoolSize(4);
          ex.setQueueCapacity(100);
          ex.setThreadNamePrefix("notif-");
          ex.initialize();
          return ex;
      }
  }
  ```
  Anotar `@Async("notificacaoExecutor")` no listener (sem fallback pro executor default do Spring).

### Adapter de saída — Telegram NotificadorPortOut

- `adapters/out/telegram/notificador/TelegramNotificadorImpl.java` (criar) — implementa `NotificadorPortOut`, anotado como `@Component` com qualifier `Canal.TELEGRAM` (decidir mecanismo: `@Qualifier("TELEGRAM")` no field do Map, ou nome do bean `telegramNotificador` + chave-fixa). Wrap do `TelegramMessageSenderService` existente. Mensagem padrão:
  ```
  ✅ Comprovante registrado para o pedido #{pedidoId}.
  Veja em https://satyansaita.com
  ```

### Wire do usecase

- `application/services/RegistrarComprovanteServiceImpl.java` (modificar) — injetar `ApplicationEventPublisher`; **após** salvar o comprovante (dentro da mesma transação), chamar `eventPublisher.publishEvent(new ComprovanteRegistradoEvent(...))`. Spring entrega ao listener pós-commit.

### Mapper e Entity

- `adapters/out/persistence/entity/RequisitanteEntity.java` (modificar) — adicionar `canal_preferido` mapeado.
- `adapters/out/persistence/mapper/RequisitanteMapper.java` (modificar) — campo novo no mapping.

### Não tocar

- Adapter WhatsApp (BE-21b cria o `WhatsAppNotificadorImpl`).
- `frontend/`, infra.

## Critérios de aceitação

- [ ] **Migração** aplica clean (Testcontainers); Pedro existente fica com `canal_preferido='TELEGRAM'`.
- [ ] `ComprovanteRegistradoEvent` é record imutável em `domain/event/`.
- [ ] `NotificadorPortOut` definido em `application/port/out/`.
- [ ] `NotificacaoComprovanteListener` com `@TransactionalEventListener(AFTER_COMMIT)` + `@Async("notificacaoExecutor")` + try/catch + log ERROR + TODO marcado pra BE-22.
- [ ] `AsyncConfig` com pool bounded (2/4/100 ou justificar outro valor).
- [ ] `RegistrarComprovanteServiceImpl` publica `ComprovanteRegistradoEvent` após salvar.
- [ ] `TelegramNotificadorImpl` envia mensagem com link pro site usando `TelegramMessageSenderService`.
- [ ] `Map<Canal, NotificadorPortOut>` injetável no listener; teste confirma que `Canal.TELEGRAM` é resolvido.
- [ ] **Testes com transação real** (Testcontainers + `@SpringBootTest`):
  - Cenário **sucesso**: registrar comprovante → commit → listener dispara após commit → `TelegramMessageSenderService` é chamado (mock).
  - Cenário **rollback**: forçar exceção no `RegistrarComprovanteServiceImpl` após o publish → rollback → listener **NÃO** dispara.
  - Cenário **falha no listener** (mock do sender lançando exceção) → log ERROR + transação do usecase **não afetada** (já comitou).
- [ ] `mvn test` verde (incluindo todos os testes anteriores).
- [ ] `mvn package -DskipTests` ok.
- [ ] Branch saiu de `develop`; território só `financas_bot_telegram/`.
- [ ] Status report `docs/sprints/02-canal-whatsapp/status/BE-21a.md` com frontmatter válido. Anotar:
  - **Decisão de unificação do enum `Canal`** com BE-19a (se aplicável).
  - **TODOs marcados pra BE-22** (counter de falha do listener; timer do envio segmentado por canal/resultado).
  - **Default 'TELEGRAM' do canal_preferido** confirmado.
  - **Mecanismo do `Map<Canal, NotificadorPortOut>`** escolhido (Qualifier vs nome do bean).

## Coordenação

- **Esta task entrega EVO-02 funcional pelo canal Telegram.** Quando merger e deployar, o Pedro começa a receber notificação automática no Telegram a cada comprovante registrado.
- **BE-21b** adiciona o `WhatsAppNotificadorImpl` (mais um bean no Map) — sem tocar listener, usecase, nem evento.
- **BE-22** vai instrumentar: (a) **counter de falha do listener**, (b) **timer de envio do notificador segmentado por canal/resultado**. Esta task deixa TODOs marcados nos pontos certos.
- **BE-19a** cria `CanalMensagem`; **decisão de unificação com `Canal` aqui** — coordenar via PR/Reviewer.
- **Acoplamento Spring no `application/`:** o ADR 0014 aceita pragmatic (não criar `EventPublisherPort`). Reviewer pode discutir; manter o acoplamento desta task como decisão pequena.

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build, lint na, testes verdes com transação real, branch, território), status report válido, e **revisão do Reviewer** — toca usecase ativo, sistema novo (eventos), schema de banco, novo executor de threads. Alto risco. Abrir PR pra `develop`; não mergear sozinho.

## Referências

- ADR 0014 (decisão arquitetural canônica).
- `docs/architecture/adapter-whatsapp-cloud-api.md` §3 (pacotes pós-evento) e §6 (fluxo da EVO-02 reescrito).
- ADR 0013 (multi-canal — motiva o `Map<Canal, NotificadorPortOut>`).
- `docs/aprendizado/whatsapp-restricoes-pais-business-nao-verificada.md` (por que Telegram-first agora).
- `docs/architecture/estado-atual.md` §3/§6 (estado pré-refactor — espelhar padrões do Telegram sender).
- `docs/sprints/02-canal-whatsapp/plans/BE-19a-idempotencia-mensagem-processada.md` (coordenação do enum `Canal`).
- Documentação Spring: `@TransactionalEventListener`, `@Async`, `ThreadPoolTaskExecutor`.
</content>
