# WhatsApp: modelo de mensagens (janela 24h, categorias, oficial vs não-oficial)

## Contexto da dúvida

Apareceu na Sprint 02 (EVO-01), ao escolher o provider de WhatsApp pro bot (ADR 0012). Pra comparar custo e risco entre a **Cloud API oficial da Meta** e soluções **não-oficiais** (Z-API, Evolution/Baileys), precisei entender como a Meta cobra e o que ela permite. O conceito é formativo porque também molda o **design da EVO-02** (notificar Pedro automaticamente).

## Resumo destilado

**Receber mensagem nunca é cobrado.** A Meta só cobra por mensagens que o **seu negócio envia**, e mesmo assim só em alguns casos. Quando o usuário te manda algo, custo zero.

A Cloud API oficial cobra **por mensagem** (modelo vigente desde 01/jul/2025; antes era por conversa). O que define se você paga e quanto são **duas coisas**: a **janela de atendimento de 24h** e a **categoria** da mensagem.

A **janela de 24h** abre quando o **usuário te manda** uma mensagem e fica aberta por 24h a partir da última mensagem dele. Dentro da janela, você responde livremente com mensagens de **serviço**, que são **gratuitas**. Fora da janela, você só pode iniciar conversa usando um **template pré-aprovado** pela Meta, e aí entra a cobrança por categoria.

**Mensagem de serviço:** mensagem de **texto/conteúdo livre** (não-template) que você envia pro usuário **dentro da janela de 24h** que ele abriu. Chama "de serviço" porque você está *atendendo* uma conversa que o **usuário começou** — em oposição a você **iniciar** uma conversa do nada (que exige template).

As **categorias** de template: **serviço** (resposta na janela, grátis), **utilidade** (transacional: confirmação, recibo, status — barato, ~US$0,008 no Brasil, e grátis se enviado dentro da janela), **autenticação** (OTP/códigos) e **marketing** (promoção — o mais caro, ~US$0,0625 no Brasil). Categorizar errado (mandar utilidade como marketing) custa mais e pode ser reclassificado pela Meta.

As **não-oficiais** (Z-API, Evolution sobre Baileys) contornam tudo isso pareando um número como se fosse o WhatsApp Web: cobram **mensalidade fixa** (Z-API) ou são grátis/self-host (Evolution), sem janela nem template. O preço disso é **violar o ToS** — a Meta detecta e **bane o número**. Para um número **pessoal**, ban = perder a conta real, não só um canal.

## Pontos-chave

- **Receber é sempre grátis.** Cobrança só no envio do negócio pro usuário.
- **Janela de 24h**: abre quando o **usuário** fala; dentro dela mensagens de **serviço são grátis**.
- **Fora da janela** só dá pra iniciar com **template aprovado** → cobrança por categoria.
- **Categorias** (Brasil, ordem de custo): serviço (grátis) < utilidade (~US$0,008, grátis na janela) < autenticação < marketing (~US$0,0625).
- **Régua mental:** receber = grátis; responder na janela = grátis (serviço); iniciar/fora da janela = template (pago, mas utilidade é baratíssima).
- **Volume baixo + conversa iniciada pelo usuário** ⇒ custo da oficial é praticamente **zero**. Mensalidade fixa de não-oficial pode sair **mais cara**.
- **Não-oficial = violação de ToS** = risco de **ban**. Pior quando o número é pessoal/familiar.
- **Impacto de design (EVO-02)**: notificar alguém **fora da janela** exige **template de utilidade aprovado**; dentro da janela, mensagem de serviço grátis basta.
- **Setup da oficial** tem atrito one-time: Meta Business verificado, **número dedicado** (não pode ser o mesmo número logado no app comum), webhook com **HTTPS válido** e validação de assinatura.

## Pra aprofundar

- Webhook da Cloud API: *verify challenge* no `GET` (`hub.challenge`) e assinatura `X-Hub-Signature-256` no `POST`.
- Graph API de envio: `POST /{phone-number-id}/messages`; download de mídia em 2 passos (URL → binário com bearer).
- ADR 0012 (decisão do provider) e ADR 0003 (webhook sempre 200 — a Meta também retenta em não-2xx).
