# DEP-01 — Hosted zone (Route 53) + certificado ACM

## Contexto

Primeira tarefa de infra da Fase 3c (deploy). Cria a base de DNS e HTTPS pro front (CloudFront, DEP-02) e pra API (`api.<domínio>`, DEP-03).

**Pré-requisito já resolvido (DEP-00):** o domínio será registrado **direto no Amazon Route 53**. O registro no Route 53 **cria automaticamente uma hosted zone** pro domínio (com registros NS + SOA). Isso muda o desenho original do FASE-3: **não vamos criar uma `aws_route53_zone` nova** — vamos **referenciar a zona existente via data source**, pra não duplicar zona nem arriscar destruí-la num `terraform destroy`.

## Branch

`feature/dep-01-route53-zone-acm` — nova, a partir de `develop` (convenção do CLAUDE.md: uma tarefa = uma branch). Território de **infra → Claude do back**.

## Correções de premissa em relação ao FASE-3-VISUALIZACAO.md

O plano-mãe (seção Fase 3c) precisa destes ajustes, já incorporados aqui:

1. **Caminho da infra:** os arquivos ficam em `financas_bot_telegram/infra/`, **não** na raiz `infra/`. O FASE-3 referencia `infra/dns.tf` / `infra/acm.tf` — o correto é `financas_bot_telegram/infra/dns.tf` e `.../acm.tf`.
2. **Sem provider aliased:** o `provider "aws"` em `provider.tf` **já é `region = "us-east-1"`**. Como o ACM pro CloudFront exige us-east-1 e já estamos nessa região, **não criar** provider com alias. Usar o provider default.
3. **Zona não é criada, é referenciada:** `data "aws_route53_zone"` em vez de `resource "aws_route53_zone"` (porque o registro do domínio já criou a zona).

## Checagens pré-apply (obrigatórias antes de qualquer `terraform apply`)

- **State remoto é a fonte da verdade.** O backend é S3 (`finbot-tfstate-satyans`, key `prod/terraform.tfstate`). Rodar `terraform init` e confirmar que está usando o backend S3 — **não** o `terraform.tfstate` local commitado na pasta (que parece resíduo). Se o init pedir migração de state, **parar e conferir** antes de aceitar.
- **Higiene de repo (anotar, não bloquear):** há `terraform.tfstate`, `.tfstate.backup` e `.terraform/` versionados em `financas_bot_telegram/infra/`. Idealmente entram no `.gitignore` (state remoto não deve ter cópia local versionada). Pode virar um `fix/` à parte — não é escopo do DEP-01, mas registrar em `docs/PENDENCIAS-TECNICAS.md`.
- O domínio precisa estar **registrado e com a hosted zone ativa** no Route 53 (DEP-00 concluído) — senão o data source falha.

## Variáveis novas

Em `financas_bot_telegram/infra/variables.tf`:

```hcl
variable "domain_name" {
  description = "Domínio raiz registrado no Route 53 (ex: finbot.com.br)"
  type        = string
}
```

Em `prod.tfvars` (e `dev.tfvars`, mesmo valor, pra não quebrar apply com profile dev):

```hcl
domain_name = "SEU_DOMINIO_AQUI"   # ex: finbot.com.br
```

## Arquivos

### `financas_bot_telegram/infra/dns.tf` (criar)

```hcl
# Zona já existe (criada pelo registro do domínio no Route 53). Referenciar, não criar.
data "aws_route53_zone" "principal" {
  name         = var.domain_name
  private_zone = false
}
```

### `financas_bot_telegram/infra/acm.tf` (criar)

```hcl
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
```

### Outputs (adicionar em `outputs.tf`, criar o arquivo se não existir)

```hcl
output "route53_zone_id" {
  description = "Zone ID da hosted zone — usado por DEP-02/03 nos registros alias"
  value       = data.aws_route53_zone.principal.zone_id
}

output "acm_certificate_arn" {
  description = "ARN do cert validado — usado pelo CloudFront (DEP-02)"
  value       = aws_acm_certificate_validation.principal.certificate_arn
}
```

## Critérios de aceitação

- `terraform init` conecta no backend S3 (não no state local).
- `terraform plan -var-file=prod.tfvars` limpo, mostrando **apenas adições** (data source + cert + records de validação + validation). Nenhum recurso de prod existente é modificado/destruído.
- `terraform apply -var-file=prod.tfvars` cria os recursos sem erro.
- No console ACM (us-east-1), o certificado fica com status **ISSUED** (validação DNS completa). Pode levar alguns minutos.
- `terraform output acm_certificate_arn` e `terraform output route53_zone_id` retornam valores válidos.
- Idempotência: um segundo `terraform plan` logo após o apply mostra **"No changes"**.

## Dependências

- **DEP-00** (domínio registrado no Route 53 + hosted zone ativa). Bloqueante.
- **Desbloqueia:** DEP-02 (CloudFront usa `acm_certificate_arn` + `route53_zone_id`) e DEP-03 (registro `api.<domínio>` usa `route53_zone_id`).

## Coordenação

- Executado pelo Claude do **back** (território de infra).
- Outputs (`acm_certificate_arn`, `route53_zone_id`) são o contrato pro DEP-02/03 — não renomear sem alinhar.
- Ao terminar, status report em `docs/status/DEP-01.md` (o que foi criado, ARN do cert, zone_id, qualquer desvio). Parar pra revisão antes de seguir pro DEP-02.

## Notas

- O cert wildcard `*.<domínio>` **não** cobre o apex sozinho — por isso o apex entra como `domain_name` e o wildcard como SAN. Os dois juntos cobrem `<domínio>`, `api.<domínio>`, `www.<domínio>`, etc.
- DNS de validação com TTL baixo (60s) acelera a primeira validação. `allow_overwrite = true` evita erro se um registro de validação igual já existir.
