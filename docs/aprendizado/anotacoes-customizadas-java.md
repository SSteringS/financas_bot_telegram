# Anotações customizadas em Java — `@Target`, `@Retention`, `@interface`

## Contexto da dúvida

Na BE-12, o `RequisitanteId.java` foi declarado assim:

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequisitanteId {
}
```

A pergunta foi: o que cada uma dessas linhas faz e por que essa combinação.

## Resumo destilado

Você está **declarando uma annotation customizada chamada `@RequisitanteId`**. Cada linha cumpre um papel:

### `public @interface RequisitanteId {}`

A sintaxe `@interface` (com `@`) declara um **tipo de annotation**, não uma interface comum. Sem nada no corpo (`{}` vazio), é uma **marker annotation** — não carrega dados, só "marca" alguma coisa. Pra contrastar, uma annotation com dados teria:

```java
public @interface MaxLength {
    int value();
    String mensagem() default "muito longo";
}
```

### `@Target(ElementType.PARAMETER)`

Meta-annotation que **restringe onde sua annotation pode ser aplicada**. `PARAMETER` significa "só em parâmetros de método". Tentar usar em campo, classe ou método vira **erro de compilação**.

Opções comuns: `TYPE` (classes/interfaces), `FIELD`, `METHOD`, `PARAMETER`, `CONSTRUCTOR`, `LOCAL_VARIABLE`, `ANNOTATION_TYPE`, `TYPE_USE`, `RECORD_COMPONENT`.

### `@Retention(RetentionPolicy.RUNTIME)`

Meta-annotation que controla **quando a annotation existe no ciclo de vida do código**:

| Política | O que acontece | Quando usar |
|---|---|---|
| `SOURCE` | Compilador descarta. Não vai pro `.class` | Checks de compile-time (`@Override`, `@SuppressWarnings`) |
| `CLASS` | Vai pro `.class` mas JVM não carrega | Ferramentas que leem bytecode |
| `RUNTIME` | Acessível via reflection em runtime | **Frameworks** (Spring, Hibernate, Jackson) que leem dinamicamente |

Pra o Spring conseguir detectar a annotation via `parameter.hasParameterAnnotation(RequisitanteId.class)`, ela **precisa** ser `RUNTIME`. Com `SOURCE` ou `CLASS`, o Spring não veria.

## Pontos-chave

- **A annotation sozinha não faz nada.** É só uma etiqueta semântica. Quem dá significado é outro código (resolver, processor, validator) que LÊ a annotation via reflection.
- **No nosso caso:** `RequisitanteIdArgumentResolver` é quem lê. Ele detecta o parâmetro com `@RequisitanteId`, pega o valor do request attribute (que o filter colocou) e injeta como argumento do método.
- **Padrão idiomático Spring** quando precisa de injeção customizada de argumento, além das padrão (`@RequestParam`, `@PathVariable`, `@RequestBody`, etc).
- **Annotations com dados** (`@MaxLength(value=10)`) declaram "métodos" no `@interface`. O nome `value` é especial — permite sintaxe curta `@MaxLength(10)` sem precisar dizer `value=`.
- **Annotations podem ter outras annotations como atributo**:
  ```java
  public @interface Combo {
      Author author();  // outra annotation
  }
  ```
  Útil pra annotations compostas.

## Pra aprofundar

- Reflection em Java — `Method.getParameterAnnotations()`, `Class.getDeclaredAnnotations()`
- `@Inherited` — meta-annotation que faz annotations serem herdadas por subclasses
- `@Repeatable` (Java 8+) — permite a mesma annotation aparecer múltiplas vezes
- Annotation Processing API — processar annotations em compile-time (Lombok, MapStruct usam isso)
- Como Spring registra `HandlerMethodArgumentResolver` e em que ordem executa
