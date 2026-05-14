# Aprendizado — biblioteca pessoal de conceitos

Pasta destinada a guardar resumos de conceitos técnicos que apareceram em discussões com o Claude de planejamento ao longo do projeto. Cada arquivo é auto-contido e serve pra:

- **Refresh** rápido de algo que você já entendeu mas pode esquecer
- **Base de estudo** se quiser ir mais a fundo no tópico
- **Referência cruzada** quando o tema aparece em outra conversa

Não é documentação de arquitetura (pra isso, `docs/architecture/`) nem plano de tarefa (pra isso, `docs/plans/`). É **conteúdo formativo**, organizado por tópico.

## Convenções

- **Um arquivo por tópico**, nome em kebab-case (ex: `cookies-samesite.md`)
- **Estrutura padrão** de cada arquivo:
  1. Contexto da dúvida — onde apareceu no projeto e qual era a pergunta original
  2. Resumo destilado — explicação curta e direta
  3. Pontos-chave — bullets pra revisão rápida
  4. Pra aprofundar (opcional) — tópicos relacionados ou conceitos pra estudar mais
- **Atualizar em vez de duplicar** — se o tópico volta a aparecer com nuance, edita o arquivo existente
- **Não inflar com perguntas operacionais** — só dúvidas conceituais/técnicas

## Índice por categoria

### Java / Spring fundamentos

- [`java-records.md`](java-records.md) — quando usar records vs classe com Lombok
- [`anotacoes-customizadas-java.md`](anotacoes-customizadas-java.md) — `@Target`, `@Retention`, `@interface`
- [`exception-handlers-scope.md`](exception-handlers-scope.md) — `@RestControllerAdvice` e `basePackages`
- [`jjwt-3-artefatos.md`](jjwt-3-artefatos.md) — por que a lib JWT tem 3 dependências
- [`argument-resolver-vs-requestparam.md`](argument-resolver-vs-requestparam.md) — por que `@RequisitanteId` ignora query param do mesmo nome

### React / TypeScript fundamentos

- [`fetch-wrapper-pattern.md`](fetch-wrapper-pattern.md) — padrão de cliente HTTP em React (FE-03)

### Segurança web

- [`cookies-samesite.md`](cookies-samesite.md) — atributo SameSite e proteção CSRF
- [`cors-vs-samesite.md`](cors-vs-samesite.md) — diferença entre CORS e SameSite
- [`jwt-secret-vs-api-key.md`](jwt-secret-vs-api-key.md) — por que ter dois secrets diferentes
- [`admin-vs-requisitante.md`](admin-vs-requisitante.md) — dois papéis distintos de auth no sistema

## Como adicionar

O Claude de planejamento cria/atualiza arquivos aqui automaticamente quando uma discussão técnica acontece. Se você quiser puxar um tópico específico pra registrar, basta pedir explicitamente: "registra o aprendizado de X em `docs/aprendizado/`". Se sentir que um arquivo está incompleto, peça pra eu adicionar mais detalhe ou exemplos.
