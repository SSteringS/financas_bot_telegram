# 1. IAM Role para a Lambda (A identidade da função)
# Esta role permite que a Lambda assuma uma identidade dentro da AWS para interagir com outros serviços.
resource "aws_iam_role" "lambda_exec" {
  name = "finbot-${var.env}-iam-role-lambda"

  # O "assume_role_policy" define que o serviço Lambda tem permissão para usar esta role.
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })

  tags = { Name = "finbot-${var.env}-iam-role-lambda" }
}

# 2. Política para Logs (Boas práticas de Operação)
# Toda Lambda precisa de permissão para escrever logs no CloudWatch para você debugar depois.
resource "aws_iam_role_policy_attachment" "lambda_logs" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# 3. Política para VPC (Necessário porque a Lambda está na rede privada)
resource "aws_iam_role_policy_attachment" "lambda_vpc_access" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

# 4. Secrets Manager - O Cofre
resource "aws_secretsmanager_secret" "app_secrets" {
  name        = "finbot-${var.env}-secrets"
  description = "Segredos do Bot de Financas (DB e Telegram)"

  # Boas práticas: impede a exclusão acidental sem um período de recuperação
  recovery_window_in_days = 0

  tags = {
    Name = "finbot-${var.env}-secrets"
  }
}

# O CONTEÚDO
resource "aws_secretsmanager_secret_version" "app_secrets_val" {
  secret_id = aws_secretsmanager_secret.app_secrets.id

  # Valores iniciais
  secret_string = jsonencode({
    db_username    = "admin"
    db_password    = "mudar-no-console"
    db_host        = aws_db_instance.finbot_mysql.address
    telegram_token = "mudar-no-console" # Você mudará este manualmente
  })

  # A MÁGICA ESTÁ AQUI:
  lifecycle {
    ignore_changes = [
      secret_string, # O Terraform não vai mais tentar "corrigir" o valor se você mudar no console
    ]
  }
}

# 5. Permissão para a Lambda ler o Segredo
# Aqui dizemos explicitamente que a identidade da Lambda pode ler este cofre específico.
resource "aws_iam_policy" "lambda_secrets_policy" {
  name        = "finbot-${var.env}-policy-secrets"
  description = "Permite a Lambda ler segredos no Secrets Manager"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action   = ["secretsmanager:GetSecretValue"]
      Effect   = "Allow"
      Resource = [aws_secretsmanager_secret.app_secrets.arn]
    }]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_secrets_attach" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = aws_iam_policy.lambda_secrets_policy.arn
}

resource "aws_iam_role_policy" "lambda_s3_read" {
  name = "finbot-${var.env}-lambda-s3-policy"
  role = aws_iam_role.lambda_exec.id # Garanta que esse nome bate com a sua role

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "s3:GetObject",
          "s3:GetObjectVersion"
        ]
        Effect   = "Allow"
        Resource = "arn:aws:s3:::finbot-deploy-artifacts-satyans/app.jar"
      }
    ]
  })
}