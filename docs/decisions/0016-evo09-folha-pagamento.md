---
adr: "0016"
titulo: "EVO-09: Folha de Pagamento — decisoes arquiteturais"
data: 2026-05-31
status: Proposed
decisores: humano-com-arquiteto
relacionado: ["0004", "0005", "0007", "0011", "0015"]
supersedes: null
superseded_by: null
---

# ADR 0016 — EVO-09: Folha de Pagamento — decisoes arquiteturais

> **Sprint 03.** Refinamento com o arquiteto em 2026-05-31.
> Revisado pos-analise DBA: CHECK constraints, mes_referencia, atualizado_em.
> Aguarda homologacao do humano para mudar status para `Accepted`.

---

## Contexto

A EVO-09 adiciona gestao de folha de pagamento de funcionarios domesticos.
O backlog identificou tres decisoes abertas com impacto arquitetural:

1. **Onde mora o registro de vale/folha?** — entidade nova separada, ou reuso do Pedido existente?
2. **Como modelar adiantamentos parcelados?** — campo simples, ou entidade Adiantamento separada?
3. **Como o bot recebe o cadastro?** — conversacao multi-turno, ou front-end?

Alem das tres perguntas originais, a analise do DBA (2026-05-31) gerou decisoes adicionais
de integridade que foram incorporadas ao modelo final.

---

## Decisao

### 1. Vale e Folha como Pedido com coluna `categoria`

Reaproveitamos a entidade Pedido (`pedidos_pagamento`) adicionando a coluna
`categoria ENUM('VALE','FOLHA') NULL`.

- `NULL` = pedido existente sem categoria (zero backfill — historico intacto).
- `VALE` = adiantamento pontual registrado no mes.
- `FOLHA` = pagamento mensal gerado pelo fechamento.

**Rejeitado:** entidade `Holerite` separada — duplicaria campos de valor, status,
beneficiario; aumentaria surface de APIs sem ganho de modelo.

**Rejeitado:** tabela `pedido_folha_meta` como extensao 1:0..1 — a invariante
"pedido VALE/FOLHA tem linha em meta" nao pode ser enforçada no banco sem triggers;
a abordagem STI com CHECK constraint resolve a mesma preocupacao de isolamento
com menor complexidade. Referencia: Fowler, PEAA, "Single Table Inheritance".

### 2. STI com CHECK constraints — colunas em `pedidos_pagamento`

Alem de `categoria`, quatro colunas novas entram em `pedidos_pagamento`:

- `funcionario_id BIGINT NULL FK` — vinculo ao funcionario (so para VALE e FOLHA).
- `fechado BOOLEAN NOT NULL DEFAULT FALSE` — vale consumido num fechamento.
- `observacao VARCHAR(1000) NULL` — snapshot legivel do calculo (so para FOLHA).
- `mes_referencia DATE NULL` — primeiro dia do mes de referencia; so para FOLHA.

CHECK constraints garantem integridade STI no banco:

```sql
CONSTRAINT chk_pedido_folha CHECK (
    categoria IS NULL
    OR (categoria IN ('VALE','FOLHA') AND funcionario_id IS NOT NULL)
)
CONSTRAINT chk_pedido_folha_mes CHECK (
    categoria <> 'FOLHA' OR mes_referencia IS NOT NULL
)
```

### 3. Idempotencia de fechamento via UNIQUE INDEX no banco

O campo `mes_referencia DATE NULL` combinado com `UNIQUE INDEX uq_folha_por_mes
(funcionario_id, mes_referencia)` garante idempotencia de fechamento no banco:

- NULLs em UNIQUE MySQL sao distintos: VALE (mes_referencia=NULL) coexistem.
- FOLHA (mes_referencia=YYYY-MM-01) sao unicos por funcionario/mes.

**Preferido sobre:** verificacao via query apenas (sem UNIQUE KEY) — sujeita a
race condition em duplo-clique. A DBA recomendou UNIQUE KEY como "estritamente melhor".

**Preferido sobre:** entidade `fechamento_mes` separada — a pergunta-chave era
"precisamos de queries analiticas sobre os campos agregados (total_vales, valor_final)?".
Resposta do PO: nao. Logo, o snapshot textual em `observacao` e suficiente,
e elimina ~5 classes hexagonais sem perda de auditabilidade.

### 4. Adiantamento como plano de desconto parcelado

Adiantamentos parcelados sao representados pela entidade `adiantamento`:

- Registra o contrato: `valor_total`, `valor_parcela`, `num_parcelas`, `data_inicio`.
- Rastreia execucao: `parcelas_pagas` (incrementado a cada FecharMes).
- `ativo=false` quando quitado ou cancelado manualmente.

CHECK constraints evitam inconsistencia matematica entre os campos derivados:

```sql
CONSTRAINT chk_adiant_consistencia CHECK (
    ABS(valor_total - (valor_parcela * num_parcelas)) <= 0.01
)
CONSTRAINT chk_adiant_parcelas CHECK (parcelas_pagas BETWEEN 0 AND num_parcelas)
```

**Rejeitado:** gerar um Pedido VALE por parcela antecipadamente — viola o principio de
nao criar registros "futuros"; dificulta cancelamento e auditoria.

### 5. Cadastro de vale via front-end apenas

O cadastro de vales e o fechamento mensal ficam exclusivamente no front-end.
Nenhum comando Telegram novo para EVO-09.

**Razao:** conversacao multi-turno no Telegram requer gerenciamento de estado,
tratamento de timeout, e CallbackQuery — complexidade desproporcional para o caso de uso.
Documentado em `docs/aprendizado/telegram-conversas-multi-turno.md`.

### 6. Entidade `funcionario` como dominio de primeiro nivel

`funcionario` e uma entidade raiz independente. Campos obrigatorios: `nome`,
`salario_base`, `forma_pagamento`, dados bancarios conforme forma.

Campo `atualizado_em DATETIME ON UPDATE CURRENT_TIMESTAMP` incluido para
rastreabilidade de mudancas de salario e dados bancarios (recomendacao DBA).

CHECK constraints garantem consistencia de dados bancarios por forma de pagamento:

```sql
CONSTRAINT chk_func_pix CHECK (forma_pagamento <> 'PIX' OR chave_pix IS NOT NULL)
CONSTRAINT chk_func_ted CHECK (
    forma_pagamento <> 'TED'
    OR (banco IS NOT NULL AND agencia IS NOT NULL AND conta IS NOT NULL AND tipo_conta IS NOT NULL)
)
```

---

## Razoes

- **NULL semantico** e mais limpo que valor sentinel SEM_CATEGORIA.
- **Reuso de Pedido** preserva consistencia: vales e folha entram no mesmo fluxo
  de aprovacao/comprovante. Historico financeiro em uma so tabela.
- **STI com CHECK** resolve o isolamento de dominio sem a complexidade de tabela
  de extensao separada (pedido_folha_meta rejeitado).
- **UNIQUE INDEX em mes_referencia** garante idempotencia no banco sem entidade
  fechamento_mes (rejeitada por ausencia de necessidade analitica confirmada pelo PO).
- **Adiantamento como plano** mantem o principio de que o sistema registra fatos
  ocorridos, nao antecipa eventos.
- **Front-end para cadastro** segue o padrao da sprint 02: front-end e o hub de
  operacoes; bot e canal de notificacao e consulta rapida.

---

## Consequencias

**Positivas:**
- Migracao V6 e aditiva e nao-destrutiva — nenhum dado existente e alterado.
- pedidos_pagamento continua sendo a tabela central; APIs existentes nao quebram.
- Snapshot em observacao elimina entidade FechamentoMes e ~5 classes hexagonais.
- CHECK constraints garantem integridade no banco, nao apenas na aplicacao.
- atualizado_em em funcionario rastreia mudancas de salario e dados bancarios.
- UNIQUE INDEX garante idempotencia de fechamento mesmo em race condition.

**Negativas / custos:**
- pedidos_pagamento cresce em largura — 5 novas colunas. Queries sem
  WHERE categoria IS NULL retornam pedidos mistos.
- fechado e mes_referencia em pedidos_pagamento sao flags de lifecycle
  do dominio de folha vivendo na tabela de pedidos gerais — acoplamento leve, aceitavel.
- Cancelamento de adiantamento parcelado e manual (ativo=false); sem automacao por
  saida de funcionario — risco operacional baixo no contexto domestico.
- DataIntegrityViolationException do UNIQUE INDEX precisa ser traduzida para
  FechamentoDuplicadoException no service.

---

## Alternativas consideradas

| Alternativa | Por que descartada |
|---|---|
| Entidade Holerite separada | Duplica campos; dois fluxos de pagamento sem ganho de modelo |
| SEM_CATEGORIA como valor enum | Anti-pattern: NULL ja representa ausencia |
| pedido_folha_meta (extensao 1:0..1) | Invariante nao enforçavel no banco sem triggers; STI com CHECK resolve com menor complexidade |
| fechamento_mes como entidade separada | ~5 classes extras; necessidade analitica nao confirmada pelo PO; snapshot em observacao e suficiente |
| Idempotencia so por query | Race condition em duplo-clique; UNIQUE INDEX e estritamente melhor |
| Pedido por parcela antecipado | Registra eventos futuros; dificulta cancelamento e auditoria |
| Cadastro de vale via bot Telegram | Complexidade de estado multi-turno desproporcional ao ganho |

---

## Riscos

| Risco | Probabilidade | Impacto | Mitigacao |
|---|---|---|---|
| Pedidos VALE aparecem na lista do Pedro sem filtro | Medio | Medio | Decidir antes de BE-folha-4: filtrar por categoria IS NULL ou exibir com tag |
| requisitante_id nao aceitar NULL em Pedidos FOLHA | Medio | Medio | Verificar schema antes de BE-folha-6; ALTER se necessario |
| Adiantamento nao cancelado apos saida de funcionario | Medio | Baixo | Controle manual; ativo=false e operacao simples |
| Migracao V6 em prod com colunas nullable | Baixo | Baixo | ADD COLUMN NULL e instant DDL no MySQL 8 |
| valor_final negativo (vales > salario) | Baixo | Baixo | Dominio aceita; UI pode exibir aviso |

---

## Referencias

- docs/plans/BACKLOG-produto.md — bloco EVO-09 (perguntas abertas de produto)
- docs/architecture/estado-atual-dev.md — schema atual de pedidos_pagamento
- docs/data/schema-er.md — diagrama ER completo e documentacao de tabelas (V6)
- docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md — spec tecnica detalhada
- docs/aprendizado/telegram-conversas-multi-turno.md — CommandRouter e multi-turno
- ADR 0004 (taxonomia de ownership) · ADR 0007 (frontmatter YAML) · ADR 0015 (roles x skills)
- Fowler, PEAA, "Single Table Inheritance" — STI vs tabela de extensao separada
- Karwin, SQL Antipatterns, "31 Flavors" — redundancia derivavel em adiantamento
- Winand, Use The Index Luke — CHECK constraints como parte do dominio
