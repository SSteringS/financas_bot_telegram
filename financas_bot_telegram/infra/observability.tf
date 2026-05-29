# ── DEP-09: Observability — log group, metric filter e alarmes básicos ──
# Sem SNS por ora; alarmes ficam só no console (calibrar canais depois).
# CW agent deve ser instalado manualmente na EC2 (ver status/DEP-09.md).

# Log group principal da aplicação
resource "aws_cloudwatch_log_group" "app_logs" {
  name              = "/finbot/app"
  retention_in_days = 30

  tags = { Name = "finbot-${var.env}-app-logs" }
}

# Metric filter: conta ocorrências de "ERROR" no log group → métrica custom
resource "aws_cloudwatch_log_metric_filter" "app_errors" {
  name           = "finbot-${var.env}-error-count"
  log_group_name = aws_cloudwatch_log_group.app_logs.name
  pattern        = "ERROR"

  metric_transformation {
    name          = "errors"
    namespace     = "finbot/app"
    value         = "1"
    default_value = "0"
    unit          = "Count"
  }
}

# Alarme 1: disco > 80% (métrica do CW agent — namespace CWAgent)
# O CW agent expõe disk_used_percent com dimensões host/path/device/fstype.
# Dimensões concretas só ficam disponíveis após o agent rodar na instância;
# o alarm fica em INSUFFICIENT_DATA até o agent enviar o primeiro ponto.
resource "aws_cloudwatch_metric_alarm" "disk_used_high" {
  alarm_name          = "finbot-${var.env}-disk-used-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "disk_used_percent"
  namespace           = "CWAgent"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "Uso de disco da EC2 acima de 80% (2 períodos de 5min)"
  treat_missing_data  = "missing"

  dimensions = {
    host   = aws_instance.finbot_app.private_dns
    path   = "/"
    device = "nvme0n1p1"
    fstype = "xfs"
  }
}

# Alarme 2: CPU > 80% (métrica built-in EC2 — namespace AWS/EC2)
resource "aws_cloudwatch_metric_alarm" "cpu_high" {
  alarm_name          = "finbot-${var.env}-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 3
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "CPU da EC2 acima de 80% (3 períodos de 5min)"
  treat_missing_data  = "missing"

  dimensions = {
    InstanceId = aws_instance.finbot_app.id
  }
}

# Alarme 3: taxa de ERROR > 5 em janela de 5min
resource "aws_cloudwatch_metric_alarm" "error_rate" {
  alarm_name          = "finbot-${var.env}-error-rate"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "errors"
  namespace           = "finbot/app"
  period              = 300
  statistic           = "Sum"
  threshold           = 5
  alarm_description   = "Mais de 5 linhas de ERROR no log da app em 5min"
  treat_missing_data  = "notBreaching"
}

# Outputs úteis para referência no status report e runbook
output "cw_log_group_name" {
  description = "Nome do log group da aplicação no CloudWatch"
  value       = aws_cloudwatch_log_group.app_logs.name
}

output "alarm_disk_name" {
  description = "Nome do alarme de disco"
  value       = aws_cloudwatch_metric_alarm.disk_used_high.alarm_name
}

output "alarm_cpu_name" {
  description = "Nome do alarme de CPU"
  value       = aws_cloudwatch_metric_alarm.cpu_high.alarm_name
}

output "alarm_error_rate_name" {
  description = "Nome do alarme de error rate"
  value       = aws_cloudwatch_metric_alarm.error_rate.alarm_name
}
