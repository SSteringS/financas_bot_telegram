# Avaliação — BE-16 (resumo parametrizado + pastas S3)

**Branch:** `feature/be-16-resumo-e-pastas-s3`
**Tarefas:** Task 1 (resumo aceita mês + busca, campo `todos`) · Task 2 (separar pastas S3)
**Implementador:** Claude do back
**Avaliado por:** humano (validação manual) + Claude de planejamento (análise de código)
**Data:** 2026-05-19

---

## Análise de código (planejamento)

### Veredito: aprovado com 2 observações

**Task 1 — Resumo.** Implementação limpa. `ResumoController` valida `mes` e lança `MesFormatoInvalidoException` (→ 400). `ResumoMesServiceImpl` usa `Clock` (testável), default = mês corrente, ramifica por busca, monta `todos = pendentes + pagos`. DTO renomeou `mesAtual→mes` e adicionou `todos`. Query agrega via `GROUP BY status` com `COALESCE(SUM, 0)`.

**Task 2 — S3.** `TipoUploadS3` (PEDIDO/COMPROVANTE) limpo. `uploadFile(bytes, extensao, tipoUpload)` parametriza folder + extensão; `uploadImage` legado delega pra `PEDIDO`. Strategies passam o enum certo.

### Observação 1 — branch contém EVO-07 (atenção no merge)

As strategies importam `TipoArquivo` / `TipoArquivoNaoSuportadoException` e tratam document/PDF — isso é da EVO-07. Logo, esta branch foi baseada na `polish-evo07` (ou a polish-evo07 já entrou na develop). **Consequência boa:** o conflito S3 que o plano antecipava já está resolvido (assinatura `uploadFile` combina `extensao` da EVO-07 + `TipoUploadS3` da BE-16). **Atenção:** confirmar a ordem de merge — se polish-evo07 ainda não está na develop, mergear ela antes (ou o PR da BE-16 já carrega a EVO-07 junto, o que precisa ser intencional).

### Observação 2 — `todos` exclui CANCELADO (inconsistência latente)

`StatusPedido` tem 3 valores (PENDENTE, PAGO, CANCELADO). A query do resumo agrupa por **todos** os status, mas o service só extrai PENDENTE e PAGO; `todos = pendentes + pagos`. Um pedido CANCELADO é silenciosamente descartado de todos os contadores.

Hoje não há fluxo de cancelamento, então é **latente, não bug ativo**. Mas: o front (FE-12) vai usar `resumo.todos.quantidade` como contador "Tudo". Se a listagem (`GET /api/v1/pedidos?status=todos`) algum dia incluir cancelados e o resumo não, recriamos a inconsistência que a BE-16 veio consertar. Recomendação: decidir e documentar que CANCELADO é excluído em todo lugar (resumo + listagem), ou alinhar quando o cancelamento existir. Teste 1.10 abaixo cobre essa checagem.

---

## Roteiro de validação manual

Ambiente dev rodando (`mvn spring-boot:run -Dspring-boot.run.profiles=dev`). Para Task 1, estar autenticado (JWT válido via fluxo magic link → `/auth/exchange`, ou autorizar no Swagger). Para Task 2, bot de dev ativo + acesso ao bucket S3 dev. Preencha **Resultado** com ✅/❌/⚠️.

### Pré-condições

- [ ] App dev subiu sem erro (BE-16 não adiciona migration — nada novo de schema)
- [ ] Você tem pelo menos um mês com pedidos de status variados no banco dev (ideal: um mês com PENDENTE + PAGO)

---

### Task 1 — Resumo parametrizado (via Swagger ou curl autenticado)

| # | Ação | Esperado | Resultado | Observação |
|---|---|---|-----------|---|
| 1.1 | `GET /api/v1/resumo` sem params | 200 com `mes` (mês corrente), `todos`, `pendentes`, `pagos`, cada um com `quantidade` e `total` | OK        | |
| 1.2 | `GET /api/v1/resumo?mes=2026-04` (um mês que tem pedidos) | 200, dados referentes a abril | OK        | |
| 1.3 | `GET /api/v1/resumo?mes=2026-05` | 200, dados de maio — diferentes de 1.2 | OK        | |
| 1.4 | `GET /api/v1/resumo?busca=<texto que existe>` (ex: `energia`) | 200, contadores refletem só pedidos cuja descrição contém o texto | OK        | |
| 1.5 | `GET /api/v1/resumo?mes=2026-04&busca=<texto>` | 200, combina mês + busca | OK        | |
| 1.6 | `GET /api/v1/resumo?mes=2026-13` (mês inválido) | 400 com código `MES_INVALIDO` (ou estrutura de erro padrão) | OK        | |
| 1.7 | `GET /api/v1/resumo?mes=abc` (formato inválido) | 400 | OK        | |
| 1.8 | Em qualquer resposta 200, conferir a invariante | `todos.quantidade == pendentes.quantidade + pagos.quantidade` e `todos.total == pendentes.total + pagos.total` | OK        | |
| 1.9 | Inspecionar o JSON | Campo se chama `mes` (não mais `mesAtual`) | OK        | |
| 1.10 | **Consistência com a listagem:** comparar `resumo.todos.quantidade` (de `?mes=X`) com o `total` de `GET /api/v1/pedidos?de=X-01&ate=X-31&status=todos` | Os dois números batem. Se divergirem, investigar CANCELADO (ver Observação 2) | OK        | |
| 1.11 | `GET /api/v1/resumo` **sem** JWT | 401 ou 403 (não 200) | OK        | |

---

### Task 2 — Pastas S3 separadas (via bot Telegram + console S3)

| # | Ação | Esperado | Resultado | Observação                                         |
|---|---|---|-----------|----------------------------------------------------|
| 2.1 | Enviar **foto** com legenda de pedido (ex: `100 boleto Energia`) | Bot confirma pedido. No bucket S3, arquivo aparece em `pedidos/YYYYMMDD/<uuid>.jpg` | OK        |                                                    |
| 2.2 | Enviar **foto** de comprovante (ex: `#<id_pendente> pix`) | Bot confirma comprovante. No bucket, arquivo aparece em `comprovantes/YYYYMMDD/<uuid>.jpg` | OK        |                                                    |
| 2.3 | Enviar **PDF como arquivo** com legenda de pedido (ex: `200 ted João`) | Arquivo em `pedidos/YYYYMMDD/<uuid>.pdf` (extensão correta) |           | Nao executado                                                     |
| 2.4 | Enviar **PDF como arquivo** de comprovante (ex: `#<id_pendente> boleto`) | Arquivo em `comprovantes/YYYYMMDD/<uuid>.pdf` |           | Nao executado                                      |
| 2.5 | Abrir o front (ou a URL direta) de um comprovante **antigo** (salvo antes da BE-16, sob `pedidos/`) | Continua carregando normalmente — URLs antigas não quebraram |           | Front ainda nao esta testavel integrado com o back |

---

## Cobertura automatizada (referência — não precisa validar manual)

Conforme relatório `docs/sprints/01-mvp/status/BE-16.md`: 226 testes, `BUILD SUCCESS`. Cobrem:
- `ResumoMesServiceImplTest` (9 casos: com/sem mês, com/sem busca, zeros)
- `ResumoControllerTest` (6 casos: validação 400, passagem de params)
- `S3ImageUploadServiceTest` (7 casos: prefixos corretos, chaves únicas, erro S3)
- `ResumoIntegrationTest` (5 casos: Testcontainers, busca, 400, 401)
- `PaymentRequestStrategyTest` / `PaymentProofStrategyTest` (verificam enum correto)

**Recomendado:** rodar `mvn clean verify` localmente e confirmar verde antes do merge.

---

## Resultado consolidado (preencher após os testes)

| Item | Resultado | Observações |
|---|---|---|
| Task 1 — resumo parametrizado | | |
| Task 2 — pastas S3 | | |
| `mvn clean verify` | | |
| **Recomendação geral** | mergear / ajustar / bloquear | |

---

## Para o Claude de planejamento (pós-validação)

Após preencher, decidir:
- Ordem de merge considerando que a branch carrega EVO-07 (Observação 1)
- Se trata a Observação 2 (CANCELADO) agora ou registra como pendência técnica
- Sincronizar o front (FE-12) quanto ao rename `mesAtual → mes`
