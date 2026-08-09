---
name: Docker indisponível — 26 testes de integração falham localmente
description: TestContainers não encontra Docker no ambiente local; 26 IntegrationTests falham com ExceptionInInitializerError
type: project
---

26 testes de integração (`*IntegrationTest`) usam Testcontainers/MySQL e requerem Docker disponível.

**Atenção — usar sempre `mvn clean test`** (não só `mvn test`): sem o `clean`, artifacts stale em `target/` de migrations renomeadas causam `Found more than one migration with version N` e falham antes de o Docker ser necessário.

**How to apply:**
- Sempre rodar `mvn clean test` para a suite completa (não `mvn test`)
- Se Docker não estiver disponível: `mvn test -Dtest="!*IntegrationTest"` para 262 testes unitários
- `mvn clean test` com Docker: 288/288 verdes (confirmado em 2026-06-01)
- Sem Docker: 262 unitários verdes, 26 integração falham com `Could not find a valid Docker environment`
