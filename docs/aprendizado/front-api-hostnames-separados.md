# Por que front e API em hostnames separados

## Contexto da dúvida

Discutido após o DEP-02, ao entender por que o front fica no apex `satyansaita.com` e a API num subdomínio `api.satyansaita.com` (DEP-03), em vez de tudo no mesmo host. É uma decisão de arquitetura de deploy com tradeoff real.

## Resumo destilado

São **dois workloads com necessidades opostas**; separar por hostname mantém cada um limpo.

### 1. Cache é o oposto pra cada um
- **Front (assets estáticos):** quer cache agressivo no edge, longo.
- **API (dados financeiros dinâmicos, por-usuário):** **nunca** pode cachear — sempre fresca.
- Juntos no mesmo host, seria preciso recortar por caminho (`/api/*` sem cache, resto com cache) — mistura responsabilidades e é fácil errar. Separados: `satyansaita.com` passa pelo CloudFront e cacheia; `api.satyansaita.com` vai **direto pra EC2**, sem CDN.

### 2. Origens/backends diferentes
- Origem do front = **bucket S3** (estático). Origem da API = **EC2** (Spring Boot, dinâmico).
- Cada um com seu pipeline de deploy (front: `s3 sync` + invalidação no DEP-04; back: deploy do JAR) e domínio de falha independente.

### 3. Cookie e CORS (cross-origin vs. same-site)
Front em `satyansaita.com`, API em `api.satyansaita.com`:
- **Origens diferentes** (host muda) → chamadas `fetch` são **cross-origin** → **CORS** necessário (backend libera a origem do front; DEP-05 ajusta `allowed-origin = https://satyansaita.com`).
- **Mesmo site** (eTLD+1 = `satyansaita.com`) → cookie `SameSite=Lax` **é enviado** (subdomínio do mesmo domínio é same-site) → login persiste.
- Resumo: separar por subdomínio custa CORS, mas **não** quebra o cookie. DEP-05 seta `Domain=.satyansaita.com` pra valer em todos os subdomínios.
- É o **mesmo padrão já testado localmente** (front `:5173`, API `:8080` — cross-origin, same-site). Em prod só mudam os nomes.

### Alternativa (não escolhida): single-origin com path routing
Botar a API atrás do **mesmo** CloudFront num behavior `/api/*` → segunda origem (EC2), cache off.
- **Vantagem:** single-origin → **sem CORS** e sem cookie cross-origin.
- **Custo (motivo de recusar):** CloudFront na frente da API (hop/latência), precisa desligar cache e repassar todos headers/métodos/cookies com cuidado, e acopla os dois workloads. Preferiu-se API direto na EC2 + front isolado, pagando CORS + cookie — que **já estavam construídos** (`WebMvcConfig`/CORS e `CookieFactory` desde a auth).

### Materialização
- **DEP-03:** `api.satyansaita.com` → EC2 atrás de proxy reverso HTTPS (não expor Spring na 8443). Cert wildcard `*.satyansaita.com` do DEP-01 já cobre.
- **DEP-05:** CORS `allowed-origin = https://satyansaita.com` + cookie `Domain=.satyansaita.com`.

## Pontos-chave

- Separar por hostname quando **cache, origem e ciclo de deploy** diferem — caso clássico "front estático cacheável" + "API dinâmica não-cacheável".
- Front (S3+CloudFront, cacheado) e API (EC2, direto, fresco) = políticas opostas, melhor isoladas.
- Subdomínio do mesmo domínio = **cross-origin (CORS)** mas **same-site (cookie Lax flui)**. As duas coisas são independentes.
- DEP-05 fecha prod: `allowed-origin` no CORS + `Domain=.subdomínio` no cookie.
- Alternativa single-origin (path `/api/*` no CloudFront) elimina CORS, mas acopla e mete CDN na frente da API — recusada.
- O cert wildcard do DEP-01 já cobre o `api.`.

## Pra aprofundar

- `cors-vs-samesite.md` e `cookies-samesite.md` (a base de cross-origin vs same-site).
- Proxy reverso na EC2 (Nginx/Caddy) e terminação TLS pra API (DEP-03).
- Quando single-origin compensa: apps internos simples, sem necessidade de CDN, onde evitar CORS vale o acoplamento.
- Headers de segurança (HSTS, CSP) por hostname.
