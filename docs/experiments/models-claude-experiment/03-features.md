# As três features

## 1. Rubrica de complexidade — pré-registrada

Complexidade precisa ser **medida antes**, por rubrica, e classificada por **dois avaliadores independentes**. Complexidade assertada pelo autor não sobrevive a revisão.

**Pontuação:**

- **+1** por camada tocada: domínio · aplicação · API/controller · persistência · adapter de canal · frontend · infra
- **+2** migration com backfill
- **+2** integração externa nova
- **+1** se mais de 8 critérios de aceitação
- **+1** se exige padrão arquitetural novo

**Faixas:** Baixa ≤ 3 · Média 4–7 · **Alta ≥ 8**

## 2. Seleção

| Nível | Feature | Score | Camadas / arquivos | Por que discrimina |
|---|---|---|---|---|
| **Baixa (3)** | **FIX — comprovante mal-formado vira pedido novo.** `123 pix` (sem `#`) é classificado como pedido em vez de erro de comprovante; deve responder erro orientativo | domínio(1) + aplicação(1) + regra nova(1) | `LegendaParser`, `PaymentProofStrategy`, `PaymentRequestStrategy`, `MensagemEntranteService.ERROR_MESSAGE` | Exige **decidir a regra de desambiguação**, não só codificar. Sem migration, sem frontend. Fonte: `docs/PENDENCIAS-TECNICAS.md:131` |
| **Média (5)** | **EVO-04 — resumo mensal pelo bot** (`resumo abril`) | domínio(1) + aplicação(1) + adapter in(1) + adapter out(1) + padrão novo(1) | primeira strategy que **não exige mídia** → força extensão do `TelegramMessageMapper` | A API de resumo já existe (BE-09), então o trabalho é de **integração e extensão de ponto de extensão**, não de CRUD |
| **Alta (8)** | **Contatos-BE — cadastro de chaves PIX úteis (variante B2)** | domínio(1)+aplicação(1)+API(1)+persistência(1) + backfill(2) + >8 critérios(1) + VO novo(1) | `TipoChavePix`, VO `ChavePix` com normalização, `V8__contatos.sql` com backfill idempotente, unicidade + 409, escopo por requisitante | Metade sem template no repo. Backfill idempotente e normalização por tipo de chave são armadilhas clássicas |

## 3. Por que `Contatos` precisa ser a variante B2

O levantamento do domínio mostrou que **`chavePix` já existe — mas só como `String` crua** em `Funcionario` (`@Column(name="chave_pix", length=255)`, validação = apenas `!isBlank()` quando `formaPagamento == PIX`). Não existe entidade, tabela, endpoint, value object ou enum de tipo de chave.

**Custo medido de um CRUD típico neste repositório** (vertical `Funcionario`, a mais recente):

| | Quantidade |
|---|---|
| Arquivos de produção | **15 novos** + 1 edição (`RestExceptionHandler`) |
| Migration | 1 (`V8__*.sql` seria a próxima) |
| Arquivos de teste | **6** · **41 testes** (13 use case · 7 controller · 14 persistência · 7 integração) |
| Frontend, se incluído | ~7 arquivos (5 novos, 2 editados) |

**Isso é um problema experimental sério.** `Funcionario` é um template quase perfeito para `Contatos`. Um CRUD puro seria copiar-e-adaptar 15 arquivos: todos os modelos acertariam, **efeito de teto**, poder discriminante próximo de zero, e nove runs caros gastos para não descobrir nada.

A complexidade tem que vir de onde **não há template**.

### Trade-off do escopo de `Contatos`

| | **B1 — CRUD puro** | **B2 — CRUD + regras de domínio + backfill** (recomendado) | **B3 — B2 + interface pelo bot** |
|---|---|---|---|
| Escopo | apelido + chave (String) + soft delete + 5 endpoints | B1 + `TipoChavePix` com validação e normalização por tipo (CPF/CNPJ com dígito verificador, e-mail, telefone E.164, aleatória UUID) + escopo por requisitante com teste de isolamento + unicidade `(requisitante, chave_normalizada)` → 409 + migration V8 com backfill de `funcionario.chave_pix` | B2 + strategy nova no bot |
| Score | 4 → Média | **8 → Alta** | 10+ → Alta |
| Template no repo | Cópia quase literal de `Funcionario` | Metade tem template, metade não existe | Terreno inexplorado |
| Poder discriminante | **Próximo de zero (efeito de teto)** | **Alto** | Alto, com variância enorme |
| Risco | Desperdiça ~9 runs caros | Médio | **Alto** — exige mexer no `TelegramMessageMapper` para texto puro; a ordem das strategies é `findFirst()` não-determinística; risco de colisão de regex |

**Recomendação: B2, backend-only.** O frontend fica fora — o subagente `frontend` não existe no repositório — e é entregue depois, fora do experimento. O bot fica fora: habilitar mensagem de texto puro tem custo real e transformaria a feature em pesquisa exploratória, o oposto do que um experimento controlado precisa.

## 4. Esqueleto da spec de `Contatos` — a refinar pelo humano

> Refinar até **"o quê"**. Parar. Congelar. Não descer para plano técnico.

- Entidade `Contato`: apelido, chave, tipo de chave, `ativo` (soft delete), `criado_em`, `atualizado_em`
- **Escopo por requisitante** — decisão de modelagem nº 1. `Funcionario` é global (a tabela V6 não tem `requisitante_id`); `Pedido` é por requisitante (`@RequisitanteId` + `IsolamentoRequisitanteIntegrationTest`). `Contato` segue o padrão de `Pedido`, com teste de isolamento próprio
- `TipoChavePix` = `CPF` | `CNPJ` | `EMAIL` | `TELEFONE` | `ALEATORIA`
- **Normalização e validação por tipo**, com dígito verificador para CPF e CNPJ, E.164 para telefone, UUID para aleatória
- **Unicidade** por `(requisitante_id, chave_normalizada)` → conflito retorna **409**
- 5 endpoints REST sob `/api/v1/contatos` — a rota fica protegida automaticamente pela allowlist positiva do `JwtAuthenticationFilter`, sem configuração adicional
- Soft delete seguindo a convenção do repo (`ativo BOOLEAN NOT NULL DEFAULT TRUE`)
- **Migration `V8__contatos.sql` com backfill idempotente** de `funcionario.chave_pix` para contatos do requisitante dono
- Critérios de aceitação numerados e **executáveis** — mais de 8, conforme a rubrica

## 5. Convenções do repositório que a spec deve respeitar

Extraídas do código existente, para que a spec não precise repeti-las e para que desvio delas conte como defeito:

- **Arquitetura hexagonal com padrão "port + service"** (o mais novo, usado em EVO-09): `application/port/in/XxxPortIn.java` (interface pura) + `application/services/XxxServiceImpl.java` (`@Service`). Não usar o padrão antigo `application/usecases/`.
- **Persistência em 3 arquivos por agregado:** `XxxJpaRepository` (Spring Data) + `XxxRepositoryAdapter` (`@Component`, implementa o port out) + `XxxMapper` (`@Component`, escrito à mão — o repo **não usa MapStruct**).
- **DTOs:** classes Lombok em `application/dto/`, sufixo `Request`/`Response`; response com factory estática `from(DomainEntity)`. Não há records.
- **Validação:** Bean Validation no DTO para regras simples; **validações condicionais e cross-field vão no use case**, lançando `IllegalArgumentException` → 400 `PARAMETRO_INVALIDO`. Regra explícita documentada no JavaDoc de `FuncionarioRequest`.
- **Erros:** `RestExceptionHandler` (`@RestControllerAdvice` restrito a `adapters.in.rest`), payload `ErroDTO` = `{codigo, mensagem}`. Uma feature nova adiciona `@ExceptionHandler` aqui.
- **Migration Flyway:** `V<n>__snake_case.sql`, cabeçalho em comentário explicando o porquê, MySQL 8 / InnoDB / utf8mb4, `BIGINT AUTO_INCREMENT`, `criado_em`/`atualizado_em`, `CHECK` nomeadas espelhando as regras do use case, prefixos `fk_`/`chk_`/`uq_`/`idx_`, comentário sobre impacto de lock.
- **Testes de integração:** Testcontainers com MySQL 8 real (não H2), `AbstractIntegrationTest` com container singleton, Flyway rodando de verdade, autenticação JWT real via `autenticarComo(Long)`, limpeza em `@AfterEach` respeitando ordem de FK. **Não existe `putAutenticado` na base** — oportunidade de reuso se Contatos tiver PUT.

## 6. Riscos das features

1. **Efeito de teto em `Contatos`.** O template `Funcionario` é forte. A variante B2 mitiga concentrando o trabalho onde não há template, mas o risco sobrevive — se o piloto mostrar qualidade equivalente em todas as configurações, a feature alta não discrimina.
2. **Complexidade confundida com domínio.** A feature alta não é só mais complexa: é outro subdomínio. Insolúvel com 3 features. Declarar como limitação.
3. **EVO-04 depende de um ponto de extensão nunca exercitado.** Todo o pipeline do bot assume mídia anexada; as duas strategies existentes lançam `PhotoProcessingException` se `fileBytes == null`, e `supports()` roda sobre a legenda. Mensagem de texto puro provavelmente não chega com `caption` preenchida — a extensão do `TelegramMessageMapper` é parte legítima da feature, mas aumenta a variância.
4. **Ordem das strategies é `findFirst()` sobre a lista injetada** — não determinística nem explicitamente controlada. Uma strategy nova com regex ampla pode colidir com as existentes.
