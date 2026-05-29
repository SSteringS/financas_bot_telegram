# Status — EVO-07: Aceitar document/PDF no bot

**Branch:** `feature/backend-polish-evo07`
**Data:** 2026-05-13
**Status:** ✅ Concluída

---

## O que foi feito

### Novo enum `TipoArquivo`
- `domain/enums/TipoArquivo.java` com valores `IMAGEM` e `PDF`

### Nova exceção `TipoArquivoNaoSuportadoException`
- `adapters/in/telegram/exception/TipoArquivoNaoSuportadoException.java`
- Segue o mesmo padrão das outras exceções do adapter telegram (campo `chatId`, mensagem amigável)

### Migration V3
- `db/migration/V3__add_tipo_arquivo_comprovantes.sql`
- Adiciona coluna `tipo_arquivo ENUM('IMAGEM','PDF') NOT NULL DEFAULT 'IMAGEM'` à tabela `comprovantes`
- Backfill implícito via `DEFAULT 'IMAGEM'` — todos os registros históricos ficam como IMAGEM

### `Comprovante` (domain model)
- Adicionado campo `TipoArquivo tipoArquivo`

### `ComprovanteEntity`
- Adicionado `@Enumerated(EnumType.STRING) @Column(name = "tipo_arquivo", nullable = false) TipoArquivo tipoArquivo`

### `ComprovanteMapper`
- `toDomain`: mapeia `tipoArquivo` com null-safe default para `TipoArquivo.IMAGEM` (dados históricos sem coluna)
- `toEntity`: mapeia `tipoArquivo` com mesmo null-safe default

### `RegistrarComprovanteUsecase` (interface)
- Assinatura atualizada: `execute(Long pedidoId, String tipoPagamento, String fileIdTelegram, String imagemUrl, TipoArquivo tipoArquivo, Long chatId)`

### `RegistrarComprovanteServiceImpl`
- Aceita `TipoArquivo tipoArquivo` e popula o `Comprovante` na criação

### `S3ImageUploadService`
- Novo método `uploadFile(byte[] bytes, String extension)` — genérico, usado para imagem e PDF
- `uploadImage(byte[])` mantido como wrapper de compatibilidade: `return uploadFile(imageBytes, "jpg")`
- Chave S3 agora inclui a extensão correta (`.pdf`, `.png`, `.webp`, etc)

### `PaymentRequestStrategy`
- Método privado `extrair(Message, Long)` retornando `record ExtraidoArquivo(fileId, tipo, extensao)`
- Fluxo: tenta `message.getPhoto()` primeiro → fallback para `message.getDocument()`
- MIME types suportados: `image/*`, `application/pdf`, `application/octet-stream` (WhatsApp)
- MIME types não suportados: lança `TipoArquivoNaoSuportadoException` com mensagem amigável
- Sem foto nem document: lança `PhotoProcessingException`

### `PaymentProofStrategy`
- Mesmo padrão `extrair()` com `ExtraidoArquivo`
- `RegistrarComprovanteUsecase.execute()` agora passa `extraido.tipo()`

### `GlobalTelegramExceptionHandler`
- Handler para `TipoArquivoNaoSuportadoException`: retorna 200 + mensagem `❌` pro usuário

---

## Testes

| Arquivo | Novos testes |
|---|---|
| `PaymentRequestStrategyTest` | `deveAceitarDocumentPdfComoPedido`, `deveLancarTipoArquivoNaoSuportadoParaVideoEmPedido`, `deveLancarPhotoProcessingExceptionQuandoSemFotoEDocument` |
| `PaymentProofStrategyTest` | `deveAceitarDocumentComMimeTypeImageJpeg`, `deveAceitarDocumentComMimeTypePdf`, `deveAceitarDocumentOctetStreamComoImagem`, `deveLancarTipoArquivoNaoSuportadoParaVideo`, `deveLancarPhotoProcessingExceptionQuandoSemFotoEDocument` |
| `RegistrarComprovanteServiceImplTest` | Parâmetro `TipoArquivo.IMAGEM` adicionado em todos os 4 cenários existentes |
| `ComprovanteMapperTest` | `deveMappearTipoArquivoImagemRoundTrip`, `deveMappearTipoArquivoPdfRoundTrip`, `deveUsarDefaultImagemQuandoTipoArquivoNulo` |
| `GlobalTelegramExceptionHandlerTest` | `deveRetornarOkEEnviarMensagemParaTipoArquivoNaoSuportadoException` |
| `S3ImageUploadServiceTest` | novo arquivo — 6 testes cobrindo uploadFile (jpg, pdf), bucket correto, uploadImage delegate, S3UploadException, chaves únicas |

**Total:** 212 testes, BUILD SUCCESS

---

## Notas de deploy

- A migration V3 será aplicada pelo Flyway automaticamente no próximo deploy
- Nenhuma ação manual necessária para a coluna — DEFAULT 'IMAGEM' cobre todos os registros existentes
- Verificar logs do Flyway no deploy: `Flyway: Successfully applied 1 migration to schema`
