# Fim de linha no git: CRLF vs LF, .gitattributes e renormalização

## Contexto da dúvida

Veio da revisão do DEP-02: o `terraform plan` mostrava drift recorrente de `user_data` da EC2 (update in-place) causado por fim de linha. Já tínhamos visto antes o "diff-noise" (toda linha do arquivo aparecendo como alterada). A pergunta foi entender a fundo o problema e a cura (`.gitattributes`).

## Resumo destilado

### O problema
Windows marca fim de linha com **CRLF** (`\r\n`), Unix/Mac com **LF** (`\n`) — bytes invisíveis no editor, mas reais no arquivo. O git tem `core.autocrlf` (config **por máquina**) que decide se converte. Como cada dev pode ter config diferente e o deploy é Linux, o mesmo arquivo **oscila** entre CRLF e LF conforme quem commita. Dois sintomas:

1. **Diff-noise:** quando o fim de linha muda, **toda linha** aparece como alterada (assinatura "N add / N del" em arquivo não mudado). Polui review, blame, histórico.
2. **Drift no Terraform:** `user_data` é montado do conteúdo de arquivo **byte a byte**. CRLF muda o hash → plan mostra `update in-place` que reaparece sempre. Inofensivo (metadata-only), mas mascara mudanças reais.

### A solução: `.gitattributes`
Arquivo **commitado no repo** que declara, por padrão de caminho, como o git trata fim de linha — **independente do `core.autocrlf` local**. Tira a inconsistência: passa a valer igual pra todos.

- Regra usada: `* text=auto eol=lf` → força **LF no working tree** pra todo arquivo de texto. Correto aqui porque o alvo é Linux e o working-tree em LF mantém o hash do `user_data` estável. Editores modernos lidam com LF no Windows numa boa.
- Binários marcados como `binary` (git nunca converte): `*.png`, `*.jar`, `*.p12`, etc.
- `*.bat`/`*.cmd` → `eol=crlf` (se existirem; Windows precisa).

### O pulo do gato: renormalizar
`.gitattributes` **só vale dali pra frente** — não conserta o que já está commitado. Pra limpar o CRLF existente:

```
git add .gitattributes
git add --renormalize .   # reescreve os blobs com o EOL normalizado
git commit -m "fix: normaliza fim de linha via .gitattributes"
```

Gera um diff grande (esperado — só fim de linha) → fazer em **commit único e dedicado**, com o repo "quieto" (sem feature branches abertas, pra evitar conflito de EOL no merge).

## Pontos-chave

- **CRLF** (Windows) ≠ **LF** (Unix). Misturar = diff-noise + drift de hash (Terraform `user_data`).
- `core.autocrlf` é **por máquina** → inconsistente entre devs. `.gitattributes` é **do repo** → consistente pra todos.
- `* text=auto eol=lf` força LF no working tree (certo quando o alvo é Linux e ferramentas leem bytes).
- Marcar binários como `binary` pra git nunca convertê-los.
- `.gitattributes` não age retroativamente → **`git add --renormalize .`** + commit único pra limpar o legado.
- Fazer com o repo quieto (sem branches abertas) pra evitar conflito de EOL.

## Pra aprofundar

- `git ls-files --eol` — inspeciona o EOL no índice (`i/lf`) e no working tree (`w/`).
- Diferença entre `text=auto` (normaliza no repo, EOL nativo no checkout) e `text=auto eol=lf` (força LF também no working tree).
- Relação com `git-reset-e-area-de-staging.md` (a mesma área de staging onde o renormalize age).
- `.editorconfig` como camada complementar (configura o editor, não o git).
