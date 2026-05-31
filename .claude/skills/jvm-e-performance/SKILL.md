---
name: jvm-e-performance
description: >
  Tuning de JVM — sizing de heap e selecao de GC para o ambiente alvo (instancia EC2,
  container, qualquer perfil de memoria/CPU), flags obrigatorias, diagnostico com thread
  dump e Micrometer, implicacoes de virtual threads em performance. Carregar quando a
  task envolve parametros de JVM, analise de latencia/throughput, sizing de infra ou
  spec que define requisitos de performance.
load_pattern: shared
used_by: [backend, architect]
created: 2026-05-30
adr: 0015
status: ativa
---

# Skill — JVM e Performance

## Quando carregar (gatilho explícito)

- **Backend:** task configura JVM flags (`finbot.service`, `application.properties`, Dockerfile);
  aparece `OOMKilled`, latência alta, ou thread pool no contexto.
- **Architect:** spec define SLA de latência, sizing de instância/container, ou compara
  opções de infra que afetam JVM footprint.
- **Sinal concreto:** aparece `-Xmx`, `GC`, `heap`, `OOM`, `thread dump`, `Micrometer`,
  `actuator`, `virtual threads` no contexto.
- **Pré-requisito:** ler a spec de infra do projeto (`docs/architecture/especificacao-tecnica.md`)
  antes de calcular qualquer valor — RAM disponível e tipo de ambiente (EC2, container, cgroup)
  determinam todos os parâmetros.

## Resumo da capacidade

Provê os princípios e o raciocínio de tuning de JVM para qualquer ambiente. O agente
aplica as fórmulas e critérios lendo a infra declarada — sem hardcode de valores.

## Princípio de sizing de heap

```
heap_max = RAM_total - overhead_fixo
overhead_fixo = OS + Metaspace + off-heap (NIO, direct buffers) + margem de segurança
```

- **EC2 (processo direto):** overhead típico 250–400 MB — ajustar pela RAM da instância.
- **Container:** overhead similar, mas o teto é o `cgroup memory.limit`, não a RAM do host.
  Usar `-XX:+UseContainerSupport` (on por default, Java 11+) que lê o cgroup automaticamente;
  ainda assim, definir `-Xmx` explicitamente como teto de segurança.
- **Regra prática:** `-Xms` = 1/3 a 1/2 do `-Xmx` — reduz resize de heap no startup sem
  travar RAM.

## Flags JVM — template comentado

```bash
JAVA_OPTS="-Xms<calculado>       # 1/3 do Xmx — startup rápido
           -Xmx<calculado>       # heap_max = RAM - overhead; ver spec de infra
           -XX:+UseG1GC          # padrão para heap médio (256 MB – 4 GB)
           -XX:MaxGCPauseMillis=200
           -XX:MaxMetaspaceSize=128m   # limita metaspace — evita leak de classloader
           -XX:+HeapDumpOnOutOfMemoryError
           -XX:HeapDumpPath=<path>/heapdump.hprof
           -Djava.security.egd=file:/dev/./urandom"  # evita bloqueio no startup Tomcat
```

## Seleção de GC

| GC | Quando usar | Cuidado |
|---|---|---|
| **G1GC** (padrão) | Heap 256 MB – 4 GB, mix throughput + pause | Baseline correto para a maioria |
| SerialGC | Heap <= 256 MB, container mínimo, single-thread | Menos overhead em RAM muito restrita |
| ZGC | Pauses < 1 ms, heap grande, latência crítica | Overhead de RAM maior |
| ShenandoahGC | Similar ZGC | Mesmo problema de footprint |

## Virtual threads e performance (Java 21)

- **IO-bound tasks** (JDBC, HTTP): virtual threads reduzem overhead de contexto — ativar
  com `spring.threads.virtual.enabled=true`.
- **CPU-bound tasks**: virtual threads não ajudam; usar thread pool dedicado.
- **Carrier thread pinning** (bug mais comum): `synchronized` segura o carrier thread OS.
  - Diagnóstico: `-Djdk.tracePinnedThreads=full`.
  - Correção: `ReentrantLock` no código próprio; libs atualizadas (HikariCP 5.1+,
    Tomcat 10.1.25+) já corrigiram do lado delas.
- **Em container:** virtual threads funcionam normalmente — a JVM conta vCPUs do cgroup.

## Diagnóstico rápido sem ferramentas externas

```bash
# Thread dump — deadlock / liveness
kill -3 $(pgrep -f <processo>)
jstack $(pgrep -f <processo>) > /tmp/tdump.txt

# Heap usage via Actuator
curl http://localhost:<porta>/actuator/metrics/jvm.memory.used

# GC pause stats
curl http://localhost:<porta>/actuator/metrics/jvm.gc.pause
```

## Métricas Micrometer úteis

| Métrica | O que indica |
|---|---|
| `jvm.memory.used{area="heap"}` | Heap em uso — alertar se > 80% de Xmx |
| `jvm.gc.pause` | Latência de pausa do GC |
| `jvm.threads.live` | Threads vivas — spike indica leak |
| `hikaricp.connections.active` | Conexões DB em uso — saturação = gargalo JDBC |
| `http.server.requests` | Latência e throughput dos endpoints |

## Checklist de sizing / tuning

- [ ] Leu a spec de infra (tipo de ambiente, RAM disponível, número de vCPUs).
- [ ] `-Xmx` calculado a partir da fórmula: RAM menos overhead estimado.
- [ ] `-XX:MaxMetaspaceSize` definido.
- [ ] `-XX:+HeapDumpOnOutOfMemoryError` com path válido e espaço em disco.
- [ ] GC selecionado explicitamente — não depender do default da JVM.
- [ ] Container: `-Xmx` dentro do cgroup limit; `UseContainerSupport` ativo.
- [ ] Virtual threads: verificou carrier thread pinning nas libs usadas.
- [ ] Actuator exposto: ao menos `metrics` e `health`.

## Ler junto

- Skill `ecossistema-spring` — virtual threads afetam HikariCP sizing e threading model.
- `docs/architecture/especificacao-tecnica.md` — infra declarada do projeto (ler antes de calcular).
