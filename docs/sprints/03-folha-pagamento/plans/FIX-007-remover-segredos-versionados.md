---
task: FIX-007
titulo: "Remover segredos versionados do estado atual do repositório (repo público)"
sprint: 03-folha-pagamento
data_planejamento: 2026-08-09
branch_alvo: fix/007-remover-segredos-versionados
prioridade: alta
esforco: baixo
territorio: back
estado: pronto-pra-execucao
depende_de: []
bloqueia: []
skills_dispatched: []
integration_branch: null                   # FIX sai direto de develop
fluxos_qa: []
---

# FIX-007 — Remover segredos versionados do estado atual do repositório

## Intake

- **Origem:** varredura de histórico executada em 2026-08-09, durante o diagnóstico do incidente de bot mudo (token do Telegram revogado). O humano confirmou na mesma sessão que **o repositório é público**.
- **Por quê agora:** há segredos em texto claro no estado atual de `main` e `develop`, indexáveis por qualquer scanner. Nenhum deles compromete produção hoje (ver §Avaliação de impacto), mas continuam sendo coletados enquanto estiverem lá.
- **Esforço:** baixo (~1h). Edições textuais + um ajuste em script de provisionamento.
- **Riscos resumidos:** baixos, com **uma exceção** — a mudança em `bootstrap.sh` só é exercitada em recreate da EC2, ou seja, não é testável por CI. Ver §Riscos.

---

## Avaliação de impacto (o que este FIX **não** é)

Registro explícito para evitar que a urgência seja mal calibrada por quem ler depois:

| Segredo | Exposto em | Compromete prod hoje? | Verificação |
|---|---|---|---|
| Token Telegram gen-1 | `README.md:128` | **Não** — token morto. Rotacionado em 2026-08-09 (gen-2 → gen-3) | `getMe` com o token do README falharia; prod usa outro |
| `admin_api_key` `UJ…k=` | `http/http-client.env.json:4`, `runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md:79,99` | **Não** — é a chave de **dev**. O humano conferiu `finbot-prod-secrets` em 2026-08-09: a de prod começa com `T8` | Comparação de prefixo feita pelo humano |
| `keystore_password` (senha literal do keystore) | `infra/provision/bootstrap.sh:146` + 18 ocorrências em `docs/**` | **Não diretamente** — é a senha de um PKCS12 self-signed que, após a migração do webhook para a Caddy, só termina TLS no loopback. Explorar exige acesso prévio ao host | Leitura de `Caddyfile.tftpl` + `application-prod.properties:7-10` |
| Senha MySQL `Sa…23` | `runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md:22` | **Não** — é a senha do MySQL **local** de desenvolvimento, não do RDS | `application-prod.properties:17` usa `${db_password}` do Secrets Manager |

**Conclusão:** isto é faxina de higiene, não resposta a incidente. Executar com cuidado normal, sem pressa de emergência.

**Não verificado (assumir o pior):** há quanto tempo o repositório é público. Se sempre foi, todo valor já commitado deve ser considerado coletado por bots. É por isso que o caminho é **rotacionar**, não reescrever histórico.

---

## Decisão / abordagem

**Limpar o estado atual de `main`/`develop`. Não reescrever histórico.**

Reescrever histórico (`git filter-repo`, BFG) em repositório público **não desfaz o vazamento**: forks, clones locais, caches do GitHub e scanners de terceiros já possuem os valores. O que resolve é o valor deixar de ser válido. Limpar o estado atual tem valor real e distinto — interrompe a **exposição contínua** e tira o segredo da primeira página do repo — mas não é remediação.

**Ordem correta, registrada para o futuro:** rotacionar → limpar estado atual → (opcionalmente) purgar histórico. Este FIX cobre só o passo 2. O passo 1 é operacional do humano (ver §Fora de escopo).

---

## Escopo / arquivos

### Modificar

- `README.md:128` — substituir o token literal por `<SEU_TOKEN_AQUI>`, alinhando com o padrão já usado na linha 65 do mesmo arquivo.
- `docs/runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md` — linha 22 (senha MySQL local), linhas 79 e 99 (`X-Admin-Key`). Substituir por placeholders descritivos (`<sua-senha-mysql-local>`, `<sua-admin-api-key>`), mantendo o runbook utilizável.
- `financas_bot_telegram/http/BE-16-testes.http:30` — substituir o token de sessão por placeholder.
- **18 ocorrências da senha literal do keystore em `docs/**`** — arquivos listados em §Referências. Substituir por `<keystore-password>`. **Não reescrever a narrativa histórica dos status reports**; só mascarar o valor.
- `financas_bot_telegram/infra/provision/bootstrap.sh:146` — trocar o `-passout pass:<senha literal>` por leitura de `keystore_password` a partir de `finbot-prod-secrets`. Falhar alto e claro (`exit 1` com mensagem) se a chave não vier.
- `.gitignore` (raiz) — adicionar `http-client.env.json`.

### Criar

- `financas_bot_telegram/http/http-client.env.json.example` — mesma estrutura, valores como placeholders. Seguir o padrão já estabelecido por `frontend/.env.example`.

### Remover do tracking

- `financas_bot_telegram/http/http-client.env.json` — via `git rm --cached` (manter o arquivo local do humano intacto).

### Não tocar

- Histórico git — decisão registrada acima.
- Valores em `finbot-prod-secrets` — rotação é operação do humano.
- `infra/prod.tfvars` (Account ID, `my_ip`) — não são segredos; item separado de menor prioridade.
- Qualquer código de aplicação. Este FIX não altera comportamento em runtime.

---

## Verificação obrigatória antes de implementar

**`bootstrap.sh` só pode ler do Secrets Manager se o instance profile tiver permissão.** Antes de escrever a mudança, confirmar em `financas_bot_telegram/infra/iam*.tf` que a role da EC2 tem `secretsmanager:GetSecretValue` sobre `finbot-prod-secrets`.

- **Se tiver:** implementar como planejado.
- **Se não tiver:** **parar e reportar**. Não adicionar permissão IAM por conta própria — isso amplia o escopo para mudança de infraestrutura com blast radius próprio, e exige decisão do planner.

Registrar no status report qual dos dois casos ocorreu, com o arquivo e linha que sustentam a conclusão.

---

## Critérios de aceitação

- [ ] Busca pela senha literal do keystore retorna **zero** ocorrências na árvore de trabalho. Comando reprodutível (recupera o literal do histórico, sem reintroduzi-lo no repo):
      `git grep -n "$(git show <sha-anterior>:financas_bot_telegram/infra/provision/bootstrap.sh | sed -n 's/.*-passout pass://p')"` — saída registrada no status report.
- [ ] `git grep -nE "[0-9]{8,10}:AA[A-Za-z0-9_-]{30,}"` retorna **zero** ocorrências.
- [ ] `git grep -n "UJ"` não retorna a `admin_key` em `http-client.env.json` nem no runbook (conferir manualmente os 3 pontos citados).
- [ ] `git ls-files financas_bot_telegram/http/http-client.env.json` retorna vazio (arquivo destrackeado).
- [ ] `financas_bot_telegram/http/http-client.env.json.example` existe e é válido como JSON.
- [ ] `.gitignore` cobre `http-client.env.json`.
- [ ] `bootstrap.sh` não contém senha literal; lê de `finbot-prod-secrets` e falha explicitamente se ausente.
- [ ] `bash -n financas_bot_telegram/infra/provision/bootstrap.sh` passa (checagem de sintaxe, sem executar).
- [ ] O runbook `ROTEIRO-INTEGRACAO-FRONT-BACK.md` continua **seguível** após a substituição — um leitor entende o que precisa preencher.
- [ ] `mvn test` verde (nenhuma mudança de código esperada, mas confirmar ausência de regressão).
- [ ] Branch `fix/007-remover-segredos-versionados` saiu de `develop`.
- [ ] Status report em `docs/sprints/03-folha-pagamento/status/FIX-007-remover-segredos-versionados.md` com frontmatter válido, incluindo o resultado da verificação IAM.

---

## Fora de escopo

- **Rotação dos segredos** (`admin_api_key` de dev, `keystore_password`, senha MySQL local) — operação do humano no BotFather / Secrets Manager / MySQL. O FIX limpa o repositório; não troca valores.
- **Purga de histórico** (`git filter-repo` / BFG) — decisão separada, com custo alto (quebra forks e SHAs) e benefício baixo num repo já público.
- **Fechar a porta 8443 no security group** — **deliberadamente adiado.** O webhook foi repontado para a Caddy hoje (2026-08-09); fechar a 8443 antes de confirmar estabilidade elimina o caminho de rollback. Revisitar após alguns dias de tráfego limpo. Registrado como item separado.
- **Gate de secret scanning no CI** — vira `CI-002`, escopo próprio.
- Mover `telegram.allowed-user-ids` para banco — já em `PENDENCIAS-TECNICAS.md`.

---

## Riscos & mitigações

| Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|
| `bootstrap.sh` quebra e só se descobre no próximo recreate da EC2 | Média | **Alto** — provisionamento falha, keystore não é gerado | `bash -n` obrigatório; falha explícita com mensagem em vez de senha vazia silenciosa; Reviewer deve ler a lógica linha a linha, já que CI não cobre |
| Instance profile sem permissão de Secrets Manager | Média | Médio | Verificação obrigatória **antes** de implementar; parar e reportar se ausente |
| Substituição em `docs/` destrói o sentido histórico dos status reports | Baixa | Baixo | Instrução explícita: mascarar valor, preservar narrativa |
| Runbook fica inutilizável após placeholders | Baixa | Médio | Critério de aceitação exige que continue seguível |
| Alguém interpreta este FIX como "vazamento resolvido" | **Média** | **Alto** | §Avaliação de impacto e §Decisão dizem explicitamente que limpar estado atual **não** é remediação; rotação é o que resolve |

---

## Coordenação

- **Paralelo:** não conflita com nada em andamento. Territórios: `README.md`, `docs/**`, `financas_bot_telegram/http/`, `financas_bot_telegram/infra/`, `.gitignore`.
- **Atenção pro Reviewer:**
  1. `bootstrap.sh` é o único ponto executável tocado e **não tem cobertura de CI**. Ler a lógica de leitura do secret e o caminho de falha manualmente. Não aceitar "passou no `bash -n`" como evidência de correção.
  2. Confirmar que a verificação IAM foi feita **de fato**, com arquivo e linha citados — não aceitar afirmação sem referência.
  3. Rodar os `git grep` dos critérios de aceitação de forma independente, sem confiar no relato do implementador.
  4. Verificar que nenhum segredo novo entrou no `.example`.
- **Após merge:** lembrar o humano da rotação (passo 1 da ordem correta), que este FIX não executa.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, **revisão do Reviewer** (ADR 0005).

`fluxos_qa: []` → **sem gate QA**, com justificativa: a mudança não altera comportamento de aplicação nem cria superfície nova; são substituições textuais mais um script de provisionamento que a suíte E2E não exercita (só roda em recreate de EC2). O risco real está concentrado em `bootstrap.sh`, e o controle proporcional é revisão humana atenta — não teste automatizado, que não existe para essa camada. PR direto para `develop`.

---

## Referências

- Varredura de segredos, 2026-08-09 — inventário completo de 408 commits, todas as refs.
- Ocorrências da senha literal do keystore em docs: `PENDENCIAS-TECNICAS.md:332`; `architecture/estado-atual-dev.md:202,275`; `runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md:208`; `sprints/01-mvp/plans/FIX-keystore-password-secret.md:3,22,39,59`; `sprints/01-mvp/status/FIX-keystore-password-secret.md:12,19,23,42`; `sprints/01-mvp/status/DEP-07.md:86,98`; `sprints/01-mvp/status/_RESUMO-overnight-back-2.md:30,32,63`; `sprints/01-mvp/status/_RESUMO-overnight-deploy.md:50`; `sprints/01-mvp/avaliacoes/backend-polish-evo07.md:45`.
- `docs/sprints/01-mvp/plans/FIX-keystore-password-secret.md` — o FIX que moveu a senha para o Secrets Manager **e documentou o valor em claro**, anulando a própria mitigação. É a origem da maior parte das ocorrências e a evidência que motiva o `CI-002`.
