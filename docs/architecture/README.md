# Architecture — índice e convenções

Artefatos **duráveis** sobre como o sistema é construído. Cada arquivo tem propósito único e critério explícito de quando atualizar.

## Arquivos e propósitos

| Arquivo | O que contém | Quando atualizar |
|---|---|---|
| `especificacao-tecnica.md` | Visão sistêmica: domínio, hexagonal, stack, pontos de integração, contratos de API | Novo canal, novo conceito de domínio, mudança estrutural |
| `estado-atual-dev.md` | Snapshot da realidade em develop: pacotes, features implementadas, o que falta | Quando planner informa que task foi **mergeada em develop** → arquiteto atualiza como subagente |
| `estado-atual-prod.md` | Snapshot da realidade em produção: o que está deployado e funcionando | Quando planner informa que task foi **deployed em prod** → arquiteto atualiza como subagente |
| `fluxo-autenticacao.md` | Diagrama de sequência da autenticação | Quando o fluxo de auth mudar |
| `integracoes/whatsapp.md` | Design duradouro do canal WhatsApp (adapter in/out, portas, contratos) | Quando a integração WhatsApp mudar estruturalmente |

## O que NÃO fica aqui

- **Specs efêmeras do arquiteto** (input pro planner de tasks) → `docs/sprints/<NN>/specs/`
- **ADRs** (decisões arquiteturais canônicas) → `docs/decisions/`
- **Exploração de UI/UX** → `docs/frontend/design-proposals/`
- **Aprendizados e conceitos** → `docs/aprendizado/`

## Critério de distinção

> "Isso ainda vai ser verdade daqui a 6 meses?" → se sim, fica aqui. Se não, é efêmero e vai para sprints/ ou decisions/.

## Como o arquiteto usa esta pasta

**Em sessão de refinamento:**
1. Lê `especificacao-tecnica.md` + `estado-atual-dev.md` para entender o ponto de partida.
2. Escreve a spec efêmera em `docs/sprints/<NN>/specs/<slug>.md`.
3. Após aprovação da spec: atualiza `especificacao-tecnica.md` (se algo muda na visão sistêmica).

**Quando acionado pelo planner (subagente):**
- Task mergeada em develop → atualiza `estado-atual-dev.md`
- Task deployed em prod → atualiza `estado-atual-prod.md`
