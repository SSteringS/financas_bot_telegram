# Exception handlers no Spring — escopo via `basePackages`

## Contexto da dúvida

O Swagger UI quebrou com `NoSuchMethodError` do springdoc (incompatibilidade de versão Spring 6.2 vs springdoc 2.5). A pista interessante: o erro foi capturado pelo `GlobalTelegramExceptionHandler` mesmo sendo uma request pra `/v3/api-docs`, não pro webhook do Telegram. A pergunta foi: por que o handler do Telegram pegou um erro do springdoc, e como evitar isso.

Esse problema gerou o plano `BE-15b — Restringir escopo dos exception handlers`.

## Resumo destilado

`@RestControllerAdvice` (e `@ControllerAdvice`) sem parâmetros tem **escopo global**: captura exceções de QUALQUER controller no projeto. Não importa onde a classe do handler está; o que importa é a anotação.

Pra restringir o escopo, usar `basePackages`:

```java
// Captura exceções de QUALQUER controller (ruim quando você tem múltiplos contextos):
@RestControllerAdvice
public class GlobalTelegramExceptionHandler { ... }

// Captura só exceções de controllers em adapters.in.telegram:
@RestControllerAdvice(basePackages = "br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram")
public class GlobalTelegramExceptionHandler { ... }
```

Outras formas de restringir:
- `basePackageClasses = SomeClass.class` — alternativa com referência tipada (refactor-safe)
- `assignableTypes = SomeController.class` — só pra controllers específicos
- `annotations = SomeAnnotation.class` — só pra controllers com certa annotation

## Quando o problema aparece

Em projetos com **múltiplos "contextos" de entrada** (no nosso caso: webhook Telegram + API REST), você geralmente quer **handlers separados pra cada contexto** com mensagens, status codes e formatos de resposta apropriados. Sem `basePackages`, eles "vazam" entre contextos.

No nosso projeto:
- `GlobalTelegramExceptionHandler` → deveria capturar só exceções do webhook (pra mandar mensagem amigável pro chat do Telegram via `sendMessage`)
- `RestExceptionHandler` → captura só exceções dos endpoints REST (pra retornar JSON com `ErroDTO`)
- Exceções de outros lugares (springdoc, Spring Boot internals, etc) → caem no handler default do Spring, que retorna 500

## Pontos-chave

- **`@RestControllerAdvice` sem parâmetros = escopo global.** Pegada comum em projetos com múltiplos contextos.
- **Restringir com `basePackages`** = um handler por "domínio de entrada".
- **Handler com `@ExceptionHandler(Exception.class)` (fallback genérico)** vira o gargalo: captura tudo, inclusive coisas que NÃO deveria.
- **Sintoma de escopo errado:** logs estranhos — handler de um contexto reportando erros de outro contexto, request payload não-batendo com o esperado, mensagens vazias ou nulos onde deveria ter dado erro.
- **Múltiplos `@RestControllerAdvice`** podem coexistir. Spring escolhe o "mais específico" baseado em `Order` (default: ordem de detecção).
- **Hierarquia recomendada em projetos com múltiplos contextos:**
  1. Um handler específico por contexto (REST API, webhook Telegram, etc), com `basePackages`
  2. Exceções não-mapeadas caem no handler default do Spring (500)
  3. Evita "rede de pesca" global que captura tudo e mascara bugs

## Pra aprofundar

- `ResponseEntityExceptionHandler` (Spring) — base class pra handlers REST mais elaborados
- Ordem de handlers — `@Order` em `@ControllerAdvice` controla precedência
- Diferença entre `@ControllerAdvice` (returna view ou body) e `@RestControllerAdvice` (sempre body, equivalente a `@ControllerAdvice + @ResponseBody`)
- Como Spring resolve exceções: primeiro `@ExceptionHandler` no próprio controller, depois `@ControllerAdvice` matching, depois fallback default
