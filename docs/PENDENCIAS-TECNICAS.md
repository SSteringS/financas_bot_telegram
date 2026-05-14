# Pendências técnicas

Lista de débitos técnicos conhecidos — coisas que **funcionam hoje** mas têm espaço pra melhoria, ajuste ou refactor. Não são bugs nem features; são itens de qualidade/manutenibilidade que valem revisitar quando houver folga ou quando o problema correlato aparecer.

Não confundir com `docs/plans/` (planos de tarefa ativos) nem com a seção "Fase 3d — Evolução pós-MVP" do FASE-3 (features futuras). Aqui é **dívida acumulada**, não evolução.

## Como usar

- Quando bater num assunto que vira pendência, adicionar item aqui com: descrição, contexto, fix sugerido, prioridade
- Quando decidir tratar um item, promover pra um plano em `docs/plans/BE-XX-*.md`
- Marcar item como `~~resolvido~~` (ou apagar) quando o plano correspondente for mergeado

---

## Itens abertos



### `server.ssl.key-store-password` hardcoded em `application-prod.properties`

**Contexto:** atualmente a senha do keystore SSL está direto no `application-prod.properties` como string puro (`finbot123`). Não é segredo crítico (cert é auto-assinado, vai ser substituído eventualmente), mas idealmente deveria vir do Secrets Manager.

**Fix sugerido:** adicionar chave `keystore_password` no `finbot-prod-secrets`, mudar property pra `server.ssl.key-store-password=${keystore_password}`.

**Esforço:** baixo (~3 linhas + ajuste no Secrets Manager).

**Prioridade:** baixa. Será obsoleto quando migrar pra Let's Encrypt + domínio real (próximo item).

---

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

### Revisar mensagens de erro/ajuda do bot

**Contexto:** algumas mensagens que o bot manda pro usuário estão desatualizadas em relação às features atuais. Identificadas até agora:

- `PaymentRequestStrategy.parsePedido()` joga `IllegalArgumentException("Formato inválido. Use: pedido <valor> <descrição>")` quando a legenda não bate com o regex. **A mensagem não menciona que dá pra incluir o tipo (boleto, pix, ted, agendamento) na descrição** pra o pedido ser auto-categorizado pela BE-03.
- Provavelmente há outras mensagens (no `GlobalTelegramExceptionHandler`, no `PaymentProofStrategy`, etc) que ficaram parados na versão anterior.

**Fix sugerido:** varredura geral nas strings que o bot manda pro chat (`sendMessage`, mensagens de exceptions), atualizando pra refletir o estado atual das features. Idealmente:

- Mensagem de ajuda no caso de legenda inválida no `PaymentRequestStrategy`: `"Formato inválido. Use: <valor> <descrição com tipo opcional>. Exemplos: \`100 boleto Energia\`, \`200 pix Maria\`, \`50 Almoço\` (sem tipo vira OUTRO)"`
- Mensagem do `PaymentProofStrategy` quando legenda não bate: já tem exemplo `"#123 pix"`, mas vale revisar se está em sincronia com o esperado.
- Considerar criar um comando `/ajuda` no bot que envia o quick reference de formatos suportados.

**Esforço:** baixo a médio (depende de quantas mensagens forem encontradas; provavelmente 5-10 strings).

**Prioridade:** baixa. Não bloqueia uso (você sabe os formatos), mas melhora UX se outras pessoas usarem o bot.

---

## Itens resolvidos

### ~~Esconder `@RequisitanteId` do Swagger UI~~

Resolvido em `feature/backend-polish-evo07` (commit `fix(FIX-hide-requisitanteid-swagger)`). Adicionado bloco `static { SpringDocUtils.getConfig().addAnnotationsToIgnore(RequisitanteId.class); }` em `OpenApiConfig.java`.

### ~~Escopo dos `@RestControllerAdvice` (BE-15b)~~

Resolvido em `feature/backend-polish-evo07` (commit `fix(BE-15b)`). `GlobalTelegramExceptionHandler` migrado de `@ControllerAdvice` para `@RestControllerAdvice(basePackages = "...adapters.in.telegram")`. `RestExceptionHandler` já tinha `basePackages` correto desde a BE-11.
