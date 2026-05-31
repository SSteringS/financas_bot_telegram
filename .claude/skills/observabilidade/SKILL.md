---
name: observabilidade
description: >
  Estrategia de observabilidade — comparativo de plataformas (CloudWatch, Grafana Cloud,
  Datadog, New Relic, Prometheus self-hosted, Splunk), os tres pilares (metrics/logs/traces),
  OpenTelemetry como padrao aberto, decisao baseada em custo e escala. Carregar quando a
  proposta envolve monitoring strategy, plataforma de observabilidade, ou custo de CloudWatch
  esta em discussao.
load_pattern: contextual
used_by: [architect]
created: 2026-05-31
adr: 0015
status: ativa
---

# Skill — Observabilidade

## Quando carregar (gatilho explícito)

- Proposta envolve monitoring, alertas, dashboards ou logging centralizado.
- Custo de CloudWatch está sendo questionado.
- Nova integração externa que precisa de tracing distribuído.
- **Sinal concreto:** aparecem "CloudWatch", "Datadog", "Grafana", "Prometheus", "Splunk",
  "New Relic", "OpenTelemetry", "traces", "dashboards", "alertas" no contexto.

## Os três pilares — o que cada plataforma precisa cobrir

| Pilar | O que é | No projeto atual |
|---|---|---|
| **Metrics** | Séries temporais (CPU, heap, latência, pool JDBC) | Micrometer já configurado — só mudar o exporter |
| **Logs** | Eventos estruturados com contexto (JSON) | stdout do Spring Boot → precisa de coletor e destino |
| **Traces** | Rastreamento de um request de ponta a ponta | Não implementado — OpenTelemetry quando necessário |

**Vantagem do projeto:** Spring Boot Actuator + Micrometer já existem. A instrumentação está feita. A decisão é só **qual exporter usar** — uma linha de dependência Maven.

## Comparativo de plataformas

| Plataforma | Custo | Setup | Lock-in | Melhor para |
|---|---|---|---|---|
| **CloudWatch** | $$: custom metrics $0.30/métrica/mês; logs $0.50/GB | Zero (nativo AWS) | Alto | Alertas de infra nativos (EC2, RDS) — esses são grátis |
| **Grafana Cloud** (free tier) | **$0** até 10k séries, 50 GB logs, 50 GB traces/mês | Médio | Baixo | **Melhor custo/esforço para o projeto atual** |
| **Prometheus + Grafana** self-hosted | Custo de CPU/RAM no EC2 | Alto | Nenhum | Dados sensíveis que não podem sair do ambiente |
| **New Relic** | $0 até 100 GB/mês de dados ingeridos; depois caro | Baixo | Médio | Time pequeno que quer setup fácil com tier generoso |
| **Datadog** | $15–23/host/mês (Standard) | Baixo | Alto | Time > 5 pessoas, compliance enterprise, SLA formal |
| **Splunk** | Enterprise — muito caro | Alto | Alto | Compliance regulatório pesado (banco, saúde) |

## Recomendação para o projeto atual — Grafana Cloud free

**Por quê:**
- Micrometer já presente → adicionar `micrometer-registry-prometheus` + configurar remote write.
- Limite de 10k séries ativas é suficiente para o projeto (< 50 séries reais em uso).
- 50 GB de logs/mês é suficiente para um serviço de baixo tráfego.
- Custo: $0. Se o projeto crescer além do free tier, reavaliar.

**Como implementar (fluxo de dados):**
```
Spring Boot Actuator → Micrometer → Prometheus exporter (scrape local)
                                  → Grafana Alloy → Grafana Cloud (remote write)

stdout (JSON logs) → Grafana Alloy → Grafana Loki (Cloud)
```

**O que CloudWatch ainda faz sentido manter:**
- Alertas de infra nativos: `CPUUtilization`, `FreeStorageSpace` (RDS), `StatusCheckFailed` (EC2) — são **gratuitos** e sem configuração.
- Não mover métricas de aplicação (Micrometer) para CloudWatch — esse é o custo que explode.

## Critério de decisão por contexto

**Budget < $20/mês:** Grafana Cloud free tier (métricas + logs + traces).

**Dados sensíveis não podem sair do ambiente:** Prometheus + Grafana self-hosted na própria EC2 (adicionar ~10% de uso de CPU/RAM).

**Time cresceu para 5+ pessoas e precisa de features de colaboração/alerting avançado:** New Relic (free tier generoso) ou Datadog (mais caro, mas melhor UX para times).

**Compliance regulatório (LGPD, PCI, SOC2):** self-hosted ou Datadog/Splunk com DPA assinado.

## OpenTelemetry — o padrão aberto para traces

**Por que OTel e não vendor-specific SDK:**
- Instrumentação uma vez → dados vão para qualquer backend (Grafana Tempo, Jaeger, Datadog, New Relic).
- Sem lock-in: trocar de plataforma não exige reinstrumentar a aplicação.
- Spring Boot 3+ tem auto-instrumentação OTel via `spring-boot-starter-actuator` + dependência `opentelemetry-spring-boot-starter`.

**Quando adicionar traces:** quando houver múltiplos serviços ou quando latência de endpoint específico é difícil de diagnosticar só com métricas.

**Para o projeto atual (single service):** traces são opcionais. Métricas + logs cobrem 90% dos casos de diagnóstico.

## Logs — structured logging como base

Independente da plataforma, logs em JSON são mais fáceis de ingerir e consultar:

```xml
<!-- logback-spring.xml — formato JSON pra prod -->
<springProfile name="prod">
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
  </appender>
</springProfile>
```

**O que nunca logar:** tokens, JWTs, senhas, CPF, dados financeiros brutos — independente da plataforma.

**Retenção:** definir política antes de escolher plataforma. Grafana Cloud free guarda 14 dias; CloudWatch Logs mantém indefinidamente se não configurado (= custo crescente).

## Checklist para ADR de observabilidade

- [ ] Os três pilares cobertos: qual ferramenta pra metrics, logs e traces.
- [ ] Custo mensal estimado da plataforma escolhida (verificar pricing atual).
- [ ] Alternativas avaliadas e descartadas com justificativa.
- [ ] Lock-in avaliado: trocar de plataforma no futuro exige o quê?
- [ ] Dados sensíveis: logs/métricas não expõem PII ou tokens.
- [ ] Retenção definida (logs e métricas — não acumular indefinidamente).
- [ ] Critério de saída do free tier: a partir de qual escala o custo muda?

## Ler junto

- Skill `otimizacao-custos-aws` — decisão de plataforma impacta diretamente o custo.
- Skill `jvm-e-performance` — Micrometer e Actuator: instrumentação já em place.
- `docs/architecture/especificacao-tecnica.md` — stack atual (Spring Boot 3, Actuator).
