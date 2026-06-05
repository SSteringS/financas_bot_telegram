---
task: BE-030
titulo: "Tornar telegram.file.url configurável — pré-requisito WireMock E2E"
data: 2026-06-05
branch: feature/be-030-telegram-file-url-configuravel
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: na
  testes: ok
  testes_total: 356
  testes_novos: 2
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits: []
pr: null
desvios: 1
pendencias_humano: 0
---

# BE-030 — Tornar `telegram.api.file.url` configurável

## O que foi feito

`TelegramFileDownloaderService` tinha a URL de download do arquivo Telegram hardcoded:
```java
String downloadUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
```

Adicionado campo `telegramFileUrl` com injeção via `@Value("${telegram.api.file.url:https://api.telegram.org/file/bot}")`.
O default garante que produção continua funcionando sem alterar `application-prod.properties`.

A URL de download agora é:
```java
String downloadUrl = telegramFileUrl + botToken + "/" + filePath;
```

Criado `TelegramFileDownloaderServiceTest` com 2 testes unitários:
- `deveUsarFileUrlPadraoNaUrlDeDownload` — verifica que a URL padrão de prod é usada
- `deveUsarFileUrlCustomizadaParaWireMock` — verifica que URL injetada (ex: `http://localhost:8089/file/bot`) é usada sem nenhuma referência a `api.telegram.org`

---

## Desvios do plano

**1 desvio (nome da propriedade):** o plano especificava `telegram.file.url`, mas os arquivos de properties já continham `telegram.api.file.url` (adicionado pelo planner antecipando a task). Usado `telegram.api.file.url` por ser mais consistente com `telegram.api.url` existente e para não criar divergência entre código e properties já commitadas.

---

## Decisões tomadas durante a execução

**`@Value` com default inline:** `@Value("${telegram.api.file.url:https://api.telegram.org/file/bot}")` é o padrão Spring. Garante backward compatibility — se a propriedade estiver ausente (ex: em um ambiente de teste que não carrega properties), o default correto é usado automaticamente.

**Ordem dos parâmetros no construtor:** `telegramFileUrl` foi adicionado ao final para não quebrar a ordem dos parâmetros existentes, minimizando impacto em testes que constroem o service diretamente.

**`@SuppressWarnings("unchecked")` no teste:** necessário porque o mock de `RestClient.RequestHeadersUriSpec<?>` envolve generics raw. Padrão aceitável para testes com RestClient em Spring Boot 3.x.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- **QA-011 (Sub-área A)** desbloqueada: o WireMock pode agora interceptar o download injetando `TELEGRAM_API_FILE_URL=http://localhost:8089/file/bot` via env var (Spring Boot relaxed binding: `telegram.api.file.url` → `TELEGRAM_API_FILE_URL`).
- `application-dev.properties` (gitignored) deve ter `telegram.api.file.url=https://api.telegram.org/file/bot` para dev normal. O QA-011 sobrescreverá para o host do WireMock no perfil E2E.

---

## Padrões e decisões técnicas

**OCP em `TelegramFileDownloaderService`:** o service agora aceita qualquer base URL para download — extensível para novos ambientes (staging, WireMock, mock local) sem modificar a classe. Antes, qualquer mudança de URL exigia editar o código; agora é configuração.

**Arquitetura hexagonal:** a mudança está inteiramente no adapter de saída (`adapters/out/telegram/service/`). Nenhum port, use case ou entidade de domínio foi tocado. O domínio não sabe que o Telegram existe — a parametrização de URL é um detalhe de infraestrutura do adapter.

**Injeção via construtor (DI explícita):** todos os colaboradores do service são injetados via construtor — `RestClient`, `telegramApiUrl`, `botToken`, `telegramFileUrl`. Zero `@Autowired` em campo; testabilidade máxima (construção direta no teste sem Spring context).

---

## Arquivos criados/modificados

- `adapters/out/telegram/service/TelegramFileDownloaderService.java` (modificado: +campo `telegramFileUrl`, +parâmetro construtor, URL de download parametrizada)
- `adapters/out/telegram/service/TelegramFileDownloaderServiceTest.java` (novo: 2 testes unitários)
- `docs/sprints/03-folha-pagamento/status/BE-030-telegram-file-url-configuravel.md` (novo — este arquivo)
