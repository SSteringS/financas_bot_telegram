---
task: FIX-005
titulo: "Padronizar /api/funcionarios/** → /api/v1/funcionarios/** + proteger com JWT"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-04
branch_alvo_back: fix/005-padronizar-api-v1
branch_alvo_front: fix/005-padronizar-api-v1-front
prioridade: alta
esforco: baixo
territorio: back+front
estado: pronto-pra-execucao
depende_de: []
bloqueia: [QA-009]
skills_dispatched: [seguranca-backend, qualidade-de-testes]
integration_branch: null
fluxos_qa: []
---

# FIX-005 — Padronizar `/api/funcionarios/**` → `/api/v1/funcionarios/**` + proteger com JWT

## Intake

- **Origem:** dois problemas identificados pelo humano na revisão da sprint 03 (2026-06-04), promovidos a FIX imediato por severidade.
- **Problema 1 — Inconsistência de versionamento de URL (arquitetural):** a API expõe endpoints sob dois prefixos diferentes: `/api/v1/**` (pedidos, auth, resumo) e `/api/funcionarios/**` (toda a folha de pagamento). Viola o princípio de padronização do projeto — qualquer dev ou consumidor da API precisa saber de dois padrões distintos. O reviewer não capturou isso durante a sprint 03 (ver §Contexto).
- **Problema 2 — Gap de autenticação JWT (segurança):** `JwtAuthenticationFilter.shouldNotFilter()` usa condição negativa que salta o filtro para qualquer path fora de `/api/v1/**`, incluindo `/api/funcionarios/**`. Todos os 11 endpoints de folha/funcionário são acessíveis sem autenticação.
- **Relação entre os problemas:** resolver Problema 1 (mover para `/api/v1/`) resolve Problema 2 implicitamente — os novos paths já seriam cobertos pelo filtro existente. Mesmo assim o filtro será refatorado (ver §Decisão/abordagem — defesa em profundidade).
- **Esforço:** baixo no back (renomear `@RequestMapping`, atualizar testes); baixo no front (substituir strings de URL nos serviços de API).

---

## Contexto

### Inconsistência de URL descoberta tarde

Os endpoints da folha de pagamento (BE-024..BE-029) foram criados com o prefixo `/api/funcionarios/**`. O padrão já existente no projeto é `/api/v1/**`. Essa inconsistência passou pelo reviewer automatizado da sprint 03 sem ser flagada.

**Por que o reviewer não capturou:** o reviewer analisa código de cada task em isolamento. A inconsistência de URL só é visível quando se compara o novo controller com os controllers pré-existentes (`PedidosController`, `AuthController`, `ResumoController`) — uma comparação cross-file que exige olhar o projeto como um todo, não só o diff. Esse é um **smell de consistência arquitetural** que deve ser adicionado ao checklist do reviewer (`docs/roles/reviewer.md`).

### Análise da causa raiz — JWT

`JwtAuthenticationFilter.shouldNotFilter()` usa condição negativa:

```java
// ATUAL — lógica negativa com gap:
protected boolean shouldNotFilter(HttpServletRequest request) {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        return true;
    }
    String path = request.getRequestURI();
    return !path.startsWith("/api/v1/")      // true para /api/funcionarios/** → filter skips
            || path.equals("/api/v1/auth/exchange");
}
```

**Endpoints atualmente desprotegidos (11 no total):**
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

### Fix primário — Padronizar URLs (back + front)

Mover todos os endpoints de folha/funcionário para o prefixo `/api/v1/`:

| De | Para |
|---|---|
| `/api/funcionarios/**` | `/api/v1/funcionarios/**` |

Isso resolve **ambos os problemas**: a inconsistência arquitetural e o gap de JWT (os novos paths passam automaticamente pelo filtro existente, que protege todo `/api/v1/**`).

**Back — FuncionarioController e FolhaController:**
```java
// DE:
@RequestMapping("/api/funcionarios")
// PARA:
@RequestMapping("/api/v1/funcionarios")
```

Confirmar que é uma única anotação `@RequestMapping` por controller (não path hardcoded nos métodos). Se houver paths hardcoded nos métodos individuais, atualizar também.

**Front — todos os serviços/hooks de API:**
Buscar e substituir todas as ocorrências de `/api/funcionarios` por `/api/v1/funcionarios` nos arquivos do front. Usar grep para mapear antes de editar:
```bash
grep -r "/api/funcionarios" frontend/src/ --include="*.ts" --include="*.tsx" -l
```
Atualizar todos os arquivos listados.

### Fix secundário — `shouldNotFilter()` com allowlist explícito (defesa em profundidade)

Mesmo após mover as URLs para `/api/v1/`, refatorar o filtro para usar allowlist explícito. Razão: qualquer endpoint futuro sob `/api/v2/`, `/api/internal/` ou outro prefixo sofreria o mesmo gap se o filtro continuar com lógica negativa.

```java
// NOVO — allowlist explícito:
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        return true;
    }
    String path = request.getRequestURI();
    // Allowlist de paths públicos — tudo sob /api/ não listado aqui requer JWT
    return path.equals("/api/v1/auth/exchange")
            || path.startsWith("/webhook")
            || path.startsWith("/actuator")
            || !path.startsWith("/api/");
}
```

### `AbstractIntegrationTest` — helpers de POST/DELETE autenticados

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

### `FecharMesIntegrationTest` — adicionar autenticação + atualizar URLs

Após o fix de URL e JWT, `FecharMesIntegrationTest` recebe 401 em todos os POSTs (que agora requerem JWT) e 404 se as URLs não forem atualizadas. Atualizar:

```java
// Adicionar campo:
private String cookie;

// Em @BeforeEach, após criar funcionário:
cookie = autenticarComo(12345L); // qualquer requisitanteId — JWT só precisa ser válido
```

Substituir todas as chamadas sem auth:
- `restTemplate.postForEntity(url, requestBodyEntity(body), String.class)` → `postAutenticado(url, cookie, body, String.class)`
- `restTemplate.getForEntity(url, String.class)` → `getAutenticado(url, cookie, String.class)`

**Ocorrências a atualizar (5 calls) + atualizar URLs de `/api/funcionarios/` para `/api/v1/funcionarios/`.**

---

## Escopo / arquivos

### Back — `fix/005-padronizar-api-v1` (sai de `develop`)

| Arquivo | O quê |
|---|---|
| `financas_bot_telegram/src/main/java/.../adapters/in/rest/folha/FolhaController.java` | `@RequestMapping` de `/api/funcionarios` → `/api/v1/funcionarios` |
| `financas_bot_telegram/src/main/java/.../adapters/in/rest/folha/FuncionarioController.java` | idem |
| `financas_bot_telegram/src/main/java/.../infra/security/JwtAuthenticationFilter.java` | Refatorar `shouldNotFilter()` para allowlist explícito |
| `financas_bot_telegram/src/test/java/.../integration/AbstractIntegrationTest.java` | Adicionar `postAutenticado()` e `deleteAutenticado()` |
| `financas_bot_telegram/src/test/java/.../integration/FecharMesIntegrationTest.java` | Adicionar auth (`autenticarComo`) + atualizar URLs |

> **Verificar também:** `SecurityConfig.java` — ausência de `permitAll()` para `/api/funcionarios/**`; se existir, remover.

> **Confirmar:** se outros controllers além de `FolhaController` e `FuncionarioController` usam o path `/api/funcionarios` (ex: métodos anotados diretamente), mapear com grep:
> ```bash
> grep -r "api/funcionarios" financas_bot_telegram/src/main/ --include="*.java"
> ```

### Front — `fix/005-padronizar-api-v1-front` (sai de `develop`, após back mergear)

| O quê | Como encontrar |
|---|---|
| Todas as chamadas `/api/funcionarios/**` → `/api/v1/funcionarios/**` | `grep -r "/api/funcionarios" frontend/src/ --include="*.ts" --include="*.tsx" -l` |

> O front **não** deve alterar nenhum arquivo do back. Território: apenas `frontend/`.

### Sequência de dispatch

```
back implementa fix/005-padronizar-api-v1
    ↓ back abre PR → develop, reviewer aprova, merge
front implementa fix/005-padronizar-api-v1-front (com URLs já corretas)
    ↓ front abre PR → develop, reviewer aprova, merge
planner sincroniza develop → integration/03-folha-pagamento
    ↓
QA-009 inicia (bloqueio levantado)
```

---

## Testes — o que muda

### Back
- `FecharMesIntegrationTest` — refatorado (auth + URLs). Nenhum teste novo.
- `AbstractIntegrationTest` — 2 métodos helpers novos (não são testes em si).
- Todos os outros integration tests existentes (`PedidosListIntegrationTest`, etc.) — **não são afetados** (usam `/api/v1/**`, já passavam pelo filtro).

### Front
- Nenhum teste de front é adicionado. A mudança é apenas nas chamadas de API (strings de URL).
- `mvn test` verde no back confirma que os testes de integração ainda passam com auth.

---

## Critérios de aceitação

**Back:**
- [ ] `grep -r "api/funcionarios" financas_bot_telegram/src/main/` retorna zero resultados.
- [ ] `curl -X GET http://localhost:8080/api/funcionarios` sem cookie retorna **404** (path não existe mais).
- [ ] `curl -X GET http://localhost:8080/api/v1/funcionarios` sem cookie retorna **401**.
- [ ] `curl -X GET http://localhost:8080/api/v1/funcionarios` com cookie válido retorna **200**.
- [ ] `AbstractIntegrationTest` tem `postAutenticado()` e `deleteAutenticado()`.
- [ ] `FecharMesIntegrationTest` usa auth e URLs `/api/v1/funcionarios/**`.
- [ ] `mvn compile` verde.
- [ ] `mvn test` verde (sem regressões).
- [ ] Branch: `fix/005-padronizar-api-v1` saindo de `develop`.
- [ ] Commit back: `fix(FIX-005): padronizar /api/funcionarios → /api/v1/funcionarios + shouldNotFilter allowlist`.
- [ ] Status report back em `docs/sprints/03-folha-pagamento/status/FIX-005-back-*.md`.

**Front:**
- [ ] `grep -r "/api/funcionarios" frontend/src/` retorna zero resultados.
- [ ] App carrega corretamente — tela de funcionários e folha funcionam com as novas URLs.
- [ ] `npm run build` verde.
- [ ] Branch: `fix/005-padronizar-api-v1-front` saindo de `develop`.
- [ ] Commit front: `fix(FIX-005): atualizar chamadas /api/funcionarios → /api/v1/funcionarios no front`.
- [ ] Status report front em `docs/sprints/03-folha-pagamento/status/FIX-005-front-*.md`.

---

## Fora de escopo

- Testes 401/403 para `FolhaControllerTest` — vão em **QA-009** após FIX-005 mergear.
- `ValeIntegrationTest`, `AdiantamentoIntegrationTest`, `FuncionarioCRUDIntegrationTest` — criados pelo QA-009 **já com `/api/v1/funcionarios/**` e com auth desde o início**.
- Mover outros endpoints para `/api/v2/` ou qualquer outra versão — fora de escopo.
- Autorização por papel (RBAC) — não existe hoje; não é escopo desta task.

---

## Gap do reviewer — ação pendente

O reviewer da sprint 03 não capturou a inconsistência de URL. Causa: o smell de "consistência de versionamento de API" não está no checklist do reviewer (`docs/roles/reviewer.md`). Ação para o dispatch de materialização do ADR 0019 (DISPATCH-ENG-IA):

> Adicionar à seção "Smells" de `docs/roles/reviewer.md`:
> **Consistência de prefixo de URL:** se o projeto usa `/api/vN/`, todo controller novo deve seguir o mesmo prefixo. Endpoints criados fora do padrão (ex: `/api/funcionarios/` quando o padrão é `/api/v1/`) são smell arquitetural a ser flagado como observação material.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Path hardcoded em método `@GetMapping`/`@PostMapping` (além do `@RequestMapping`) | Média | Médio | `grep -r "api/funcionarios" src/main/` lista tudo; critério exige zero resultados |
| Front chama um path que o grep não capturou (ex: concatenação dinâmica) | Baixa | Alto | Testar a tela de funcionários manualmente após o fix do front |
| `autenticarComo(12345L)` requer persistência do requisitante em banco | Baixa | Médio | Verificar `GerarTokenConviteUseCase.gerar()` — se persistir, o `@AfterEach` deve limpar |
| Outro arquivo de teste hardcoda `/api/funcionarios/` | Baixa | Baixo | `grep -r "api/funcionarios" src/test/` antes de commitar |

---

## Coordenação

- **Pode rodar em paralelo com:** nada — FIX-005 (back) deve mergear antes do front iniciar. Ambos devem mergear antes de QA-009.
- **Depende de:** nada (sai direto de `develop`).
- **Bloqueia:** QA-009 (Sub-área A precisa de auth + `/api/v1/`; Sub-área D idem).
- **Após merge de ambos (back + front):**
  - Planner atualiza QA-009: remover nota "não escrever 401/403" + adicionar cenários 401/403 à Sub-área A; Sub-área D usa `/api/v1/funcionarios/**` e `autenticarComo()`.
  - Planner sincroniza `develop → integration/03-folha-pagamento`.
  - Planner adiciona smell de URL ao DISPATCH-ENG-IA (materialização ADR 0019).
  - Marcar item "JWT" em `docs/PENDENCIAS-TECNICAS.md` como `~~resolvido~~`.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status reports válidos (um por território), ciclo reviewer→QA conforme ADR 0019. PRs `fix/005-*-back → develop` e `fix/005-*-front → develop` (FIXes vão direto para develop).

---

## Referências

- `docs/PENDENCIAS-TECNICAS.md` §"/api/funcionarios/** sem autenticação JWT"
- `docs/sprints/03-folha-pagamento/avaliacoes/BE-026-029-sprint03-folha.md §8` — identificação da vulnerabilidade
- `docs/decisions/0019-workflow-reviewer-qa-loop.md` — protocolo reviewer→QA a seguir
- `docs/sprints/03-folha-pagamento/plans/QA-009-cobertura-testes-backend.md` — bloqueado por FIX-005
- `docs/plans/DISPATCH-ENG-IA-implementar-adr-0019.md` — adicionar smell de URL à materialização
- `financas_bot_telegram/src/main/java/.../infra/security/JwtAuthenticationFilter.java`
- `financas_bot_telegram/src/test/java/.../integration/AbstractIntegrationTest.java`
- `financas_bot_telegram/src/test/java/.../integration/FecharMesIntegrationTest.java`
