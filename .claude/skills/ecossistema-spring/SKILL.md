---
name: ecossistema-spring
description: >
  Guia de decisao para o ecossistema Spring Boot — Spring Data JDBC vs JPA,
  clientes REST (RestClient/WebClient/Feign), eventos sincronos vs assincronos,
  virtual threads Java 21, HikariCP sizing. Carregar quando a task ou spec envolve
  escolha de biblioteca Spring, integracao externa HTTP ou configuracao de concorrencia.
load_pattern: shared
used_by: [backend, architect]
created: 2026-05-30
adr: 0015
status: ativa
---

# Skill — Ecossistema Spring Boot

## Quando carregar (gatilho explícito)

- **Backend:** task envolve nova integração HTTP, escolha entre Spring Data JDBC / JPA,
  configuração de pool de conexão, ou uso de eventos/mensageria.
- **Architect:** spec técnica exige decisão de biblioteca Spring ou topologia de
  concorrência (ex.: threading model, integração externa, sizing de pool).
- **Sinal concreto:** aparece `JpaRepository`, `@Async`, `WebClient`, `Feign`,
  `HikariCP`, `spring.threads.virtual` no contexto da discussão.

## Resumo da capacidade

Mapeia os trade-offs das principais escolhas de biblioteca Spring com regras de decisão
prontas para uso. Evita escolher por hábito ("uso JPA porque sempre uso") quando a
alternativa mais simples resolve melhor.

## Decisões de persistência — Spring Data JDBC vs JPA

| Critério | Spring Data JDBC | Spring Data JPA |
|---|---|---|
| Modelo de mapeamento | Simples, sem proxies, sem lazy loading | Entidades gerenciadas, proxies, lazy/eager |
| Comportamento padrão | Previsível — sem N+1 surpresa | Fácil cair em N+1 sem perceber |
| Eventos de domínio | Nativos (`@DomainEvent`) — disparo limpo | Complexo (entity lifecycle callbacks) |
| Lock-in | Spring Data só | Spring Data + Hibernate |
| Quando usar | Modelo simples, domínio claro, sem herança de tabela | Herança de tabela complexa, cache L2, legado com ORM |

**Spring Data JDBC é a escolha padrão** para domínios limpos com arquitetura hexagonal — sem proxies, sem N+1 surpresa, aggregate roots explícitos. Introduzir JPA requer justificativa explícita (ex.: ADR).

## Clientes REST

| Cliente | Sincronos/Assíncronos | Quando usar |
|---|---|---|
| `RestClient` (Spring 6.1+) | Síncrono, fluente | **Padrão** — chamadas simples, legível, low overhead |
| `WebClient` (Reactor) | Reativo (Mono/Flux) | Só se precisar de pipeline assíncrono real; não usar por hábito |
| `Feign` (OpenFeign) | Síncrono declarativo | Boa opção quando há muitos endpoints a declarar; adiciona dependência |
| `RestTemplate` | Síncrono (legado) | Não usar em código novo |

**Regra:** `RestClient` pra maioria; `WebClient` só se reactive pipeline; Feign aceitável se interface > 5 métodos.

## Eventos

- **`ApplicationEventPublisher` síncrono** — mesmo thread, transação propagada. Usar para side-effects leves dentro do mesmo bounded context.
- **`@Async` + `@EventListener`** — thread pool separado (ex.: `TaskExecutor`). Usar para notificações não-críticas.
- **Kafka/RabbitMQ** — overkill para serviços monolíticos sem necessidade de durabilidade ou pub/sub real. Não adotar sem ADR.

## Virtual threads (Java 21 + Spring Boot 3.2+)

```properties
spring.threads.virtual.enabled=true
```

- **Vantagem:** IO-bound tasks (JDBC, HTTP) escalam sem overhead de threads OS.
- **Cuidado:** `synchronized` blocks causam **carrier thread pinning** — substitua por `ReentrantLock`.
- **Diagnóstico de pinning:** `-Djdk.tracePinnedThreads=full` nos JVM flags.
- **Compatibilidade:** HikariCP 5.1+ e Tomcat 10.1.25+ suportam virtual threads nativamente.

## HikariCP — sizing

Fórmula base: `pool_size = (vCPUs * 2) + effective_spindle_count`.

Para RDS MySQL via network (sem spindle físico): `pool_size ≈ (vCPUs * 2) + 1`.

```yaml
spring.datasource.hikari:
  maximum-pool-size: <calculado pela fórmula — ler spec de infra para vCPU count>
  minimum-idle: 2
  connection-timeout: 3000
  idle-timeout: 60000
```

**Cuidado com over-provisioning:** pool muito grande não melhora throughput — o gargalo é o DB.
Acima de 10–15 conexões em instâncias pequenas, o DB geralmente satura antes do pool.

## Checklist de decisão

- [ ] Persistência: Spring Data JDBC é suficiente? Se JPA, tem justificativa explícita?
- [ ] Cliente REST: `RestClient` é suficiente? Se `WebClient`, há reactive pipeline real?
- [ ] Eventos: síncrono (mesmo TX) ou assíncrono (side-effect)? Escolha explícita no plano.
- [ ] Virtual threads ativados? Se sim, revisou `synchronized` blocks em libs usadas.
- [ ] Pool HikariCP dimensionado pela fórmula, considerando vCPUs da infra alvo.

## Ler junto

- Skill `jvm-e-performance` — sizing de heap e threading afetam pool sizing.
- `docs/architecture/especificacao-tecnica.md` — decisões de biblioteca já tomadas no projeto.
- ADR 0011 — arquitetura hexagonal e onde as dependências de framework moram.
