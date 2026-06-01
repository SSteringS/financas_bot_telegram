---
name: Testes de integracao falham localmente por falta de Docker
description: 26 testes de integracao (*IntegrationTest) falham por Could not find a valid Docker environment — condicao pre-existente, nao regressao de tasks
type: project
---

Os 26 testes de integracao com Testcontainers falham no ambiente local de CI por `Could not find a valid Docker environment`. Isso e uma condicao pre-existente em `develop` — nao e regressao introduzida por nenhuma task.

**Why:** O ambiente local nao tem Docker disponivel para o Testcontainers. O CI (GitHub Actions) tem Docker e os testes passam la.

**How to apply:** Ao revisar qualquer task de backend, rodar `mvn test "-Dtest=!*IntegrationTest"` para os 262 testes unitarios. Os 26 de integracao falham sempre localmente — verificar apenas que o motivo da falha continua sendo `Could not find a valid Docker environment` e nao um erro novo relacionado a task. Se o numero de falhas mudar ou aparecer novo erro, investigar.
