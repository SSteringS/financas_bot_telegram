---
name: jvm-e-performance
description: >
  Tuning de JVM para o projeto — sizing de heap para o t4g.micro (1 GB RAM), seleção de
  GC, flags obrigatórias, diagnóstico com thread dump e Micrometer, implicações de virtual
  threads em performance. Carregar quando a task envolve parâmetros de JVM, análise de
  latência/throughput, sizing de infra ou spec que define requisitos de performance.
load_pattern: shared
used_by: [backend, architect]
created: 2026-05-30
adr: 0015
status: ativa
---

# Skill — JVM e Performance

## Quando carregar (gatilho explícito)

- **Backend:** task configura `finbot.service` ou `application.properties` com flags de JVM;
  aparece `OOMKilled`, latência alta, ou thread pool no contexto.
- **Architect:** spec define SLA de latência, sizing de instância, ou compara opções de infra
  que afetam JVM footprint.
- **Sinal concreto:** aparece `-Xmx`, `GC`, `heap`, `OOM`, `thread dump`, `Micrometer`,
  `actuator`, `virtual threads` no contexto.

## Resumo da capacidade

Provê as configurações de JVM adequadas pra t4g.micro, explica o porquê de cada flag,
e indica como diagnosticar problemas de memória/concorrência sem precisar de ferramentas
externas pesadas.

## Contexto de infra — t4g.micro

| Recurso | Valor |
|---|---|
| RAM total | 1 GB |
| vCPU | 2 (AWS Graviton2 ARM) |
| JVM heap seguro | **≤ 700 MB** |
| Sobra pra OS + metaspace + off-heap | ~300 MB |

Heap acima de 700 MB → risco de OOMKill pelo kernel quando o OS precisar de buffer.

## Flags JVM recomendadas

```bash
JAVA_OPTS="-Xms256m \
           -Xmx700m \
           -XX:+UseG1GC \
           -XX:MaxGCPauseMillis=200 \
           -XX:MaxMetaspaceSize=128m \
           -XX:+HeapDumpOnOutOfMemoryError \
           -XX:HeapDumpPath=/opt/finbot/heapdump.hprof \
           -Djava.security.egd=file:/dev/./urandom"
```

| Flag | Motivo |
|---|---|
| `-Xms256m` | Evita resize de heap no startup (latência inicial menor) |
| `-Xmx700m` | Teto seguro pro t4g.micro |
| `-XX:+UseG1GC` | Melhor equilíbrio throughput/pause em heap médio |
| `-XX:MaxGCPauseMillis=200` | Meta de pausa; G1GC ajusta regiões pra bater |
| `-XX:MaxMetaspaceSize=128m` | Limita metaspace — evita leak de classloader |
| `-XX:+HeapDumpOnOutOfMemoryError` | Dump automático em OOM — diagnóstico sem acesso em tempo real |
| `java.security.egd` | Evita bloqueio em `/dev/random` no startup do Tomcat |

## Seleção de GC

| GC | Quando usar | Cuidado |
|---|---|---|
| **G1GC** (padrão recomendado) | Heap 256 MB – 700 MB, mix de throughput + pause | Baseline certo para o projeto |
| SerialGC | Heap ≤ 256 MB, single-thread | Boa opção em ambientes muito restritos |
| ZGC | Pauses ultra-baixas (<1ms) | Overhead de memória maior — não vale no t4g.micro |
| ShenandoahGC | Similar ZGC | Mesmo problema de footprint |

## Virtual threads e performance (Java 21)

- **IO-bound tasks** (JDBC, HTTP): virtual threads reduzem overhead de contexto significativamente — ativar com `spring.threads.virtual.enabled=true`.
- **CPU-bound tasks**: virtual threads não ajudam; usar thread pool dedicado.
- **Carrier thread pinning** (bug mais comum): `synchronized` em código de framework segura o carrier thread OS.
  - Diagnóstico: `-Djdk.tracePinnedThreads=full` → imprime stack quando pinning ocorre.
  - Solução: substituir `synchronized` por `ReentrantLock` no código próprio; libs bem atualizadas (HikariCP 5.1+, Tomcat 10.1.25+) já corrigiram.
- **Monitoramento de virtual threads**: `jstack` lista carrier threads, não virtual threads individuais — usar `Thread.getAllStackTraces()` ou Micrometer.

## Diagnóstico rápido sem ferramentas externas

```bash
# Thread dump — deadlock / liveness
kill -3 $(pgrep -f finbot)   # sinal SIGQUIT → dump em stdout do processo
# ou
jstack $(pgrep -f finbot) > /tmp/tdump.txt

# Heap usage em runtime
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# GC stats
curl http://localhost:8080/actuator/metrics/jvm.gc.pause
```

## Métricas Micrometer úteis (Spring Boot Actuator)

| Métrica | O que indica |
|---|---|
| `jvm.memory.used{area="heap"}` | Heap em uso — alertar se > 80% de Xmx |
| `jvm.gc.pause` | Latência de pausa do GC |
| `jvm.threads.live` | Threads vivas — spike indica leak |
| `hikaricp.connections.active` | Conexões DB em uso — saturação = gargalo JDBC |
| `http.server.requests` | Latência e throughput dos endpoints |

## Checklist de sizing / tuning

- [ ] `-Xmx` ≤ 700 MB no `finbot.service` ou `JAVA_OPTS`.
- [ ] `-XX:MaxMetaspaceSize` definido (previne leak silencioso).
- [ ] `-XX:+HeapDumpOnOutOfMemoryError` ativo com path válido e espaço em disco.
- [ ] GC selecionado explicitamente (não default da JVM).
- [ ] Se virtual threads: verificou carrier thread pinning em libs usadas.
- [ ] Actuator habilitado e ao menos `metrics` e `health` expostos.

## Ler junto

- Skill `ecossistema-spring` — virtual threads afetam HikariCP sizing e threading model.
- `finbot.service` — onde os JVM flags vivem no projeto.
- `docs/architecture/especificacao-tecnica.md` — infra declarada (t4g.micro, Graviton2 ARM).
