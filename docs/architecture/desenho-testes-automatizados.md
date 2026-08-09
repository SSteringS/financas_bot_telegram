# Desenho — Testes Automatizados

Documento de arquitetura da estratégia de testes automatizados do projeto. Cobre o estado atual (baseline), o estado-alvo (pirâmide completa), as ferramentas — com explicação didática pra quem não conhece —, fluxogramas de execução, estratégia de isolamento de dados e roadmap de implementação em fases.

> **Status:** proposta consolidada com o humano em 2026-06-01. Aberto a refinamento.
> **Decisões base:** stack local (sem Docker Compose, sem CI), requisitante dedicado id=99, smoke MVP com 3 specs cobrindo 4 tipos de cobertura.
>
> **Atualização 2026-06-01 (arquiteto, pós-revisão para servir ao planner):**
> - §7.2 / §8.2 — nomes reais das tabelas (`pedidos_pagamento`, `comprovantes`) corrigidos a partir do `estado-atual-dev.md` §4.
> - §10 — removido item "Migration ou endpoint admin pra requisitante 99" (eliminado pela Decisão 8); referência à quebra concreta em tasks `QA-NNN` na spec efêmera da sprint.
> - §13 — decisões pendentes reclassificadas por urgência (bloqueador da Fase 1 vs. evoluções da Fase 2).
> - Acoplamento com **ADR 0017 `Accepted`** (prefixo `QA-NNN`, homologado 2026-06-01): a quebra em tasks usa o prefixo `QA-NNN`. Atualizações downstream em CLAUDE.md raiz + templates ficam por conta do planner.

---

## 1. Por que este desenho existe

Hoje o projeto tem dois mundos de teste isolados:

- **Backend testa backend** — unitários (~52 arquivos) + integração com Testcontainers (7 suites). Cobre tudo dentro da JVM até o banco MySQL real.
- **Frontend testa frontend** — Vitest + React Testing Library + MSW (12 arquivos). Cobre componentes, hooks e fluxos de UI **com a API mockada**.

Mas **nada testa os dois juntos automaticamente**. Quando o back muda um contrato (rename de campo, mudança de código HTTP, novo cabeçalho), o front pode quebrar sem ninguém perceber até o deploy. Hoje, a única forma de descobrir é rodando manualmente o `ROTEIRO-INTEGRACAO-FRONT-BACK.md` — 45 a 60 minutos de execução manual.

Este desenho fecha esse buraco com uma suíte E2E (end-to-end) automatizada que roda em ~2 minutos e pode ser disparada sempre que o backend muda ou antes de mergear pra `develop`.

---

## 2. Pirâmide-alvo

```
                            ┌──────────────────────────────────────┐
                            │  E2E site (Playwright)               │  ← NOVO
                            │  E2E webhook (POST /webhook)         │  ← NOVO
                            │  Acessibilidade (axe via Playwright) │  ← NOVO
                            ├──────────────────────────────────────┤
                            │  Contract OpenAPI (Schemathesis)     │  ← NOVO (fase 2)
                            ├──────────────────────────────────────┤
                            │  Integração back (Testcontainers)    │  ← EXISTE (7 suites)
                            │  Componente front (Vitest+RTL+MSW)   │  ← EXISTE (12 testes)
                            ├──────────────────────────────────────┤
                            │  Unitário back (JUnit+Mockito)       │  ← EXISTE (~52 testes)
                            │  Unitário front (Vitest)             │  ← EXISTE (parte dos 12)
                            └──────────────────────────────────────┘

                  Mais lento, mais caro, menos testes ↑
                  Mais rápido, mais barato, mais testes ↓
```

**Princípio:** a base larga (unitários + integração + componente) é o que protege o dia-a-dia. Os testes do topo (E2E + a11y + contract) cobrem o que a base **não pega**: divergência front↔back real, regressão de fluxo de uso, contrato de API.

A pirâmide tem essa forma por uma razão econômica: testes rápidos rodam centenas de vezes por dia (a cada save, a cada commit). Testes lentos rodam algumas vezes por dia. Se invertermos a pirâmide (E2E para tudo), o ciclo de feedback fica intolerável.

---

## 3. Como decidir que tipo de teste escrever

Fluxograma de decisão pra cada feature ou mudança:

```
Tenho uma nova feature/mudança. Que teste escrever?
                       │
                       ▼
       ┌────────────────────────────────────┐
       │ É lógica pura (parser, mapper,     │   sim
       │ calculadora, validador)?           │  ─────►  Unitário (JUnit / Vitest)
       └────────────────┬───────────────────┘
                        │ não
                        ▼
       ┌────────────────────────────────────┐
       │ Toca query JPA, migration Flyway,  │   sim
       │ ou fluxo HTTP completo dentro da    │  ─────►  Integração (Testcontainers)
       │ JVM?                                │
       └────────────────┬───────────────────┘
                        │ não
                        ▼
       ┌────────────────────────────────────┐
       │ É componente React isolado, hook,  │   sim
       │ ou interação de UI sem precisar    │  ─────►  Vitest + RTL + MSW
       │ do back real?                       │
       └────────────────┬───────────────────┘
                        │ não
                        ▼
       ┌────────────────────────────────────┐
       │ Fluxo do usuário no navegador      │   sim
       │ falando com o back real?            │  ─────►  Playwright (E2E site)
       └────────────────┬───────────────────┘
                        │ não
                        ▼
       ┌────────────────────────────────────┐
       │ Canal de entrada (Telegram/WA)     │   sim
       │ chegando no back?                   │  ─────►  E2E webhook (POST /webhook)
       └────────────────┬───────────────────┘
                        │ não
                        ▼
       ┌────────────────────────────────────┐
       │ Validar que o back não quebrou o   │   sim
       │ contrato OpenAPI?                   │  ─────►  Schemathesis (fase 2)
       └────────────────┬───────────────────┘
                        │ não
                        ▼
       ┌────────────────────────────────────┐
       │ Acessibilidade de uma página?      │   sim
       │                                     │  ─────►  @axe-core/playwright
       └────────────────┬───────────────────┘
                        │ não
                        ▼
                 [Revisar: é teste mesmo?]
```

---

## 4. Glossário de ferramentas — explicação pra quem não conhece

Cada ferramenta da pirâmide com: **o que é**, **como funciona conceitualmente**, **o que ela pega**, **o que ela NÃO pega**, e **exemplo curto**.

### 4.1. JUnit 5 + Mockito (backend, unitários) — JÁ EXISTE

**O que é:** framework de testes do Java. JUnit roda os testes; Mockito cria objetos falsos ("mocks") pra substituir dependências.

**Como funciona:** você escreve uma classe `XPTOTest.java` com métodos anotados `@Test`. Dentro, instancia a classe que quer testar, substitui as dependências reais (banco, S3, Telegram) por mocks, chama o método, e verifica o resultado com `assertThat(...)`.

**O que pega:** bugs de lógica dentro de uma classe isolada — cálculos errados, ramificações de `if`, exceções não tratadas, regras de parse.

**O que NÃO pega:** problemas que só aparecem quando classes reais conversam (configuração do Spring errada, query JPA inválida, mapeamento entidade ↔ tabela quebrado).

**Exemplo:**
```java
@Test
void parseLegenda_quandoFormatoInvalido_lancaInvalidMessageFormatException() {
    var parser = new LegendaParser();
    assertThatThrownBy(() -> parser.parse("oi"))
        .isInstanceOf(InvalidMessageFormatException.class);
}
```

### 4.2. MockMvc (backend, controllers HTTP) — JÁ EXISTE

**O que é:** ferramenta do Spring que simula requests HTTP sem subir um servidor real. Você manda um "fake POST /api/v1/auth/exchange" e mede o que o controller responde.

**Como funciona:** o Spring constrói uma fatia leve da aplicação (só a parte web), o MockMvc dispara requests sintéticos contra ela, sem rede e sem porta TCP. Tudo na mesma JVM, rápido (~50ms por request).

**O que pega:** binding de DTOs (request JSON ↔ classe), validações (`@Valid`), códigos HTTP, headers, mapeamento de URL.

**O que NÃO pega:** problemas no banco (queries reais não rodam), CORS de verdade (filter chain pode estar parcial), comportamento de cookies entre requests.

**Exemplo:** está em vários arquivos do projeto, ex: `AuthControllerTest.java`.

### 4.3. Testcontainers (backend, integração com banco real) — JÁ EXISTE

**O que é:** biblioteca que sobe **contêineres Docker reais** dentro do teste — um MySQL de verdade — só pra aquela execução.

**Como funciona:** quando o teste começa, Testcontainers pede pro Docker subir um container `mysql:8.0` numa porta aleatória. O Spring conecta nele. Flyway aplica todas as migrations. Os testes rodam contra esse MySQL "descartável". No final, o container é destruído.

**O que pega:** queries JPA quebradas (dialect diferente do H2), migration Flyway com bug, problemas de transação, comportamento de índice, constraints de unicidade.

**O que NÃO pega:** problemas de produção (RDS tem network policy, IAM, latência diferente), interação com S3 real, integração com Telegram.

**Pré-requisito:** Docker Desktop precisa estar rodando localmente. Primeira execução baixa a imagem MySQL (~600MB), depois é cache.

**Exemplo:** classe base `AbstractIntegrationTest.java`.

### 4.4. Vitest (frontend, runner) — JÁ EXISTE

**O que é:** framework de testes do mundo Node/Vite. É o "JUnit do front". Sucessor moderno do Jest, mais rápido porque reaproveita o pipeline do Vite.

**Como funciona:** procura arquivos `*.test.ts` / `*.test.tsx`, executa cada `it(...)` ou `test(...)`, mostra verde/vermelho. Pode rodar com ambiente `jsdom` ou `happy-dom` (que simula o DOM do navegador) pra testes de componente.

**O que pega:** lógica pura de funções (formatadores, parsers, utils), comportamento de componentes React com DOM simulado, hooks customizados.

**O que NÃO pega:** comportamento real do navegador (CSS de verdade, eventos do browser, performance), integração com back real.

**Exemplo:** `frontend/src/lib/formato.test.ts` — testa que `formatarReais(287.5)` retorna `"R$ 287,50"`.

### 4.5. React Testing Library (frontend, componente) — JÁ EXISTE

**O que é:** biblioteca que ajuda a testar componentes React **da perspectiva do usuário**, não dos detalhes internos.

**Filosofia:** "encontre elementos pelo que o usuário vê" (texto, role, label) em vez de "encontre o elemento pelo id ou pela classe CSS". Isso deixa os testes resilientes a refactors — se você mudar a classe `<div class="pedido-card">` mas o texto "R$ 287,50" continuar visível, o teste continua passando.

**Como funciona:** renderiza um componente em memória (no jsdom), usa `screen.getByText(...)`, `screen.getByRole('button', {name: 'Salvar'})` pra encontrar elementos, e `userEvent.click(...)` pra simular interação. Verifica o resultado com `expect(...).toBeVisible()`.

**O que pega:** componente renderiza errado, clique não dispara o handler, formulário não submete, mensagem de erro não aparece.

**O que NÃO pega:** estilo visual (CSS), eventos reais do browser (scroll inertial, drag-and-drop nativo), integração com back real.

**Exemplo:** `frontend/src/components/PedidoCard.test.tsx`.

### 4.6. MSW (Mock Service Worker) — JÁ EXISTE

**O que é:** biblioteca que **intercepta requests HTTP do front** e devolve respostas falsas, sem precisar de servidor de back rodando.

**Como funciona:** você define handlers tipo "quando alguém fizer `GET /api/v1/pedidos`, devolva esse JSON aqui". O MSW intercepta a request no nível do navegador (via service worker) ou no Node (durante testes). Pro código do front, parece que a API real respondeu.

**Por que isso é importante:** permite que o front rode em desenvolvimento e em teste **sem o back estar de pé**. Pedro Marques sempre tem 3 pedidos no banco-fake do MSW; o desenvolvedor não precisa subir Docker, MySQL, Spring Boot só pra trabalhar no front.

**O que pega:** que o front consome o contrato correto da API (o que ele *espera*).

**O que NÃO pega:** que o back realmente entrega esse contrato. **Esse é exatamente o gap que o E2E vai cobrir.**

**Exemplo:** `frontend/src/mocks/handlers.ts` (já existe).

### 4.7. Playwright (E2E navegador) — **NOVO**

**O que é:** ferramenta que controla um navegador real (Chromium, Firefox ou WebKit) automaticamente. Você escreve um script "vai pra `/`, clique no botão Entrar, digite isso, clique Enviar, verifique que apareceu tal texto" — e o Playwright faz isso num navegador headless (sem janela visível).

**Como funciona:** sobe um Chromium em modo headless. Cada teste abre uma **aba isolada** (sem cookies de outro teste). Você usa a API `page.goto(...)`, `page.getByText(...).click()`, `expect(page.getByText(...)).toBeVisible()`. Por baixo, o Playwright fala com o Chromium via protocolo DevTools.

**Por que Playwright e não Cypress ou Selenium:**
- Mais rápido que Selenium (sem driver intermediário).
- Mais robusto que Cypress (suporte oficial a múltiplos navegadores, auto-wait nativo sem `cy.wait` hardcoded).
- Suporte oficial a TypeScript.
- Captura **screenshots e vídeos** automáticos quando um teste falha, ajudando muito o debug.
- Gera "traces" navegáveis (Playwright Trace Viewer) que mostram cada passo com a tela.

**O que pega:** divergência front↔back real, regressão de fluxo de uso, cookie não persiste, CORS quebrado, redirect errado, componente que não aparece com dados reais.

**O que NÃO pega:** problemas que só acontecem com volume real (carga), problemas de produção (cert, IP, IAM), regressão visual fina (cor errada num botão — pra isso existe screenshot testing).

**Comando típico:**
```bash
npx playwright test                  # roda tudo headless
npx playwright test --ui             # roda com UI navegável (debug interativo)
npx playwright show-report           # abre relatório HTML com screenshots/vídeos
```

### 4.8. @axe-core/playwright (acessibilidade) — **NOVO**

**O que é:** plugin que injeta o **axe-core** (motor de auditoria de acessibilidade da Deque, padrão da indústria) dentro do navegador controlado pelo Playwright. Analisa a página renderizada e devolve uma lista de violações de WCAG (Web Content Accessibility Guidelines).

**Como funciona:** depois que o Playwright navegou pra uma página, você chama `await new AxeBuilder({page}).analyze()`. O plugin executa o axe-core na DOM real, recebe um relatório, e você verifica `expect(violations).toEqual([])`.

**Por que isso importa pro projeto:** o usuário final (Pedro) é uma pessoa específica que vai acessar o site no celular. Pequenas regressões de acessibilidade — contraste, labels, ordem de foco — afetam a usabilidade dele direto. axe-core pega isso automaticamente.

**O que pega:** alt text faltando em imagem, contraste insuficiente, botão sem label acessível, heading order errado, formulário sem `<label>`, ARIA atributos inválidos.

**O que NÃO pega:** problemas semânticos que precisam de julgamento humano (linguagem clara, ordem lógica do conteúdo). Não substitui auditoria humana, complementa.

**Exemplo:**
```typescript
const violations = (await new AxeBuilder({ page }).analyze()).violations;
expect(violations.filter(v => v.impact !== 'minor')).toEqual([]);
```

### 4.9. Schemathesis (contract OpenAPI) — **NOVO, FASE 2**

**O que é:** ferramenta de "property-based testing" que lê o **OpenAPI** gerado pelo Springdoc e **gera milhares de requests sintéticos** contra a API pra descobrir divergências entre o contrato e a implementação.

**Como funciona:** lê `http://localhost:8080/v3/api-docs`, vê todos os endpoints documentados, e dispara requests com inputs aleatórios respeitando os schemas declarados. Pra cada resposta, verifica: o código HTTP está documentado? O body bate com o schema declarado? Headers obrigatórios estão presentes? Se algo divergir, falha.

**Por que vale a pena:** o front consome o OpenAPI via `openapi-typescript` (ver script `gen:types` no `package.json`). Se o back devolver algo que diverge do schema, o front pode tratar mal. Schemathesis pega isso antes do front estourar em produção.

**Por que ficou na fase 2:** Schemathesis exige Python no ambiente do agente, e o ganho marginal sobre o `openapi-typescript` (que já força a divergência a aparecer no `tsc -b` do front) é médio. Vale a pena quando o contrato ficar maior. Hoje, o tipo gerado já cobre 80%.

**Comando típico:**
```bash
pipx install schemathesis
schemathesis run http://localhost:8080/v3/api-docs --base-url http://localhost:8080
```

---

## 5. Decisões arquiteturais consolidadas

### 5.1. Decisões de arquitetura base

| # | Decisão | Por quê |
|---|---|---|
| 1 | **Stack local, sem Docker Compose, sem CI** | Humano mantém MySQL local sempre rodando. Agente sobe back+front via script Node. Roda sob demanda. Simplifica drasticamente; aceita "regressão pode passar batido se ninguém rodar" como dívida explícita. |
| 2 | **Requisitante dedicado `id=99 "E2E Tester"`** | Isolamento total dos dados reais do Pedro (`id=1`). Cleanup deleta APENAS `WHERE requisitante_id=99`. Sem risco de corromper banco dev. |
| 3 | **Smoke MVP: 3 arquivos de spec, 4 tipos de cobertura** | Levanta a infra de E2E com investimento mínimo. Prova o conceito. Expansão de cenários vira tarefa de sprint futura. |
| 4 | **Playwright + Chromium apenas (pra começar)** | Pedro usa Chrome no celular (PWA). Outros browsers entram quando a base de usuários crescer. |
| 5 | **Webhook E2E via Playwright `request` (não browser)** | POST direto no `/webhook` é mais rápido que orquestrar Telegram fake via browser. Mesma infra do site E2E (Playwright), sem precisar de Testcontainers no front. |
| 6 | **Cleanup em `beforeEach`, nunca em `afterAll`** | Garante que cada teste começa limpo, mesmo se o anterior crashar. |
| 7 | **Schemathesis fica fora do MVP (fase 2)** | `openapi-typescript` já age como contract test passivo. Schemathesis adiciona valor mas tem custo de setup (Python). |

### 5.2. Decisões resolvidas em 2026-06-01 (rodada de refinamento)

| # | Decisão | Escolha | Por quê |
|---|---|---|---|
| 8 | **Como criar o requisitante 99** | Helper TS direto via `mysql2/promise` no `e2e/fixtures/banco.ts` (`INSERT IGNORE` antes dos testes, `DELETE WHERE requisitante_id=99` no `beforeEach`) | Zero código novo no back. Mantém toda infra de teste no escopo do front. Front já vai conhecer o schema pra fazer cleanup mesmo. |
| 9 | **Onde guardar credenciais MySQL/admin** | `frontend/.env.e2e` (gitignored) + `frontend/.env.e2e.example` (versionado como template) | Segue padrão Vite. Alinhado com convenção do projeto: secrets de dev local não vão pro git (`application-dev.properties` já está no `.gitignore`). |
| 10 | **Quem roda `e2e:full` no ciclo de PR** | Combinação **E + A + D**: (E) plano da task declara `exige_e2e: true|false` por critério; (A) implementador roda quando exigido; (D) Reviewer não roda, audita pelo status report | Mantém Reviewer somente-leitura (defendido por ADR 0005). Foca custo onde o risco existe. Independência da verificação via status report estruturado. |
| 11 | **Threshold de a11y** | Falha em `serious` + `critical`. Tolera `minor` + `moderate` no início. | Pedro como usuário único, site pequeno. Bar baixa o suficiente pra raros falsos positivos, alta o suficiente pra cobrar regressão real. Pode evoluir pra `moderate+` em sprint futura. |
| 12 | **Reportagem de resultados** | HTML local do Playwright + bloco `e2e_full` no frontmatter do status report (+ seção textual com resumo) | Casa com ADR 0007 (status report como output contract). Reviewer audita pelo frontmatter. Não commitar `playwright-report/` (artefato gerado, blobs binários). |
| 13 | **Cenários de webhook no MVP** | Spec parametrizada (`describe.each` ou `forEach`) com tabela de cenários — MVP começa com 2-3 (happy path, texto puro, possivelmente sticker); Fase 2 amplia trivialmente | Padrão extensível desde o início. Custo marginal por cenário é 1 linha. Cobre regressão do BE-15 (handler genérico) já no MVP. |

### 5.3. Critério de gating do `e2e:full` (decisão 10, dimensão E)

Plano de task declara `exige_e2e_full: true` quando **qualquer** condição se aplica:

- Toca controller/DTO REST exposto em `/api/v1/*`
- Toca filter/security/auth (cookie, JWT, exchange, /me)
- Toca migration Flyway de tabela exposta via API (pedido, comprovante, requisitante, auth_token)
- Toca componente React que consome API real (via `useQuery` / cliente HTTP)
- Toca código do webhook (`TelegramWebhookController`, `WhatsAppWebhookController`, strategies, parsers de legenda)

Declara `exige_e2e_full: false` quando a mudança é:

- Refactor interno sem mudar contrato (rename de variável, extração de método, etc.)
- Estilo visual puro (CSS, Tailwind) sem mudar markup acessível
- Documentação, ADR, plano, status report
- Infra (Terraform, GitHub Actions, systemd) sem mudar comportamento da aplicação
- Testes (adicionar/refatorar testes não exige rodar a suíte E2E inteira de novo)

---

## 6. Arquitetura de execução

### 6.1. Estrutura de pastas proposta

```
frontend/
├── .env.e2e                              ← gitignored — credenciais reais (decisão 9)
├── .env.e2e.example                      ← versionado — template
├── e2e/                                  ← NOVO — toda a suíte E2E
│   ├── playwright.config.ts              ← config: baseURL, timeout, projects, traces, globalSetup
│   ├── fixtures/
│   │   ├── banco.ts                      ← helpers SQL: garantirRequisitanteE2E, limpar, semear
│   │   ├── auth.ts                       ← helper: gerar convite + exchange → cookie
│   │   ├── global-setup.ts               ← roda 1x antes de tudo: INSERT IGNORE requisitante 99
│   │   └── payloads-telegram.ts          ← factories de payloads sintéticos pra webhook
│   ├── specs/
│   │   ├── site-fluxo-feliz.spec.ts      ← MVP — login → home → detalhe → comprovante
│   │   ├── webhook-cenarios.spec.ts      ← MVP — parametrizado (happy + sad paths)
│   │   └── a11y-home.spec.ts             ← MVP — axe nas páginas autenticadas
│   ├── scripts/
│   │   ├── subir-stack.ts                ← spawn back + front, espera healthcheck
│   │   ├── derrubar-stack.ts             ← SIGTERM nos processos
│   │   └── aguardar-saude.ts             ← polling de healthcheck (util)
│   └── tsconfig.json                     ← tsconfig isolado pro escopo E2E
└── package.json                          ← scripts: e2e, e2e:full, e2e:ui, e2e:report
```

### 6.2. Novos scripts no `package.json`

```json
{
  "scripts": {
    "e2e": "playwright test",
    "e2e:full": "tsx e2e/scripts/subir-stack.ts && (playwright test; tsx e2e/scripts/derrubar-stack.ts)",
    "e2e:ui": "playwright test --ui",
    "e2e:report": "playwright show-report"
  }
}
```

`tsx` roda TypeScript direto sem compilar. Alternativa: `ts-node`.

### 6.3. Fluxograma — execução do `npm run e2e:full`

```
┌─────────────────────────────────┐
│ npm run e2e:full                │
└────────────────┬────────────────┘
                 │
                 ▼
        ┌────────────────────┐
        │ subir-stack.ts     │
        └─────────┬──────────┘
                  │
                  ▼
   ┌──────────────────────────────┐
   │ MySQL local respondendo em   │── não ──┐
   │ localhost:3306?              │         │
   └──────────────┬───────────────┘         ▼
                  │ sim         ┌─────────────────────────────────┐
                  ▼             │ ERRO: sobe seu MySQL local e    │
   ┌──────────────────────────┐ │ rode de novo. Exit 1.           │
   │ requisitante id=99 existe│ └─────────────────────────────────┘
   │ no banco?                │
   └──────────────┬───────────┘
                  │ não ───► INSERT INTO requisitante (id=99, ...)
                  │ sim
                  ▼
     ┌──────────────────────────────┐
     │ spawn `mvnw spring-boot:run  │
     │ -Dspring-boot.run.profiles=  │
     │ dev` (background)            │
     └──────────────┬───────────────┘
                    │
                    ▼
     ┌──────────────────────────────────┐
     │ poll GET /actuator/health        │
     │ até retornar 200 UP (timeout 60s)│
     └──────────────┬───────────────────┘
                    │
                    ▼
     ┌──────────────────────────────┐
     │ spawn `vite` (background)    │
     └──────────────┬───────────────┘
                    │
                    ▼
     ┌──────────────────────────────────┐
     │ poll GET http://localhost:5173/  │
     │ até retornar 200 (timeout 30s)   │
     └──────────────┬───────────────────┘
                    │
                    ▼
         ┌──────────────────────┐
         │ playwright test      │
         └──────────┬───────────┘
                    │
        ┌───────────┴────────────┐
        ▼                        ▼
  ┌──────────┐            ┌──────────┐
  │ verde ✓  │            │ vermelho │
  └────┬─────┘            │ + screen │
       │                  │ shots    │
       │                  └─────┬────┘
       │                        │
       └────────┬───────────────┘
                ▼
       ┌────────────────────────┐
       │ derrubar-stack.ts      │
       │ SIGTERM no back        │
       │ SIGTERM no front       │
       │ (try/finally garante)  │
       └────────────┬───────────┘
                    ▼
            ┌───────────────┐
            │ exit 0 ou 1   │
            └───────────────┘
```

**Pontos críticos:**
- `try/finally` no Node garante que back e front são derrubados mesmo se o `playwright test` lançar exceção.
- Se MySQL não estiver rodando, falha rápido com mensagem clara (não fica travando esperando).
- Se o back demorar mais de 60s pra responder (timeout configurável), aborta com log.

---

## 7. Estratégia de fixtures de banco

> **Decisão 8 aplicada:** sem migration Flyway, sem endpoint admin. Toda a infra de fixtures vive no escopo do front, via helper TS que fala com MySQL direto.
> **Decisão 9 aplicada:** credenciais via `.env.e2e` (gitignored) + `.env.e2e.example` (versionado como template).

### 7.1. Config — `frontend/.env.e2e.example` (versionado)

```bash
# Template de configuração da suíte E2E.
# Copie pra .env.e2e e preencha com seus valores de dev local.
# .env.e2e está no .gitignore — credenciais nunca vão pro git.

E2E_DB_HOST=localhost
E2E_DB_PORT=3306
E2E_DB_USER=root
E2E_DB_PASSWORD=<sua-senha-mysql-local>
E2E_DB_NAME=financas_bot_telegram_db

E2E_ADMIN_KEY=<copiar-de-application-dev.properties>
E2E_BACKEND_URL=http://localhost:8080
E2E_FRONTEND_URL=http://localhost:5173
```

### 7.2. Helper `e2e/fixtures/banco.ts` — garante requisitante 99, limpa e semeia

```typescript
import { createConnection, Connection } from 'mysql2/promise';
import 'dotenv/config'; // carrega .env.e2e via DOTENV_CONFIG_PATH

let conn: Connection | null = null;

async function getConn(): Promise<Connection> {
  if (!conn) {
    conn = await createConnection({
      host: process.env.E2E_DB_HOST!,
      port: Number(process.env.E2E_DB_PORT),
      user: process.env.E2E_DB_USER!,
      password: process.env.E2E_DB_PASSWORD!,
      database: process.env.E2E_DB_NAME!,
    });
  }
  return conn;
}

/** Garante que o requisitante de teste exista. Idempotente. */
export async function garantirRequisitanteE2E() {
  const c = await getConn();
  await c.query(
    `INSERT IGNORE INTO requisitante (id, nome, telefone, email, ativo, criado_em)
     VALUES (99, 'E2E Tester', '+5511000000099', 'e2e@test.local', true, NOW())`
  );
}

/** Limpa SÓ o que pertence ao requisitante de teste (id=99). */
// NOTA (correção 2026-06-01): nomes reais das tabelas no schema atual são
// `pedidos_pagamento` (não `pedido`) e `comprovantes` (não `comprovante`).
// Veja `docs/architecture/estado-atual-dev.md` §4 e a migração V6 da sprint 03 (BE-023).
export async function limparDadosE2E() {
  const c = await getConn();
  await c.query(`DELETE FROM comprovantes WHERE pedido_id IN
                  (SELECT id FROM pedidos_pagamento WHERE requisitante_id = 99)`);
  await c.query(`DELETE FROM pedidos_pagamento WHERE requisitante_id = 99`);
  await c.query(`DELETE FROM auth_token WHERE requisitante_id = 99`);
  await c.query(`DELETE FROM mensagem_processada WHERE telegram_user_id = '99'`);
}

export interface PedidoFake {
  descricao: string;
  valor: number;
  status: 'PENDENTE' | 'PAGO';
  tipo: 'BOLETO' | 'PIX' | 'TED' | 'AGENDAMENTO' | 'OUTRO';
  dataPedido: string;        // YYYY-MM-DD
  sKeyComprovante?: string;  // opcional, só se PAGO
}

export async function semearPedidos(pedidos: PedidoFake[]) {
  const c = await getConn();
  for (const p of pedidos) {
    // Tabela real: `pedidos_pagamento` (ver `estado-atual-dev.md` §4).
    // Colunas reais: `data_criacao` (DATETIME) em vez de `data_pedido` (DATE).
    // Ajustar nomes conforme o schema atual da sprint que vai implementar.
    await c.query(
      `INSERT INTO pedidos_pagamento (requisitante_id, descricao, valor, status, tipo, data_criacao)
       VALUES (99, ?, ?, ?, ?, NOW())`,
      [p.descricao, p.valor, p.status, p.tipo]
    );
  }
}

/** Util pra asserts: consulta arbitrária. */
export async function querySql<T = any>(sql: string, params: any[] = []): Promise<T[]> {
  const c = await getConn();
  const [rows] = await c.query(sql, params);
  return rows as T[];
}
```

### 7.3. Inicialização global — `e2e/fixtures/global-setup.ts`

```typescript
import { garantirRequisitanteE2E } from './banco';

/** Roda 1x antes de TODOS os testes. Configurado em playwright.config.ts. */
export default async function globalSetup() {
  await garantirRequisitanteE2E();
}
```

```typescript
// playwright.config.ts (trecho relevante)
export default defineConfig({
  globalSetup: './e2e/fixtures/global-setup.ts',
  use: { baseURL: process.env.E2E_FRONTEND_URL },
  // ...
});
```

### 7.4. Helper de auth — `e2e/fixtures/auth.ts`

```typescript
import { Page } from '@playwright/test';

const BACKEND = process.env.E2E_BACKEND_URL!;
const ADMIN_KEY = process.env.E2E_ADMIN_KEY!;

export async function loginE2E(page: Page) {
  // 1. Gerar convite via admin API
  const convite = await fetch(`${BACKEND}/admin/api/v1/requisitantes/99/convite`, {
    method: 'POST',
    headers: { 'X-Admin-Key': ADMIN_KEY },
  }).then(r => r.json());

  // 2. Extrair token da URL
  const token = new URL(convite.url).searchParams.get('t')!;

  // 3. Exchange → recebe cookie
  const exchange = await fetch(`${BACKEND}/api/v1/auth/exchange`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token }),
  });

  // 4. Capturar Set-Cookie
  const setCookie = exchange.headers.get('set-cookie')!;
  const cookieValue = parseCookieValue(setCookie, 'finbot_session');

  // 5. Injetar no contexto do Playwright
  await page.context().addCookies([{
    name: 'finbot_session',
    value: cookieValue,
    domain: 'localhost',
    path: '/',
    httpOnly: true,
    sameSite: 'Lax',
  }]);
}

function parseCookieValue(setCookie: string, name: string): string {
  const match = setCookie.match(new RegExp(`${name}=([^;]+)`));
  if (!match) throw new Error(`Cookie ${name} não encontrado em Set-Cookie`);
  return match[1];
}
```

---

## 8. Suite MVP — 3 specs detalhadas

### 8.1. `site-fluxo-feliz.spec.ts` — fluxo principal + a11y

**Objetivo:** validar que o usuário consegue logar via link mágico, ver pedidos reais, abrir um detalhe, e visualizar o modal de comprovante. Mais a11y na home.

#### Fluxograma do teste

```
beforeEach
   │
   ▼
┌──────────────────────────────────────────┐
│ limparDadosE2E()                         │
│ DELETE de tudo do requisitante 99        │
└─────────────────┬────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────────┐
│ semearPedidos([                          │
│   { id, descricao: 'E2E Boleto',         │
│     valor: 287.50, status: PAGO, ... },  │
│   { id, descricao: 'E2E PIX',            │
│     valor: 320, status: PENDENTE, ... }  │
│ ])                                        │
└─────────────────┬────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────────┐
│ loginE2E(page)                            │
│ → POST /admin/.../convite                 │
│ → POST /api/v1/auth/exchange              │
│ → addCookies('finbot_session')            │
└─────────────────┬────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────────┐
│ page.goto('/')                            │
└─────────────────┬────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────────┐
│ EXPECT 'E2E Boleto' visível               │
│ EXPECT 'E2E PIX' visível                  │
│ EXPECT '1 pedido pendente' no header      │
└─────────────────┬────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────────┐
│ AxeBuilder.analyze()                      │
│ EXPECT violations === []                  │
└─────────────────┬────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────────┐
│ click 'E2E Boleto'                        │
│ EXPECT URL /pedidos/<id>                  │
└─────────────────┬────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────────┐
│ click 'Ver comprovante'                   │
│ EXPECT role=dialog visível                │
└─────────────────┬────────────────────────┘
                  │
                  ▼
              ✓ ou ✗
       (se ✗ → screenshot, vídeo, trace
        salvos em playwright-report/)
```

#### Código

```typescript
import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { limparDadosE2E, semearPedidos } from '../fixtures/banco';
import { loginE2E } from '../fixtures/auth';

test.beforeEach(async () => {
  await limparDadosE2E();
  await semearPedidos([
    { descricao: 'E2E Boleto Energia', valor: 287.50, status: 'PAGO', tipo: 'BOLETO',
      dataPedido: '2026-06-01', sKeyComprovante: 'e2e/sample.jpg' },
    { descricao: 'E2E PIX Maria',      valor: 320.00, status: 'PENDENTE', tipo: 'PIX',
      dataPedido: '2026-05-30' },
  ]);
});

test('fluxo feliz: login → home → detalhe → comprovante (com a11y)', async ({ page }) => {
  await loginE2E(page);
  await page.goto('/');

  await expect(page.getByText('E2E Boleto Energia')).toBeVisible();
  await expect(page.getByText('E2E PIX Maria')).toBeVisible();
  await expect(page.getByText(/1 pedido pendente/i)).toBeVisible();

  const a11y = await new AxeBuilder({ page }).analyze();
  expect(a11y.violations.filter(v => v.impact !== 'minor')).toEqual([]);

  await page.getByText('E2E Boleto Energia').click();
  await expect(page).toHaveURL(/\/pedidos\/\d+/);

  await page.getByRole('button', { name: /ver comprovante/i }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
});
```

### 8.2. `webhook-cenarios.spec.ts` — canal de entrada parametrizado

> **Decisão 13 aplicada:** spec parametrizada desde o MVP. Em vez de N arquivos por cenário, 1 arquivo com tabela de dados. Adicionar cenário novo = 1 entrada na tabela.

**Objetivo:** validar que diferentes tipos de update do Telegram chegam no `/webhook` e disparam o comportamento esperado — pedido criado, mensagem amigável devolvida, sem crash do bot. Cobre o handler genérico (BE-15) e o parser de legenda já no MVP.

#### Fluxograma do teste

```
beforeEach
   │
   ▼
┌──────────────────────────────────────────┐
│ limparDadosE2E()                         │
└─────────────────┬────────────────────────┘
                  │
                  ▼
            (para CADA cenário da tabela)
                  │
                  ▼
┌──────────────────────────────────────────┐
│ Montar payload sintético do tipo         │
│ declarado no cenário                     │
└─────────────────┬────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────────┐
│ request.post('/webhook', { data })       │
└─────────────────┬────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────────┐
│ EXPECT status === 200                    │
│ (webhook NUNCA retorna 5xx — ADR 0003)   │
└─────────────────┬────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────────┐
│ Asserções específicas do cenário:        │
│  • esperaPedido: SELECT no banco         │
│  • valor/tipo/status esperados           │
│  • OU: 0 pedidos criados (sad path)      │
└─────────────────┬────────────────────────┘
                  ▼
              ✓ ou ✗
```

#### Código

```typescript
import { test, expect } from '@playwright/test';
import { limparDadosE2E, querySql } from '../fixtures/banco';
import {
  telegramUpdateFotoLegenda,
  telegramUpdateTextoPuro,
  telegramUpdateSticker,
} from '../fixtures/payloads-telegram';

const BACKEND = process.env.E2E_BACKEND_URL!;

interface Cenario {
  nome: string;
  payload: object;
  esperaPedido: boolean;
  asserts?: (pedido: any) => void;
}

const cenarios: Cenario[] = [
  {
    nome: 'foto + caption "150 boleto E2E energia" cria pedido BOLETO',
    payload: telegramUpdateFotoLegenda({
      fromUserId: 99,
      caption: '150 boleto E2E energia',
    }),
    esperaPedido: true,
    asserts: (p) => {
      expect(Number(p.valor)).toBeCloseTo(150);
      expect(p.tipo).toBe('BOLETO');
      expect(p.status).toBe('PENDENTE');
    },
  },
  {
    nome: 'texto puro "obrigado" retorna 200 sem criar pedido',
    payload: telegramUpdateTextoPuro({ fromUserId: 99, text: 'obrigado' }),
    esperaPedido: false,
  },
  {
    nome: 'sticker retorna 200 sem criar pedido (handler genérico BE-15)',
    payload: telegramUpdateSticker({ fromUserId: 99 }),
    esperaPedido: false,
  },
];

test.beforeEach(async () => {
  await limparDadosE2E();
});

for (const c of cenarios) {
  test(`webhook: ${c.nome}`, async ({ request }) => {
    const res = await request.post(`${BACKEND}/webhook`, { data: c.payload });
    expect(res.status()).toBe(200);

    const pedidos = await querySql<any>(
      `SELECT valor, descricao, tipo, status FROM pedidos_pagamento WHERE requisitante_id = 99`
    );

    if (c.esperaPedido) {
      expect(pedidos).toHaveLength(1);
      c.asserts?.(pedidos[0]);
    } else {
      expect(pedidos).toHaveLength(0);
    }
  });
}
```

#### Como adicionar um cenário novo (Fase 2)

Adicionar 1 entrada em `cenarios`:

```typescript
{
  nome: 'forward de outra conversa retorna 200 sem criar pedido',
  payload: telegramUpdateForward({ fromUserId: 99, originalText: '...' }),
  esperaPedido: false,
},
```

Adicionar a função factory correspondente em `fixtures/payloads-telegram.ts`. Pronto.

#### O que esse teste NÃO cobre

- **Telegram Bot API real:** payload é sintético, não vem do Telegram de fato. Sutilezas (campos opcionais que mudam de release pra release, encoding de emoji em caption) podem divergir.
- **Download de mídia:** quando o caption indica foto+pedido, o back **tentaria** baixar a foto via `getFile`. Pra evitar dependência externa, isso precisa ser mockado no profile dev (ex: `app.telegram.api-url` aponta pra um WireMock local) ou o teste foca só na parte de classificação/dispatch antes do download.
- **Cert SSL + `setWebhook`:** continua sendo Camada 5 manual via `RUNBOOK-smoke-test-telegram-cadeia.md`.

> **Detalhe a resolver na implementação:** decidir como o back trata o download de mídia em dev — mock automático na config dev, ou flag `app.telegram.skip-media-download=true` exclusivo de dev/E2E, ou injetar um `TelegramFileDownloader` stub. Vai pra "decisões a resolver na task BE-XX de seed/setup E2E".

### 8.3. `a11y-home.spec.ts` — acessibilidade em todas as páginas autenticadas

**Objetivo:** garantir que nenhuma página principal tem violação de a11y de impacto sério.

```typescript
test('nenhuma violação de a11y séria em páginas autenticadas', async ({ page }) => {
  await loginE2E(page);
  for (const path of ['/', '/erro', '/pedidos/1']) {
    await page.goto(path);
    const a11y = await new AxeBuilder({ page }).analyze();
    const seriousOrCritical = a11y.violations.filter(
      v => v.impact === 'serious' || v.impact === 'critical'
    );
    expect(seriousOrCritical, `Violações em ${path}`).toEqual([]);
  }
});
```

---

## 9. Integração com o ciclo de PR (decisões 10 + 12 detalhadas)

> **Resumo das decisões:** implementador roda `e2e:full` quando o plano da task declara `exige_e2e_full: true`. Reviewer não roda — audita pelo bloco `e2e_full` no status report. Evidência fica em `playwright-report/` local (não commitado).

### 9.1. No plano da task

Frontmatter do plano de task ganha campo novo:

```yaml
---
task_id: BE-XX
# ...campos existentes...
exige_e2e_full: true
exige_e2e_full_justificativa: >
  Toca contrato do endpoint /api/v1/pedidos (adiciona campo dataCancelamento
  no DTO de detalhe). Pode quebrar o front se o codegen não bater.
---
```

Planner preenche com base nos critérios da §5.3. Casos ambíguos: planner consulta humano ou marca `true` por conservadorismo.

### 9.2. No status report da task (decisão 12)

Frontmatter do status report ganha bloco `e2e_full`:

```yaml
---
task_id: BE-XX
# ...campos existentes...
e2e_full:
  exigido: true             # ecoa do plano
  executado: true
  status: verde             # verde | vermelho | nao-aplicavel
  specs_total: 3
  specs_passed: 3
  specs_failed: 0
  duracao_segundos: 142
  data_execucao: 2026-06-01T15:30:00Z
---
```

Combinações válidas:

| `exigido` | `executado` | `status` | Reviewer aprova? |
|---|---|---|---|
| `true` | `true` | `verde` | sim |
| `true` | `true` | `vermelho` | não — implementador corrige |
| `true` | `false` | qualquer | **não** — pendência bloqueante |
| `false` | `false` | `nao-aplicavel` | sim |
| `false` | `true` | qualquer | sim (rodou opcionalmente, ok) |

Seção textual no status report:

```markdown
## Evidência E2E

`npm run e2e:full` executado em 2026-06-01 15:30. Resultado:
- site-fluxo-feliz.spec.ts → 1 passed
- webhook-cenarios.spec.ts → 3 passed (foto+caption, texto puro, sticker)
- a11y-home.spec.ts → 1 passed

Total: 5 passed, 0 failed, 0 skipped (2m22s).

Relatório HTML: `frontend/playwright-report/index.html` (local, não commitado).
```

### 9.3. No PRE-MERGE-CHECKLIST

Item novo no `docs/runbooks/PRE-MERGE-CHECKLIST.md`, na seção de gates do implementador:

> - [ ] Se plano declara `exige_e2e_full: true`: `npm run e2e:full` verde, bloco `e2e_full` preenchido no status report

Item no checklist do Reviewer:

> - [ ] Se `exige_e2e_full: true` no plano: confirmar que `e2e_full.executado: true` e `e2e_full.status: verde` no status report. Caso contrário, **rejeitar** com pendência bloqueante.

### 9.4. Fluxograma do ciclo

```
Plano da task escrito pelo planner
   │
   ▼
exige_e2e_full?  ──── não ────► implementador implementa, status report sem rodar
   │ sim                              │
   ▼                                  │
implementador implementa              │
   │                                  │
   ▼                                  │
npm run e2e:full                      │
   │                                  │
   ├─── verde ────► preenche e2e_full ┤
   │                no status report   │
   │                                  │
   └─── vermelho ─► fixa bug ─► re-roda
                                      │
                                      ▼
                            PR feature → integration
                                      │
                                      ▼
                            Reviewer audita status report
                                      │
                       ┌──────────────┴───────────────┐
                       │                              │
                       ▼                              ▼
              e2e_full coerente?              e2e_full ausente ou
                       │                       inconsistente?
              sim → aprova                    não → rejeita,
                                              pendência bloqueante
```

### 9.5. Quando o plano declara `false` mas o Reviewer discorda

Reviewer pode escalar pro planner: "essa task toca tal coisa, deveria ter `exige_e2e_full: true`". Planner decide se reescreve plano + pede implementador rodar, ou aceita o `false` com justificativa explícita. Decisão fica no relatório de avaliação do Reviewer (`docs/sprints/<NN>/avaliacoes/`).

---

## 10. Roadmap em 3 fases

### Fase 1 — MVP (1 sprint enxuta)

> **Atualizado 2026-06-01 (arquiteto):** removido o item "Migration ou endpoint admin pra requisitante 99 (back, ~1h)". A Decisão 8 (§5.2) consolidou que o seed do requisitante 99 é feito por helper TS via `mysql2/promise` no `fixtures/banco.ts` — **não há task de back de seed**. A única verificação de back necessária é confirmar que `requisitante_id` existe em `pedidos_pagamento` pós-EVO-09 (custo ~0 — checagem no `SHOW CREATE TABLE`).
>
> A quebra concreta em tasks `QA-NNN` (pendendo do ADR 0017 `Proposed`) está em `docs/sprints/<NN>/specs/qa-suite-e2e-fase1.md`.

| Task (alto nível) | Responsável | Esforço |
|---|---|---|
| Setup Playwright + tsconfig + scripts npm | front (QA) | ~2h |
| `fixtures/banco.ts` + `fixtures/auth.ts` | front (QA) | ~2h |
| `scripts/subir-stack.ts` + `derrubar-stack.ts` | front (QA) | ~2h |
| 3 specs do MVP | front (QA) | ~3h |
| Doc em `docs/runbooks/ROTEIRO-E2E.md` | planner | ~1h |
| Item novo no PRE-MERGE-CHECKLIST | planner | ~15min |

**Total:** ~10h.

**Critério de aceite:** `npm run e2e:full` verde 3 vezes seguidas em ambiente limpo.

### Fase 2 — Cobertura do ROTEIRO completo

Replicar cada cenário do `ROTEIRO-INTEGRACAO-FRONT-BACK.md`:

- Filtros (status, mês, busca com debounce)
- Regressão dos bugs A e B da FE-12
- Paginação (page=0 → page=1)
- 401 / sessão expirada
- Pedido inexistente (404)
- Isolamento entre requisitantes (IDOR — cria requisitante 100, tenta acessar pedido dele com cookie de 99, espera 403)
- Boundary tests (data limite do mês, valor zero, valor máximo)
- Schemathesis como step opcional

**Esforço:** ~1 sprint adicional (~8-10h).

### Fase 3 — Pré-deploy hardening

- Smoke pós-deploy em prod: subset reduzido (1-2 specs) apontando pra `https://api.finbot.dom.br` e `https://finbot.dom.br`
- Disparado por GitHub Actions logo após o deploy
- Notificação em caso de falha (Slack, email, ou só log)

**Esforço:** ~3-4h.

---

## 11. Trade-offs e dívidas conscientes

| Decisão | Trade-off aceito | Mitigação |
|---|---|---|
| Sem CI | Regressão pode passar batido se ninguém rodar | Item no PRE-MERGE-CHECKLIST + hábito dos agentes |
| Banco persistente | Lixo acumula se cleanup falhar | Cleanup em `beforeEach` (não em `afterAll`) |
| Sem ngrok / Telegram real | Não pega regressão de `setWebhook`, cert, IP da EC2 | Camada 5 manual continua existindo |
| Schemathesis na Fase 2 | Contract testing fica passivo | `openapi-typescript` força divergência grande no `tsc -b` |
| 1 browser (Chromium) | Não pega bug específico de Safari/Firefox | Pedro usa Chrome no celular (PWA); expansão fácil depois |
| Credenciais em `.env.e2e` local | Cada máquina precisa do arquivo preenchido | `.env.e2e.example` versionado como template auto-documentado |
| Auth via fetch real (não mock) | Teste depende de admin API estar de pé | É exatamente o que queremos validar |
| Webhook usa payload sintético | Pode divergir do payload real do Telegram | Manter factories em `payloads-telegram.ts` atualizadas; Camada 5 manual captura divergência |
| Reviewer não roda `e2e:full` | Confia no que implementador anotou no status report | Frontmatter estruturado + rejeição automática em casos ambíguos (§9.2) |

---

## 12. Métricas de sucesso

| Métrica | Hoje | Após Fase 1 | Após Fase 2 |
|---|---|---|---|
| Fluxos E2E automatizados | 0 | 3 | ~12 |
| Tempo de "validar integração front↔back" | 45-60 min (manual) | ~2 min | ~5 min |
| Regressão de a11y detectada | nunca | em 1 fluxo | em todos os fluxos |
| Item no PRE-MERGE-CHECKLIST | inexistente | "rodou `e2e:full` verde" | idem + Schemathesis |
| Cobertura JaCoCo (back) | atual | sem mudança | sem mudança |
| Cobertura Vitest (front) | atual | aumento marginal | aumento marginal |

A meta dos E2E **não é elevar cobertura de linha** — é cobrir os caminhos que unit+componente+integração não pegam. Cobertura de linha continua sendo função da pirâmide base.

---

## 13. Decisões resolvidas — histórico

Estas decisões foram discutidas e fechadas com o humano em 2026-06-01. Estão consolidadas na §5.2 — ficam listadas aqui pra rastreabilidade.

| # | Pergunta original | Resposta final |
|---|---|---|
| D1 | Como criar o requisitante 99? | Helper TS direto (sem migration, sem endpoint admin) |
| D2 | Onde guardar a senha do MySQL? | `.env.e2e` gitignored + `.env.e2e.example` versionado |
| D3 | Como integrar com Reviewer? | E+A+D combinados — gating no plano, implementador roda, Reviewer audita |
| D4 | Threshold de a11y? | Falha em `serious` + `critical` |
| D5 | Reportagem dos resultados? | HTML local + bloco `e2e_full` no status report |
| D6 | Cenários de webhook (forward, sticker, etc.)? | Spec parametrizada desde o MVP com tabela de cenários |

### Decisões ainda em aberto

> **Reclassificado 2026-06-01 (arquiteto):** decisões agora marcadas por urgência. Bloqueadores da Fase 1 ficam no topo, evoluções futuras descem.

#### Bloqueador da Fase 1 — resolver em sessão dedicada antes de despachar a task afetada

1. **Mock do download de mídia do Telegram em dev/E2E.** O back tenta baixar a foto via `getFile` quando recebe foto+caption. Em E2E, isso falha (token Telegram dev pode estar válido mas o `file_id` sintético não existe). Opções a comparar: (a) WireMock no profile dev, (b) flag `app.telegram.skip-media-download=true` ativada por profile, (c) stub `TelegramFileDownloader` injetado via `@Profile`. **Bloqueia** o cenário "foto + caption" do `webhook-cenarios.spec.ts` (Decisão 6/§8.2). Não bloqueia os cenários "texto puro" e "sticker" — esses podem ir no MVP normalmente, e a foto+caption entra depois desta decisão fechar. Decisão **deve virar ADR `Proposed` separado** antes da task que implementa o helper de payloads Telegram ser despachada.

#### Para resolver na Fase 2 (não bloqueia MVP)

2. **Critério detalhado de gating quando a task é mista** (toca controller REST + parser de legenda + arquivo de config). Hoje §5.3 trata como OR — se uma condição se aplica, exige. Edge cases ambíguos podem precisar de decisão caso a caso. Resolver com casos reais quando aparecerem.
3. **Comportamento desejado pra album (várias fotos em 1 mensagem)** — Telegram manda N updates, comportamento atual não é definido. Discutir com humano antes de adicionar cenário à tabela parametrizada de webhook.
4. **Evolução do threshold de a11y** — quando e em qual sprint subir pra `moderate+`? Atrelar a critério mensurável (ex: "depois de N sprints sem violação serious/critical").

---

## 14. Anexos

### 14.1. Documentos relacionados

- `docs/runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md` — fluxo manual que esta suíte vai automatizar
- `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` — pirâmide atual do backend
- `docs/runbooks/RUNBOOK-smoke-test-telegram-cadeia.md` — smoke E2E via Telegram (continua manual)
- `docs/architecture/especificacao-tecnica.md` — contratos da API e modelo de dados
- `docs/decisions/0007-reporting-com-gates-e-status-report-como-output-contract.md` — métrica `testes_novos` por sprint

### 14.2. Comandos de referência rápida

```bash
# Rodar tudo (sobe back+front, executa specs, derruba tudo)
cd frontend && npm run e2e:full

# Rodar só os testes (back+front já rodando)
cd frontend && npm run e2e

# Debug interativo
cd frontend && npm run e2e:ui

# Ver relatório do último run (HTML com screenshots e traces)
cd frontend && npm run e2e:report

# Rodar uma spec específica
cd frontend && npx playwright test specs/site-fluxo-feliz.spec.ts

# Atualizar baseline de a11y (se intencionalmente aceitar nova violação minor)
# — manual, não há comando, é decisão de PR
```
