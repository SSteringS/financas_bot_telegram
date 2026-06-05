---
task: QA-011
titulo: "Expansão E2E — cenários positivos (foto+caption, WhatsApp, comprovante, folha)"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-04
branch_alvo: feature/qa-011-expansao-e2e
integration_branch: integration/03-folha-pagamento
prioridade: media
esforco: alto
territorio: front
estado: pronto-pra-execucao
depende_de: [QA-008, BE-030]
bloqueia: []
skills_dispatched: [qualidade-de-testes, ecossistema-frontend]
fluxos_qa: []
---

# QA-011 — Expansão E2E (foto+caption, WhatsApp, comprovante, folha de pagamento)

> **Status: pronto-pra-execucao.** Depende de **BE-030** (tornar `telegram.file.url` configurável — ver ADR 0020) estar mergeada antes de rodar Sub-área A. Sub-áreas B, C e D podem ser implementadas em paralelo enquanto BE-030 está em execução. Implementador coordena com o planner antes de despachar.

---

## Intake

- **Origem:** análise de gaps de teste em 2026-06-04 — suíte E2E atual cobre só 1 cenário positivo (fluxo feliz site) + 2 cenários negativos (webhook texto puro, webhook sticker, ambos NÃO criam pedido). Falta TODO o **caminho positivo** do produto: criar pedido via webhook, registrar pagamento via upload de comprovante, fluxo de folha de pagamento, canal WhatsApp inteiro.
- **Por quê agora:** sem cenários positivos cobertos, a suíte E2E protege contra regressão em **detalhe** mas não no **happy path principal**. Bug no fluxo "usuário manda foto com caption → pedido aparece no site" pode passar despercebido até produção.
- **Esforço:** alto (~10-14h pós-ADR, ~2 dias). 4 sub-áreas (foto+caption, WhatsApp, comprovante, folha). Cada uma destrava um caminho crítico.
- **Riscos resumidos:** Sub-área A depende de **BE-030** (URL `telegram.file.url` configurável — ADR 0020 `Proposed`). Sub-áreas B, C e D não têm dependência externa e podem começar imediatamente. WhatsApp tem decisão análoga ao Telegram (já resolvida — `WhatsAppMediaDownloaderService` é mockável via test profile). Folha de pagamento e upload de comprovante são REST puros.

---

## Contexto

A suíte E2E atual entrega **3 testes** (1 fluxo feliz site + 2 webhook negativos). Conforme o desenho original de testes (`docs/architecture/desenho-testes-automatizados.md`), a expectativa era cobrir:

- **Site:** fluxo feliz autenticado → home → modal de comprovante ✓
- **Webhook Telegram:** texto puro ✓, sticker ✓, **foto+caption** ❌ (bloqueado por mock de mídia)
- **Upload de comprovante** ❌ (registrar pagamento via foto)
- **Folha de pagamento** ❌ (cadastrar funcionário → vale → adiantamento → fechar mês)
- **WhatsApp:** todos os cenários ❌

### Por que foto+caption depende de BE-030

`frontend/e2e/fixtures/payloads-telegram.ts:11` declara:

```typescript
// TODO Fase 1.1: adicionar telegramUpdateFotoLegenda após decisão de mock (ADR 00XX)
//   Bloqueado por: decisão sobre como o back trata download de mídia em E2E
//   (§3 da spec qa-suite-e2e-fase1.md — opções: WireMock, flag skip-media, stub @Profile)
```

O fluxo real é: Telegram manda update com `photo[]` + `caption`. Back chama `getFile` na Telegram Bot API pra obter URL pública, baixa a mídia, faz upload pro S3, cria pedido.

**Decisão ADR 0020 (`Proposed`, 2026-06-04): WireMock standalone** na porta 8089. Back recebe `TELEGRAM_API_URL=http://localhost:8089/bot` e `TELEGRAM_FILE_URL=http://localhost:8089/file/bot` em E2E. Stubs JSON em `frontend/e2e/wiremock/mappings/`.

**O que falta no back (task BE-030):** `TelegramFileDownloaderService` já tem a URL do `getFile` configurável mas a URL de download do arquivo está hardcoded em `"https://api.telegram.org/file/bot"`. BE-030 adiciona `@Value("${telegram.file.url}")` e a propriedade nos arquivos `.properties`. Escopo pequeno — 1 arquivo Java + 3 arquivos de properties.

### Sub-áreas que NÃO dependem de BE-030

- **Upload de comprovante** — fluxo é UI-driven (usuário no site clica em upload → POST `/api/v1/pedidos/{id}/comprovante` com multipart). Mock do S3 já existe pro back em testes; pra E2E pode usar bucket dev real (já é a convenção).
- **Folha de pagamento** — endpoints `/api/funcionarios/*` são REST puros, sem dependência externa. Roda direto.
- **WhatsApp** — `WhatsAppMediaDownloaderService` já é mockável via configuração; padrão estabelecido pode ser reusado.

Decisão de design pra esta task: **manter atomic mesmo com bloqueio parcial**. Quando a ADR sair, as 4 sub-áreas vão juntas no mesmo PR. Alternativa (split em QA-011a e QA-011b) descartada por aumentar overhead de coordenação sem ganho real.

---

## Decisão / abordagem

### Sub-área A — Cenário foto+caption (Telegram webhook)

Adicionar 1 entrada na tabela `cenarios` de `webhook-cenarios.spec.ts`:

```typescript
{
  nome: 'foto com caption cria pedido',
  payload: telegramUpdateFotoLegenda({ fromUserId: 99, caption: '50 boleto luz' }),
  esperaPedido: true,
  asserts: (pedido) => {
    expect(pedido.valor).toBe('50.00');
    expect(pedido.descricao).toContain('luz');
    expect(pedido.tipo).toBe('BOLETO');
  },
},
```

Pré-condições:
- Factory `telegramUpdateFotoLegenda` em `payloads-telegram.ts` (existe TODO declarado, falta implementar).
- WireMock na porta 8089 com stubs em `frontend/e2e/wiremock/mappings/` (conforme ADR 0020 §3).
- BE-030 mergeada — `telegram.file.url` configurável no back.
- `loginE2E` não necessário (webhook não exige cookie).

### Sub-área B — WhatsApp webhook

Spec nova: `frontend/e2e/specs/whatsapp-cenarios.spec.ts`. Mesma estrutura parametrizada de `webhook-cenarios.spec.ts`:

```typescript
const cenarios = [
  { nome: 'texto puro', payload: whatsappUpdateTexto(...), esperaPedido: false },
  { nome: 'imagem com caption', payload: whatsappUpdateImagemLegenda(...), esperaPedido: true, asserts: ... },
  { nome: 'documento', payload: whatsappUpdateDocumento(...), esperaPedido: true, asserts: ... },
];
```

Pré-condições:
- Factories em `frontend/e2e/fixtures/payloads-whatsapp.ts` (novo arquivo, espelhando `payloads-telegram.ts`).
- Validação de signature do Meta — verificar como `MetaSignatureValidator` se comporta em E2E (provavelmente bypass via test profile já existente).
- Headers e payload conforme spec do WhatsApp Cloud API (Meta).

### Sub-área C — Upload de comprovante (registrar pagamento)

Spec nova: `frontend/e2e/specs/registrar-pagamento.spec.ts`. Fluxo UI completo:

```typescript
test('registrar pagamento: pedido pendente → upload foto → pedido vira pago', async ({ page }) => {
  // Setup: seed 1 pedido pendente
  // 1. loginE2E
  // 2. page.goto('/')
  // 3. Clicar no botão "registrar pagamento" do pedido pendente
  // 4. Upload de arquivo via input file (Playwright suporta nativo)
  // 5. Verificar feedback de sucesso
  // 6. Verificar pedido agora aparece como PAGO (refresh ou refetch)
  // 7. Asserção no banco: status = PAGO, comprovante registrado
});
```

Pré-condições:
- Fixture de imagem real em `frontend/e2e/fixtures/sample-pagamento.jpg` (~50KB).
- S3 bucket dev acessível (já é a convenção do dev local).

### Sub-área D — Folha de pagamento

Spec nova: `frontend/e2e/specs/folha-pagamento.spec.ts`. Fluxo completo:

```typescript
test('folha: cadastrar funcionário → vale → adiantamento → fechar mês', async ({ page, request }) => {
  // 1. loginE2E
  // 2. Cadastrar funcionário via UI (página FuncionariosPage)
  // 3. Abrir folha do funcionário (FolhaFuncionarioPage)
  // 4. Cadastrar vale (ValeForm)
  // 5. Cadastrar adiantamento (AdiantamentoForm)
  // 6. Fechar mês (ModalFechamento)
  // 7. Verificar fechamento aparece em FechamentosSection
  // 8. Asserção no banco: pedido FOLHA criado com breakdown correto
});
```

Pré-condições:
- `limparDadosE2E` estendido pra limpar `funcionario`, `adiantamento`, `pedidos_pagamento` de tipo FOLHA/VALE.
- Fixture pra criar funcionário base (ou rodar tudo via UI).

---

## Escopo / arquivos

### Criar

**WireMock stubs (Sub-área A):**
- `frontend/e2e/wiremock/mappings/telegram-getfile.json`
- `frontend/e2e/wiremock/mappings/telegram-getfile-error.json`
- `frontend/e2e/wiremock/mappings/telegram-download.json`
- `frontend/e2e/wiremock/__files/sample-photo.jpg` (JPEG ~1-2KB — imagem mínima pra WireMock servir)

**Specs:**
- `frontend/e2e/specs/whatsapp-cenarios.spec.ts` (Sub-área B)
- `frontend/e2e/specs/registrar-pagamento.spec.ts` (Sub-área C)
- `frontend/e2e/specs/folha-pagamento.spec.ts` (Sub-área D)

**Fixtures:**
- `frontend/e2e/fixtures/payloads-whatsapp.ts` (Sub-área B)
- `frontend/e2e/fixtures/sample-pagamento.jpg` (Sub-área C — imagem pequena pra teste de upload)

### Modificar

- `frontend/e2e/specs/webhook-cenarios.spec.ts` — adicionar cenário foto+caption (Sub-área A, requer BE-030 mergeada).
- `frontend/e2e/fixtures/payloads-telegram.ts` — implementar `telegramUpdateFotoLegenda` (Sub-área A).
- `frontend/e2e/fixtures/banco.ts` — estender `limparDadosE2E` pra tabelas `funcionario`, `adiantamento` e pedidos FOLHA/VALE (Sub-área D).
- `frontend/e2e/scripts/subir-stack.ts` — adicionar start/stop/healthcheck do WireMock na porta 8089 (Sub-área A).
- `frontend/.env.e2e.example` — adicionar `TELEGRAM_API_URL=http://localhost:8089/bot` e `TELEGRAM_FILE_URL=http://localhost:8089/file/bot`.

### Não tocar (escopo limitado)

- **Código de produto** (`frontend/src/`, `financas_bot_telegram/src/main/`) — as mudanças no back (URL configurável) são escopo de BE-030, **não desta task**.
- **Specs existentes** (`site-fluxo-feliz`, parte sem alteração de `webhook-cenarios`) — não refatorar.

---

## Testes

A própria task entrega testes. Estimativa:

- **Sub-área A** (foto+caption): +1 test.
- **Sub-área B** (WhatsApp): 3 cenários parametrizados = +3 tests.
- **Sub-área C** (comprovante): 1 fluxo UI completo + 1 cenário de erro (upload de arquivo inválido) = +2 tests.
- **Sub-área D** (folha): 1 fluxo completo + 1 cenário de erro (fechar mês duplicado) = +2 tests.

**`testes_novos` esperado:** 8 testes E2E.
**`testes_total` esperado pós-task:** 11 testes E2E (era 3).

Critério duro: `npm run e2e:full` verde **3 vezes consecutivas** em ambiente limpo (mesma régua do QA-008).

---

## Critérios de aceitação

> **Pré-condição Sub-área A:** BE-030 mergeada em `integration/03-folha-pagamento` antes de implementar o cenário foto+caption. Sub-áreas B, C, D podem ser implementadas sem essa pré-condição.

- [ ] 3 specs novas criadas (`whatsapp-cenarios`, `registrar-pagamento`, `folha-pagamento`).
- [ ] Factory `telegramUpdateFotoLegenda` implementada em `payloads-telegram.ts` conforme ADR 0020.
- [ ] Factories WhatsApp implementadas em `payloads-whatsapp.ts` (texto, imagem+caption, documento).
- [ ] `webhook-cenarios.spec.ts` ganha 1 entrada na tabela `cenarios` (foto+caption) com `esperaPedido: true`.
- [ ] Fixture `sample-pagamento.jpg` commitada (~50KB).
- [ ] `limparDadosE2E` em `banco.ts` estendido pra cobrir folha + funcionário.
- [ ] `npm run e2e:full` verde com **11 testes** (3 anteriores + 8 novos).
- [ ] `npm run e2e:full` verde **3 vezes consecutivas** em ambiente limpo.
- [ ] Tempo total da suíte ≤ 60s (alvo — ajustar se realista mais alto).
- [ ] Zero mudança em código de produto, salvo o que a ADR exigir (se exigir).
- [ ] Branch: `feature/qa-011-expansao-e2e` saindo da branch correta.
- [ ] Status report com frontmatter válido. `testes_novos: 8`. Corpo lista por sub-área.

---

## Fora de escopo (explicitamente)

- **A ADR de mock Telegram** — é trabalho do arquiteto, não desta task. Esta task só consome.
- **Schemathesis (contract test)** — Fase 2, abrir QA-NNN separado quando esta estiver liso.
- **Specs de cenários negativos exaustivos** (payload inválido, autorização cruzada IDOR, filtro vazio) — vira QA-NNN futura após esta.
- **Testes de carga ou stress** — fora.
- **Cobertura E2E do canal admin (`/admin/api/*`)** — fora desta task. Auth via API key, fluxo curto, não é prioritário.
- **Testes E2E de erros HTTP genéricos** (404, 500 do front) — fora.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| ADR de mock demorar a ser tomada | Alta | Alto (task fica parada) | Critério `estado: rascunho` + bloqueio explícito; planner pode reabrir prioridade |
| Decisão da ADR escolher opção que invade código de produto (Opção C — @Profile) | Média | Médio (esta task vira BE+QA, não só QA) | Se acontecer, esta task descreve o que QA precisa; tasks BE separadas implementam o stub no back |
| Upload de arquivo no Playwright ser flaky | Baixa | Médio | Padrão `setInputFiles` é estável; usar `await page.locator('input[type=file]').setInputFiles(path)` |
| Folha E2E quebrar por race entre seed e UI | Baixa | Médio | Já mitigado em QA-008 com `workers: 1`; cleanup estendido garante estado limpo |
| Fixture de imagem ser grande demais e poluir repo | Baixa | Baixo | Manter ≤ 50KB; usar imagem pequena gerada (`convert -size 100x100 sample.jpg`) |
| WhatsApp signature validation falhar no E2E | Média | Médio | Verificar se `MetaSignatureValidator` tem bypass em test profile; se não, abrir BE-NNN pra adicionar |

---

## Coordenação

- **Pode rodar em paralelo com:** QA-009 (back), QA-010 (front) — pasta E2E é território próprio. Tasks BE/FE de feature que não toquem em `frontend/e2e/`.
- **Depende sequencialmente de:** QA-008 (suíte estável) + ADR de mock Telegram (a ser criada).
- **Bloqueia:** QA-006 (pre-merge gate) — sem cenários positivos cobertos, o gate é fraco. Idealmente QA-006 espera QA-011 fechar.
- **Atenção pro arquiteto** (antes desta task começar): redigir ADR de mock Telegram, escolher entre WireMock / flag skip-media / @Profile stub.
- **Atenção pro Reviewer:** verificar que (a) zero mudança em produto exceto o que a ADR autorizou; (b) cleanup limpa exclusivamente dados do requisitante 99 + funcionários de teste; (c) 3x verde consecutivas no `e2e:full`; (d) factories WhatsApp respeitam o schema real do Meta Cloud API.
- **Após merge:** QA-006 (pre-merge gate) pode ser priorizado — agora faz sentido como gate real.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido conforme `_TEMPLATE-status.md`, revisão do Reviewer.

`fluxos_qa: []` — task de tooling pura, sem fluxo de produto a validar via QA externo.

---

## Referências

- Análise de gaps original — conversa qa-test-specialist com humano em 2026-06-04.
- `docs/architecture/desenho-testes-automatizados.md` — desenho original que previa esses cenários como Fase 1.1/2.
- `docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md` §3 — análise das 3 opções de mock de mídia Telegram.
- `frontend/e2e/fixtures/payloads-telegram.ts:11` — TODO declarado que esta task fecha.
- Modelos referenciais:
  - `frontend/e2e/specs/site-fluxo-feliz.spec.ts` (para `registrar-pagamento.spec.ts` e `folha-pagamento.spec.ts`)
  - `frontend/e2e/specs/webhook-cenarios.spec.ts` (para `whatsapp-cenarios.spec.ts`)
  - `frontend/e2e/fixtures/payloads-telegram.ts` (para `payloads-whatsapp.ts`)
- **ADR 0020** `docs/decisions/0020-mock-telegram-api-wiremock-e2e.md` — decisão de mock Telegram: WireMock standalone + URL configurável.
- `docs/decisions/0017-prefixo-qa-tasks-tooling-qualidade.md` — convenção do prefixo QA-NNN.
