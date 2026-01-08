# 1. IAM Role para o Scheduler
resource "aws_iam_role" "scheduler_role" {
  name = "finbot-${var.env}-iam-role-scheduler"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "scheduler.amazonaws.com" }
    }]
  })
}

# 2. Política para permitir Start/Stop no RDS
resource "aws_iam_policy" "rds_stop_start" {
  name = "finbot-${var.env}-policy-rds-automation"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["rds:StartDBInstance", "rds:StopDBInstance"]
      Resource = [aws_db_instance.finbot_mysql.arn]
    }]
  })
}

resource "aws_iam_role_policy_attachment" "attach_automation" {
  role       = aws_iam_role.scheduler_role.name
  policy_arn = aws_iam_policy.rds_stop_start.arn
}

# 3. Agendamento para DESLIGAR
resource "aws_scheduler_schedule" "stop_rds" {
  name       = "finbot-${var.env}-schedule-stop-rds"
  group_name = "default"

  flexible_time_window {
    mode = "OFF"
  }

  schedule_expression = "cron(0 1 * * ? *)"

  target {
    arn      = "arn:aws:scheduler:::aws-sdk:rds:stopDBInstance"
    role_arn = aws_iam_role.scheduler_role.arn

    input = jsonencode({
      DbInstanceIdentifier = aws_db_instance.finbot_mysql.identifier
    })
  }
}

# 4. Agendamento para LIGAR
resource "aws_scheduler_schedule" "start_rds" {
  name       = "finbot-${var.env}-schedule-start-rds"
  group_name = "default"

  flexible_time_window {
    mode = "OFF"
  }

  schedule_expression = "cron(0 11 * * ? *)"

  target {
    arn      = "arn:aws:scheduler:::aws-sdk:rds:startDBInstance"
    role_arn = aws_iam_role.scheduler_role.arn

    input = jsonencode({
      DbInstanceIdentifier = aws_db_instance.finbot_mysql.identifier
    })
  }
}