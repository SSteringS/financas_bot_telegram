# 0013 — Estratégia multi-canal: manter Telegram e adicionar canais como adapters

**Data:** 2026-05-27
**Status:** `Proposed`
**Decisores:** humano (PO) — proposto pelo Arquiteto

> Reflete direção dada explicitamente pelo humano em 2026-05-27 ("manter o Telegram; só adicionar adapters; Discord como evolução futura"). Mantido como `Proposed` por disciplina de papel (o Arquiteto não homologa a própria proposta — ADR 0011); pronto pra homologação junto do ADR 0012.

---

## Contexto

A Sprint 02 (EVO-01) enquadrava o canal WhatsApp como **"migrar (ou adicionar)"** o canal do bot — deixando em aberto se o Telegram seria **desligado** após o WhatsApp entrar. O ADR 0012 escolheu o provider (Cloud API oficial), e a spec `adapter-whatsapp-cloud-api.md` §7 deixou a escolha migrar-vs-adicionar como decisão de produto/PO.

A arquitetura hexagonal já isola o canal de entrada num adapter, com porta de entrada (a ser tornada agnóstica, `MensagemEntrantePortIn`) e usecases compartilhados que **não conhecem o canal**. Isso torna a coexistência de canais barata por construção.

Era preciso fechar a direção pra o planner conseguir quebrar EVO-01/EVO-02 em tasks (notificar em qual canal? desligar Telegram ou não?).

---

## Decisão

**O Telegram permanece.** O WhatsApp é **adicionado** como um canal a mais (não é migração). Daqui pra frente, **canal = adapter**: cada novo canal entra como um par de adapters (entrada/saída) sobre a **mesma porta agnóstica de canal** e os **mesmos usecases**, sem tocar no núcleo.

O **Discord** fica **registrado como ideia de evolução futura** — candidato a próximo adapter, **sem compromisso de prazo nem escopo** nesta sprint.

---

## Razões

- **Custo de coexistir é marginal:** a hexagonal já paga o preço do desacoplamento; com a porta agnóstica + usecases compartilhados, manter dois canais é "mais um controller + um sender", não duplicação de regra de negócio.
- **Não há motivo pra perder o Telegram:** funciona, tem custo de ToS zero, e é o canal que o dono do projeto já usa (inclusive como uso administrativo / fallback). A família migra o uso pro WhatsApp, mas isso não exige **desligar** o Telegram.
- **Evita cutover arriscado:** "adicionar" elimina a janela de virada (desligar A e ligar B ao mesmo tempo). Combina com a ordem da sprint (observability cedo) e reduz risco da migração.
- **Estabelece o ponto de extensão:** firmar "canal = adapter" como padrão torna canais futuros (Discord, etc.) baratos e previsíveis, e **justifica permanentemente** o investimento no refactor da porta agnóstica — ela deixa de ser andaime de transição e vira infra de longo prazo (vale pra ≥2 canais pra sempre).

---

## Consequências

**Positivas:**
- Sem risco de cutover; Telegram continua disponível.
- Canais futuros ficam baratos (padrão "canal = adapter" firmado).
- O refactor da porta agnóstica (`MensagemEntrantePortIn`) e do `NotificadorPortOut` passa a se pagar de forma permanente, não só na transição.

**Negativas:**
- **Manter ≥2 adapters de entrada indefinidamente:** dois webhooks, dois senders, dois esquemas de validação/assinatura, dois formatos de envelope. Mais superfície de manutenção e de teste.
- **Autorização duplicada por canal:** a allow-list (hoje `telegram.allowed-user-ids`) precisa de equivalente por canal (`whatsapp.allowed-wa-ids`, etc.). Vale pensar numa fonte única de identidade do requisitante mapeando os IDs de cada canal.
- **Roteamento da notificação (EVO-02) vira decisão real:** com >1 canal, "avisar o Pedro" precisa saber **em qual canal**. Provável: **canal preferido por requisitante** (campo/mapa), com fallback. Não bloqueia a EVO-01, mas o planner precisa prever na EVO-02.

---

## Alternativas consideradas

- **Migrar (desligar o Telegram após o WhatsApp):** descartado. Joga fora um canal que funciona e não custa nada, e força uma virada arriscada sem benefício — a hexagonal torna a coexistência barata, então não há economia real em desligar.
- **Só WhatsApp daqui pra frente (sem mais canais):** descartado. Fecharia a porta pra evolução (Discord etc.) sem ganho; o custo de manter o padrão aberto é baixo.
- **Adicionar Discord agora:** fora de escopo. Registrado como **ideia futura**, não comprometido. *Nota técnica pro futuro:* o Discord não é webhook-puro como Telegram/WhatsApp — bots usam tipicamente uma conexão **Gateway (WebSocket)** persistente (ex.: lib JDA no Java) ou um **Interactions endpoint** HTTP pra slash commands. A porta agnóstica continua valendo, mas o **transporte de entrada difere** do padrão webhook-POST atual — o adapter Discord terá forma de entrada própria. Avaliar quando virar prioridade.

---

## Consequências pro planejamento (insumo pro planner)

- Reconciliar o texto "migrar (ou adicionar)" do `docs/sprints/02-canal-whatsapp/README.md` com esta decisão (coexistir) — território do planner/PO.
- EVO-02: incluir **roteamento de notificação por canal** (canal preferido do requisitante + fallback).
- Identidade do requisitante: considerar mapear IDs por canal numa fonte única (evolução do `requisitante` discutido em `architecture/estado-atual.md`).

---

## Referências

- `docs/decisions/0012-provider-whatsapp-cloud-api-oficial.md` — provider do WhatsApp (Cloud API oficial)
- `docs/architecture/adapter-whatsapp-cloud-api.md` §3, §6, §7 — porta agnóstica, EVO-02, migrar vs. adicionar
- `docs/sprints/02-canal-whatsapp/README.md` — enquadramento original "migrar (ou adicionar)"
- Direção dada pelo humano (PO) em 2026-05-27
