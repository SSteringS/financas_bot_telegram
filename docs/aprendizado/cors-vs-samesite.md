# CORS vs SameSite — mecanismos diferentes, problemas diferentes

## Contexto da dúvida

Depois de configurar `SameSite=Lax` no cookie (BE-11), a BE-13 também configurou CORS. A pergunta foi: por que precisa configurar CORS se já tem SameSite? Os dois não fazem a mesma coisa?

## Resumo destilado

São mecanismos **diferentes do navegador** que operam em **camadas diferentes** da request:

| Mecanismo | Controla | Protege contra | Granularidade |
|---|---|---|---|
| `SameSite` (cookie) | Se o navegador **anexa o cookie** numa request | CSRF (atacante usando cookie da vítima) | "Site" (registrable domain) |
| CORS (headers no response) | Se o JavaScript **pode ler a resposta** de uma request cross-origin | Leitura ilegal cross-origin (atacante lendo dados de outra origem) | "Origin" (scheme + host + port) |

**SameSite** age no "envio". **CORS** age na "leitura". São fases diferentes do ciclo da request.

### O fluxo completo, passo a passo

Cenário: front `localhost:5173` fetch pra API `localhost:8080`.

1. **Navegador decide se manda o cookie**
   - Verifica `SameSite` do cookie
   - `Lax` + mesmo site → cookie é anexado ✓
2. **Navegador eventualmente faz preflight OPTIONS**
   - Pra requests não-simples (POST JSON, headers custom, etc), browser pergunta ao servidor "posso fazer essa request do origin X?"
3. **Servidor processa a request real**
   - Cookie chegou, requisitante é identificado, lógica executa
4. **Navegador checa CORS no response**
   - `Access-Control-Allow-Origin: <origem do front>` está presente e bate?
   - `Access-Control-Allow-Credentials: true` está presente (porque o fetch usou `credentials: 'include'`)?
   - Se sim: navegador entrega response pro JS
   - Se não: navegador bloqueia o JS de ler, mesmo que o servidor tenha respondido OK

### Por que precisa dos DOIS

**Só SameSite (sem CORS):**
- Cookie é anexado ✓
- Request é feita ✓
- Servidor responde ✓
- Navegador bloqueia JS de ler ✗
- **Resultado:** seu próprio front não consegue consumir sua API. App quebrado.

**Só CORS (com SameSite=None mal configurado):**
- Site `evil-site.com` faz POST pra sua API com `credentials: 'include'`
- Cookie é anexado ✓ (SameSite=None permite)
- Servidor processa a request **(estrago feito se tem side effect)**
- Navegador bloqueia evil-site de ler response — mas a ação já foi executada
- **Resultado:** CSRF não mitigado

Você precisa de SameSite **pra impedir que cookies viagem pra origens não confiáveis** + CORS **pra autorizar seu próprio front a fazer requests cross-origin legítimas**.

## Pontos-chave

- **Site ≠ Origin.** Subdomínios do mesmo registrable domain são same-site mas DIFERENTES origins.
- **`localhost:5173` e `localhost:8080`** → mesma site, origins diferentes (porta diferente). Precisam de CORS pra falar.
- **`finbot.dom.br` e `api.finbot.dom.br`** → mesma site, origins diferentes. Precisam de CORS.
- **CORS exige config no servidor**. Não dá pra cliente "se autorizar".
- **`credentials: 'include'` no fetch + `Access-Control-Allow-Credentials: true` no servidor** = obrigatório pra cookie ser anexado e response ser lida.
- **`Access-Control-Allow-Origin` NUNCA é `*`** quando `allowCredentials=true` — tem que ser origin específico.
- **Mecanismos são complementares**, não substitutos.

## Pra aprofundar

- Same-Origin Policy — a regra geral do navegador que CORS releaxa controladamente
- Preflight OPTIONS — quando dispara e quando não, headers envolvidos
- `Access-Control-Expose-Headers` — pra response headers (`Set-Cookie` por exemplo) ficarem legíveis pro JS
- "Site Isolation" do Chrome — mecanismo arquitetural por baixo do conceito de "site"
