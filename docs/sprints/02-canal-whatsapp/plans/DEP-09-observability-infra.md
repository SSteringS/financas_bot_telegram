# DEP-09 — Observability infra (CloudWatch: logs + alarmes)

> **Intake (contrato de entrada da task)**
>
> - **Origem:** RETRO-01 (ação #4 — observabilidade mínima) + spec `docs/architecture/adapter-whatsapp-cloud-api.md` §8.1 (a frente de observability é pré-requisito da migração WhatsApp). Incidente que motivou: disco da EC2 encheu sem aviso (FIX-volume).
> - **Prioridade:** alta — rede de segurança da sprint 02 e fecha gap conhecido.
> - **Esforço:** baixo-médio (poucos arquivos Terraform + config do agent; sem código de app além de logback).
> - **Território / quem executa:** `financas_bot_telegram/infra/` (Terraform) + `financas_bot_telegram/src/main/resources/` (logback) → **Claude do back**.
> - **Branch:** `feature/dep-09-observability-infra`, a partir de `develop`.
> - **Dependências:** nenhuma de código. A IAM `CloudWatchAgentServerPolicy` **já está anexada** à role da EC2 (`security.tf`), então mandar logs/métricas pra CloudWatch é só configurar.
> - **Riscos:**
>   1. **Alarmes ruidosos demais** (e-mails/SNS espurros que viram cego). Mitigação: começar **conservador** (limiares folgados; sem SNS de e-mail nesta task — só alarme no console). Calibrar com dado real depois.
>   2. **Custo crescer** se a retenção não for setada. Mitigação: log group com **retenção explícita** (30 dias).
>   3. **Conflito com Micrometer (BE-22)** se essa task tentar instrumentar código. Mitigação: **escopo desta task NÃO inclui Micrometer** — só logs + alarmes infra-level (ver "Não tocar").

---

## Contexto

Hoje "voamos cego": pra ver logs precisamos de SSH na EC2 + `journalctl`, e o disco encheu sem alarme (incidente FIX-volume). A sprint 02 vai mexer no canal de entrada (migração WhatsApp) — sem observability externalizada, debugar fica inviável. A spec do arquiteto §8.1 também aponta a observability como pré-requisito (e adiciona o requisito de métricas/Micrometer, que **fica pra BE-22**, depois do refactor BE-17, pra não conflitar com o move de pacotes).

**Conforme `docs/aprendizado/observability-logs-externalizar.md`:** CloudWatch é o caminho de menor atrito (IAM já cabeada, free tier cobre, vira casa de logs **e** alarmes).

## Decisão / abordagem

**Logs externalizados pra CloudWatch + alarmes básicos via Terraform.** A app passa a escrever log em arquivo (logback) com formato JSON; o **CloudWatch agent** (a ser instalado/configurado pelo humano via SSH no apply) lê o arquivo e empurra pro log group. Alarmes (disco, CPU, contagem de ERROR via metric filter) ficam declarados em Terraform.

**O que esta task NÃO faz** (deliberadamente):

- **Não instrumenta Micrometer/timers no código** — isso é BE-22, depois do BE-17 (que move strategies/orchestrator de pacote). Misturar agora vira conflito de merge.
- **Não instala/inicia o CW agent na instância** — `user_data` não re-roda (mesma razão do DEP-07); a instalação do agent é passo manual SSH no apply (documentado no status). Quando o DEP-07 for executado, o `bootstrap.sh` captura o agent também (sinergia).
- **Não configura SNS/e-mail dos alarmes** — começa só com alarme no console; calibrar canais depois.

## Escopo / arquivos

**Terraform (`financas_bot_telegram/infra/`):** criar `observability.tf` com:

- `aws_cloudwatch_log_group` `/finbot/app` (retenção 30 dias).
- `aws_cloudwatch_log_metric_filter` que conta a string `"ERROR"` no log group → métrica custom `finbot/app/errors`.
- `aws_cloudwatch_metric_alarm` (no console, sem SNS por enquanto):
  - **`disk_used_high`** — `DiskSpaceUtilization` (do CW agent) > 80% por 2 datapoints de 5min.
  - **`cpu_high`** — `CPUUtilization` (built-in EC2) > 80% por 3 datapoints de 5min.
  - **`error_rate`** — soma da métrica `finbot/app/errors` > 5 em 5min.
- Outputs do log group name + nomes dos alarmes (pra documentação).

**App-side (`financas_bot_telegram/src/main/resources/`):**

- `logback-spring.xml` (ou ajuste no existente): adicionar um **FileAppender** escrevendo em `/var/log/finbot/app.log` (path padrão que o agent lê), com **layout JSON** (`logstash-logback-encoder` se não estiver no `pom`; senão, pattern simples com chaves). Manter o appender de console (pra dev e `journalctl`). Em prod, ativar via profile.
- `application-prod.properties`: garantir o profile ativa o FileAppender / qualquer property que o logback consuma.

**Config do CW agent (versionada no repo, instalada manualmente):**

- `infra/observability/cloudwatch-agent-config.json` — arquivo de config do CW agent listando o log group, o caminho do log file, e as métricas básicas (disk, cpu, memory). Vai pra `/opt/aws/amazon-cloudwatch-agent/etc/` na instância via passo manual SSH.

**Não tocar:** `frontend/`, nada de código de domínio/app além do logback config.

## Critérios de aceitação

- [ ] `terraform fmt -check` e `terraform validate` ok. `terraform plan` (humano roda) mostra **só adds** — nenhum replace da EC2.
- [ ] `mvn package -DskipTests` ok (logback config válido).
- [ ] `mvn test` verde (mudança de logback não pode quebrar testes).
- [ ] `cloudwatch-agent-config.json` versionado e documentado (path de instalação na instância + comando de start).
- [ ] Status report em `docs/sprints/02-canal-whatsapp/status/DEP-09.md` com frontmatter válido + **comandos manuais documentados** (apply, ssh, install agent, start, smoke test no console CW).

## Passos manuais (post-apply, pro humano fazer de manhã)

Documentados no status report; resumo:

1. `terraform apply` na `infra/` (confirmar in-place; o log group/alarmes são adds).
2. SSH na EC2: instalar o CW agent (`dnf install amazon-cloudwatch-agent` ou pacote AL2023), copiar o `cloudwatch-agent-config.json` pra `/opt/aws/amazon-cloudwatch-agent/etc/`, iniciar (`systemctl enable --now amazon-cloudwatch-agent`).
3. Smoke test: gerar uma linha de log na app (ou um `logger.error` num healthcheck) → ver aparecer no log group `/finbot/app` em ~1min.
4. Confirmar os 3 alarmes no console (`OK` ou `INSUFFICIENT_DATA` no início).

## Coordenação

- **BE-22 (Micrometer)** vem depois do **BE-17** mergeado — instrumentação no código, dimensões `tipo`/`canal`/`resultado`. Esta task só prepara o substrato (CW agent rodando + log group).
- **DEP-07 (bootstrap.sh)** quando entrar, captura a instalação/config do CW agent — coordenar pra não duplicar a etapa manual.
- **FIX de disco (FIX-volume)** já aplicado — o alarme de disco aqui é a rede de segurança que faltava no incidente.

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build/lint/testes verdes; `terraform validate` limpo), status report válido, e **revisão do Reviewer** (toca infra de prod). Abrir PR pra `develop`; **não mergear sozinho**.

## Referências

- `docs/aprendizado/observability-logs-externalizar.md` (escolha de CloudWatch + raciocínio de custo)
- RETRO-01, ação #4 (`docs/retrospectivas/RETRO-01-mvp-fase3.md`)
- `docs/architecture/adapter-whatsapp-cloud-api.md` §8.1 (requisito de métricas — pra BE-22, não esta)
- `financas_bot_telegram/infra/security.tf` (IAM `CloudWatchAgentServerPolicy` já anexada)
- `docs/plans/DEP-07-codificar-provisionamento-ec2.md` (sinergia futura: capturar agent no bootstrap.sh)
</content>
