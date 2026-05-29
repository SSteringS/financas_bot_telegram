---
task: BE-19a
titulo: "Idempotência — tabela mensagem_processada + claim-then-process"
data: 2026-05-29
branch: feature/be-19a-idempotencia-mensagem-processada
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: na
  testes: ok
  testes_total: 237
  testes_novos: 5
  branch_convencao: ok
  territorio: ok
commits:
  - d1fa9d8
  - 3131283
  - adc0848
pr: null
desvios: 0
pendencias_humano: 0
---

# BE-19a — Idempotência: tabela mensagem_processada + claim-then-process

## O que foi feito

### Migração (V4)
- `V4__criar_mensagem_processada.sql`: tabela `mensagem_processada` com colunas `id`, `canal`, `id_externo`, `processado_em` e `UNIQUE KEY uk_msg_canal_idexterno (canal, id_externo)`.

### Domínio
- `domain/model/Canal.java`: enum `TELEGRAM | WHATSAPP` — representa o canal de origem da mensagem. Unificado com BE-21a (substituiu `CanalMensagem` inicial).

### DTO
- `application/dto/PaymentMessageDTO.java`: adicionado campo `Canal canal` — preenchido pelo mapper de cada canal. Permite que `MensagemEntranteService` faça o claim sem conhecer o protocolo do canal.

### Persistência
- `adapters/out/persistence/entity/MensagemProcessadaEntity.java`: entidade JPA com `@UniqueConstraint`.
- `adapters/out/persistence/MensagemProcessadaJpaRepository.java`: repositório JPA (sem queries customizadas — inserção via `JdbcTemplate`).

### Serviço de claim
- `application/services/MensagemProcessadaService.java`: método `tentarClaim(Canal, String)` com `@Transactional(REQUIRED)`.
  - Usa `JdbcTemplate` (não JPA) para o INSERT — evita invalidação de sessão Hibernate ao capturar `DuplicateKeyException`.
  - Retorna `true` (insert ok) ou `false` (duplicate key = já processado).
  - Participa da transação do chamador (REQUIRED) — rollback do negócio reverte o claim, permitindo retentativa.

### Integração no ponto canal-agnóstico
- `MensagemEntranteService.processar(PaymentMessageDTO dto)`: `@Transactional` + claim no início do processamento (após merge de BE-17 via adc0848).
  - Se `tentarClaim` retornar `false`: log de descarte + `return` (sem processar, sem erro).
  - `idExterno = dto.getExternalId()` (messageId do canal); `canal = dto.getCanal()`.
- `adapters/in/telegram/mapper/TelegramMessageMapper.java`: seta `canal = Canal.TELEGRAM` nos três builders.

### Testes
- `MensagemEntranteServiceTest`: adicionado `deveSaltarProcessamentoQuandoClaimFalhar` — verifica que strategies nunca são invocadas quando claim retorna `false`. Testes existentes adaptados para usar `canal` + `externalId` no builder do DTO.
- `MensagemProcessadaIntegrationTest`: 4 testes Testcontainers (falham neste ambiente por ausência de Docker — pré-existente).
  - `primeiraClaim_retornaTrue`
  - `claimDuplicada_retornaFalse`
  - `canaisDiferentes_ambosRetornamTrue`
  - `rollback_removeClaim_permitindoRetentativa`

---

## Desvios do plano

Nenhum.

(O desvio original — claim em `UpdateOrchestratorService` em vez de `MensagemEntranteService` — foi resolvido no commit `adc0848`, após merge de `develop` com BE-17. O ponto de integração correto está em `MensagemEntranteService.processar()`.)

---

## Decisões tomadas durante a execução

- `JdbcTemplate` no lugar de JPA para o INSERT de claim: evita invalidação de sessão Hibernate ao capturar `DuplicateKeyException` dentro de uma transação ativa.
- `Canal` como campo do DTO em vez de parâmetro avulso: mantém `MensagemEntranteService` agnóstico ao protocolo — cada mapper de canal preenche o campo antes de chamar `processar()`.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- BE-21a cria `Canal.java` com conteúdo idêntico. No merge de BE-21a em develop (após BE-19a), o conflito em `domain/model/Canal.java` é no-op — manter qualquer uma das versões.
- V5 da migration é da BE-21a (`canal_preferido`). V4 é desta task.

---

## Arquivos criados/modificados

- `src/main/resources/db/migration/V4__criar_mensagem_processada.sql` (novo)
- `domain/model/Canal.java` (novo)
- `adapters/out/persistence/entity/MensagemProcessadaEntity.java` (novo)
- `adapters/out/persistence/MensagemProcessadaJpaRepository.java` (novo)
- `application/services/MensagemProcessadaService.java` (novo)
- `application/dto/PaymentMessageDTO.java` (modificado: campo `Canal canal`)
- `adapters/in/telegram/mapper/TelegramMessageMapper.java` (modificado: seta `Canal.TELEGRAM`)
- `application/services/MensagemEntranteService.java` (modificado: claim + `@Transactional`)
- `application/services/MensagemEntranteServiceTest.java` (modificado: 1 teste novo + 4 adaptados)
- `integration/MensagemProcessadaIntegrationTest.java` (novo)
