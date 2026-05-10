# IAM Role para a EC2
resource "aws_iam_role" "ec2_role" {
  name = "finbot-${var.env}-iam-role-ec2"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })

  tags = { Name = "finbot-${var.env}-iam-role-ec2" }
}

# Instance Profile — vincula a role à EC2
resource "aws_iam_instance_profile" "ec2_profile" {
  name = "finbot-${var.env}-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# Secrets Manager — leitura do finbot-prod-secrets
data "aws_secretsmanager_secret" "app_secrets" {
  name = "finbot-${var.env}-secrets"
}

resource "aws_iam_policy" "ec2_secrets_policy" {
  name        = "finbot-${var.env}-policy-secrets"
  description = "Permite a EC2 ler segredos do Secrets Manager"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue"]
      Resource = [data.aws_secretsmanager_secret.app_secrets.arn]
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ec2_secrets_attach" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = aws_iam_policy.ec2_secrets_policy.arn
}

# S3 — upload e leitura de imagens do bucket de pagamentos
resource "aws_iam_policy" "ec2_s3_policy" {
  name        = "finbot-${var.env}-policy-s3"
  description = "Permite a EC2 fazer upload e leitura de imagens no S3"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ]
      Resource = "arn:aws:s3:::${var.s3_images_bucket}/*"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ec2_s3_attach" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = aws_iam_policy.ec2_s3_policy.arn
}

# CloudWatch Logs — para monitorar erros da aplicação
resource "aws_iam_role_policy_attachment" "ec2_cloudwatch" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}
