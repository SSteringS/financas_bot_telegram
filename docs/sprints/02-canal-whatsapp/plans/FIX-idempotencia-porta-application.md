# FIX — Extrair porta `IdempotenciaMensagemPort` (separar interface da implementação)

> **Intake (contrato de entrada da task)**
>
> - **Origem:** revisão humana do PR da **BE-19a**. Comportamento aprovado; o follow-up é apenas a **separação de camadas hexagonal**.
> - **Prioridade:** média. Refatoração arquitetural pura — comportamento externo não muda. Vai entrar **na sprint 02 como próxima task** após a fila de revisão de PRs assentar.
> - **Esforço:** baixo (criar porta, mover impl, ajustar consumidor). Sem mudança de SQL.
> - **Território / quem executa:** `financas_bot_telegram/src/main/java/` → **Claude do back**.
> - **Branch:** `fix/idempotencia-porta-application`, a partir de `develop`.
> - **Dependências:** **BE-19a em `develop`** ✅ (criou o consumidor atual e o `JdbcTemplate` direto).
> - **Riscos:** baixíssimo. É movimentação de classe entre camadas + introdução de interface, sem mudar lógica, SQL nem comportamento. Mitigação: testes da BE-19a passam **sem alteração**.

---

## Contexto

A BE-19a introduziu `MensagemProcessadaService.tentarClaim(Canal, String)` usando `JdbcTemplate` direto em `application/services/`. A motivação técnica está **correta** (`JdbcTemplate` evita invalidação da Hibernate Session em caso de `DuplicateKeyException`).

O problema é apenas de **direção de dependência hexagonal**:

- `application/services/MensagemProcessadaService` está em camada de aplicação, mas importa `JdbcTemplate` (infraestrutura).
- A regra: application depende de **portas**, não de adapters concretos.
- A justificativa "evitar invalidação da Session" é raciocínio de infra que vazou pra application.

Esta task **apenas separa a interface da implementação**. Lógica, SQL e tratamento de `DuplicateKeyException` continuam **idênticos** — só mudam de lugar.

## Decisão / abordagem

**Inversão de dependência simples — só isso.** Sem otimizar o SQL, sem trocar exceção por verificação de resultado, sem mexer no `noRollbackFor`. O comportamento existente está aprovado pelo Reviewer e pelo humano; o que falta é a fronteira de pacote.

> **Explicitamente descartado:** trocar `try/catch DuplicateKeyException` por `INSERT IGNORE` / `ON DUPLICATE KEY UPDATE`. Decisão do humano em 2026-05-28 — `INSERT IGNORE` fica como opção, **não como ação desta task**. Mantém o escopo cirúrgico.

## Escopo / arquivos

**Criar:**

- `application/port/out/IdempotenciaMensagemPort.java` — interface:
  ```java
  public interface IdempotenciaMensagemPort {
      /**
       * Tenta reservar (claim) o processamento de uma mensagem.
       * Idempotente: chamadas com a mesma (canal, idExterno) só conseguem claim uma vez.
       * @return true se este chamador fez o claim; false se já estava claimado.
       */
      boolean tentarClaim(Canal canal, String idExterno);
  }
  ```
  Semântica de **intenção** ("reservar processamento"), não de implementação ("inserir linha").

- `adapters/out/persistence/idempotencia/JdbcIdempotenciaMensagemAdapter.java` (ou caminho equivalente seguindo o padrão do projeto) — implementa a porta. Aqui mora **toda a lógica atual** que estava no `MensagemProcessadaService`: o `JdbcTemplate`, o SQL `INSERT`, o `try/catch DuplicateKeyException` retornando `false`. Anotado `@Component` pra o Spring registrar.

**Modificar:**

- O consumidor atual da operação (o `MensagemEntranteService` da BE-17, integrado pela BE-19a) — passa a receber **`IdempotenciaMensagemPort`** por construtor em vez do `MensagemProcessadaService` antigo (ou em vez do `JdbcTemplate` direto, dependendo de como ficou). Zero imports de `org.springframework.jdbc.*` em `application/`.

- **Decisão do implementador:** o `MensagemProcessadaService` ainda faz sentido?
  - Se ele só envelopava o `JdbcTemplate` (que agora foi pro adapter), **ele some** — o caller passa a usar a porta direto.
  - Se ele tem outra lógica genuína de aplicação (ex.: derivar `idExterno` por tipo de canal antes de chamar a porta), **ele permanece** como helper de aplicação, agora **consumindo a porta** em vez do `JdbcTemplate`.
  - Anotar a decisão no status report.

**Não tocar:**

- Schema da tabela `mensagem_processada`.
- SQL do INSERT (continua igual; só muda onde é executado).
- Tratamento de `DuplicateKeyException` (mesma lógica, mesma transação, mesmo `@Transactional`).
- Comportamento externo de idempotência (preservado em 100%).
- Enum `Canal`.

## Critérios de aceitação

- [ ] Porta `IdempotenciaMensagemPort` em `application/port/out/` com o método `tentarClaim(Canal, String): boolean`.
- [ ] **`grep` limpo na application:** sem imports de `org.springframework.jdbc.*` em `src/main/java/.../application/**`. Comando sugerido:
  ```
  grep -r "org\.springframework\.jdbc" src/main/java/.../application
  ```
  → não retorna nada.
- [ ] Adapter implementando a porta na camada de infra (`adapters/out/persistence/idempotencia/` ou equivalente), registrado como bean.
- [ ] **Testes da BE-19a passam sem alteração funcional** — pode ser que precise ajustar o mock/spy pra apontar pra porta em vez do service antigo, mas a *intenção* dos testes é a mesma.
- [ ] Decisão sobre o `MensagemProcessadaService` (mantido ou absorvido) anotada no status report com justificativa breve.
- [ ] `mvn test` verde · `mvn package -DskipTests` ok.
- [ ] Branch saiu de `develop`; território só `financas_bot_telegram/`.
- [ ] Status report `docs/sprints/02-canal-whatsapp/status/FIX-idempotencia-porta-application.md` com frontmatter válido.

## Fora de escopo (explicitamente)

- **Trocar SQL por `INSERT IGNORE` / `ON DUPLICATE KEY UPDATE`** — descartado pelo humano. `try/catch` segue como está, só muda de pacote.
- **Mexer no `@Transactional(noRollbackFor = ...)`** se existir — continua igual.
- Outros usos de `JdbcTemplate` no projeto.
- Mudar estratégia de idempotência (continua claim-by-insert sobre unique constraint).
- Refatoração do modelo `MensagemProcessada` ou semântica de `Canal`.

## Coordenação

- **Próxima da fila do back** depois que os PRs em revisão fecharem.
- **Risco baixo** — refator de movimentação, comportamento preservado.
- **Reviewer foca em:** (a) `grep` confirmando que application ficou limpa de imports de infra, (b) testes preservados sem alteração de intenção, (c) decisão sobre o `MensagemProcessadaService` documentada.
- **Coordenação meta (RETRO-02):** essa observação foi pega pelo humano, não pelo Reviewer. Está registrado como tópico pra RETRO-02 no `BACKLOG-evolucao-workflow.md` — checklist arquitetural explícito no `reviewer.md`.

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, e **revisão do Reviewer** — toca a estrutura hexagonal. Abrir PR pra `develop`; não mergear sozinho.

## Referências

- BE-19a (`docs/sprints/02-canal-whatsapp/plans/BE-19a-idempotencia-mensagem-processada.md`) — task original.
- Discussão de revisão humana (2026-05-28) — brief original do humano com as 3 etapas + a opção do `INSERT IGNORE` que foi **descartada**.
- ADR 0004 (taxonomia — hexagonal e camadas).
- `docs/plans/BACKLOG-evolucao-workflow.md` item #8 (melhoria do Reviewer pra RETRO-02).
</content>
