# ---- OIDC provider: GitHub Actions → AWS (singleton da conta) ----
# Se já existir um provider com esta URL, importar em vez de duplicar:
#   terraform import aws_iam_openid_connect_provider.github \
#     arn:aws:iam::<account_id>:oidc-provider/token.actions.githubusercontent.com
resource "aws_iam_openid_connect_provider" "github" {
  url = "https://token.actions.githubusercontent.com"

  client_id_list = ["sts.amazonaws.com"]

  # Thumbprint da CA raiz do GitHub OIDC (estável; atualizar se a CA rodar)
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

# ---- Role assumível pelo workflow de deploy do front ----
data "aws_iam_policy_document" "gha_frontend_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    # Escopo: só o branch main do repo — evita que qualquer fork/branch assuma a role
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:SSteringS/financas_bot_telegram:ref:refs/heads/main"]
    }
  }
}

resource "aws_iam_role" "gha_frontend_deploy" {
  name               = "finbot-prod-gha-frontend-deploy"
  assume_role_policy = data.aws_iam_policy_document.gha_frontend_assume.json
}

# ---- Policy mínima: S3 (bucket do front) + CloudFront (invalidação) ----
data "aws_iam_policy_document" "gha_frontend_policy" {
  statement {
    sid       = "S3ListBucket"
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.frontend.arn]
  }

  statement {
    sid    = "S3ReadWrite"
    effect = "Allow"
    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject",
    ]
    resources = ["${aws_s3_bucket.frontend.arn}/*"]
  }

  statement {
    sid       = "CloudFrontInvalidate"
    effect    = "Allow"
    actions   = ["cloudfront:CreateInvalidation"]
    resources = [aws_cloudfront_distribution.frontend.arn]
  }
}

resource "aws_iam_policy" "gha_frontend_deploy" {
  name   = "finbot-prod-gha-frontend-deploy-policy"
  policy = data.aws_iam_policy_document.gha_frontend_policy.json
}

resource "aws_iam_role_policy_attachment" "gha_frontend_deploy" {
  role       = aws_iam_role.gha_frontend_deploy.name
  policy_arn = aws_iam_policy.gha_frontend_deploy.arn
}

# ---- Output: ARN da role (usado no workflow) ----
output "gha_frontend_deploy_role_arn" {
  description = "ARN da role IAM para o workflow deploy-frontend.yml"
  value       = aws_iam_role.gha_frontend_deploy.arn
}
