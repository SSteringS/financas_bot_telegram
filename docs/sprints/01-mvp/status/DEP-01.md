# Status — DEP-01: Hosted Zone (Route 53) + Certificado ACM

**Branch:** `feature/dep-01-route53-zone-acm`
**Data de execução:** 2026-05-26
**Executor:** Claude do back
**Estado:** concluído — aguardando revisão antes de mergear em develop

---

## Recursos criados

| Recurso | ID / ARN |
|---|---|
| `data.aws_route53_zone.principal` | `Z06950183U96CYLZM2MVU` (referência — não criado) |
| `aws_acm_certificate.principal` | `arn:aws:acm:us-east-1:776658251579:certificate/5481752d-d925-48ed-a7fd-47720a75a098` |
| `aws_route53_record.acm_validation["satyansaita.com"]` | CNAME de validação na zona |
| `aws_route53_record.acm_validation["*.satyansaita.com"]` | CNAME de validação na zona (mesmo registro — Route 53 deduplicou) |
| `aws_acm_certificate_validation.principal` | Validação concluída em ~34s |

## Outputs

```
acm_certificate_arn = "arn:aws:acm:us-east-1:776658251579:certificate/5481752d-d925-48ed-a7fd-47720a75a098"
route53_zone_id     = "Z06950183U96CYLZM2MVU"
```

## Checagens realizadas

- [x] `terraform init` — backend S3 confirmado (`finbot-tfstate-satyans`, `prod/terraform.tfstate`)
- [x] `terraform plan` antes do apply — apenas adições (4 to add, 0 to change, 0 to destroy)
- [x] `terraform apply` — concluído sem erro (4 added)
- [x] Certificado ACM com status **ISSUED** (validação DNS completou em ~1s após os registros serem criados)
- [x] `terraform output acm_certificate_arn` e `route53_zone_id` — valores válidos
- [x] Segundo `terraform plan` — **No changes** (idempotência confirmada)

## Desvios em relação ao plano

### Desvio 1 — Drift de AMI na EC2 (pré-existente, não do DEP-01)

O primeiro `terraform plan` mostrou `aws_instance.finbot_app` marcado para **destroy + replace** por drift de AMI (`ami-0e6d903c989c08b79` → `ami-00f40869bea0a58a3`). A causa é o `data.aws_ami.amazon_linux_2023` com `most_recent = true` em `ec2.tf` — o AWS publicou uma AMI nova depois que a EC2 foi criada.

**Resolução aprovada pelo humano:** adicionado `lifecycle { ignore_changes = [ami] }` em `aws_instance.finbot_app` no `ec2.tf`. A EC2 de prod não foi recriada. O fix é incluído nesta branch.

### Desvio 2 — Domínio não estava documentado

O domínio `satyansaita.com` não constava em nenhum plano ou doc (DEP-00 não tinha status report). Execução pausada e domínio confirmado pelo humano antes de prosseguir.

## Pendências geradas

- `docs/PENDENCIAS-TECNICAS.md` atualizado com item sobre `terraform.tfstate` commitado (ver seção correspondente).

## Próximo passo

DEP-02 (CloudFront + S3 para o frontend) consome:
- `acm_certificate_arn` = `arn:aws:acm:us-east-1:776658251579:certificate/5481752d-d925-48ed-a7fd-47720a75a098`
- `route53_zone_id` = `Z06950183U96CYLZM2MVU`
