# 0012 — Provider de WhatsApp: Cloud API oficial da Meta (EVO-01)

**Data:** 2026-05-27
**Status:** `Proposed`
**Decisores:** humano (PO) — proposto pelo Arquiteto

> ADR proposto. Só vira `Accepted` por homologação do humano (papel do Arquiteto: não homologa a própria decisão).

---

## Contexto

A Sprint 02 (`docs/sprints/02-canal-whatsapp/README.md`) move o canal do bot do Telegram pro **WhatsApp**, onde a família já vive (EVO-01), e adiciona notificação automática quando um comprovante é registrado (EVO-02, depende do canal). A arquitetura hexagonal isola o canal de entrada num adapter (`adapters/in/telegram` hoje), então trocar/adicionar canal é, em tese, um adapter novo reaproveitando os usecases existentes (`SalvarPedidoPagamentoUsecase`, `RegistrarComprovanteUsecase`) — ver `docs/architecture/estado-atual.md` §3.

A escolha **em aberto** é o provider de acesso ao WhatsApp, e ela cascateia em custo recorrente, risco operacional e formato do adapter:

1. **Cloud API oficial da Meta** — a API de negócios suportada pela Meta. Webhook de entrada (JSON) + Graph API de saída.
2. **Z-API** — SaaS brasileiro **não-oficial** que pareia um número de WhatsApp comum via protocolo WhatsApp Web e expõe REST. Mensalidade fixa.
3. **Evolution API** — projeto **open-source** self-hosted (Docker) que também usa o protocolo WhatsApp Web (lib **Baileys**) e expõe REST. (Hoje também suporta a Cloud API oficial como backend.)

O perfil de uso é **volume minúsculo e número pessoal/familiar**: poucas mensagens por dia, e o número usado é o WhatsApp real da família. Isso muda o peso dos trade-offs em relação ao caso típico de e-commerce (onde se discute custo por mensagem em alto volume).

---

## Decisão

Adotar a **Cloud API oficial da Meta** como provider do canal WhatsApp da EVO-01.

O adapter de entrada vira `adapters/in/whatsapp` (webhook que **sempre retorna 200**, herdando o ADR 0003), e o de saída vira `adapters/out/whatsapp` (envio e download de mídia via Graph API), espelhando a estrutura atual do Telegram. Os usecases da camada de aplicação **não mudam**.

Z-API e Evolution ficam **fora** — Evolution-via-Baileys e Z-API por violarem o ToS do WhatsApp e expor o número da família a banimento; a Cloud API cobre a necessidade com custo efetivamente nulo neste volume.

---

## Razões

Por que a oficial e **não** as alternativas, nos quatro eixos pedidos:

**Custo — a oficial é a mais barata neste volume, não a mais cara.** Desde 01/jul/2025 a Meta cobra **por mensagem** (não mais por conversa). Mensagens de **serviço** (resposta dentro da janela de 24h iniciada pelo usuário) são **gratuitas**, e templates de **utilidade** dentro dessa janela também. No nosso caso, o registro de pedido/comprovante é sempre o usuário falando com o bot → janela aberta → **R$ 0**. A notificação da EVO-02 a Pedro, se cair fora da janela, é um **template de utilidade** (~US$ 0,008 ≈ R$ 0,04 no Brasil) ou grátis dentro da janela. À família, isso é praticamente zero. Já o **Z-API custa R$ 55–99/mês fixos** independentemente de mandar 5 ou 5.000 mensagens — ou seja, **mais caro** que a oficial pro nosso volume. Evolution é "grátis" como software, mas paga-se em **operação** (rodar e manter o container, atualizar o Baileys quando a Meta muda o protocolo) — custo de tempo, não de fatura.

**Risco de ToS — decisivo aqui.** Z-API e Evolution-via-Baileys **violam explicitamente** os Termos de Serviço do WhatsApp; a Meta detecta padrões não-humanos e **bane contas**. Pro caso típico, banimento = perda de vendas. **Pro nosso caso é pior**: o número é o WhatsApp **real da família**, então um ban significa perder a conta pessoal, não um canal comercial descartável. Para uma utilidade doméstica que deve durar anos, aceitar um canal que "vive 2–8 semanas até o próximo ciclo de detecção" é risco desproporcional ao que se economiza (que, como visto, é negativo). A Cloud API é o caminho **sancionado** pela Meta — sem esse risco por construção.

**Esforço — a oficial tem mais atrito de setup, porém one-time, e o adapter é igual nos três.** Os três providers são **webhook-in / REST-out**, então o formato do adapter hexagonal é parecido em todos. A diferença de esforço está nas pontas: a oficial exige **setup inicial** (verificação no Meta Business, número dedicado, registro do webhook, **aprovação de template** pra notificação da EVO-02) — atrito de uma vez. Z-API encurta o setup mas adiciona **dependência de um SaaS terceiro** no caminho crítico. Evolution elimina o terceiro mas adiciona **carga operacional permanente** na EC2 t4g.micro (mais um processo Node + Baileys pra manter vivo e atualizado), competindo por recursos com o backend Java.

**Encaixe hexagonal — a oficial dá o contrato mais estável.** Como todos são webhook+REST, todos encaixam. Mas o contrato da Cloud API é **versionado e estável** (Graph API), enquanto Baileys (Z-API/Evolution) é engenharia reversa do WhatsApp Web que **quebra quando a Meta muda o protocolo**. Um adapter sobre um contrato estável é menos manutenção ao longo do tempo — exatamente o que a hexagonal quer proteger atrás da porta.

---

## Consequências

**Positivas:**
- Custo recorrente efetivamente **nulo** no volume da família (vs. R$ 55–99/mês fixos do Z-API).
- **Sem risco de banimento** do número da família — caminho sancionado pela Meta.
- Contrato de API **estável e versionado**; adapter sofre menos com mudanças de protocolo.
- ADR 0003 (webhook sempre 200) e o padrão de strategies se reaproveitam quase inteiros.
- Nada novo pra manter rodando na EC2 t4g.micro.

**Negativas:**
- **Setup inicial mais pesado**: conta Meta Business + verificação, número de telefone **dedicado** (um número em uso na Cloud API **não pode** ser o mesmo logado no app WhatsApp comum), registro de webhook, validação de assinatura.
- A **EVO-02 fica condicionada à janela de 24h**: notificar Pedro fora da janela exige **template de utilidade pré-aprovado** pela Meta (processo de aprovação + categorização correta pra não cair como "marketing"). Isso é uma **dependência da EVO-02**, não da EVO-01.
- Precisa expor o webhook publicamente com HTTPS válido — hoje a EC2 usa **certificado auto-assinado** na 8443 (`estado-atual.md` §6); a Meta exige cert **válido/confiável**. Provável dependência de infra (cert real / proxy / domínio), a mapear com o planner.
- Curva de aprendizado da Graph API (categorias de mensagem, janela, templates) maior que "manda string no Z-API".

---

## Alternativas consideradas

### Comparativo (um aspecto por linha)

Legenda: ✅ favorável · ⚠️ atenção/atrito · ❌ desfavorável · 〰️ neutro/depende · ➕ adiciona dependência.

| Aspecto | **Cloud API oficial (Meta)** | **Z-API (não-oficial)** | **Evolution API (self-host/Baileys)** |
|---|---|---|---|
| Natureza | API de negócios sancionada pela Meta | SaaS BR que pareia nº via WhatsApp Web | Open-source, self-host (Docker), usa Baileys |
| Custo no nosso volume | ✅ ~**R$ 0** (resposta na janela 24h grátis; notificação = template utilidade ~R$0,04 ou grátis na janela) | ⚠️ **R$ 55–99/mês fixos** (mande 5 ou 5.000) | 〰️ Software grátis, mas custa **operação** (container + manutenção na EC2) |
| Modelo de cobrança | Por mensagem, por categoria (serviço grátis / utilidade barata / marketing cara) | Mensalidade fixa, mensagens ilimitadas | Sem cobrança por mensagem; custo é infra/tempo |
| Risco de ToS / ban | ✅ **Nenhum** — caminho oficial | ❌ **Viola ToS**, Meta bane o número | ❌ **Viola ToS** (Baileys), bane o número |
| Gravidade do ban no nosso caso | n/a | 🔴 Perde o WhatsApp **real da família** | 🔴 Perde o WhatsApp **real da família** |
| Estabilidade do contrato | ✅ Graph API versionada e estável | ⚠️ Quebra quando a Meta muda o WhatsApp Web | ⚠️ Idem — depende da comunidade atualizar o Baileys |
| Esforço de setup (one-time) | ⚠️ Alto: verificação Meta Business, nº dedicado, registro de webhook, validação de assinatura | ✅ Baixo: lê QR code e pronto | 〰️ Médio: subir container, parear, configurar |
| Esforço recorrente / manutenção | ✅ Baixo (nada novo rodando na EC2) | ✅ Baixo (terceiro cuida) | ❌ Alto (manter processo Node + Baileys vivo/atualizado) |
| Encaixe hexagonal (formato do adapter) | Webhook-in + REST-out → espelha o Telegram atual | Webhook-in + REST-out (parecido) | Webhook-in + REST-out (parecido) |
| Aderência ao ADR 0003 (webhook sempre 200) | ✅ Aplica igual (Meta retenta em não-2xx) | 〰️ Aplica conforme o provider | 〰️ Aplica conforme config |
| Reaproveita usecases atuais | ✅ Sim (core não muda) | ✅ Sim | ✅ Sim |
| Dependência externa no caminho crítico | Só a Meta (inevitável) | ➕ Um SaaS terceiro | Nenhum terceiro, mas ➕ infra própria |
| Número de telefone | Dedicado (não pode ser o mesmo logado no app comum) | Um nº comum no app | Um nº comum no app |
| Restrição que afeta a EVO-02 | Notificar fora da janela 24h exige **template aprovado** | Sem restrição de janela/template | Sem restrição de janela/template |
| Infra extra necessária | HTTPS com **cert válido** (hoje é auto-assinado) | HTTPS válido | Container + HTTPS |
| **Veredito** | ✅ **Escolhido** | ❌ Descartado | ❌ Descartado (como Baileys) |

Leitura curta: o único eixo em que as não-oficiais ganham é **atrito de setup** — e pagam isso com **violação de ToS + risco de perder o número da família** e **custo recorrente** (Z-API) ou **carga operacional** (Evolution). No nosso volume, a oficial sai mais barata *e* mais segura.

### Detalhamento

- **Z-API (SaaS não-oficial):** descartado. Viola o ToS do WhatsApp (risco de ban do número **pessoal** da família), custa **mais** que a oficial neste volume (mensalidade fixa vs. ~zero), e insere um terceiro no caminho crítico. A vantagem (setup trivial) não compensa.
- **Evolution API self-hosted (Baileys):** descartado **como backend Baileys/WhatsApp-Web**. Mesmo problema de ToS/ban, mais carga operacional na t4g.micro e fragilidade quando a Meta muda o protocolo. *Nuance:* a Evolution **também sabe falar a Cloud API oficial** — se no futuro quisermos uma camada de orquestração multi-canal (n8n, Chatwoot etc.), Evolution **sobre Cloud API** poderia ser reconsiderada sem violar ToS. Não é o caso agora (over-engineering pro escopo).
- **Manter só Telegram:** fora de escopo — contraria o objetivo declarado da Sprint 02 (estar onde a família já está). Registrado só pra deixar claro que foi considerado.

---

## Riscos e dependências (pro planner quebrar em tasks)

- **Número dedicado** pra Cloud API — definir origem (chip novo / número virtual). Bloqueia o início da EVO-01.
- **Verificação Meta Business** — processo externo com prazo próprio; começar cedo.
- **HTTPS com cert válido** no webhook — provável task de infra antes do adapter (hoje é auto-assinado).
- **Template de utilidade aprovado** pra EVO-02 — dependência da notificação, não do canal.
- **Observability primeiro** (já na ordem sugerida da sprint) — rede de segurança pra debugar a migração; ver `observability-logs-externalizar.md`.
- **Porta de entrada agnóstica de canal** — oportunidade de refactor (`TelegramPortIn` → nome neutro) ao introduzir o segundo canal.

---

## Referências

- `docs/sprints/02-canal-whatsapp/README.md` — objetivo da sprint (EVO-01/02 + observability)
- `docs/architecture/estado-atual.md` §3, §5, §6 — estrutura hexagonal e config atual
- `docs/decisions/0003-controller-webhook-nunca-retorna-5xx.md` — padrão de webhook resiliente (aplica-se à Meta)
- `docs/aprendizado/whatsapp-modelo-mensagens.md` — conceito: janela 24h, categorias, oficial vs não-oficial (criado junto deste ADR)
- `docs/architecture/adapter-whatsapp-cloud-api.md` — spec técnica do adapter (webhook, mídia, porta agnóstica, impacto na EVO-02)
- Meta — Pricing on the WhatsApp Business Platform (modelo per-message, vigente 01/jul/2025): https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing
- Pesquisa de preço Brasil 2026 (utility ~US$0,008 / marketing ~US$0,0625): Message Central, EngageLab
- Z-API (planos R$55–99/mês, mensagens ilimitadas, modelo não-oficial): https://z-api.io/
- Evolution API (open-source, Baileys/WhatsApp-Web + suporte a Cloud API): https://github.com/EvolutionAPI/evolution-api
