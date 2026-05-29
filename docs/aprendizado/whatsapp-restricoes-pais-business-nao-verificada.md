# WhatsApp Cloud API — restrição de país pra Business não verificada (e por que Verification não é opcional pro Brasil)

## Contexto da dúvida

Durante o setup da Cloud API (PREP-WA), o primeiro teste do Test number pra um número brasileiro retornou:

```
"code": 130497,
"title": "Business account is restricted from messaging users in this country."
```

A reação inicial é "deve ser config" — mas é **política deliberada da Meta**, e tem impacto direto no plano da sprint 02 (EVO-02 inteira depende disso).

## Resumo destilado

A Meta bloqueia **qualquer envio saindo do negócio** pra usuários em **certos países sensíveis** quando a **Business Account não está verificada** — **Brasil é um deles**. A finalidade é antispam: forçar o negócio a se identificar (Business Verification) antes de poder atuar nesses mercados.

⚠️ **Atualização crítica (descoberta 2026-05-28):** inicialmente eu pensei que a restrição valia **só** pra business-initiated (template/fora da janela) — e que dentro da janela de 24h (user-initiated, "service message") fluiria livremente. **Errado.** Teste real: usuário BR mandou mensagem pro Test number; segundos depois, tentativa de resposta do bot pelo curl da API **caiu no mesmo erro 130497**. A restrição de país **ignora** a categorização de conversa (service vs marketing/utility) — pra BR com Business não-verificada, **TODO envio do bot é bloqueado**, independentemente da janela.

### Como isso recorta nosso uso (corrigido)

| Cenário | Categoria | Bloqueia sem verificação? |
|---|---|---|
| Pedro manda foto de comprovante → bot **recebe** (webhook) | entrada | Não (só envio é restrito) |
| Pedro manda foto → bot **responde** "registrado ✅" na hora | user-initiated (janela 24h) | **Sim — bloqueia (descoberta de 2026-05-28)** |
| Pedido registrado → bot avisa Pedro automaticamente (EVO-02) | business-initiated (template) | **Sim** |
| Pedro pediu algo → bot responde 25h depois | business-initiated (fora da janela) | **Sim** |
| Manda mensagem inicial pra usuário em país NÃO restrito | business-initiated | Não (mas precisa template aprovado) |

### Por que a Business Verification deixa de ser opcional

Tinha sido apresentada como "vale começar cedo" no runbook PREP-WA original. Pós-descoberta, ela é **bloqueador absoluto do canal WhatsApp pro caso do Pedro** — tanto a EVO-01 (responder o Pedro) quanto a EVO-02 (notificação proativa) precisam de envio do bot pra número BR, e ambas travam até a verificação sair. Só o **fluxo de entrada (recepção via webhook)** funciona sem verificação, mas sem poder responder ele isolado não serve.

Lead time externo da verificação: **dias a semanas**. Documentação (CNPJ ou MEI) ajuda — sem CNPJ a verificação tem chance baixa de aprovar. MEI é simples e barato de abrir e desbloqueia o caminho.

### Por que afeta o Test number também

Não é só problema do número de produção — o Test number da Meta (que parece "sandbox sem regras") **também** obedece à restrição, porque a regra é por **Business Account**, não por número. Sandbox não burla.

### Workaround da janela de 24h — **NÃO funciona** pra BR

Hipótese inicial (descartada por teste real): user mandar mensagem primeiro abriria a janela e a resposta do bot fluiria. **Não.** A restrição de país é avaliada **antes** da categorização de conversa. Testado em 2026-05-28: mensagem chegou ao Test number; resposta em segundos via curl da Graph API → mesmo 130497.

### Caminhos viáveis pra continuar a sprint enquanto a verificação não sai

- **Submeter Business Verification agora** (caminho real). Lead time: dias a semanas. Sem CNPJ, abrir **MEI** pelo Portal do Empreendedor (grátis, em horas) e usar pra verificar.
- **Testar BE-18 / BE-19 contra recipientes de país NÃO restrito** (se tiver alguém em US/UE disposto a topar). Valida o código mas não o caso real do Pedro.
- **Testar a parte de código sem Meta real** — unit tests do `WhatsAppMessageSenderService` contra mock do Graph; do `WhatsAppWebhookController` com payload stub. Cobre BE-17/18/19/19a inteira sem dependência da Meta.
- **Integração real (BE-20) e validação pelo Pedro só rolam pós-verificação.** O cronograma da sprint 02 pra fim-a-fim com Pedro depende do prazo da Meta.

## Pontos-chave

- **Erro 130497 = política, não bug:** Business **não-verificada** + destinatário em país restrito (BR) = bloqueio garantido em **qualquer** envio do bot.
- **A restrição é por Business Account**, não por número — **Test number também sofre**. Sandbox não burla.
- **A restrição ignora a categorização de conversa** (service vs marketing vs utility). Janela de 24h **não** ajuda no BR. *Descoberta corrigida em 2026-05-28; minha hipótese inicial estava errada.*
- **Recepção (webhook) funciona** sem verificação — só envio é bloqueado. Mas sem poder responder, recepção isolada não serve.
- **Business Verification é bloqueador absoluto** do canal WhatsApp pro caso BR (EVO-01 **e** EVO-02). Sem CNPJ, MEI grátis e em horas no Portal do Empreendedor destrava.
- **Desenvolvimento (BE-17/18/19/19a)** segue contra mocks + recipientes em país não-restrito.
- **Integração real e validação com Pedro só pós-verificação.** Cronograma da sprint 02 fica amarrado ao prazo da Meta.

## Pra aprofundar

- Documentação Meta — *Per-User Marketing Message Limits* e *Conversation Types* (service vs marketing vs utility).
- Business Verification: requisitos, documentação aceita, prazos típicos em PT-BR.
- Categorias de template (utility, marketing, authentication) — mesma restrição, custos diferentes.
- Janela de 24h (Customer Service Window) — quando abre, quando fecha, contagem por par usuário↔negócio.
- Relação com a EVO-02 e a discussão de "canal preferido por requisitante" do ADR 0013 (fallback pro Telegram quando o WhatsApp estiver restrito).
</content>
