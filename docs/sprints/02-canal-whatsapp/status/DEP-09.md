---
task: DEP-09
titulo: "Observability infra — CloudWatch log group, alarmes e logback JSON"
data: 2026-05-28
branch: feature/dep-09-observability-infra
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: na
  testes: ok
  testes_total: 207
  testes_novos: 0
  branch_convencao: ok
  territorio: ok
commits:
  - pendente
pr: null
desvios: 1
pendencias_humano: 3
---

# DEP-09 — Observability infra (CloudWatch + logback JSON)

## O que foi feito

- `infra/observability.tf`: `aws_cloudwatch_log_group` `/finbot/app` (30 dias), `aws_cloudwatch_log_metric_filter` contando "ERROR" → métrica `finbot/app/errors`, 3 alarmes sem SNS (disco, CPU, error rate). Outputs do log group e nomes de alarme.
- `src/main/resources/logback-spring.xml`: ConsoleAppender sempre ativo + `RollingFileAppender` com layout JSON inline em `/var/log/finbot/app.log` ativado apenas no profile `prod`. Rotação diária, mantém 7 dias locais.
- `infra/observability/cloudwatch-agent-config.json`: config versionada do CW agent para coleta do log file + métricas de CPU, disco e memória.
- `infra/prod.tfvars`: corrigido alinhamento de `=` pelo `terraform fmt` (arquivo pre-existente; não é mudança funcional).
- `terraform fmt -check` + `terraform validate` + `mvn test` (207 testes unitários) + `mvn package -DskipTests`: todos verdes.

---

## Desvios do plano

1. **Logback sem `logstash-logback-encoder`** — O plano menciona `logstash-logback-encoder` como opção preferida para layout JSON; como a dependência não está no pom e adicionar uma nova dep não foi explicitamente autorizado, usou-se um `<pattern>` JSON inline. CloudWatch Logs Insights processa o formato corretamente (campos timestamp/level/message/logger presentes). Desvio de baixo impacto; se quiser o encoder real, basta adicionar a dep e ajustar o appender.

---

## Decisões tomadas durante a execução

- `disk_used_percent` como nome de métrica do CW agent (não `DiskSpaceUtilization` do agente legado). Namespace `CWAgent`, dimensões `host/path/device/fstype` para o volume raiz (`/`). O alarme fica em `INSUFFICIENT_DATA` até o agent rodar — esperado.
- `treat_missing_data = "missing"` nos alarmes de disco e CPU para não falso-alarmar antes do agent estar rodando; `treat_missing_data = "notBreaching"` no error_rate (sem dados = sem erros).
- Arquivo de log rotaciona diariamente em `/var/log/finbot/app.log`; o CW agent coleta em tempo real (não espera rotação).

---

## Decisões pendentes (esperando humano)

1. **`terraform apply`** — O humano deve rodar `terraform plan` e confirmar que só há adds (sem replace da EC2) antes de `apply`. Comandos abaixo.
2. **Instalação manual do CW agent na EC2** — `user_data` não re-roda; instalação é passo SSH manual (comandos abaixo).
3. **Diretório `/var/log/finbot/`** — Deve ser criado manualmente na instância antes de o app tentar escrever (`mkdir -p /var/log/finbot && chown finbot:finbot /var/log/finbot`). O profile `prod` ativa o FileAppender, então sem o diretório o app vai logar um erro na inicialização. Alternativa: adiar ativação do FileAppender até o DEP-07 (bootstrap.sh) ser executado.

---

## Próximos passos / observações pro próximo

### CHECKLIST MANUAL PÓS-APPLY (humano)

```bash
# 1. Terraform apply (confirmar só adds)
cd financas_bot_telegram/infra
terraform plan -out=dep09.tfplan
# Verificar: apenas aws_cloudwatch_log_group, aws_cloudwatch_log_metric_filter, 3x aws_cloudwatch_metric_alarm — ZERO replace
terraform apply dep09.tfplan

# 2. SSH na EC2
ssh -i ~/.ssh/finbot-prod-key.pem ec2-user@3.228.138.109

# 3. Instalar CW agent (Amazon Linux 2023)
sudo dnf install -y amazon-cloudwatch-agent

# 4. Copiar config
sudo cp /tmp/cloudwatch-agent-config.json /opt/aws/amazon-cloudwatch-agent/etc/cloudwatch-agent-config.json
# (scp o arquivo do repo para /tmp antes)

# 5. Iniciar e habilitar
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config \
  -m ec2 \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/cloudwatch-agent-config.json \
  -s
sudo systemctl enable amazon-cloudwatch-agent

# 6. Criar diretório do log da app
sudo mkdir -p /var/log/finbot
sudo chown finbot:finbot /var/log/finbot

# 7. Smoke test: verificar que logs chegam no CloudWatch
#    - Aguardar ~1 min após reiniciar o app (ou gerar um erro de propósito)
#    - No console AWS: CloudWatch > Log groups > /finbot/app > log streams
#    - Verificar que aparecem entradas JSON
#    - Verificar alarmes em CloudWatch > Alarms (devem estar OK ou INSUFFICIENT_DATA)
```

### Sinergia com DEP-07
Quando o DEP-07 (bootstrap.sh) for executado, capturar nele: instalação do CW agent, cópia do config, `mkdir /var/log/finbot`, `chown`. Isso elimina o passo SSH manual acima.

---

## Arquivos criados/modificados

- `financas_bot_telegram/infra/observability.tf` (novo: log group, metric filter, 3 alarmes, outputs)
- `financas_bot_telegram/infra/observability/cloudwatch-agent-config.json` (novo: config do CW agent)
- `financas_bot_telegram/src/main/resources/logback-spring.xml` (novo: ConsoleAppender + FileAppender JSON para prod)
- `financas_bot_telegram/infra/prod.tfvars` (modificado: formatação pelo `terraform fmt`)
