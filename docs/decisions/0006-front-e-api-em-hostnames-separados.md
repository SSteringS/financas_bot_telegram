# ADR 0006 — Front no apex e API em subdomínio (hostnames separados)

**Data:** 2026-05-26 (registro retroativo de decisão tomada durante a Fase 3c)
**Status:** Accepted
**Decisores:** humano (com ajuda do Claude planejador)
**Relacionado:** materializado por DEP-02 (front) e DEP-03 (API); base conceitual em `docs/aprendizado/front-api-hostnames-separados.md`

---

## Contexto

A Fase 3c hospeda dois workloads: o **front** (SPA React estática) e a **API REST** (Spring Boot na EC2). Era preciso decidir como expô-los na web: tudo sob um único hostname (single-origin com roteamento por path, ex. `/api/*` atrás do mesmo CloudFront) ou em **hostnames separados** — front no apex `satyansaita.com`, API no subdomínio `api.satyansaita.com`.

A escolha cascateia em cache, CORS, comportamento de cookie e pipeline de deploy. Foi decidida na prática durante o DEP-02/DEP-03 e documentada em `aprendizado/`, mas nunca virou ADR — este registro a torna canônica.

---

## Decisão

**Front e API ficam em hostnames separados:** front no **apex `satyansaita.com`** (S3 + CloudFront), API no **subdomínio `api.satyansaita.com`** (EC2 atrás de proxy reverso HTTPS, sem CDN na frente).

---

## Razões

- **Cache é oposto pra cada workload.** O front (assets estáticos) quer cache agressivo no edge; a API (dados financeiros, por-usuário) **nunca** pode cachear. Hostnames separados deixam cada política limpa: o apex passa pelo CloudFront e cacheia; `api.` vai direto pra EC2, sempre fresco. Juntos no mesmo host exigiria recorte por path (`/api/*` sem cache) — mistura responsabilidades e é fácil errar.
- **Origens e ciclos de deploy diferentes.** Origem do front = bucket S3; origem da API = EC2. Cada um com seu pipeline (front: `s3 sync` + invalidação no DEP-04; back: deploy do JAR) e domínio de falha independente.
- **Custa CORS, mas não quebra o cookie.** Subdomínio do mesmo domínio é **cross-origin** (host muda → `fetch` precisa de CORS) mas **same-site** (eTLD+1 igual → cookie `SameSite=Lax` flui). São coisas independentes. O DEP-05 fecha prod com `allowed-origin = https://satyansaita.com` no CORS e `Domain=.satyansaita.com` no cookie.
- **Reaproveita o que já existe.** CORS (`WebMvcConfig`) e `CookieFactory` já foram construídos na fase de auth e já são exercitados localmente (front `:5173`, API `:8080` — mesmo padrão cross-origin/same-site). Em prod só mudam os nomes.
- **Cert já cobre.** O certificado wildcard `*.satyansaita.com` do DEP-01 já cobre o `api.`.

---

## Consequências

**Positivas:**
- Políticas de cache opostas ficam isoladas e corretas por construção.
- Deploys e domínios de falha independentes entre front e API.
- Padrão idêntico ao já testado em dev — menos surpresa em prod.

**Negativas:**
- **CORS é obrigatório** entre front e API (preflight, `allowCredentials`, manutenção do `allowed-origin`).
- Mais um registro DNS + terminação TLS própria pra API (proxy reverso na EC2, DEP-03) em vez de reusar o CloudFront.

---

## Alternativas consideradas

- **Single-origin com path routing** (API atrás do mesmo CloudFront, behavior `/api/*` → origem EC2, cache off): **descartada**. Elimina CORS e cookie cross-origin, mas mete o CloudFront na frente da API (hop/latência), exige desligar cache e repassar headers/métodos/cookies com cuidado, e **acopla** os dois workloads. O ganho (sem CORS) não compensa, ainda mais com CORS + cookie **já construídos**. Compensaria em apps internos simples, sem necessidade de CDN, onde evitar CORS vale o acoplamento — não é o caso.

---

## Referências

- `docs/aprendizado/front-api-hostnames-separados.md` (a explicação conceitual destilada)
- `docs/aprendizado/cors-vs-samesite.md`, `cookies-samesite.md` (cross-origin vs same-site)
- `docs/aprendizado/hospedagem-spa-s3-cloudfront.md` (o lado do front)
- `docs/plans/FASE-3-VISUALIZACAO.md` (DEP-02, DEP-03, DEP-05) e `docs/status/DEP-02.md`
</content>
