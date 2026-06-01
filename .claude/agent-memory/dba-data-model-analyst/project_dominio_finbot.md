---
name: Domínio e escala do bot de finanças
description: Contexto de negócio e volumetria que governam decisões de modelagem do projeto
type: project
---

Sistema é bot de finanças pessoais domésticas (Telegram + WhatsApp + front web), Spring Boot 3.4.5, MySQL 8.0 em RDS, Flyway, arquitetura hexagonal.

**Volumetria real:** dezenas de pedidos/mês, 1-2 funcionários domésticos cadastrados, 1 usuário operador ("Pedro").

**Why:** Escala é doméstica, não enterprise. Otimizações de performance (particionamento, sharding, cache agressivo) são desproporcionais. Foco deve ser em consistência, clareza do modelo e idempotência — não em throughput.

**How to apply:** Antes de recomendar índices secundários, particionamento, materialized views ou desnormalização por performance, lembrar que o sistema processa dezenas de linhas/mês. Recomendar somente otimizações que pagam por si mesmas em clareza/segurança, não por ganho de latência.

**Entidades centrais (estado em 2026-05-31):**
- `pedidos_pagamento` — tabela operacional central; existente desde V1.
- `comprovantes` — 1:N pedido; armazena arquivos via S3 (imagem_url).
- `funcionario`, `adiantamento` — entrando em V6 (EVO-09 / sprint 03).

**Tipos monetários:** todo o modelo usa DECIMAL(10,2). Aceitável dado o contexto doméstico (limite ~R$ 99.999.999,99). Em refatoração futura, considerar padronizar para DECIMAL(15,2).
