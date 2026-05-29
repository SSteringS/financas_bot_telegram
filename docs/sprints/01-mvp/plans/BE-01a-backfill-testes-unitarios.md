# BE-01a — Backfill de testes unitários + reativar pipeline

> **Esta tarefa precede a BE-01 da Fase 3.** Executar **depois de BE-00 (Flyway) e BE-00B (refatoração de persistência)** estarem mergeadas em develop. A BE-00B é importante porque cria os mappers e refatora os adapters, que entram no escopo de teste.

---

## Contexto

Hoje o pipeline de CI passa testes com `-DskipTests` por causa de uma decisão temporária registrada no `CLAUDE.md` da raiz: *"Testes pulados temporariamente — reativar com H2 antes da próxima feature."* Existe apenas um teste (`FinancasBotTelegramApplicationTests`) que é um smoke test mínimo de Spring boot.

A Fase 3 vai introduzir muito código sensível (endpoints REST de leitura, auth via JWT, pre-signed URLs, parsing de filtros). Sem uma base de testes unitários sólida cobrindo o que **já existe**, vamos misturar testes novos sem rede de proteção pro código antigo, e a primeira regressão silenciosa só vai aparecer em produção.

Esta tarefa **fecha essa lacuna antes da BE-01** começar:

1. Escreve testes unitários pras classes existentes com lógica não-trivial
2. Configura H2 pra suportar o teste de contexto Spring que já existe
3. Reativa o pipeline removendo `-DskipTests`
4. A partir daqui, **cada tarefa BE-* da Fase 3 inclui seus próprios testes** como critério de aceitação obrigatório

---

## Objetivo

Ao final desta tarefa:

1. As 11 classes listadas na seção "Classes em escopo" abaixo têm **arquivos de teste** com cobertura mínima de caminho feliz + erros principais
2. `FinancasBotTelegramApplicationTests` continua passando, agora com H2 em memória
3. Pipeline de CI roda os testes (sem `-DskipTests`) e o build verde só passa se todos forem verdes
4. `./mvnw test` localmente executa todos os testes em menos de 30 segundos (sem rede, sem banco real)
5. Não há nenhum teste do tipo "integração" nesta tarefa (sem Testcontainers, sem MySQL real) — só unitário com mocks

---

## Pré-requisitos

- **BE-00 (Flyway) merge em develop** — confirmar pelo `git log` ou status
- **BE-00B (Refatoração de persistência) merge em develop** — confirmar idem. As classes `PedidoPagamentoMapper`, `ComprovanteMapper`, `PedidoPagamentoEntity`, `ComprovanteEntity` precisam existir antes desta tarefa começar
- Antes de iniciar, ler `docs/architecture/estado-atual.md` pra contexto

---

## Stack de testes a usar

Já vem incluído no `pom.xml` via `spring-boot-starter-test`:

- **JUnit 5 (Jupiter)** — framework de teste
- **Mockito** — para mocks
- **AssertJ** — assertions fluentes (`assertThat(x).isEqualTo(y)`)
- **Spring Boot Test** — para o teste de contexto

A adicionar nesta tarefa:

- **H2 Database** (escopo `test`) — banco em memória pro teste de contexto Spring

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

**Não usar** Testcontainers, JaCoCo, PIT, PowerMock, ou qualquer outra ferramenta nova nesta tarefa. Mantém escopo enxuto.

---

## Classes em escopo (11 testes a escrever)

| # | Classe sob teste | Foco do teste | Arquivo de teste |
|---|---|---|---|
| 1 | `PaymentRequestStrategy` | regex de parsing, extração de valor/descrição, parseamento BigDecimal com vírgula/ponto, `supports()` true/false | `PaymentRequestStrategyTest.java` |
| 2 | `PaymentProofStrategy` | regex `#<id> <tipo>`, extração de pedidoId e tipoPagamento, `InvalidCaptionException` em formato inválido, `supports()` | `PaymentProofStrategyTest.java` |
| 3 | `UpdateOrchestratorService` | escolha de strategy baseada em `supports()`, fallback quando nenhuma suporta | `UpdateOrchestratorServiceTest.java` |
| 4 | `SalvarPedidoPagamentoServiceImpl` | delegação ao port, status PENDENTE atribuído, telegramUserId atribuído | `SalvarPedidoPagamentoServiceImplTest.java` |
| 5 | `RegistrarComprovanteServiceImpl` | busca pedido, lança `PedidoNaoEncontradoException` se não acha, salva comprovante, atualiza status do pedido para PAGO | `RegistrarComprovanteServiceImplTest.java` |
| 6 | `PedidoPagamentoMapper` | toDomain/toEntity round-trip preserva dados, null safety | `PedidoPagamentoMapperTest.java` |
| 7 | `ComprovanteMapper` | toDomain extrai pedidoId da relação, toEntity recebe pedidoEntity, null safety | `ComprovanteMapperTest.java` |
| 8 | `PedidoPagamentoRepositoryAdapter` | save chama mapper → jpa → mapper, findById mapeia Optional | `PedidoPagamentoRepositoryAdapterTest.java` |
| 9 | `ComprovanteRepositoryAdapter` | save busca pedido, lança exceção se pedido não existe, salva via jpa | `ComprovanteRepositoryAdapterTest.java` |
| 10 | `TelegramWebhookController` | retorna 200 pra usuário autorizado, lança `UnauthorizedUserException` pra não autorizado, lança `InvalidUpdateException` pra payload sem message/from, delega ao orchestrator | `TelegramWebhookControllerTest.java` |
| 11 | `GlobalTelegramExceptionHandler` | cada tipo de exceção produz resposta HTTP esperada e tenta enviar mensagem pro chat do Telegram | `GlobalTelegramExceptionHandlerTest.java` |

### Fora do escopo desta tarefa (não escrever testes pra)

- DTOs sem lógica (`PaymentMessageDTO`, `PaymentCategory`)
- Interfaces puras (ports, use cases sem implementação local)
- POJOs de domain sem métodos (`PedidoPagamento`, `Comprovante` após BE-00B — eles têm só getters/setters)
- JPA entities (`PedidoPagamentoEntity`, `ComprovanteEntity` — só estrutura)
- Enums (`StatusPedido`)
- I/O wrappers que dependem de SDK externo: `TelegramFileDownloaderService`, `S3ImageUploadService`, `TelegramMessageSenderService` (testar com mocks aqui dá pouco valor — viram integration tests numa fase futura)
- Exception classes (`InvalidCaptionException`, etc — são containers de erro, não têm comportamento)
- `AppConfig` (apenas wiring)
- `FinancasBotTelegramApplication` (só `main`)

---

## Estrutura de pastas dos testes

Espelhar a estrutura de `src/main/java/`:

```
src/test/java/br/com/satyan/stering/saita/financasbottelegram/
├── FinancasBotTelegramApplicationTests.java   (já existe — pode precisar ajuste)
├── adapters/
│   ├── in/telegram/
│   │   ├── controller/TelegramWebhookControllerTest.java
│   │   ├── exceptionhandler/GlobalTelegramExceptionHandlerTest.java
│   │   ├── service/UpdateOrchestratorServiceTest.java
│   │   └── strategy/
│   │       ├── PaymentRequestStrategyTest.java
│   │       └── PaymentProofStrategyTest.java
│   └── out/persistence/
│       ├── PedidoPagamentoRepositoryAdapterTest.java
│       ├── ComprovanteRepositoryAdapterTest.java
│       └── mapper/
│           ├── PedidoPagamentoMapperTest.java
│           └── ComprovanteMapperTest.java
└── application/services/
    ├── SalvarPedidoPagamentoServiceImplTest.java
    └── RegistrarComprovanteServiceImplTest.java
```

---

## Convenções de teste

**Nomenclatura de método:** `should<comportamento>_when<contexto>` em inglês ou `deve<comportamento>_quando<contexto>` em português. Escolher um e ser consistente. Recomendo português pra ficar alinhado com o domínio:

```java
@Test
void deveSalvarPedidoComStatusPendente() { ... }

@Test
void deveLancarExcecaoQuandoPedidoNaoEncontrado() { ... }
```

**Estrutura:** Given/When/Then com comentários ou arrange/act/assert.

**Mocks:** `@ExtendWith(MockitoExtension.class)` no nível da classe, `@Mock` nas dependências, `@InjectMocks` no SUT.

**Assertions:** AssertJ (`assertThat(...)`), não JUnit asserts.

---

## Exemplo completo de teste (referência)

`PaymentRequestStrategyTest.java`:

```java
package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.s3.service.S3ImageUploadService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramFileDownloaderService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.SalvarPedidoPagamentoUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@ExtendWith(MockitoExtension.class)
class PaymentRequestStrategyTest {

  @Mock private SalvarPedidoPagamentoUsecase salvarPedidoPagamentoUsecase;
  @Mock private TelegramMessageSenderService telegramMessageSenderService;
  @Mock private S3ImageUploadService s3ImageUploadService;
  @Mock private TelegramFileDownloaderService telegramFileDownloaderService;

  @InjectMocks private PaymentRequestStrategy strategy;

  // ---- supports() ----

  @Test
  void deveSuportarMensagemComLegendaDePedidoFormatoValido() {
    Update update = mockUpdateComLegenda("150.00 Almoço no restaurante");
    assertThat(strategy.supports(update)).isTrue();
  }

  @Test
  void deveSuportarMensagemComValorComVirgula() {
    Update update = mockUpdateComLegenda("150,00 Almoço");
    assertThat(strategy.supports(update)).isTrue();
  }

  @Test
  void naoDeveSuportarLegendaDeComprovante() {
    Update update = mockUpdateComLegenda("#123 PIX");
    assertThat(strategy.supports(update)).isFalse();
  }

  @Test
  void naoDeveSuportarLegendaSemValor() {
    Update update = mockUpdateComLegenda("Almoço");
    assertThat(strategy.supports(update)).isFalse();
  }

  // ---- process() ----

  @Test
  void deveProcessarPedidoEPersistirComStatusPendente() {
    Update update = mockUpdateCompleto("150.50 Almoço com cliente", 12345L, 99L, "file_abc");
    when(telegramFileDownloaderService.downloadImageByFileId("file_abc")).thenReturn(new byte[]{1,2,3});
    when(s3ImageUploadService.uploadImage(any())).thenReturn("https://s3.../foto.jpg");
    PedidoPagamento pedidoSalvo = PedidoPagamento.builder().id(1L).valor(new BigDecimal("150.50")).descricao("Almoço com cliente").build();
    when(salvarPedidoPagamentoUsecase.execute(any(), eq(12345L))).thenReturn(pedidoSalvo);

    strategy.process(update);

    ArgumentCaptor<PedidoPagamento> captor = ArgumentCaptor.forClass(PedidoPagamento.class);
    verify(salvarPedidoPagamentoUsecase).execute(captor.capture(), eq(12345L));
    PedidoPagamento capturado = captor.getValue();
    assertThat(capturado.getValor()).isEqualByComparingTo("150.50");
    assertThat(capturado.getDescricao()).isEqualTo("Almoço com cliente");
    assertThat(capturado.getStatus()).isEqualTo(StatusPedido.PENDENTE);
    assertThat(capturado.getTelegramUserId()).isEqualTo("99");
    assertThat(capturado.getFileIdTelegram()).isEqualTo("file_abc");
    assertThat(capturado.getImagemUrl()).isEqualTo("https://s3.../foto.jpg");

    verify(telegramMessageSenderService).sendMessage(eq(12345L), any());
  }

  // ---- helpers ----

  private Update mockUpdateComLegenda(String legenda) {
    Update update = new Update();
    Message message = new Message();
    message.setCaption(legenda);
    update.setMessage(message);
    return update;
  }

  private Update mockUpdateCompleto(String legenda, Long chatId, Long userId, String fileId) {
    Update update = new Update();
    Message message = new Message();
    message.setCaption(legenda);
    message.setChatId(chatId);

    User user = new User(userId, "Pedro", false);
    message.setFrom(user);

    PhotoSize photo = new PhotoSize();
    photo.setFileId(fileId);
    photo.setFileSize(1000);
    message.setPhoto(List.of(photo));

    update.setMessage(message);
    return update;
  }
}
```

Esse é o nível de detalhe esperado pra cada teste. Os outros 10 testes seguem o mesmo padrão: 3-8 métodos, mocks pras dependências, AssertJ pras assertions, helpers pra montar fixtures complexas (Telegram Update etc).

---

## Passos de execução

### Passo 1 — Adicionar H2 ao `pom.xml`

Dentro de `<dependencies>`, adicionar (escopo test):

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

### Passo 2 — Criar `application-test.properties`

`src/test/resources/application-test.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.flyway.enabled=false
telegram.allowed-user-ids=999
telegram.bot-token=test-token
telegram.api.url=http://localhost
telegram.api.file.url=http://localhost
aws.s3.bucket-name=test-bucket
aws.s3.region=us-east-1
financasbot.s3.base-url=http://localhost/
```

(Flyway desligado em teste — H2 com `ddl-auto=create-drop` recria schema do zero a cada execução, é mais rápido pra unit/context tests.)

### Passo 3 — Atualizar `FinancasBotTelegramApplicationTests`

Anotar pra usar profile test:

```java
@SpringBootTest
@ActiveProfiles("test")
class FinancasBotTelegramApplicationTests {
    @Test
    void contextLoads() { }
}
```

### Passo 4 — Escrever os 11 testes unitários

Seguir o exemplo da `PaymentRequestStrategyTest` como referência. Para cada classe da tabela "Classes em escopo":

- Criar arquivo no path correspondente em `src/test/java/...`
- Cobrir caminho feliz + 2-4 cenários de erro/borda
- Usar `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`
- Assertions com AssertJ
- Não chamar Spring (`@SpringBootTest`) em nenhum desses — todos puramente unitários

**Pra `TelegramWebhookControllerTest`:** cuidado — o controller usa `@RestController`, mas teste unitário NÃO precisa de MockMvc. Construa o controller via `new TelegramWebhookController(orchestratorMock, List.of("123"))` e chame `controller.receberMensagem(update)` direto. Mais rápido, mais simples.

**Pra `GlobalTelegramExceptionHandlerTest`:** verificar a estrutura do handler antes — ele provavelmente tem `@ExceptionHandler` em métodos. Testar cada método chamando ele direto com a exceção como argumento.

### Passo 5 — Reativar pipeline

1. Localizar `.github/workflows/deploy.yml` (ou outros workflows que rodem mvn)
2. Procurar `-DskipTests` no arquivo
3. Remover a flag — comando deve virar algo como `./mvnw clean package` ou `./mvnw clean verify`
4. Confirmar que ainda há um step que faz build do JAR pra deploy (o `verify` faz; o `package` também)

Não adicionar coverage reports, badges, ou qualquer integração extra. Manter mudança mínima: só remover o skip.

### Passo 6 — Atualizar `CLAUDE.md` da raiz

A linha que diz "Testes pulados temporariamente (`-DskipTests`) — reativar com H2 antes da próxima feature" não é mais verdade. Atualizar pra refletir o novo estado:

```
Pipeline roda testes unitários no merge para `main`. Cobertura mínima:
toda classe com lógica não-trivial tem teste; tarefas BE-* devem incluir
testes próprios como critério de aceitação.
```

### Passo 7 — Validar localmente

1. `./mvnw clean test` — todos os testes verdes em < 30s
2. Conferir que `target/surefire-reports/` tem 11+ relatórios XML (um por classe de teste)
3. Confirmar que `./mvnw clean package` (sem `-DskipTests`) também passa

### Passo 8 — Validar no CI

1. Push da branch, abrir PR pra develop
2. Aguardar pipeline rodar
3. Confirmar que o step de teste ficou verde
4. Conferir se o tempo total de pipeline ainda é razoável (não regredir mais que 1-2 min)

---

## Critério de aceitação

- [ ] H2 adicionado ao `pom.xml` no escopo `test`
- [ ] `src/test/resources/application-test.properties` criado
- [ ] `FinancasBotTelegramApplicationTests` passa com profile test (H2)
- [ ] 11 classes de teste criadas conforme tabela "Classes em escopo"
- [ ] Cada classe de teste tem pelo menos 3 métodos cobrindo caminho feliz + erros
- [ ] `./mvnw clean test` localmente passa em menos de 30 segundos
- [ ] Nenhum teste é `@SpringBootTest` exceto o `FinancasBotTelegramApplicationTests` (todos os outros são puramente unitários com Mockito)
- [ ] Pipeline atualizado: `-DskipTests` removido do workflow YAML
- [ ] PR pra develop tem build CI verde com testes rodando
- [ ] `CLAUDE.md` da raiz atualizado refletindo o novo estado do pipeline
- [ ] Nenhum teste depende de rede externa (Telegram, S3 reais) ou banco MySQL real
- [ ] Cobertura visual: `target/surefire-reports/` tem 12 relatórios XML (11 novos + 1 existente)

---

## Fora de escopo

- **Testes de integração** (Testcontainers, MySQL real) — fica pra fase futura
- **Coverage gate** (mínimo de %, JaCoCo, etc) — fica pra fase futura
- **Mutation testing** (PIT) — fora de cogitação por enquanto
- **Testes de performance / load** — não se aplica
- **Testes E2E via Telegram** — manuais, não automáticos
- **Adicionar comportamento novo nas classes sob teste** — esta tarefa só escreve testes contra o comportamento existente. Se um teste expor um bug, anotar em `status/` e tratar separado, não corrigir aqui.
- **Refatorar classes pra ficarem mais testáveis** — se uma classe está difícil de testar, escrever o teste com o que dá (ainda que feio) e anotar no status pra refactor futuro. Mudar produção é fora do escopo.
- **MapStruct, Lombok extras, ou qualquer dependência além do H2** — não adicionar.
- **Testar getters/setters / equals / hashCode** — sem valor, não escrever.

---

## Riscos e mitigações

**Risco:** algum teste expõe bug real no código existente.
**Mitigação:** anotar em `status/BE-01a-backfill-testes-unitarios.md` na seção "Decisões pendentes" listando o bug e o que o teste espera. Marcar o teste com `@Disabled("aguarda fix — ver status BE-01a")` e seguir. Não corrigir o bug nesta tarefa.

**Risco:** `FinancasBotTelegramApplicationTests` falha porque o contexto Spring não sobe sem certas configs em prod (Secrets Manager, etc).
**Mitigação:** o `application-test.properties` deve ter todas as configs mínimas mockadas (token, S3, etc placeholder). Se algum bean específico de prod (`@Profile("prod")`) explodir, anotar e ajustar o profile pra excluir.

**Risco:** o tempo de execução total de testes vira lerdo.
**Mitigação:** unit tests com Mockito puro são rápidos (< 50ms cada). 11 classes × 5 testes = 55 testes × 50ms = 3 segundos. Mais o boot do contexto Spring uma vez (~5-10s). Total esperado: < 20s. Se passar disso, investigar.

---

## Reportar status

Ao terminar, criar `docs/status/BE-01a-backfill-testes-unitarios.md` seguindo `docs/status/_TEMPLATE.md`. Cobrir:

- Confirmação de cada item do critério de aceitação
- Resultado de `./mvnw clean test` (cole o sumário final: `Tests run: X, Failures: Y, Errors: Z, Skipped: W`)
- Tempo total de execução dos testes
- Lista das classes que ganharam teste (deveria bater com a tabela de 11)
- Eventuais bugs descobertos e marcados como `@Disabled` (se houver)
- Print/cópia da execução do pipeline em CI mostrando build verde com testes
- Próximo passo: BE-01 (migration V2 da Fase 3) está liberada

**Próximo passo após esta tarefa:** BE-01 da Fase 3 — migration V2 com `requisitante`, datas, categoria como enum, `auth_token`. Pré-requisito: pipeline com testes rodando (entregue por esta tarefa).
