---
name: Secrets de dev local não são commitados
description: Convenção do projeto: arquivos com credenciais de dev local (mesmo que "só local") ficam fora do git
type: project
---

Convenção do projeto sobre secrets em arquivos de dev local:

- `application-dev.properties` está no `.gitignore` (raiz e pasta do back). NÃO está versionado.
- Mesmo sendo credenciais "só de dev local" (ex: senha MySQL `Satyan123`), a política é não commitar.
- Isso significa que **não existe precedente de senha hardcoded no repo** que justifique reusar o padrão.

**Why:** humano explicitou em 2026-06-01 ao decidir Decisão 2 do desenho de testes E2E: "preferimos nao comitar mesmo que seja so local". Postura de segurança consistente — evita vazamento por descuido futuro (push pra repo público, screenshot do código, etc).

**How to apply:** ao propor onde guardar credenciais em qualquer arquivo (back ou front, dev ou prod), assumir que devem ficar fora do git. Opções aceitáveis: `.env.*` gitignored com `.env.*.example` versionado, variáveis de ambiente do sistema, Secrets Manager (prod). Opção rejeitada: hardcoded em arquivo versionado, mesmo com aviso.
