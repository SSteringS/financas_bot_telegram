# 0014 — Notificações via eventos in-process (Spring ApplicationEvents)

**Data:** 2026-05-27
**Status:** `Proposed`
**Decisores:** humano (PO) — proposto pelo Arquiteto

> Decisão direcionada pelo humano em 2026-05-27 após comparativo de alternativas (síncrono / Spring events / outbox / broker). Mantido `Proposed` por disciplina de papel.

---

## Contexto

Com o ADR 0013 (multi-canal: manter Telegram + adicionar WhatsApp + Discord futuro) e a EVO-02 (notificação automática quando um comprovante é registrado), o desenho original da spec — `RegistrarComprovanteUsecase` chamando `NotificadorPortOut` **direto** — começa a mostrar fadiga:

- Cada nova **reação** ao registro de comprovante (notificar canal A, notificar canal B, futura auditoria, métricas de negócio, eventual webhook externo) entraria no usecase como mais uma chamada → o usecase vira colecionador de ports e perde estabilidade.
- A latência do registro fica refém do envio externo (Graph API): Meta lenta = comprovante lento.
- Com múltiplos canais (ADR 0013), o usecase precisaria conhecer **a estratégia de roteamento** ("canal preferido do requisitante") — responsabilidade que não é dele.

A inversão natural é **eventos**: o usecase publica um fato ("comprovante registrado") e quem se interessa se inscreve.

---

## Decisão

Adotar **eventos in-process via Spring `ApplicationEvents`**, com **`@TransactionalEventListener(phase = AFTER_COMMIT)`** e **`@Async`**, como mecanismo de reação a fatos de negócio. Caso de uso inicial: notificação da EVO-02; padrão se estende pra futuras reações.

Forma concreta:

```
domain/event/
└── ComprovanteRegistradoEvent.java          // POJO/record canal-agnóstico
                                              // (id comprovante, id pedido, ref requisitante)

application/event/
└── NotificacaoComprovanteListener.java
    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Async
    onComprovanteRegistrado(ev):
       requisitante = repo.find(ev.requisitanteId)
       canal = requisitante.canalPreferido            // ADR 0013
       notificadorDoCanal(canal).notificar(...)       // Map<Canal, NotificadorPortOut>
```

O `RegistrarComprovanteUsecase` apenas **publica**:

```
salvar(comprovante)                     // mesma transação do negócio
eventPublisher.publish(new ComprovanteRegistradoEvent(...))
```

---

## Razões

- **Múltiplas reações desacopladas do usecase** (princípio aberto/fechado): nova reação = novo listener, sem tocar no usecase.
- **Zero infra:** built-in do Spring, nada novo na EC2.
- **`AFTER_COMMIT` é não-negociável** e é o ponto que evita o pior bug do desenho ingênuo: publicar evento **dentro** da transação faria o listener disparar antes do commit; se a transação fizer rollback, "notificado" sem "registrado". Com `AFTER_COMMIT`, o evento só dispara após persistência durável.
- **`@Async` desacopla latência:** registro do comprovante não espera Graph API; falha/lentidão da Meta não rebenta na thread do request. Interage diretamente com a métrica fim-a-fim por tipo (§8.1 da spec) — o tempo do comprovante não inclui o envio da notificação.
- **Multi-canal limpo:** `Map<Canal, NotificadorPortOut>` (um bean por canal); adicionar Discord no futuro = registrar mais um bean, sem tocar listener nem usecase nem evento.
- **Hexagonal preservada:** evento é POJO de domínio (`domain/event/`); listener é aplicação (`application/event/`); porta de saída inalterada.

---

## Consequências

**Positivas:**
- Desacoplamento limpo entre "fato aconteceu" e "quem reage".
- Latência do registro independente da Graph API/S3 do envio.
- Futuras reações (auditoria, métricas, webhooks) entram como listeners.
- Encaixe coerente com os outros ADRs desta sprint (0012, 0013) e com a estrutura hexagonal pós-refactor.

**Negativas (assumidas conscientemente):**
- **Sem durabilidade:** evento publicado mas ainda não processado quando o JVM cai some. **Aceito** porque a notificação aqui é "agradável de ter no momento" — o requisitante consegue ver o comprovante registrado pelo front (entregue na Sprint 01). Não é fonte da verdade. Mitigações: métrica de falha no listener (§8.1) + alarme se taxa subir.
- **Sem retry estruturado** por padrão. Listener trata best-effort + log + métrica. Retry sofisticado seria evolução (outbox).
- **`@Async` exige tratamento de exceção dentro do listener** (não propaga pro publisher). Sem isso, falhas viram silêncio. Disciplina obrigatória no listener.
- **Single JVM only.** Se um dia formos multi-instância, precisaria migrar pra outbox/broker.
- **Spring acopla domain via `ApplicationEventPublisher`** se injetado direto no usecase. Mitigação opcional: criar `EventPublisherPort` na camada de aplicação com impl Spring fora — útil se quisermos pureza estrita, dispensável pragmatic (a base já tem JPA no domínio, `estado-atual.md` §3.1).

---

## Alternativas consideradas

- **A. Síncrono direto (estado atual da spec antes deste ADR):** `RegistrarComprovanteUsecase` chama `NotificadorPortOut` direto. Descartado — é o problema que estamos resolvendo. Acoplamento cresce linearmente com cada reação nova.
- **C. Outbox** (tabela `outbox` gravada na mesma transação + worker dispatcher): mais robusto (at-least-once, sobrevive a crash), mais código (tabela, worker, dedup). Descartado **agora** porque durabilidade da notificação não é requisito — a redundância do front cobre. **Caminho natural de evolução** se o requisito mudar (ver abaixo).
- **D. Broker externo** (SNS/SQS, RabbitMQ, Kafka): cross-process, fan-out grande, replay. Descartado — operacional alto sem benefício pro nosso volume + single-instance.

---

## Caminho de evolução

- **Promover B → C (outbox)** se:
  - Métrica de falha do listener virar material (notificações perdidas relevantes); OU
  - EVO-02 virar **fonte da verdade** pra algo (ex.: ação por WhatsApp tipo "confirmar pagamento"); OU
  - For necessário garantir entrega após crash do JVM.
- **Promover C → D (broker)** se:
  - Multi-instância (vários backends consumindo); OU
  - Outro serviço externo precisar reagir aos mesmos eventos.

O caminho é incremental: o **contrato do evento** (`ComprovanteRegistradoEvent`) sobrevive a todas as evoluções — só a infraestrutura de transporte muda. Vale tratar o evento como **contrato estável** desde já (nome, campos), porque mudá-lo depois custa mais.

---

## Riscos e dependências (pro planner)

- Configurar **executor de `@Async`** com pool pequeno e bounded (ex.: 2-4 threads), evitando crescer ilimitado em incidente.
- **Tratamento de exceção dentro do listener** (não propaga via `@Async`) — checklist obrigatório de revisão.
- **Métricas de falha de listener** entram no escopo da observability (§8.1 da spec), junto das outras.
- Pequeno cuidado de teste: `@TransactionalEventListener(AFTER_COMMIT)` exige transação real — testes que mockam tx não disparam o listener.

---

## Referências

- `docs/decisions/0013-estrategia-multi-canal-adapters.md` — multi-canal (motiva o `Map<Canal, NotificadorPortOut>`)
- `docs/architecture/adapter-whatsapp-cloud-api.md` §6 — fluxo da EVO-02 (atualizado com este ADR)
- `docs/aprendizado/eventos-in-process-spring.md` — conceito formativo (criado junto)
- Direção dada pelo humano (PO) em 2026-05-27 após análise comparativa das alternativas A/B/C/D
