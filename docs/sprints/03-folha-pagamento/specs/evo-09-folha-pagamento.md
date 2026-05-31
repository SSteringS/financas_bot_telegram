---
sprint: "03"
feature: EVO-09
slug: folha-pagamento
status: refinado
arquiteto: sim
data: 2026-05-31
revisoes:
  - 2026-05-31: modelo inicial
  - 2026-05-31: revisao DBA - CHECK constraints, mes_referencia, atualizado_em
---

# Spec Tecnica — EVO-09: Folha de Pagamento

> Refinada pelo arquiteto em 2026-05-31. Decisoes arquiteturais em ADR 0016.
> **Status:** refinado — aguarda homologacao do humano antes de despachar para implementacao.

---

## 1. Visao geral

Adicionar gestao de folha de pagamento domestica:

- Cadastro de **funcionario** com salario base e dados bancarios.
- Registro de **vales** (Pedidos categoria=VALE) vinculados ao funcionario e mes.
- Registro de **adiantamentos parcelados** com plano de desconto mensal.
- **Fechamento mensal**: calcula salario liquido, gera Pedido FOLHA com snapshot do calculo
  em `observacao`, marca vales como fechados, incrementa parcelas de adiantamentos ativos.
- Historico de fechamentos via listagem de Pedidos WHERE categoria=FOLHA AND funcionario_id={id}.

Canal de cadastro: **front-end exclusivamente** (sem novos comandos bot nesta sprint).

---

## 2. Migracao de banco — V6

Arquivo: `financas_bot_telegram/src/main/resources/db/migration/V6__folha_pagamento.sql`

```sql
-- ============================================================
-- V6: Folha de pagamento
-- Revisado pos-analise DBA (2026-05-31):
--   + CHECK constraints em todas as tabelas
--   + atualizado_em em funcionario (rastreabilidade de salario)
--   + mes_referencia + UNIQUE INDEX para idempotencia no banco
--   + Sem pedido_folha_meta (invariante por CHECK em pedidos_pagamento)
--   + Sem fechamento_mes (snapshot em observacao do Pedido FOLHA)
-- ============================================================

-- 1. Funcionario domestico
CREATE TABLE funcionario (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome                     VARCHAR(120)                NOT NULL,
    salario_base             DECIMAL(10,2)               NOT NULL,
    forma_pagamento          ENUM('PIX','TED')           NOT NULL,
    chave_pix                VARCHAR(255)                NULL,
    banco                    VARCHAR(50)                 NULL,
    agencia                  VARCHAR(20)                 NULL,
    conta                    VARCHAR(30)                 NULL,
    tipo_conta               ENUM('CORRENTE','POUPANCA') NULL,
    conta_propria            BOOLEAN NOT NULL DEFAULT TRUE,
    obs_pagamento            VARCHAR(500)                NULL,
    dia_pagamento_referencia INT                         NULL,
    ativo                    BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_func_pix CHECK (
        forma_pagamento <> 'PIX' OR chave_pix IS NOT NULL
    ),
    CONSTRAINT chk_func_ted CHECK (
        forma_pagamento <> 'TED'
        OR (banco IS NOT NULL AND agencia IS NOT NULL AND conta IS NOT NULL AND tipo_conta IS NOT NULL)
    ),
    CONSTRAINT chk_func_dia CHECK (
        dia_pagamento_referencia IS NULL OR dia_pagamento_referencia BETWEEN 1 AND 31
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Plano de adiantamento parcelado
CREATE TABLE adiantamento (
    id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
    funcionario_id BIGINT        NOT NULL,
    descricao      VARCHAR(255)  NOT NULL,
    valor_total    DECIMAL(10,2) NOT NULL,
    valor_parcela  DECIMAL(10,2) NOT NULL,
    num_parcelas   INT           NOT NULL,
    parcelas_pagas INT           NOT NULL DEFAULT 0,
    data_inicio    DATE          NOT NULL,
    ativo          BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_adiant_funcionario   FOREIGN KEY (funcionario_id) REFERENCES funcionario(id),
    CONSTRAINT chk_adiant_consistencia CHECK (ABS(valor_total - (valor_parcela * num_parcelas)) <= 0.01),
    CONSTRAINT chk_adiant_parcelas     CHECK (parcelas_pagas BETWEEN 0 AND num_parcelas),
    CONSTRAINT chk_adiant_valores      CHECK (valor_total > 0 AND valor_parcela > 0 AND num_parcelas > 0),
    INDEX idx_adiant_func_ativo (funcionario_id, ativo, data_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Extensao minima em pedidos_pagamento (STI com CHECK + idempotencia via UNIQUE INDEX)
-- ADD COLUMN NULL = instant DDL no MySQL 8 (sem lock de tabela)
ALTER TABLE pedidos_pagamento
    ADD COLUMN categoria      ENUM('VALE','FOLHA') NULL          AFTER status,
    ADD COLUMN funcionario_id BIGINT               NULL          AFTER categoria,
    ADD COLUMN fechado        BOOLEAN NOT NULL DEFAULT FALSE      AFTER funcionario_id,
    ADD COLUMN observacao     VARCHAR(1000)        NULL          AFTER fechado,
    ADD COLUMN mes_referencia DATE                 NULL          AFTER observacao,
    ADD CONSTRAINT fk_pedido_funcionario FOREIGN KEY (funcionario_id) REFERENCES funcionario(id),
    ADD CONSTRAINT chk_pedido_folha CHECK (
        categoria IS NULL
        OR (categoria IN ('VALE','FOLHA') AND funcionario_id IS NOT NULL)
    ),
    ADD CONSTRAINT chk_pedido_folha_mes CHECK (
        categoria <> 'FOLHA' OR mes_referencia IS NOT NULL
    ),
    -- NULLs sao distintos no UNIQUE MySQL:
    --   VALE (mes_referencia=NULL) coexistem; FOLHA garante unicidade por funcionario/mes
    ADD UNIQUE INDEX uq_folha_por_mes (funcionario_id, mes_referencia),
    ADD INDEX idx_pedido_folha (funcionario_id, categoria, fechado, data_criacao DESC);
```

**Notas de migracao:**
- Todas as novas colunas em `pedidos_pagamento` sao NULL ou DEFAULT — instant DDL, sem lock.
- `fechado=FALSE` padrao: historico existente fica intacto.
- `mes_referencia=NULL` e `funcionario_id=NULL` em pedidos existentes — UNIQUE INDEX nao conflita
  pois `(NULL, NULL)` sao distintos no MySQL.
- Flyway roda automaticamente no startup; nenhum script manual necessario.

---

## 3. Estrutura hexagonal

### Domain

```
domain/entity/
  Funcionario.java
  Adiantamento.java
  -- SEM FechamentoMes.java (snapshot em observacao do Pedido FOLHA)

domain/vo/
  FormaPagamento.java    (enum: PIX, TED)
  CategoriaPedido.java   (enum: VALE, FOLHA)
```

> **Nota sobre CategoriaPedido vs TipoPagamento:** dimensoes ortogonais.
> `CategoriaPedido` = VALE ou FOLHA (categoria do pedido no dominio de folha).
> `TipoPagamento` = como o pagamento foi feito (PIX, TED, BOLETO...).
> Confirmar na implementacao que nao ha colisao semantica.

### Application ports (in)

```
application/port/in/
  CadastrarFuncionarioPortIn.java
  AtualizarFuncionarioPortIn.java
  CadastrarValePortIn.java
  CadastrarAdiantamentoPortIn.java
  CancelarAdiantamentoPortIn.java
  FecharMesPortIn.java
  ConsultarFolhaPortIn.java      -- lista Pedidos FOLHA do funcionario
```

### Application ports (out)

```
application/port/out/
  FuncionarioRepositoryPortOut.java
  AdiantamentoRepositoryPortOut.java
  -- SEM FechamentoMesRepositoryPortOut.java
  -- PedidoRepositoryPortOut.java (existente) — adicionar:
  --   findValesAbertos(funcionarioId, inicio, fim)
  --   existsFolhaNomes(funcionarioId, mesReferencia)  <- idempotencia
  --   markAllClosed(List<Long> pedidoIds)
  --   findFolhasByFuncionario(funcionarioId)
```

### Application use cases

```
application/usecase/
  CadastrarFuncionarioUseCase.java
  CadastrarValeUseCase.java
  CadastrarAdiantamentoUseCase.java
  FecharMesUseCase.java           -- ver secao 4
  ConsultarFolhaUseCase.java
```

### Adapters in

```
adapters/in/web/
  FuncionarioController.java
  FolhaController.java
```

### Adapters out

```
adapters/out/persistence/
  FuncionarioJpaRepository.java
  AdiantamentoJpaRepository.java
  -- SEM FechamentoMesJpaRepository.java
  FuncionarioRepositoryAdapter.java
  AdiantamentoRepositoryAdapter.java
```

---

## 4. Logica do FecharMesUseCase

```
@Transactional
fechar(funcionarioId: Long, mesReferencia: YearMonth, ajuste: BigDecimal):

  (1) IDEMPOTENCIA — verificar se ja existe Pedido categoria=FOLHA para (funcionario_id, mesReferencia)
      se existsFolhaNomes(funcionarioId, mesReferencia): throw FechamentoDuplicadoException
      -- O UNIQUE INDEX uq_folha_por_mes no banco e a guarda adicional em caso de race condition.

  (2) funcionario = funcionarioRepo.findAtivoById(funcionarioId)
      se nao encontrado: throw FuncionarioNaoEncontradoException

  (3) primeiroDoMes = mesReferencia.atDay(1)
      ultimoDoMes   = mesReferencia.atEndOfMonth()

  (4) vales = pedidoRepo.findValesAbertos(funcionarioId, primeiroDoMes, ultimoDoMes)
      -- WHERE funcionario_id=? AND categoria='VALE' AND fechado=FALSE
      -- AND DATE(data_criacao) BETWEEN primeiroDoMes AND ultimoDoMes

  (5) adiantamentosAtivos = adiantamentoRepo.findAtivosParaMes(funcionarioId, primeiroDoMes)
      -- WHERE funcionario_id=? AND ativo=TRUE AND data_inicio <= primeiroDoMes
      --   AND parcelas_pagas < num_parcelas

  (6) totalVales    = soma(vale.valor para vale em vales)
      totalParcelas = soma(adiant.valorParcela para adiant em adiantamentosAtivos)
      valorFinal    = funcionario.salarioBase - totalVales - totalParcelas + ajuste

  (7) observacao = gerarTextoFechamento(funcionario.salarioBase, totalVales,
                     vales.size(), totalParcelas, adiantamentosAtivos.size(),
                     ajuste, valorFinal)
      -- Exemplo de saida:
      -- "Salario base: R$ 2.000,00
      --  Vales: R$ 150,00 (3 vales)
      --  Adiantamentos: R$ 80,00 (2 parcelas)
      --  Ajuste: R$ 0,00
      --  Liquido: R$ 1.770,00"

  (8) pedidoFolha = pedidoRepo.criar(
        categoria      = FOLHA,
        valor          = valorFinal,
        status         = PENDENTE,
        funcionarioId  = funcionarioId,
        observacao     = observacao,
        mesReferencia  = primeiroDoMes   -- 'YYYY-MM-01'; garante idempotencia via UNIQUE INDEX
      )

  (9) para cada vale em vales:
        vale.fechado = true
        pedidoRepo.save(vale)

  (10) para cada adiantamento em adiantamentosAtivos:
         adiantamento.parcelasPagas += 1
         se adiantamento.parcelasPagas >= adiantamento.numParcelas:
           adiantamento.ativo = false
         adiantamentoRepo.save(adiantamento)

  (11) return PedidoFolhaResponse(pedidoFolha)
```

**Nota sobre idempotencia:** o passo (1) e a guarda principal; o UNIQUE INDEX
`uq_folha_por_mes (funcionario_id, mes_referencia)` e a guarda no banco — um duplo-clique
rapido que passe pelo check vai bater no UNIQUE e receber `DataIntegrityViolationException`,
que deve ser traduzida para `FechamentoDuplicadoException` no service.

**Nota sobre `requisitante_id`:** verificar antes de BE-folha-6 se a coluna aceita NULL.
Pedidos FOLHA nao tem requisitante humano natural. Se NOT NULL, avaliar: (a) tornar nullable
via ALTER, ou (b) usar o `telegram_user_id` do operador que fechou como proxy.

---

## 5. Endpoints REST

### Funcionarios

```
POST   /api/funcionarios              CadastrarFuncionarioPortIn
GET    /api/funcionarios              listar ativos
PUT    /api/funcionarios/{id}         atualizar (salario_base, conta_propria, obs_pagamento)
DELETE /api/funcionarios/{id}         desativar (ativo=false)
```

### Vales e Adiantamentos

```
POST   /api/funcionarios/{id}/vales
GET    /api/funcionarios/{id}/vales?mes=YYYY-MM   listar vales do mes
POST   /api/funcionarios/{id}/adiantamentos
GET    /api/funcionarios/{id}/adiantamentos       listar adiantamentos ativos
DELETE /api/adiantamentos/{id}                    cancelar (ativo=false)
```

### Fechamento

```
POST   /api/funcionarios/{id}/fechamentos
       body: { "mes": "2026-05", "ajuste": 0.00 }
       retorna: Pedido FOLHA criado (com observacao preenchida)

GET    /api/funcionarios/{id}/fechamentos
       retorna: lista de Pedidos WHERE categoria=FOLHA AND funcionario_id={id}
```

---

## 6. Telas front-end (escopo)

### Tela: Lista de Funcionarios
- Tabela: nome, salario base, status (ativo/inativo).
- Badge laranja para funcionarios com conta_propria = false.
- Botao Novo funcionario abre form de cadastro com campos bancarios condicionais por forma_pagamento.
- Clique na linha navega para Tela de Folha do Funcionario.

### Tela: Folha do Funcionario
- Header: nome, salario base. Badge de conta de terceiro se conta_propria=false.
- Secao Vales do mes: seletor de mes, tabela (descricao, valor, status), botao Registrar vale.
- Secao Adiantamentos ativos: tabela (descricao, parcela X/total, valor_parcela), botao Novo.
- Secao Fechamentos anteriores: lista Pedidos FOLHA (mes, valor_final, status).
  Clique expande observacao com o breakdown do calculo.
- Botao Fechar mes YYYY-MM quando o mes nao foi fechado ainda.

### Modal: Fechar Mes
- Previa calculada em tempo real: salario base, total vales, total parcelas, ajuste, valor final.
- Botao Confirmar fechamento.

---

## 7. Decisao pendente — bloqueia FE-folha-2 e BE-folha-4

> Pergunta ao PO: pedidos VALE aparecem na lista principal do Pedro?

| Opcao | Comportamento | Impacto tecnico |
|---|---|---|
| A) Nao aparecem | Vales exclusivos na tela de folha | GET /api/pedidos filtra: AND categoria IS NULL |
| B) Aparecem com destaque | Pedro ve tudo; tag visual VALE/FOLHA | Nenhuma mudanca na query |

**Decisao pendente com o humano/PO antes de iniciar BE-folha-4 e FE-folha-2.**

---

## 8. Breakdown de tarefas

### Backend

| ID | Titulo | Dependencias | Porte |
|---|---|---|---|
| BE-folha-1 | Migracao V6 — DDL + Flyway | — | P |
| BE-folha-2 | Entidades JPA + repositorios (Funcionario, Adiantamento) | BE-folha-1 | M |
| BE-folha-3 | CadastrarFuncionario + CRUD /api/funcionarios | BE-folha-2 | M |
| BE-folha-4 | CadastrarVale + endpoint vales | BE-folha-2 | P |
| BE-folha-5 | CadastrarAdiantamento + endpoint adiantamentos + cancelamento | BE-folha-2 | P |
| BE-folha-6 | FecharMesUseCase + endpoint fechamento (gera Pedido FOLHA + observacao) | BE-folha-2,4,5 | M |
| BE-folha-7 | Testes: FecharMesUseCase (unitario) + integracao endpoint | BE-folha-6 | M |

### Frontend

| ID | Titulo | Dependencias | Porte |
|---|---|---|---|
| FE-folha-1 | Tela Funcionarios (lista + form cadastro/edicao + badge conta de terceiro) | BE-folha-3 | M |
| FE-folha-2 | Tela Folha do Funcionario (vales + adiantamentos + accordion fechamentos) | BE-folha-4,5,6 + decisao sec7 | G |
| FE-folha-3 | Modal fechamento com calculo em tempo real | BE-folha-6 | M |

### Ordem sugerida de despacho

1. BE-folha-1 (habilita tudo)
2. BE-folha-2 (habilita todos os outros BE)
3. BE-folha-3 + BE-folha-4 + BE-folha-5 (paralelos)
4. BE-folha-6 (depende de 2, 4, 5)
5. BE-folha-7 (depende de 6)
6. FE-folha-1 (pode comecar em paralelo com BE-folha-3)
7. FE-folha-2 (depende de BE-folha-4, 5, 6 + decisao secao 7)
8. FE-folha-3 (depende de BE-folha-6)

---

## 9. Riscos e dependencias

| Item | Descricao | Acao |
|---|---|---|
| requisitante_id nullability | Verificar se aceita NULL antes de BE-folha-6 | Verificar schema; ALTER se necessario |
| Decisao pendente sec.7 | Bloqueia BE-folha-4 e FE-folha-2 | Resolver com PO antes de despachar |
| Idempotencia race condition | Duplo-clique: UNIQUE INDEX barra o insert com DataIntegrityViolationException | Traduzir para FechamentoDuplicadoException |
| Migracao V6 em prod | DDL aditivo; instant DDL MySQL 8 | Baixo risco; Flyway cuida |
| CategoriaPedido vs TipoPagamento | Confirmar ortogonalidade na implementacao | Verificar em BE-folha-2 |
| valor_final negativo | Se vales > salario, liquido e negativo | Decidir se UI mostra aviso |

---

## 10. Referencias

- ADR 0016 (docs/decisions/0016-evo09-folha-pagamento.md) — decisoes arquiteturais
- docs/data/schema-er.md — diagrama ER completo e documentacao de tabelas
- docs/plans/BACKLOG-produto.md — bloco EVO-09
- docs/architecture/estado-atual-dev.md — schema atual de pedidos_pagamento
- docs/aprendizado/telegram-conversas-multi-turno.md — CommandRouter e multi-turno
