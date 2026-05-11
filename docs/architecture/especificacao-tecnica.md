# Especificação Técnica — Camada de Visualização

Documento de referência pra implementação do site que o Pedro (pai) vai usar pra consultar pedidos e baixar comprovantes. Cobre evolução do modelo de dados, contratos da API, fluxo de autenticação, estrutura do projeto React, e plano de deploy. As decisões tomadas aqui são proposta — me corrige onde discordar.

## 1. Modelo de dados — evolução

### Estado atual (inferido do contexto)

Tabela única `pedido` (ou similar) que armazena tanto o pedido quanto o vínculo com o comprovante. Um único usuário Telegram. Sem conceito explícito de requisitante.

### Estado proposto

Três mudanças, todas baixo risco se aplicadas com a base ainda pequena:

1. **Introduzir `requisitante`** (uma tabela): mesmo que hoje só exista uma linha (Pedro). Sem isso, evolução pra multi-cliente requer migração retroativa.
2. **Separar `data_pedido` de `data_pagamento`**: hoje provavelmente só existe um timestamp de criação. Pro caso de uso "comprovante do mês X", precisa rastrear quando o pai *pediu* (input você) e quando você *pagou* (input do upload do comprovante).
3. **Adicionar `tipo`** (enum): boleto, pix, ted, agendamento, outro. Vira filtro útil e elimina ambiguidade. Opcional mas recomendado.

### DDL de migração

```sql
-- V2__add_requisitante_and_dates.sql

CREATE TABLE requisitante (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    telefone VARCHAR(20),
    email VARCHAR(120),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_requisitante_telefone (telefone)
) ENGINE=InnoDB;

-- Cria o registro inicial (seu pai)
INSERT INTO requisitante (nome, telefone) VALUES ('Pedro Marques', '+5511999990000');

-- Evolui a tabela de pedido
ALTER TABLE pedido
    ADD COLUMN requisitante_id BIGINT NOT NULL DEFAULT 1 AFTER id,
    ADD COLUMN data_pedido DATE NOT NULL DEFAULT (CURRENT_DATE) AFTER status,
    ADD COLUMN data_pagamento DATE NULL AFTER data_pedido,
    ADD COLUMN tipo ENUM('BOLETO','PIX','TED','AGENDAMENTO','OUTRO') NOT NULL DEFAULT 'OUTRO' AFTER tipo_existente,
    ADD CONSTRAINT fk_pedido_requisitante FOREIGN KEY (requisitante_id) REFERENCES requisitante(id),
    ADD INDEX idx_pedido_requisitante_data (requisitante_id, data_pedido DESC),
    ADD INDEX idx_pedido_status_data (status, data_pedido DESC);

-- Backfill data_pedido com criado_em existente (ajusta nome da coluna existente se diferente)
UPDATE pedido SET data_pedido = DATE(criado_em) WHERE data_pedido = CURRENT_DATE;

-- Backfill data_pagamento pros pedidos já pagos (ajusta nome da coluna que tem timestamp do comprovante)
UPDATE pedido SET data_pagamento = DATE(comprovante_criado_em) WHERE status = 'PAGO' AND comprovante_criado_em IS NOT NULL;

-- Remove default depois do backfill (opcional, mais correto)
ALTER TABLE pedido ALTER COLUMN data_pedido DROP DEFAULT;
```

> Observação: o exato nome de colunas (`criado_em`, `comprovante_criado_em`, `tipo_existente`) precisa ser ajustado pro schema que você tem hoje. Roda em ambiente dev primeiro com `EXPLAIN` nos índices novos pra confirmar que MySQL vai usar.

### Índices justificados

`idx_pedido_requisitante_data` cobre a query principal: "todos os pedidos do requisitante X ordenados por data desc". Com 5k linhas isso ainda é trivial sem índice, mas em 2 anos com 25k linhas, vira diferença visível.

`idx_pedido_status_data` cobre o filtro "pendentes" e "pagos no mês X".

## 2. API REST — contrato

Um endpoint REST single-purpose pro front. Versionado em `/api/v1/`.

### Endpoints

```
GET  /api/v1/pedidos
GET  /api/v1/pedidos/{id}
GET  /api/v1/pedidos/{id}/foto-pedido
GET  /api/v1/pedidos/{id}/comprovante
GET  /api/v1/resumo
POST /api/v1/auth/exchange
GET  /api/v1/auth/me
```

### `GET /api/v1/pedidos`

Lista paginada. Esse é o endpoint mais usado.

**Query params:**
- `status` — `pendente` | `pago` | `todos` (default: `todos`)
- `tipo` — `boleto` | `pix` | `ted` | `agendamento` | `outro` (opcional, repetível)
- `de` — data ISO `YYYY-MM-DD` (filtro `data_pedido >= de`)
- `ate` — data ISO `YYYY-MM-DD` (filtro `data_pedido <= ate`)
- `busca` — texto livre, busca em descrição (LIKE %busca%) e valor (se for número)
- `page` — int, default 0
- `tamanho` — int, default 20, max 50

**Response 200:**
```json
{
  "items": [
    {
      "id": 142,
      "valor": 287.50,
      "descricao": "Boleto Energia Elétrica",
      "tipo": "BOLETO",
      "status": "PAGO",
      "dataPedido": "2026-05-03",
      "dataPagamento": "2026-05-04",
      "temComprovante": true
    }
  ],
  "total": 142,
  "pagina": 0,
  "tamanho": 20,
  "totalPaginas": 8
}
```

### `GET /api/v1/pedidos/{id}`

Detalhe de um pedido. Retorna o mesmo objeto da listagem, sem `temComprovante` (sempre dá pra perguntar `/comprovante` direto).

### `GET /api/v1/pedidos/{id}/foto-pedido`

Retorna 302 redirect pra **pre-signed URL** do S3, válida por 10 minutos. Front faz `<img src="/api/v1/pedidos/142/foto-pedido">` e o navegador segue o redirect transparente.

```
HTTP/1.1 302 Found
Location: https://s3.amazonaws.com/finbot-images/pedidos/20260503/uuid.jpg?X-Amz-Signature=...
Cache-Control: private, max-age=600
```

### `GET /api/v1/pedidos/{id}/comprovante`

Mesmo padrão. 404 se ainda não foi pago. Sempre verifica que o `requisitante_id` do pedido bate com o requisitante autenticado.

### `GET /api/v1/resumo`

Pra cabeçalho do site (ex: "3 pendentes, R$ 7.230,00").

```json
{
  "mesAtual": "2026-05",
  "pendentes": { "quantidade": 3, "total": 7230.00 },
  "pagos": { "quantidade": 9, "total": 12480.72 }
}
```

### `POST /api/v1/auth/exchange`

Troca um token de uso único (vindo no link mágico) por uma sessão de longa duração.

**Request:**
```json
{ "token": "8f3a1c..." }
```

**Response 200:** seta cookie HTTP-only `finbot_session` com JWT de 180 dias. Retorna info do requisitante.
```json
{
  "requisitante": { "id": 1, "nome": "Pedro Marques" }
}
```

**Response 401** se token inválido/expirado.

### `GET /api/v1/auth/me`

Retorna o requisitante autenticado. Front chama no boot pra saber se a sessão tá viva.

### Ordenação e regras

- Listagem default: `dataPedido DESC, id DESC` (mais recentes primeiro). Adicionar parâmetro `ordem` se virar necessidade.
- Todas as queries são **automaticamente filtradas** por `requisitante_id` do JWT — front não consegue forçar ver dados de outro requisitante.
- Erros padronizados em formato `{ "erro": { "codigo": "PEDIDO_NAO_ENCONTRADO", "mensagem": "..." } }`.

## 3. Autenticação — link mágico via WhatsApp

### Fluxo

1. Você (operador, via comando no bot ou painel admin) gera um **token de convite** pro Pedro: `POST /admin/api/v1/requisitantes/1/convite` → retorna URL `https://finbot.exemplo.com/entrar?t=ABCD...`
2. Você cola essa URL no zap dele.
3. Ele clica. O front captura `t=`, chama `POST /api/v1/auth/exchange { token }`, que valida o token (uso único, expira em 7 dias), grava JWT em cookie HTTP-only com 180 dias.
4. Daí em diante ele só abre o site, o cookie tá lá, ele entra direto.
5. Quando o cookie expira (180 dias), front redireciona pra tela "peça um novo link pro filho" com instrução amigável.

### Token de convite

Gerado como UUID v4 ou 32 bytes random base64url. Armazenado hashed no banco (SHA-256), invalidado quando usado.

```sql
CREATE TABLE auth_token (
    token_hash CHAR(64) PRIMARY KEY,
    requisitante_id BIGINT NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_em TIMESTAMP NOT NULL,
    usado_em TIMESTAMP NULL,
    FOREIGN KEY (requisitante_id) REFERENCES requisitante(id)
);
```

### JWT da sessão

```
HS256, secret no Secrets Manager
Claims:
  sub: requisitante_id
  iat: issued at
  exp: 180 dias
```

Cookie:
```
Set-Cookie: finbot_session=<jwt>; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=15552000
```

### Por que cookie HTTP-only e não localStorage

Cookie HTTP-only não é acessível via JS, então XSS não vaza o token. SameSite=Lax cobre o básico de CSRF (em endpoints GET; pros poucos POST de auth, adicionar token CSRF ou usar SameSite=Strict).

### Renovação

A cada request bem-sucedido, se o JWT está com mais da metade da validade gasta, devolver um novo cookie. Pedro raramente vai sair do prazo se acessar uma vez por mês.

## 4. Estrutura do projeto React

### Stack

- **Vite** — build e dev server
- **React 18** — base
- **TypeScript** — type safety, ajuda com refactor
- **Tailwind CSS** — styling utilitário
- **React Router** — roteamento
- **TanStack Query** (React Query) — fetch, cache, invalidation
- **Zod** — validação de runtime das responses (defesa contra mudança no contrato)
- **date-fns** — formatação ptBR de datas

### Layout do projeto

```
finbot-frontend/
├── public/
│   └── icone-app.png
├── src/
│   ├── api/
│   │   ├── client.ts          (axios/fetch wrapper, injeta cookies)
│   │   ├── pedidos.ts         (queries + mutations)
│   │   ├── auth.ts
│   │   └── tipos.ts           (interfaces TS dos responses)
│   ├── components/
│   │   ├── ui/                (botão, input, badge, etc)
│   │   ├── PedidoCard.tsx
│   │   ├── FiltroData.tsx
│   │   ├── BarraBusca.tsx
│   │   └── ResumoMes.tsx
│   ├── paginas/
│   │   ├── Home.tsx           (lista principal)
│   │   ├── DetalhePedido.tsx
│   │   ├── Entrar.tsx         (consome token mágico)
│   │   └── Erro.tsx
│   ├── hooks/
│   │   ├── useAuth.ts
│   │   └── usePedidos.ts
│   ├── lib/
│   │   ├── formato.ts         (R$, datas)
│   │   └── ambiente.ts        (URL da API por env)
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css              (tailwind directives)
├── index.html
├── package.json
├── tailwind.config.js
├── tsconfig.json
└── vite.config.ts
```

### Decisões opinionadas

- **Single Page App, client-side routing**: simplicidade. Nenhuma necessidade de SSR.
- **TanStack Query gerencia todo estado de servidor**: nada de Redux, Context global, etc. Estado local fica em useState.
- **PWA habilitado** (vite-plugin-pwa): manifesto + service worker pra cache. Pedro adiciona à tela inicial e é "como um app".
- **i18n não necessário**: 1 idioma (ptBR), constantes hardcoded.
- **Sem bibliotecas de componentes pesadas** (MUI, Chakra): Tailwind + componentes próprios são mais leves e dão mais controle visual.

### Variáveis de ambiente

```
# .env.development
VITE_API_BASE_URL=http://localhost:8080

# .env.production
VITE_API_BASE_URL=https://api.finbot.seu-dominio.com.br
```

## 5. Deploy do front

### Arquitetura proposta

```
                                  ┌─────────────────┐
                                  │   Route 53      │
                                  │ finbot.dom.br   │
                                  └────────┬────────┘
                                           │
                                  ┌────────▼────────┐
                                  │   CloudFront    │
                                  │  (HTTPS, cache) │
                                  └────────┬────────┘
                                           │
                                  ┌────────▼────────┐
                                  │   S3 bucket     │
                                  │ (build estático)│
                                  └─────────────────┘
```

CloudFront com origin S3 privado (OAC — Origin Access Control). ACM pra cert HTTPS (us-east-1, requisito do CloudFront). Route 53 pro domínio.

API roda em outro hostname (`api.finbot.dom.br`) apontando pra EC2 — assim o CloudFront pode cachear o front agressivamente sem impactar a API.

### Custos estimados

- S3: ~$0.50/mês (poucos KB, baixo tráfego)
- CloudFront: free tier de 1TB/mês cobre folgado
- ACM: grátis
- Route 53: $0.50/mês por hosted zone

Total: < $2/mês de adicional.

### Esqueleto Terraform

```hcl
# infra/frontend.tf

resource "aws_s3_bucket" "frontend" {
  bucket = "finbot-frontend-prod"
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket                  = aws_s3_bucket.frontend.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_cloudfront_origin_access_control" "frontend" {
  name                              = "finbot-frontend-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "frontend" {
  enabled             = true
  is_ipv6_enabled     = true
  default_root_object = "index.html"
  aliases             = ["finbot.seu-dominio.com.br"]

  origin {
    domain_name              = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_id                = "s3-frontend"
    origin_access_control_id = aws_cloudfront_origin_access_control.frontend.id
  }

  default_cache_behavior {
    target_origin_id       = "s3-frontend"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    cache_policy_id        = data.aws_cloudfront_cache_policy.optimized.id
    compress               = true
  }

  # SPA fallback: 404 do S3 vira /index.html
  custom_error_response {
    error_code         = 403
    response_code      = 200
    response_page_path = "/index.html"
  }
  custom_error_response {
    error_code         = 404
    response_code      = 200
    response_page_path = "/index.html"
  }

  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate.frontend.arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  restrictions {
    geo_restriction { restriction_type = "none" }
  }
}

# Bucket policy pra permitir CloudFront
data "aws_iam_policy_document" "frontend_bucket" {
  statement {
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.frontend.arn}/*"]
    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.frontend.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "frontend" {
  bucket = aws_s3_bucket.frontend.id
  policy = data.aws_iam_policy_document.frontend_bucket.json
}
```

ACM cert tem que ser em `us-east-1` (provider aliased). Rota pro CNAME no Route 53 — esses pedaços vão na próxima iteração.

### Pipeline de deploy

Simples: GitHub Actions disparado em push pra `main` da branch do front:

```yaml
- npm ci
- npm run build
- aws s3 sync dist/ s3://finbot-frontend-prod --delete
- aws cloudfront create-invalidation --distribution-id $CF_ID --paths '/*'
```

OIDC entre GitHub e AWS pra evitar credenciais long-lived. Custo: zero.

## 6. CORS e endpoints autenticados

Como o front (`finbot.dom.br`) e a API (`api.finbot.dom.br`) estão em subdomínios diferentes do mesmo domínio raiz, com cookie de sessão em `.dom.br`, evita complicação de CORS com `credentials: 'include'`.

Spring config (proposta):

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter() {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of("https://finbot.seu-dominio.com.br"));
        cors.setAllowedMethods(List.of("GET", "POST"));
        cors.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return new CorsWebFilter(source);
    }
}
```

## 7. Performance / cargas estimadas

- 100 pedidos/semana × 52 = ~5.200/ano
- Em 3 anos: ~15.000 linhas
- Pedro acessa o site talvez 4-5 vezes por mês (estimativa)
- Operação dominante: query paginada em `pedido` filtrando por `requisitante_id` e ordenando por `data_pedido` — coberta pelo índice composto.

Conclusão: zero preocupação de performance pelos próximos 5 anos com `db.t3.micro`. O custo computacional do projeto inteiro continua dominado pelo Spring boot subindo, não pelo banco.

## 8. Roadmap proposto pra implementação

### Fase 3a — Backend (1-2 semanas)
1. Migração SQL aplicada em dev → prod
2. Endpoints REST: `GET /pedidos`, `GET /pedidos/{id}`, `GET /pedidos/{id}/foto-pedido`, `GET /pedidos/{id}/comprovante`
3. Service de geração de pre-signed URL pro S3 (10 min TTL)
4. Endpoint `GET /resumo`
5. Auth: tabela `auth_token`, geração via comando admin, `POST /auth/exchange`, JWT, middleware
6. Testes unitários dos services + um teste de integração com Testcontainers

### Fase 3b — Front (1-2 semanas)
1. Scaffold Vite + Tailwind + TanStack Query
2. Tela Entrar (consome `?t=`)
3. Tela Home com filtros, listagem, paginação
4. Componente PedidoCard com botão de comprovante
5. Vista detalhada (modal ou página)
6. PWA: manifest + service worker
7. Tratamento de sessão expirada

### Fase 3c — Deploy (3-4 dias)
1. Domínio + Route 53
2. Terraform pro S3+CloudFront+ACM
3. GitHub Actions pra deploy do front
4. CORS configurado na API
5. Teste end-to-end com link real mandado pro Pedro

## 9. Decisões em aberto pra confirmar

- **Domínio**: você tem um domínio comprado, ou precisa pegar? Sugiro algo curto e neutro tipo `pagto.com.br` ou similar.
- **Categoria/tipo**: confirma se quer adicionar agora (boleto/pix/ted/agendamento) ou se é overkill pra agora?
- **Comprovante: visualizar inline ou só baixar?**: sugestão é abrir num modal com a imagem (PIX é screenshot, boleto pode ser PDF) e ter botão "baixar". Para PDFs fica mais complexo — pdf.js ou só link de download? Recomendaria abrir em nova aba pro browser nativo cuidar de PDF, e mostrar inline pra imagens.
- **Delete/cancelamento de pedido**: o pai pode marcar "esse aqui não precisa mais"? Ou só você consegue (e ele vê)? Sugestão: só você, mantém ele só-leitura pra reduzir risco.
- **Notificação**: quando o pagamento é feito, manda zap automático com link do site, ou continua manual? Sugestão pra MVP: manual. Depois automatiza.

Quando você confirmar o domínio + variante de design + as 4 perguntas acima, dá pra escafolder o projeto inteiro numa próxima sessão.
