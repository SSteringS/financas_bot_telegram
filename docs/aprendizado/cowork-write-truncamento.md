# Cowork Write/Edit truncam escritas + mount FUSE serve view defasada

## Contexto da dúvida

Durante a sprint 02 e em especial na sessão de planejamento de 2026-05-30 (escrita da RETRO-02 e abertura da sprint kaizen), arquivos importantes do repo do planner chegaram **truncados no commit** sem que ninguém notasse no momento. O exemplo material é o commit `2c20dd9` ("documentação recuperada para ser puxada"), feito pra recompor pedaços perdidos. A hipótese inicial era **"sync gremlin do OneDrive"** — mas em 2026-05-30 o humano confirmou via Explorer que `C:\Users\satya\src\` **não está sob OneDrive**. A pergunta virou: o que está truncando os writes?

Diagnóstico (revisto na RETRO-02 §8a): combinação de **bug nas Cowork tools `Write`/`Edit`** (truncam writes médios reportando sucesso) e **visão defasada do mount FUSE** que o sandbox Linux do Cowork usa pra acessar arquivos do Windows.

## Resumo destilado

Cowork roda no Windows do humano e expõe o disco pra um sandbox Linux via um mount FUSE. Duas falhas separadas atrapalham trabalho com docs:

1. **`Write` e `Edit` truncam silenciosamente.** O tool retorna "updated successfully" mas o disco final tem menos linhas do que o conteúdo enviado — final cortado mid-string ou mid-line. Não é determinístico no tamanho (visto entre 1 e 50 linhas perdidas).
2. **O mount FUSE serve uma view defasada do disco.** `git status` no sandbox pode mostrar arquivos como "modified" que estão íntegros no disco real (`git status` no terminal Windows mostra clean). `Read` tool e `cat` no sandbox podem retornar conteúdo diferente do que está realmente no `.git`.
3. **Mount FUSE não permite delete.** `rm` falha com `Operation not permitted`. Lixo (`.tmp`, `index.lock`, `DISPATCH-*` antigos) só some quando o humano deleta pelo Windows.

Não temos controle sobre o Cowork. A mitigação é puramente operacional: **escrever via bash heredoc, verificar com `wc -l && tail`, e tratar o terminal Windows do humano como fonte da verdade do estado do repo**.

## Pontos-chave

- **Cowork `Write`/`Edit` mente:** retorna sucesso mesmo quando trunca o arquivo. Não confiar.
- **Bash via `cat > arquivo << 'EOF' ... EOF`** é o caminho confiável pra escrever arquivos médios/grandes.
- **Sempre verificar** com `wc -l arquivo && tail -3 arquivo` após o write — se o tail não bater, refazer.
- **`git status` no sandbox pode mentir.** Mount FUSE serve disco defasado em alguns casos; terminal Windows do humano é a verdade.
- **`rm`/`unlink` no sandbox falha** dentro do mount; humano apaga pelo Explorer/PowerShell.
- **Não é OneDrive.** `C:\Users\satya\src\` está fora do OneDrive — a hipótese original estava errada.
- Pattern de truncamento não tem tamanho fixo: já vimos cortes em writes de 1 linha (`docs/retrospectivas/README.md`), 50 linhas (`metricas_status.py`) e arquivos inteiros (CLAUDE.md de 162 → 115 linhas).

## Evidência

### A — `Write`/`Edit` tools truncam writes médios (4 ocorrências em 2026-05-30)

1. **Sessão anterior, CLAUDE.md (planner):** edição que deveria resultar em ~162 linhas terminou em 115 linhas com final literal `git cherry-pic`. Foi commitada truncada no `2c20dd9`.
2. **Esta sessão (planning RETRO-02), Write em CLAUDE.md:** tool retornou "updated successfully" mas timestamp do disco não mudou. `Read` tool via 162 linhas; bash `wc -l` via 115. Discrepância só foi resolvida ao reescrever via bash heredoc.
3. **Esta sessão, Edit em `docs/scripts/metricas_status.py`:** patch deveria adicionar ~50 linhas; arquivo terminou em `sd = sprint_` (corte mid-string).
4. **Esta sessão, Edit em `docs/retrospectivas/README.md`:** patch curto (1 linha trocada) terminou em `[`RETRO-0`.

Padrão: truncamento **não é determinístico em tamanho** (de 1 a 50 linhas perdidas), e a tool **reporta sucesso** mesmo quando o write fica incompleto.

### B — Mount FUSE do sandbox Linux mostra disco defasado

Após o FE-14 mergear em develop (PR #80) e o humano rodar `git restore` no terminal Windows:

- `git status` no terminal Windows: **clean**.
- `git status` no sandbox Linux do Cowork: **7 arquivos do front como modified** com conteúdo truncado.
- `git show HEAD:frontend/.../ModalComprovante.tsx | wc -c`: 596 bytes (correto — bate com o que está no commit).
- `wc -c frontend/.../ModalComprovante.tsx` no sandbox: 4166 bytes (inclui trailing whitespace gigante que não existe no disco real).

`Read` tool e `cat` no sandbox podem ver coisas diferentes do disco real. Decisão baseada na visão do sandbox arrisca rodar `git restore` em arquivo que está clean — apagando trabalho.

### C — Mount FUSE não permite delete

`rm` e `os.unlink()` falham com `Operation not permitted` em arquivos dentro do mount, mesmo em arquivos que o próprio sandbox acabou de criar via bash. Sobraram `.tmp` e `DISPATCH-FIX-001` na pasta do planner em 2026-05-30; só o humano conseguiu apagar (via Explorer/PowerShell no Windows). Mesma classe de problema travou um `git add` na sessão da RETRO-02 — sobrou `index.lock` órfão que o sandbox não conseguiu remover.

## Workarounds testados

| Operação | Tool problemática | Workaround confiável |
|---|---|---|
| Escrever arquivo > 20 linhas | `Write`, `Edit` (Cowork) | `cat > arquivo << 'EOF' ... EOF` via bash (`mcp__workspace__bash`) |
| Confirmar conteúdo escrito | `Read` tool | `wc -l arquivo && tail -3 arquivo` via bash + comparação com expectativa |
| Conferir estado real do repo | `git status` no sandbox | `git status` no terminal Windows do humano |
| Apagar lixo no mount do planner | `rm` no sandbox | Humano apaga via Explorer/PowerShell |

Regra operacional do planner fica em `docs/roles/planner.md` §"Escrita defensiva de arquivos (workaround Cowork)" — três regras curtas que o agente segue no boot de toda sessão.

## Bug report rascunho (pra colar no feedback do Cowork)

> **Idioma:** inglês. **Quem cola:** humano. **Quando:** quando der vontade — o workaround interno cobre o dia a dia, mas o bug deveria ser reportado pro time entender o impacto.

```
Title: Cowork Write/Edit tools silently truncate file writes on Windows + FUSE mount serves stale view

Environment:
- Cowork desktop app on Windows 11.
- Working folder is a Git worktree under C:\Users\satya\src\ (not under OneDrive — confirmed via Explorer; folder has no OneDrive sync column).
- Sandbox runs a Linux VM that accesses the Windows folder via what appears to be a FUSE mount.

Three related symptoms observed repeatedly across multiple sessions in May 2026:

1. Write/Edit tools silently truncate writes.
   The Write and Edit tools return "updated successfully" / no error, but the file on disk is shorter than the content sent — cut mid-string or mid-line, no terminating newline.
   Truncation length is non-deterministic: I have seen 1-line patches truncated to "[`RETRO-0", a ~50-line patch truncated mid-string ("sd = sprint_"), and a full-file Write truncated from 162 lines to 115 (final line "git cherry-pic"). The truncated writes have been committed by mistake because the tool reported success — recovery commits are visible in the repo history.
   Workaround: writing files via the bash tool using `cat > file << 'EOF' ... EOF` (heredoc) is reliable; verifying with `wc -l && tail` catches the truncation when it happens.

2. FUSE mount serves a stale view of the disk.
   After the user runs `git restore` in their Windows terminal and the Windows `git status` shows clean, the sandbox Linux's `git status` shows several files as "modified" with truncated/garbage content. `wc -c` on the same path inside the sandbox and on Windows return different sizes — sandbox sees a cached/stale snapshot of the file.
   Workaround: treat the Windows terminal `git status` as ground truth; never run `git restore` or `git checkout -- <path>` from the sandbox based on what the FUSE mount reports.

3. Mount does not allow deletes.
   `rm` and `os.unlink()` fail with `Operation not permitted` on files inside the mount, even on files the sandbox itself created. `.tmp` files, stale `index.lock` files and abandoned scratch files accumulate; only deleting from Windows works.
   Workaround: ask the user to delete stale files from Explorer/PowerShell.

Questions for the team:
- Is there a known issue with Write/Edit on Windows file paths > N bytes?
- Can the FUSE mount be flushed / re-synced after the host disk changes?
- Is there a configuration option to allow deletes from the sandbox?

Happy to provide repro steps and exact file paths if useful.
```

## Pra aprofundar

- FUSE caching modes (writeback, default, direct_io) e como afetam consistência sandbox ↔ disco real.
- Padrões de heredoc seguro em bash (`<< 'EOF'` vs `<< EOF` — aspas evitam expansão de variáveis dentro do conteúdo).
- Por que `wc -l && tail` é mais confiável que `Read` tool: bash chama `read()`/`stat()` direto no FS; `Read` tool pode passar por cache do Cowork.
- Padrões de "write-then-verify" em sistemas distribuídos — análogo conceitual.
- Histórico do projeto: ações #6 da RETRO-01 (confiabilidade do `STATE.md`) e #1 da RETRO-02 (esta mesma causa, escopo correto).
