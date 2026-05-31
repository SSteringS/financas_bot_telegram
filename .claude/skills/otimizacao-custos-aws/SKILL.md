---
name: otimizacao-custos-aws
description: >
  Avaliacao de custo de servicos AWS — pricing model dos principais servicos (EC2, RDS,
  S3, Secrets Manager, CloudWatch), armadilhas de custo frequentes, criterios de
  right-sizing, Savings Plans, lifecycle policies e quando avaliar alternativas mais
  baratas. Carregar quando a proposta envolve novo servico AWS, comparativo de infra,
  right-sizing, ou ADR que afeta custo operacional.
load_pattern: contextual
used_by: [architect]
created: 2026-05-31
adr: 0015
status: ativa
---

# Skill — Otimização de Custos AWS

## Quando carregar (gatilho explícito)

- Proposta de novo serviço AWS (ElastiCache, SQS, Lambda, etc.).
- Comparativo de opções de infra com custo diferente.
- ADR de infra sendo escrita — custo estimado é campo obrigatório.
- Discussão de right-sizing de EC2, RDS ou container.
- **Sinal concreto:** nomes de serviços AWS, "instância", "escalar", "custo", "fatura" ou "pricing" no contexto.

## Regra de ouro

**Toda ADR que propõe serviço AWS deve incluir custo mensal estimado:**
- Cenário atual (baseline).
- Cenário de crescimento em 12 meses.
- Alternativa mais barata avaliada e por que foi ou não escolhida.

Sempre validar em [calculator.aws](https://calculator.aws) — preços mudam; nunca hardcodar valores.

## Pricing de referência — verificar calculadora para valores atuais

| Serviço | On-demand aprox. | Ponto de atenção |
|---|---|---|
| EC2 t4g.micro | ~$7/mês | Savings Plan 1 ano reduz ~35%; ARM Graviton é ~20% mais barato que x86 equivalente |
| EC2 t4g.small | ~$14/mês | 2 GB RAM — próximo nível de upsize após micro |
| RDS db.t4g.micro | ~$14/mês | Multi-AZ dobra o custo; backups automáticos grátis até o tamanho da instância |
| S3 Standard | $0.023/GB | Lifecycle pra IA depois de 30 dias → $0.0125/GB; Glacier Instant após 90 dias → $0.004/GB |
| Secrets Manager | $0.40/secret/mês | SSM Parameter Store SecureString: $0.05/parâmetro/mês — 8× mais barato para projetos simples |
| CloudWatch metrics nativas (EC2, RDS) | **Grátis** | CPU, FreeStorageSpace, NetworkIn/Out — não custam nada |
| CloudWatch custom metrics | $0.30/métrica/mês | Armadilha: Micrometer pode emitir 50+ séries → $15+/mês escondido |
| CloudWatch Logs | $0.50/GB ingestão | Retenção: definir política — logs sem TTL encarecem indefinidamente |
| CloudWatch Dashboards | $3/dashboard/mês | Alternativa: Grafana Cloud free tier |
| Data Transfer | $0.09/GB saída | Dentro da mesma região: grátis; para internet: cobrado |

## Armadilhas de custo frequentes

**CloudWatch + Micrometer:** exportar métricas de aplicação para CloudWatch parece natural mas é caro. Micrometer com `micrometer-registry-cloudwatch2` emite uma série por combinação de tags. Um único endpoint com 3 tags pode gerar 10+ séries. Preferir Grafana Cloud (free tier: 10k séries) ou Prometheus self-hosted.

**RDS Multi-AZ em dev/staging:** duplica o custo sem benefício real. Usar snapshot + restore para recriar ambientes não-produtivos quando necessário.

**Secrets Manager para muitos segredos:** para projetos com > 5 segredos, avaliar SSM Parameter Store SecureString. Break-even: 8 segredos em Secrets Manager = $3.20/mês vs $0.40/mês no SSM.

**S3 sem lifecycle:** objetos antigos ficam em Standard desnecessariamente. Lifecycle policy é configuração de 5 minutos com payback em semanas.

**Logs sem retenção definida:** CloudWatch Logs sem política de retenção acumulam indefinidamente. Definir 30 dias para logs de aplicação, 90 dias para logs de auditoria.

## Critérios de right-sizing de EC2

**Nunca escalar sem dados.** Coletar ao menos 2 semanas de métricas antes de propor upsize.

| Métrica | Threshold pra considerar upsize | Threshold pra considerar downsize |
|---|---|---|
| CPU (média) | > 60% sustentado | < 15% constante |
| CPU (pico) | > 85% por > 1h/dia | — |
| RAM usada | > 80% do disponível | < 40% constante |
| Swap usado | Qualquer uso constante | — |

**Opções de scaling de EC2:**
- **Vertical:** passar para o próximo tier (~2× o custo) — sem mudança de arquitetura.
- **Horizontal:** só se a aplicação for stateless + tiver load balancer → mudança arquitetural maior.

Antes de propor upsize: verificar se a causa é código (ex.: leak de memória, query sem índice) antes de comprar mais hardware.

## Savings Plans vs Reserved Instances vs On-Demand

| Opção | Compromisso | Desconto típico | Quando usar |
|---|---|---|---|
| On-Demand | Nenhum | 0% | Workload imprevisível, projeto novo, < 3 meses de uso |
| Savings Plan Compute 1 ano | 1 ano de gasto horário | ~35% | Instância sempre ligada > 6 meses |
| Savings Plan 3 anos | 3 anos | ~55% | Instância de produção estável, projeto consolidado |
| Reserved Instance | 1-3 anos + instância específica | ~40-60% | Menos flexível que Savings Plan |
| Spot | Interruptível | até 90% | Batch, CI runners — não para app principal |

**Guideline:** ficar em On-Demand até confirmar que o workload é estável e contínuo; depois avaliar Savings Plan Compute 1 ano.

## Quando avaliar alternativa a serviço AWS proprietário

Sinal: custo do serviço AWS > custo de rodar alternativa open-source na própria instância ou em cloud gratuita.

| Serviço AWS | Alternativa | Break-even |
|---|---|---|
| CloudWatch custom metrics | Grafana Cloud free (até 10k séries) | Imediato — free tier |
| CloudWatch Logs | Grafana Cloud Loki (50 GB/mês grátis) | Imediato — free tier |
| Secrets Manager (> 5 secrets) | SSM Parameter Store SecureString | ~5 segredos |
| ElastiCache | Redis self-hosted na própria instância | Sempre (para pequena escala) |

## Checklist para ADR com componente de infra AWS

- [ ] Custo mensal estimado no cenário atual (verificado em calculator.aws).
- [ ] Custo no cenário de crescimento em 12 meses.
- [ ] Alternativa mais barata avaliada (mesmo que rejeitada, registrar por quê).
- [ ] Armadilha de custo identificada e mitigada (ex: Micrometer + CloudWatch custom metrics).
- [ ] Lifecycle policy definida para S3 quando aplicável.
- [ ] Retenção de logs definida (nunca "infinita").
- [ ] Savings Plan avaliado se o workload for previsível e contínuo.

## Ler junto

- Skill `observabilidade` — decisão de plataforma de monitoring afeta custo diretamente.
- Skill `jvm-e-performance` — sizing de instância depende do footprint da JVM.
- `docs/architecture/especificacao-tecnica.md` — inventário de serviços e infra declarada do projeto (ler para aplicar os critérios acima ao contexto concreto).
