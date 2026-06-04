---
task: FIX-003
titulo: "Gaps de cobertura qa-test-specialist — FecharMesServiceImpl (B1, B2, B3)"
data: 2026-06-04
branch: fix/003-qa-gaps-fechar-mes
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: ok
  testes: ok
  testes_total: 321
  testes_novos: 3
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits: []
pr: null
desvios: 0
pendencias_humano: 0
---

# FIX-003 — Gaps de cobertura qa-test-specialist — FecharMesServiceImpl

## O que foi feito

O qa-test-specialist identificou três gaps bloqueantes nas entregas BE-027/028/029 (sprint 03 folha de pagamento):

- **B1 — filtro `dataInicio`**: o critério `dataInicio <= primeiroDoMes` do passo 5 (seleção de adiantamentos para desconto) nunca havia sido testado explicitamente. Adicionados dois testes em `FecharMesServiceImplTest`: um para adiantamento com início posterior ao mês (deve ser ignorado, `valorFinal = salárioBase`) e um para o caso-limite em que `dataInicio == primeiroDoMes` (deve ser incluído — `<=`, não `<`).

- **B2 — race condition `DataIntegrityViolationException`**: a cláusula `catch (DataIntegrityViolationException)` do passo 8, responsável por traduzir duplicação concorrente para `FechamentoDuplicadoException`, nunca havia sido exercida. Adicionado teste que faz `existsFolha` retornar `false` (passa o check) mas `pedidoRepository.save()` lança `DataIntegrityViolationException`. O teste verifica que a exceção é re-lançada como `FechamentoDuplicadoException` com mensagem contendo o mês.

- **B3 — atomicidade transacional com banco real**: o `@Transactional` de `FecharMesServiceImpl.fechar()` cobre 11 passos, incluindo escrita no banco (passo 8: INSERT pedido FOLHA, passo 9: markAllClosed, passo 10: UPDATE adiantamentos). Adicionado teste de integração em `FecharMesIntegrationTest` que usa `@SpyBean AdiantamentoRepositoryPortOut` para injetar `RuntimeException` no passo 10 e verifica via `jdbcTemplate` que nenhum pedido FOLHA existe após a falha (rollback completo).

Além dos testes, esta branch também formaliza o fluxo Reviewer + QA que estava sendo executado informalmente: documentação adicionada em `CLAUDE.md` (seção "Definição de pronto"), em `docs/roles/backend.md` (checklist do papel) e na memória do agente (`.claude/agent-memory/backend/feedback_chamar_reviewer_e_qa.md`).

---

## Desvios do plano

Nenhum. Esta task não tem plano formal — é um fix de gap apontado pelo qa-test-specialist após BE-027/028/029.

---

## Decisões tomadas durante a execução

**B3 via `@SpyBean` em vez de `@MockBean` isolado**: optou-se por `@SpyBean AdiantamentoRepositoryPortOut` (spy sobre a implementação real) e não por `@MockBean` para que a execução do passo 10 antes da exceção seja real (o adiantamento realmente existe no banco e o adapter JPA realmente é invocado). O spy é resetado via `@AfterEach resetarSpy()` para não vazar para outros testes na mesma classe.

**Mês `2026-07` para o B3**: os outros testes do `FecharMesIntegrationTest` usam `2026-05`, `2026-04`, `2026-06`, `2026-03`, `2026-02`. O B3 usa `2026-07` para evitar qualquer colisão de idempotência com o `@BeforeEach`/`@AfterEach` compartilhado.

**Duas anotações `@AfterEach`**: JUnit 5 permite múltiplos métodos `@AfterEach` na mesma classe. `tearDown()` limpa o banco; `resetarSpy()` reseta o spy. Isso mantém as responsabilidades separadas.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- O `@SpyBean` em `FecharMesIntegrationTest` causa criação de um contexto Spring separado (diferente dos outros `*IntegrationTest` que não têm spy). Não é problema de corretude, mas é uma alocação extra de contexto no CI — aceitável dado o valor do teste.
- **Reviewer pendente**: por ADR 0005, a revisão independente deve ser uma sessão separada do Claude. O implementador não pode chamar o Reviewer programaticamente. Após mergear esta fix, iniciar uma sessão de Reviewer com: branch `integration/03-folha-pagamento`, diff de BE-027/028/029 + FIX-001/002/003.

---

## Padrões técnicos

**B1/B2 — Teste de lógica de domínio via Mockito puro (DIP):**
`FecharMesServiceImplTest` injeta o serviço com mocks das três ports (`FuncionarioRepositoryPortOut`, `PedidoPagamentoRepositoryPort`, `AdiantamentoRepositoryPortOut`). Os testes B1 e B2 exercem lógica que pertence exclusivamente ao domínio da aplicação (regra de filtro de datas, tradução de exceção de infraestrutura). Nenhum adapter é instanciado — princípio DIP preservado.

**B3 — Strategy de spy para teste transacional:**
O B3 precisa de um banco real para verificar rollback. Usar `@MockBean` no repositório tornaria o teste trivial (a transação não existiria, pois o JPA não seria invocado). `@SpyBean` mantém o adapter JPA real mas permite injetar uma falha precisa no momento certo. Esta é a única exceção ao padrão de testes unitários puros — justificada porque o comportamento sob teste (rollback `@Transactional`) só existe com banco real.

**Arquitetura hexagonal:**
Os três gaps existiam no use case `FecharMesServiceImpl` (camada `application/services/`). As correções são apenas testes — o código de produção já estava correto. A fix mantém o isolamento: B1/B2 testam a lógica da application sem conhecer adapters; B3 testa o comportamento E2E via HTTP + banco, sem acessar beans internos diretamente.

---

## Arquivos criados/modificados

- `financas_bot_telegram/src/test/.../services/FecharMesServiceImplTest.java` (modificado: +3 testes B1 × 2, B2 × 1)
- `financas_bot_telegram/src/test/.../integration/FecharMesIntegrationTest.java` (modificado: +B3, @SpyBean, @AfterEach resetarSpy)
- `CLAUDE.md` (modificado: obrigatoriedade do Reviewer + qa-test-specialist na definição de pronto)
- `docs/roles/backend.md` (modificado: checklist do papel atualizado com gates de Reviewer e QA)
- `.claude/agent-memory/backend/feedback_chamar_reviewer_e_qa.md` (novo: memória da obrigação de chamar Reviewer + QA)
- `.claude/agent-memory/backend/MEMORY.md` (modificado: +entrada para feedback_chamar_reviewer_e_qa)
