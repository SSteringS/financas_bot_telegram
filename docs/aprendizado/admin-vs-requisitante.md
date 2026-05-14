# Admin vs Requisitante — dois papéis distintos no sistema de auth

## Contexto da dúvida

Durante o setup da camada 4 de testes, a pergunta foi: "preciso me cadastrar como requisitante no banco antes de chamar a API admin?". A confusão é natural porque os dois papéis usam o mesmo sistema mas com mecanismos completamente diferentes.

## Resumo destilado

São **papéis diferentes** no sistema, com mecanismos de autenticação independentes:

| Papel | Quem é | Como autentica | Precisa estar no banco? |
|---|---|---|---|
| **Admin** (operador) | Quem opera o sistema (você) | Header `X-Admin-Key` com a chave estática armazenada em config | Não — autenticação é por posse da chave |
| **Requisitante** | Usuário final do site (o pai, eventualmente outros familiares) | Cookie `finbot_session` (JWT) após exchange de link mágico | Sim — linha em `requisitante` com id referenciado por `pedidos_pagamento.requisitante_id` |

### Fluxo de uso

O admin **age sobre** o requisitante, não é um requisitante:

```
Admin (você) → POST /admin/api/v1/requisitantes/1/convite
                 ↓
              gera link mágico FOR requisitante 1
                 ↓
Requisitante 1 (pai) → recebe link → POST /api/v1/auth/exchange
                       ↓
                       ganha cookie de sessão
                       ↓
                       usa /api/v1/pedidos com cookie
```

O `{id}` na URL admin **não é o admin**, é o destinatário do convite. O admin é "invisível" pra esse fluxo — ele só prova quem é via `X-Admin-Key`.

## Pontos-chave

- **Admin não tem `id` no banco.** É autenticado por posse de chave estática, não por identidade armazenada.
- **Toda chamada admin tem dois "sujeitos":** o admin (quem chama, via header) + o requisitante (sobre quem age, via path `{id}`).
- **A V2 da migration já criou requisitante id=1** com o nome do dono do projeto. Em dev, isso facilita teste: você é admin + age sobre requisitante 1 + você mesmo abre o link gerado (acting as o requisitante 1).
- **Em prod, papéis se separam fisicamente:** você como admin gera links pro seu pai, que é requisitante distinto. Você não precisa ser requisitante pra operar — pode até nunca abrir um link mágico se quiser.
- **Adicionar novos requisitantes** é INSERT direto no banco (SQL). Não há endpoint admin pra criar requisitante (apenas pra gerar convite). Quando virar comum, pode entrar como feature pós-MVP.
- **Multi-requisitante já está preparado desde a BE-01.** Todos os endpoints filtram dados por `requisitante_id` extraído do JWT, isolando dados entre requisitantes.

## Pra aprofundar

- Conceito de "roles" em controle de acesso — distinção entre auth de máquinas/operadores (API keys) e auth de usuários (sessões)
- Como sistemas reais separam "admin console" de "user app" — geralmente APIs diferentes, eventualmente domínios diferentes
- Padrão RBAC (Role-Based Access Control) vs ABAC (Attribute-Based) — formas mais estruturadas de modelar permissões quando o sistema cresce
- "Service account" vs "User account" em ecossistemas cloud (AWS, GCP)
