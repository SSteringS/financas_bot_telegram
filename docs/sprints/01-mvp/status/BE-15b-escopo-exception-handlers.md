# BE-15b — Restringir escopo dos `@RestControllerAdvice` por package

**Data:** 2026-05-13
**Branch:** feature/backend-fase3-api-completa
**Commit/PR:** (ver commit deste arquivo)
**Responsável (instância):** Claude Code (CLI)

---

## O que foi feito

- Trocado `@ControllerAdvice` por `@RestControllerAdvice(basePackages = "br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram")` no `GlobalTelegramExceptionHandler`.
- Atualizado import: removido `ControllerAdvice`, adicionado `RestControllerAdvice`.
- `RestExceptionHandler` (BE-11) verificado — já está com `basePackages = "...adapters.in.rest"` correto, nenhuma alteração necessária.
- Testes: `mvn clean test` — **192 testes, 0 falhas, 0 erros.** 17 pulados (Testcontainers sem Docker local, comportamento esperado e documentado).

---

## Desvios do plano

O plano assumia que o handler estava anotado com `@RestControllerAdvice` sem `basePackages`. O estado real era `@ControllerAdvice` (sem `basePackages`). O fix aplicado é equivalente ao planejado — a mudança para `@RestControllerAdvice` com `basePackages` é o estado correto em ambos os casos.

---

## Decisões tomadas durante a execução

Nenhuma decisão local relevante — o fix é exatamente o previsto no plano: 1 parâmetro adicionado na anotação + troca de import.

---

## Decisões pendentes (esperando humano)

Smoke tests manuais ficam com o humano:
1. Subir app em dev e acessar `/v3/api-docs` — log do `GlobalTelegramExceptionHandler` **não deve aparecer**.
2. Enviar mensagem no Telegram com legenda mal formatada — log do `GlobalTelegramExceptionHandler` **deve aparecer** (confirma que o escopo certo ainda funciona).

---

## Confirmação do `RestExceptionHandler.basePackages`

```java
@RestControllerAdvice(basePackages = "br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest")
public class RestExceptionHandler {
```

Correto. Nenhuma alteração necessária.

---

## Próximos passos / observações pro próximo

- Com os dois handlers corretamente escopados, exceções de bibliotecas de terceiros (springdoc, Spring Security, etc.) caem no handler default do Spring — comportamento adequado.
- Fica pendente apenas o smoke test manual citado acima.

---

## Arquivos criados/modificados

- `adapters/in/telegram/exceptionhandler/GlobalTelegramExceptionHandler.java` (modificado: `@ControllerAdvice` → `@RestControllerAdvice(basePackages = ...)`)
- `docs/status/BE-15b-escopo-exception-handlers.md` (novo)
