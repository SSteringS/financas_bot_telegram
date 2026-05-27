# Roteiro de Testes — Backend (antes de mergear `feature/*` em develop)

Guia em camadas, do mais rápido (automático) ao mais lento (manual). Cada camada cobre algo que a anterior **não cobre**. Antes de fazer um merge significativo em develop, vale subir pelo menos até a camada 4. Pra um merge mais "modesto" (1-2 tarefas pequenas), camada 1 + 2 já são suficientes.

A ideia geral segue a **pirâmide de testes**: muitos testes baratos e rápidos na base, poucos testes caros e completos no topo.

```
                    ┌─────────────────────────┐
                    │   E2E manual (camada 5) │   ← lento, mas insubstituível
                    ├─────────────────────────┤
                    │  Smoke API (camada 4)   │
                    ├─────────────────────────┤
                    │  Integração (camada 3)  │
                    ├─────────────────────────┤
                    │  Estática (camada 2)    │
                    ├─────────────────────────┤
                    │  Unitários (camada 1)   │   ← rápido, dezenas/centenas
                    └─────────────────────────┘
```

---

## Camada 1 — Testes unitários (`./mvnw test`)

**O que valida:** lógica isolada de cada classe. Mappers, parsers, services, handlers. Tudo mockado nas dependências externas (banco, S3, Telegram).

**Quem roda:** Claude do back, automaticamente, depois de cada tarefa. Você não precisa rodar de novo se o status report já mostrou tudo verde — mas é **trivialmente barato** rodar do seu lado pra confirmar.

```bash
cd C:\Users\satya\src\financas_bot_telegram\financas_bot_telegram
./mvnw test
```

**Esperado:** algo tipo `Tests run: 90, Failures: 0, Errors: 0, Skipped: 0`. Tempo < 30s.

**O que sugere falha:** se vier um `Failures > 0` ou `Errors > 0`, algum commit quebrou um teste antigo. Investigar o teste vermelho — geralmente é o último commit que mexeu na classe testada.

**Limitação:** unitários não pegam:
- Erros de configuração (Spring não consegue iniciar contexto)
- Queries JPA quebradas (mapeamento errado, query inválida)
- Problemas de integração entre componentes
- Problemas com migrations
- Erros de network/HTTP/serialização end-to-end

---

## Camada 2 — Verificação estática (humano + ferramentas, 5-10 min)

**O que valida:** que o código compila limpo, que a API expõe o que deveria, que o git histórico está aceitável.

### 2.1. Build limpo

```bash
./mvnw clean package
```

Se passa, significa que: testes verdes + JAR gerado + sem warnings de compilação. Se algum warning estranho aparecer (deprecação, unchecked cast), vale ler.

### 2.2. Revisão do git log

```bash
git log --oneline develop..HEAD
```

Lista de todos os commits que serão mergeados. Cada um deve ter mensagem clara no formato `feat(BE-XX): titulo`. Se algum commit estiver "WIP", "fix", ou outra coisa solta, considerar amendar antes de mergear.

### 2.3. Diff por arquivo (modo learning)

```bash
git diff develop..HEAD --stat   # resumo: arquivo + linhas +/-
git diff develop..HEAD <arquivo>  # diff completo de um arquivo
```

Pra cada arquivo novo importante (controller, service, mapper), passa o olho. Não precisa entender 100% — basta procurar coisas suspeitas:
- TODO/FIXME esquecidos
- `System.out.println` em vez de logger
- Senhas, tokens, IPs hardcoded
- Imports gigantes/desnecessários

### 2.4. Swagger UI exploration

Subir app local em dev e abrir `http://localhost:8080/swagger-ui.html`. Conferir:

- Todos os endpoints novos aparecem (lista `pedidos`, `auth`, `admin`, `resumo`)
- Cada endpoint tem descrição (não só "GET /api/v1/pedidos" sem mais nada)
- Schemas dos DTOs aparecem com descrição em cada campo
- "Try it out" abre formulário pra testar — é o que vai facilitar a camada 4

**O que sugere falha:** endpoint que existe no código mas não aparece no Swagger geralmente é problema de visibilidade (não é `@RestController`, está em pacote não escaneado, etc).

---

## Camada 3 — Testes de integração (`./mvnw test` com Testcontainers)

**O que valida:** queries JPA reais, migrations Flyway, fluxo HTTP completo contra banco MySQL de verdade. É o que a BE-14 entrega.

```bash
# Docker precisa estar rodando
docker ps                       # confirma daemon vivo
./mvnw test -Dtest=*IntegrationTest
```

Esperado: testes da pasta `src/test/java/.../integration/` rodam, Testcontainers sobe MySQL automaticamente (~30-60s primeira vez, ~5s reuso depois), todos verdes.

**O que sugere falha:**
- Queries JPA com erro de sintaxe (não pegou no unitário porque H2 dialect é diferente)
- Migration V2 ou V3 com bug
- Algum bean Spring não inicializa em ambiente "real"

**Limitação:** ainda não exercita S3, Telegram, ou interação real com o front. Esses são integration mocks no test.

---

## Camada 4 — Smoke test manual da API REST (45-60 min, pedagógico)

**O que valida:** que toda a stack HTTP funciona end-to-end no seu ambiente, com seu MySQL local, S3 dev, e cookies de verdade no navegador. **Esta é a camada onde você aprende mais sobre o sistema.**

### Setup

1. Subir app em dev:

```bash
cd financas_bot_telegram
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

2. Banco local rodando (MySQL conectado em `localhost:3306` com schema `financas_bot_telegram_db`)

3. Tools recomendadas (pelo menos uma):
   - **Swagger UI** em `http://localhost:8080/swagger-ui.html` — ótimo pra explorar e disparar requests
   - **HTTP Client do IntelliJ Ultimate** (`Tools → HTTP Client → Create Request in HTTP Client`) — salva requests como arquivos `.http` versionáveis
   - **Postman / Insomnia** — alternativa visual
   - **curl** no terminal — mais "raw" e pedagógico
   - **DBeaver / MySQL Workbench** — pra inspecionar banco em paralelo

Eu vou usar curl nos exemplos abaixo. Cole no terminal ou traduza pra outra ferramenta.

### Passo 4.1 — Confirmar que app subiu

```bash
curl http://localhost:8080/actuator/health
```

Esperado: `{"status":"UP"}` (se actuator estiver exposto). Se não tiver actuator, tenta:

```bash
curl -I http://localhost:8080/swagger-ui.html
```

Esperado: 200 ou 302.

### Passo 4.2 — Pegar a API admin key local

```bash
grep "app.admin.api-key" financas_bot_telegram/src/main/resources/application-dev.properties
```

Salva o valor numa variável de ambiente:

```bash
export ADMIN_KEY="<o-valor-de-dev>"
```

### Passo 4.3 — Gerar link mágico de convite

```bash
curl -X POST "http://localhost:8080/admin/api/v1/requisitantes/1/convite" \
     -H "X-Admin-Key: $ADMIN_KEY"
```

Esperado: `{"url":"http://localhost:5173/entrar?t=ABCdef123..."}`

**Pedagógico aqui — abre uma aba do DBeaver e roda:**

```sql
SELECT token_hash, requisitante_id, criado_em, expira_em, usado_em
FROM auth_token
ORDER BY criado_em DESC LIMIT 1;
```

Você vai ver o token HASHED (não o plain). Confere que `expira_em ≈ criado_em + 7 dias`. Esse é o ponto "armazenar hash, não plain" funcionando.

**Salva o token plain da URL:**

```bash
export TOKEN_CONVITE="ABCdef123..."  # cola o que veio depois de ?t=
```

### Passo 4.4 — Trocar o token por sessão (exchange)

```bash
curl -i -X POST "http://localhost:8080/api/v1/auth/exchange" \
     -H "Content-Type: application/json" \
     -c cookies.txt \
     -d "{\"token\":\"$TOKEN_CONVITE\"}"
```

O `-i` mostra os headers da resposta (importante pra ver o `Set-Cookie`). O `-c cookies.txt` salva os cookies num arquivo pro próximo curl reusar.

Esperado:
- Status `200 OK`
- Header `Set-Cookie: finbot_session=eyJhbGc...; Path=/; HttpOnly; SameSite=Lax; Max-Age=15552000`
- Body `{"requisitante":{"id":1,"nome":"Satyan Saita"}}`

**Pedagógico — cola o JWT em https://jwt.io e veja:**
- Header: `{"alg":"HS256","typ":"JWT"}`
- Payload: `{"sub":"1","iat":<timestamp>,"exp":<timestamp+180d>}`
- Signature: a parte que só o backend consegue gerar

Calcula: `(exp - iat) / 86400` deve dar ~180 dias.

**Pedagógico — confere no banco:**

```sql
SELECT token_hash, usado_em FROM auth_token
WHERE requisitante_id = 1 ORDER BY criado_em DESC LIMIT 1;
```

`usado_em` agora deve estar preenchido (não null). Tente fazer o exchange de novo com o mesmo token:

```bash
curl -i -X POST "http://localhost:8080/api/v1/auth/exchange" \
     -H "Content-Type: application/json" \
     -d "{\"token\":\"$TOKEN_CONVITE\"}"
```

Esperado: `401 Unauthorized` com `{"codigo":"TOKEN_INVALIDO", "mensagem":"token já foi usado"}`. **Esse é o single-use funcionando.**

### Passo 4.5 — Testar `/auth/me`

```bash
curl -i -b cookies.txt "http://localhost:8080/api/v1/auth/me"
```

`-b cookies.txt` faz o curl enviar o cookie salvo no passo anterior.

Esperado: `200 OK` + body `{"requisitante":{"id":1,"nome":"Satyan Saita"}}`.

**Pedagógico — testa sem cookie:**

```bash
curl -i "http://localhost:8080/api/v1/auth/me"
```

Esperado: `401 Unauthorized` com `{"codigo":"SESSAO_AUSENTE",...}`. **Esse é o filter JwtAuthenticationFilter funcionando.**

### Passo 4.6 — Listar pedidos (precisa ter dados no banco)

Antes, garante alguns dados:

```sql
INSERT INTO pedidos_pagamento
    (requisitante_id, telegram_user_id, telegram_message_id, valor, descricao,
     status, tipo, data_pedido, data_criacao)
VALUES
    (1, '999', '1', 150.00, 'Boleto energia teste', 'PAGO', 'BOLETO', '2026-05-15', NOW()),
    (1, '999', '2', 320.00, 'PIX maria teste',    'PAGO', 'PIX',    '2026-05-14', NOW()),
    (1, '999', '3', 1200.00,'TED construtora',    'PENDENTE', 'TED', '2026-05-13', NOW());
```

Agora:

```bash
curl -b cookies.txt "http://localhost:8080/api/v1/pedidos" | jq
```

Esperado: `PaginaDTO<PedidoResumoDTO>` com 3 items, ordenados por `dataPedido` DESC.

**Pedagógico — testa filtros:**

```bash
# só pendentes
curl -b cookies.txt "http://localhost:8080/api/v1/pedidos?status=PENDENTE" | jq

# só PIX
curl -b cookies.txt "http://localhost:8080/api/v1/pedidos?tipo=PIX" | jq

# intervalo de datas
curl -b cookies.txt "http://localhost:8080/api/v1/pedidos?de=2026-05-14&ate=2026-05-15" | jq

# busca textual
curl -b cookies.txt "http://localhost:8080/api/v1/pedidos?busca=construtora" | jq

# paginação
curl -b cookies.txt "http://localhost:8080/api/v1/pedidos?page=0&tamanho=2" | jq
```

Olha o `total`, `pagina`, `totalPaginas` em cada response. Faz sentido com a quantidade de dados que tem no banco.

### Passo 4.7 — Detalhe

```bash
curl -i -b cookies.txt "http://localhost:8080/api/v1/pedidos/1"
```

Esperado: 200 com PedidoDetalheDTO.

Tenta um id que não existe:

```bash
curl -i -b cookies.txt "http://localhost:8080/api/v1/pedidos/9999"
```

Esperado: 404 com `{"codigo":"PEDIDO_NAO_ENCONTRADO"}`.

### Passo 4.8 — Resumo do mês

```bash
curl -b cookies.txt "http://localhost:8080/api/v1/resumo" | jq
```

Esperado:
```json
{
  "mesAtual": "2026-05",
  "pendentes": {"quantidade": 1, "total": 1200.00},
  "pagos":     {"quantidade": 2, "total": 470.00}
}
```

### Passo 4.9 — Foto/comprovante (retorna 302 pra S3)

Pra esse passo, precisa ter um pedido com `imagem_url` apontando pra algum objeto válido no S3 dev. Se você tem dados reais do bot Telegram em dev, use um id deles. Senão, mocka:

```sql
UPDATE pedidos_pagamento
SET imagem_url = 'https://s3.amazonaws.com/bot-financas-pagamentos-dev/test/sample.jpg'
WHERE id = 1;
```

E:

```bash
curl -i -b cookies.txt "http://localhost:8080/api/v1/pedidos/1/foto-pedido"
```

Esperado: `302 Found` + header `Location: https://s3.amazonaws.com/.../sample.jpg?X-Amz-Signature=...&X-Amz-Expires=600`.

**Pedagógico — copia esse Location e cola no navegador.** Se o objeto existe no S3, o navegador baixa. Espera 10 min, tenta de novo — a URL retorna 403 do S3 (expirou). Esse é o TTL da pre-signed URL.

### Passo 4.10 — Teste de isolamento entre requisitantes

Crie um segundo requisitante manualmente:

```sql
INSERT INTO requisitante (id, nome, telefone, ativo, criado_em)
VALUES (2, 'Outro Requisitante', '+5511999999998', true, NOW());

INSERT INTO pedidos_pagamento
    (requisitante_id, telegram_user_id, valor, descricao, status, tipo, data_pedido, data_criacao)
VALUES
    (2, '888', 999.00, 'Pedido do req 2', 'PENDENTE', 'PIX', '2026-05-15', NOW());
```

Agora tenta acessar o pedido do requisitante 2 com o cookie do requisitante 1:

```bash
# primeiro descobre o id do pedido do req 2 via SQL
# digamos que ele é id=4

curl -i -b cookies.txt "http://localhost:8080/api/v1/pedidos/4"
```

Esperado: `403 Forbidden` com `{"codigo":"ACESSO_NEGADO"}`. **Esse é o filtro por requisitante funcionando.**

E listar deve mostrar só os do requisitante 1:

```bash
curl -b cookies.txt "http://localhost:8080/api/v1/pedidos" | jq '.items | length'
```

Esperado: continua sendo 3 (os do req 1), não 4. **Isolamento funcionando.**

### Limpeza após camada 4

Quando terminar, limpa os dados de teste:

```sql
DELETE FROM comprovantes WHERE pedido_id IN (SELECT id FROM pedidos_pagamento WHERE descricao LIKE '%teste%' OR descricao LIKE '%req 2%');
DELETE FROM pedidos_pagamento WHERE descricao LIKE '%teste%' OR descricao LIKE '%req 2%';
DELETE FROM requisitante WHERE id = 2;
DELETE FROM auth_token WHERE requisitante_id IN (1, 2);
```

(Adapta de acordo com o que você criou.)

---

## Camada 5 — Smoke test end-to-end via Telegram

**O que valida:** que o fluxo do bot continua funcionando depois de toda a evolução do back. Pega regressões que nenhuma camada anterior cobre.

### Setup

1. App em dev rodando (mesmo do camada 4)
2. ngrok exposto pra `localhost:8080`:
   ```bash
   ngrok http 8080
   ```
3. Webhook do Telegram (do bot **de dev**, não o de prod) apontando pro URL do ngrok:
   ```bash
   curl -X POST "https://api.telegram.org/bot${TOKEN_DEV}/setWebhook" \
        -d "url=https://<seu-ngrok>.ngrok.io/webhook"
   ```

### Cenários a exercitar

| # | Cenário | Esperado |
|---|---|---|
| 1 | Foto + legenda `100 boleto Energia` | Pedido criado com `tipo=BOLETO`, `data_pedido=hoje`, `requisitante_id=1` |
| 2 | Foto + legenda `200 pix Maria` | Pedido com `tipo=PIX` |
| 3 | Foto + legenda `150 Almoço` (sem palavra-chave) | Pedido com `tipo=OUTRO` (BE-03 do parser) |
| 4 | Foto + legenda `#<id_do_pedido_1> pix` | Comprovante registrado, status do pedido vira PAGO |
| 5 | Foto + legenda sem padrão (ex: `oi`) | Mensagem de erro amigável retornada pro chat, sem 500 |
| 6 | Mensagem sem foto (ex: só texto) | Mensagem de erro amigável; o bot **não trava** |
| 7 | Imagem como **documento** (compartilha do WhatsApp) com legenda `#<id> pix` | Se EVO-07 não estiver implementada: erro amigável pro usuário no chat, **sem travar a fila**. Esse é o BE-15 (handler genérico) funcionando. |

Pra cada cenário:
1. Mandar a mensagem
2. Conferir resposta do bot no chat
3. Conferir banco com SQL:
   ```sql
   SELECT id, valor, descricao, tipo, status, data_pedido, data_pagamento
   FROM pedidos_pagamento ORDER BY id DESC LIMIT 5;
   ```
4. Conferir logs no terminal onde o app está rodando — sem ERROR não esperado

### Pedagógico — entendendo os logs

Enquanto manda mensagens, o terminal vai mostrar o fluxo:

```
INFO --- TelegramWebhookController : Recebendo mensagem do Telegram: Update(...)
INFO --- UpdateOrchestratorService : Executando estratégia de adaptador: PaymentRequestStrategy
INFO --- PaymentRequestStrategy    : Estratégia de Pedido de Pagamento ativada
INFO --- TelegramFileDownloaderService : Iniciando download da imagem...
INFO --- S3ImageUploadService      : Imagem enviada com sucesso para S3
Hibernate: insert into pedidos_pagamento (...) values (?, ?, ?, ...)
INFO --- PaymentRequestStrategy    : Pedido X salvo com sucesso.
```

**Aproveita pra entender:** cada linha é um passo. O `Hibernate: insert` mostra o SQL real gerado. Confere que as colunas mencionadas batem com o que você espera.

---

## Camada 6 — Verificação pós-merge em prod (logo após mergear pra main)

Não roda agora antes de mergear pra develop, mas anota pra quando promover develop → main:

1. `getWebhookInfo` em prod → confirmar que o webhook continua apontando pro IP certo e o cert tá presente (`has_custom_certificate: true`)
2. Tail do log da EC2 enquanto manda mensagem de teste do seu Telegram pessoal:
   ```bash
   ssh ec2-user@<elastic-ip> 'sudo journalctl -u finbot -f'
   ```
3. Mandar 1 pedido + 1 comprovante reais e conferir no RDS via SQL
4. Acessar `https://3.228.138.109:8443/swagger-ui.html` — **deve retornar 404** (porque em prod está desabilitado). Se voltar com a página completa, a config de prod tá errada
5. Fazer um exchange com convite real (gerado via admin endpoint contra prod) e ver o cookie real chegar no celular do pai

---

## Checklist consolidado pré-merge

Antes de aprovar PR `feature/* → develop`:

- [ ] Camada 1 verde: `./mvnw test` passa
- [ ] Camada 2: build limpo, git log com mensagens decentes, Swagger UI mostra tudo certo
- [ ] Camada 3 verde (se BE-14 já entregue): `./mvnw test` com Testcontainers
- [ ] Camada 4: smoke test manual cobriu auth + listar + detalhe + resumo + imagem + isolamento entre requisitantes
- [ ] Camada 5: pelo menos um pedido + comprovante reais via Telegram funcionaram, bot não travou em mensagem mal formada
- [ ] Segredos pendentes (`admin_api_key`, `jwt_secret`) **registrados em algum lugar** pra serem adicionados ao Secrets Manager antes do deploy em prod
- [ ] Status reports de todas as tarefas estão em `docs/status/` com `Próximo passo` apontando pra próxima tarefa

Se tudo isso passa: pode mergear `develop` com segurança.

---

## Frequência sugerida das camadas

Não precisa rodar todas em todo merge. Sugiro:

| Tipo de mudança | Camadas necessárias |
|---|---|
| Bugfix de 1 arquivo | 1 + 2 |
| Refactor estrutural (mappers, ports) | 1 + 2 + 3 |
| Novo endpoint REST | 1 + 2 + 3 + 4 |
| Nova migration / mudança de schema | 1 + 2 + 3 + 4 (com inspeção SQL) |
| Mudança em strategy / parser do bot | 1 + 2 + 4 (smoke) + 5 (Telegram) |
| Pre-deploy em prod | tudo de 1 a 5 + plano da 6 |

---

## Coisas que esse roteiro NÃO cobre (consciente)

- **Testes de carga / performance.** Não relevante pro volume atual (1-2 usuários, ~100 pedidos/semana).
- **Testes de penetração / security audit.** Vale fazer eventualmente, mas é skill diferente. Hoje, basta a tríade Cookie HttpOnly + Secure + SameSite + uso de Secrets Manager.
- **Chaos engineering / failover.** EC2 cai? Banco indisponível? Não temos cenário pra testar; quando vier necessidade, planeja.
- **Testes de regressão visual no front.** Front-end vai ter seus próprios testes (Vitest + screenshot tests via Playwright eventualmente). Esse roteiro é só backend.
