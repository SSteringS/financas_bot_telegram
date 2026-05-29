# Externalizar logs (observabilidade) — opções e o que realmente pesa no custo

## Contexto da dúvida

Pós-MVP, ver logs exigia **entrar na EC2 por SSH e rodar `journalctl`** — fricção real, ainda mais perto de uma mudança arriscada (migração do bot pra WhatsApp, EVO-01). Surgiu a pergunta: dá pra externalizar o log por um preço baixo? E isso seria caso de chamar um arquiteto?

## Resumo destilado

**No volume do projeto (um bot de um usuário + API de baixo tráfego), custo de log é desprezível em praticamente qualquer opção** — free tiers ou centavos/mês. Logo, a decisão **não é de preço**; é de **simplicidade, UX de busca e onde os dados moram**. Não justifica um arquiteto: é decisão de infra que o planner já homologa (ADR 0004), e mal há trade-off de custo.

Detalhe decisivo: a EC2 **já tem a IAM pronta** — o `security.tf` anexa `CloudWatchAgentServerPolicy` à role. Mandar log/métrica pro CloudWatch é só configurar o agente, sem mexer em permissão.

### Opções (do mais "encaixado" ao menos)

- **CloudWatch Logs (nativo AWS) — recomendado.** IAM já existe; free tier (5 GB ingestão/12 meses) cobre; depois ~US$0,50/GB ingerido + ~US$0,03/GB-mês de storage. UX de busca via Logs Insights (ok, não linda). **Bônus:** é também a casa natural dos **alarmes** (disco baixo, contagem de ERROR) — resolve a cegueira que deixou o disco encher sem aviso. Definir **retenção** (ex.: 30 dias) pra storage não crescer.
- **SaaS de log (Axiom, Better Stack, Papertrail).** UX/busca muito boas, free tiers generosos (Axiom especialmente). Custo zero no nosso volume. Trade-off: +1 vendor, +1 agente, dados saem da AWS.
- **Grafana Cloud (Loki).** LogQL + dashboards fortes, free tier cobre. Mais peças pra montar.
- **S3 + Athena.** Storage baratíssimo; ótimo pra arquivo/auditoria, ruim pra "tail" do dia a dia.
- **Self-host (Loki/Grafana na própria EC2).** ❌ Anula o propósito — come CPU/disco do t4g.micro (que já encheu uma vez).

### Recomendação

**CloudWatch Logs**: nativo, IAM já cabeada, free tier cobre, e centraliza logs + alarmes num lugar. Se um dia quiser busca/dashboard mais bonita, Axiom/Grafana entram por cima sem retrabalho.

## Pontos-chave

- **Custo de log ≈ zero no nosso volume** — escolha por simplicidade/UX, não por preço.
- A EC2 **já tem `CloudWatchAgentServerPolicy`** → CloudWatch é o caminho de menor atrito.
- CloudWatch mata dois coelhos: **logs externalizados** + **alarmes** (disco/CPU/ERROR) — fechando o gap de observabilidade da RETRO-01.
- Sempre setar **retenção** de log (senão storage cresce devagar pra sempre).
- Não self-hospedar stack de log numa t4g.micro — anula o ganho.
- Fazer a observabilidade **cedo no ciclo da EVO-01** — migração de canal é risco alto e você vai querer logs de fora pra debugar.
- Isso **não** justifica um arquiteto; o gatilho de arquiteto seria a densidade de decisões técnicas da própria EVO-01 (provider de WhatsApp, redesenho do adapter), não o log.

## Pra aprofundar

- CloudWatch agent (config de coleta de journald/arquivo) + Logs Insights (consulta).
- Metric filters → CloudWatch Alarms (ex.: alarme de `DiskSpaceUtilization`, contagem de `ERROR`).
- Structured logging (JSON no logback) pra busca melhor em qualquer backend.
- Trade-off vendor-lock vs UX (CloudWatch nativo vs SaaS de observabilidade).
- Relação com `RETRO-01` (ação #4: observabilidade mínima) e o incidente do disco cheio.
</content>
