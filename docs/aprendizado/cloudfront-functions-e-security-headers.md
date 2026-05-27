# Edge compute no CloudFront: Functions, Lambda@Edge e headers de segurança

## Contexto da dúvida

Discutido após o DEP-02, explorando o que dá pra fazer "no edge" do CloudFront: redirect `www`→apex e injeção de headers de segurança (HSTS/CSP). São enhancements opcionais, não escopo do DEP-02.

## Resumo destilado

### Duas ferramentas de edge compute

| | CloudFront Functions | Lambda@Edge |
|---|---|---|
| Runtime | JS restrito (ES5.1 + um pouco de ES6) | Node/Python (Lambda completa) |
| Onde roda | **Todas** as edge locations (600+) | Regional edge caches (menos pontos) |
| Latência | Sub-milissegundo | Milissegundos de 1 dígito+ |
| Rede/disco | **Não** | Sim |
| Tamanho | ~10KB | Lambda normal |
| Eventos | viewer request / viewer response | os 4 |
| Pra quê | header, URL rewrite, redirect, token simples | lógica pesada, chamadas externas |

**Regra de bolso:** mexer em header/URL/redirect → **CloudFront Functions**. Lambda@Edge só quando precisa de rede/lógica complexa.

### Os 4 ganchos no ciclo da requisição

1. **Viewer request** — chega no edge, antes do cache.
2. **Origin request** — só no cache miss, antes da origem.
3. **Origin response** — origem respondeu, antes de cachear.
4. **Viewer response** — antes de devolver ao navegador.

CloudFront Functions só pega **1 e 4**. Lambda@Edge pega os 4. Redirect (viewer request) e headers (viewer response) cabem em CloudFront Functions.

### Caso A: redirect `www` → apex
Precisa de: `www` como **alias** na distribuição + **registro Route 53** apontando `www` (cert wildcard do DEP-01 já cobre) + CloudFront Function no viewer request que, vendo Host `www`, devolve **301** pro apex. Acontece no edge, sem tocar no S3.

```js
function handler(event) {
  var req = event.request;
  if (req.headers.host.value === 'www.satyansaita.com') {
    return { statusCode: 301, statusDescription: 'Moved Permanently',
      headers: { location: { value: 'https://satyansaita.com' + req.uri } } };
  }
  return req;
}
```
Pra nós é *nice-to-have* (link vai como apex pelo WhatsApp) → opcional/evolução.

### Caso B: headers de segurança — preferir Response Headers Policy
Dá pra fazer com função no viewer response, **mas pra headers existe coisa melhor e sem código:** o **`aws_cloudfront_response_headers_policy`** (declarativo). Anexa via `response_headers_policy_id` no cache behavior. Função só quando precisar de **lógica condicional**.

```hcl
resource "aws_cloudfront_response_headers_policy" "seguranca" {
  name = "finbot-seguranca"
  security_headers_config {
    strict_transport_security {
      access_control_max_age_sec = 63072000
      include_subdomains = true
      preload = true
      override = true
    }
    content_type_options { override = true }                 # nosniff
    frame_options   { frame_option = "DENY", override = true }
    referrer_policy { referrer_policy = "strict-origin-when-cross-origin", override = true }
  }
}
```

O que cada um faz:
- **HSTS:** "só HTTPS por N segundos". `preload` = lista embutida dos browsers (commitment — só com HTTPS garantido em tudo).
- **nosniff:** navegador não "adivinha" content-type (vetor XSS).
- **frame DENY / frame-ancestors:** anti-clickjacking.
- **Referrer-Policy:** limita o que vaza no header Referer.
- **CSP (Content-Security-Policy):** mais forte contra XSS, mas **traiçoeiro em SPA** — calibra errado e quebra estilo inline/bundle do Vite. Tratar como **follow-up cuidadoso**, testando contra o build real. Ligar primeiro os ganhos fáceis (HSTS, nosniff, frame, referrer).

## Pontos-chave

- Edge compute = rodar código na edge location, no caminho da request, sem round-trip à origem.
- **CloudFront Functions** (leve, viewer request/response) cobre redirect e header. **Lambda@Edge** só pra músculo (rede/lógica).
- Pra **headers de segurança**, o **Response Headers Policy** (declarativo, sem código) é melhor que função.
- HSTS/nosniff/frame/referrer = ganhos fáceis. **CSP em SPA = cuidado**, é follow-up.
- `www`→apex: alias + Route 53 record + função 301 no viewer request; pra nós é opcional.
- Nada disso é escopo/bloqueio do DEP-02 — são enhancements pequenos.

## Pra aprofundar

- Response Headers Policy: também faz CORS e custom headers declarativos.
- Como montar uma CSP pra SPA (nonce/hash pra inline, `connect-src` pra API em `api.satyansaita.com`).
- Lambda@Edge: casos reais (auth no edge, A/B testing, image resize on the fly).
- Relação com `hospedagem-spa-s3-cloudfront.md` (mesma distribuição) e `front-api-hostnames-separados.md` (CSP `connect-src` precisa liberar o hostname da API).
