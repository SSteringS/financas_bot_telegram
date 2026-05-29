# Runbook — PREP-WA: setup manual do WhatsApp Cloud API (Meta)

Roteiro pra deixar a conta/credenciais da **WhatsApp Cloud API** prontas pra a sprint 02 (Canal WhatsApp). É **manual e externo** — passos no console da Meta + Secrets Manager. Não é automatizável por sessão de Claude.

**Quem executa:** humano (você).
**Quando:** começar **o quanto antes**, em paralelo com o desenvolvimento. A **Meta Business Verification** (passo 4) tem prazo externo de dias a semanas — é o gargalo se atrasar.
**Onde os passos batem na sprint:** a maior parte (1–7, 9) dá pra fazer **agora**; o **passo 8 (configurar webhook na Meta)** só funciona depois que o **BE-19** estiver deployado em prod (senão o handshake `GET` falha).

---

## Fases — visão geral

| Fase | Bloco | Lead time | Dá pra fazer agora? |
|---|---|---|---|
| 1 | Pré-requisitos pessoais (número dedicado, conta) | depende de você | Sim |
| 2 | Criar App + WABA | ~5–10 min | Sim |
| 3 | Adicionar o número à WABA + verificar (SMS) | ~5 min | Sim |
| 4 | **Iniciar Meta Business Verification** (assíncrona) | **dias a semanas** | **Sim — começar JÁ** |
| 5 | Obter Phone Number ID + WABA ID | ~2 min | Sim |
| 6 | Obter App Secret + criar System User Access Token (permanent) | ~10 min | Sim |
| 7 | Salvar tudo no AWS Secrets Manager | ~5 min | Sim |
| 8 | Configurar webhook na Meta (`api.satyansaita.com/webhook/whatsapp`) + subscribe `messages` | ~3 min | **Só depois do BE-19 em prod** |
| 9 | Teste com o "Test number" da Meta | ~5 min | Sim (independe do BE-19) |

---

## Fase 1 — Pré-requisitos pessoais

- [x] **Número de telefone DEDICADO.** Não pode ter o WhatsApp comum **nem** o WhatsApp Business app logado nele. Quando o número entra na Cloud API, ele é "convertido" e os dois apps deixam de funcionar nele. Opções:
  - Chip novo (recomendado se for permanente).
  - Número antigo que você já não usa no WhatsApp.
  - Se for um número em uso no WhatsApp comum, **deslogar/desinstalar** o app antes (a Meta também valida isso na hora).
- [x] **Conta no Meta for Developers:** `developers.facebook.com` (login com sua conta Facebook pessoal — não tem como evitar). Quando pedir telefone aqui, é o **seu celular pessoal** (pra 2FA/recuperação da sua conta Facebook) — **não** confundir com o "número dedicado" do bot logo abaixo. São coisas independentes.
- [x] **Conta Meta Business (Business Manager):** `business.facebook.com`. É a camada organizacional **obrigatória** pra usar a Cloud API (segura o WABA, o System User, billing). Se ainda não tem, criar — **não precisa de CNPJ pra abrir**: pode usar qualquer nome de negócio (ex.: "Finbot", seu nome), seu e-mail, e mais adiante um cartão de crédito. CNPJ/MEI só vira necessário **se** quiser fazer a Business Verification (Fase 4) — pro volume da família, rodar sem verificar é perfeitamente OK (250 msgs/dia iniciadas pelo negócio).
- [ ] **Cartão de crédito** vinculado à conta Business. A Cloud API tem **free tier** (1.000 conversas iniciadas pelo usuário/mês), mas a conta exige cartão como pré-condição.

---

## Fase 2 — Criar o App + WABA (WhatsApp Business Account)

No **Meta for Developers** (`developers.facebook.com`):

- [x] *Meus apps* (*My Apps*) → **Criar app** (*Create App*). Dar nome (ex.: `finbot-prod`).
  - Se for perguntado "What do you want your app to do?" / "O que você quer que seu app faça?", escolher **"Other"** (algumas opções ocultam o WhatsApp).
  - Se for perguntado "App type" / "Tipo do app", escolher **"Business"** (não "Consumer"). **Tipo do app não dá pra mudar depois** — se errar, mais rápido apagar e recriar.
- [x] Dentro do App, **adicionar o caso de uso (use case) de WhatsApp**.

  > A Meta reorganizou a UI: o que antes eram "Produtos" virou "**Casos de uso**" (*Use cases*). É a mesma coisa, com outro nome.

  - No painel do App, clicar em **"Adicionar casos de uso"** no canto superior direito (ou no item **"Casos de uso"** na lateral esquerda).
  - No catálogo, procurar o caso de uso que envolve **WhatsApp** — geralmente "Conectar-se com clientes pelo WhatsApp Business Platform" ou similar. Se não aparecer direto, tentar a opção **"Outro"** (leva pra lista crua de produtos onde o WhatsApp está disponível) ou usar a busca.
  - Selecionar o caso de uso → a Meta abre a tela de configuração do WhatsApp.
- [x] Na primeira tela do WhatsApp, vai aparecer uma tela de **autorização de WABAs** — "Escolha os(as) Contas do WhatsApp que o `<seu-app>` deverá acessar":
  - A Meta cria automaticamente uma **"Test WhatsApp Business Account"** (sandbox) — selecionar essa.
  - Recomendo deixar em **"Aceitar apenas os(as) Contas do WhatsApp atuais"** (princípio do menor privilégio). Quando criar a WABA de prod depois (pra o número real), essa tela aparece de novo e você concede acesso a ela.
  - Clicar **Continuar**.

> **Sandbox grátis (a Test WABA):** vem com um **"Test number"** com prefixo gringo (`+1 555…`) + 5 destinatários permitidos. Dá pra mandar/receber mensagens **sem custo, sem verificação Business e sem número real** — ambiente perfeito pra desenvolver e testar a BE-18/BE-19 enquanto a Fase 4 (verificação) roda e antes da Fase 3 (adicionar o número real). A Test WABA NÃO suporta número real; quando for adicionar o número de prod, criar uma WABA nova.

---

## Fase 3 — Adicionar o número real à WABA

Ainda no painel WhatsApp do App:

- [ ] *Phone numbers* → **Add phone number**.
- [ ] Preencher: display name (ex.: `Finbot`), categoria, descrição.
- [ ] Receber código de verificação (SMS ou ligação) no número → digitar → confirmar.

> Não bote o **display name** com palavras genéricas tipo "WhatsApp" ou nomes de marcas conhecidas — risca a aprovação.

---

## Fase 4 — Meta Business Verification (**BLOQUEADOR real, não opcional**)

⚠️ **Atualização (descoberto no primeiro teste):** a Business Verification deixou de ser "vale começar cedo" e virou **bloqueador real da sprint 02**. A Meta retorna erro **`130497 — Business account is restricted from messaging users in this country`** quando uma Business **não-verificada** tenta mandar mensagem **business-initiated** (template/fora da janela) pra usuários no Brasil. Detalhe conceitual em `docs/aprendizado/whatsapp-restricoes-pais-business-nao-verificada.md`.

Impacto no nosso projeto:
- **EVO-01** (Pedro manda → bot recebe e responde dentro de 24h): **funciona** sem verificação — é user-initiated.
- **EVO-02** (bot notifica Pedro proativamente quando o comprovante for registrado): **NÃO funciona** sem verificação — é por definição business-initiated, fora da janela, com destinatário em BR. Trava em **todas** as notificações com o erro 130497.

A Business Verification é **assíncrona** (dias a semanas) e geralmente exige **CNPJ ou MEI**. Sem CNPJ, MEI é simples/grátis de abrir e desbloqueia o caminho.

**O que muda com/sem verificação:**

| | Sem verificação | Com verificação |
|---|---|---|
| Mensagens **user-initiated** (resposta na janela 24h) a BR | livre | livre |
| Mensagens **business-initiated** (templates) a BR | **bloqueada (erro 130497)** | até 250/dia → tiers maiores |
| Mensagens a outros países (US, UE) | template livre, 250/dia | tiers maiores |
| Display name visível | "Test" / limitado | nome aprovado custom |

**Por que se aplica até no Test number:** a restrição é por **Business Account**, não por número. Sandbox não burla. 

Como iniciar:

- [ ] `business.facebook.com` → *Business Settings* → *Security Center* → **Start Verification**.
- [ ] Preencher dados do negócio (CNPJ ou equivalente).
- [ ] Upload de documentação solicitada (varia: contrato social, comprovante de endereço, etc.).
- [ ] Submeter.

> O **desenvolvimento da EVO-01 e EVO-02 não precisa esperar** essa verificação terminar — dá pra fazer tudo no ambiente sandbox/test number e migrar pro número real depois.

---

## Fase 5 — Obter `Phone Number ID` e `WABA ID`

Anotar **valores numéricos**, não os displays.

- [ ] No painel WhatsApp do App → *API Setup* → o **`Phone number ID`** aparece em destaque (e há um seletor pro número adicionado).
- [ ] O **`WhatsApp Business Account ID` (WABA ID)** aparece logo abaixo (ou em *WhatsApp Manager*).

> Anote ambos num bloco temporário (ex.: arquivo local fora do repo) — vão pro Secrets Manager na Fase 7.

---

## Fase 6 — Obter `App Secret` e criar **System User Access Token (permanent)**

São **dois** tokens diferentes — confundir é a armadilha mais comum:

### 6.1 — `App Secret` (valida a assinatura do webhook)

- [ ] No App → *Settings* → *Basic* → **App Secret** (clicar em *Show*, digitar senha do Facebook).
- [ ] Anotar.

### 6.2 — `System User Access Token` (Bearer da Graph API — vai pra prod)

⚠️ **Não usar o "Temporary access token"** que aparece em *API Setup*: ele expira em **24h** e é só pra brincar. Pra prod, criar um **System User Access Token** (que pode ser permanente):

- [ ] `business.facebook.com` → *Business Settings* → *Users* → **System Users** → **Add** → tipo **Admin** → nome (ex.: `finbot-prod-system`).
- [ ] No System User criado, *Add Assets* → vincular o **App** e o **WABA**.
- [ ] No mesmo System User, **Generate New Token**:
  - App: o App finbot.
  - Permissions: marcar **`whatsapp_business_messaging`** (enviar/receber mensagens) e **`whatsapp_business_management`** (criar/gerenciar templates — necessário pra EVO-02).
  - Expiração: **Never** (token permanente).
- [ ] Copiar o token gerado — **é a única vez que aparece**. Se perder, gera outro.

> ⚠️ Esse token dá controle do WABA. Trate como senha: vai direto pro Secrets Manager, sem passar por chat/arquivo versionado.

### 6.3 — `Verify Token` (você escolhe)

É uma **string secreta arbitrária** que você define e usa como senha do handshake `GET` do webhook (Fase 8). A Meta envia esse valor no `hub.verify_token` e nosso back compara com o que está no Secrets.

- [ ] Gerar uma string aleatória forte (ex.: `openssl rand -hex 32`).
- [ ] Anotar — vai pro Secrets Manager **e** pra configuração do webhook na Meta (Fase 8).

---

## Fase 7 — Salvar no AWS Secrets Manager

No secret existente **`finbot-prod-secrets`** (mesmo que já guarda `telegram_token` etc.), adicionar as 4 chaves:

```
whatsapp_app_secret         = <App Secret da Fase 6.1>
whatsapp_access_token       = <System User Token da Fase 6.2>
whatsapp_verify_token       = <string aleatória da Fase 6.3>
whatsapp_phone_number_id    = <Phone Number ID da Fase 5>
```

E opcionalmente:

```
whatsapp_waba_id            = <WABA ID da Fase 5>
```

> Confere que o nome das chaves bate **exatamente** com o que o `application-prod.properties` vai esperar (`whatsapp.app-secret`, `whatsapp.access-token`, `whatsapp.verify-token`, `whatsapp.phone-number-id` — o Spring Cloud AWS mapeia hifens automaticamente). Se a BE-18/BE-19 escolherem nomes ligeiramente diferentes, ajustar aqui pra bater.

---

## Fase 8 — Configurar webhook na Meta (**depois do BE-19 em prod**)

Esta fase **só funciona** quando o `WhatsAppWebhookController` (BE-19) estiver deployado em produção, porque a Meta faz um `GET` de handshake antes de aceitar a config — se ninguém responder ou se o verify_token bater errado, ela rejeita.

- [ ] No App → painel WhatsApp → *Configuration* → **Webhook** → *Edit*.
- [ ] Callback URL: **`https://api.satyansaita.com/webhook/whatsapp`** (reuso do Caddy do DEP-03 — cert LE já válido, não precisa de infra nova).
- [ ] Verify token: a string da Fase 6.3 (mesma do Secrets).
- [ ] Salvar → a Meta dispara o `GET` → nosso back responde com o `hub.challenge` → config aceita.
- [ ] **Subscribe to fields:** marcar **`messages`** (mensagens entrantes; obrigatório pra EVO-01) e **`message_template_status_update`** (status de aprovação de template; útil pra EVO-02).

> Se der erro de verificação aqui: 99% das vezes é (a) verify_token diferente entre Meta e Secrets, ou (b) o BE-19 não está deployado / a app não está respondendo no path certo.

---

## Fase 9 — Teste rápido com o Test Number

Mesmo sem o BE-19 pronto, dá pra confirmar que o token funciona:

- [ ] No painel *API Setup*, copiar o exemplo de `curl` que a Meta gera (envia "hello_world" pro **Test number**).
- [ ] Trocar o token do exemplo pelo **System User Token** (Fase 6.2) — não use o temporário.
- [ ] Rodar — deve voltar `200` com `messages.[0].id`.
- [ ] No primeiro envio, adicionar seu número (o seu, não o do bot) como **Recipient phone number** permitido (até 5 grátis sem verificação Business).

> Esse teste valida **o lado de saída** (Graph API + token). O lado de entrada (webhook) só dá pra testar depois da Fase 8.

---

## Allow-list de remetentes (decisão pra depois)

Análogo ao `telegram.allowed-user-ids` que já existe: o WhatsApp vai precisar de **`whatsapp.allowed-wa-ids`** — lista de números (formato E.164 **sem o `+`**, ex.: `5511987654321`) autorizados a interagir com o bot. Pode começar simples (lista em `application-prod.properties`) e migrar pra banco junto da EVO-06 (multi-requisitante).

- [ ] Anotar o `wa_id` do Pedro (e do seu próprio, pra testes) — vai entrar na config quando a BE-19 ler essa allow-list.

---

## Armadilhas comuns (pra você não tropeçar)

- **Confundir os 3 "tokens"**: `App Secret` (valida assinatura), `Access Token` (Bearer da Graph API), `Verify Token` (handshake do webhook). São coisas diferentes; cada uma vai numa chave separada do Secrets.
- **Usar o temporary token em prod** → expira em 24h e o bot para. Sempre **System User**, sempre permanent.
- **Número com WhatsApp comum ativo** → a Meta rejeita ou converte e quebra o app comum. Limpar antes.
- **Display name genérico** ("WhatsApp", "Test", marcas conhecidas) → reprovado.
- **Webhook configurado antes do BE-19 deployado** → handshake falha; a Meta marca o webhook como inválido.
- **Webhook respondendo > 20s** → a Meta considera falha e pode parar de entregar. A regra "sempre 200 rápido" do ADR 0003 + idempotência da BE-19a cobrem isso.
- **Token vazado em chat/log/arquivo versionado** → revoga e gera outro (no painel do System User).
- **Subscribe esquecido** em `messages` → mensagens chegam no servidor da Meta mas não são entregues pro seu webhook.

---

## Custos (referência rápida)

- **Free tier:** 1.000 conversas iniciadas pelo usuário/mês (conversa = janela de 24h após primeira mensagem do user, ilimitadas dentro dela).
- **Conversas iniciadas pelo negócio** (templates) — tarifa por categoria/país. **Utilidade no BR** ≈ R$ 0,04 por conversa iniciada. Pro volume da família, são centavos/mês.
- O custo aparece na fatura da conta Business (cartão).

---

## Próximo passo (não é desta runbook)

A **submissão do template de utilidade pra EVO-02** (TPL-WA) é um processo separado — também manual na Meta, com aprovação de dias. Vai virar outra runbook (ou seção) **quando entrarmos na EVO-02**, depois do canal funcionar de ponta a ponta na EVO-01.

---

## Referências

- `docs/architecture/adapter-whatsapp-cloud-api.md` §2 (conceitos), §4.1 (handshake), §4.2 (`messages`), §8 (config/secrets), §6 (templates pra EVO-02).
- ADR 0012 (provider = Cloud API oficial) · ADR 0013 (multi-canal).
- `docs/sprints/02-canal-whatsapp/README.md` (objetivo da sprint).
- `docs/sprints/02-canal-whatsapp/plans/BE-19-...` (o controller que responde o webhook — ainda a escrever).
- Docs oficiais Meta: *Get Started* da Cloud API e *Business Verification*.
</content>
