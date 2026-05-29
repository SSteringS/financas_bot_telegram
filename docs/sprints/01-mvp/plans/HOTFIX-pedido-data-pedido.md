# HOTFIX — Bot quebrado: `Field 'data_pedido' doesn't have a default value`

> **Urgente.** Bot está fora do ar em dev (e potencialmente em prod, se o deploy aconteceu). Pedido de pagamento não é persistido — falha com SQL Error 1364 ao tentar INSERT em `pedidos_pagamento`.

---

## Diagnóstico

### Erro observado

```
java.sql.SQLException: Field 'data_pedido' doesn't have a default value
SQL: insert into pedidos_pagamento (data_criacao, descricao, file_id_telegram, imagem_url, status, telegram_message_id, telegram_user_id, valor) values (?,?,?,?,?,?,?,?)
```

### Root cause

**Mismatch entre schema do banco e código:**

- O **banco de dados** tem a migration V2 do BE-01 aplicada (Flyway rodou em algum momento anterior). Isso significa que `pedidos_pagamento` tem colunas novas (`data_pedido`, `requisitante_id`, possivelmente `data_pagamento`, `tipo`, etc), todas declaradas como `NOT NULL` sem default.
- O **código atualmente em `develop`** está num estado anterior à BE-00B e BE-01 (a branch `develop` foi resetada). A `PedidoPagamentoEntity` (ou a antiga `domain/entity/PedidoPagamento`) não tem esses campos novos, então o Hibernate não os inclui no INSERT.

Resultado: INSERT tenta inserir sem `data_pedido` → MySQL rejeita.

### Por que aconteceu

Pelo `git reflog`, o trabalho de BE-00B, BE-01a e BE-01 foi feito e committado, depois movido pra `feature/api-consulta-pedidos-comprovantes`, e `develop` foi resetada pra antes desses commits. Mas o banco já tinha a V2 aplicada de uma execução anterior. Resultado: schema do banco está à frente do código de develop.

---

## Estratégia de hotfix

**Trazer BE-00B + BE-01a + BE-01 da feature branch pra develop**, alinhando o código com o estado real do banco. Tentar um patch isolado em develop (adicionar só os campos faltantes) gera divergência que vai dar conflito quando a feature branch for mergeada via PR depois — desperdício de trabalho.

Essa é a opção que respeita o workflow de branches e mantém a história limpa.

---

## Pré-requisitos

- Acesso ao terminal com git no repo
- IntelliJ aberto pra fazer build + restart local
- Conexão com o banco de dev pra eventualmente verificar estado do schema

---

## Passos de execução

### Passo 1 — Confirmar estado da feature branch

Antes de mergear, validar que a branch `feature/api-consulta-pedidos-comprovantes` tem o código que esperamos:

```bash
git checkout feature/api-consulta-pedidos-comprovantes
git pull
```

Verificações esperadas:

```bash
# Deve existir a entidade JPA nova:
ls financas_bot_telegram/src/main/java/br/com/satyan/stering/saita/financasbottelegram/adapters/out/persistence/entity/PedidoPagamentoEntity.java

# Deve existir o V2:
ls financas_bot_telegram/src/main/resources/db/migration/V2*.sql

# Deve ter campos data_pedido, requisitante_id, tipo etc na entity:
grep -E "data_pedido|requisitante_id|tipo|data_pagamento" financas_bot_telegram/src/main/java/br/com/satyan/stering/saita/financasbottelegram/adapters/out/persistence/entity/PedidoPagamentoEntity.java
```

**Se algum desses não existir, parar e relatar.** Pode ser que o BE-01 tenha entregue só a migration e esquecido de atualizar a entity — nesse caso o hotfix vira "completar BE-01" e exige passos diferentes (ver seção "Cenário alternativo" no fim).

### Passo 2 — Build e teste local na feature branch antes de mergear

Confirmar que a feature branch sobe limpo:

```bash
./mvnw clean package -DskipTests -f financas_bot_telegram/pom.xml
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -f financas_bot_telegram/pom.xml
```

Esperado: app sobe sem erro, Flyway log mostra que V2 **já está aplicada** (status `Success` na tabela `flyway_schema_history`), nenhuma migration nova rodando.

### Passo 3 — Smoke test: enviar pedido via Telegram

Com a app rodando localmente apontando pro banco de dev (ngrok pra webhook):

1. Mandar uma foto pelo Telegram com legenda tipo `100.00 Hotfix teste`
2. Esperado: app retorna sucesso, registro aparece na tabela `pedidos_pagamento` com `data_pedido` preenchido
3. Mandar comprovante: `#<id> pix`
4. Esperado: comprovante salvo, status do pedido vira PAGO

Se passar, código está alinhado com banco. Se não passar, parar e relatar exatamente onde quebrou.

### Passo 4 — Mergear feature branch em develop

```bash
git checkout develop
git pull
git merge feature/api-consulta-pedidos-comprovantes --no-ff -m "hotfix: sincroniza develop com BE-00B + BE-01a + BE-01"
git push origin develop
```

Se houver conflito, resolver manualmente. Conflitos prováveis:
- `pom.xml` se algum commit em develop mexeu nele
- `application*.properties` se algo mudou
- `CLAUDE.md` se foi editado em paralelo

### Passo 5 — Validar develop pós-merge

```bash
./mvnw clean test -f financas_bot_telegram/pom.xml
```

Esperado: 55+ testes verdes (os do BE-01a) + os novos que BE-01 deve ter adicionado.

Subir app de develop e refazer o smoke test do Passo 3.

### Passo 6 — Avaliar deploy em prod

Verificar se prod já está quebrado também:

- Acessar logs da EC2: `ssh ec2... journalctl -u finbot -n 200 --no-pager`
- Procurar pelo mesmo erro `Field 'data_pedido' doesn't have a default value`

**Se prod estiver quebrado:** abrir PR de `develop` pra `main` ASAP, mergear, deixar a pipeline deployar. Antes, conferir que `flyway_schema_history` em prod **já tem V2 aplicada** (`SELECT * FROM flyway_schema_history WHERE version = '2';`). Se sim, ok — o deploy só precisa atualizar o código. Se não, a V2 vai rodar agora no deploy, o que também é ok porque o app vai aceitar.

**Se prod estiver ok** (porque o erro acontece só quando alguém manda mensagem e ninguém mandou ainda): tem tempo. Abrir PR normalmente e tratar com calma.

---

## Cenário alternativo — se a feature branch tem o mesmo bug

Se no Passo 1 a verificação mostrar que `PedidoPagamentoEntity` **não** tem os campos novos (ou o domain POJO não tem, ou o parser/strategy não está populando), o BE-01 está incompleto. Nesse caso:

### Patch direto na feature branch antes do merge

Em `feature/api-consulta-pedidos-comprovantes`:

1. **Ler `V2*.sql`** pra identificar TODOS os campos novos `NOT NULL` adicionados em `pedidos_pagamento` (e possivelmente `comprovantes`)

2. **Atualizar `PedidoPagamentoEntity.java`** adicionando esses campos com as anotações JPA corretas. Por exemplo, se a V2 adicionou `data_pedido DATE NOT NULL`:

```java
@Column(name = "data_pedido", nullable = false)
private LocalDate dataPedido;
```

Idem pra `requisitante_id`, `tipo`, `data_pagamento`.

3. **Atualizar `PedidoPagamento.java`** (domain POJO em `domain/model/`) com os mesmos campos.

4. **Atualizar `PedidoPagamentoMapper.java`** pra mapear esses campos nos dois sentidos (toDomain e toEntity).

5. **Atualizar `PaymentRequestStrategy.parsePedido()`** pra popular os campos novos quando criar um novo pedido:

```java
pedido.setDataPedido(LocalDate.now());
pedido.setRequisitanteId(1L);  // Pedro (default)
pedido.setTipo(extrairTipoDaLegenda(text));  // ou TipoPagamento.OUTRO se não detectar
```

6. **Atualizar testes unitários** do `PaymentRequestStrategyTest` e `PedidoPagamentoMapperTest` pra cobrir os novos campos.

7. **Rodar testes**: `./mvnw test` — deve ficar verde.

8. **Smoke test local**, conforme Passo 3 acima.

9. **Commitar** com mensagem `fix(BE-01): popular data_pedido, requisitante_id, tipo ao salvar pedido` e push.

10. Voltar pro Passo 4 (merge em develop).

---

## Critério de aceitação do hotfix

- [ ] Bot recebe pedido pelo Telegram e persiste com sucesso (sem SQL Error)
- [ ] `data_pedido` preenchido com a data do dia (não null)
- [ ] `requisitante_id` preenchido com `1` (Pedro)
- [ ] `tipo` preenchido conforme legenda ou `OUTRO` como fallback
- [ ] Bot recebe comprovante e atualiza status do pedido pra PAGO
- [ ] `develop` tem todos os commits de BE-00B, BE-01a, BE-01 mergeados
- [ ] Testes unitários passam: `./mvnw test`
- [ ] Smoke test manual em dev passou
- [ ] (Se deploy em prod foi necessário) prod operando normalmente

---

## Reportar status

Criar `docs/status/HOTFIX-pedido-data-pedido.md` seguindo `docs/status/_TEMPLATE.md`. Cobrir:

- Cenário aplicado: "merge direto" ou "patch na feature branch + merge"
- Output do smoke test em dev (IDs do pedido + comprovante criados, com print do `SELECT * FROM pedidos_pagamento ORDER BY id DESC LIMIT 1;`)
- Se prod precisou de deploy emergencial, momento e resultado
- Qualquer ajuste adicional que precisou (campos extras, valores de default diferentes, etc)
- Confirmação de que o `flyway_schema_history` em prod (e dev) tem V2 como `Success`

---

## Lições pra registrar depois (não bloqueia hotfix)

Essa quebra aconteceu porque:

1. **Reset de branch sem cuidado**: develop foi resetada pra antes de BE-01 mas o banco já tinha V2 aplicada. Em projeto solo isso é tolerável, mas vale registrar como ADR: **"se um banco tem migration X aplicada, develop deve ter o código que conhece X, ou todos os ambientes que rodam contra esse banco quebram"**.

2. **Smoke test não foi feito antes de promover BE-01**: o relatório de BE-00B já registrava que smoke test via Telegram não foi feito. BE-01 também não fez. Combinar daqui pra frente: tarefas que mexem em entity/migration **exigem smoke test antes de marcar como done**, mesmo que demore 5 min a mais. Adicionar isso à definition-of-done dos planos futuros.

Esses dois pontos podem virar ADRs em `docs/decisions/` depois do hotfix estar resolvido.
