# Certificado para o apex + wildcard (cobre api.<domínio>, www, etc).
# Provider default já é us-east-1 — requisito do CloudFront.
resource "aws_acm_certificate" "principal" {
  domain_name               = var.domain_name
  subject_alternative_names = ["*.${var.domain_name}"]
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

# Registros de validação DNS na hosted zone (um por domínio/SAN; o Route 53 dedupe iguais).
resource "aws_route53_record" "acm_validation" {
  for_each = {
    for dvo in aws_acm_certificate.principal.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      type   = dvo.resource_record_type
      record = dvo.resource_record_value
    }
  }

  zone_id         = data.aws_route53_zone.principal.zone_id
  name            = each.value.name
  type            = each.value.type
  records         = [each.value.record]
  ttl             = 60
  allow_overwrite = true
}

# Espera a validação concluir antes de marcar o cert como pronto.
resource "aws_acm_certificate_validation" "principal" {
  certificate_arn         = aws_acm_certificate.principal.arn
  validation_record_fqdns = [for r in aws_route53_record.acm_validation : r.fqdn]
}
