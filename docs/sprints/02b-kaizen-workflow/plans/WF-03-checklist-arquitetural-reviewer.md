# WF-03 — Checklist arquitetural explícito no `reviewer.md`

> **Intake**
>
> - **Origem:** item #8 do `BACKLOG-evolucao-workflow.md` (descoberto 2026-05-28 na revisão da BE-19a). Reviewer aprovou comportamento mas não pegou que `MensagemProcessadaService` (application) usava `JdbcTemplate` (infra) diretamente — drift hexagonal. Virou `FIX-idempotencia-porta-application` na sprint 02.
> - **Prioridade:** alta. Toda task que mexer em camada nova arrisca repetir o tipo de drift até o "olho arquitetural" virar explícito.
> - **Esforço:** baixo (~30min). Edita um arquivo (`reviewer.md`) — texto, sem código.
> - **Território / quem executa:** `docs/roles/reviewer.md` → **Claude do planejamento** (meta-workflow).
> - **Fluxo de git:** commit direto em `develop` (regra do planner — CLAUDE.md §Instâncias). **Sem branch de feature, sem PR.** Humano revisa diff via `git show HEAD` antes do `git push`.
> - **Dependências:** nenhuma. Independente das outras WF-NN. Pode mergear em qualquer ordem.
> - **Riscos:** baixos — só doc. Risco residual: lista de 4 smells pode estar incompleta; ajustar em retros futuras.

---

## Contexto

`docs/roles/reviewer.md` hoje tem 33 linhas com 6 seções enxutas (objetivo, princípio, faz, NÃO faz, checklist do papel, ler sempre). O Reviewer hoje cobre comportamento, testes, gates e drift de contrato front/back. **Falta:** drift arquitetural — o tipo de violação que aparece quando uma task adiciona uma camada nova ou mexe em borda entre adapter e application.

**Princípio do papel** já estabelecido em `reviewer.md`: "verifica contra a realidade, não contra o relatório". O caso BE-19a mostrou o ponto cego: testes verdes + comportamento ok não detectam violação de direção de dependência hexagonal. Sintoma estrutural exige checagem estrutural — e ela precisa estar listada.

**Decisões da discovery 2026-05-30:**

- **Onde mora:** seção nova no próprio `reviewer.md` (não runbook separado). Reviewer lê o role inteiro no boot — bullet list direto na cabeça é mais confiável que "pra tasks que mexem em camadas, puxe o runbook X". Custo aceitável (~25 linhas a mais).
- **Quantos smells:** 4 (não a lista interminável). Escolha vinda da discovery — cobre o que dói no projeto Spring/hexagonal sem virar academic.
- **Métrica de sucesso:** contar FIXes "violação arquitetural" por sprint, no fim de cada uma — ritual manual no início (vira candidato a automação na WF-06 se valer a pena).

## Decisão / abordagem

Adicionar **uma seção nova** em `reviewer.md`, entre "NÃO faz" e "Checklist do papel", com os 4 smells priorizados. Acoplar um **item novo no checklist** apontando pra ela. Resto do role intacto.

**Por que esses 4 (e não outros):**

1. **Direção de dependência invertida** — o smell que gerou esta ação (BE-19a). Detectável por grep simples.
2. **Lógica de domínio no controller/adapter de entrada** — clássico, repetitivo, fácil de cometer com pressa.
3. **Anti-corruption ausente na saída** — o projeto vai expor cada vez mais APIs externas (Meta, AWS). Smell que escala mal se não pegar cedo.
4. **Exception handler com escopo errado** — sintoma da BE-15b, problema do projeto.

Outros candidatos (repo direto em controller; acoplamento entre adapters; @Transactional em método errado; service depende de classe concreta) ficam fora — ou são variantes dos 4 acima ou ainda não machucaram o suficiente pra justificar carga no role. **Revisitar na RETRO-03.**

## Escopo / arquivos

### Modificar

- **`docs/roles/reviewer.md`** — adicionar:
  - Seção nova `## Smells arquiteturais (em tasks que mexem em camadas)` entre `## NÃO faz` e `## Checklist do papel`. Conteúdo: parágrafo curto de quando aplicar + 4 bullets, cada um com (a) nome, (b) sintoma concreto, (c) regra de ação. Bullet 1 inclui referência ao caso BE-19a (concreto = memorável).
  - Item novo no `## Checklist do papel`: `[ ] Em tasks que mexem em camadas (adapter, port, application): passei pelo bloco "Smells arquiteturais".`
- **`docs/plans/BACKLOG-evolucao-workflow.md`** — marcar item #8 como ✅ feito, com link pra esta task e pra `reviewer.md`.

### Conteúdo proposto da seção nova (texto pronto):

```markdown
## Smells arquiteturais (em tasks que mexem em camadas)

Quando a task entrega código em mais de uma camada (controller + service + repo, adiciona adapter novo, ou tira/coloca port), passar ativamente por estes 4 sintomas estruturais. Testes verdes e comportamento ok **não** detectam — exige conferir imports e assinaturas.

1. **Direção de dependência invertida.** `application/` ou `domain/` importando classes de `infra/`/`adapters/` ou frameworks de borda. Sintomas: `org.springframework.jdbc.*`, `software.amazon.awssdk.*`, `org.springframework.web.client.RestClient`, etc. em arquivos da application. **Caso vivido:** BE-19a aprovou `MensagemProcessadaService` (application) usando `JdbcTemplate` direto — virou FIX-idempotencia-porta-application. Regra: se a application precisa de algo de infra, é por **port** (interface) — adapter implementa.

2. **Lógica de domínio no controller/adapter de entrada.** `@PostMapping`/`@GetMapping`/handler de webhook fazendo cálculo, validação de regra de negócio ou decisão sobre estado de domínio. Sintoma: `if`/`switch` sobre status/tipo de domínio em controller; aritmética financeira em adapter. Regra: controller só traduz protocolo→domínio e chama service.

3. **Anti-corruption ausente na saída.** Tipo da API externa (Meta, Telegram, AWS) vazando pra cima do adapter de saída. Sintoma: classe externa (`WhatsAppErrorResponse`, `S3Object`, `Update`) aparecendo em assinatura de método em `application/port/out/` ou consumida pela application. Regra: borda do adapter mapeia para tipo de domínio; application não conhece o externo.

4. **Exception handler com escopo errado.** `@ExceptionHandler` definido em controller específico capturando exceções gerais (vira "captura demais"), ou em `@ControllerAdvice` global tratando exceções específicas que só fazem sentido num escopo (vira "captura de menos"). **Caso vivido:** BE-15b. Regra: handler vive onde o escopo bate — global pra erros transversais, local pra erros do próprio controller.

Quando achar: registrar no veredito com **trecho do código + qual princípio foi violado**. Se inegável, **reprovar com observação** mesmo com testes/gates verdes — autorrelato não detecta sintoma estrutural.
```

### Não tocar

- Resto do `reviewer.md` (objetivo, princípio, faz, NÃO faz, ler sempre) — o delta é só a seção nova + 1 bullet no checklist.
- `docs/roles/backend.md`, `frontend.md`, `planner.md` — smells são responsabilidade do Reviewer detectar; implementadores naturalmente seguem `architecture/especificacao-tecnica.md`. Adicionar nos roles dos implementadores espalha responsabilidade — não faz.
- `CLAUDE.md` — mudança não é regra global; é especialização do papel.
- Script de métricas — métrica vem como ritual manual primeiro (ver "métrica" abaixo); automação fica como candidato na WF-06.

## Métrica de sucesso

**Ritual manual no fim de cada sprint** (vira parte do WF-06 quando este mergear):

```bash
# Contar FIXes do tipo "violação arquitetural" da sprint atual
ls docs/sprints/<NN>/plans/FIX-*.md 2>/dev/null | \
  xargs grep -l -iE "viola(ç|c)ao|drift arquitetural|hexagonal|camada|refator pra port" \
  2>/dev/null | wc -l
```

Anotar na retro:
- Sprint 02 (baseline): 1 FIX arquitetural (FIX-idempotencia-porta-application). Sprint **antes** desta task entrar em vigor.
- Sprint 03 em diante: comparar.

**Sinal de sucesso:** 0 FIXes do tipo nas sprints 03 e 04. **Sinal de regressão:** ≥1 FIX por sprint recorrente — ajustar a shortlist ou a forma de comunicar.

## Critérios de aceitação

- [ ] `docs/roles/reviewer.md` ganha seção `## Smells arquiteturais (em tasks que mexem em camadas)` com os 4 bullets do texto proposto.
- [ ] `docs/roles/reviewer.md` `## Checklist do papel` ganha item novo apontando pra seção.
- [ ] Caso vivido (BE-19a) citado no bullet 1 e (BE-15b) citado no bullet 4 — sem isso a seção vira teoria.
- [ ] `docs/plans/BACKLOG-evolucao-workflow.md` item #8 marcado ✅ feito com link pra esta task.
- [ ] Status report `docs/sprints/02b-kaizen-workflow/status/WF-03.md` com frontmatter válido.
- [ ] Território só `docs/`.
- [ ] Humano revisou o diff do commit (`git show HEAD`) e aprovou antes de `git push`. **Meta:** o próprio Reviewer aprovar uma task que muda como ele opera é fechamento de loop.

## Fora de escopo (explicitamente)

- **Lista completa de smells arquiteturais** — 4 é o suficiente pro MVP; mais vira ruído. Ajustar via retro.
- **Runbook detalhado com exemplos pra cada smell** — bullets concretos do texto proposto já apontam casos vividos. Runbook fica como candidato pós-MVP se sentir falta.
- **Estender `metricas_status.py`** com a contagem de FIXes arquiteturais — ritual manual primeiro; automação só se valer (WF-06 decide).
- **Treinar implementadores pra evitar os smells** — implementadores seguem spec técnica; smell é coisa do Reviewer detectar (separação de papéis).
- **Adicionar smell **
  campo no status report** — escopo do WF-08 (`pendencias_humano` + extensões de schema); decidir lá se entra.

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Reviewer ignora a seção nova ou esquece em sprints futuras | Média | Médio | Item explícito no checklist + frase no boot do role ("Em tasks que mexem em camadas: passar pelo bloco"). |
| Lista de 4 smells está enviesada / incompleta | Média | Baixo | Discovery 2026-05-30 escolheu baseado em casos vividos. Revisar na RETRO-03 (ação esperada). |
| "Mexem em camadas" é vago — Reviewer aplica seletivamente errado | Média | Baixo | Bullet de quando aplicar tem 3 exemplos concretos ("adiciona adapter novo, mexe em port, controller+service+repo"). |
| Métrica grep falsa positivo/negativo | Alta | Baixo | Métrica é orientativa, não bloqueante. Retro tem espaço pra interpretar. |
| Implementadores reagem mal a "reprovar com testes verdes" | Baixa | Baixo (cultura) | Princípio já existe ("autorrelato não detecta"); reforçar na seção. |

## Coordenação

- **Não toca código** — zero conflito com back/front.
- **Independente das outras WF-NN.**
- **Após merge:** Reviewer próximas tasks da kaizen (WF-04 em diante) e da sprint 03 já passa pelo bloco. Métrica vira ritual no WF-06.
- **Humano (revisor) foca em:** (a) os 4 bullets são acionáveis? (b) caso vivido BE-19a/BE-15b está claramente citado? (c) seção tá curta o suficiente pra não diluir o role?

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report com frontmatter válido, revisão direta do humano (Reviewer dispensado nesta sprint). Commit direto em `develop`; humano revisa `git show HEAD` antes do push.

## Referências

- BACKLOG-evolucao-workflow.md item #8 (versão íntegra de 128 linhas restaurada na sessão da RETRO-02).
- BE-19a — caso fundador (`docs/sprints/02-canal-whatsapp/plans/BE-19a-*.md` + status).
- FIX-idempotencia-porta-application — fix derivado.
- BE-15b — sintoma de exception handler com escopo errado (`docs/sprints/01-mvp/plans/BE-15b-*.md`).
- `docs/roles/reviewer.md` atual.
- ADR 0005 (sessões especializadas por papel).
