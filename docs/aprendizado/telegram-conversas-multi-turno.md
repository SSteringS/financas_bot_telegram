# Telegram: conversas multi-turno e o padrao CommandRouter

**Contexto da duvida:** no refinamento do EVO-09 (2026-05-31), avaliou-se se o cadastro de
vales deveria ser feito via bot Telegram (comandos `/vale`, `/fechar`) ou via front-end.
A discussao revelou a complexidade do gerenciamento de estado multi-turno no Telegram.

---

## Problema: o Telegram e stateless por natureza

Cada mensagem que o bot recebe chega como um evento independente via webhook. Nao ha sessao.
Para perguntas em sequencia ("qual o funcionario? qual o valor?"), o bot precisa lembrar
*onde* esta na conversa para cada usuario — isso e gerenciamento de estado multi-turno.

---

## Opcao A: gerenciamento de estado em memoria

```java
Map<Long, EstadoConversa> conversasAtivas = new ConcurrentHashMap<>();

// EstadoConversa:
//   etapa: AGUARDANDO_FUNCIONARIO | AGUARDANDO_VALOR | CONFIRMANDO
//   dadosParciais: FuncionarioId, valor, ...
//   timestamp: Instant  // para timeout
```

**Vantagens:** simples de implementar; sem schema de banco.

**Desvantagens:**
- Perde estado no restart da aplicacao (finbot.service reinicia em deploys).
- Precisa de logica de timeout (conversa abandonada ocupa memoria).
- Dificulta debugging (estado invisivel por fora).

---

## Opcao B: estado em banco (tabela `conversa_bot`)

```sql
CREATE TABLE conversa_bot (
  chat_id     BIGINT PRIMARY KEY,
  etapa       VARCHAR(50) NOT NULL,
  contexto    JSON,         -- dados parciais da conversa
  atualizado  DATETIME NOT NULL
);
```

**Vantagens:** sobrevive a restarts; auditavel.

**Desvantagens:** mais complexo; requer limpeza periodica de conversas abandonadas.

---

## CallbackQuery: o mecanismo de teclado inline

Quando o bot envia um teclado inline (botoes), o usuario clica e o Telegram envia
uma `CallbackQuery` (nao uma mensagem comum). O handler deve:

1. Chamar `answerCallbackQuery(callbackQueryId)` para remover o "carregando" do botao.
2. Ler `callbackQuery.getData()` para saber qual botao foi clicado.
3. Atualizar o estado da conversa.

O webhook atual so trata `Update` com `message` — seria necessario adicionar
tratamento de `callbackQuery` para suportar teclado inline.

---

## Padrao CommandRouter

Para escalar alem de um ou dois comandos, o padrao e:

```java
@Component
public class CommandRouter {
    private final Map<String, BotCommandHandler> handlers;

    public CommandRouter(List<BotCommandHandler> handlers) {
        this.handlers = handlers.stream()
            .collect(Collectors.toMap(BotCommandHandler::getCommand, h -> h));
    }

    public void route(Message message) {
        String text = message.getText();
        if (text != null && text.startsWith("/")) {
            String command = text.split(" ")[0].toLowerCase();
            BotCommandHandler handler = handlers.get(command);
            if (handler != null) {
                handler.handle(message);
            }
        }
    }
}
```

**Vantagem sobre regex:** cada comando e um bean Spring isolado; facil de testar;
nao quebra os demais ao adicionar novo comando; sem regex frágil.

**Coexistencia com estrategias de texto livre:** o `UpdateOrchestratorService` pode
chamar o `CommandRouter` primeiro (se a mensagem comecar com `/`), e so entrar nas
`UpdateProcessingStrategy` existentes se nao for comando.

---

## Por que o EVO-09 foi de front-end

Para o contexto do projeto (operacao domestica, volumes baixos, usuario ja usa o front),
a complexidade do multi-turno bot nao justificou o ganho. Decisao registrada em ADR 0016 §5.

A porta fica aberta: se futuramente um `/vale Maria 150` (comando + parametros inline,
sem multi-turno) for desejado, o `CommandRouter` e o caminho — sem estado, sem CallbackQuery,
implementacao em uma tarde.

---

## Pontos-chave

- Telegram e stateless: cada update e independente.
- Multi-turno = gerenciar estado (memoria ou banco) — nao e gratis.
- `CallbackQuery` e diferente de `Message` — o webhook atual nao trata.
- `CommandRouter` (Map<String, Handler>) e o padrao para escalar comandos `/`.
- Comando simples com parametros inline (`/vale Maria 150`) e a forma mais simples
  de cadastro via bot — sem multi-turno, sem CallbackQuery.

---

## Para aprofundar

- `UpdateOrchestratorService.java` — orquestrador atual de strategies.
- `PaymentRequestStrategy.java` e `PaymentProofStrategy.java` — exemplos de strategy de texto.
- Telegram Bot API: `sendMessage` com `InlineKeyboardMarkup` para teclados inline.
