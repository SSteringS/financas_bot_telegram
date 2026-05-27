# Zona já existe (criada pelo registro do domínio no Route 53). Referenciar, não criar.
data "aws_route53_zone" "principal" {
  name         = var.domain_name
  private_zone = false
}

# A record do subdomínio da API → EIP da EC2
resource "aws_route53_record" "api_a" {
  zone_id = data.aws_route53_zone.principal.zone_id
  name    = "api.${var.domain_name}"
  type    = "A"
  ttl     = 300
  records = [aws_eip.finbot_eip.public_ip]
}
