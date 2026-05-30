---
task: FIX-idempotencia-porta-application
titulo: "Extrair porta IdempotenciaMensagemPort (separar interface da implementação)"
data: 2026-05-29
branch: fix/idempotencia-porta-application
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: na
  testes: ok
  testes_total: 248
  testes_novos: 0
  branch_convencao: ok
  territorio: ok
commits:
  - d3d8c40
pr: null
desvios: 0
pendencias_humano: 0
---

# FIX-idempotencia-porta-application — Extrair porta IdempotenciaMensagemPort

## O que foi feito

Movimentação cirúrgica de camada — comportamento externo idêntico, SQL idêntico, `DuplicateKeyException` idêntico.

### Criado

- `application/port/out/IdempotenciaMensagemPort.java`: interface com `boolean tentarClaim(Canal, String)`. Semântica de intenção ("reservar processamento"), não de implementação.

- `adapters/out/persistence/idempotencia/JdbcIdempotenciaMensagemAdapter.java`: implementação `@Component`. Toda a lógica que estava em `MensagemProcessadaService` foi movida aqui sem alteração: `JdbcTemplate`, SQL `INSERT INTO mensagem_processada`, `@Transactional(propagation = REQUIRED)`, `try/catch DuplicateKeyException`.

### Removido

- `application/services/MensagemProcessadaService.java`: **absorvido pelo adapter** (ver decisão abaixo). Só envelopava `JdbcTemplate` — nenhuma lógica de aplicação.

### Modificado

- `application/services/MensagemEntranteService.java`: campo/parâmetro `MensagemProcessadaService` substituído por `IdempotenciaMensagemPort`. Zero imports de `org.springframework.jdbc.*` em `application/`.
- `MensagemEntranteServiceTest.java`: `@Mock MensagemProcessadaService` → `@Mock IdempotenciaMensagemPort`. 5 testes, 0 falhas — intenção preservada.
- `MensagemProcessadaIntegrationTest.java`: `@Autowired MensagemProcessadaService` → `@Autowired IdempotenciaMensagemPort`. 4 testes (falham por Docker ausente — pré-existente).

---

## Decisão: MensagemProcessadaService absorvido pelo adapter

`MensagemProcessadaService` continha **apenas** a lógica de infra (`JdbcTemplate` + `DuplicateKeyException`) sem nenhuma lógica genuína de aplicação. Não havia derivação de `idExterno`, validações de negócio, nem composição com outros serviços.

Decisão: **ele some**. `MensagemEntranteService` (o único caller) passa a injetar `IdempotenciaMensagemPort` diretamente. A indireção extra (service de aplicação delegando para porta delegando para adapter) seria overhead sem valor.

---

## Validação do critério principal

```
grep -r "org\.springframework\.jdbc" src/main/java/.../application
```

**Resultado: VAZIO** (exit code 1 — nenhuma ocorrência). A camada de aplicação está limpa de imports de infraestrutura.

---

## Desvios do plano

Nenhum.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- `MensagemProcessadaJpaRepository` ainda existe em `adapters/out/persistence/` — não foi usado pelo JdbcTemplate e não foi tocado nesta task (fora do escopo). Se não houver uso real, pode ser removido numa limpeza futura.

---

## Arquivos criados/modificados

- `application/port/out/IdempotenciaMensagemPort.java` (novo)
- `adapters/out/persistence/idempotencia/JdbcIdempotenciaMensagemAdapter.java` (novo — recebe lógica do service removido)
- `application/services/MensagemProcessadaService.java` (removido)
- `application/services/MensagemEntranteService.java` (modificado: injeta porta)
- `MensagemEntranteServiceTest.java` (modificado: mock da porta)
- `integration/MensagemProcessadaIntegrationTest.java` (modificado: autowire da porta)
