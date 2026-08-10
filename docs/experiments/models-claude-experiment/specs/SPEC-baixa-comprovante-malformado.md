# SPEC — Baixa — Comprovante mal-formado não vira pedido silencioso

> **Congelada em 2026-08-10.** Score de complexidade: **2**.
> Entrada idêntica para todos os runs. Não editar durante a execução do experimento.

## Objetivo

Quando o usuário manda uma foto com legenda que **tenta** ser um comprovante mas está mal-formada, o bot deve responder com erro orientativo em vez de salvar um pedido de pagamento novo.

## Contexto mínimo

O webhook do bot recebe todas as mensagens num endpoint só e escolhe entre duas strategies pelo formato da legenda:

- **Pedido:** `^(\d+([.,]\d{1,2})?)\s+(.+)$` — valor seguido de descrição
- **Comprovante:** `#(\d+)\s+(.+)` — `#` mais id do pedido, mais tipo

A escolha é `findFirst()` sobre a lista de strategies injetada — **a ordem não é determinística nem controlada explicitamente**.

Hoje, legenda `#abc pix` não casa com o regex de comprovante. Cai adiante e, dependendo do caso, é descartada ou classificada errado. O usuário não recebe orientação.

## Requisitos funcionais

1. Legenda que **começa com `#`** mas não satisfaz o regex de comprovante é classificada como **tentativa de comprovante mal-formada**.
2. Nesse caso o sistema responde ao usuário com mensagem de erro **orientativa**, explicando o formato esperado.
3. Nesse caso o sistema **não** cria pedido de pagamento e **não** cria comprovante.
4. Legenda que satisfaz o regex de comprovante continua funcionando exatamente como hoje.
5. Legenda que satisfaz o regex de pedido e **não** começa com `#` continua funcionando exatamente como hoje.
6. A detecção acontece **antes** de a legenda chegar na strategy de pedido.

## Critérios de aceitação

Cada item é verificável por teste automatizado.

1. Legenda `#abc pix` → erro orientativo; nenhum pedido e nenhum comprovante persistido.
2. Legenda `#` (sozinha) → erro orientativo; nada persistido.
3. Legenda `# 123 pix` (espaço depois do `#`) → erro orientativo; nada persistido.
4. Legenda `#123` (sem tipo) → erro orientativo; nada persistido.
5. Legenda `#123 pix` → comportamento atual preservado: comprovante registrado no pedido 123.
6. Legenda `123 pix` → comportamento atual preservado: pedido criado com valor 123 e descrição `pix`.
7. A mensagem de erro cita o formato correto de forma acionável para quem não conhece o sistema.
8. `mvn test` verde, sem regressão na suíte existente.

## Fora de escopo

- **Legenda sem `#` nenhum (ex.: `123 pix`).** É o sintoma original relatado, e é **deliberadamente não resolvido**: `123 pix` é indistinguível de um pedido legítimo de R$ 123 para "pix". Não existe sinal textual confiável. Resolver isso exigiria heurística de contexto (consultar se existe pedido 123 pendente), o que muda a natureza da feature.
- Migrar o bot para comandos explícitos (`/pedido`, `/comprovante`).
- Botão inline de confirmação — exige estado conversacional, que não existe no repositório.
- Tornar determinística a ordem das strategies.
- Qualquer mudança no canal WhatsApp.

## O que esta spec deliberadamente não decide

Espaço de trabalho do planner e do implementador:

- Onde a detecção mora — no parser de domínio, numa strategy nova, ou no serviço de aplicação.
- Se a regra vira regex, predicado, ou tipo de retorno novo.
- Como a mensagem de erro é modelada e onde o texto fica.
- Quebra em tasks, sequência e estratégia de teste.
- Redação exata da mensagem (o critério 7 avalia a qualidade, não o texto literal).

## Referências

- `docs/PENDENCIAS-TECNICAS.md` — item "Comprovante mal-formado (sem `#`) é classificado como pedido novo"
- Convenções do repositório: `03-features.md` §4
