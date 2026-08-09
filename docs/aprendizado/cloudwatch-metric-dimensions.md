# CloudWatch — identidade de métrica e o silent failure do alarme com dimensões inconsistentes

## Contexto da dúvida

Na revisão da DEP-09 (observability infra), o Reviewer encontrou um bug que teria deixado o **alarme de disco** — a rede de segurança contra o incidente FIX-volume — **silenciosamente inoperante** em produção. Causa: o `cloudwatch-agent-config.json` tinha `append_dimensions` que carimbava `InstanceId`/`InstanceType` em todas as métricas publicadas, mas o alarme declarado em Terraform não incluía essas duas dimensões. O alarme nunca encontraria a métrica.

## Resumo destilado

**A identidade de uma métrica no CloudWatch é a tupla `(namespace, nome, conjunto_de_dimensões)` — onde "conjunto" é EXATO.** Não há matching parcial. Métricas com o mesmo namespace e nome mas conjuntos de dimensões diferentes são **métricas distintas**. Alarmes procuram a série temporal cujo conjunto de dimensões bate **exatamente** com o que declararam — ou não acham nada.

### Por que isso é perigoso

Quando o alarme não encontra a métrica, ele **não erra** — vai pra `INSUFFICIENT_DATA` e fica lá. Esse estado **não grita**: não sobe vermelho, não dispara notificação. Olhando rápido o console e vendo "nenhum alarme em estado ALARM", a percepção é "tudo bem" — mas a rede de segurança está **inoperante**. Só se descobre por inspeção deliberada ou por re-incidente.

É o pior tipo de bug numa rede de segurança: **falha silenciosa**.

### Como acontece na prática

- O **CloudWatch agent** publica métricas com dimensões definidas em duas fontes:
  - **No bloco de coleta** (ex.: pra `disk`, ele agrega métrica por `host`/`path`/`device`/`fstype`).
  - **No bloco `append_dimensions` global** (carimba TODAS as métricas com mais dimensões — ex.: `InstanceId`, `InstanceType`).
- A métrica publicada tem a **união** dos dois — ex.: `{host, path, device, fstype, InstanceId, InstanceType}`.
- O alarme declarado em Terraform precisa repetir o **conjunto inteiro** — não basta listar um subconjunto.

Outros dois cenários que **funcionam** (e ajudam a entender o porquê):
- **Métricas nativas da AWS** (ex.: `AWS/EC2 / CPUUtilization`) — são publicadas pela própria AWS com `InstanceId`. O alarme declara `InstanceId` e bate.
- **Métricas custom de `metric_filter` em log group** — publicadas sem dimensões. O alarme lê sem dimensões. Conjunto vazio = conjunto vazio → match.

### Decisão prática

Pra **uma única instância**, o melhor caminho é **NÃO usar `append_dimensions`**: o `host` (que o agent adiciona automaticamente do hostname) já identifica unicamente a fonte. `InstanceId`/`InstanceType` só pagam custo quando há frota e precisa desambiguar a mesma métrica de várias máquinas.

Se decidir manter o `append_dimensions`, **toda declaração de alarme precisa repetir essas dimensões** — e isso **acopla o alarme à `InstanceId` específica** (se recriar a EC2, precisa atualizar). Resiliência menor.

## Pontos-chave

- Identidade de métrica = `(namespace, nome, conjunto_de_dimensões)` — conjunto **exato**.
- **Match parcial não existe**: 4 dimensões no alarme + 6 na métrica = métricas diferentes; alarme não encontra nada.
- Alarme órfão fica em **`INSUFFICIENT_DATA`** indefinidamente — estado **silencioso** que parece "tudo bem".
- `append_dimensions` no agent carimba **todas** as métricas — efeito global, não opt-in por métrica.
- **Single instance:** `host` já basta; **`append_dimensions` é redundante e perigoso** (esquecer de repetir nos alarmes mata cada um).
- **Frota:** `append_dimensions` faz sentido, mas exige disciplina rígida de declarar o conjunto completo em cada alarme.
- Métricas nativas AWS (com `InstanceId`) e métricas de `metric_filter` (sem dimensões) **não passam pelo agent** — não são afetadas pelo `append_dimensions`.

## Pra aprofundar

- Modelo de identidade do CloudWatch (`PutMetricData` API: como dimensões definem séries temporais distintas).
- `aws_cloudwatch_metric_alarm` em Terraform: por que `dimensions` é mapa exato, não matcher.
- Alternativas a polling de alarme estático: **anomaly detection** + alarmes baseados em escala, que toleram dimensões variáveis.
- Estratégias de "alarme do alarme" — monitorar quantos alarmes estão em `INSUFFICIENT_DATA` por tempo prolongado, justamente pra pegar essa classe de falha silenciosa.
- Relação com `docs/aprendizado/observability-logs-externalizar.md` (a decisão de adotar CloudWatch).
</content>
