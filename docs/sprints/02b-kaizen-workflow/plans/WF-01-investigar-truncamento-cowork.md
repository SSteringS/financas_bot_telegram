# WF-01 — Investigar e mitigar truncamento de writes do Cowork

> **Intake**
>
> - **Origem:** ação #1 da RETRO-02 (com causa-raiz corrigida na seção 8a). Sintomas observados ao longo das sprints 01 e 02; agudizaram na sessão de planejamento de 2026-05-30 (4 truncamentos em sequência + visão defasada do mount FUSE descoberta tarde).
> - **Prioridade:** alta. Sem isso, qualquer sessão de planejamento pode gerar commits truncados em arquivos críticos (CLAUDE.md, STATE.md, planos, ADRs, retros). Já aconteceu (`2c20dd9` "documentação recuperada para ser puxada" foi commit truncado real).
> - **Esforço:** baixo (~1h, sessão única do planner). Mecânico depois das decisões fechadas.
> - **Território / quem executa:** `docs/aprendizado/` + `docs/roles/planner.md` + `CLAUDE.md` → **Claude do planejamento** (esta task é meta-workflow, o próprio planner executa).
> - **Fluxo de git:** commit direto em `develop` (regra do planner — CLAUDE.md §Instâncias). **Sem branch de feature, sem PR.** Humano revisa diff via `git show HEAD` antes do `git push`.
> - **Dependências:** nenhuma; rodável a qualquer momento da sprint kaizen.
> - **Riscos:** baixos — não muda código de produto, só documentação + regras. Risco residual: regra pode ser ignorada por sessões futuras se não estiver na boot sequence.

---

## Contexto

A RETRO-02 §4 e §8a registraram um conjunto de incidentes recorrentes que estavam sendo atribuídos a "sync gremlin do OneDrive". Diagnóstico revisto em 2026-05-30: **`C:\Users\satya\src\` não está no OneDrive** (confirmado pelo humano via Explorer). A causa real é outra — provavelmente uma combinação de bug das Cowork tools `Write`/`Edit` (truncam writes médios) e visão defasada do mount FUSE do sandbox Linux.

Não temos controle sobre o Cowork. Esta task documenta o que sabemos, fixa um padrão defensivo que o planner segue, e prepara um bug report pro time do Cowork.

## Sintomas observados (com evidência)

### A. `Write`/`Edit` tools truncam writes médios

Quatro ocorrências confirmadas na sessão 2026-05-30:

1. **Sessão anterior, CLAUDE.md (planner):** edição que deveria resultar em ~162 linhas terminou em 115 linhas com final "git cherry-pic". Foi commitada truncada (`2c20dd9`).
2. **Esta sessão, Write em CLAUDE.md:** tool retornou "updated successfully" mas timestamp do disco não mudou. Read tool via 162 linhas; bash `wc -l` via 115. Discrepância só foi resolvida ao reescrever via bash heredoc.
3. **Esta sessão, Edit em `metricas_status.py`:** patch deveria adicionar ~50 linhas; arquivo terminou em "sd = sprint_" (corte mid-string).
4. **Esta sessão, Edit em `docs/retrospectivas/README.md`:** patch curto (1 linha trocada) terminou em "[`RETRO-0".

Pattern: **truncamento não é determinístico em tamanho** (1 a 50 linhas), e a tool **reporta sucesso** mesmo quando o write fica incompleto.

### B. Mount FUSE do sandbox Linux mostra disco defasado

Após o FE-14 mergear em develop (PR #80) e o humano rodar `git restore` no terminal Windows:
- `git status` no terminal Windows: **clean**.
- `git status` no sandbox Linux do Cowork: **7 arquivos do front como modified** com conteúdo truncado.
- `git show HEAD:frontend/.../ModalComprovante.tsx | wc -c`: 596 bytes (correto).
- `wc -c frontend/.../ModalComprovante.tsx` no sandbox: 4166 bytes (inclui trailing whitespace gigante que não existe no disco real).

Mount FUSE serve uma view cacheada/desatualizada do disco em alguns casos. Read tool e bash `cat` veem coisas diferentes do disco real.

### C. Mount FUSE não permite delete

`rm` e `os.unlink()` falham com `Operation not permitted` em arquivos criados via bash dentro do mount. Sobraram `.tmp` e `DISPATCH-FIX-001` na pasta do planner; só o humano conseguiu apagar (via Explorer/PowerShell no Windows).

## Workarounds testados

| Operação | Tool problemática | Workaround confiável |
|---|---|---|
| Escrever arquivo > 20 linhas | `Write`, `Edit` (Cowork) | `cat > arquivo << 'EOF' ... EOF` via bash (`mcp__workspace__bash`) |
| Confirmar conteúdo escrito | Read tool | `wc -l arquivo && tail -3 arquivo` via bash + comparação com expectativa |
| Conferir estado real do repo | `git status` no sandbox | `git status` no terminal Windows do humano |
| Apagar lixo no mount do planner | `rm` no sandbox | Humano apaga via Explorer/PowerShell |

## Decisão / abordagem

Esta task entrega **três saídas defensivas** + **um bug report rascunho**. Sem código novo (sem script de detecção — decisão da discovery 2026-05-30, item 1).

1. **Diagnóstico em `docs/aprendizado/`** — referência permanente com sintomas, evidência e workarounds.
2. **Regra de escrita defensiva em `docs/roles/planner.md`** — checklist mental que toda sessão de planning segue.
3. **Correção de `CLAUDE.md`** — substituir as menções a "sync gremlin do OneDrive" pelo diagnóstico correto + apontar pra `planner.md`.
4. **Bug report rascunho** anexado ao aprendizado, pronto pra humano colar no feedback do Cowork (thumbs down + texto).

## Escopo / arquivos

### Criar

- **`docs/aprendizado/cowork-write-truncamento.md`** — novo. Estrutura padrão dos aprendizados (contexto, resumo destilado, pontos-chave, pra aprofundar) + duas seções extras:
  - **"Evidência"** — os 3 sintomas A/B/C com exemplos diretos da sessão 2026-05-30 (paths exatos, números, citações de output).
  - **"Bug report rascunho"** — apêndice em formato pronto pra colar (200-300 palavras, em inglês porque o Cowork é internacional). Cobre: contexto (Cowork+Windows+mount FUSE Linux), os 3 sintomas com evidência mínima, workarounds adotados, perguntas pro time.

### Modificar

- **`docs/roles/planner.md`** — adicionar seção **"Escrita defensiva de arquivos (workaround Cowork)"** com 3 regras:
  1. **Writes em arquivos importantes** (CLAUDE.md, STATE.md, docs em `docs/architecture/`, `docs/decisions/`, `docs/plans/`, `docs/sprints/<NN>/`, `docs/retrospectivas/`) usar **sempre** `cat > arquivo << 'EOF' ... EOF` via bash. Edit/Write tool só pra **mudanças mínimas em arquivos pequenos** (< 20 linhas no patch + arquivo < 100 linhas total).
  2. **Após qualquer write em arquivo importante**, verificar com `wc -l arquivo && tail -3 arquivo` (no mesmo bash call do write quando possível). Se o tail não bater com o esperado, refazer via bash heredoc. Não passar adiante sem verificar.
  3. **Fonte da verdade do estado do repo é o terminal Windows do humano.** `git status` no sandbox pode mostrar arquivos como "modified" que estão íntegros no disco — verificar via terminal Windows antes de qualquer `git restore`/`git checkout -- <path>`.
- **`CLAUDE.md`** (raiz) — substituir as menções a "sync gremlin do OneDrive" pela referência ao aprendizado novo + apontar pra `docs/roles/planner.md` §"Escrita defensiva". Locais a editar:
  - §Worktrees git — frase "writes em arquivos novos podem se perder quando o implementador trocar de branch (sync gremlin do OneDrive)" — reescrever com causa correta.
  - Qualquer outra menção a "OneDrive" no contexto de arquivos sumindo.
- **`docs/aprendizado/README.md`** — adicionar entrada `cowork-write-truncamento.md` no índice, categoria "ferramental / ambiente" (criar categoria se não existir).

### Não tocar

- Código de produto (`financas_bot_telegram/`, `frontend/`, `infra/`).
- Outros roles (`docs/roles/backend.md`, `frontend.md`, `reviewer.md`) — implementadores raramente escrevem arquivos longos via Write/Edit, foco onde dói.
- Cowork tools em si — sem controle.

## Critérios de aceitação

- [ ] `docs/aprendizado/cowork-write-truncamento.md` escrito com seções: contexto, resumo destilado, pontos-chave, evidência (A/B/C), workarounds, bug report rascunho, pra aprofundar.
- [ ] `docs/aprendizado/README.md` indexa o arquivo novo.
- [ ] `docs/roles/planner.md` ganha a seção "Escrita defensiva de arquivos (workaround Cowork)" com as 3 regras.
- [ ] `CLAUDE.md` corrigido — zero menções a "sync gremlin OneDrive"; substituídas pela referência ao aprendizado + planner.md.
- [ ] Bug report no aprendizado é **autocontido** — humano consegue colar sem reescrever.
- [ ] Status report `docs/sprints/02b-kaizen-workflow/status/WF-01.md` com frontmatter válido conforme `docs/status/_TEMPLATE.md`.
- [ ] Branch `feature/wf-01-investigar-truncamento-cowork` saiu de `develop` (fluxo novo do CLAUDE.md §Worktrees git).
- [ ] Território só `docs/` (sem código de produto).
- [ ] Humano revisou o diff do commit (`git show HEAD`) e aprovou antes de `git push`.

## Fora de escopo (explicitamente)

- **Script automatizado de verificação de truncamento** — decisão da discovery 2026-05-30 #1: só regra escrita.
- **Atualizar `docs/roles/backend.md` ou `frontend.md`** — implementadores não sofrem desse bug (writes deles são de código, raramente longos via Write/Edit; e estão fora do worktree do planner).
- **Reportar bug pro time do Cowork pelos canais oficiais** — humano decide quando enviar o rascunho.
- **Reescrever os 7 arquivos do front no sandbox pra resolver a visão defasada do mount** — não tem como; o disco real está clean (confirmado).
- **Migrar repo pra fora do OneDrive** — não está no OneDrive (descoberta da seção 8a da RETRO-02).
- **Forçar refresh do mount FUSE do Cowork** — sem mecanismo conhecido; tolerar até bug ser resolvido pelo time.

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Próxima sessão do planner ignora a regra | Média | Médio (truncamento novo) | Boot sequence do planner inclui `docs/roles/planner.md` — agente lê antes de qualquer ação. CLAUDE.md aponta pra planner.md. |
| Cowork resolve o bug, regra fica obsoleta | Baixa | Baixo (regra continua sendo boa prática) | Sem ação preventiva; quando confirmar fix, revisar planner.md como item de retro. |
| Heurística "writes ≥ 20 linhas" mal calibrada | Média | Baixo (algumas escritas pequenas viram bash quando não precisava) | Aceitar fricção menor; ajustar threshold em retro se ficar incômodo. |
| Bug report virar baixa prioridade no Cowork | Alta | Baixo (mitigação interna já funciona) | Mandar de qualquer jeito; não bloquear a sprint nisso. |
| Visão defasada do mount FUSE causa confusão futura | Alta | Médio (decisão errada por achar que repo está sujo) | Regra 3 (terminal Windows como fonte da verdade) cobre. |

## Coordenação

- **Não toca código** — zero conflito com back/front.
- **Pode rodar em paralelo** com qualquer task (kaizen ou outra) — mexe só em docs.
- **Após merge:** outras tasks da sprint kaizen (WF-02 em diante) já podem aplicar a regra de escrita defensiva.

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report com frontmatter válido, revisão direta do humano (Reviewer dispensado nesta sprint) (foco em: bug report está autocontido? regras do planner.md são acionáveis? CLAUDE.md ficou consistente?). **Commit direto em `develop`** seguindo a regra do planner; **sem push até humano revisar `git show HEAD`** e confirmar.

## Referências

- RETRO-02 §4, §8a, ação #1 (`docs/retrospectivas/RETRO-02-canal-whatsapp.md`).
- Sessão de planning 2026-05-30 — 4 truncamentos observados + descoberta da visão defasada do mount.
- `docs/aprendizado/_TEMPLATE.md` (se existir) ou padrão dos aprendizados existentes (ex.: `structured-outputs.md`, `cookies-samesite.md`).
- `docs/roles/planner.md` atual — onde a seção nova entra.
- CLAUDE.md atual §Worktrees git + §Acesso ao git pelo Cowork.
