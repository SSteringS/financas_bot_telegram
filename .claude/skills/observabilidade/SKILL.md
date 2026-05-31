---
name: observabilidade
description: >
  Estrategia de observabilidade — comparativo de plataformas (CloudWatch, Grafana Cloud,
  Datadog, New Relic, Prometheus self-hosted, Splunk), os tres pilares (metrics/logs/traces),
  OpenTelemetry como padrao aberto, criterios de decisao por custo e escala. Carregar
  quando a proposta envolve monitoring strategy, plataforma de observabilidade, alertas,
  logging centralizado, ou custo de CloudWatch esta em discussao.
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
- **Sinal concreto:** "CloudWatch", "Datadog", "Grafana", "Prometheus", "Splunk",
  "New Relic", "OpenTelemetry", "traces", "dashboards", "alertas" no contexto.

## Os três pilares — o que cada plataforma precisa cobrir

| Pilar | O que é | Exemplo de coleta em Spring Boot |
|---|---|---|
| **Metrics** | Séries temporais (CPU, heap, latência, pool JDBC) | Micrometer + exporter (CloudWatch, Prometheus, Datadog) |
| **Logs** | Eventos estruturados com contexto (JSON) | stdout JSON → coletor → plataforma |
| **Traces** | Rastreamento de um request de ponta a ponta | OpenTelemetry agent + backend (Grafana Tempo, Jaeger, Datadog) |

**Vantagem de Spring Boot + Micrometer:** instrumentação de métricas é desacoplada do backend.
Trocar de CloudWatch para Grafana = mudar a dependência Maven do exporter, não o código da aplicação.

## Comparativo de plataformas

| Plataforma | Custo | Setup | Lock-in | Melhor para |
|---|---|---|---|---|
| **CloudWatch** | $$: custom metrics $0.30/métrica/mês; logs $0.50/GB | Zero (nativo AWS) | Alto | Alertas de infra nativos (EC2, RDS) — gratuitos |
| **Grafana Cloud** (free tier) | **$0** até 10k séries, 50 GB logs, 50 GB traces/mês | Médio | Baixo | Melhor custo/esforço para serviços de baixo tráfego |
| **Prometheus + Grafana** self-hosted | CPU/RAM da instância onde roda | Alto | Nenhum | Dados sensíveis que não podem sair do ambiente |
| **New Relic** | $0 até 100 GB/mês de dados ingeridos; depois caro | Baixo | Médio | Time pequeno que quer setup fácil com tier generoso |
| **Datadog** | $15–23/host/mês (Standard) | Baixo | Alto | Time > 5 pessoas, compliance enterprise, SLA formal |
| **Splunk** | Enterprise — muito caro | Alto | Alto | Compliance regulatório pesado (banco, saúde) |

## Critério de decisão por contexto

**Budget < $20/mês:** Grafana Cloud free tier (métricas + logs + traces).
Adicionar `micrometer-registry-prometheus` + remote write → zero código de aplicação.

**Dados sensíveis não podem sair do ambiente:** Prometheus + Grafana self-hosted
(adiciona ~10% de uso de CPU/RAM na instância que hospedar).

**Time cresceu para 5+ pessoas, precisa de alerting avançado e colaboração:** 
New Relic (free tier generoso) ou Datadog (mais caro, melhor UX).

**Compliance regulatório (LGPD, PCI, SOC2):** self-hosted ou Datadog/Splunk com DPA assinado.

**Armadilha do CloudWatch + Micrometer:** `micrometer-registry-cloudwatch2` emite uma série
por combinação de tags. Um endpoint com 3 tags = 10+ séries = ~$3/mês por endpoint.
Escala mal — preferir Grafana Cloud para métricas de aplicação, CloudWatch apenas para
alertas de infra nativos (CPU, storage) que são gratuitos.

## Fluxo de dados — Spring Boot com Grafana Cloud

```
Spring Boot Actuator → Micrometer → Prometheus exporter (scrape local)
                                  → Grafana Alloy → Grafana Cloud (remote write)

stdout (JSON logs) → Grafana Alloy → Grafana Loki (Cloud)

OpenTelemetry agent → Grafana Tempo (Cloud)
```

O que continuar usando o CloudWatch nativo:
- Alertas de infra: CPU, FreeStorageSpace (RDS), StatusCheckFailed (EC2) — gratuitos.
- Não mover métricas de aplicação (Micrometer) para CloudWatch.

## OpenTelemetry — o padrão aberto para traces

**Por que OTel e não SDK vendor-specific:**
- Instrumentar uma vez → dados vão para qualquer backend (Grafana Tempo, Jaeger, Datadog, New Relic).
- Sem lock-in: trocar de plataforma não exige reinstrumentar a aplicação.
- Spring Boot 3+ tem auto-instrumentação via `opentelemetry-spring-boot-starter`.

**Quando adicionar traces:** múltiplos serviços, ou latência de endpoint difícil de diagnosticar só com métricas.
Para serviço único, métricas + logs cobrem 90% dos casos de diagnóstico.

## Logs — structured logging como base

Independente da plataforma, logs em JSON são mais fáceis de ingerir:

```xml
<!-- logback-spring.xml — formato JSON em produção -->
<springProfile name="prod">
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
  </appender>
</springProfile>
```

**Nunca logar:** tokens, JWTs, senhas, CPF, dados financeiros brutos.
**Retenção:** definir política antes de escolher plataforma. Grafana Cloud free guarda 14 dias;
CloudWatch mantém indefinidamente se não configurado (custo crescente).

## Checklist para ADR de observabilidade

- [ ] Os três pilares cobertos: qual ferramenta pra metrics, logs e traces.
- [ ] Custo mensal estimado da plataforma escolhida (verificar pricing atual).
- [ ] Alternativas avaliadas e descartadas com justificativa.
- [ ] Lock-in avaliado: trocar de plataforma no futuro exige o quê?
- [ ] Dados sensíveis: logs/métricas não expõem PII ou tokens.
- [ ] Retenção definida (logs e métricas — não acumular indefinidamente).
- [ ] Critério de saída do free tier: a partir de qual escala o custo muda?

## Ler junto

- Skill `otimizacao-custos-aws` — decisão de plataforma impacta diretamente o custo AWS.
- Skill `jvm-e-performance` — Micrometer e Actuator: instrumentação de métricas da JVM.
- `docs/architecture/especificacao-tecnica.md` — stack atual do projeto (ler para saber qual instrumentação já existe antes de propor mudança).
