output "route53_zone_id" {
  description = "Zone ID da hosted zone — usado por DEP-02/03 nos registros alias"
  value       = data.aws_route53_zone.principal.zone_id
}

output "acm_certificate_arn" {
  description = "ARN do cert validado — usado pelo CloudFront (DEP-02)"
  value       = aws_acm_certificate_validation.principal.certificate_arn
}
