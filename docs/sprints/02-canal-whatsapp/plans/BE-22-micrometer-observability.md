# BE-22 — Micrometer + counters + timers fim-a-fim (observability do listener + entrada + chamadas externas)

> **Intake (contrato de entrada da task)**
>
> - **Origem:** TODOs deixados pela BE-21a no `NotificacaoComprovanteListener` (`// TODO BE-22: incrementar counter de falha do listener (metric)`) + spec do arquiteto `docs/architecture/adapter-whatsapp-cloud-api.md` §8.1 (requisito de métricas — timer fim-a-fim por tipo/canal/resultado, timers de queries DB e chamadas externas).
> - **Prioridade:** média. Não bloqueia BE-19/BE-20 (canal WhatsApp funcionando), mas **deve estar em prod antes de BE-20 ir live em volume real** — observability é rede pra debugar a migração WhatsApp em prod.
> - **Esforço:** **médio-alto** (~4–7h). Adicionar deps + setup do Micrometer + instrumentação em 4–5 pontos (listener, MensagemEntranteService, WhatsAppMessageSenderService, WhatsAppMediaDownloaderService, S3ImageUploadService) + testes + config CloudWatch + properties.
> - **Território / quem executa:** `financas_bot_telegram/` (pom + java + properties) → **Claude do back**.
> - **Branch:** `feature/be-22-micrometer-observability`, a partir de `develop`.
> - **Dependências:**
>   - **DEP-09 em `develop`** ✅ — CW log group + CW agent config + IAM `CloudWatchAgentServerPolicy` já na EC2. **Substrato pronto pra publicar métricas custom.**
>   - **BE-21a em `develop`** ✅ — listener com o TODO marcado.
>   - **BE-19 (adapter de entrada WhatsApp) — RECOMENDADO, NÃO BLOQUEANTE:** instrumentar o caminho de entrada agora cobre Telegram; quando WhatsApp entrar, métricas passam a refletir também (dimensão `canal=WHATSAPP` aparece no console). Se BE-22 mergear antes da BE-19, o canal WhatsApp simplesmente ainda não vai gerar timer com `canal=WHATSAPP` até deployar BE-19. Sem conflito.
> - **Riscos:**
>   1. **Custo de CloudWatch custom metrics** ($0.30/métrica/mês × cardinalidade). Mitigação: dimensões de baixa cardinalidade (`tipo`, `canal`, `resultado`) → ~50 métricas → ~$15/mês. **Sem dimensões ID-específicas** (`requisitanteId`, `wamid`).
>   2. **Dimension cardinality explosion** silenciosa. Mitigação: `aprendizado/cloudwatch-metric-dimensions.md` lido; lista finita de valores enumerados; revisão obrigatória do Reviewer.
>   3. **Configuração errada do registry** publicando métricas no namespace errado → alarmes futuros não acham. Mitigação: namespace explícito (`finbot/app`, consistente com DEP-09), smoke test pós-deploy verificando métricas aparecendo no console.
>   4. **Async timer mal-medido** — `@Async` adiciona delay de fila do executor entre AFTER_COMMIT e execução; medir do início do método do listener (não do publishEvent) capta só a execução. Mitigação: documentar o ponto de medição no plano e no código.
>   5. **Throttling do PutMetricData** — limite de 1.000 transactions/sec por região (longe do nosso volume); irrelevante na prática.

---

## Contexto

Pós-overnight 2 (BE-21a) o `NotificacaoComprovanteListener` (em `application/event/`) já é o ponto onde a notificação async acontece — `@TransactionalEventListener(AFTER_COMMIT) + @Async`, com try/catch interno e log `ERROR` em falha, mas **sem métrica de falha** (TODO marcado pela própria task). A spec §8.1 também aponta como requisito **timer fim-a-fim por tipo/canal/resultado** (visibilidade da UX real do usuário) + **timers em chamadas externas** (Graph API e S3, onde estão as falhas/lentidão que não controlamos).

**O que já existe e BE-22 não precisa criar:**
- Infra de logs/alarmes no CloudWatch (DEP-09).
- IAM da EC2 com `CloudWatchAgentServerPolicy` (permite `cloudwatch:PutMetricData`).
- CW agent rodando na EC2 (instalado pós-DEP-09; coleta CPU/disco/memória + log file).
- Namespace `finbot/app` em uso (log metric filter `finbot/app/errors` da DEP-09).

**O que não existe e BE-22 precisa adicionar:**
- Nenhuma dependência Micrometer/Actuator no pom — adicionar.
- Nenhuma instrumentação em código — adicionar nos 4–5 pontos abaixo.
- Nenhum exporter pra CloudWatch (o CW agent só lê o log file e métricas de sistema; **não** lê métricas Micrometer da app) — adicionar `micrometer-registry-cloudwatch2`, que publica direto via `PutMetricData` API.

## Decisão / abordagem

**Stack:** `spring-boot-starter-actuator` (habilita Micrometer + endpoint `/actuator/metrics`) + `micrometer-registry-cloudwatch2` (publica direto pra CloudWatch via SDK AWS, autenticando com instance profile — IAM já está).

**Por que publicar direto via SDK (e não via CW agent intermediário):**
- App → CW direto é o caminho mais curto e idiomático no Spring Boot.
- CW agent **não** consome endpoint Prometheus por default na nossa config (faria Prometheus + scrape uma camada a mais).
- IAM `CloudWatchAgentServerPolicy` cobre `PutMetricData` — sem mudança em Terraform.
- Latência de publicação: Micrometer batched a cada `step` (configurável, recomendar `1m`).

**Namespace:** `finbot/app` (mesmo dos logs/log-metric-filter da DEP-09; um lugar só no console).

**Dimensões padronizadas** (baixa cardinalidade — segue `aprendizado/cloudwatch-metric-dimensions.md`):
- `tipo`: `PEDIDO` | `COMPROVANTE` (2 valores)
- `canal`: `TELEGRAM` | `WHATSAPP` (2 valores; futuro DISCORD)
- `resultado`: `SUCESSO` | `FALHA_INFRA` | `FALHA_NEGOCIO` (3 valores — distinguir erro de negócio = legenda inválida, do erro de infra = banco/Graph API fora)

**Conjuntos de timer típicos:** 2 × 2 × 3 = 12 séries por timer name. Aceitável.

**Sem dimensões ID-específicas** (`requisitanteId`, `wamid`, `pedidoId`, `chatId`). Essas vão em **log estruturado** (já feito pelo DEP-09), não em métrica. Métrica é agregado; ID é evento.

**Métricas a publicar (visão consolidada):**

| Métrica | Tipo | Dimensões | Onde é medida | Pra que serve |
|---|---|---|---|---|
| `mensagem_entrante_processamento` | Timer | `tipo`, `canal`, `resultado` | `MensagemEntranteService.processar()` em volta da escolha de strategy + execução | UX fim-a-fim — quanto tempo do dispatch até gravar pedido/comprovante |
| `notificacao_envio` | Timer | `canal`, `resultado` | `NotificacaoComprovanteListener.onComprovanteRegistrado()` em volta da chamada ao notificador | Latência da notificação async — quanto tempo entre AFTER_COMMIT e mensagem entregue |
| `notificacao_falha` | Counter | `canal`, `motivo` (`SEM_NOTIFICADOR` \| `EXCEPTION`) | catch block do listener + branch de `notificador == null` | Conta falhas de notificação por causa — substitui o `// TODO BE-22` |
| `whatsapp_graph_api_chamada` | Timer | `operacao` (`ENVIAR_TEXTO` \| `ENVIAR_TEMPLATE` \| `DOWNLOAD_MEDIA_URL` \| `DOWNLOAD_MEDIA_BYTES`), `resultado` (`SUCESSO` \| `FALHA`) | Em volta de cada `restClient` call em `WhatsAppMessageSenderService` e `WhatsAppMediaDownloaderService` | Latência e taxa de erro do canal externo — caminho de falha §4.5 |
| `s3_upload` | Timer | `bucket`, `resultado` | Em volta da chamada em `S3ImageUploadService` | Latência/erro do storage |

**Fora desta task (registrar como pendência se necessário):**
- Timer das queries DB (idempotência claim + writes de pedido/comprovante). Requer ou instrumentação manual no adapter de idempotência + JPA listeners, ou um datasource proxy. Pode entrar em BE-22b ou EVO futura. **Spec §8.1 cita; a gente registra que ficou pra depois pra não inchar BE-22.**
- Métricas JVM/HTTP do Actuator (vão por padrão — `jvm.*`, `http.server.requests`, `process.uptime`, etc. — só precisam ser configuradas pra publicar **ou não** no CW pra não inflar custo; ver "Filtro de métricas exportadas" abaixo).
- Alarmes sobre as métricas novas — fica pra próxima task (calibrar com dado real depois de 1-2 semanas em prod). Análogo à DEP-09 ter feito métrica `finbot/app/errors` + alarme `error_rate` no mesmo PR; aqui o trade-off é deixar publicar primeiro, criar alarme com baseline depois.

**Filtro de métricas exportadas:** Actuator habilita ~80 métricas built-in (JVM heap, GC, threads, HTTP timing, etc.). Publicar TODAS no CW custa caro ($0.30 × 80 ≈ $24/mês só pra built-ins). Decisão: **whitelist explícita** das métricas custom (`mensagem_entrante_*`, `notificacao_*`, `whatsapp_graph_api_*`, `s3_*`) + um subset pequeno de JVM essencial (`jvm.memory.used`, `jvm.threads.live`). Configurar via `management.metrics.export.cloudwatch.*` properties + `MeterFilter` bean se necessário.

## Escopo / arquivos

### Modificar — `pom.xml`

- Adicionar `spring-boot-starter-actuator`.
- Adicionar `io.micrometer:micrometer-registry-cloudwatch2`.
- Adicionar `software.amazon.awssdk:cloudwatch` (vem como transitive do registry; confirmar versão consistente com o resto do SDK no projeto).

### Criar

- `application/metrics/MetricsConstants.java` — strings das métricas, dimensões, valores enumerados. Centralizar evita typos em string literal (`"tipo"` vs `"Tipo"` quebra dimension matching silenciosamente — vide aprendizado).
- `application/metrics/MetricsConfig.java` (`@Configuration`) — `MeterRegistryCustomizer<CloudWatchMeterRegistry>` com tags globais (`@Value("${spring.profiles.active}")` como tag `env`?), filtros de métricas exportadas (whitelist), e config de timers (`Timer.builder(...).publishPercentiles(0.5, 0.95).register(meterRegistry)`).

### Modificar — instrumentação

- `application/services/MensagemEntranteService.processar()` (mergeada via BE-17 + FIX-idempotencia):
  - Envolver a parte de "escolher strategy + executar" num `Timer.Sample.start()` / `.stop(timer)` com dimensões `tipo` (derivado da strategy escolhida — `PaymentRequestStrategy` → `PEDIDO`, `PaymentProofStrategy` → `COMPROVANTE`), `canal` (do DTO), `resultado` (`SUCESSO` ao final do happy path; `FALHA_NEGOCIO` no caso `findFirst` vazio → `InvalidMessageFormatException`; `FALHA_INFRA` em qualquer outra exceção que escape).
  - **Atenção:** o `tentarClaim` retornando `false` (já processado) **não** entra na métrica — não é processamento real. Pular o stop nesse caso.

- `application/event/NotificacaoComprovanteListener.onComprovanteRegistrado()`:
  - Timer `notificacao_envio` em volta do bloco try (do início do try até o `notificar(dto)` ou catch).
  - Counter `notificacao_falha` no catch (`motivo=EXCEPTION`) e no branch `notificador == null` (`motivo=SEM_NOTIFICADOR`).
  - Remover o `// TODO BE-22` (substituído pelo counter).

- `adapters/out/whatsapp/service/WhatsAppMessageSenderService`:
  - Timer `whatsapp_graph_api_chamada` em volta de cada call HTTP. `operacao=ENVIAR_TEXTO` no `enviarTexto`, `operacao=ENVIAR_TEMPLATE` no `enviarTemplate`. `resultado=SUCESSO` ou `FALHA` (catch).

- `adapters/out/whatsapp/service/WhatsAppMediaDownloaderService`:
  - Timer `whatsapp_graph_api_chamada` em volta dos dois calls. `operacao=DOWNLOAD_MEDIA_URL` no GET do `media_id`, `operacao=DOWNLOAD_MEDIA_BYTES` no GET da URL.

- `adapters/out/s3/service/S3ImageUploadService` (path provável; confirmar):
  - Timer `s3_upload` em volta da chamada `putObject`. Dimensões `bucket` (`@Value`), `resultado`.

### Modificar — `application*.properties`

- **`application.properties` (base):**
  ```
  # Actuator — só endpoints essenciais
  management.endpoints.web.exposure.include=health,metrics,info
  management.endpoint.health.show-details=when-authorized

  # Micrometer CloudWatch export
  management.metrics.export.cloudwatch.namespace=finbot/app
  management.metrics.export.cloudwatch.step=1m
  management.metrics.export.cloudwatch.batch-size=20
  # Por padrão NÃO publica nada — habilitar só no profile prod
  management.metrics.export.cloudwatch.enabled=false
  ```

- **`application-prod.properties`:**
  ```
  management.metrics.export.cloudwatch.enabled=true
  ```
  E garantir que a região AWS está disponível via instance profile/env (provavelmente já está, mas confirmar com o `S3ImageUploadService` que já usa a SDK).

- **`application-dev.properties` (+ `.example`):** mantém `enabled=false` (default base) — em dev a gente vê pelo endpoint `/actuator/metrics` localhost sem publicar.

### Não tocar

- Terraform — DEP-09 já bastante. Alarmes sobre as métricas novas ficam pra próxima task.
- `logback-spring.xml` — log estruturado já está bom.
- Domain/usecases — não instrumentar lá; métricas vivem nas bordas (services, listeners, adapters externos).
- `MensagemEntranteService.processar()` core de idempotência (já refatorado pela FIX-idempotencia-porta-application) — só envolver com timer.
- Strategies (canal-agnósticas) — não precisam de timer próprio; o timer do `processar()` já cobre.

## Testes

- **Unit `MetricsConfigTest`** — registry inicializa, namespace correto, whitelist filtra fora `process.uptime` mas mantém `mensagem_entrante_processamento`.
- **Unit `NotificacaoComprovanteListenerTest`** (já existe — atualizar):
  - Cenário sucesso: counter `notificacao_envio` (timer) registra 1 sample; counter `notificacao_falha` permanece em 0.
  - Cenário `notificador == null` (canal sem implementação): counter `notificacao_falha{motivo=SEM_NOTIFICADOR}` incrementa.
  - Cenário exceção no `notificar(dto)`: counter `notificacao_falha{motivo=EXCEPTION}` incrementa.
- **Unit `MensagemEntranteServiceTest`** (atualizar):
  - `PaymentRequestStrategy` happy: timer `mensagem_entrante_processamento{tipo=PEDIDO,canal=TELEGRAM,resultado=SUCESSO}` registra 1 sample.
  - `PaymentProofStrategy` happy: idem com `tipo=COMPROVANTE`.
  - `InvalidMessageFormatException` (nenhuma strategy bate): timer com `resultado=FALHA_NEGOCIO`.
  - Exceção de infra (strategy lança `DatabaseException`): timer com `resultado=FALHA_INFRA`.
  - Mensagem já processada (claim retorna `false`): **NÃO** registra timer.
- **Unit `WhatsAppMessageSenderServiceTest`** e **`WhatsAppMediaDownloaderServiceTest`** — usar `SimpleMeterRegistry` no teste; verificar que timer registra sample com dimensão `operacao` correta.

**Não usar real CloudWatchMeterRegistry nos testes** — `SimpleMeterRegistry` cobre toda a verificação. CW só entra em smoke pós-deploy.

**Smoke pós-deploy (manual, BE-20 ou follow-up):**
- Disparar pedido pelo Telegram → conferir no console CW: namespace `finbot/app`, métrica `mensagem_entrante_processamento`, dimensões corretas, sample dentro de 1-2 min.
- Forçar uma falha (canal sem notificador, ou DB temporariamente fora) → ver `notificacao_falha` subir.
- Anotar baseline observado pra calibrar alarmes futuros.

**`testes_total`** deve subir uns 10–15 testes novos.

## Critérios de aceitação

- [ ] `pom.xml` com `spring-boot-starter-actuator` + `micrometer-registry-cloudwatch2` adicionados.
- [ ] Endpoint `/actuator/metrics` responde 200 com lista incluindo `mensagem_entrante_processamento`, `notificacao_envio`, `notificacao_falha`, `whatsapp_graph_api_chamada`, `s3_upload`.
- [ ] `MetricsConstants` centraliza nomes de métrica e dimensões — **zero string literal** disperso no código de instrumentação.
- [ ] `NotificacaoComprovanteListener` tem o counter de falha (+ remoção do `// TODO BE-22`) e timer.
- [ ] `MensagemEntranteService.processar()` tem timer fim-a-fim com 3 dimensões corretas; mensagem já processada (idempotência) **não** gera sample.
- [ ] Senders/downloaders WhatsApp e S3 upload instrumentados.
- [ ] Properties: export CloudWatch **habilitado só em `prod`**, namespace `finbot/app`, step `1m`.
- [ ] Whitelist de métricas exportadas pra CW (não publicar tudo do Actuator).
- [ ] `mvn test` verde com `testes_total` ≥ ~290 (depende de BE-19 ter mergeado antes ou não) e `testes_novos` ≥ 10.
- [ ] `mvn package -DskipTests` ok.
- [ ] Branch saiu de `develop` (fluxo novo: `git fetch && git checkout -b feature/be-22-micrometer-observability develop`).
- [ ] Território: só `financas_bot_telegram/`. NÃO toca `frontend/` nem `infra/`.
- [ ] Status report `docs/sprints/02-canal-whatsapp/status/BE-22.md` com frontmatter válido + anotar:
  - Métricas finais publicadas (lista + dimensões).
  - Custo estimado mensal (cardinalidade × $0.30).
  - Pendências marcadas (timer de queries DB, alarmes sobre as novas métricas, baseline a calibrar).

## Fora de escopo (explicitamente)

- **Timer das queries DB** (idempotência + writes de pedido/comprovante) — requer datasource proxy ou JPA listener; trabalho separado. Registrar como pendência (BE-22b ou EVO).
- **Alarmes sobre as métricas novas** — precisa baseline de dado real; entrar em task de calibração depois (BE-22b ou DEP-XX).
- **Endpoint Prometheus** — não vai. Path direto pra CW.
- **Dashboard customizado no console CW** — opcional, manual, pode entrar como pendência.
- **JVM metrics completas** — só whitelist mínima (`jvm.memory.used`, `jvm.threads.live`). Resto fica acessível via `/actuator/metrics` mas não publica no CW pra economizar custo.
- **HTTP request metrics** do Actuator (`http.server.requests`) — útil mas alta cardinalidade (path × method × status); filtrar fora da publicação no CW por enquanto.
- **Retry estruturado** no listener (item opcional da BE-21a) — não entra aqui; é trabalho de robustez separado.
- **`DatabaseException` 500 do Telegram** — débito conhecido; não aproveitar esta task pra mexer.

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Cardinalidade explode (alguém adiciona dimensão `requisitanteId` "por debug") | Média | Alto (custo + perf) | `MetricsConstants` centralizado; revisão obrigatória; lista finita enumerada documentada. |
| Métricas publicadas no namespace errado → alarmes futuros não acham | Baixa | Médio | Property explícita; smoke test pós-deploy. |
| `@Async` async timer mede só execução, não fila do executor | Alta (intencional) | Médio | Documentar no plano e no JavaDoc do listener: timer mede só o trabalho síncrono dentro do método, não a fila. Latência total (commit → mensagem entregue) é métrica diferente (futura). |
| Custo de CloudWatch escala silenciosamente | Média | Médio | Whitelist explícita; estimar custo no status; reavaliar trimestralmente. |
| `cloudwatch:PutMetricData` sem IAM | Baixa | Alto (silent — métricas somem) | Confirmar `CloudWatchAgentServerPolicy` cobre (cobre); smoke pós-deploy valida. |
| Quebra de testes existentes ao injetar `MeterRegistry` em services testados sem ele | Média | Baixo | Em testes, injetar `SimpleMeterRegistry` via `@MockBean` ou bean default; padrão Spring Boot. |
| Filtro de export deixa passar `http.server.requests` por default | Média | Médio (custo) | Whitelist por allow-list de prefixo (não deny-list). Teste valida que `http.*` não exporta. |
| Conflito de merge com FIX-idempotencia-porta-application (em execução) — ambos tocam `MensagemEntranteService` | Alta (estrutural) | Baixo (resolução simples) | BE-22 sai de develop depois da FIX mergear. **Coordenação:** FIX em paralelo agora, BE-22 espera develop limpo. |
| Conflito de merge com BE-19 — também toca `MensagemEntranteService` (string ERROR_MESSAGE) | Baixa | Baixo | Tocam linhas diferentes. Resolução fácil. Ordem provável: FIX → BE-22 → BE-19, ou FIX → BE-19 → BE-22. |

## Coordenação

- **Ordem sugerida das próximas tasks:**
  1. **Atual: FIX-idempotencia-porta-application** (back executando agora) — toca `MensagemEntranteService`.
  2. **Após FIX mergeado:** despachar BE-22 OU BE-19 (qualquer ordem; ambas saem de develop).
  3. **BE-22 pode ir antes da BE-19** porque o Telegram já roda em prod e a instrumentação tem valor imediato pra observar o canal vivo.
  4. **Recomendação:** BE-19 primeiro (frente de produto) → BE-22 depois (observability vem cobrir os dois canais de uma vez). Mas inverso também é defensável.
- **Atenção pro Reviewer (insumo pro checklist arquitetural — item #8 do backlog):**
  - Instrumentação não vaza pra domain (POJOs/records permanecem limpos).
  - `MeterRegistry` injetado por construtor (não estático).
  - `MetricsConstants` é constante de aplicação, não de infra — pode ficar em `application/metrics/` (decisão do implementador).
  - Verificar que nenhum timer tem dimensão de alta cardinalidade.
  - Verificar que custo estimado está no status.
- **Após merge:** atualizar `docs/STATE.md` mencionando que observability custom está em prod; smoke test em prod (manual) confirmando métricas chegam no console; anotar baseline observado.

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido com **custo estimado anotado**, e **revisão obrigatória do Reviewer** (toca custo de prod + dimensões silenciosas). Abrir PR pra `develop`; **não mergear sozinho**.

## Referências

- Spec: `docs/architecture/adapter-whatsapp-cloud-api.md` §8.1 (requisito de métricas: timers DB / chamadas externas / fim-a-fim por tipo).
- ADR 0014 (eventos in-process) — contexto do listener e do `@Async`.
- Aprendizado: `docs/aprendizado/cloudwatch-metric-dimensions.md` (identidade de métrica = (namespace, nome, dimensões); silent failure de alarme com mismatch).
- Aprendizado: `docs/aprendizado/observability-logs-externalizar.md` (escolha de CloudWatch como casa de obs).
- DEP-09: `docs/sprints/02-canal-whatsapp/plans/DEP-09-observability-infra.md` + status — substrato CW.
- BE-21a status: `docs/sprints/02-canal-whatsapp/status/BE-21a.md` (TODOs deixados explicitamente pra BE-22).
- Tarefa irmã/sucessora: BE-22b (a criar — alarmes sobre as métricas novas com baseline real).
- `docs/plans/BACKLOG-evolucao-workflow.md` item #8 (checklist arquitetural do Reviewer — instrumentação é área que beneficia de skill).
