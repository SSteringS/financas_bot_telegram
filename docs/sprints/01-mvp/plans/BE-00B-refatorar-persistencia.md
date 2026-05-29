# BE-00B — Refatorar camada de persistência (separar domain de JPA + eliminar dual port)

> **Esta tarefa precede a Fase 3 (`FASE-3-VISUALIZACAO.md`)**. Executar **depois da BE-00 (Flyway)** estar merge em develop. Não tem dependência funcional com BE-00, apenas ordenação prática.

---

## Contexto

A análise em `docs/architecture/estado-atual.md` (seções 3 e 7) identificou duas dívidas técnicas na camada de persistência:

1. **Domain entities anotadas com JPA** — `domain/entity/PedidoPagamento.java` e `domain/entity/Comprovante.java` têm `@Entity`, `@Table`, `@Column`, etc. Isso vaza Hibernate pra dentro do domínio, contrariando a regra hexagonal de domínio puro.

2. **Dual port** — existem dois ports concorrentes pra cada repositório:
   - `PedidoPagamentoRepository` (extends `JpaRepository<PedidoPagamento, Long>`) — vaza Spring Data
   - `PedidoPagamentoRepositoryPort` (interface limpa, com `save`/`findById`)
   
   E o mesmo padrão duplicado pra `Comprovante`. Refactor inacabado.

Esta tarefa resolve **as duas pendências em uma só passada**, porque ambas tocam os mesmos arquivos da camada de persistência. Fazê-las separadamente exigiria refatorar duas vezes os adapters e mappers.

**Não está no escopo desta tarefa:**
- Mudar `tipo_pagamento` de VARCHAR pra enum (será tarefa separada, possivelmente parte da BE-01).
- Adicionar regras de negócio nas entidades de domínio (métodos como `marcarComoPago()`, `cancelar()` etc). O escopo aqui é puramente estrutural — separar arquivos, criar mappers, eliminar duplicações. Comportamento permanece idêntico.

---

## Objetivo

Ao final desta tarefa:

1. As classes de domínio são POJOs puros, sem **nenhuma** anotação de JPA, Hibernate ou Spring
2. As entidades JPA vivem isoladamente em `adapters/out/persistence/entity/`, sem vazar pra fora dali
3. Existem mappers explícitos (`adapters/out/persistence/mapper/`) que convertem entre domain ↔ entity JPA
4. Cada repositório tem **um único port** (`*RepositoryPort`), sem o equivalente que extends `JpaRepository`
5. Todos os services e use cases continuam funcionando, agora dependendo apenas dos `*RepositoryPort`
6. Os testes existentes continuam passando
7. O fluxo do bot Telegram (registrar pedido, registrar comprovante) continua funcionando end-to-end

---

## Arquivos esperados ao final

### Adicionados (novos)

- `domain/model/PedidoPagamento.java` (POJO puro)
- `domain/model/Comprovante.java` (POJO puro)
- `adapters/out/persistence/entity/PedidoPagamentoEntity.java` (JPA, antes era `domain/entity/PedidoPagamento.java`)
- `adapters/out/persistence/entity/ComprovanteEntity.java` (JPA, antes era `domain/entity/Comprovante.java`)
- `adapters/out/persistence/mapper/PedidoPagamentoMapper.java`
- `adapters/out/persistence/mapper/ComprovanteMapper.java`

### Removidos

- `domain/entity/PedidoPagamento.java` (substituída pela versão POJO em `domain/model/`)
- `domain/entity/Comprovante.java` (substituída pela versão POJO em `domain/model/`)
- `application/port/out/PedidoPagamentoRepository.java` (dual port — eliminado)
- `application/port/out/ComprovanteRepository.java` (dual port — eliminado)
- A pasta `domain/entity/` deve ficar vazia e ser removida

### Modificados

- `application/port/out/PedidoPagamentoRepositoryPort.java` — assinaturas usam `domain.model.PedidoPagamento` (em vez de `domain.entity.PedidoPagamento`)
- `application/port/out/ComprovanteRepositoryPort.java` — idem com Comprovante
- `adapters/out/persistence/PedidoPagamentoJpaRepository.java` — agora estende `JpaRepository<PedidoPagamentoEntity, Long>` (entity, não domain)
- `adapters/out/persistence/ComprovanteJpaRepository.java` — idem com `ComprovanteEntity`
- `adapters/out/persistence/PedidoPagamentoRepositoryAdapter.java` — usa mapper na entrada/saída de cada operação
- `adapters/out/persistence/ComprovanteRepositoryAdapter.java` — idem
- Todos os arquivos que importavam `domain.entity.PedidoPagamento` ou `domain.entity.Comprovante` — atualizar import pra `domain.model.PedidoPagamento` / `domain.model.Comprovante`
- Arquivos que injetavam `PedidoPagamentoRepository` ou `ComprovanteRepository` (os ports duplicados) — trocar pra `*RepositoryPort`

Suspeitos de impacto (verificar com Find Usages):
- `application/services/SalvarPedidoPagamentoServiceImpl.java`
- `application/services/RegistrarComprovanteServiceImpl.java`
- `application/usecases/SalvarPedidoPagamentoUsecase.java`
- `application/usecases/RegistrarComprovanteUsecase.java`
- `adapters/in/telegram/strategy/PaymentRequestStrategy.java` (usa `domain.entity.PedidoPagamento`)
- `adapters/in/telegram/strategy/PaymentProofStrategy.java` (usa `domain.entity.Comprovante`)

---

## Passos de execução

### Passo 1 — Criar a estrutura de pastas nova

```
mkdir -p adapters/out/persistence/entity
mkdir -p adapters/out/persistence/mapper
```

(`domain/model/` já existe — `TelegramMediaGroup.java` mora nela hoje.)

### Passo 2 — Criar `domain/model/PedidoPagamento.java` como POJO puro

```java
package br.com.satyan.stering.saita.financasbottelegram.domain.model;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import java.math.BigDecimal;
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
    private String telegramUserId;
    private String telegramMessageId;
    private String fileIdTelegram;
    private String imagemUrl;
    private BigDecimal valor;
    private String descricao;
    private StatusPedido status;
    private LocalDateTime dataCriacao;
}
```

Notar: **zero anotações de framework**. Lombok é aceitável (não é framework de runtime, é só geração de código em compile-time).

### Passo 3 — Criar `domain/model/Comprovante.java` como POJO puro

```java
package br.com.satyan.stering.saita.financasbottelegram.domain.model;

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
public class Comprovante {
    private Long id;
    private Long pedidoId;          // <-- nota: ID, não a entidade inteira
    private String fileIdTelegram;
    private String imagemUrl;
    private String tipoPagamento;
    private LocalDateTime dataPagamento;
}
```

**Decisão consciente:** o domínio guarda só `pedidoId` (Long), não a `PedidoPagamento` inteira. Razões: simplicidade, evita ciclos, evita carregar pedido toda vez que carrega comprovante. Se alguma regra futura precisar do pedido, busca via repository.

### Passo 4 — Criar `adapters/out/persistence/entity/PedidoPagamentoEntity.java`

Mover a classe atual de `domain/entity/PedidoPagamento.java` pra esse novo path, **renomeando** pra `PedidoPagamentoEntity` e ajustando o package:

```java
package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import jakarta.persistence.*;
import java.math.BigDecimal;
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

    @CreationTimestamp
    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;
}
```

Conteúdo é praticamente idêntico ao da classe antiga, com 2 diferenças: package novo e nome `PedidoPagamentoEntity`.

### Passo 5 — Criar `adapters/out/persistence/entity/ComprovanteEntity.java`

```java
package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Entity
@Table(name = "comprovantes")
public class ComprovanteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoPagamentoEntity pedido;

    @Column(name = "file_id_telegram")
    private String fileIdTelegram;

    @Column(name = "imagem_url", columnDefinition = "TEXT")
    private String imagemUrl;

    @Column(name = "tipo_pagamento")
    private String tipoPagamento;

    @CreationTimestamp
    @Column(name = "data_pagamento", updatable = false)
    private LocalDateTime dataPagamento;
}
```

A relação `@ManyToOne` aqui é com `PedidoPagamentoEntity`, não com o domain. Isso é proposital — a entity JPA fala com outras entities JPA.

### Passo 6 — Criar `adapters/out/persistence/mapper/PedidoPagamentoMapper.java`

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
                .telegramUserId(entity.getTelegramUserId())
                .telegramMessageId(entity.getTelegramMessageId())
                .fileIdTelegram(entity.getFileIdTelegram())
                .imagemUrl(entity.getImagemUrl())
                .valor(entity.getValor())
                .descricao(entity.getDescricao())
                .status(entity.getStatus())
                .dataCriacao(entity.getDataCriacao())
                .build();
    }

    public PedidoPagamentoEntity toEntity(PedidoPagamento domain) {
        if (domain == null) return null;
        PedidoPagamentoEntity entity = new PedidoPagamentoEntity();
        entity.setId(domain.getId());
        entity.setTelegramUserId(domain.getTelegramUserId());
        entity.setTelegramMessageId(domain.getTelegramMessageId());
        entity.setFileIdTelegram(domain.getFileIdTelegram());
        entity.setImagemUrl(domain.getImagemUrl());
        entity.setValor(domain.getValor());
        entity.setDescricao(domain.getDescricao());
        entity.setStatus(domain.getStatus());
        entity.setDataCriacao(domain.getDataCriacao());
        return entity;
    }
}
```

Não usar MapStruct nesta tarefa — manter mapper manual, é simples o suficiente e evita dependência nova.

### Passo 7 — Criar `adapters/out/persistence/mapper/ComprovanteMapper.java`

```java
package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.ComprovanteEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Comprovante;
import org.springframework.stereotype.Component;

@Component
public class ComprovanteMapper {

    public Comprovante toDomain(ComprovanteEntity entity) {
        if (entity == null) return null;
        return Comprovante.builder()
                .id(entity.getId())
                .pedidoId(entity.getPedido() != null ? entity.getPedido().getId() : null)
                .fileIdTelegram(entity.getFileIdTelegram())
                .imagemUrl(entity.getImagemUrl())
                .tipoPagamento(entity.getTipoPagamento())
                .dataPagamento(entity.getDataPagamento())
                .build();
    }

    /**
     * Converte domain -> entity. Como a entity tem relação @ManyToOne com PedidoPagamentoEntity,
     * o adapter precisará buscar/montar o PedidoPagamentoEntity antes de chamar toEntity,
     * OU chamar setEntityPedido(...) depois. Veja PedidoComprovanteRepositoryAdapter.
     */
    public ComprovanteEntity toEntity(Comprovante domain, PedidoPagamentoEntity pedidoEntity) {
        if (domain == null) return null;
        ComprovanteEntity entity = new ComprovanteEntity();
        entity.setId(domain.getId());
        entity.setPedido(pedidoEntity);
        entity.setFileIdTelegram(domain.getFileIdTelegram());
        entity.setImagemUrl(domain.getImagemUrl());
        entity.setTipoPagamento(domain.getTipoPagamento());
        entity.setDataPagamento(domain.getDataPagamento());
        return entity;
    }
}
```

### Passo 8 — Refatorar `PedidoPagamentoJpaRepository.java`

Agora estende `JpaRepository` da **entity**, não do domain:

```java
package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoPagamentoJpaRepository extends JpaRepository<PedidoPagamentoEntity, Long> {
}
```

### Passo 9 — Refatorar `ComprovanteJpaRepository.java`

```java
package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.ComprovanteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComprovanteJpaRepository extends JpaRepository<ComprovanteEntity, Long> {
}
```

### Passo 10 — Refatorar `PedidoPagamentoRepositoryAdapter.java`

A implementação do port limpo agora usa o mapper:

```java
package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper.PedidoPagamentoMapper;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PedidoPagamentoRepositoryAdapter implements PedidoPagamentoRepositoryPort {

    private final PedidoPagamentoJpaRepository jpaRepository;
    private final PedidoPagamentoMapper mapper;

    public PedidoPagamentoRepositoryAdapter(
            PedidoPagamentoJpaRepository jpaRepository,
            PedidoPagamentoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PedidoPagamento save(PedidoPagamento pedido) {
        PedidoPagamentoEntity entity = mapper.toEntity(pedido);
        PedidoPagamentoEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PedidoPagamento> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }
}
```

### Passo 11 — Refatorar `ComprovanteRepositoryAdapter.java`

A complicação: pra montar a entity do comprovante, precisa do `PedidoPagamentoEntity` do banco (referência da relação `@ManyToOne`). O adapter resolve isso buscando o pedido pelo ID:

```java
package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.ComprovanteEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper.ComprovanteMapper;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.ComprovanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoEncontradoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Comprovante;
import org.springframework.stereotype.Component;

@Component
public class ComprovanteRepositoryAdapter implements ComprovanteRepositoryPort {

    private final ComprovanteJpaRepository jpaRepository;
    private final PedidoPagamentoJpaRepository pedidoJpaRepository;
    private final ComprovanteMapper mapper;

    public ComprovanteRepositoryAdapter(
            ComprovanteJpaRepository jpaRepository,
            PedidoPagamentoJpaRepository pedidoJpaRepository,
            ComprovanteMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.pedidoJpaRepository = pedidoJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Comprovante save(Comprovante comprovante) {
        PedidoPagamentoEntity pedidoEntity = pedidoJpaRepository.findById(comprovante.getPedidoId())
                .orElseThrow(() -> new PedidoNaoEncontradoException(
                        "Pedido " + comprovante.getPedidoId() + " não encontrado ao salvar comprovante"));
        ComprovanteEntity entity = mapper.toEntity(comprovante, pedidoEntity);
        ComprovanteEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
```

(Ajustar a assinatura de acordo com o contrato do `ComprovanteRepositoryPort` atual. Se ele tiver outros métodos, replicar a lógica.)

### Passo 12 — Apagar os arquivos antigos

```
git rm src/main/java/.../domain/entity/PedidoPagamento.java
git rm src/main/java/.../domain/entity/Comprovante.java
git rm src/main/java/.../application/port/out/PedidoPagamentoRepository.java
git rm src/main/java/.../application/port/out/ComprovanteRepository.java
```

A pasta `domain/entity/` deve ficar vazia. Se for o caso, removê-la também.

### Passo 13 — Atualizar imports e usos no projeto inteiro

Usar **Find Usages** ou **Find in Path** do IntelliJ pra localizar todos os arquivos que:

1. Importam `domain.entity.PedidoPagamento` → trocar pra `domain.model.PedidoPagamento`
2. Importam `domain.entity.Comprovante` → trocar pra `domain.model.Comprovante`
3. Injetam `PedidoPagamentoRepository` (o que extends JpaRepository) → trocar pra `PedidoPagamentoRepositoryPort`
4. Injetam `ComprovanteRepository` (o que extends JpaRepository) → trocar pra `ComprovanteRepositoryPort`

Suspeitos prováveis de impacto (verificar e ajustar):

- `application/services/SalvarPedidoPagamentoServiceImpl.java`
- `application/services/RegistrarComprovanteServiceImpl.java`
- `application/usecases/SalvarPedidoPagamentoUsecase.java` (pode ser interface — verificar assinatura)
- `application/usecases/RegistrarComprovanteUsecase.java` (idem)
- `adapters/in/telegram/strategy/PaymentRequestStrategy.java`
- `adapters/in/telegram/strategy/PaymentProofStrategy.java`

Em qualquer arquivo que cria `new PedidoPagamento()` ou `new Comprovante()`, **a API agora usa o builder do Lombok**: `PedidoPagamento.builder().valor(...).build()`. Setters também continuam disponíveis.

### Passo 14 — Compilar e testar

1. `./mvnw clean compile` — não pode haver erro de compilação
2. `./mvnw test` — testes existentes (smoke test) devem passar
3. Subir aplicação em dev: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
4. Aplicação sobe sem erro
5. Smoke test: enviar via Telegram um pedido com foto e legenda `100.00 Teste BE-00B`, confirmar que registra
6. Smoke test: enviar comprovante com foto e legenda `#<id> pix`, confirmar que registra

Se qualquer um desses falhar, **parar e relatar**. Não improvisar fixes que mudam comportamento — provavelmente é um caso que o refactor deixou passar e precisa ajuste cirúrgico.

---

## Critério de aceitação

- [ ] `domain/model/PedidoPagamento.java` existe como POJO puro, sem nenhuma anotação `jakarta.persistence.*` ou `org.hibernate.*`
- [ ] `domain/model/Comprovante.java` existe como POJO puro com `Long pedidoId` (não a entidade inteira)
- [ ] `adapters/out/persistence/entity/PedidoPagamentoEntity.java` e `ComprovanteEntity.java` existem com as anotações JPA
- [ ] `adapters/out/persistence/mapper/PedidoPagamentoMapper.java` e `ComprovanteMapper.java` existem
- [ ] `domain/entity/` foi removida (pasta vazia ou deletada)
- [ ] `PedidoPagamentoRepository.java` e `ComprovanteRepository.java` (os que estendiam `JpaRepository`) foram removidos
- [ ] `PedidoPagamentoRepositoryPort` e `ComprovanteRepositoryPort` são os únicos ports do tipo
- [ ] Todos os services e use cases compilam contra os ports limpos
- [ ] `./mvnw clean compile` passa sem erros
- [ ] `./mvnw test` passa
- [ ] Aplicação sobe em dev sem erro
- [ ] Smoke test: registrar pedido via Telegram funciona
- [ ] Smoke test: registrar comprovante via Telegram funciona
- [ ] Nenhum arquivo Java fora de `adapters/out/persistence/` importa `jakarta.persistence.*` (exceto enums de `EnumType` se aplicável — mas idealmente isolado)

Verificação rápida do último item:

```
grep -rl "jakarta.persistence" src/main/java/ | grep -v "adapters/out/persistence"
```

(Esperado: nenhum resultado.)

---

## Fora de escopo desta tarefa

- **Não migrar** `tipo_pagamento` de String pra enum. Será tarefa separada (parte da BE-01).
- **Não adicionar** métodos de negócio nos novos POJOs (`marcarComoPago()`, `cancelar()`, etc). Esta tarefa só reorganiza estrutura.
- **Não usar** MapStruct, Lombok `@Builder.Default`, ou outras ferramentas além do que já está no `pom.xml`.
- **Não criar** novos testes. Os existentes devem continuar funcionando, é o suficiente pra essa tarefa.
- **Não tocar** em código que não tenha import do tipo afetado. Se um arquivo não usa `domain.entity.*` nem nenhum dos ports antigos, não mexe.
- **Não fazer** mudanças "de oportunidade" enquanto estiver lá (rename de variável, formatação, etc). Diff cirúrgico.

---

## Reportar status

Ao terminar, criar `docs/status/BE-00B-refatorar-persistencia.md` seguindo `docs/status/_TEMPLATE.md`. Cobrir:

- Confirmação de cada item do critério de aceitação
- Lista exata de arquivos modificados (com Find Usages)
- Output do `grep -rl "jakarta.persistence" src/main/java/ | grep -v "adapters/out/persistence"` — esperado: vazio
- Resultado dos smoke tests via Telegram (se passaram, idealmente com IDs dos pedidos criados)
- Qualquer comportamento inesperado que apareceu no caminho

**Próximo passo após esta tarefa:** BE-01 da Fase 3 (criar migration V2 com `requisitante`, datas, categoria, `auth_token`).
