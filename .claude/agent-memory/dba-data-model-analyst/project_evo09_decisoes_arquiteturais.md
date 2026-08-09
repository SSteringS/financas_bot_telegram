---
name: EVO-09 Folha de Pagamento — decisões arquiteturais aprovadas (ADR 0016)
description: Decisões de design aprovadas para o domínio de folha; usar como baseline em análises futuras
type: project
---

ADR 0016 (status Proposed em 2026-05-31, aguardando homologação humano) define o design da folha de pagamento:

1. **Vale e Folha são `Pedido`** (reuso de `pedidos_pagamento`) com coluna `categoria ENUM('VALE','FOLHA') NULL`. NULL = pedido legado/normal sem categoria.
2. **`funcionario_id` e `fechado` adicionados em `pedidos_pagamento`** — colunas planas, sem tabela de extensão. Acoplamento leve aceito explicitamente em troca de simplicidade.
3. **`adiantamento` é plano de desconto parcelado** — não gera pedidos antecipados; rastreia `parcelas_pagas` que é incrementado a cada `FecharMes`.
4. **NÃO existe entidade `fechamento_mes`** — snapshot do cálculo vive em `pedidos_pagamento.observacao` (VARCHAR 1000). Foi decisão consciente para eliminar ~5 classes hexagonais.
5. **Idempotência via query** (não UNIQUE KEY) — aceitável para o volume; advisory lock como porta aberta se aparecer problema.
6. **Cadastro só pelo front-end** — nenhum comando bot novo na EVO-09.

**Why:** Decisões já passaram por refinamento com arquiteto e estão documentadas em `docs/decisions/0016-evo09-folha-pagamento.md` e `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md`.

**How to apply:** Em qualquer análise futura do domínio de folha, validar se a proposta está alinhada com essas decisões. Se diverge (ex: alguém propõe tabela `fechamento_mes` separada, ou `pedido_folha_meta` como extensão 1:0..1), apontar a divergência explicitamente — pode ser revisão consciente, mas o humano precisa saber que está revertendo uma decisão registrada.
