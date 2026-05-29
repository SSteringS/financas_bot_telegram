---
task: DEP-02
titulo: "S3 privado + CloudFront frontend (OAC, HTTPS, SPA fallback)"
data: 2026-05-26
branch: feature/dep-02-s3-cloudfront-frontend
responsavel: claude-back
estado: concluido
gates:
  build: na
  lint: na
  testes: na
  testes_total: na
  testes_novos: na
  branch_convencao: ok
  territorio: ok
commits:
  - 1a895ca
pr: null
desvios: 2
pendencias_humano: 0
---

# DEP-02 — S3 privado + CloudFront frontend (OAC, HTTPS, SPA fallback)

## O que foi feito

- `frontend.tf` criado com: bucket S3 privado (`finbot-frontend-prod-776658251579`), public access block, OAC (`finbot-frontend-oac`), distribuição CloudFront com cert do DEP-01, SPA fallback 403/404→`/index.html`, bucket policy restrita ao CloudFront via `AWS:SourceArn`, registros alias A+AAAA no Route 53 apontando o apex `satyansaita.com` para a distribuição.
- `outputs.tf` atualizado com `frontend_bucket_name`, `cloudfront_distribution_id`, `cloudfront_domain_name`.
- `variables.tf` e `prod.tfvars` com nova variável `frontend_bucket_name`.
- Apply: 6 added, 0 changed, 0 destroyed. Segundo plan: No changes.
- Smoke test: `https://satyansaita.com` → 200 OK, cert válido (TLS 1.2+), edge GRU3-P9 (São Paulo).
- SPA fallback: `https://satyansaita.com/rota/inexistente` → 200 OK (servindo `index.html`).

---

## Desvios do plano

**Desvio 1 — Nome de bucket `finbot-frontend-prod` já tomado globalmente (previsto no plano como gotcha)**

O primeiro apply falhou com `BucketAlreadyExists`. Conforme orientação explícita do plano ("ex: sufixo com account id"), o nome foi ajustado para `finbot-frontend-prod-776658251579`. O OAC e o update da EC2 já tinham sido aplicados nesse apply parcial; o segundo apply criou o bucket com o nome correto e o restante dos recursos sem erro.

**Desvio 2 — EC2 apareceu como update in-place no plan (user_data drift, pré-existente)**

`aws_instance.finbot_app` apareceu como `~` (update in-place) com mudança de hash de `user_data`. Causa: CRLF/LF introduzido pelo git no Windows ao commitar o `ec2.tf` no DEP-01. Update in-place de `user_data` em instância rodando é metadata-only — não reinicia a instância nem afeta o serviço. A instância permaneceu running durante o apply. Condição de parada (replace/destroy) não foi atingida.

---

## Decisões tomadas durante a execução

- Bucket name com sufixo de account id (`776658251579`) foi a escolha de menor surpresa, conforme sugerido no próprio plano. Nome está no `default` da variável e no `prod.tfvars`.
- `price_class = "PriceClass_All"` mantido conforme o plano (inclui edge GRU3 em São Paulo, validado no smoke test).

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- **DEP-04 (pipeline deploy)** consome:
  - `frontend_bucket_name` = `finbot-frontend-prod-776658251579`
  - `cloudfront_distribution_id` = `E1WG4Q8MG3V9HY`
- O `index.html` de smoke test está no bucket — o deploy real do front vai sobrescrever com `aws s3 sync dist/ s3://... --delete`.
- A distribuição CloudFront tem `wait_for_deployment = true` por default no provider AWS — o apply já aguardou o status `Deployed`.
- O drift de `user_data` da EC2 vai reaparecer em cada plan até que o `ec2.tf` seja normalizado para LF. Considerar adicionar `.gitattributes` com `*.tf text eol=lf` para evitar reintrodução.

---

## Arquivos criados/modificados

- `financas_bot_telegram/infra/frontend.tf` (novo: bucket S3, OAC, distribuição CloudFront, bucket policy, registros DNS A+AAAA)
- `financas_bot_telegram/infra/outputs.tf` (modificado: +3 outputs DEP-02)
- `financas_bot_telegram/infra/variables.tf` (modificado: +`frontend_bucket_name`)
- `financas_bot_telegram/infra/prod.tfvars` (modificado: +`frontend_bucket_name`)
