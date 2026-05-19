# BE-16 — Resumo parametrizado + separação de pastas S3

## Contexto

Esta task agrupa duas mudanças no backend que serão entregues juntas numa única branch/PR:

**Task 1 — Resumo parametrizado.** O endpoint `GET /api/v1/resumo` hoje só devolve o mês corrente e não aceita parâmetros. Isso causa dois bugs visíveis no front (revisão FE-07 e FE-08):
- **Bug A (contadores):** o `FiltroStatus` da Home calcula `pendente`/`pago` em cima de `data.items` da listagem, que já vem filtrada pelo status ativo. Clicar em "Pendente" zera "Pago". A solução estrutural é os contadores virem de uma agregação independente do filtro de status.
- **Bug B (header travado):** o cabeçalho mostra "Olá Pedro, 3 pedidos pendentes" mesmo quando o usuário troca de mês. Causa: o endpoint não aceita mês.

Ambos pedem o mesmo recurso: **agregação por status, parametrizada por mês e busca**. O `/api/v1/resumo` é o ponto natural — basta parametrizar.

**Task 2 — Separar pastas S3.** Hoje o `S3ImageUploadService` salva tudo (foto de pedido + comprovante) num único prefixo hardcoded `pedidos/`. Identificado pelo humano ao validar arquivos no bucket após `feature/backend-polish-evo07`. Em volume baixo é só inconveniente; em volume alto vira fonte real de confusão (auditoria, debug, eventual ferramenta admin).

As duas tasks são independentes em escopo mas vão na mesma branch pra reduzir overhead de PR/review.

## Branch

Criar nova branch a partir de `develop`:

```
feature/be-16-resumo-e-pastas-s3
```

Não usar a `feature/backend-polish-evo07` (que está esperando review separado). Ver "Coordenação" no final pra detalhe sobre conflitos previsíveis.

---

## Task 1 — Resumo parametrizado

### Contrato novo

```
GET /api/v1/resumo?mes=YYYY-MM&busca=texto
```

#### Query params

| Param | Tipo | Obrigatório | Default | Descrição |
|---|---|---|---|---|
| `mes` | string | não | mês corrente (`YearMonth.now()`) | Formato `YYYY-MM`. Filtra `dataPedido` entre o primeiro e o último dia do mês. |
| `busca` | string | não | sem filtro | Texto pra filtrar `descricao` (contém, case-insensitive). Mesma semântica do `busca` em `GET /api/v1/pedidos`. |

#### Validação

- `mes` em formato inválido → `400` com erro estruturado (mesmo padrão dos outros endpoints). Aceitar parsing via `YearMonth.parse(mes)`.
- `mes` vazio ou ausente → default = mês corrente.
- `busca` vazia → tratar como ausente (sem filtro).

#### Response (200)

```json
{
  "mes": "2026-05",
  "todos":     { "quantidade": 6, "total": 7227.40 },
  "pendentes": { "quantidade": 3, "total": 5239.90 },
  "pagos":     { "quantidade": 3, "total": 1987.50 }
}
```

Mudanças em relação ao contrato atual:
- Campo `mesAtual` renomeado para `mes` (mais correto agora que o endpoint aceita qualquer mês).
- Novo campo `todos: { quantidade, total }` — agregado do recorte inteiro (pendentes + pagos do filtro mes + busca).

Não há mudança de status/erros além do 400 para mês inválido.

### Implementação

**Controller**
- Adicionar `@RequestParam(required = false) String mes` e `@RequestParam(required = false) String busca`.
- Parsing/validação de `mes` via `YearMonth.parse(...)` dentro de um try/catch que devolve 400.
- Passar `YearMonth` + `busca` (pode ser null/blank) pro service.

**Service**
- Calcular `de` = primeiro dia do mês, `ate` = último dia.
- Repositório agrega: `SUM(valor)` e `COUNT(*)` agrupados por `status`, filtrando `dataPedido BETWEEN de AND ate` e (se busca não for nula/branca) `LOWER(descricao) LIKE %lower(busca)%`. Considerar também o `requisitanteId` autenticado, conforme padrão dos outros endpoints.
- Montar DTO: `pendentes` e `pagos` vêm das linhas do `GROUP BY`; `todos` = soma dos dois.
- Se algum status não tiver linha, mandar `quantidade: 0, total: 0`.

**DTO de resposta**
- Atualizar `ResumoMes` (ou nome equivalente) com: `mes`, `todos`, `pendentes`, `pagos`.

**OpenAPI / Swagger**
- Anotar `@Operation` no endpoint com descrição dos novos params.
- `@Parameter` em `mes` e `busca` com exemplo e descrição.
- Conferir que a UI do Swagger mostra os params como opcionais.

---

## Task 2 — Separar pastas S3 (pedidos vs comprovantes)

### Objetivo

Particionar o S3 por **propósito do upload**:
- `bucket/pedidos/YYYYMMDD/<uuid>.<ext>` — foto enviada junto com o pedido novo
- `bucket/comprovantes/YYYYMMDD/<uuid>.<ext>` — comprovante de pagamento de pedido existente

Particionamento por data e UUID se mantêm.

### Implementação

**1. Novo enum** (em `domain/enums/` ou `adapters/out/s3/`):

```java
public enum TipoUploadS3 {
  PEDIDO("pedidos"),
  COMPROVANTE("comprovantes");

  private final String folder;

  TipoUploadS3(String folder) { this.folder = folder; }
  public String getFolder() { return folder; }
}
```

**2. `S3ImageUploadService` — receber tipo de upload**

Substituir o `FOLDER_PREFIX` estático por parâmetro:

```java
public String uploadImage(byte[] imageBytes, TipoUploadS3 tipoUpload) { ... }

private String generateS3Key(TipoUploadS3 tipoUpload) {
  String today = LocalDate.now().format(DATE_FORMATTER);
  String fileName = UUID.randomUUID() + FILE_EXTENSION;
  return String.format("%s/%s/%s", tipoUpload.getFolder(), today, fileName);
}
```

**3. Callers — passar o tipo correto**

- `PaymentRequestStrategy.process()`:
  ```java
  String s3ImageUrl = s3ImageUploadService.uploadImage(imageBytes, TipoUploadS3.PEDIDO);
  ```
- `PaymentProofStrategy.process()`:
  ```java
  String s3ImageUrl = s3ImageUploadService.uploadImage(imageBytes, TipoUploadS3.COMPROVANTE);
  ```

### Não migrar arquivos antigos

Arquivos já em S3 sob `pedidos/` (que misturam pedidos e comprovantes) **ficam onde estão**. Os URLs guardados no banco continuam apontando pra eles e seguem funcionando.

A separação vale só pra uploads novos a partir do deploy.

**Justificativa:** migração envolveria copiar arquivos no S3 (custo, risco) + atualizar URLs no banco (transação ampla). O ganho é só "limpeza visual"; o sistema funciona normalmente sem migrar. Se um dia precisar (auditoria, ferramenta admin), dá pra escrever um script de migração isolado.

Documentar isso no commit message: "novos uploads particionam por tipo; arquivos antigos sob `pedidos/` continuam acessíveis pelas URLs originais."

---

## Testes

### Task 1 — Resumo

**Unitários (service)**
- `default` (sem mes, sem busca) → traz mês corrente sem filtro de texto.
- `com mes específico` → traz dados só do mês pedido.
- `com busca` → filtra `descricao`.
- `com mes + busca` → combina os dois.
- Mês com 0 pedidos → todos os campos devolvem `0` (não null).
- Pedidos com mistura de status → contagens e totais corretos.

**Integração (Testcontainers)**
- Insere 4 pedidos (2 maio pendentes, 1 maio pago, 1 abril pago) → chama `GET /api/v1/resumo?mes=2026-05` → confere payload completo.
- Chama com `busca` específica → confere que filtra.
- Chama com `mes` inválido (`2026-13`) → confere 400.
- Chama sem mes (default) → confere que devolve dados do mês corrente.

### Task 2 — S3 folders

**`S3ImageUploadServiceTest`**
- `upload de PEDIDO gera chave com prefixo "pedidos/"`
- `upload de COMPROVANTE gera chave com prefixo "comprovantes/"`
- (Manter) `chave inclui data corrente no formato YYYYMMDD`
- (Manter) `UUID único por chamada`

**`PaymentRequestStrategyTest` e `PaymentProofStrategyTest`**
Verificar (via mock do `S3ImageUploadService`) que cada strategy chama com o enum correto:

```java
verify(s3ImageUploadService).uploadImage(any(), eq(TipoUploadS3.PEDIDO));
```

---

## Critérios de aceite

### Task 1
- [ ] Endpoint `/api/v1/resumo` responde corretamente nos 4 cenários (default / mes / busca / mes+busca)
- [ ] Mês inválido → 400 com erro estruturado
- [ ] Resposta inclui `todos`, `pendentes`, `pagos` com `quantidade` e `total` numéricos (nunca null)
- [ ] Swagger UI mostra params opcionais com descrição

### Task 2
- [ ] Existe `TipoUploadS3` (ou nome equivalente) com pelo menos `PEDIDO` e `COMPROVANTE`
- [ ] `S3ImageUploadService` aceita o tipo e gera chave com folder apropriado
- [ ] `PaymentRequestStrategy` chama com `PEDIDO`; `PaymentProofStrategy` chama com `COMPROVANTE`
- [ ] Hardcode `FOLDER_PREFIX = "pedidos"` removido do service

### Geral
- [ ] Todos os testes (existentes + novos) passam
- [ ] `mvn clean verify` passa local
- [ ] Sem regressão em outros endpoints
- [ ] Validação manual pós-deploy (S3): enviar 1 foto de pedido + 1 comprovante; conferir no bucket que aparecem em `pedidos/YYYYMMDD/` e `comprovantes/YYYYMMDD/` respectivamente

### Commits

Recomendado: dois commits semanticamente separados na mesma branch — `feat(BE-16): resumo aceita mes e busca, adiciona todos` e `fix(BE-16): separar pastas S3 pedidos vs comprovantes` — pra facilitar reverter um sem o outro se preciso.

---

## Coordenação

### Com o front (FE-12)

O Claude do front está trabalhando em paralelo na branch `feature/frontend-fase3-completa` consumindo o contrato do `/api/v1/resumo` via MSW mock. O contrato definido aqui é a fonte de verdade — qualquer alteração precisa ser sincronizada antes de mergear.

### Com `feature/backend-polish-evo07`

A branch `feature/backend-polish-evo07` está em review separado e contém EVO-07, que **modifica a assinatura do `S3ImageUploadService`** (de `uploadImage(bytes)` para `uploadFile(bytes, TipoArquivo)`).

A Task 2 deste plano também modifica essa mesma classe — o que vai gerar **conflito previsível** quando uma das duas branches mergear primeiro.

**Como resolver o conflito quando aparecer (na hora do merge):**
- A assinatura final do método deve aceitar **os dois enums**: `uploadFile(bytes, TipoArquivo, TipoUploadS3)` (ou nomes equivalentes).
- `TipoArquivo` representa o formato (IMAGEM/PDF, da EVO-07).
- `TipoUploadS3` representa o propósito (PEDIDO/COMPROVANTE, deste plano).
- Os dois enums coexistem — **não confundir**.

A ordem mais limpa é polish-evo07 mergear primeiro, depois esta branch rebase em cima da develop atualizada e ajusta a assinatura. Mas se esta branch mergear primeiro, o ajuste vai pro rebase da polish-evo07. Qualquer das duas ordens funciona.

---

## Referências

- Bug A documentado em `docs/aprendizado/react-tanstack-query.md` (FE-07)
- Bug B documentado em `docs/aprendizado/react-estado-derivado-e-composicao-de-hooks.md` (FE-08)
- Plano do front que consome o novo contrato: `docs/plans/FE-12-resumo-parametrizado-e-contadores.md`
- Observação original sobre S3: `docs/avaliacoes/backend-polish-evo07.md`
- Pendência técnica relacionada (não escopo desta task): comprovante mal-formado classificado como pedido — `docs/PENDENCIAS-TECNICAS.md`
- Código afetado (Task 2):
  - `financas_bot_telegram/src/main/java/.../adapters/out/s3/service/S3ImageUploadService.java`
  - `financas_bot_telegram/src/main/java/.../adapters/in/telegram/strategy/PaymentRequestStrategy.java`
  - `financas_bot_telegram/src/main/java/.../adapters/in/telegram/strategy/PaymentProofStrategy.java`
