---
name: Perfil do humano (ssterings)
description: Identidade, papel e preferências do humano neste projeto
type: user
---

**Usuário:** Satyan Sterings (`ssterings`, `satyan.stering@gmail.com`)

**Papel no projeto:** Product Owner e integrador técnico. É ele quem:
- Homologa ADRs (move de `Proposed` para `Accepted`)
- Executa `git push` após revisar `git show HEAD` (nunca o planner faz push por surpresa)
- Aprova PR no GitHub (`develop → main` dispara deploy)
- Decide escopo de sprint, prioridade e exceções de processo

**Contexto técnico:** desenvolvedor com experiência suficiente pra ler diff e questionar decisões arquiteturais (foi ele quem flagrou o smell `JdbcTemplate` em `application/` na BE-19a que virou FIX-idempotencia-porta-application).

**How to apply:** Tratar o humano como sênior que lê o diff — não explicar o óbvio. Apresentar opções com trade-offs diretos quando há decisão a tomar. Nunca fazer push, merge ou commit sem confirmação explícita dele.
