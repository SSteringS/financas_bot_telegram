# BE-19a — Idempotência: `mensagem_processada` + claim-then-process

> **Intake (contrato de entrada da task)**
>
> - **Origem:** `docs/architecture/adapter-whatsapp-cloud-api.md` §4.3. WhatsApp Cloud API entrega `messages` **at-least-once** → mesmo `wamid` pode chegar 2+ vezes e gerar pedidos/comprovantes duplicados sem dedup. Tabela agnóstica de canal (ADR 0013) serve Telegram (`update_id`) e WhatsApp (`wamid`) pelo mesmo mecanismo.
> - **Prioridade:** alta — pré-requisito da BE-19 (entrada WhatsApp).
> - **Esforço:** médio (migração + entity/repo + helper + integração no orchestrator + testes de integração).
> - **Território / quem executa:** `financas_bot_telegram/src/main/java/` + `src/main/resources/db/migration/` → **Claude do back**.
> - **Branch:** `feature/be-19a-idempotencia-mensagem-processada`, a partir de `develop`.
> - **Dependências:** **BE-17 em `develop`** ✅ (precisa do `MensagemEntranteService` em `application/services/` pra integrar o claim no início do processamento). Flyway já adotado (BE-00).
> - **Riscos:**
>   1. **Claim e processamento fora da MESMA transação** → ADR 0003 (sempre 200) + claim "vazado" = mensagem sumiria marcada como feita mas não persistida. Mitigação: garantir `@Transactional` único cobrindo claim + negócio; teste de integração que força rollback e confirma que claim some.
>   2. **Esquecer a unique key** (canal, id_externo) → dedup silenciosamente furada. Mitigação: criterio explícito + teste que insere duplicata e confirma DuplicateKey.
>   3. **Numeração da migração** — V1/V2/V3 já existem; próxima é V4. Conferir no diretório antes.

---

## Contexto

A spec §4.3 detalha o problema e a solução. Resumo crítico:

- **Não dá pra usar unicidade de negócio** (valor + descrição) — o usuário pode legitimamente mandar "150.00 Almoço" duas vezes. O único identificador estável é o **`id_externo` do canal** (`wamid` no WhatsApp, `update_id` no Telegram).
- **Tabela agnóstica** (`canal, id_externo`) serve os dois canais pela mesma chave única.
- **Cache em memória NÃO basta** — perde a cada restart; a Meta pode reentregar pós-deploy.
- **Claim-then-process na MESMA transação** — se o processamento falhar, rollback **remove** o claim, dando chance ao reenvio. Se commitar o claim antes, ADR 0003 (sempre 200) faz a mensagem sumir.

Esta task fica **agnóstica de canal**: a integração no `MensagemEntranteService` (criado pela BE-17) já roda pros dois adapters. Pós-BE-19a, o Telegram passa a ser idempotente **de graça** — ganho colateral.

## Decisão / abordagem

Migração Flyway V4 criando a tabela; entity + repository simples; um helper de claim que tenta `INSERT` e devolve um booleano; `MensagemEntranteService` (BE-17) ganha uma chamada de claim no **início** da transação de processamento — falha do claim = descarte silencioso (já processada).

### Esquema da tabela (§4.3)

```sql
CREATE TABLE mensagem_processada (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    canal VARCHAR(20) NOT NULL,            -- 'TELEGRAM' | 'WHATSAPP'
    id_externo VARCHAR(255) NOT NULL,      -- wamid (WhatsApp) ou update_id (Telegram)
    processado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_msg_canal_idexterno (canal, id_externo)
) ENGINE=InnoDB;
```

### Fluxo (claim-then-process)

1. Início da transação de processamento da mensagem entrante.
2. `INSERT INTO mensagem_processada (canal, id_externo) VALUES (?, ?)`.
   - Sucesso → segue pro passo 3.
   - `DataIntegrityViolationException` (unique key) → **já processada/processando** → não processa, retorna OK e segue. **Não loga ERROR** (não é erro, é dedup esperada).
3. Processa o negócio (pedido ou comprovante).
4. Commit: claim e escrita de negócio commitam juntos.

## Escopo / arquivos

**Criar:**

- `src/main/resources/db/migration/V4__criar_mensagem_processada.sql` — conforme schema acima. **Conferir o número V4 disponível** (`ls db/migration/`); se houver `V4__...` já, usar V5.
- `adapters/out/persistence/entity/MensagemProcessadaEntity.java` — `@Entity`, `@Table(name = "mensagem_processada")`, com `canal`/`idExterno`/`processadoEm`. Unique constraint declarada via `@Table(uniqueConstraints = ...)` pra ficar visível no Java (não é gate de DB, é doc).
- `adapters/out/persistence/repository/MensagemProcessadaRepository.java` — extends `JpaRepository<MensagemProcessadaEntity, Long>`. Sem queries adicionais — só o `save` que dispara o INSERT.
- `domain/model/CanalMensagem.java` — enum `{ TELEGRAM, WHATSAPP }`. **Decisão:** se BE-21a também precisar de enum análogo (`CanalNotificacao`), pesar se unifica. Pra esta task, criar separado é seguro; unificação fica como TODO se a BE-21a achar útil.
- `application/services/MensagemProcessadaService.java` (ou nome equivalente em `application/`) — método público `boolean tentarClaim(CanalMensagem canal, String idExterno)`:
  - Tenta `repository.save(new MensagemProcessadaEntity(canal, idExterno))`.
  - Captura `DataIntegrityViolationException` (chave única) → retorna `false`.
  - Sucesso → retorna `true`.
  - Anotar `@Transactional(propagation = REQUIRED)` — garante que entra na transação do caller.

**Modificar:**

- `application/services/MensagemEntranteService.java` (criado pela BE-17) — no método de processar mensagem:
  1. Antes de despachar pra strategy, chamar `mensagemProcessadaService.tentarClaim(...)`.
  2. Se `false`, logar `INFO` ("mensagem duplicada descartada") e retornar — **não** chamar strategy.
  3. Se `true`, segue o fluxo atual (strategy + usecase).
  4. **Crítico:** todo o método precisa estar dentro de **uma única `@Transactional`** — o caller já deve garantir (é orchestrator chamado pelo controller, que abre a transação). Confirmar no estado-atual ou adicionar `@Transactional` aqui se necessário.

**Não tocar:** `frontend/`, infra, controllers diretamente. A integração é toda em `application/`.

## Critérios de aceitação

- [ ] Migração `V4__criar_mensagem_processada.sql` aplicada limpa no Testcontainers MySQL (testes de integração).
- [ ] Unique key `(canal, id_externo)` presente no DDL e funcional — teste insere a mesma tupla 2x e a segunda lança `DataIntegrityViolationException`.
- [ ] `MensagemProcessadaService.tentarClaim`:
  - 1ª chamada com `(WHATSAPP, "wamid.X")` → `true`.
  - 2ª chamada com `(WHATSAPP, "wamid.X")` → `false` sem exception.
  - Chamada com `(TELEGRAM, "wamid.X")` → `true` (canal diferente, OK).
- [ ] Integração no `MensagemEntranteService`:
  - Teste com a **mesma mensagem entrando 2x** (mock do controller chamando processar duas vezes) → primeiro chamado cria pedido; segundo retorna sem criar nada e não lança exception.
  - Teste com **falha de negócio dentro da transação** → rollback remove o claim → reenvio funciona.
- [ ] `mvn test` verde (incluindo testes existentes do Telegram — não pode regredir).
- [ ] `mvn package -DskipTests` ok.
- [ ] Branch saiu de `develop`; território só `financas_bot_telegram/`.
- [ ] Status report `docs/sprints/02-canal-whatsapp/status/BE-19a.md` com frontmatter válido + nota se enum `CanalMensagem` foi criado/unificado.

## Coordenação

- **BE-17** já está em `develop` — `MensagemEntranteService` existe e é o ponto de integração único do claim.
- **BE-19** (entrada WhatsApp) **depende desta** pra cumprir §4.3.
- **Telegram** ganha idempotência de graça — confirmar no smoke test (`RUNBOOK-smoke-test-telegram-cadeia.md`, cenário 6 opcional) que reentregar o mesmo `update_id` não cria pedido duplicado.
- **BE-21a** pode precisar de enum similar (`CanalNotificacao`). Esta task cria `CanalMensagem`; BE-21a decide se unifica.

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build, lint na, testes verdes incluindo integração com Testcontainers, branch, território), status report válido, e **revisão do Reviewer** — toca a cadeia de processamento e o esquema de banco. Abrir PR pra `develop`; não mergear sozinho.

## Referências

- `docs/architecture/adapter-whatsapp-cloud-api.md` §4.3 (a fonte da decisão de modelagem e fluxo).
- ADR 0003 (controller webhook sempre 200 — explica por que mesma transação).
- ADR 0013 (multi-canal — explica por que a tabela é agnóstica).
- `docs/architecture/estado-atual.md` §6 (Flyway já adotado).
- `docs/runbooks/RUNBOOK-smoke-test-telegram-cadeia.md` (cenário 6 cobre validação da idempotência também no canal Telegram).
</content>
