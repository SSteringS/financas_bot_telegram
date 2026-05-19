# Avaliação — backend-polish-evo07

**Branch:** `feature/backend-polish-evo07`
**Tarefas:** FIX-hide-requisitanteid-swagger, BE-15b, FIX-keystore-password-secret, FIX-revisar-msgs-erro-bot, EVO-07
**Período:** 2026-05-13 (overnight)
**Implementador:** Claude do back (claude-sonnet-4-6)
**Avaliado por:** (humano — validação manual + Claude de planejamento — análise de código)

---

## Roteiro de validação manual

Execute os testes abaixo com o ambiente dev rodando (`mvn spring-boot:run -Dspring-boot.run.profiles=dev`) e o bot de dev ativo no Telegram. Preencha a coluna **Resultado** com ✅ passou / ❌ falhou / ⚠️ parcial e anote observações onde relevante.

### Pré-condições

- [ ] App dev subiu sem erros
- [ ] Log de startup mostra migration V3 aplicada: `Successfully applied 1 migration` (ou `up to date` se já aplicada numa rodada anterior)

---

### Task 1 — FIX-hide-requisitanteid-swagger

| # | Ação | Esperado | Resultado | Observação |
|---|---|---|---|---|
| 1.1 | Abrir `http://localhost:8080/swagger-ui.html` e expandir qualquer endpoint de `PedidoController` (ex: `GET /pedidos`) | Sem campo `requisitanteId` nos parâmetros | | |
| 1.2 | Expandir endpoint de `ResumoController` (ex: `GET /resumo/mes`) | Sem campo `requisitanteId` nos parâmetros | | |

---

### Task 2 — BE-15b (@RestControllerAdvice restrito ao package telegram)

| # | Ação | Esperado | Resultado | Observação |
|---|---|---|---|---|
| 2.1 | `GET /pedidos` sem JWT (sem header Authorization) | Resposta JSON de erro, status 401 ou 403 — **não** uma resposta 200 vazia | | |

---

### Task 3 — FIX-keystore-password-secret

> Não testável em dev (a chave `${keystore_password}` só é lida via Secrets Manager no perfil prod). Validar no momento do deploy.

| # | Ação | Esperado | Resultado | Observação |
|---|---|---|---|---|
| 3.1 | **Antes do deploy:** adicionar chave `keystore_password = finbot123` no secret `finbot-prod-secrets` no AWS Secrets Manager | Chave adicionada | | |
| 3.2 | **Após o deploy:** `sudo journalctl -u finbot.service -n 50 \| grep -E "Started\|keystore\|ERROR"` | App subiu sem `Could not resolve placeholder 'keystore_password'` | | |

---

### Task 4 — FIX-revisar-msgs-erro-bot

| # | Ação | Esperado | Resultado | Observação |
|---|---|---|---|---|
| 4.1 | Enviar foto + legenda `200 pix Maria` | Resposta com `*Tipo:* PIX`, sem dica adicional | | |
| 4.2 | Enviar foto + legenda `50 Almoço` | Resposta com `*Tipo:* OUTRO` + dica em itálico sobre usar `boleto`/`pix`/`ted`/`agendamento` | | |
| 4.3 | Enviar foto + legenda inválida sem valor (ex: `Almoço`) | Mensagem de erro com exemplos: `100 boleto Energia`, `200 pix Maria`, etc. | | |
| 4.4 | Enviar foto + legenda de comprovante fora do formato (ex: `123 pix` sem `#`) | Mensagem de erro descritiva com exemplo correto (`#123 PIX`) | | |

---

### Task 5 — EVO-07 (aceitar document/PDF)

| # | Ação | Esperado | Resultado | Observação |
|---|---|---|---|---|
| 5.1 | Startup: verificar log da migration V3 | `Successfully applied 1 migration` ou `up to date` | | |
| 5.2 | Enviar **PDF como arquivo** com legenda `100 boleto Energia` | Bot confirma `✅ Pedido registrado!` com `*Tipo:* BOLETO` | | |
| 5.3 | Enviar **imagem como arquivo** (não foto) com legenda `80 ted João` | Bot confirma `✅ Pedido registrado!` com `*Tipo:* TED` | | |
| 5.4 | Enviar **PDF como arquivo** com legenda `#<ID_PENDENTE> pix` | Bot confirma comprovante registrado | | |
| 5.5 | Verificar no banco: `SELECT tipo_arquivo, imagem_url FROM comprovantes WHERE pedido_id = <ID>;` | `tipo_arquivo = 'PDF'`, URL termina em `.pdf` | | |
| 5.6 | Enviar **imagem como arquivo** com legenda `#<ID_PENDENTE> ted` | Bot confirma comprovante registrado | | |
| 5.7 | Verificar no banco: `SELECT tipo_arquivo, imagem_url FROM comprovantes WHERE pedido_id = <ID>;` | `tipo_arquivo = 'IMAGEM'`, URL termina em `.jpg` | | |
| 5.8 | Enviar **arquivo `.mp4` ou `.zip`** com legenda válida (ex: `200 pix Teste`) | Resposta `❌ Tipo de arquivo 'video/mp4' não suportado. Envie foto, imagem ou PDF.` | | |
| 5.9 | Enviar **foto normal** (não arquivo) com legenda `30 pix Ana` — smoke test de regressão | Bot continua funcionando normalmente | | |

---

## Cobertura por testes unitários (referência)

Os cenários abaixo foram validados pelos 212 testes automatizados e **não precisam de validação manual**:

- Parsing de legenda (valor com `.` e `,`, detecção de tipo pix/ted/boleto/agendamento/outro)
- Mapeamento `TipoArquivo` IMAGEM/PDF no mapper — round-trip e null-safe default para dados históricos
- Handler `TipoArquivoNaoSuportadoException` retorna 200 e envia mensagem ao usuário
- `S3ImageUploadService.uploadFile` gera chave S3 com extensão correta e URL única por chamada
- `RegistrarComprovanteServiceImpl` propaga `TipoArquivo` até o domain e rejeita pedido já pago

---

## Resultado consolidado (preencher após os testes)

| Tarefa | Testes passaram | Observações |
|---|---|---|
| Task 1 — hide-requisitanteid-swagger | | |
| Task 2 — BE-15b | | |
| Task 3 — keystore-password-secret | pendente deploy | |
| Task 4 — revisar-msgs-erro-bot | | |
| Task 5 — EVO-07 document/PDF | | |
| **Recomendação geral** | mergear / ajustar / bloquear | |

---

## Para o Claude de planejamento

Este documento serve de entrada para a avaliação da entrega `feature/backend-polish-evo07`. Os status reports individuais estão em `docs/status/` (prefixo `FIX-*`, `BE-15b-*`, `EVO-07-*`) e o resumo consolidado da sessão em `docs/status/_RESUMO-overnight-back-2.md`.
