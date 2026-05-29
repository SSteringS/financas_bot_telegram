# HOTFIX (PATCH) — Completar BE-01 no Java após merge

> Substitui o plano anterior `HOTFIX-pedido-data-pedido.md`. O merge da feature branch já foi feito (commit `db18136`), mas o bug persiste porque o BE-01 entregou só a migration SQL — **o código Java não foi atualizado** para conhecer os campos novos. Este plano é o patch focado pra fechar essa lacuna.

---

## Confirmação do diagnóstico

Verificado lendo `develop` pós-merge:

- ✅ `V2__add_requisitante_dates_categoria_auth.sql` existe — adiciona `requisitante_id` (DEFAULT 1), `data_pedido` (DATE NOT NULL após backfill), `data_pagamento` (nullable), `tipo` (ENUM nullable)
- ❌ `PedidoPagamentoEntity.java` — **não foi atualizada**, ainda tem só os 9 campos originais
- ❌ `PedidoPagamento.java` (POJO domain) — **não foi atualizada**, mesma situação
- ❌ `PedidoPagamentoMapper.java` — **não mapeia** os campos novos
- ❌ `PaymentRequestStrategy.parsePedido()` — **não popula** `dataPedido` (que é obrigatório no banco)
- ❌ `TipoPagamento` enum Java — **não existe**

Único campo de inserção bloqueante: **`data_pedido`** (NOT NULL sem default). Os outros 3 funcionam mesmo sem código (`requisitante_id` tem DEFAULT 1, `data_pagamento` e `tipo` são nullable). Mas vamos completar todos os 4 nesta passada — é o que o BE-01 deveria ter entregue.

---

## Branch

```bash
git checkout develop
git pull
git checkout -b hotfix/BE-01-completar-entity
```

---

## Patch — código pronto pra colar

### 1. Criar enum `TipoPagamento`

**Novo arquivo:** `financas_bot_telegram/src/main/java/br/com/satyan/stering/saita/financasbottelegram/domain/enums/TipoPagamento.java`

```java
package br.com.satyan.stering.saita.financasbottelegram.domain.enums;

public enum TipoPagamento {
    BOLETO,
    PIX,
    TED,
    AGENDAMENTO,
    OUTRO
}
```

Valores batem com o ENUM SQL da V2.

### 2. Atualizar POJO domain `PedidoPagamento`

**Arquivo:** `financas_bot_telegram/src/main/java/br/com/satyan/stering/saita/financasbottelegram/domain/model/PedidoPagamento.java`

Adicionar 4 campos e o import de `LocalDate` e `TipoPagamento`:

```java
package br.com.satyan.stering.saita.financasbottelegram.domain.model;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoPagamento {
    private Long id;
    private Long requisitanteId;
    private String telegramUserId;
    private String telegramMessageId;
    private String fileIdTelegram;
    private String imagemUrl;
    private BigDecimal valor;
    private String descricao;
    private StatusPedido status;
    private TipoPagamento tipo;
    private LocalDate dataPedido;
    private LocalDate dataPagamento;
    private LocalDateTime dataCriacao;
}
```

### 3. Atualizar JPA entity `PedidoPagamentoEntity`

**Arquivo:** `financas_bot_telegram/src/main/java/br/com/satyan/stering/saita/financasbottelegram/adapters/out/persistence/entity/PedidoPagamentoEntity.java`

```java
package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Entity
@Table(name = "pedidos_pagamento")
public class PedidoPagamentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "requisitante_id", nullable = false)
    private Long requisitanteId;

    @Column(name = "telegram_user_id")
    private String telegramUserId;

    @Column(name = "telegram_message_id")
    private String telegramMessageId;

    @Column(name = "file_id_telegram")
    private String fileIdTelegram;

    @Column(name = "imagem_url", columnDefinition = "TEXT")
    private String imagemUrl;

    @Column(precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    private StatusPedido status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TipoPagamento tipo;

    @Column(name = "data_pedido", nullable = false)
    private LocalDate dataPedido;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @CreationTimestamp
    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;
}
```

### 4. Atualizar `PedidoPagamentoMapper`

**Arquivo:** `financas_bot_telegram/src/main/java/br/com/satyan/stering/saita/financasbottelegram/adapters/out/persistence/mapper/PedidoPagamentoMapper.java`

Adicionar os 4 campos novos nas duas direções:

```java
package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import org.springframework.stereotype.Component;

@Component
public class PedidoPagamentoMapper {

    public PedidoPagamento toDomain(PedidoPagamentoEntity entity) {
        if (entity == null) return null;
        return PedidoPagamento.builder()
                .id(entity.getId())
                .requisitanteId(entity.getRequisitanteId())
                .telegramUserId(entity.getTelegramUserId())
                .telegramMessageId(entity.getTelegramMessageId())
                .fileIdTelegram(entity.getFileIdTelegram())
                .imagemUrl(entity.getImagemUrl())
                .valor(entity.getValor())
                .descricao(entity.getDescricao())
                .status(entity.getStatus())
                .tipo(entity.getTipo())
                .dataPedido(entity.getDataPedido())
                .dataPagamento(entity.getDataPagamento())
                .dataCriacao(entity.getDataCriacao())
                .build();
    }

    public PedidoPagamentoEntity toEntity(PedidoPagamento domain) {
        if (domain == null) return null;
        PedidoPagamentoEntity entity = new PedidoPagamentoEntity();
        entity.setId(domain.getId());
        entity.setRequisitanteId(domain.getRequisitanteId());
        entity.setTelegramUserId(domain.getTelegramUserId());
        entity.setTelegramMessageId(domain.getTelegramMessageId());
        entity.setFileIdTelegram(domain.getFileIdTelegram());
        entity.setImagemUrl(domain.getImagemUrl());
        entity.setValor(domain.getValor());
        entity.setDescricao(domain.getDescricao());
        entity.setStatus(domain.getStatus());
        entity.setTipo(domain.getTipo());
        entity.setDataPedido(domain.getDataPedido());
        entity.setDataPagamento(domain.getDataPagamento());
        entity.setDataCriacao(domain.getDataCriacao());
        return entity;
    }
}
```

### 5. Atualizar `PaymentRequestStrategy.parsePedido()`

**Arquivo:** `financas_bot_telegram/src/main/java/br/com/satyan/stering/saita/financasbottelegram/adapters/in/telegram/strategy/PaymentRequestStrategy.java`

Substituir o método `parsePedido` por:

```java
private PedidoPagamento parsePedido(Message message) {
    String text = message.getCaption().trim();
    Matcher matcher = PEDIDO_PATTERN.matcher(text);

    if (!matcher.matches()) {
        throw new IllegalArgumentException("Formato inválido. Use: pedido <valor> <descrição>");
    }

    String valorStr = matcher.group(1).replace(',', '.');
    BigDecimal valor = new BigDecimal(valorStr);
    String descricao = matcher.group(3);

    return PedidoPagamento.builder()
        .valor(valor)
        .descricao(descricao)
        .telegramUserId(message.getFrom().getId().toString())
        .telegramMessageId(message.getMessageId().toString())
        .status(StatusPedido.PENDENTE)
        .requisitanteId(1L)              // default: Pedro (único requisitante por enquanto)
        .dataPedido(LocalDate.now())     // obrigatório no banco
        .build();
}
```

Adicionar o import:

```java
import java.time.LocalDate;
```

**Observação consciente:** `tipo` fica `null` por enquanto. Extração de tipo da legenda (BOLETO/PIX/TED/AGENDAMENTO/OUTRO) é trabalho da próxima feature, não bloqueia hotfix. Nullable no banco, então OK.

### 6. Atualizar testes existentes

Os testes da BE-01a (`PedidoPagamentoMapperTest`, `PaymentRequestStrategyTest`) vão quebrar porque o builder agora aceita campos novos. Atualizar:

**`PedidoPagamentoMapperTest.java`** — em todos os testes que constroem `PedidoPagamento` ou `PedidoPagamentoEntity`, adicionar os 4 campos novos com valores não-null. Exemplo:

```java
PedidoPagamento domain = PedidoPagamento.builder()
    .id(1L)
    .requisitanteId(1L)                    // NOVO
    .telegramUserId("123")
    .valor(new BigDecimal("100.00"))
    .descricao("Teste")
    .status(StatusPedido.PENDENTE)
    .tipo(TipoPagamento.PIX)               // NOVO
    .dataPedido(LocalDate.of(2026,5,11))   // NOVO
    .dataPagamento(LocalDate.of(2026,5,11))// NOVO (opcional, pode deixar null)
    .dataCriacao(LocalDateTime.now())
    .build();
```

E nos asserts do round-trip, adicionar verificação dos 4 campos.

**`PaymentRequestStrategyTest.java`** — no teste `deveProcessarPedidoComTodosOsDados`, adicionar assertion:

```java
assertThat(capturado.getRequisitanteId()).isEqualTo(1L);
assertThat(capturado.getDataPedido()).isEqualTo(LocalDate.now());
```

---

## Validação

### 1. Build e testes

```bash
cd financas_bot_telegram
./mvnw clean test
```

Esperado: tudo verde. Se algum teste quebrar por causa dos campos novos, ajustar fixture.

### 2. Subir app em dev e smoke test

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Esperado nos logs:
- Flyway log mostra `V2` como `Success` (já aplicada anteriormente, sem rerun)
- App sobe sem erro

Via Telegram, mandar foto com legenda `100.00 hotfix test`. Esperado:
- ✅ Bot responde com mensagem de sucesso ("Pedido registrado")
- ✅ INSERT inclui `data_pedido` e `requisitante_id` no SQL gerado pelo Hibernate
- ✅ Verificar no banco:

```sql
SELECT id, valor, descricao, data_pedido, requisitante_id, tipo, status
FROM pedidos_pagamento
ORDER BY id DESC LIMIT 1;
```

Esperado: linha com `data_pedido` = hoje, `requisitante_id` = 1, `tipo` = NULL, `status` = PENDENTE.

### 3. Smoke test do comprovante

Via Telegram, mandar comprovante: `#<id> pix`. Esperado: status do pedido vira PAGO, comprovante salvo.

---

## Merge e deploy

Se tudo validado:

```bash
git add .
git commit -m "fix(BE-01): popular campos data_pedido, requisitante_id, tipo, data_pagamento na entity e parser

Completa a BE-01 que tinha entregue só a migration V2 sem atualizar o
código Java correspondente. Resolve o erro 'Field data_pedido doesn't
have a default value' no INSERT de pedidos_pagamento.

Mudanças:
- domain/enums/TipoPagamento.java (novo)
- domain/model/PedidoPagamento.java: adiciona requisitanteId, tipo, dataPedido, dataPagamento
- adapters/out/persistence/entity/PedidoPagamentoEntity.java: idem com anotações JPA
- adapters/out/persistence/mapper/PedidoPagamentoMapper.java: mapeia campos novos
- adapters/in/telegram/strategy/PaymentRequestStrategy.java: parsePedido popula requisitanteId=1 e dataPedido=now
- Testes ajustados pra cobrir novos campos"
git push origin hotfix/BE-01-completar-entity
```

Abrir PR direto pra `develop`. Após approve + merge, abrir PR `develop → main` pra deploy em prod (só se prod estiver afetado também — verificar logs primeiro).

---

## Critério de aceitação

- [ ] `TipoPagamento.java` criado em `domain/enums/`
- [ ] `PedidoPagamento.java` tem 4 campos novos: `requisitanteId`, `tipo`, `dataPedido`, `dataPagamento`
- [ ] `PedidoPagamentoEntity.java` tem 4 campos novos com anotações JPA corretas
- [ ] `PedidoPagamentoMapper.java` mapeia os 4 campos em ambas direções
- [ ] `PaymentRequestStrategy.parsePedido()` popula `requisitanteId=1` e `dataPedido=LocalDate.now()` ao criar novo pedido
- [ ] `./mvnw clean test` passa (testes existentes ajustados pra novos campos)
- [ ] Smoke test em dev: pedido persiste com `data_pedido` preenchido
- [ ] Smoke test em dev: comprovante atualiza status
- [ ] Logs do Hibernate mostram o INSERT incluindo `data_pedido`, `requisitante_id`, `tipo` (mesmo que tipo seja NULL)

---

## Fora de escopo deste patch

- **Extração de tipo da legenda** (parsing de "boleto/pix/ted/..." da string da legenda). Fica como melhoria futura. Por enquanto, `tipo` fica null em pedidos novos.
- **API REST de consulta** (endpoints pra o front consumir). É a feature seguinte da Fase 3, não parte do hotfix.
- **Entidades pra Requisitante e AuthToken** (que a V2 também criou). Vão ser implementadas quando os endpoints de auth e leitura precisarem. Por enquanto o `requisitanteId` é só um `Long` na entity, sem `@ManyToOne` mapeado — funciona.
- **Mudar `requisitante_id` no banco pra remover DEFAULT 1** — agora que o código popula explicitamente, podemos remover o default em uma V3 futura. Não nesta tarefa.

---

## Reportar status

Criar `docs/status/HOTFIX-pedido-data-pedido-PATCH.md` seguindo `docs/status/_TEMPLATE.md`. Cobrir:

- Output de `./mvnw test` (sumário final)
- Smoke test: ID do pedido criado + `SELECT` mostrando todos os campos preenchidos
- Estado da pipeline em CI após push
- Próximo passo: revisão de `FASE-3-VISUALIZACAO.md` pra continuar com endpoints REST
