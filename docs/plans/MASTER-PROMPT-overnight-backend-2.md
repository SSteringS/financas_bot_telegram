# Prompt mestre — overnight backend #2 (polish + EVO-07)

> Continuação do trabalho do back. A primeira sessão overnight (BE-03 a BE-15 + BE-14) já foi mergeada em develop. Esta sessão pega 5 tarefas: 4 de polish (pequenas) + 1 substancial (EVO-07 — aceitar PDF/document). O resultado fica em uma branch isolada que NÃO vai pra develop sem revisão humana primeiro.

---

## Copie tudo abaixo desta linha pra dentro do Claude Code do back

---

Você é o **Claude do back-end**, continuando o trabalho de uma sessão anterior que já fechou a Fase 3a completa (BE-03 a BE-15 + BE-14 integração). Esta noite você vai executar 5 tarefas: 4 pequenas de polish e 1 substancial de evolução (EVO-07).

**Antes de tudo, leia obrigatoriamente:**

- `financas_bot_telegram/CLAUDE.md` (se existir) E `CLAUDE.md` na raiz do repo — sua identidade, território, regras
- `docs/README.md` — convenções da pasta de docs
- `docs/PENDENCIAS-TECNICAS.md` — contexto dos itens pequenos que vamos atacar

**Regras gerais de execução (NÃO QUEBRE):**

1. **Crie uma única branch** chamada `feature/backend-polish-evo07` a partir de `develop` atualizada. Trabalhe nela do começo ao fim. **NÃO faça push pra develop em momento nenhum** — o humano vai revisar tudo depois e mergear quando achar adequado.

2. **Cada tarefa = um commit dedicado nesta branch.** Mensagem padronizada:
   - `fix(FIX-XX): título` pras tarefas de polish/correção
   - `feat(EVO-07): título` pra EVO-07
   
   Use seu julgamento pra escrever mensagens descritivas em uma linha.

3. **Antes de começar cada tarefa:** leia o plano correspondente em `docs/plans/<arquivo>.md`. Os planos têm código pronto pra usar como base.

4. **Implemente seguindo o plano.** Se quiser desviar (estrutura diferente, biblioteca diferente, decisão de design), **pare e documente no status report**, não improvise.

5. **Após implementar, rode os testes:**
   ```bash
   cd financas_bot_telegram
   ./mvnw test
   ```
   Tudo verde antes de commitar. Se algum teste antigo vermelho, investiga: a tarefa quebrou contrato existente? Provavelmente bug. Pare e relate.

6. **Escreva o status report** em `docs/status/<TASK-ID>-titulo.md` seguindo `docs/status/_TEMPLATE.md`. Status report é parte do commit (mesmo commit da implementação, não separado).

7. **Atualize `docs/PENDENCIAS-TECNICAS.md`** movendo cada item que você fechou da seção "Itens abertos" pra "Itens resolvidos", com referência ao commit.

8. **Commit** com mensagem padrão. Não faça push intermediário.

9. **Passe pra próxima tarefa.** Sem pedir permissão, sem aguardar.

**Pontos pra PARAR e me esperar:**

Cria `docs/status/<TASK-ID>-PARADO.md` explicando o motivo se:

- Testes antigos quebrarem e o problema não é trivial
- Algum plano referencia arquivo/classe/método que você não encontra
- Compilação fica num estado quebrado e o fix não é claro em <15 min
- Você bate em decisão de produto não documentada (não improvisa — relate)
- Pra EVO-07: se a migration V3 não rodar em dev por algum motivo (banco em estado estranho, conflito com schema atual)

**Pré-requisito da EVO-07: Docker pode ser opcional aqui**, mas se for executar testes de integração da BE-14 junto (Testcontainers), precisa de Docker. Como já existem em develop, se Docker não rolar, o `@Testcontainers(disabledWithoutDocker = true)` configurado vai pular os de integração — não bloqueia.

**Sequência das 5 tarefas, na ordem exata:**

1. `docs/plans/FIX-hide-requisitanteid-swagger.md` — esconder `@RequisitanteId` do Swagger UI (~30 min)
2. `docs/plans/BE-15b-escopo-exception-handlers.md` — restringir `basePackages` dos `@RestControllerAdvice` (~45 min)
3. `docs/plans/FIX-keystore-password-secret.md` — mover keystore_password pro Secrets Manager (~20 min, atenção: anote no status que humano precisa adicionar a chave no Secrets Manager antes de deploy)
4. `docs/plans/FIX-revisar-msgs-erro-bot.md` — revisar mensagens de erro/ajuda do bot (~1h)
5. `docs/plans/EVO-07-aceitar-document-pdf.md` — aceitar document/PDF no bot (~4-5h, inclui migration V3, novo enum, mudanças em strategy + S3 service + mapper + tests)

**Total estimado:** 6-7 horas. Confortável pra uma noite.

**Lembretes finais:**

- Esta branch **NÃO** vai pra develop. O humano revisa e decide depois.
- **Tipo `record` em Java pra DTOs** quando criar algum novo — segue padrão estabelecido na BE-04
- **Mapper manual** (sem MapStruct) — padrão estabelecido em BE-00B
- **Lombok** em entities JPA e domain POJOs — padrão estabelecido
- **Testes mockados** com Mockito + AssertJ — padrão estabelecido em BE-01a
- **Não rotacionar o token do Telegram** — é tarefa do humano via BotFather, fora do seu escopo
- **Não tocar em `frontend/`** — território do Claude do front
- **Não tocar em `docs/architecture/`, `docs/aprendizado/`, ou outros docs do planejador** — só `docs/status/` e `docs/PENDENCIAS-TECNICAS.md` (este último apenas pra mover itens fechados pra seção de resolvidos)

**Resumo final:**

Quando terminar todas as 5 (ou parar em alguma), deixa `docs/status/_RESUMO-overnight-back-2.md` listando:

- Tabela tarefa → commit hash → status
- O que ficou parado e por quê (se aplicável)
- Pendências novas que apareceram
- Comportamento estranho notado durante a execução
- Recordatório explícito sobre **adicionar `keystore_password` no Secrets Manager antes do próximo deploy em prod** (esse é o único item que exige ação manual do humano)

Esse arquivo é o que o humano vai ler primeiro de manhã.

Boa execução.

---

## Fim do prompt — daqui pra baixo são notas pra você (humano)

---

## O que esperar de manhã

Se tudo correr bem, ao acordar você terá:

- **Branch `feature/backend-polish-evo07`** com 5 commits novos, sem push pra develop
- **5 status reports** em `docs/status/FIX-*.md` e `docs/status/EVO-07-*.md`
- **1 arquivo `docs/status/_RESUMO-overnight-back-2.md`** com tudo organizado
- **`docs/PENDENCIAS-TECNICAS.md`** atualizado, com 4 itens movidos pra "resolvidos"
- **`docs/plans/FASE-3-VISUALIZACAO.md`** com EVO-07 marcada como concluída

## Antes de mergear essa branch em develop

Diferente dos overnights anteriores, esta sessão NÃO vai pra develop direto. Você vai mergear quando:

1. Terminar a análise do front comigo (revisão FE-03 a FE-11)
2. Mergear o front em develop primeiro
3. Decidir tratar essas polish + EVO-07 também — aí mergeia essa branch

Quando for hora de mergear, vale rodar de novo localmente:
- `./mvnw test` — todos verdes (unit + integration)
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` — sobe sem erro
- Smoke test do bot: mandar foto + legenda inválida (ver nova msg de erro), mandar PDF/imagem-do-WhatsApp pra registrar comprovante (EVO-07)
- Conferir `keystore_password` no Secrets Manager antes do deploy em prod
