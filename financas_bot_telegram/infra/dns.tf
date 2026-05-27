# Zona já existe (criada pelo registro do domínio no Route 53). Referenciar, não criar.
data "aws_route53_zone" "principal" {
  name         = var.domain_name
  private_zone = false
}
