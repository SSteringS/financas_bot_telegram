# Memory Index

- [Desenho E2E — decisões consolidadas](project_e2e_design.md) — stack local, requisitante id=99, Playwright, 3 specs MVP
- [Estado de testes em 2026-06-01](project_estado_testes_2026-06-01.md) — baseline back (~52 unit + 7 integração) e front (12 Vitest+MSW)
- [Secrets dev não são commitados](project_convencao_secrets_dev.md) — application-dev.properties no .gitignore; sem hardcoded mesmo local
- [A11y automatizada fora de escopo](project_a11y_fora_de_escopo.md) — decisão 2026-06-03: descopar axe enquanto audience for fechada (humano + Pedro)
- [Sprint só fecha quando todas as atividades mergearem](feedback_estado_sprint.md) — não inferir fechamento de sprint do merge integration→develop
- [Autorizado a executar testes no worktree do implementador](feedback_autorizacao_worktree_implementador.md) — Read/Bash sem pedir permissão a cada vez, quando o objetivo é teste automatizado
- [Gaps recorrentes no domínio de folha de pagamento](project_gaps_folha_pagamento.md) — checklist para análise de FecharMes/Adiantamento (filtros temporais, @Transactional, race conditions)
- [Helpers de teste com valores hardcoded são red flag](feedback_helpers_teste_hardcoded.md) — método: cruzar campos fixos do helper com condicionais do production code
