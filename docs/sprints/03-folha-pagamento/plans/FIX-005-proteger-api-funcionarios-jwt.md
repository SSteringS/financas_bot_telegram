---
task: FIX-005
titulo: "Proteger /api/funcionarios/** com JwtAuthenticationFilter"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-04
branch_alvo: fix/005-proteger-api-funcionarios-jwt
prioridade: alta
esforco: baixo
territorio: back
estado: pronto-pra-execucao
depende_de: []
bloqueia: [QA-009]
skills_dispatched: [seguranca-backend, qualidade-de-testes]
integration_branch: null
fluxos_qa: []
---

# FIX-005 — Proteger `/api/funcionarios/**` com JwtAuthenticationFilter

## Intake

- **Origem:** identificado pelo humano na revisão da sprint 03 (2026-06-04). Registrado em `docs/PENDENCIAS-TECNICAS.md` — "gravíssimo", promovido a FIX imediato.
- **Por quê agora:** todos os endpoints de folha de pagamento (`/api/funcionarios/**`) estão acessíveis sem autenticação. Qualquer requisição HTTP sem cookie JWT retorna 200/201/204 normalmente. Em produção isso significa que qualquer pessoa com acesso à URL pode criar, listar e deletar dados de funcionários e folha.
- **Esforço:** baixo. A correção principal é uma linha em `shouldNotFilter()`. O custo real é atualizar os testes de integração que passavam a esmo sem auth.
- **Riscos resumidos:** zero risco funcional (não altera comportamento de endpoints já autenticados). Risco principal: `FecharMesIntegrationTest` e futuros testes da folha precisam adicionar auth — se esquecido, os testes ficam vermelhos e o CI bloqueia.

---

## Contexto — análise da causa raiz

`JwtAuthenticationFilter.shouldNotFilter()` usa condição negativa:

```java
// ATUAL — com bug
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        return true;
    }
    String path = request.getRequestURI();
    return !path.startsWith("/api/v1/")     // ← BUG: true para /api/funcionarios/**
            || path.equals("/api/v1/auth/exchange");
}
```

**Leitura do bug:** `!path.startsWith("/api/v1/")` é `true` para qualquer path que **não** começa com `/api/v1/`. Isso inclui `/api/funcionarios/**` — todos os endpoints da folha criados na sprint 03. Resultado: o filtro pula a validação JWT nesses paths.

**Paths afetados (todos desprotegidos hoje):**
- `POST   /api/funcionarios`
- `GET    /api/funcionarios`
- `GET    /api/funcionarios/{id}`
- `PUT    /api/funcionarios/{id}`
- `POST   /api/funcionarios/{id}/vales`
- `GET    /api/funcionarios/{id}/vales`
- `POST   /api/funcionarios/{id}/adiantamentos`
- `GET    /api/funcionarios/{id}/adiantamentos`
- `DELETE /api/funcionarios/adiantamentos/{adiantamentoId}`
- `POST   /api/funcionarios/{id}/fechamentos`
- `GET    /api/funcionarios/{id}/fechamentos`

---

## Decisão / abordagem

### Fix em `shouldNotFilter()`

Substituir a condição negativa por um **allowlist explícito de paths públicos**. Tudo que cair sob `/api/` — independente do sub-path — passa pelo filtro JWT.

```java
// NOVO — allowlist explícito
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        return true;
    }
    String path = request.getRequestURI();
    // Paths públicos explícitos — qualquer /api/** não listado aqui requer JWT
    return path.equals("/api/v1/auth/exchange")
            || path.startsWith("/webhook")
            || path.startsWith("/actuator")
            || !path.startsWith("/api/");
}
```

**Lógica da nova implementação:**
- `/api/v1/auth/exchange` — único endpoint público da API (troca token de convite por JWT)
- `/webhook/**` — recebe mensagens do Telegram (autenticado pelo token do bot, não por JWT)
- `/actuator/**` — health checks (sem auth intencional)
- `!path.startsWith("/api/")` — tudo que não é API (ex: `/favicon.ico`, `/`) passa livre
- **Tudo o mais sob `/api/`** — inclui `/api/v1/**` e `/api/funcionarios/**` — requer JWT

> **Verificar em `SecurityConfig`:** antes de commitar, o implementador deve confirmar que não há regra `permitAll()` para `/api/funcionarios/**` no `HttpSecurity`. Se houver, remover também.

### Atualizar `AbstractIntegrationTest` — helpers de POST e DELETE autenticados

`AbstractIntegrationTest` já tem `autenticarComo()` e `getAutenticado()`. Adicionar:

```java
protected <T> ResponseEntity<T> postAutenticado(
        String url, String cookie, String jsonBody, Class<T> responseType) {
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.add(org.springframework.http.HttpHeaders.COOKIE, cookie);
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    return restTemplate.exchange(
            url, org.springframework.http.HttpMethod.POST,
            new org.springframework.http.HttpEntity<>(jsonBody, headers),
            responseType);
}

protected ResponseEntity<Void> deleteAutenticado(String url, String cookie) {
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.add(org.springframework.http.HttpHeaders.COOKIE, cookie);
    return restTemplate.exchange(
            url, org.springframework.http.HttpMethod.DELETE,
            new org.springframework.http.HttpEntity<>(headers),
            Void.class);
}
```

### Atualizar `FecharMesIntegrationTest` — adicionar autenticação

`FecharMesIntegrationTest` chama `restTemplate.postForEntity()` e `restTemplate.getForEntity()` **sem** cookie. Após o fix, esses calls receberão 401. Atualizar o `@BeforeEach` e todos os calls:

```java
// Adicionar campo:
private String cookie;

// Em setUp(), após criar o funcionário:
cookie = autenticarComo(12345L); // qualquer requisitanteId — JWT só precisa ser válido

// Substituir chamadas:
// ANTES: restTemplate.postForEntity(url, requestBodyEntity(body), String.class)
// DEPOIS: postAutenticado(url, cookie, body, String.class)

// ANTES: restTemplate.getForEntity(url, String.class)
// DEPOIS: getAutenticado(url, cookie, String.class)
```

**Ocorrências a atualizar em `FecharMesIntegrationTest`** (linha → novo call):
- L72: `restTemplate.postForEntity(...)` → `postAutenticado(...)`
- L104/109: `restTemplate.postForEntity(...)` → `postAutenticado(...)`
- L134/139: `restTemplate.postForEntity(...)` (setup state) → `postAutenticado(...)`
- L143: `restTemplate.getForEntity(...)` → `getAutenticado(...)`
- L176: `restTemplate.postForEntity(...)` (B3) → `postAutenticado(...)`

O helper `requestBodyEntity()` privado do arquivo pode ser mantido ou removido — após a migração não é mais necessário (substituído por `postAutenticado`).

---

## Escopo / arquivos

### Modificar

| Arquivo | O quê |
|---|---|
| `financas_bot_telegram/src/main/java/.../infra/security/JwtAuthenticationFilter.java` | Substituir `shouldNotFilter()` pelo allowlist explícito |
| `financas_bot_telegram/src/test/java/.../integration/AbstractIntegrationTest.java` | Adicionar `postAutenticado()` e `deleteAutenticado()` |
| `financas_bot_telegram/src/test/java/.../integration/FecharMesIntegrationTest.java` | Adicionar `cookie` no `@BeforeEach`, migrar todos os calls para helpers autenticados |

### Verificar (não necessariamente modificar)

| Arquivo | O quê verificar |
|---|---|
| `SecurityConfig.java` (ou equivalente) | Ausência de `permitAll()` para `/api/funcionarios/**` — se existir, remover |

### Não tocar

- `frontend/` — zero mudança no front (a autenticação já acontecia; o front já envia o cookie JWT)
- Nenhuma lógica de negócio — apenas filtro e testes

---

## Testes

`testes_novos: 0` no código de produção. Em testes:
- `FecharMesIntegrationTest` é refatorado (não novo) — zero novos testes, apenas adiciona auth nos existentes.
- `AbstractIntegrationTest` recebe 2 métodos novos (helpers).

Validação: `mvn compile` + `mvn test` verdes após o fix.

---

## Critérios de aceitação

- [ ] `shouldNotFilter()` usa allowlist explícito — sem condição negativa com `!startsWith`.
- [ ] `curl -X POST http://localhost:8080/api/funcionarios/1/vales -H 'Content-Type: application/json' -d '{}'` sem cookie retorna **401**, não 400/200.
- [ ] `AbstractIntegrationTest` tem `postAutenticado()` e `deleteAutenticado()`.
- [ ] `FecharMesIntegrationTest` usa `postAutenticado()` / `getAutenticado()` — zero calls `restTemplate.postForEntity` sem auth.
- [ ] `mvn compile` verde.
- [ ] `mvn test` verde (sem regressões — todos os testes de integração passam com auth).
- [ ] Branch: `fix/005-proteger-api-funcionarios-jwt` saindo de `develop`.
- [ ] Commit: `fix(FIX-005): proteger /api/funcionarios/** com JwtAuthenticationFilter — allowlist explícito`.
- [ ] Status report em `docs/sprints/03-folha-pagamento/status/FIX-005-*.md`.

---

## Fora de escopo

- Testes 401/403 para `FolhaControllerTest` — esses vão em **QA-009** (que bloqueia em FIX-005). O plano de QA-009 será atualizado pelo planner após o merge desta task: a nota "não escrever testes 401/403" e o comentário de pendência são removidos; cenários 401/403 são adicionados à Sub-área A do QA-009.
- `ValeIntegrationTest`, `AdiantamentoIntegrationTest`, `FuncionarioCRUDIntegrationTest` — criados pelo QA-009, **já devem usar auth desde o início** (não precisam de retrofix).
- Mover endpoints para `/api/v1/funcionarios/**` — mudança de URL não justificada; o fix é no filtro.
- Autorização por papel (RBAC) — não existe hoje; não é escopo desta task. O JWT só autentica (quem é), não autoriza (o que pode fazer).

---

## Sequenciamento e coordenação

```
FIX-005 merge → develop
    ↓
develop → sync → integration/03-folha-pagamento  (planner faz o merge)
    ↓
QA-009 executa (já com auth nos integration tests)
```

**FIX-005 deve mergear ANTES de QA-009 iniciar.** Caso contrário, os integration tests de folha de QA-009 seriam escritos sem auth e precisariam de retrofix imediato.

- **Pode rodar em paralelo com:** FIX-004 (território disjunto — FIX-004 toca `domain/enums/`).
- **Bloqueia:** QA-009 — Sub-área A (`FolhaControllerTest`) deve incluir 401/403; Sub-área D (integration tests da folha) deve usar auth desde o início.
- **Após merge:** planner atualiza QA-009 removendo a nota "não escrever 401/403" e adicionando cenários de 401/403 à Sub-área A. Planner também sincroniza `develop → integration/03-folha-pagamento`.
- **Após merge:** marcar item "JWT" em `docs/PENDENCIAS-TECNICAS.md` como `~~resolvido~~`.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Import `HttpStatus.UNAUTHORIZED` / `403` esquecido num teste | Baixa | Baixo | `mvn test` vermelho deixa claro |
| SecurityConfig tem `permitAll()` não documentado que ainda libera os endpoints | Baixa | Alto | Critério: testar manualmente com `curl` sem cookie e confirmar 401 |
| `requisitanteId` passado para `autenticarComo()` exige existência em tabela | Baixa | Médio | Verificar se `GerarTokenConviteUseCase.gerar()` persiste algo; se sim, o `@AfterEach` deve limpar |

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, ciclo reviewer→QA conforme ADR 0019. PR `fix/005-... → develop` (FIX vai direto para develop, não passa pela integration branch).

---

## Referências

- `docs/PENDENCIAS-TECNICAS.md` §"/api/funcionarios/** sem autenticação JWT"
- `docs/sprints/03-folha-pagamento/avaliacoes/BE-026-029-sprint03-folha.md §8` — identificação da vulnerabilidade
- `docs/decisions/0019-workflow-reviewer-qa-loop.md` — protocolo reviewer→QA a seguir
- `docs/sprints/03-folha-pagamento/plans/QA-009-cobertura-testes-backend.md` — Sub-área A e D afetadas pelo fix
- `financas_bot_telegram/src/main/java/.../infra/security/JwtAuthenticationFilter.java` — arquivo principal
- `financas_bot_telegram/src/test/java/.../integration/AbstractIntegrationTest.java`
- `financas_bot_telegram/src/test/java/.../integration/FecharMesIntegrationTest.java`
