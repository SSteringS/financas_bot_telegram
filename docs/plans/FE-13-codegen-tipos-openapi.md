# FE-13 — Codegen do `tipos.ts` a partir do OpenAPI

> **Intake (contrato de entrada da task)**
>
> - **Origem:** backlog de evolução do workflow, item #1 (`docs/plans/BACKLOG-evolucao-workflow.md`). Motivado pelo incidente da **FE-12** (`mesAtual → mes`): o `tipos.ts` é mantido à mão e divergiu do contrato real do backend, gerando bug que só apareceu em runtime.
> - **Prioridade:** alta (mata uma classe inteira de bug de drift front/back).
> - **Esforço:** médio.
> - **Território / quem executa:** `frontend/` → **Claude do front**. (O backend só precisa expor o `/v3/api-docs`, que já existe desde a BE-04 — sem mudança de código no back.)
> - **Branch:** `feature/fe-13-codegen-tipos-openapi`, a partir de `develop`.
> - **Dependências:** nenhuma de código. Pré-condição operacional: backend sobe localmente e serve `/v3/api-docs` (springdoc, já configurado).
> - **Riscos:** (1) tipos gerados verbosos/diferentes dos atuais → mitigado mantendo um `tipos.ts` de fachada que re-exporta/estreita os gerados (app não importa o arquivo gerado direto); (2) CI dependente de backend vivo → mitigado versionando um snapshot do spec e gerando do arquivo, não da rede; (3) drift continua possível se o snapshot não for atualizado → mitigado por script de refresh + check de drift no CI (fase 2).

---

## Contexto

Hoje `frontend/src/api/tipos.ts` é escrito à mão. O backend gera o contrato OpenAPI **automaticamente** a partir das anotações `@Schema`/`@Operation` (springdoc, decisão da BE-04) e serve em `/v3/api-docs` (JSON) + Swagger UI em `/swagger-ui.html`. Ou seja: **existe uma fonte de verdade de máquina pro contrato**, mas o front não a consome — re-digita os tipos. O custo disso já apareceu: na FE-12, o backend passou a devolver `mes` (e `todos`) no `/api/v1/resumo`, mas o front ainda tinha `mesAtual` no `tipos.ts`; o desencontro só estourou em runtime.

Esta task faz o front **gerar** os tipos a partir do OpenAPI, em vez de mantê-los à mão. Contrato e tipos do front deixam de poder divergir silenciosamente.

## Decisão de ferramenta

**`openapi-typescript`** (gera *só tipos* TypeScript a partir de um OpenAPI 3.x; zero runtime).

Por que esta e não as alternativas:

- O front **já tem** uma camada de cliente escrita à mão (`client.ts`, `pedidos.ts`, `auth.ts`) com tratamento de 401, cookies (`credentials: 'include'`), `ApiError` etc. Não queremos gerar um cliente — só os **tipos**. `openapi-typescript` faz exatamente isso e nada mais.
- **`orval` / `openapi-generator` / `swagger-typescript-api`**: geram cliente + hooks além dos tipos. Descartados — substituiriam (ou duplicariam) a camada de cliente já testada, com churn grande e sem ganho proporcional ao objetivo (matar o drift de *tipos*).

## Estratégia: snapshot versionado + geração a partir do arquivo

Em vez de gerar direto da rede (`http://localhost:8080/v3/api-docs`) toda vez, **versionar um snapshot do spec** no repo e gerar os tipos a partir dele. Razões:

- **CI não depende de backend vivo** — gera do arquivo commitado.
- O snapshot vira **artefato revisável**: um diff no `openapi.json` no PR mostra exatamente o que mudou no contrato.
- Atualizar o snapshot é um passo **deliberado** (rodar o script de refresh contra o back local), não um efeito colateral.

Fluxo:

```
backend rodando  --(refresh, manual/deliberado)-->  frontend/openapi.json (commitado)
                                                          |
                                  (openapi-typescript)    v
                                              frontend/src/api/tipos-gerados.ts (commitado, do-not-edit)
                                                          |
                                  (re-export/narrowing)   v
                                              frontend/src/api/tipos.ts (fachada, escrita à mão)
```

App-code continua importando de `tipos.ts`. O arquivo gerado é detalhe de implementação — isola o resto do código de mudanças de forma do gerador.

## Escopo / arquivos

**Adicionar (`frontend/`):**

- `frontend/openapi.json` — snapshot do `/v3/api-docs` (commitado).
- `frontend/src/api/tipos-gerados.ts` — saída do `openapi-typescript` (commitado; header `// GERADO — não editar à mão`).
- `package.json` — devDependency `openapi-typescript` + scripts:
  - `"gen:api": "<curl/node> http://localhost:8080/v3/api-docs -o openapi.json"` — refaz o snapshot a partir do back local (passo deliberado; ajustar host/porta ao ambiente real do back).
  - `"gen:types": "openapi-typescript ./openapi.json -o ./src/api/tipos-gerados.ts"` — gera os tipos do snapshot.
- `README` do front (ou comentário no topo de `tipos.ts`): como/quando rodar o refresh.

**Modificar:**

- `frontend/src/api/tipos.ts` — passar a **re-exportar/estreitar** os tipos gerados em vez de redeclarar. Onde fizer sentido, manter aliases amigáveis (`PedidoResumo`, `ResumoMes`, `Pagina<T>`, `StatusPedido`, `TipoPagamento`, etc.) que apontam pros tipos gerados. Objetivo: **nenhuma mudança de import no resto do app**.

**Não tocar:** `client.ts`, `pedidos.ts`, `auth.ts` (camada de cliente permanece à mão), nem qualquer coisa fora de `frontend/`.

## Critérios de aceitação

- `openapi-typescript` instalado como devDependency; `npm run gen:types` gera `tipos-gerados.ts` a partir de `openapi.json` sem erro.
- `frontend/openapi.json` commitado, refletindo o contrato atual do backend (gerado de um back rodando a Fase 3a atual).
- `tipos.ts` passa a derivar dos tipos gerados; **`npm run build` (tsc) e `npm test` continuam verdes** sem mudar imports no app.
- Prova do valor (anti-drift): regenerar os tipos a partir do `openapi.json` commitado **não produz diff** (`npm run gen:types && git diff --exit-code src/api/tipos-gerados.ts` sai 0). Documentar esse comando.
- Os campos da FE-12 batem: o tipo de `/api/v1/resumo` derivado do spec tem `mes`, `todos`, `pendentes`, `pagos` — e **não** `mesAtual`.
- README/coment­ário explicando o ciclo: alterou contrato no back → roda o back → `npm run gen:api` → `npm run gen:types` → commita o diff de `openapi.json` + `tipos-gerados.ts`.

## Fase 2 (follow-up, fora desta task — anotar no backlog)

- **Check de drift no CI:** job que roda `npm run gen:types` e falha se houver diff vs. o commitado (garante que ninguém mexeu no `openapi.json` sem regenerar). Encaixa no `ci.yml` da CI-01.
- **Refresh do snapshot no pipeline do back:** pra pegar o drift mais profundo (back mudou o contrato mas ninguém atualizou o snapshot), considerar um passo no CI do backend que exporta o `/v3/api-docs` e compara/abre PR. Avaliar custo/benefício antes — pode ser overhead.

## Coordenação

- Executado pelo **front**. O **back não muda** — só precisa que o `/v3/api-docs` esteja completo e estável (está, desde BE-04 + FIX-hide-requisitanteid-swagger).
- Coordenação pontual: o front precisa do backend rodando **uma vez** pra capturar o snapshot inicial. Se o back não estiver à mão, o humano pode fornecer o JSON do `/v3/api-docs`.
- Não mergear direto — abrir PR pra `develop` e parar pra revisão.

## Definição de pronto

Passar pelos gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build, lint, testes, convenção de branch, território) e escrever o status report em `docs/sprints/01-mvp/status/FE-13-*.md` com frontmatter válido (`docs/templates/_TEMPLATE-status.md`). `estado: concluido` só com todos os gates ok e zero pendência.

## Referências

- `docs/plans/BACKLOG-evolucao-workflow.md` (item #1, origem)
- `docs/sprints/01-mvp/status/FE-12.md` e `docs/plans/FE-12-resumo-parametrizado-e-contadores.md` (o incidente de drift `mesAtual→mes`)
- `docs/plans/BE-04-dtos-openapi.md` (decisão de gerar OpenAPI via springdoc a partir das anotações)
- `docs/plans/CI-01-gate-pr-develop.md` (onde o check de drift da fase 2 se encaixa)
</content>
