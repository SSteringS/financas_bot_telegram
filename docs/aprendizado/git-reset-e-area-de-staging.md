# Git — área de staging, `git reset` e o lock do index

## Contexto da dúvida

Durante a fase de revisão da Fase 3 do front, eu (Claude de planejamento) fiz um commit local ruim na `develop` por causa de um atalho perigoso (escrever direto em `.git/HEAD` pra trocar de branch sem checkout). Pra desfazer sem perder arquivos em disco, foi necessário rodar `git reset --mixed <sha>` precedido de um `Remove-Item .git\index.lock`. O humano pediu pra explicar exatamente o que cada comando faz.

## Resumo destilado

### As três "camadas" do git

Toda operação do git mexe em alguma combinação dessas três:

1. **HEAD / branch ref** — o ponteiro que diz qual commit é o "atual" da branch.
2. **Index** (a.k.a. staging area) — arquivo binário em `.git/index` que registra o que vai entrar no próximo commit.
3. **Working tree** — os arquivos físicos que você edita em disco.

Em condições normais, depois de um commit limpo, os três batem. Mas eles podem desencontrar — e é exatamente nesse desencontro que `git reset` opera.

### Lock files do git — não só o index

Vários arquivos críticos dentro de `.git/` ganham um `.lock` adjacente durante operações que precisam escrever neles:

- `.git/index` ↔ `.git/index.lock` — protege o staging area (afetado por `add`, `commit`, `reset`, `checkout`).
- `.git/HEAD` ↔ `.git/HEAD.lock` — protege o ponteiro de "branch atual" (afetado por `checkout`, `reset`, `commit`).
- `.git/refs/heads/<branch>` ↔ `.git/refs/heads/<branch>.lock` — protege o ponteiro de cada branch (afetado quando o ref da branch é atualizado: `commit`, `reset`, `merge` etc.).
- Outros: `.git/packed-refs.lock`, `.git/ORIG_HEAD.lock`, etc.

O padrão é sempre o mesmo: o git cria o `.lock` no começo da operação, faz o trabalho, e renomeia o `.lock` pra substituir o original (rename atômico) ou apaga o lock se deu erro. É o mecanismo padrão pra evitar dois processos escreverem no mesmo arquivo ao mesmo tempo.

Quando um `git` morre no meio (crash, processo morto, problema de permissão de filesystem, sandbox/container que perde acesso), o lock pode sobrar. Comandos seguintes que precisam daquele arquivo batem em algo do tipo:

```
fatal: Unable to create '.git/index.lock': File exists.
error: cannot lock ref 'HEAD': Unable to create '.git/HEAD.lock': File exists.
```

**Solução cirúrgica:** apagar o lock específico mencionado no erro.

```powershell
Remove-Item -Force .git\HEAD.lock
# ou
rm -f .git/index.lock
```

**Solução em varredura** (quando você suspeita de múltiplos locks zumbis):

```powershell
# PowerShell
Get-ChildItem .git -Recurse -Filter "*.lock" | Remove-Item -Force
```

```bash
# Bash
find .git -name "*.lock" -delete
```

**Risco:** se houver um `git` rodando de verdade no momento, apagar o lock pode corromper a operação em andamento. Use só quando tem certeza que nenhuma operação está rodando (típico: depois de um crash, ou em ambiente single-user sem git automation rodando).

### Os três modos de `git reset <commit>`

| Modo | Move HEAD | Reseta index | Mexe no working tree |
|---|---|---|---|
| `--soft` | sim | não | não |
| `--mixed` (padrão) | sim | sim | **não** |
| `--hard` | sim | sim | **sim — destrutivo** |

- **`--soft`** — "volta o ponteiro mas mantém tudo staged, pronto pra re-commitar". Uso clássico: refazer só a mensagem do último commit, ou agrupar uma série de commits num só.
- **`--mixed`** — "volta o ponteiro, desfaz o staging, mas não toca em disco". O modo mais "seguro" pra desfazer um commit local sem perder o trabalho — tudo vira "uncommitted changes" pra você decidir o que fazer.
- **`--hard`** — "volta o ponteiro E sobrescreve o disco pra bater com o commit alvo". Apaga modificações não commitadas. Use só quando tem certeza absoluta.

### Reset desfaz, mas não destrói commits

Mesmo depois de `git reset`, o commit antigo **continua no `.git/objects/`** por uns 30-90 dias (até o garbage collector limpar). O que muda é só pra onde a branch aponta — o objeto em si fica órfão, mas acessível pelo SHA.

Pra resgatar:
- `git reflog` mostra todo movimento do HEAD com SHA — dá pra encontrar o commit "perdido"
- `git reset --hard <sha-do-reflog>` ou `git cherry-pick <sha>` traz de volta

Isso transforma `git reset` numa operação reversível na prática. Pode usar com tranquilidade.

## Pontos-chave

- Git tem 3 camadas: **HEAD**, **index** (staging), **working tree**. `reset` mexe num subset.
- **`--soft`** mexe só no ponteiro. **`--mixed`** mexe no ponteiro + index. **`--hard`** mexe nos três (perigoso).
- O index é um arquivo (`.git/index`) protegido por um lock (`.git/index.lock`). Lock stale trava o git — apagar é seguro se nenhuma operação estiver rodando.
- Commits "perdidos" por `reset` continuam recuperáveis via `git reflog` por semanas.
- Regra mental ao "desfazer commit local sem perder arquivos": `git reset --mixed <ultimo-commit-bom>`.

## Pra aprofundar

- `git reflog` — registro local de tudo que o HEAD percorreu, inclusive operações que não aparecem no `log`
- `git restore` (git ≥ 2.23) — separa as funções de `git checkout` em duas: `restore` pra mexer no working tree/index, `switch` pra trocar branch. Mais previsível que o `checkout` clássico.
- `git stash` — alternativa pra "guardar" mudanças temporariamente sem commitar nem perder
- Diferença entre objetos do git: blob (conteúdo), tree (diretório), commit (snapshot + metadata), tag (ref imutável). O index é uma forma intermediária — registra blob SHAs sem ainda formar uma tree.
- Garbage collection (`git gc`) — quando o git limpa objetos órfãos. Por padrão só após 90 dias e várias condições.
