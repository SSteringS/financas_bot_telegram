# Eventos in-process com Spring (ApplicationEvents, AFTER_COMMIT, @Async)

## Contexto da dúvida

Surgiu na Sprint 02 ao desenhar a EVO-02 (notificar o requisitante quando um comprovante é registrado), num cenário multi-canal (ADR 0013: Telegram + WhatsApp + Discord futuro). O desenho ingênuo — usecase chama notificador direto — começou a mostrar fadiga: cada nova reação ao registro (notificar canal A, B, futura auditoria, métricas) iria inflar o usecase. A pergunta foi: como estruturar isso de forma orientada a eventos sem trazer infra pesada? Decisão registrada no ADR 0014.

## Resumo destilado

**A ideia central:** em vez do usecase chamar todos os interessados, ele **publica um fato** ("comprovante registrado") e quem se interessa **se inscreve**. Inverte-se a dependência: o usecase deixa de conhecer quem reage, e novas reações entram **sem mexer no usecase** (princípio aberto/fechado).

**O espectro de implementação** (do mais leve ao mais pesado):

1. **Síncrono direto** — usecase chama porta. Simples, mas acopla quem aciona a quem reage; cresce mal com N reações.
2. **Eventos in-process** (Spring `ApplicationEvents`) — publisher emite POJO, listeners (`@EventListener`) reagem. **Zero infra**, **single JVM**, **perda em crash**.
3. **Outbox** — usecase grava evento numa tabela na mesma transação do negócio; worker lê e despacha. **Durável**, sobrevive a crash, custa código.
4. **Broker externo** (SNS/SQS, Rabbit, Kafka) — publica num broker; consumers subscrevem. **Cross-process**, durável, escalável, alto custo operacional.

**Onde mora o ponto certo:** entre 2 e 3, o divisor é **durabilidade**. Se perder evento na crash do JVM é tolerável (ex.: notificação é "agradável de ter" e há fallback no produto), 2 vence por simplicidade. Se "não pode perder" virar requisito, 3.

**Os dois detalhes que separam o uso correto do uso ingênuo do Spring Events:**

- **`@TransactionalEventListener(phase = AFTER_COMMIT)`** em vez de `@EventListener` simples. Sem isso, o listener dispara **dentro** da transação que publicou; se a transação fizer rollback, você notificou "registrado" para algo que **não** foi registrado. Com `AFTER_COMMIT`, o listener só roda após o commit durável.
- **`@Async`** com um executor bounded. Sem isso, o listener roda na mesma thread do publisher — chamadas externas lentas (Graph API, S3) viram latência do usecase. Com `@Async`, o publisher retorna imediato e o listener processa em pool dedicado.

**Pegadinha do `@Async`:** exceção dentro do listener **não propaga** pro publisher. Se não tratar dentro do listener, falhas viram silêncio. Disciplina: `try/catch` no listener + log + métrica de falha.

## Pontos-chave

- **Publisher** = `ApplicationEventPublisher` (Spring) injetado onde o fato acontece (ex.: usecase). Publica um POJO de domínio (ex.: `ComprovanteRegistradoEvent`).
- **Listener** = método `@TransactionalEventListener(phase = AFTER_COMMIT) @Async` numa classe componente. Reage ao tipo do POJO.
- **Evento = contrato estável.** Quando promover pra outbox/broker no futuro, só a infra de transporte muda; o nome e os campos do evento ficam. Vale projetar o evento como contrato, não como detalhe interno.
- **Multi-listener barato.** Adicionar uma reação nova = nova classe com um método anotado. Não toca no publisher.
- **Multi-canal limpo.** Listener consulta canal preferido do requisitante e dispara o `NotificadorPortOut` correspondente via `Map<Canal, NotificadorPortOut>`. Adicionar canal = novo bean no map.
- **Trade-off explícito de durabilidade.** Evento publicado mas ainda não consumido quando o JVM cai **some**. Aceitável quando o produto tem fallback (no nosso caso, o requisitante vê o comprovante no front). Se não houver fallback, escalar pra outbox.
- **Teste de listener exige transação real.** `@TransactionalEventListener(AFTER_COMMIT)` não dispara em testes que mockam transação. Usar `@SpringBootTest` com banco real (H2/Testcontainers).
- **Sinais pra evoluir:**
  - In-process → outbox: métricas de "notificações perdidas" virarem material, ou a reação virar fonte da verdade pra algo, ou exigência de garantia de entrega após crash.
  - Outbox → broker: multi-instância da aplicação, ou outros serviços externos precisando consumir os mesmos eventos.

## Pra aprofundar

- **Transactional Outbox Pattern** — a evolução natural quando durabilidade vira requisito (Chris Richardson tem o material canônico).
- **Idempotent consumers** — listeners idempotentes facilitam migração futura pra outbox/broker (entrega at-least-once exige idempotência).
- **Spring `@Async` + configuração de `TaskExecutor`** — calibrar pool (bounded), nomear threads pra observability, configurar política de rejeição.
- **`DomainEvents` em DDD** — agregado registra eventos durante operações e dispatcher publica após persistência. Variação mais "domain-driven" do mesmo padrão.
