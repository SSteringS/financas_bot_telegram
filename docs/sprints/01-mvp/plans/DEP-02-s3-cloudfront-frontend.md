# DEP-02 — S3 (bucket privado) + CloudFront pro frontend

## Contexto

Hospedagem do front compilado (Vite → `dist/`). Bucket S3 **privado** servido via CloudFront com OAC (Origin Access Control), HTTPS pelo cert do DEP-01, e o apex `satyansaita.com` apontando pra distribuição via Route 53. SPA fallback (403/404 → `/index.html`) pra o React Router funcionar em deep links.

A API roda em hostname separado (`api.satyansaita.com`, DEP-03) — por isso o CloudFront pode cachear o front agressivamente sem afetar a API.

## Branch

`feature/dep-02-s3-cloudfront-frontend` — nova, a partir de `develop`. Território de **infra → Claude do back**.

## Definição de pronto

Seguir `docs/runbooks/PRE-MERGE-CHECKLIST.md`. Status report com frontmatter válido em `docs/sprints/01-mvp/status/DEP-02.md`. Como é infra (sem build/lint/testes de código), os gates `build`/`lint`/`testes` são `na`; os gates de `terraform` (plan limpo, apply ok, idempotência) estão nos critérios abaixo.

## Dependências

- **DEP-01** (mergeada em develop): fornece `aws_acm_certificate_validation.principal` (cert ISSUED) e `data.aws_route53_zone.principal` (zona). Como é o **mesmo módulo Terraform** (mesmo state), referenciar os recursos direto — **não** usar data source de state remoto.
- **Desbloqueia:** DEP-04 (pipeline precisa do nome do bucket + ID da distribuição) e DEP-05.

## Insumos concretos (do DEP-01)

- Domínio: `satyansaita.com` (variável `var.domain_name`, já existe).
- Cert ARN: via `aws_acm_certificate_validation.principal.certificate_arn`.
- Zone ID: via `data.aws_route53_zone.principal.zone_id`.

## Checagens pré-apply (obrigatórias)

- `terraform init` usando o backend S3 (`finbot-tfstate-satyans`), nunca o state local.
- `terraform plan -var-file=prod.tfvars` deve mostrar **só adições**. A EC2 (`aws_instance.finbot_app`) **não** pode aparecer como replace — o DEP-01 já tratou o drift de AMI com `ignore_changes = [ami]`. Se aparecer qualquer destroy/replace de recurso existente, **PARAR e mostrar o plan**.

## Variável nova

Em `variables.tf`:

```hcl
variable "frontend_bucket_name" {
  description = "Nome global do bucket S3 do front compilado (precisa ser único na AWS)"
  type        = string
  default     = "finbot-frontend-prod"
}
```

> Nome de bucket S3 é **global**. Se `finbot-frontend-prod` já estiver tomado, ajustar (ex: sufixo com account id `finbot-frontend-prod-776658251579`).

## Arquivo: `financas_bot_telegram/infra/frontend.tf` (criar)

```hcl
# ---- Bucket privado do front ----
resource "aws_s3_bucket" "frontend" {
  bucket = var.frontend_bucket_name
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket                  = aws_s3_bucket.frontend.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# ---- OAC: CloudFront acessa o bucket privado ----
resource "aws_cloudfront_origin_access_control" "frontend" {
  name                              = "finbot-frontend-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# Cache policy gerenciada da AWS (CachingOptimized)
data "aws_cloudfront_cache_policy" "optimized" {
  name = "Managed-CachingOptimized"
}

# ---- Distribuição ----
resource "aws_cloudfront_distribution" "frontend" {
  enabled             = true
  is_ipv6_enabled     = true
  default_root_object = "index.html"
  aliases             = [var.domain_name]
  comment             = "finbot frontend"
  price_class         = "PriceClass_All" # inclui edge locations da América do Sul (pai está no Brasil)

  origin {
    domain_name              = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_id                = "s3-frontend"
    origin_access_control_id = aws_cloudfront_origin_access_control.frontend.id
  }

  default_cache_behavior {
    target_origin_id       = "s3-frontend"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    cache_policy_id        = data.aws_cloudfront_cache_policy.optimized.id
    compress               = true
  }

  # SPA fallback: deep link inexistente no S3 (403/404) vira index.html 200
  custom_error_response {
    error_code         = 403
    response_code      = 200
    response_page_path = "/index.html"
  }
  custom_error_response {
    error_code         = 404
    response_code      = 200
    response_page_path = "/index.html"
  }

  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate_validation.principal.certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }
}

# ---- Bucket policy: só o CloudFront desta distribuição lê ----
data "aws_iam_policy_document" "frontend_bucket" {
  statement {
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.frontend.arn}/*"]
    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.frontend.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "frontend" {
  bucket = aws_s3_bucket.frontend.id
  policy = data.aws_iam_policy_document.frontend_bucket.json
}

# ---- DNS: apex aponta pra distribuição (alias A + AAAA) ----
resource "aws_route53_record" "frontend_a" {
  zone_id = data.aws_route53_zone.principal.zone_id
  name    = var.domain_name
  type    = "A"
  alias {
    name                   = aws_cloudfront_distribution.frontend.domain_name
    zone_id                = aws_cloudfront_distribution.frontend.hosted_zone_id
    evaluate_target_health = false
  }
}

resource "aws_route53_record" "frontend_aaaa" {
  zone_id = data.aws_route53_zone.principal.zone_id
  name    = var.domain_name
  type    = "AAAA"
  alias {
    name                   = aws_cloudfront_distribution.frontend.domain_name
    zone_id                = aws_cloudfront_distribution.frontend.hosted_zone_id
    evaluate_target_health = false
  }
}
```

## Outputs (adicionar em `outputs.tf`)

```hcl
output "frontend_bucket_name" {
  description = "Bucket do front — usado pelo deploy (aws s3 sync) no DEP-04"
  value       = aws_s3_bucket.frontend.id
}

output "cloudfront_distribution_id" {
  description = "ID da distribuição — usado pra invalidação no DEP-04"
  value       = aws_cloudfront_distribution.frontend.id
}

output "cloudfront_domain_name" {
  description = "Domínio nativo da distribuição (debug/fallback)"
  value       = aws_cloudfront_distribution.frontend.domain_name
}
```

## Critérios de aceitação

- `terraform plan -var-file=prod.tfvars` limpo, **só adições**, sem replace da EC2 nem de qualquer recurso existente.
- `terraform apply` cria bucket, OAC, distribuição, bucket policy e os registros A/AAAA sem erro.
- A distribuição chega a status **Deployed** no console (leva ~5-15 min; o apply retorna antes disso — normal).
- **Teste manual de fumaça:** subir um `index.html` de teste no bucket (`aws s3 cp index.html s3://<bucket>/`) e abrir `https://satyansaita.com` → retorna 200 sobre HTTPS, cert válido (sem aviso de segurança).
- **SPA fallback:** abrir `https://satyansaita.com/qualquer/rota/inexistente` → retorna o `index.html` (200), não erro do S3.
- `terraform output frontend_bucket_name` e `cloudfront_distribution_id` retornam valores válidos.
- Segundo `terraform plan` → **No changes** (idempotência).

## Gotchas

- **Propagação do CloudFront:** depois do apply, a distribuição leva minutos pra ficar `Deployed`. Se o `https://` não responder de cara, esperar e tentar de novo antes de suspeitar de erro.
- **Cache vs. deploy:** a invalidação `/*` no deploy (DEP-04) é o que faz updates aparecerem. Pra este teste manual, se trocar o `index.html`, invalidar via `aws cloudfront create-invalidation` ou usar query string pra furar cache.
- **Apex via alias:** registro A/AAAA do tipo `alias` funciona no apex (CNAME não funcionaria) — por isso A/AAAA e não CNAME.
- **www (fora de escopo):** o cert cobre `*.satyansaita.com`, mas servir/redirecionar `www` não está neste DEP. Se quiser depois, é um registro + behavior adicional.

## Coordenação

- Executado pelo Claude do **back** (infra).
- Outputs `frontend_bucket_name`, `cloudfront_distribution_id` são o contrato pro DEP-04 — não renomear sem alinhar.
- Ao terminar, status report em `docs/sprints/01-mvp/status/DEP-02.md` e **parar pra revisão** antes de seguir pro DEP-03/04.
