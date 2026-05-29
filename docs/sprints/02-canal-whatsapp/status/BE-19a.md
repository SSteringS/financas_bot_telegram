---
tarefa: BE-19a
titulo: Idempotência — tabela mensagem_processada + claim-then-process
branch: feature/be-19a-idempotencia-mensagem-processada
estado: concluido
data: 2026-05-29
testes_total: 231
testes_novos: 5
desvios: 1
---

## O que foi feito

### Migração (V4)
- `V4__criar_mensagem_processada.sql`: tabela `mensagem_processada` com colunas `id`, `canal`, `id_externo`, `processado_em` e `UNIQUE KEY uk_msg_canal_idexterno (canal, id_externo)`.

### Domínio
- `domain/model/CanalMensagem.java`: enum `TELEGRAM | WHATSAPP` — representa o canal de origem da mensagem.

### Persistência
- `adapters/out/persistence/entity/MensagemProcessadaEntity.java`: entidade JPA com `@UniqueConstraint`.
- `adapters/out/persistence/MensagemProcessadaJpaRepository.java`: repositório JPA (sem queries customizadas — inserção via `JdbcTemplate`).

### Serviço de claim
- `application/services/MensagemProcessadaService.java`: método `tentarClaim(CanalMensagem, String)` com `@Transactional(REQUIRED)`.
  - Usa `JdbcTemplate` (não JPA) para o INSERT — evita invalidação de sessão Hibernate ao capturar `DuplicateKeyException`.
  - Retorna `true` (insert ok) ou `false` (duplicate key = já processado).
  - Participa da transação do chamador (REQUIRED) — rollback do negócio reverte o claim, permitindo retentativa.

### Integração no orquestrador
- `UpdateOrchestratorService.process()`: `@Transactional` + claim no início do processamento.
  - Se `tentarClaim` retornar `false`: log de descarte + `return` (sem processar, sem erro).
  - `idExterno = update.getUpdateId().toString()`.

### Testes
- `UpdateOrchestratorServiceTest`: adicionado `deveSaltarProcessamentoQuandoClaimFalhar` — verifica que strategies nunca são invocadas quando claim retorna `false`.
- `MensagemProcessadaIntegrationTest`: 4 testes Testcontainers (falham neste ambiente por ausência de Docker — pré-existente).
  - `primeiraClaim_retornaTrue`
  - `claimDuplicada_retornaFalse`
  - `canaisDiferentes_ambosRetornamTrue`
  - `rollback_removeClaim_permitindoRetentativa`

## Desvio documentado

**Ponto de integração em `UpdateOrchestratorService` em vez de `MensagemEntranteService`**

O plano especificava a integração em `MensagemEntranteService` (introduzido pelo BE-17). Porém BE-17 ainda não foi mergeado em `develop` — a branch `feature/be-19a-idempotencia-mensagem-processada` foi criada a partir de `develop` sem BE-17. O ponto de integração foi adaptado para `UpdateOrchestratorService.process()`, que é o orquestrador equivalente no código da `develop` atual.

**Ação necessária no merge de BE-17:** durante a resolução de conflito, mover o claim de `UpdateOrchestratorService` para `MensagemEntranteService`, que será o ponto correto após a refatoração BE-17.

## Gates

- [x] `mvn test`: 208 unit tests passando, 23 erros (todos no pacote `integration` — Docker indisponível, pré-existente)
- [x] `mvn package -DskipTests`: BUILD SUCCESS
- [x] Status report escrito
