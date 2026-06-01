---
name: Docker indisponível — 26 testes de integração falham localmente
description: TestContainers não encontra Docker no ambiente local; 26 IntegrationTests falham com ExceptionInInitializerError
type: project
---

26 testes de integração (`*IntegrationTest` — AuthFlowIntegrationTest, IsolamentoRequisitanteIntegrationTest, etc.) falham localmente com:

```
Caused by: java.lang.IllegalStateException: Could not find a valid Docker environment.
```

**Why:** TestContainers precisa de Docker para subir MySQL em container. O ambiente local de desenvolvimento não tem Docker rodando (ou acessível ao JVM).

**How to apply:** 
- `mvn test` local vai sempre mostrar 26 erros de integração — ignorar como pré-existente
- Para verificar que a V6 (ou qualquer migration) está correta, rodar `mvn test -Dtest="!*IntegrationTest"` para testar só os 262 testes unitários
- Os testes de integração passam no CI (GitHub Actions tem Docker disponível)
- Documentar sempre no status report como "26 erros de integração por Docker indisponível — pré-existente, não relacionado à task"
