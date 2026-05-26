# Hospedagem de SPA estática: S3 + CloudFront + Route 53

## Contexto da dúvida

Surgiu durante a implementação do **DEP-02** (Fase 3c, deploy), enquanto o Claude do back criava a infra de hospedagem do frontend. A pergunta foi: explicar a fundo *tudo* o que o DEP-02 faz — não só o HCL, mas o porquê de cada peça. O padrão (SPA estática atrás de CDN) é genérico pra qualquer front moderno.

## Resumo destilado

### Por que S3 + CloudFront, e não só S3

S3 sozinho serve estático, mas só em **HTTP** e exigindo **bucket público**. Pra ter **HTTPS no domínio próprio**, cache rápido e bucket **privado**, põe-se um **CloudFront** (CDN da AWS) na frente. O CloudFront resolve TLS, cacheia perto do usuário e mantém o bucket fechado.

### Jornada de uma requisição a `https://satyansaita.com`

1. **DNS (Route 53):** registros **A (IPv4)** e **AAAA (IPv6)** do tipo *alias* apontam o apex pra distribuição. No **apex** não se pode usar **CNAME** (ele não convive com SOA/NS obrigatórios no mesmo nome). Como o CloudFront dá um *nome* e não IP fixo, a AWS resolve com **alias record** (A/AAAA proprietário que aponta pra recurso AWS, funciona no apex, é grátis).
2. **TLS no edge:** o navegador conecta na **edge location** mais próxima (`price_class = All` inclui América do Sul) e recebe o cert. `sni-only` (vários certs por IP, moderno/grátis) + `min TLS 1.2_2021`. O cert **tem que estar em us-east-1** — exigência fixa do CloudFront, independente de onde o resto roda.
3. **Cache ou origem:** edge checa o cache (`Managed-CachingOptimized`). Hit → serve na hora. Miss → busca na origem (S3).
4. **OAC (Origin Access Control):** com bucket privado (`public_access_block` nos 4 flags), o CloudFront acessa assinando com **SigV4** como service principal. A **bucket policy** libera `s3:GetObject` só pro principal `cloudfront.amazonaws.com` **e** com `AWS:SourceArn` = ARN *desta* distribuição. Só essa distribuição lê o bucket. (OAC sucede o antigo OAI.)
5. **Volta:** S3 devolve o objeto, edge cacheia, entrega ao navegador. `compress = true` → gzip/brotli.

### SPA fallback (a peça mais importante)

A SPA é um `index.html` + bundle JS; o roteamento é **client-side** (React Router reescreve a URL via history API, sem pedir ao servidor). Mas **abrir direto / dar refresh** em `/algum/caminho` faz o navegador pedir ao S3 a chave `algum/caminho`, que **não existe**. Com bucket privado via OAC, chave inexistente volta **403** (não 404 — não há permissão de listagem). Solução: `custom_error_response` mapeia **403 e 404 → `/index.html` com status 200**. O index sobe, o JS lê a URL e renderiza a rota. Mapear os dois códigos é necessário porque no setup privado o que ocorre de fato é o **403**.

### Cache vs. deploy

Assets do Vite são **content-hashed** (nome muda a cada build) → podem cachear "pra sempre". O `index.html` precisa atualizar → o pipeline (DEP-04) roda `aws cloudfront create-invalidation --paths '/*'` após o `s3 sync` pra forçar os edges a pegarem a versão nova.

## Pontos-chave

- **S3 só = HTTP + bucket público.** CloudFront na frente = HTTPS + bucket privado + cache.
- **Apex não aceita CNAME** → usar **alias A/AAAA** do Route 53 (extensão AWS, funciona no apex, grátis). AAAA porque a distribuição tem IPv6.
- **Cert do CloudFront SEMPRE em us-east-1** (exigência fixa). `sni-only` evita custo de IP dedicado.
- **OAC** = CloudFront assina (SigV4) pra ler bucket privado; bucket policy restringe por `AWS:SourceArn` da distribuição. Ninguém acessa o S3 direto.
- **SPA fallback:** 403 **e** 404 → `/index.html` 200. No bucket privado o erro real de rota inexistente é **403**, não 404.
- **Invalidação** (`/*`) no deploy é o que faz update aparecer; assets hasheados dispensam invalidação.
- **price_class All** inclui edges da América do Sul (latência pro Brasil); free tier cobre o custo (< US$2/mês total).

## Pra aprofundar

- OAC vs OAI (legado) e por que a AWS migrou.
- Cache policies gerenciadas vs. custom (TTL, cache key com query/cookies/headers).
- CloudFront Functions / Lambda@Edge (redirect `www`→apex, headers de segurança como HSTS/CSP no edge).
- Por que API em hostname separado (`api.satyansaita.com`, DEP-03): permite cachear o front agressivamente sem afetar a API.
- Relação com o `vite-plugin-pwa` (FE-10): o service worker cacheia o shell no cliente — uma segunda camada de cache além do CloudFront.
