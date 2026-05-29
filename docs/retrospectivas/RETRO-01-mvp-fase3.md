# Retrospectiva 01 — MVP (Fase 3: camada de visualização)

**Data:** 2026-05-27
**Período coberto:** Fase 3 inteira (backend REST → front PWA → deploy), ~2 semanas de execução concentrada.
**Resultado:** ✅ **MVP em produção, E2E validado.** O Pedro acessa `satyansaita.com`, entra por link mágico, vê pedidos reais, abre comprovantes, instala como PWA e a sessão persiste. O bot do Telegram seguiu operando sem interrupção durante toda a fase.

Esta é a **primeira** retro do projeto. Ela cobre dados que achamos relevantes agora; o sistema de métricas/avaliação vai amadurecer a cada ciclo (isso é, inclusive, uma das ações abaixo).

---

## 1. O que foi entregue

A camada de leitura pro requisitante, ponta a ponta, em cima do bot existente:

- **Backend (Fase 3a):** modelo de dados (requisitante, datas, tipo, auth_token), API REST com OpenAPI, auth por link mágico (token admin → JWT em cookie), pre-signed URLs do S3, resumo por mês, handler genérico de exceções do webhook, testes de integração com Testcontainers.
- **Front (Fase 3b):** React + Vite + TS + Tailwind + TanStack Query, PWA instalável, timeline de pedidos, modal de comprovante, auth guard, codegen de tipos do OpenAPI.
- **Deploy (Fase 3c):** Route 53 + ACM, S3 + CloudFront (apex), API atrás de Caddy/Let's Encrypt (`api.satyansaita.com`), pipeline do front via OIDC, CORS/cookie de prod, E2E manual.
- **Evolução pontual:** EVO-07 (aceitar document/PDF) resolvendo o incidente histórico do PDF que travava a fila.

---

## 2. Dados (o que dá pra medir hoje)

| Métrica | Valor | Observação |
|---|---|---|
| Planos escritos (`docs/plans/`) | 44 | input contracts |
| Status reports (`docs/status/`) | 43 | output contracts |
| ADRs (`docs/decisions/`) | 7 (0003–0009) | decisões canônicas |
| Aprendizados (`docs/aprendizado/`) | 23 | conhecimento retido |
| Runbooks | 5 | roteiros operacionais |
| Papéis (`docs/roles/`) | 4 | planner/back/front/reviewer |
| Testes agregados (frontmatter) | ~495 | parcial — só os reports com schema reportam |
| Desvios de plano registrados | 4 (3 back, 1 front) | baixa taxa de desvio |
| Tasks bloqueadas ao fim | 0 | nada parado |
| **Cobertura do schema de status** | **44% (19/43 com frontmatter; 8 canônicos)** | ⚠️ adoção tardia — ver "melhorar" |

> O número de cobertura do schema veio do nosso próprio `docs/scripts/metricas_status.py`. É o tipo de métrica que muda comportamento: mostra que o output-contract foi adotado no meio do caminho, não desde o início.

Entrega aconteceu muito via **sessões overnight** (lotes de tasks autônomas): front FE-03→11 numa noite, back polish + EVO-07 noutra, e o deploy DEP-04/05/07 na última. Alto throughput com o humano orquestrando de manhã.

---

## 3. O que foi bem (continuar)

- **`docs/` como cérebro compartilhado funcionou de verdade.** Sessões frias (e a separação planner/implementador) retomavam contexto pelos docs sem re-explicação. O `STATE.md` ajudou a orientar rápido.
- **Sessões overnight + master-prompt** entregaram volume grande de forma autônoma e previsível, com status reports ao fim.
- **Arquitetura hexagonal pagou dividendo:** a camada de leitura (API + front) entrou sem tocar na operação do bot — o adapter de entrada ficou isolado.
- **Governança incremental por ADR** (0003→0009) deu rota canônica pras decisões e a taxonomia de persistência (ADR 0004) cortou a fragmentação de "onde mora cada coisa".
- **Biblioteca de aprendizado (23 arquivos)** — o conhecimento técnico ficou retido em vez de evaporar no chat. Isso é raro e valioso.
- **Contrato plano→status→review** (structured outputs aplicado ao processo) deu forma e baixou a variância das entregas.
- **Especialização por papel + Reviewer** (ADR 0005) — quando entrou, deu independência de revisão que o planner sozinho não tem.

---

## 4. O que pode melhorar

- **Single source of truth pra config.** O domínio derivou em três versões (`finbot.dom.br` → `finbot.satyan.com.br` → `satyansaita.com`) e apareceu **errado** no `.env.production` (front) e no `application-prod.properties` (back). Os tipos do front também derivaram (`mesAtual`→`mes`, FE-12). Padrão claro: **config/contrato duplicado à mão diverge.** Já começamos a atacar (codegen de tipos, FE-13) — estender a disciplina pra config (domínio/URLs centralizados).
- **Schema de status adotado tarde.** Só 44% têm frontmatter; 8 canônicos. O `_TEMPLATE`/ADR 0007 vieram no meio, então o histórico é misto. As métricas só ficam confiáveis quando todo report novo seguir o schema.
- **Reviewer entrou tarde.** A maior parte das tasks da fase **não** passou por revisão independente — o Reviewer só foi formalizado/adotado perto do fim. O ganho de qualidade aparece quando ele roda desde o começo.
- **Smoke-tests baseados em suposição.** O critério do DEP-03 apontou `/actuator/health` (que não existe) e `/v3/api-docs` (desligado em prod). Validação tem que mirar endpoint que **existe**.
- **Infra dimensionada/observada de forma reativa.** O disco de 2 GB encheu e **bloqueou o deploy**, sem nenhum alarme. Faltou sizing proativo + observabilidade mínima (e um health real — daí o Actuator no débito).
- **Snowflake server.** Caddy, journald, `finbot.service` e keystore eram config manual fora do código até o DEP-07 — um recreate teria sido doloroso. Codificar o provisionamento foi reação, não prevenção.
- **Fricção de tooling no doc vivo.** O `STATE.md` sofreu reverts por sync do OneDrive ao ser editado em sequência — o doc de orientação precisa ser confiável (ou tratado como derivado/gerado).
- **Métricas formais não capturadas.** Não medimos cycle time, retrabalho nem taxa de gate-fail de forma sistemática. O script existe; falta rodar com regularidade e escolher 2–3 indicadores que mudem comportamento.

---

## 5. Aprendizados-chave do caminho

Os de maior substância (todos em `docs/aprendizado/`):

- **`structured-outputs.md`** — schema reduz variância; sintaxe ≠ semântica. Foi a lente que explicou o incidente FE-12 (input ambíguo → saída errada) e moldou os contratos de plano/status.
- **`tls-self-signed-vs-ca.md`** — por que migrar o webhook pra LE é conveniência operacional, não segurança (pinning já autentica). Evitou trabalho desnecessário.
- **`front-api-hostnames-separados.md`, `cors-vs-samesite.md`, `cookies-samesite.md`** — a base que fez o front↔API funcionar (e que apontou o bug de domínio do DEP-05).
- **`git-line-endings-crlf-lf.md`** — o drift de CRLF/LF que sujava o `terraform plan`.

---

## 6. Incidentes e como foram resolvidos

| Incidente | Causa | Resolução |
|---|---|---|
| FE-12 em branch errada | Seção "Branch" do plano contradizia a convenção | Regra de precedência no `CLAUDE.md` |
| Deploy bloqueado (`No space`) | Volume raiz de 2 GB encheu | FIX-volume (10 GB gp3) + cap journald |
| CORS/cookie quebrariam o front | Domínio antigo no `application-prod.properties` | DEP-05 (corrigir pra `satyansaita.com`) |
| `terraform plan` sujo todo run | CRLF/LF no `ec2.tf` (Windows) | `.gitattributes` (FIX-gitattributes) |
| Webhook desregistra com `setWebhook` | Cert self-signed pinado | Parqueado (DEP-08) — não bloqueante |

---

## 7. Ações pra próxima etapa

| # | Ação | Dono sugerido |
|---|---|---|
| 1 | **Adotar o Reviewer desde o início** — sessão separada pra toda task (ou ao menos as de risco) | humano (orquestra) |
| 2 | **Todo status report novo no schema canônico** (`estado` + gates) + rodar `metricas_status.py` ao fim de cada etapa | todas as sessões |
| 3 | **Single source of truth pra config crítica** (domínio/URLs centralizados; estender codegen pro contrato) | planner + back + front |
| 4 | **Observabilidade mínima:** promover Actuator (health) + alarme básico de disco/CPU | back |
| 5 | **Resolver o débito de segurança alta:** rotação do token do Telegram | humano |
| 6 | **Confiabilidade do `STATE.md`** — mitigar o sync (ou tratá-lo como derivado) | planner |
| 7 | **Definir o sistema de métricas de entrega/avaliação** — escolher 2–3 indicadores que mudem comportamento, começar simples | planner |

---

## 8. Nota sobre o próximo passo (fora do escopo desta retro)

Decisão em aberto: a próxima etapa começa com **discovery de produto (PO)** pra definir o essencial, ou com **arquiteto** pra desenhar a evolução técnica? Não se decide aqui — fica pra conversa logo após a retro, agora que o MVP está no ar e o uso real vai gerar feedback (e bugs) pra priorizar.

---

> **Como ler esta retro no futuro:** ela é o marco zero do nosso sistema de entrega/avaliação. As ações da seção 7 são o que carregamos pro próximo ciclo; a próxima retro mede se melhoramos nelas.
</content>
