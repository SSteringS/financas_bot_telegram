# As três features

> **Estado:** fechado em 2026-08-10 com o humano. Scores recalculados e rastreáveis.
> **Specs congeladas:** `specs/SPEC-baixa-*.md`, `specs/SPEC-media-*.md`, `specs/SPEC-alta-*.md`.

## 1. Rubrica de complexidade — pré-registrada

**Pontuação:**

- **+1** por camada tocada: domínio · aplicação · API/controller · persistência · adapter de canal · frontend · infra
- **+2** migration com backfill
- **+2** integração externa nova
- **+1** se mais de 8 critérios de aceitação
- **+1** se exige padrão arquitetural novo

**Faixas:** Baixa ≤ 3 · Média 4–7 · Alta ≥ 8

> ⚠️ **Limitação declarada.** As três features selecionadas pontuam **2 / 5 / 7**. Nenhuma alcança a faixa "Alta" da rubrica original. A decisão foi deliberada — ver §3. O fator complexidade opera numa faixa mais estreita que o desenho inicial previa, e isso reduz o poder de detectar interação entre complexidade e alocação de modelo. **Declarar como limitação na publicação, não mascarar renomeando faixas.**

## 2. Critérios de seleção — aprendidos durante o desenho

Estes critérios **não estavam** no desenho original. Emergiram ao descartar cinco candidatas e valem para qualquer feature futura do experimento.

1. **Domínio vivo.** A feature deve tocar código que roda em produção com dados reais. Descartou `Contatos` com backfill de `funcionario.chave_pix` e `reabertura de mês` — a folha de pagamento nunca foi deployada, logo ambos os backfills operariam sobre tabelas vazias e valiam `+2` fantasma.
2. **Utilidade real.** A feature tem que ser trabalho que se faria de qualquer jeito. Descartou `trilha de auditoria de status` — cerimônia num bot de um usuário. Complexidade sem utilidade é dívida disfarçada.
3. **Determinismo do sistema sob teste.** Nada de LLM dentro da feature. O endpoint primário Q1 mede defeitos escapados; um extrator não-determinístico dentro do sistema torna impossível separar defeito de implementação de variação do extrator.
4. **Aditiva.** Não refatora padrão já implementado. Descartou converter `CategoriaPedido` de enum para tabela — o enum está embutido em JPQL com literal qualificado (`PedidoPagamentoJpaRepository:54,74,82`).
5. **Sem template quase-literal no repo.** Evita efeito de teto.

## 3. Seleção

| Nível | Feature | Score | Spec |
|---|---|---|---|
| **Baixa** | Comprovante mal-formado sem `#` vira pedido novo | **2** | `specs/SPEC-baixa-comprovante-malformado.md` |
| **Média** | Contatos — agenda de chaves PIX (backend-only, sem backfill) | **5** | `specs/SPEC-media-contatos.md` |
| **Alta** | Extração de valor do boleto por código de barras | **7** | `specs/SPEC-alta-extracao-boleto.md` |

### Scores rastreáveis

**Baixa — comprovante mal-formado = 2**

| Item | Pontos |
|---|---|
| Camada domínio (`LegendaParser`) | +1 |
| Camada aplicação (`MensagemEntranteService`, strategies) | +1 |
| **Total** | **2** |

Sem migration, sem integração externa, ≤8 critérios, sem padrão novo.

**Média — Contatos = 5**

| Item | Pontos |
|---|---|
| Camadas: domínio, aplicação, API, persistência | +4 |
| Mais de 8 critérios de aceitação | +1 |
| **Total** | **5** |

O `+2` de migration com backfill foi **removido**: o backfill previsto lia `funcionario.chave_pix`, e `funcionario` está vazia em produção. A migration continua (tabela nova), mas **sem backfill** — não pontua.

**Alta — extração de boleto = 7**

| Item | Pontos |
|---|---|
| Camadas: domínio (parser + dígito verificador), aplicação, adapter in (Telegram), adapter out (leitor), infra (dependência nova no runtime e na EC2) | +5 |
| Mais de 8 critérios de aceitação | +1 |
| Padrão novo: pipeline de extração com validação e fallback | +1 |
| **Total** | **7** |

Não pontua integração externa: a leitura é feita por biblioteca local, **sem chamada de rede**. Foi decisão explícita — usar API de LLM daria `+2` (score 9) ao custo de reintroduzir custo recorrente, termos de uso de dados financeiros de terceiro, e não-determinismo dentro do sistema sob teste, violando o critério 3.

## 4. Convenções do repositório que as specs pressupõem

Extraídas do código existente. **Desvio delas conta como defeito na auditoria cega.** Verificadas contra o repo em 2026-08-10.

- **Ports:** padrão atual é `application/port/in/XxxPortIn` + `application/services/XxxServiceImpl` (`@Service`). O pacote `application/usecases/` é **legado** — não criar coisa nova ali.
- **JPA vive no adapter.** Entidades `@Entity` em `adapters/out/persistence/entity/`. `domain/` tem POJO puro.
- **Persistência = 3 arquivos por agregado:** `XxxJpaRepository` + `XxxRepositoryAdapter` (`@Component`) + `XxxMapper` (`@Component`, à mão — **sem MapStruct**).
- **DTOs:** classes Lombok em `application/dto/`, sufixo `Request`/`Response`, factory estática `from(...)`. **Sem records.**
- **Validação:** Bean Validation no DTO para regra simples; condicional e cross-field no service, lançando `IllegalArgumentException` → 400 `PARAMETRO_INVALIDO`.
- **Erros REST:** `RestExceptionHandler`, payload `ErroDTO = {codigo, mensagem}`.
- **Migration Flyway:** `V<n>__snake_case.sql`, **próxima livre é `V8`**. MySQL 8 / InnoDB / utf8mb4, `BIGINT AUTO_INCREMENT`, `criado_em`/`atualizado_em`, `CHECK` nomeadas, prefixos `fk_`/`chk_`/`uq_`/`idx_`, cabeçalho comentado com o porquê e impacto de lock.
- **Auth:** `JwtAuthenticationFilter` usa **allowlist positiva** — todo path sob `/api/` exige JWT exceto `/api/v1/auth/exchange`. Controller novo sob `/api/v1/**` já nasce protegido.
- **Testes de integração:** Testcontainers com **MySQL 8 real** (não H2), `AbstractIntegrationTest` com container singleton, Flyway rodando, auth JWT real via `autenticarComo(Long)`, limpeza em `@AfterEach` respeitando ordem de FK.
- **Property que aponta pra secret:** default vai no `.properties`, não no `@Value` — placeholder aninhado não herda default do externo (`docs/aprendizado/spring-placeholder-aninhado-default.md`).

## 5. Riscos das features

1. **Faixa estreita de complexidade (2/5/7).** O contraste Baixa × Alta é de 5 pontos, menor que o previsto. Reduz poder de detectar interação complexidade × papel. Declarado como limitação.
2. **Baixa e Alta tocam o mesmo despacho por regex.** Ambas mexem na decisão entre `PaymentRequestStrategy` e `PaymentProofStrategy`. Como **nenhum branch experimental é mergeado** e todo run parte do mesmo baseline, não há conflito real entre runs. Mas se as duas forem implementadas de verdade depois, exigem reconciliação — anotar no backlog.
3. **Ordem das strategies é `findFirst()`** sobre a lista injetada, não determinística nem controlada. Strategy nova com regex ampla pode colidir. Vale para Baixa e Alta.
4. **Efeito de teto na Média.** `Funcionario` é template forte para `Contatos`. Sem o backfill, a feature ficou mais próxima de CRUD puro — o risco de teto **aumentou** em relação à variante B2 original. Monitorar no piloto: se todas as configurações acertarem, a Média perde poder discriminante e o screening fica comprometido.
5. **Alta depende de especificação externa não verificada.** Posições de campo da linha digitável e formato de boleto de concessionária vêm da especificação FEBRABAN. Não foram verificadas no desenho — a task inclui essa pesquisa, e isso é parte legítima da complexidade.

## 6. Complexidade precisa de segundo avaliador

A rubrica exige classificação por **dois avaliadores independentes**. Os scores acima foram atribuídos por um só (agente `planner`, com verificação contra o código). **Pendência bloqueante da Fase 2:** um segundo avaliador aplica a rubrica às três specs sem ver esta tabela, e as divergências são registradas.
