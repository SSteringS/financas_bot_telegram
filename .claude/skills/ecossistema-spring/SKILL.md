---
name: ecossistema-spring
description: >
  Guia de decisão para o ecossistema Spring Boot no projeto — Spring Data JDBC vs JPA,
  clientes REST (RestClient/WebClient/Feign), eventos síncronos vs assíncronos, virtual
  threads Java 21, HikariCP sizing. Carregar quando a task ou spec envolve escolha de
  biblioteca Spring, integração externa HTTP ou configuração de concorrência.
load_pattern: shared
used_by: [backend, architect]
created: 2026-05-30
adr: 0015
status: ativa
---

# Skill — Ecossistema Spring Boot

## Quando carregar (gatilho explícito)

- **Backend:** task envolve nova integração HTTP, choice entre Spring Data JDBC / JPA,
  configuração de pool de conexão, ou uso de eventos/mensageria.
- **Architect:** spec técnica exige decisão de biblioteca Spring ou topologia de
  concorrência (ex.: threading model, integração externa, sizing de pool).
- **Sinal concreto:** aparece `JpaRepository`, `@Async`, `WebClient`, `Feign`,
  `HikariCP`, `spring.threads.virtual` no contexto da discussão.

## Resumo da capacidade

Mapeia os trade-offs das principais escolhas de biblioteca Spring relevantes pro projeto,
com regras de decisão prontas para uso. Evita escolher por hábito ("uso JPA porque sempre
uso") quando a alternativa mais simples resolve melhor.

## Decisões de persistência — Spring Data JDBC vs JPA

| Critério | Spring Data JDBC | Spring Data JPA |
|---|---|---|
| Modelo de mapeamento | Simples, sem proxies, sem lazy loading | Entidades gerenciadas, proxies, lazy/eager |
| Comportamento padrão | Previsível — sem N+1 surpresa | Fácil cair em N+1 sem perceber |
| Eventos de domínio | Nativos (`@DomainEvent`) — disparo limpo | Complexo (entity lifecycle callbacks) |
| Lock-in | Spring Data só | Spring Data + Hibernate |
| Quando usar | Modelo simples, domínio claro, sem herança de tabela | Herança de tabela complexa, cache L2, legado com ORM |

**Projeto usa Spring Data JDBC.** Não introduzir JPA sem ADR.

## Clientes REST

| Cliente | Síncronos/Assíncronos | Quando usar |
|---|---|---|
| `RestClient` (Spring 6.1+) | Síncrono, fluente | **Padrão** — chamadas simples, legível, low overhead |
| `WebClient` (Reactor) | Reativo (Mono/Flux) | Só se precisar de pipeline assíncrono real; não usar por hábito |
| `Feign` (OpenFeign) | Síncrono declarativo | Boa opção quando há muitos endpoints a declarar; adiciona dep |
| `RestTemplate` | Síncrono (legado) | Não usar em código novo |

**Regra:** `RestClient` pra maioria; `WebClient` só se reactive pipeline; Feign aceitável se interface > 5 métodos.

## Eventos

- **`ApplicationEventPublisher` síncrono** — mesmo thread, transação propagada. Usar para side-effects leves dentro do mesmo bounded context.
- **`@Async` + `@EventListener`** — thread pool separado (ex.: `TaskExecutor`). Usar para notificações não-críticas.
- **Kafka/RabbitMQ** — overkill para o projeto atual. Não adotar sem ADR.

## Virtual threads (Java 21 + Spring Boot 3.2+)

```properties
spring.threads.virtual.enabled=true
```

- **Vantagem:** IO-bound tasks (JDBC, HTTP) escalam sem overhead de threads OS.
- **Cuidado:** `synchronized` blocks causam **carrier thread pinning** — substitua por `ReentrantLock`.
- **Diagnóstico de pinning:** `-Djdk.tracePinnedThreads=full` nos JVM flags.
- **Compatibilidade:** HikariCP 5.1+ suporta virtual threads nativamente.

## HikariCP — sizing no t4g.micro

Regra de ouro: `pool_size = (core_count * 2) + effective_spindle_count`.

t4g.micro (2 vCPU, RDS MySQL via network): `pool_size ≈ (2 * 2) + 1 = 5`.

```yaml
spring.datasource.hikari:
  maximum-pool-size: 10   # máximo razoável; acima disso, o DB sofre
  minimum-idle: 2
  connection-timeout: 3000
  idle-timeout: 60000
```

## Checklist de decisão

- [ ] Persistência: usando Spring Data JDBC? Se JPA, tem ADR justificando?
- [ ] Cliente REST: `RestClient` é suficiente? Se `WebClient`, há reactive pipeline real?
- [ ] Eventos: síncrono (mesmo TX) ou assíncrono (side-effect)? Escolha explícita no plano.
- [ ] Virtual threads ativados? Se sim, revisou `synchronized` blocks em libs usadas.
- [ ] Pool HikariCP dimensionado pra infra real (t4g.micro = ≤10 conexões).

## Ler junto

- Skill `jvm-e-performance` — sizing de heap e threading afetam pool sizing.
- `docs/architecture/especificacao-tecnica.md` — decisões de biblioteca já tomadas.
- ADR 0011 — arquitetura hexagonal e onde as dependências de framework moram.
