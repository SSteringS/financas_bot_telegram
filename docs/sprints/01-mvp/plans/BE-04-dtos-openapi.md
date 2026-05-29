# BE-04 — DTOs da API REST + anotações OpenAPI (springdoc)

> Primeira tarefa do bloco da API de consulta. Não introduz endpoints — só prepara os contratos (DTOs com anotações Swagger/OpenAPI) que serão consumidos pelas próximas tarefas (BE-05 a BE-09 e BE-10 a BE-13). Destrava todo o resto.

---

## Contexto

A Fase 3 do projeto envolve construir uma API REST de leitura pra o front-end (variante C de design) consumir, mais auth via link mágico. Antes de escrever os endpoints, é necessário declarar os DTOs (Data Transfer Objects) que entram e saem da API, com anotações OpenAPI/Swagger pra que o `springdoc-openapi-starter-webmvc-ui` (já no `pom.xml`) gere o contrato automaticamente.

A vantagem dessa abordagem (em vez de escrever um YAML manual) é que **o contrato é gerado a partir do código** — impossível ficar fora de sincronia. Anotações ricas (`@Schema`, `@Parameter`, `@Operation` com descrições, exemplos, validações) viram um Swagger UI em `/swagger-ui.html` que o front pode explorar pra entender exatamente o que esperar de cada endpoint.

**Esta tarefa entrega só os DTOs anotados.** Endpoints, services e auth vêm depois.

---

## Pré-requisitos

- BE-00, BE-00B, BE-01a, BE-01 e hotfixes correlatos já mergeados em develop ✓
- `springdoc-openapi-starter-webmvc-ui` 2.5.0 já presente no `pom.xml` ✓
- Domínio model atualizado com novos campos (`requisitanteId`, `dataPedido`, `dataPagamento`, `tipo`) ✓

Nenhum bloqueio. Pode começar.

---

## Objetivo

Ao final desta tarefa:

1. Existem DTOs cobrindo as 4 áreas de contrato da API: **pedidos** (listagem e detalhe), **resumo do mês**, **auth** (exchange + me), e **erro padrão**
2. Cada DTO tem anotações `@Schema` em campos e classe, com descrições em português
3. Swagger UI acessível localmente em `http://localhost:8080/swagger-ui.html` mostrando os DTOs definidos (mesmo sem endpoints ainda)
4. Testes unitários cobrindo serialização/deserialização JSON dos DTOs principais
5. Nenhum endpoint foi criado nesta tarefa (controllers vêm em BE-05)
6. Configuração do springdoc ajustada (caminho do Swagger UI, agrupamento, info básica do projeto)

---

## Decisões técnicas pré-definidas

- **Records Java**, não classes com getters/setters. Boilerplate mínimo, imutáveis, suportados nativamente por Spring 3.x e Jackson. Lombok fica de fora destes arquivos específicos.
- **Bean Validation** (`@NotNull`, `@Size`, etc) em DTOs de **entrada** (requests). DTOs de saída não precisam de validação.
- **Datas como `LocalDate`** (não `LocalDateTime`) em campos como `dataPedido` — alinha com o schema do banco e com o front (que vai filtrar por dia).
- **Valores monetários como `BigDecimal`**, com `@Schema(example = "150.50")`.
- **Wrapper de paginação genérico** (`PaginaDTO<T>`) — uma única definição usada em qualquer listagem.
- **Formato de erro padronizado** (`ErroDTO`) com `codigo` (snake_case ou kebab) e `mensagem`.

---

## Arquivos esperados

Todos em `financas_bot_telegram/src/main/java/br/com/satyan/stering/saita/financasbottelegram/application/dto/`:

**Novos:**
- `PedidoResumoDTO.java`
- `PedidoDetalheDTO.java`
- `ResumoMesDTO.java`
- `ResumoStatusDTO.java` (nested: quantidade + total)
- `PaginaDTO.java` (genérico)
- `AuthExchangeRequest.java`
- `AuthMeResponse.java`
- `RequisitanteDTO.java`
- `ErroDTO.java`

**Modificados:**
- `application.properties` (config do springdoc)

**Pode ser apagado:**
- `PaymentCategory.java` (classe vazia — finalmente pode sair, agora que tem `TipoPagamento` enum cobrindo o conceito)

---

## Passos de execução

### Passo 0 — Apagar `PaymentCategory.java` vazio

Está em `application/dto/PaymentCategory.java`, classe vazia, sem uso real. `TipoPagamento` (criado no hotfix anterior) cobre o conceito de tipo de pagamento. Apagar:

```bash
git rm financas_bot_telegram/src/main/java/br/com/satyan/stering/saita/financasbottelegram/application/dto/PaymentCategory.java
```

Verificar com `grep` que ninguém importa essa classe antes de apagar.

### Passo 1 — Criar `PaginaDTO<T>` (wrapper genérico de paginação)

```java
package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Wrapper genérico de página com paginação")
public record PaginaDTO<T>(
        @Schema(description = "Itens da página atual") List<T> items,
        @Schema(description = "Total de itens encontrados (em todas as páginas)", example = "142") long total,
        @Schema(description = "Página atual (0-indexed)", example = "0") int pagina,
        @Schema(description = "Tamanho de página solicitado", example = "20") int tamanho,
        @Schema(description = "Total de páginas", example = "8") int totalPaginas
) {}
```

### Passo 2 — Criar `PedidoResumoDTO` (item da listagem)

```java
package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Resumo de um pedido pra listagem")
public record PedidoResumoDTO(
        @Schema(description = "ID único do pedido", example = "142") Long id,
        @Schema(description = "Valor em reais", example = "287.50") BigDecimal valor,
        @Schema(description = "Descrição livre do pedido", example = "Boleto energia") String descricao,
        @Schema(description = "Tipo de pagamento") TipoPagamento tipo,
        @Schema(description = "Status atual do pedido") StatusPedido status,
        @Schema(description = "Data em que o pedido foi solicitado", example = "2026-05-03") LocalDate dataPedido,
        @Schema(description = "Data em que o pagamento foi efetuado (null se PENDENTE)", example = "2026-05-04", nullable = true) LocalDate dataPagamento,
        @Schema(description = "Existe um comprovante anexado a este pedido?", example = "true") boolean temComprovante
) {}
```

### Passo 3 — Criar `PedidoDetalheDTO` (idêntico ao resumo por enquanto — separação preparada pra evolução)

```java
package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Detalhes completos de um pedido")
public record PedidoDetalheDTO(
        @Schema(example = "142") Long id,
        @Schema(example = "287.50") BigDecimal valor,
        @Schema(example = "Boleto energia") String descricao,
        TipoPagamento tipo,
        StatusPedido status,
        @Schema(example = "2026-05-03") LocalDate dataPedido,
        @Schema(example = "2026-05-04", nullable = true) LocalDate dataPagamento,
        @Schema(description = "Existe comprovante?") boolean temComprovante
) {}
```

Notar que `PedidoResumoDTO` e `PedidoDetalheDTO` têm os mesmos campos hoje, mas mantemos separados porque o detalhe pode vir a expor campos extras (auditoria, observações, etc) no futuro sem mexer no contrato da listagem.

### Passo 4 — Criar `ResumoStatusDTO` (auxiliar) e `ResumoMesDTO`

```java
package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Agregado de pedidos por status (quantidade + total)")
public record ResumoStatusDTO(
        @Schema(example = "3") int quantidade,
        @Schema(example = "7230.00") BigDecimal total
) {}
```

```java
package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo do mês atual: contagem e total de pendentes e pagos")
public record ResumoMesDTO(
        @Schema(description = "Mês de referência no formato YYYY-MM", example = "2026-05") String mesAtual,
        ResumoStatusDTO pendentes,
        ResumoStatusDTO pagos
) {}
```

### Passo 5 — Criar DTOs de auth

```java
package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Token de uso único recebido via link mágico")
public record AuthExchangeRequest(
        @NotBlank
        @Size(min = 16, max = 128)
        @Schema(description = "Token gerado pelo admin, recebido como query param ?t=", example = "ABCdef123...")
        String token
) {}
```

```java
package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados do requisitante autenticado")
public record RequisitanteDTO(
        @Schema(example = "1") Long id,
        @Schema(example = "Pedro Marques") String nome
) {}
```

```java
package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta dos endpoints de autenticação (/auth/exchange e /auth/me)")
public record AuthMeResponse(
        RequisitanteDTO requisitante
) {}
```

### Passo 6 — Criar `ErroDTO` (formato padrão de erro)

```java
package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estrutura padrão de erro retornada por endpoints REST")
public record ErroDTO(
        @Schema(description = "Código identificador do erro", example = "PEDIDO_NAO_ENCONTRADO") String codigo,
        @Schema(description = "Mensagem amigável", example = "Pedido com id 999 não foi encontrado") String mensagem
) {}
```

Observação: o envelope é o objeto raiz `{ "codigo": "...", "mensagem": "..." }` — diferente da especificação inicial que sugeria `{ "erro": { ... } }`. Simplificado pra reduzir aninhamento. Quando aparecer necessidade real de mais campos (timestamp, requestId, etc), evoluir.

### Passo 7 — Configurar springdoc no `application.properties`

Adicionar ao final de `application.properties`:

```properties
# springdoc-openapi
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operations-sorter=method
springdoc.swagger-ui.tags-sorter=alpha
springdoc.swagger-ui.tryItOutEnabled=true
springdoc.show-actuator=false
```

E em `application-prod.properties`, **desabilitar o Swagger UI em produção** (evita expor a API publicamente):

```properties
springdoc.swagger-ui.enabled=false
springdoc.api-docs.enabled=false
```

Em dev fica habilitado pra explorar.

### Passo 8 — Configurar metadados básicos do OpenAPI

Criar `infra/OpenApiConfig.java`:

```java
package br.com.satyan.stering.saita.financasbottelegram.infra;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finbot API")
                        .version("v1")
                        .description("API REST do bot de finanças — consulta de pedidos e comprovantes."));
    }
}
```

### Passo 9 — Testes unitários de serialização

Criar `application/dto/PedidoResumoDTOTest.java` (e similares pros DTOs principais):

```java
package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PedidoResumoDTOTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void deveSerializarParaJsonComCamposCamelCase() throws Exception {
        PedidoResumoDTO dto = new PedidoResumoDTO(
                142L,
                new BigDecimal("287.50"),
                "Boleto energia",
                TipoPagamento.BOLETO,
                StatusPedido.PAGO,
                LocalDate.of(2026, 5, 3),
                LocalDate.of(2026, 5, 4),
                true
        );

        String json = mapper.writeValueAsString(dto);

        assertThat(json).contains("\"id\":142");
        assertThat(json).contains("\"valor\":287.50");
        assertThat(json).contains("\"dataPedido\":\"2026-05-03\"");
        assertThat(json).contains("\"dataPagamento\":\"2026-05-04\"");
        assertThat(json).contains("\"tipo\":\"BOLETO\"");
        assertThat(json).contains("\"status\":\"PAGO\"");
        assertThat(json).contains("\"temComprovante\":true");
    }

    @Test
    void deveSerializarComDataPagamentoNulaQuandoPendente() throws Exception {
        PedidoResumoDTO dto = new PedidoResumoDTO(
                1L, new BigDecimal("100.00"), "X", TipoPagamento.PIX,
                StatusPedido.PENDENTE, LocalDate.now(), null, false
        );
        String json = mapper.writeValueAsString(dto);
        assertThat(json).contains("\"dataPagamento\":null");
    }
}
```

Repetir padrão pra `PedidoDetalheDTOTest`, `ResumoMesDTOTest`, `AuthMeResponseTest`, `PaginaDTOTest` (este último vale testar a tipagem genérica), `ErroDTOTest`.

Pra `AuthExchangeRequestTest`, cobrir também validação Bean Validation:

```java
@Test
void deveRejeitarTokenEmBranco() {
    AuthExchangeRequest req = new AuthExchangeRequest("");
    Set<ConstraintViolation<AuthExchangeRequest>> violations = validator.validate(req);
    assertThat(violations).hasSize(2); // @NotBlank + @Size(min=16)
}
```

### Passo 10 — Validar localmente

```bash
cd financas_bot_telegram
./mvnw clean test
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Em outra aba ou navegador:

- Abrir `http://localhost:8080/swagger-ui.html` — esperado: página carrega, mostra a info "Finbot API v1", e na seção de Schemas tem todos os DTOs criados, com descrições e exemplos
- Abrir `http://localhost:8080/v3/api-docs` — esperado: JSON do OpenAPI 3 com os schemas definidos
- Confirmar que **nenhum endpoint** apareceu (Swagger só vai mostrar endpoints quando BE-05+ adicionarem controllers REST)

---

## Critério de aceitação

- [ ] `PaymentCategory.java` (classe vazia) foi apagado
- [ ] Existem records pra: `PedidoResumoDTO`, `PedidoDetalheDTO`, `ResumoMesDTO`, `ResumoStatusDTO`, `PaginaDTO<T>`, `AuthExchangeRequest`, `AuthMeResponse`, `RequisitanteDTO`, `ErroDTO`
- [ ] Cada DTO tem `@Schema` na classe e em campos relevantes, com descrição em português + exemplos
- [ ] `AuthExchangeRequest` tem Bean Validation (`@NotBlank`, `@Size`)
- [ ] `application.properties` tem config do springdoc; `application-prod.properties` desabilita Swagger UI/api-docs
- [ ] `infra/OpenApiConfig.java` define metadados (title, version, description)
- [ ] `./mvnw test` passa (testes existentes + novos de serialização)
- [ ] Localmente, Swagger UI em `/swagger-ui.html` carrega sem erro e mostra a seção Schemas com todos os DTOs
- [ ] `/v3/api-docs` retorna JSON válido do OpenAPI 3
- [ ] Nenhum endpoint REST novo foi criado (essa tarefa é só DTOs + config)
- [ ] Pelo menos 1 teste de serialização por DTO principal (não precisa exaustivo)

---

## Fora de escopo desta tarefa

- **Endpoints REST** (`@RestController`, `@GetMapping`, `@PostMapping`) — virão em BE-05 a BE-13
- **Services e usecases** pra alimentar os endpoints — idem
- **Auth real** (JWT, cookies, exchange) — BE-10 a BE-13
- **CORS** — BE-13
- **Domain entities pra Requisitante e AuthToken** — vão entrar quando os respectivos endpoints precisarem (BE-10+). Por ora, a coluna `requisitante_id` já está na tabela `pedidos_pagamento` (via V2), e o id `1L` é usado hardcoded pelo strategy.
- **Mappers domain ↔ DTO** — escopo dos services em BE-05 onwards. Esta tarefa só declara os DTOs.

---

## Reportar status

`docs/status/BE-04-dtos-openapi.md` seguindo `_TEMPLATE.md`. Cobrir:

- Output de `./mvnw test` (sumário com `Tests run: X, Failures: 0, Errors: 0, Skipped: 0`)
- Screenshot/cópia da resposta de `curl http://localhost:8080/v3/api-docs` (recortar pra mostrar a seção `components/schemas` com os DTOs)
- Lista de arquivos criados/modificados
- Próximo passo: **BE-05 (endpoint `GET /api/v1/pedidos`)** está liberada
- Próximo passo alternativo: **BE-10 (auth_token + endpoint admin)** também está liberada — podem rodar em paralelo se houver outra instância de implementador disponível
