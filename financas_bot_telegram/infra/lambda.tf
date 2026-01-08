# 1. Configuração da Função Lambda
resource "aws_lambda_function" "finbot_app" {
  function_name = "finbot-${var.env}-lambda-app"
  description   = "Cerebro do Bot de Financas - Java 21 / Spring Boot 3"

  handler = "org.springframework.cloud.function.adapter.aws.FunctionInvoker"
  runtime = "java21"
  role    = aws_iam_role.lambda_exec.arn

  s3_bucket = "finbot-deploy-artifacts-satyans"
  s3_key    = "app.jar"

  memory_size = 1024
  timeout     = 30

  snap_start {
    apply_on = "PublishedVersions"
  }

  vpc_config {
    subnet_ids         = [aws_subnet.private.id]
    security_group_ids = [aws_security_group.lambda_sg.id]
  }

  environment {
    variables = {
      SPRING_PROFILES_ACTIVE = var.env
      APP_SECRETS_NAME       = aws_secretsmanager_secret.app_secrets.name
      AWS_REGION_NAME        = "us-east-1"
    }
  }

  tags = { Name = "finbot-${var.env}-lambda-app" }
}

# 2. Publicação da Versão
resource "aws_lambda_alias" "env_alias" {
  name             = var.env
  function_name    = aws_lambda_function.finbot_app.function_name
  function_version = aws_lambda_function.finbot_app.version
}

# 3. Lambda Function URL
resource "aws_lambda_function_url" "finbot_url" {
  function_name      = aws_lambda_function.finbot_app.function_name
  qualifier          = aws_lambda_alias.env_alias.name
  authorization_type = "NONE"

  cors {
    allow_origins = ["*"]
    allow_methods = ["POST"]
  }
}

output "webhook_url" {
  description = "URL para configurar o Webhook no Telegram"
  value       = aws_lambda_function_url.finbot_url.function_url
}