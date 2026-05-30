# DISPATCH — FIX-idempotencia-porta-application (single-task)

> **Sobre este arquivo:** prompt pronto pra colar numa sessão nova do **Claude do back** pra executar a FIX-idempotencia-porta-application. Estilo **single-task dispatch** (uma task focada, code-only, pára antes de mergear) — irmão menor dos `MASTER-PROMPT-overnight-*.md` que despacham várias tasks na mesma sessão.
>
> **Convenção provisória** (a confirmar em RETRO-02, item #10 do backlog): `MASTER-PROMPT-*.md` = overnight multi-task; `DISPATCH-*.md` = single-task. Os dois compartilham muito boilerplate — material pra discussão de "workflow extraído" em RETRO-02.
>
> **Quando colar:** depois que a fila de PRs em revisão clarear (✅ clareou em 2026-05-29 — todos da overnight 2 mergeados) e quando tu quiser que o back ataque o próximo item da sprint 02.

---

## Pré-condições

- BE-19a, BE-18, BE-21a, FIX-padronizar-restclient-builder **mergeados em `develop`** ✅ (commits em 2026-05-29).
- Worktree do planner em `develop` (regra nova do `CLAUDE.md` §"Worktrees git"). Implementadores devem usar o fluxo `git fetch && git checkout -b <branch> develop` direto — sem `git checkout develop` antes.

## O prompt (cole tudo a partir daqui na sessão do Claude do back)

```
Você é o Claude do back deste projeto. Leia, NESTA ORDEM:

- CLAUDE.md (regras globais — atenção especial à seção nova "Worktrees git" e ao Fluxo de branches atualizado em 2026-05-29)
- docs/roles/backend.md (seu papel)
- docs/STATE.md (onde estamos no ciclo)
- docs/sprints/02-canal-whatsapp/README.md (objetivo da sprint)
- docs/sprints/02-canal-whatsapp/plans/FIX-idempotencia-porta-application.md (a task — leia inteiro, intake até referências)

## A TASK

Executar a FIX-idempotencia-porta-application: extrair a porta `IdempotenciaMensagemPort` em `application/port/out/`, mover a implementação JdbcTemplate pra `adapters/out/persistence/idempotencia/JdbcIdempotenciaMensagemAdapter.java`, e ajustar o consumidor (`MensagemEntranteService`) pra receber a porta por construtor. SEM mudar SQL, SEM mudar tratamento de DuplicateKeyException, SEM mexer no @Transactional. É movimentação cirúrgica de camada.

## REGRAS DURAS

1. Branch: `fix/idempotencia-porta-application` (legado slug-only sem ID — exceção registrada no CLAUDE.md, plano FIX criado antes de 2026-05-29). NÃO renomear pra ter ID.
2. Criação da branch (atenção à mudança do CLAUDE.md): NÃO faça `git checkout develop` (develop está checada no worktree do planner — git vai recusar). Em vez disso:
   git fetch
   git checkout -b fix/idempotencia-porta-application develop
3. UM commit, padrão `fix(idempotencia-porta-application): extrair porta da camada application`.
4. Território: só `financas_bot_telegram/`. NÃO toque em frontend/ nem infra/.
5. NÃO mergeie. Abrir PR pra develop após status report + Reviewer.

## CRITÉRIO DE ACEITAÇÃO CHAVE (vai estar no PR)

Após a refatoração:

  grep -r "org\.springframework\.jdbc" src/main/java/.../application

DEVE retornar VAZIO. Se retornar qualquer coisa, a refatoração não está completa.

## VALIDAÇÃO LOCAL OBRIGATÓRIA

- `./mvnw test` verde (incluindo os testes da BE-19a, que devem passar sem alteração funcional — pode ser que precise ajustar mock/spy pra apontar pra porta em vez do service antigo).
- `./mvnw -q -DskipTests package` ok.

## DECISÃO A TOMAR NO MEIO DO CAMINHO

O `MensagemProcessadaService` ainda faz sentido depois de mover o JdbcTemplate pro adapter?
- Se ele só envelopava o JdbcTemplate: ele some, caller usa a porta direto.
- Se tem outra lógica genuína de aplicação: permanece como helper, consumindo a porta.

Anotar a decisão + justificativa breve no status report.

## STATUS REPORT

Escrever em `docs/sprints/02-canal-whatsapp/status/FIX-idempotencia-porta-application.md` seguindo `docs/templates/_TEMPLATE-status.md`. Frontmatter válido. Anotar:
- A decisão sobre o MensagemProcessadaService (mantido ou absorvido).
- Resultado do grep de validação.
- testes_total e testes_novos.

## SE QUEBRAR

Se algum teste da BE-19a quebrar com mudança de COMPORTAMENTO (não só ajuste de mock): PARE, registre no status como `estado: parcial`, e abra discussão. A FIX não deve mudar comportamento — quebra real significa que algo foi entendido errado.

Pare ao final do status report. Não mergeie.
```

---

## Notas pro humano (fora do prompt)

- **Estimativa de duração da sessão:** 30-60 min. É refactor mecânico; o tempo real vai depender de quanto teste da BE-19a precisa de ajuste de mock.
- **Coordenação com outras tarefas:** independente de tudo no momento. Pode rodar em paralelo com o smoke test E2E do EVO-02 Telegram (que é manual seu, não Claude).
- **Próxima task depois desta** (sugestão a confirmar): planejamento do **adapter de entrada do WhatsApp** (= EVO-01 end-to-end) ou **BE-22** (Micrometer + counter de falha do listener). Decide quando esta mergear.

## Referências

- Plano: `docs/sprints/02-canal-whatsapp/plans/FIX-idempotencia-porta-application.md`.
- CLAUDE.md §"Worktrees git" + §"Fluxo de branches" (atualizados 2026-05-29).
- Backlog item #10: `docs/plans/BACKLOG-evolucao-workflow.md` (separação roles/skills/workflows — este DISPATCH é insumo pra discussão).
