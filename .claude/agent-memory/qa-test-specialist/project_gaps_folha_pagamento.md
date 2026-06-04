---
name: Gaps recorrentes no domínio de folha de pagamento
description: Padrões de gap encontrados nos testes de FecharMesServiceImpl, CadastrarAdiantamento e CancelarAdiantamento (sprint 03)
type: project
---

Análise de 2026-06-04 sobre tasks BE-027/BE-028/BE-029 identificou padrões de gap que devem ser checados em toda análise futura do domínio de folha.

**Why:** O algoritmo de 11 passos do `FecharMesServiceImpl` tem invariantes críticas (atomicidade, idempotência, filtros temporais) que helpers de teste com valores hardcoded mascaram silenciosamente. Sem essa checklist mental, futuras análises de gap repetem o blind spot.

**How to apply:** Quando analisar testes de fechamento mensal, adiantamentos ou folha:

1. **Filtros temporais (`dataInicio <= primeiroDoMes`)**: verificar se helper do teste varia `dataInicio` — se for hardcoded para data passada, o filtro está sem cobertura efetiva. Bloqueante.
2. **Catch de `DataIntegrityViolationException`**: tradução para exceção de domínio em race conditions raramente é testada. Se houver `try { repo.save() } catch (DIVE)` no service, exigir teste explícito.
3. **`@Transactional` rollback**: nenhum teste unitário prova atomicidade (mocks não exercitam transação). Exigir pelo menos um integration test com falha injetada que valide reversão no banco real (usar `@SpyBean`).
4. **Filtros em memória sobre dados do repositório**: quando o serviço filtra resultado de query (ex.: `parcelasPagas < numParcelas`), verificar se há teste com dado "inconsistente" do banco (estado que não deveria existir mas o filtro protege).
5. **CHECK constraints do banco**: validação aplicacional + CHECK constraint formam uma dupla defesa. Boundary values (ex.: diferença exata = tolerância) merecem teste de integração cruzando os dois.
6. **Helpers com hardcoded values** (`adiantamento(...)` em `FecharMesServiceImplTest`): se um helper sempre injeta o mesmo valor em campo crítico para a lógica testada, esse caminho está sem cobertura.

Classes relevantes onde esses padrões aparecem:
- `application/services/FecharMesServiceImpl.java` (11 passos, @Transactional)
- `application/services/CadastrarAdiantamentoServiceImpl.java` (validação dupla aplicação + CHECK)
- `application/services/CancelarAdiantamentoServiceImpl.java` (idempotência implícita)
