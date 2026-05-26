---
feature: FIX-gitattributes-eol
branch: fix/gitattributes-eol
status: done
gates:
  build: na
  lint: na
  testes: na
date: 2026-05-26
---

# Status — FIX-gitattributes-eol

## O que foi feito

- Criado `.gitattributes` na raiz com `* text=auto eol=lf` (LF no working tree pra todos os arquivos de texto) e marcações binárias para imagens/JARs/keystores.
- `git add --renormalize .` executado: o índice já estava em LF — nenhum arquivo de texto adicional precisou de renormalização.
- Working copy do `ec2.tf` atualizada via `git checkout -- financas_bot_telegram/infra/ec2.tf` para forçar LF no disco antes do `terraform plan`.

## Evidência — terraform plan limpo

```
No changes. Your infrastructure matches the configuration.
```

O plan foi executado em `financas_bot_telegram/infra/` com `terraform plan -var-file=prod.tfvars`. O `aws_instance.finbot_app` **não aparece mais** como `update in-place` por mudança de hash do `user_data`. Drift CRLF/LF eliminado.

## Observação

`docs/aprendizado/README.md` tinha uma mudança de conteúdo pré-existente (link novo adicionado pelo Claude de planejamento) e foi mantido fora do commit de normalização, conforme instrução do plano ("se houver mudança de conteúdo, PARAR").

## Próximo passo

PR para develop para revisão. Nenhum `terraform apply` necessário — este fix é só de repositório.
