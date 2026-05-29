# Runbook — Smoke test da cadeia Telegram (reusável)

Checklist **enxuto** pra validar manualmente que a cadeia inteira de processamento do Telegram (controller → mapper → orchestrator → strategy → usecase → banco/S3) continua funcionando após qualquer mudança que mexa nela. Pensado pra **dev** (não prod) e pra **rodar rápido** (10–15 min).

**Quem executa:** humano (você ou outro dev).
**Quanto custa:** ~10–15 min.
**Reusar em:** qualquer task que toque a cadeia do Telegram. Lista crescendo:
- **BE-17** — refactor estrutural (mover strategies+orchestrator pra `application/`).
- **BE-17b** — rename de rota `/webhook` → `/webhook/telegram` + `setWebhook` coordenado.
- **BE-19a** — idempotência com claim-then-process (muda a transação).
- (futuro) qualquer outro refactor que entre na cadeia.

**Quando NÃO rodar:** mudanças que comprovadamente não tocam a cadeia (ex.: alteração só no front, só na API REST, só em observability, só de docs). Tests automatizados + Reviewer cobrem o resto.

---

## Pré-condições

- [ ] **App rodando em dev** (local na sua máquina ou EC2 de dev), usando profile `dev` (`spring.profiles.active=dev`).
- [ ] **Bot dev no Telegram** — token separado do prod (sai do BotFather, vive em `application-dev.properties`).
- [ ] **MySQL dev** acessível pelo app (schema com migrações aplicadas: V1..V3 + EVO-07).
- [ ] **S3 bucket dev** acessível e configurado (`bot-financas-pagamentos-dev` ou equivalente, conforme `application-dev.properties`).
- [ ] **Seu chat ID** está na lista `telegram.allowed-user-ids` do `application-dev.properties` — senão o bot ignora suas mensagens.
- [ ] **Webhook do bot dev apontando pro ngrok / EC2 de dev** (`setWebhook` feito), ou polling se for esse o modo de dev.

Se faltar algo aqui, **resolver antes** — sem ambiente dev sólido o smoke test mente.

---

## Cenários

> Cada cenário tem (a) **ação** no chat com o bot dev, (b) **resposta esperada** do bot, (c) **verificação no banco/S3** quando aplicável. Anotar resultado de cada um.

### 1. Pedido com FOTO (caminho principal de criação)

- [ ] Mandar pro bot dev: legenda `100 teste pedido foto` + uma foto qualquer.
- [ ] **Resposta do bot:** mensagem de sucesso com `Pedido #<id> registrado`, mostrando valor e tipo (provavelmente `OUTRO`, sem palavra-chave).
- [ ] **No MySQL:** `SELECT * FROM pedido_pagamento ORDER BY id DESC LIMIT 1;` → linha nova com `valor=100.00`, `descricao="teste pedido foto"`, `status='PENDENTE'`, `tipo_arquivo='image/jpeg'` (ou similar), `s3_key_pedido` preenchido.
- [ ] **No S3:** o objeto referenciado por `s3_key_pedido` existe no bucket dev.

### 2. Pedido com DOCUMENTO/PDF (caminho EVO-07)

- [ ] Mandar pro bot: legenda `200 teste pedido pdf` + um PDF qualquer (anexar como documento, não como foto).
- [ ] **Resposta:** mensagem de sucesso similar ao cenário 1.
- [ ] **No MySQL:** linha nova com `valor=200.00`, `tipo_arquivo='application/pdf'`.
- [ ] **No S3:** objeto no bucket com extensão `.pdf`.

> Se este falhar mas o cenário 1 passar, o problema é específico do caminho de document/PDF — sinaliza regressão no EVO-07.

### 3. COMPROVANTE de um pedido pendente

- [ ] Anotar o `id` do pedido criado no cenário 1 (ou criar um novo se quiser dedicado).
- [ ] Mandar pro bot: legenda `#<id> pix` + foto qualquer (simulando comprovante).
- [ ] **Resposta:** mensagem confirmando `Comprovante registrado para o pedido #<id>`.
- [ ] **No MySQL:**
  - `pedido_pagamento.status` daquele `id` agora é `PAGO`.
  - `SELECT * FROM comprovante WHERE pedido_id = <id>;` → linha nova com `s3_key` preenchido.
- [ ] **No S3:** objeto do comprovante existe.

### 4. Mensagem de ERRO de formato (caminho didático)

- [ ] Mandar pro bot: texto solto que não bate em nenhum padrão, ex.: `algumacoisa qualquer`.
- [ ] **Resposta:** mensagem didática do `InvalidMessageFormatException` (a versão polida do `FIX-revisar-msgs-erro-bot`), listando os formatos válidos com exemplos (`100 boleto`, `200 pix`, etc.).
- [ ] **No MySQL:** **nada novo** (não cria pedido nem comprovante).
- [ ] Confirma que: a) o erro de negócio é distinguido do de infra, b) o usuário recebe orientação.

### 5. Handler genérico de exceção (caminho BE-15 / ADR 0003)

- [ ] Esse exige um pouco mais — provoca uma exceção não-mapeada. Opções:
  - **Mais simples:** mandar `#999999 pix` + foto (referência a pedido inexistente). Isso deve cair como erro de negócio também, mas validar que **NÃO** retorna 5xx pro Telegram nem trava a fila.
  - **Mais fundo (se quiser):** desligar o MySQL/S3 brevemente e mandar um pedido — vê se cai no handler genérico, responde mensagem amigável e retorna 200.
- [ ] **Verificação principal:** olhar nos **logs** (CloudWatch dev se DEP-09 já estiver lá, ou local) que apareceu `ERROR` com stack trace + a mensagem amigável foi enviada + retorno 200 pro Telegram (não 5xx).
- [ ] Critério: o bot **não trava** depois do erro — o próximo cenário válido (recriar cenário 1) ainda funciona.

### 6. (Opcional, conforme contexto da task) Validações específicas

Adicionar conforme a task que motivou o smoke test:

- **BE-17b (rename de rota):** confirmar via `getWebhookInfo` que a URL aponta pra `/webhook/telegram` e `last_error_message` está vazio.
- **BE-19a (idempotência):** simular reentrega do mesmo Update (reenviar o mesmo webhook payload duas vezes via `curl`) → confirmar que o segundo é descartado (não cria pedido duplicado) e a tabela `mensagem_processada` tem só uma linha.
- (futuro) outros critérios específicos.

---

## Critério de "passou"

- [ ] Cenários 1–5 todos com resultado esperado (resposta + DB + S3 quando aplicável).
- [ ] Logs sem `ERROR` inesperado (só os erros que **você provocou** no cenário 5).
- [ ] Bot **continua respondendo** após o cenário 5 — fila não travou (regressão do incidente do PDF de maio/26).

Qualquer item falhando = **NÃO promover** pra prod. Investigar antes.

---

## Fora de escopo deste smoke test

Pra manter enxuto, isto **não** está aqui:

- E2E do site (PWA): coberto pelo `RUNBOOK-dep06-e2e-prod.md`.
- Carga / concorrência: nem dev nem prod simulam tráfego real.
- Migrações de DB: assumidas aplicadas (Flyway na subida).
- Validação CORS / cookie: domínio API/front, não Telegram.

---

## Onde anotar resultado

No **status report da task** que motivou o smoke test (ex.: `docs/sprints/02-canal-whatsapp/status/BE-17.md`):

```markdown
## Smoke test (RUNBOOK-smoke-test-telegram-cadeia.md)

Executado em <data>, ambiente dev, commit <hash>.

| Cenário | Resultado | Observação |
|---|---|---|
| 1. Pedido foto | ✅ | id=42, S3 ok |
| 2. Pedido PDF | ✅ | id=43, tipo_arquivo=application/pdf |
| 3. Comprovante | ✅ | pedido 42 → PAGO |
| 4. Erro formato | ✅ | mensagem didática ok |
| 5. Handler genérico | ✅ | 200 retornado, bot segue respondendo |
```

E linkar daqui em "Validação manual" se houver dúvida do Reviewer.

---

## Referências

- ADR 0003 (controller do webhook **sempre** 200 — cenário 5 valida).
- `docs/architecture/adapter-whatsapp-cloud-api.md` §3 (a cadeia que estamos testando, pós-BE-17).
- `docs/aprendizado/exception-handlers-scope.md` (escopo do handler que cobre o cenário 5).
- `docs/PENDENCIAS-TECNICAS.md` (item "comprovante mal-formado vira pedido fantasma" — pode aparecer como surpresa no cenário 4 se o usuário escrever `123 pix` sem `#`; comportamento conhecido).
</content>
