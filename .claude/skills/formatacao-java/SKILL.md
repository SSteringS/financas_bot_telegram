---
name: formatacao-java
description: >
  Convenções de formatação e estilo Java do projeto — naming por camada, ordenação de
  imports e anotações, uso de records para DTOs, quando usar var, Javadoc mínimo.
  Carregar quando a task cria classes Java novas ou quando Reviewer detecta inconsistência
  de estilo em código backend.
load_pattern: contextual
used_by: [backend]
created: 2026-05-30
adr: 0015
status: ativa
---

# Skill — Formatação e Convenções Java

## Quando carregar (gatilho explícito)

- Task cria **≥1 classe Java nova** — garantir que naming e estrutura seguem o padrão.
- **Reviewer** detecta inconsistência de estilo (drift de naming, imports bagunçados, anotação
  fora de ordem) — carregar pra justificar o feedback com regra concreta.
- Aparece no contexto: nova porta, adapter, DTO, record, `@RestController`, Javadoc.

## Resumo da capacidade

Uniformiza o estilo do código backend sem depender de linter externo configurado.
As regras são suficientes pro agente escrever código consistente com o que já existe.

## Naming por camada

| Camada (hexagonal) | Padrão de nome | Exemplos |
|---|---|---|
| Domain (entidades, VOs) | Substantivo direto, sem sufixo | `Pedido`, `Valor`, `TipoMensagem` |
| Application (portas de entrada) | `*UseCase` ou `*Service` | `ListarPedidosUseCase`, `PedidoService` |
| Application (portas de saída — interface) | `*Repository`, `*Gateway` | `PedidoRepository`, `StorageGateway` |
| Adapters in (controller) | `*Controller` | `PedidoController` |
| Adapters out (persistência) | `*RepositoryAdapter`, `*JdbcRepository` | `PedidoRepositoryAdapter` |
| Adapters out (integrações externas) | `*Client`, `*Adapter` | `TelegramClient`, `S3StorageAdapter` |
| DTOs (request/response) | `*Request`, `*Response`, `*Dto` | `PedidoRequest`, `ResumoResponse` |
| Config | `*Config`, `*Configuration` | `SecurityConfig`, `S3Configuration` |

**Regra anti-drift:** nome deve revelar camada. `PedidoService` que contém JDBC diretamente é
sinal de vazamento de camada — não é problema de naming, é problema arquitetural.

## Naming de membros

- **Classes:** `PascalCase`.
- **Métodos e variáveis:** `camelCase`. Evitar abreviações obscuras (`qtd` ok; `q` não).
- **Constantes:** `SCREAMING_SNAKE_CASE` em `static final`.
- **Booleans:** prefixo `is*`/`has*`/`can*` (ex.: `isAtivo`, `hasPermissao`).
- **Coleções:** nome no plural ou com sufixo `List`/`Map` (ex.: `pedidos`, `pedidosPorId`).

## Ordenação de imports

Sem wildcard imports. Ordem:

1. `static` imports (agrupados)
2. `java.*`
3. `javax.*` / `jakarta.*`
4. `org.*`
5. `com.*` (terceiros, ex: `com.fasterxml`, `com.amazonaws`)
6. Pacote interno do projeto

Linha em branco entre cada grupo. O IDE (IntelliJ) configura via _Code Style → Java → Imports_.

## Ordenação de anotações (nas classes)

```java
@Component          // ou @Service, @Repository — identidade do bean
@RequiredArgsConstructor  // Lombok — construtor (logo abaixo da identidade)
@Slf4j              // Lombok — logger
public class MeuServico { ... }
```

Nos métodos:
```java
@Override           // JDK — sempre primeiro
@Transactional      // Spring — logo depois
@GetMapping("/rota") // Spring MVC — depois dos comportamentais
public RetornoDto metodo() { ... }
```

## Records para DTOs

Preferir `record` para DTOs imutáveis (Java 16+):

```java
// Preferir:
public record PedidoRequest(String descricao, BigDecimal valor) {}

// Em vez de:
public class PedidoRequest {
    private final String descricao;
    private final BigDecimal valor;
    // constructor, getters...
}
```

**Exceção:** se o DTO precisa de validação customizada com `@AssertTrue` ou herança → usar classe.
Jackson desserializa records normalmente com `@JsonProperty` ou `@JsonCreator` quando necessário.

## Uso de `var`

```java
// OK — tipo óbvio pelo lado direito
var pedidos = pedidoRepository.findAll();
var response = new PedidoResponse(pedido.getId(), pedido.getDescricao());

// NÃO usar — tipo não-óbvio
var x = metodoObscuro();   // o que retorna?
var resultado = map.get(chave);  // ambíguo
```

**Regra:** `var` só quando o tipo do lado direito é visível sem scroll ou lookup.
Nunca em parâmetros de método ou campos de classe.

## Javadoc — quando e o que

**Obrigatório:**
- Interfaces de **porta** (ex.: `PedidoRepository`, `StorageGateway`) — contrato que adapters implementam.
- Classes de **domínio** com regras de negócio não-triviais.

**Dispensável (não comentar o óbvio):**
- Getters/setters triviais.
- Controllers com `@Operation` do Springdoc (documentação já está no OpenAPI).
- Implementações de adapter — a interface já documenta.

```java
/**
 * Porta de saída para persistência de pedidos.
 * Implementações concretas vivem em adapters/out/persistence.
 */
public interface PedidoRepository {
    List<Pedido> findByMes(int mes, int ano);
    Pedido save(Pedido pedido);
}
```

## Checklist antes do commit

- [ ] Nomes de classes revelam camada (sem `ServiceImpl`, sem `Manager` genérico).
- [ ] Sem wildcard imports (`import java.util.*`).
- [ ] Anotações na ordem: identidade → comportamental → mapeamento.
- [ ] DTOs novos usam `record` salvo exceção documentada.
- [ ] `var` só quando tipo visível no lado direito.
- [ ] Javadoc em interfaces de porta; sem Javadoc em implementações óbvias.

## Ler junto

- Skill `arquitetura-hexagonal` — naming por camada deriva da estrutura de camadas.
- Skill `padroes-qualidade-codigo` — SOLID e patterns complementam o estilo.
