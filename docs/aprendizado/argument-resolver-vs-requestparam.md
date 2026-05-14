# Argument Resolver custom vs `@RequestParam` — por que `@RequisitanteId` "esconde" o valor

## Contexto da dúvida

Durante testes da API no Swagger UI, foi observado que mesmo digitando um `requisitanteId` diferente do autenticado no campo do Swagger UI, a request continuava retornando os mesmos dados (do requisitante real, autenticado pelo cookie). A pergunta foi: por que isso acontece, e isso é seguro?

## Resumo destilado

O comportamento está **correto e seguro**, mas confuso visualmente:

1. **O servidor IGNORA qualquer `requisitanteId` vindo da URL/body**. O valor que vale é exclusivamente o do JWT no cookie `finbot_session`, extraído pelo `JwtAuthenticationFilter` e injetado no controller pelo `RequisitanteIdArgumentResolver`.

2. **O Swagger UI mostra o campo como editável** porque o springdoc (gerador de OpenAPI) não conhece a annotation custom `@RequisitanteId`. Sem entender o significado, ele assume "é um parâmetro Long sem annotation Spring conhecida → vai como query param". Bug cosmético — o servidor não usa o valor de qualquer forma.

3. **Resultado prático:**
   - Cookie válido + sem requisitanteId na URL → retorna seus dados ✓
   - Cookie válido + `requisitanteId=999` na URL → retorna seus dados (ignorou 999)
   - Sem cookie + `requisitanteId=1` na URL → 401 (filter bloqueia antes do controller existir)

## Por que isso é proposital (segurança)

Se o `requisitanteId` viesse de query param, qualquer usuário autenticado conseguiria ler dados de qualquer outro só mudando a URL — vulnerabilidade clássica chamada **IDOR** (Insecure Direct Object Reference).

Nosso desenho elimina a brecha por construção: **o cookie é a única fonte da verdade pra "quem é o usuário"**. Tudo o resto é cosmético/ignorado.

## Como o Spring resolve isso por dentro

Ordem de execução dos argument resolvers em uma request:

1. `RequestParamMethodArgumentResolver(useDefault=false)` — só pega params com `@RequestParam` explícito
2. Outros resolvers built-in (path variable, request body, etc)
3. **Custom resolvers** (incluindo `RequisitanteIdArgumentResolver`)
4. `RequestParamMethodArgumentResolver(useDefault=true)` — catch-all pra simple types sem annotation

Pra um parâmetro `@RequisitanteId Long requisitanteId`:
- Passo 1: não claims (sem `@RequestParam`)
- Passo 3: `RequisitanteIdArgumentResolver.supportsParameter` retorna `true` (vê a annotation + tipo Long)
- Spring usa o resolver custom — pega valor de `request.getAttribute("requisitanteId")` setado pelo filter
- Passo 4: não roda porque o passo 3 já claimou

Funcionou. O valor da query string nunca foi consultado.

## Como esconder do Swagger UI

Pra evitar a confusão visual, dois jeitos:

### Opção A — Anotação na declaração da annotation custom

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@io.swagger.v3.oas.annotations.Parameter(hidden = true)
public @interface RequisitanteId { }
```

Pode funcionar mas não é 100% garantido em todas versões do springdoc.

### Opção B (recomendada) — Configuração global no `OpenApiConfig`

```java
@Configuration
public class OpenApiConfig {
    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(RequisitanteId.class);
    }
    // ... resto do bean OpenAPI
}
```

Springdoc pula qualquer parâmetro com `@RequisitanteId` na geração do OpenAPI. Cleaner, centralizado.

## Pontos-chave

- **Argument resolver custom > RequestParam default** quando o parâmetro tem annotation custom
- **Springdoc só conhece annotations padrão** (`@RequestParam`, `@RequestBody`, `@RequestHeader`, `@PathVariable`) — pra qualquer custom, precisa configurar explicitamente
- **IDOR vulnerability** = expor identificador de recurso/owner como query/path param sem validar contra a auth. Nosso desenho evita por design.
- **"Silently ignore" vs "explicitly reject"** quando cliente manda dado desnecessário — escolha de design. Ignorar é mais simples, oferece menos informação a atacante; rejeitar dá feedback claro mas adiciona código de validação. Em auth, ignorar tende a ser preferível.
- **Testes negativos importam**: o teste de listagem deveria cobrir o cenário "passar requisitanteId diferente na URL com cookie de outro" pra confirmar que o servidor ignora. Mesmo que pareça paranoia, garante a invariante.

## Pra aprofundar

- OWASP IDOR — Insecure Direct Object Reference. Comum em APIs REST. Mitigação padrão = "auth determina escopo, não input do cliente".
- Spring `HandlerMethodArgumentResolverComposite` — como Spring resolve múltiplos resolvers em ordem
- Springdoc `SpringDocUtils.addAnnotationsToIgnore` — config pra annotations custom em geral, não só essa
- "Trust boundaries" — conceito de onde dados confiáveis (do JWT) começam e onde dados não-confiáveis (input do cliente) terminam. Endpoint deve "esquecer" do input não-confiável o mais cedo possível.
