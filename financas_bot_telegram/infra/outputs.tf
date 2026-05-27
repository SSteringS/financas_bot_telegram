output "route53_zone_id" {
  description = "Zone ID da hosted zone — usado por DEP-02/03 nos registros alias"
  value       = data.aws_route53_zone.principal.zone_id
}

output "acm_certificate_arn" {
  description = "ARN do cert validado — usado pelo CloudFront (DEP-02)"
  value       = aws_acm_certificate_validation.principal.certificate_arn
}

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
