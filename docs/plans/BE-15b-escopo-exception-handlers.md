# BE-15b — Restringir escopo dos `@RestControllerAdvice` por package

> Patch curto, complementar à BE-15. Hoje o `GlobalTelegramExceptionHandler` captura exceções de **qualquer endpoint do projeto** (incluindo `/v3/api-docs`, `/swagger-ui.html`, `/api/v1/pedidos`, etc), o que viola separation of concerns e gera logs confusos. Esta tarefa restringe os handlers aos packages corretos.

---

## Pré-requisitos

- Springdoc atualizado pra versão compatível com Spring Boot 3.4.x (ver issue separada — o usuário fez manualmente trocando `springdoc-openapi-starter-webmvc-ui` pra `2.7.0` ou superior)
- BE-15 mergeada (handler genérico de exceções existindo)

---

## Contexto

Após a BE-11 e BE-15, o projeto tem dois exception handlers globais:

1. **`GlobalTelegramExceptionHandler`** em `adapters/in/telegram/exceptionhandler/` — pensado pra capturar exceções do webhook do Telegram. Tem `@ExceptionHandler` pras exceções específicas do bot (`InvalidUpdateException`, `UnauthorizedUserException`, `PhotoProcessingException`, etc) **e** um fallback `@ExceptionHandler(Exception.class)` da BE-15.

2. **`RestExceptionHandler`** em `adapters/in/rest/` — criado na BE-11 pra capturar exceções dos endpoints REST de auth/pedidos. Já tem `basePackages = "...adapters.in.rest"` no `@RestControllerAdvice`, então só captura controllers REST.

**O problema:** `GlobalTelegramExceptionHandler` está anotado provavelmente só com `@RestControllerAdvice` (sem `basePackages`), o que significa **escopo global**. Resultado:

- Quando o springdoc gerou um erro (`NoSuchMethodError` por incompatibilidade de versão), o `GlobalTelegramExceptionHandler` é quem capturou — viu nos logs `f.a.i.t.e.GlobalTelegramExceptionHandler : Exceção não mapeada` num request pra `/v3/api-docs`
- Quando endpoints REST disparam algo inesperado (algo que o `RestExceptionHandler` não trata), provavelmente o handler do Telegram também pega antes
- Isso confunde análise de logs e pode mascarar bugs

**O fix:** adicionar `basePackages` no `@RestControllerAdvice` do `GlobalTelegramExceptionHandler`, restringindo ele a só capturar exceções de controllers dentro do package do adapter Telegram.

---

## Arquivos esperados

**Modificados:**
- `adapters/in/telegram/exceptionhandler/GlobalTelegramExceptionHandler.java` — adicionar `basePackages` na anotação

**Tests:**
- `GlobalTelegramExceptionHandlerTest` — pode precisar de ajuste se algum teste assumia escopo global (improvável, mas verificar)
- Possivelmente novo teste de integração leve confirmando que exceção de controller REST **não** passa pelo handler do Telegram

---

## Código-chave

### Antes (provável estado atual)

```java
@RestControllerAdvice
public class GlobalTelegramExceptionHandler {
    // ...
}
```

### Depois

```java
@RestControllerAdvice(basePackages = "br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram")
public class GlobalTelegramExceptionHandler {
    // ...
}
```

Sem mais nada no corpo. É uma adição de 1 parâmetro na anotação.

---

## Validação

1. **Build e testes:**

```bash
cd financas_bot_telegram
./mvnw clean test
```

Esperado: 100% verde. Nenhum teste novo precisa ser adicionado por essa mudança, exceto se for útil cobrir explicitamente o cenário "exceção de controller REST NÃO passa pelo handler do Telegram".

2. **Smoke test manual:**

Subir app em dev. Acessar `/v3/api-docs` (ou outro endpoint REST que dispare uma exceção qualquer). **No log do `GlobalTelegramExceptionHandler` NÃO deve aparecer "Exceção não mapeada".** Se aparecer, escopo não pegou.

3. **Smoke test do bot:** mandar mensagem pelo Telegram que dispara erro (ex: legenda mal formatada). **No log do `GlobalTelegramExceptionHandler` DEVE aparecer a captura** — confirma que ele continua funcionando pro escopo certo dele.

---

## Considerações secundárias (opcional, mas vale registrar)

- **Conferir se `RestExceptionHandler` está correto.** Olhar o `basePackages` da BE-11. Se for `"br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest"`, está OK. Se for outra coisa (ex: namespace antigo ou typo), ajustar.

- **Verificar se há outras anotações `@ControllerAdvice` ou `@RestControllerAdvice` no projeto.** Se houver mais alguma sem `basePackages`, aplicar o mesmo padrão.

- **O que acontece com exceções fora dos packages cobertos?** Elas caem no handler default do Spring, que retorna 500 (ou 404, 400, dependendo do tipo). Isso é o comportamento padrão e adequado pra erros inesperados de bibliotecas/framework.

---

## Critério de aceitação

- [ ] `GlobalTelegramExceptionHandler` tem `basePackages` apontando pro package do Telegram
- [ ] Subir app, acessar `/v3/api-docs` ou qualquer endpoint REST que dispare exceção, log do handler do Telegram **não** aparece
- [ ] Smoke test do bot continua funcionando — exceções do webhook caem no handler certo
- [ ] `./mvnw test` passa (incluindo qualquer teste antigo que cobria comportamento do handler)
- [ ] `RestExceptionHandler` (BE-11) está com `basePackages` correto — se não estiver, ajustar nesta mesma tarefa

---

## Fora de escopo

- Refatorar a hierarquia de exceptions do projeto
- Mudar o conteúdo de mensagens de erro
- Adicionar novos `@ExceptionHandler` em qualquer handler
- Mexer em `RestExceptionHandler` além de validar o `basePackages`

---

## Status report

`docs/status/BE-15b-escopo-exception-handlers.md` seguindo `_TEMPLATE.md`. Cobrir:

- Output de `mvn test`
- Smoke test: requisição pra `/v3/api-docs` (ou similar) **não** aparece nos logs do `GlobalTelegramExceptionHandler`
- Smoke test do bot: exceção do webhook ainda é capturada corretamente
- Confirmação visual do `RestExceptionHandler.basePackages` (cole o trecho da anotação)
