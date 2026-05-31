---
tipo: piloto
categoria: ferramentas
tags: [vscode, claude-code, migração, cowork]
origem: sessão 2026-05-30 piloto migração Cowork → VS Code
aplica_em: todas as sessões de planejamento
confianca: alta
ultimo_review: 2026-05-30
---

# Piloto VS Code + Claude Code — Resultados e Comparação com Cowork

## Contexto da dúvida

Em 2026-05-30, o projeto financas_bot_telegram iniciou migração das sessões não-implementadoras
(Planner, Engenheiro de IA, Arquiteto, Reviewer) de Cowork desktop para VS Code + Claude Code
(extensão nativa). O piloto foi conduzido pela sessão do Engenheiro de IA.

## Problema original com Cowork

O Cowork tinha 3 limitações documentadas que bloqueavam decisões:

1. **Truncamento silencioso de Write/Edit** — arquivos com mais de ~80 linhas eram silenciosamente
   cortados. Workaround obrigatório: `cat > arquivo << 'EOF'` + `wc -l && tail` pra verificar.
2. **Impossibilidade de escrever em `.claude/`** — bloqueava subagents nativos, skills canônicas
   Anthropic (`.claude/skills/<name>/SKILL.md`), e hooks.
3. **Mount FUSE com view defasada** — `git status` podia mentir, `rm` falhar, `git rm --cached`
   dava erro em paths Windows.

## Resultados do piloto

### Teste 1 — Renderização Markdown

**Resultado:** Sim (nativo).
Markdown é renderizado no painel de chat do VS Code com headers, tabelas, code blocks e links
clicáveis. Qualidade adequada para sessões longas de planejamento — sem degradação vs Cowork.

### Teste 2 — Escrita sem truncamento

**Resultado:** Sim (confirmado por wc -l + tail).
Write tool escreve arquivo de 150+ linhas sem truncar. Edit tool também funciona sem problema.
Não é necessário o workaround `cat > EOF`. A regra de escrita defensiva pode ser removida
dos roles quando a migração for formalizada via ADR.

### Teste 3 — `.claude/` aceita escrita

**Resultado:** Sim.
Write tool consegue criar `.claude/skills/teste-piloto/SKILL.md` com estrutura canônica Anthropic.
O que isso destrava:

- Skills canônicas Anthropic no projeto (opções B/C/D da discussão da ADR 0015 implícita)
- Subagents nativos em `.claude/agents/<papel>.md` (ADR 0016 planejada)
- Hooks em `.claude/settings.json`

### Teste 4 — Múltiplas sessões em paralelo

**Resultado:** Sim (por design do VS Code).
VS Code suporta múltiplos terminais (Ctrl+Shift+backtick ou painel inferior). Cada terminal roda
`claude` independente com seu próprio contexto de conversa. Worktree planner pode ter N sessões
simultâneas sem interferência entre contextos.

Consequência: todos os roles (planner, engenheiro de IA, arquiteto, reviewer) podem rodar
simultaneamente em abas do mesmo VS Code, apontando pro mesmo worktree planner. Elimina a
serialização forçada que existia no Cowork (uma sessão ativa por vez).

### Teste 5 — Skills proprietárias do Cowork

**Resultado:** Não há equivalente direto — mas raramente é bloqueante.
Cowork tem skills nativas pra docx, pdf, xlsx, pptx (leitura de arquivos Office/PDF).
Claude Code não tem skill equivalente nativa, mas:

- **PDF:** Read tool lê PDFs diretamente (até 20 páginas por chamada) — cobre boa parte dos casos
- **DOCX/XLSX/PPTX:** sem suporte nativo — precisaria de Bash + pandoc/libreoffice se crítico

Para o trabalho de planejamento e meta-arquitetura, isso não é bloqueante: toda a documentação
do projeto é Markdown. Se o humano eventualmente precisar processar Office docs, o Bash + ferramentas
externas cobrem via shell.

### Teste 6 — MCPs do Cowork

**Resultado:** Não disponível (esperado) — mas coberto pelo toolset nativo.

MCPs ausentes e seus substitutos:

| MCP Cowork | Substituto Claude Code |
|---|---|
| `mcp__cowork__present_files` | Read + Glob |
| `mcp__visualize__*` | sem equivalente visual; mermaid em Markdown compensa |
| `mcp__scheduled-tasks__*` | CronCreate (skill `schedule` nativa) |
| `mcp__cowork__create_artifact` | Write em arquivo + commit |

Para o Engenheiro de IA, Read/Write/Edit/Bash/Grep/Glob/WebFetch/WebSearch cobrem 100% das
necessidades confirmadas na sessão anterior. Nenhuma lacuna bloqueante para o papel.

## Pontos-chave

- VS Code + Claude Code elimina as 3 limitações críticas do Cowork sem workarounds adicionais
- Skills canônicas Anthropic (`.claude/skills/`) ficam desbloqueadas — reabre opções B/C/D
- Subagents nativos (`.claude/agents/`) ficam desbloqueados — ADR 0016 pode avançar
- Multi-sessão nativa por terminais VS Code — todos os roles podem rodar em paralelo no mesmo IDE
- Única lacuna real: leitura de Office (docx/xlsx/pptx) — não crítica para o workflow atual
- TodoWrite substitui TaskCreate/TaskUpdate do Cowork para rastrear progresso intra-sessão

## Implicações pro projeto

1. ADR de migração (0016 ou 0018) vai formalizar VS Code + Claude Code como ambiente padrão
2. **Escrita defensiva** (`cat > EOF` + `wc -l && tail`) pode ser removida dos roles
3. **Skills canônicas** (opção B/C/D da ADR 0015) reabrem para decisão
4. **Subagent Reviewer** (ADR 0016 planejada — Reviewer-sub) pode ser materializado em
   `.claude/agents/reviewer.md`

## Pra aprofundar

- `docs/aprendizado/cowork-write-truncamento.md` — diagnóstico do bug Cowork (contexto histórico)
- `docs/decisions/0015-taxonomia-roles-skills-workflows.md` — taxonomia roles × skills
- `docs/roles/engenheiro-de-ia.md` — role que conduziu o piloto
- `docs/aprendizado/curso-anthropic-agent-skills.md` — substrato conceitual das skills
