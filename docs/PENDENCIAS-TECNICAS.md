# Pendências técnicas

Lista de débitos técnicos conhecidos — coisas que **funcionam hoje** mas têm espaço pra melhoria, ajuste ou refactor. Não são bugs nem features; são itens de qualidade/manutenibilidade que valem revisitar quando houver folga ou quando o problema correlato aparecer.

Não confundir com `docs/plans/` (planos de tarefa ativos) nem com a seção "Fase 3d — Evolução pós-MVP" do FASE-3 (features futuras). Aqui é **dívida acumulada**, não evolução.

## Como usar

- Quando bater num assunto que vira pendência, adicionar item aqui com: descrição, contexto, fix sugerido, prioridade
- Quando decidir tratar um item, promover pra um plano em `docs/plans/BE-XX-*.md`
- Marcar item como `~~resolvido~~` (ou apagar) quando o plano correspondente for mergeado

---

## Itens abertos




### Substituir cert self-signed por Let's Encrypt + domínio real

**Contexto:** hoje o bot usa cert auto-assinado em `/opt/finbot/keystore.p12`, e o webhook do Telegram precisa do cert registrado via `setWebhook` com parâmetro `certificate=@cert.pem`. Toda vez que alguém roda `setWebhook` sem o parâmetro, o cert "desregistra" e o webhook quebra silenciosamente (já aconteceu).

**Fix sugerido:**
1. Registrar domínio próprio (pendência da Fase 3 — DEP-00)
2. Apontar `bot.<dominio>.com.br` pro Elastic IP via Route 53
3. Configurar Let's Encrypt via certbot na EC2 OU usar ALB com cert ACM
4. Atualizar webhook pra `https://bot.<dominio>.com.br/webhook` (sem precisar uploadar cert)

**Esforço:** médio (~meio dia de trabalho de infra).

**Prioridade:** média. Vale fazer quando o domínio for definido (resolve pendência DEP-00 também).

---

### `telegram.allowed-user-ids` hardcoded em prod

**Contexto:** lista de usuários autorizados está em `application-prod.properties` como string. Adicionar/remover usuário exige novo deploy.

**Fix sugerido:** mover pra tabela ou pra Secrets Manager. Eventualmente, pra um endpoint admin que gerencia.

**Esforço:** médio.

**Prioridade:** baixa. Só vira problema quando aparecer 2º usuário do bot (multi-requisitante real). Por enquanto é 1 user.

---

### Rotação do token do Telegram

**Contexto:** durante o incidente de SSL/webhook, o token completo do bot foi colado no chat com o Claude. Mesmo o canal sendo razoavelmente seguro, token vazado é token vazado.

**Fix sugerido:** rotacionar no BotFather (`/revoke` → `/token`), atualizar `finbot-prod-secrets` com o novo, restart do `finbot.service`.

**Esforço:** baixo (~5 min).

**Prioridade:** alta. Fazer quando puder.

---

### Comprovante mal-formado (sem `#`) é classificado como pedido novo

**Contexto:** o webhook do bot recebe todas as mensagens num único endpoint e decide entre `PaymentRequestStrategy` e `PaymentProofStrategy` puramente pelo formato da legenda:

- `<valor> <descrição>` (regex `^(\d+([.,]\d{1,2})?)\s+(.+)$`) → pedido
- `#<id> <tipo>` (regex `#(\d+)\s+(.+)`) → comprovante

Se o usuário enviar foto de comprovante com legenda `123 pix` (esquecendo o `#`), o sistema **não dispara** a mensagem de erro descritiva prevista pra "comprovante mal-formado". A legenda cai no matcher do `PaymentRequestStrategy` (que aceita "número + texto"), e o bot salva um pedido novo com `valor=123` e `descricao="pix"`. Identificado no teste 4.4 do roteiro de validação manual do `feature/backend-polish-evo07`.

**Por que não corrigir agora:** comportamento é consequência do design atual de despacho por regex, está estável, e o único usuário do bot hoje já está treinado pra usar `#`. Não atrapalha no curto prazo.

**Fix sugerido (a decidir quando virar problema):**
- Detectar legendas que "parecem comprovante mal-formado" antes de cair no `PaymentRequestStrategy` (ex: começa com `#` mas não bate o regex completo) e responder com mensagem orientadora; ou
- Migrar a UI do bot pra comandos explícitos (`/pedido`, `/comprovante`) em vez de inferir por formato; ou
- Confirmar o tipo com botão inline antes de salvar quando a legenda for ambígua.

**Esforço:** médio. Decidir abordagem antes de implementar.

**Prioridade:** baixa. Vira problema relevante quando o bot expandir pra mais de um usuário ou quando a usabilidade do pai precisar de mais robustez.

---


## Itens resolvidos

### ~~Esconder `@RequisitanteId` do Swagger UI~~

Resolvido em `feature/backend-polish-evo07` (commit `fix(FIX-hide-requisitanteid-swagger)`). Adicionado bloco `static { SpringDocUtils.getConfig().addAnnotationsToIgnore(RequisitanteId.class); }` em `OpenApiConfig.java`.

### ~~Escopo dos `@RestControllerAdvice` (BE-15b)~~

Resolvido em `feature/backend-polish-evo07` (commit `fix(BE-15b)`). `GlobalTelegramExceptionHandler` migrado de `@ControllerAdvice` para `@RestControllerAdvice(basePackages = "...adapters.in.telegram")`. `RestExceptionHandler` já tinha `basePackages` correto desde a BE-11.

### ~~`server.ssl.key-store-password` hardcoded em `application-prod.properties`~~

Resolvido em `feature/backend-polish-evo07` (commit `fix(FIX-keystore-password-secret)`). Trocado `finbot123` por `${keystore_password}`. **Ação manual obrigatória do humano antes do próximo deploy:** adicionar chave `keystore_password` com valor `finbot123` no segredo `finbot-prod-secrets` no AWS Secrets Manager.

### ~~Revisar mensagens de erro/ajuda do bot~~

Resolvido em `feature/backend-polish-evo07` (commit `fix(FIX-revisar-msgs-erro-bot)`). `PaymentRequestStrategy.parsePedido()` agora lança `InvalidMessageFormatException` (em vez de `IllegalArgumentException`) com mensagem didática e exemplos de todos os tipos. Mensagens de `InvalidCaptionException` em `PaymentProofStrategy` também atualizadas com exemplos. Mensagem de sucesso do pedido inclui o tipo detectado e dica quando OUTRO.
