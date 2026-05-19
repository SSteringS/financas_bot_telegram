# Records em Java — quando usar vs classe com Lombok

## Contexto da dúvida

Aparece no status report da BE-10 a frase "Usado `record` Java para `GerarConviteResponse` — zero boilerplate". A pergunta foi: o que é record, por que essa decisão e quando faz sentido.

## Resumo destilado

`record` é uma feature da linguagem Java (estável desde Java 16, projeto está em Java 21) pra declarar classes imutáveis de dados em uma única linha. O compilador gera automaticamente: construtor, acessores (com nome `campo()` em vez de `getCampo()`), `equals()`, `hashCode()`, `toString()`, todos os campos como `private final`.

```java
public record GerarConviteResponse(String url) {}
```

Versus o equivalente em classe + Lombok:

```java
@Data
@AllArgsConstructor
public class GerarConviteResponse {
    private final String url;
}
```

Os dois são curtos, mas record é **nativo da linguagem** — sem dependência externa, sem annotation processor, sem mágica de compile-time.

## Pontos-chave

- **Records são imutáveis por design** — não tem setter; campos são `final`. Perfeito pra DTOs.
- **Acessor é `campo()`, não `getCampo()`** — convenção de records. Jackson, Spring, springdoc entendem nativamente.
- **Bean Validation funciona** em records (`@NotBlank`, `@Size` em parâmetros).
- **Anotações de schema (springdoc) funcionam** em records (`@Schema(description="...")`).
- **Não pode ter herança** entre records (são `final` implicitamente). Use só quando a estrutura é "flat".
- **Use record quando**: DTOs, payloads, value objects, parâmetros agrupados. Quando o conteúdo é "dados, ponto".
- **Use classe (com ou sem Lombok) quando**: precisa de herança, lifecycle (callbacks), comportamento mutável, ou anotações JPA pesadas (`@Entity` não combina bem com record).

## Pra aprofundar

- Comparar com `data class` do Kotlin e `case class` do Scala — mesmo conceito, raízes diferentes
- Pattern matching em records (Java 21+) — `if (obj instanceof Point(int x, int y))`
- Quando usar `Compact Constructor` em record pra validação no construtor:
  ```java
  public record Cpf(String value) {
      public Cpf {
          if (!isValid(value)) throw new IllegalArgumentException();
      }
  }
  ```
