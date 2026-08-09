# FIX — Padronizar criação do `RestClient` via Builder (uniformizar Telegram com BE-18)

> **Intake (contrato de entrada da task)**
>
> - **Origem:** revisão do PR da **BE-18** + análise do **Arquiteto** (2026-05-27). Itens consolidados pelo Arquiteto em `docs/PENDENCIAS-TECNICAS.md` (entrada "Padronizar criação do `RestClient` via Builder…") + convenção registrada em `docs/architecture/adapter-whatsapp-cloud-api.md` §5.3.
> - **Prioridade:** baixa-média. Não bloqueia BE-19; é alinhamento de convenção **antes que o próximo adapter consolide a divergência**. Pode entrar como follow-up curto da BE-18.
> - **Esforço:** **15–30 min** (estimativa do arquiteto). É troca de 3-4 linhas + comentário de convenção + rodar testes.
> - **Território / quem executa:** `financas_bot_telegram/src/main/java/.../adapters/.../config/` (o `AppConfig` que cria o `RestClient` do Telegram) → **Claude do back**.
> - **Branch:** `fix/padronizar-restclient-builder`, a partir de `develop`.
> - **Dependências:** **BE-18 em `develop`** (a referência do "padrão certo" que o Telegram vai espelhar).
> - **Riscos:** baixíssimo. É reescrita do mesmo singleton via fonte diferente; comportamento idêntico.

---

## Contexto (resumo — detalhe na entrada da PENDENCIAS)

A BE-18 introduziu o sender HTTP do WhatsApp recebendo `RestClient.Builder` auto-configurado pelo Spring Boot — em vez do padrão atual do projeto, em que `AppConfig` expõe um `RestClient` singleton via `RestClient.create()` direto. A justificativa do implementador (registrada no status da BE-18) foi viabilizar `@RestClientTest` (slice de teste que amarra o Builder a um `MockRestServiceServer`). Ele próprio classificou a divergência como "leve inconsistência".

**A análise do arquiteto mostrou que dá pra ter os dois lados:** manter o padrão "singleton no `AppConfig`" **e** preservar a capacidade de `@RestClientTest`, desde que o singleton seja **construído a partir do `RestClient.Builder` auto-configurado**, em vez de `RestClient.create()` direto. A slice intercepta o Builder; como o singleton vem dele, o mock vale pro singleton inteiro.

**Convenção alvo (já registrada na spec §5.3):** *singleton por adapter, construído a partir do `RestClient.Builder` auto-configurado pelo Spring Boot*. Vale pro Telegram (corrigir aqui), WhatsApp (já segue) e adapters futuros (Discord eventual).

## Decisão / abordagem

Refatorar a fábrica do `RestClient` do Telegram pra o padrão Builder. Trabalho mecânico — sem mudar comportamento em runtime, sem mexer no sender em si.

```java
// Antes (estado atual)
@Bean
RestClient telegramRestClient() {
    return RestClient.create(/* baseUrl/headers… */);
}

// Depois (alinhado com BE-18)
@Bean
RestClient telegramRestClient(RestClient.Builder builder) {
    return builder
        .baseUrl(/* … */)
        .defaultHeader(/* … */)
        .build();
}
```

E um comentário curto no `AppConfig` documentando a convenção, pra adapters futuros não voltarem a divergir.

## Escopo / arquivos

**Modificar:**

- O `AppConfig` (ou nome equivalente que segura o `@Bean` do `RestClient` do Telegram — implementador localiza em `adapters/.../telegram/.../config/`):
  - Substituir `RestClient.create()` direto pelo padrão Builder-via-parâmetro.
  - **Adicionar comentário curto** acima do bean, explicando a convenção (uma frase: "singleton construído a partir do `RestClient.Builder` auto-configurado — mantém compatibilidade com `@RestClientTest`; padrão da spec §5.3").

**Verificar (e modificar SE houver):**

- Testes do adapter Telegram que usam o `RestClient` — segundo o status da BE-18, *"os adapters Telegram não têm testes"*; confirmar antes de codar. Se houver, migrar pra `@RestClientTest` + `MockRestServiceServer`.

**Não tocar:**

- O sender em si (lógica de chamada HTTP) — só a fábrica do client muda.
- O adapter WhatsApp (BE-18) — já está no padrão certo.
- Qualquer outro `RestClient.create()` que **não seja do Telegram** (se houver, é discussão à parte, fora deste FIX).

## Critérios de aceitação

- [ ] `RestClient` do Telegram é construído a partir do `RestClient.Builder` injetado, **não** mais via `RestClient.create()` direto.
- [ ] Comentário curto no `AppConfig` explicitando a convenção + ponteiro pra `docs/architecture/adapter-whatsapp-cloud-api.md` §5.3.
- [ ] `mvn test` verde — sem regressão; comportamento externo idêntico.
- [ ] `mvn package -DskipTests` ok.
- [ ] **Se** houver teste do Telegram que mockava o `RestClient` antigo, migrado pra `@RestClientTest` + `MockRestServiceServer` (ou ajustado pra continuar funcionando). Documentar a decisão no status.
- [ ] Branch saiu de `develop`; território só `financas_bot_telegram/`.
- [ ] Status report `docs/sprints/02-canal-whatsapp/status/FIX-padronizar-restclient-builder.md` com frontmatter válido. Anotar:
  - Caminho exato do `AppConfig` modificado.
  - Se havia testes a migrar (ou confirmação de que não havia).

## Coordenação

- **Janela ideal:** entre as merges das tasks da overnight 2 (BE-19a, BE-18, BE-21a) e a entrada da BE-19 — porque BE-19 usará `WhatsAppMessageSenderService` (Builder) pra mensagens de feedback, e ter Telegram alinhado evita a divergência se acentuar.
- **Independente de FIX-idempotencia-porta-application.** Podem rodar em qualquer ordem.
- **Risco baixo + impacto cosmético-arquitetural** → Reviewer foca em (a) bean ainda é singleton, (b) sem regressão de testes, (c) comentário registrando a convenção.

## Observação meta (RETRO-02)

Caso diferente do `FIX-idempotencia-porta-application` — aqui o **implementador foi auto-consciente** (registrou a "leve inconsistência" no próprio status do BE-18) **e** o **Arquiteto fez a análise comparativa** que destravou a convergência. Sistema funcionou como deveria.

Esse contraste é matéria-prima boa pra RETRO-02:
- BE-19a / `data_pagamento`: drift **só** o humano pegou → sinal de gap no Reviewer.
- BE-18 / RestClient: drift **o implementador flagrou** + **Arquiteto resolveu via PENDENCIAS** → sistema operando.

O **diferencial** é: quando há sinal explícito do implementador ("leve inconsistência"), o Arquiteto consegue agir. Quando o drift é silencioso, fica dependendo do humano. Isso reforça **o que** o checklist arquitetural do Reviewer precisa cobrir: os smells silenciosos, não os já-flagrados.

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, e **revisão do Reviewer** (cosmético-arquitetural; baixo risco). Abrir PR pra `develop`; não mergear sozinho.

## Referências

- `docs/PENDENCIAS-TECNICAS.md` — entrada "Padronizar criação do `RestClient` via Builder…" (análise completa do arquiteto).
- `docs/architecture/adapter-whatsapp-cloud-api.md` §5.3 (convenção canônica registrada).
- Status report da BE-18 (justificativa original do `RestClient.Builder` + auto-classificação como "leve inconsistência").
- `docs/plans/BACKLOG-evolucao-workflow.md` item #8 (Reviewer checklist arquitetural — tópico RETRO-02).
</content>
