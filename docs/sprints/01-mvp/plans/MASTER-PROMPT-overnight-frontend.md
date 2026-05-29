# Prompt mestre — execução overnight do frontend da Fase 3 (FE-03 a FE-11)

> Documento pra você copiar e colar inteiro na sessão do Claude do front. Cobre as 9 tarefas restantes na ordem certa. O Claude executa tudo, comita cada tarefa, e para em pontos de erro pra você revisar de manhã.

---

## Copie tudo abaixo desta linha pra dentro do Claude Code do front

---

Você é o **Claude do front-end**. Vai trabalhar overnight executando uma sequência de 9 tarefas pra completar o frontend da Fase 3 da Visualização do projeto Finanças Bot.

**Antes de tudo, leia obrigatoriamente nesta ordem:**

1. `frontend/CLAUDE.md` — sua identidade, território, stack, regras
2. `docs/README.md` — convenções da pasta de docs
3. `docs/runbooks/ROTEIRO-FRONTEND.md` — sua bíblia. Tem os prompts prontos pra cada tarefa FE-XX na seção 5.
4. `docs/plans/FASE-3-VISUALIZACAO.md` — contexto detalhado de cada FE-XX
5. `docs/architecture/especificacao-tecnica.md` — contratos REST que você vai consumir
6. `docs/architecture/fluxo-autenticacao.md` — diagrama do fluxo de auth (importante pra FE-04)
7. `docs/architecture/design-proposals/variante-c-timeline.html` — design alvo. Abra no navegador antes de implementar FE-05+.

**Regras gerais de execução (NÃO QUEBRE):**

1. **Crie uma única branch** chamada `feature/frontend-fase3-completa` a partir de `develop` atualizada. Trabalhe nela do começo ao fim. Não faça push pra `develop` em momento nenhum — o humano revisa todos os commits e mergeia de manhã.

2. **Cada tarefa = um commit dedicado nesta branch.** Mensagem padronizada:
   - `feat(FE-XX): título curto` pra implementações
   - `style(FE-XX): título` quando for ajuste puro de UI sem nova lógica
   - `fix(FE-XX): título` quando for correção
   
   O título do commit deve dar pra pessoa entender o que mudou em 5 segundos.

3. **Antes de começar cada tarefa:** leia a seção correspondente em `docs/plans/FASE-3-VISUALIZACAO.md` E o prompt completo da tarefa em `docs/runbooks/ROTEIRO-FRONTEND.md` seção 5. Os prompts já têm código pronto pra usar como referência.

4. **Implemente seguindo a runbook + plano.** Se quiser desviar significativamente (ex: usar biblioteca diferente, estrutura diferente), **pare e documente no status**, não improvise.

5. **Após implementar, rode os testes:**
   ```bash
   cd frontend
   npm test
   ```
   Tudo verde é pré-requisito pra commit. Se algum teste vermelho:
   - Se é teste que você acabou de escrever, ajuste e re-rode
   - Se é teste antigo que quebrou, investiga: a tarefa atual quebrou contrato? Pode ser bug. **Pare** e relate.

6. **Após implementar, rode também o lint e o build:**
   ```bash
   npm run lint
   npm run build
   ```
   Esses dois também devem passar antes de commit. Lint pega problemas de estilo/typing; build confirma que o código compila pra produção.

7. **Escreva o status report** em `docs/status/FE-XX-<slug>.md` seguindo `docs/status/_TEMPLATE.md`. Cobrir:
   - O que foi feito (arquivos modificados/criados)
   - Desvios do plano (se houver)
   - Decisões tomadas durante a execução
   - Próximo passo
   
   Status report é parte do commit (mesmo commit da implementação, não separado).

8. **Commit** com a mensagem padrão. Não faça push intermediário — mantém local. O humano vai puxar de manhã.

9. **Passe pra próxima tarefa.** Sem pedir permissão, sem aguardar.

**Pontos pra PARAR e me esperar:**

Cria `docs/status/FE-XX-PARADO.md` explicando o motivo se:

- `npm test` falha em testes antigos e não é óbvio o porquê
- `npm run build` falha por erro de TypeScript que parece estrutural
- `npm run lint` falha com erros que você não consegue resolver mantendo o código funcional
- Pré-requisito do prompt não está satisfeito (ex: FE-07 pede `usePedidos` que assume FE-03 já está pronta)
- O contrato real da API (Swagger UI em `localhost:8080/swagger-ui.html`) **diverge** do que `docs/architecture/especificacao-tecnica.md` documenta — significa que alguém precisa decidir qual é a verdade, não é decisão sua
- Você bate em decisão de produto/design não especificada (ex: "cor exata desse botão? como reagir quando lista está vazia? texto exato dessa mensagem de erro?")

**Não pare por:**
- Pequenos ajustes de import, formatação, ordem de props — isso é normal
- Pequenas adaptações de Tailwind classes pra o visual ficar como o mockup
- Diferenças entre o que está no plano e seu conhecimento atual do código — siga o plano como guia, ajuste pra fazer sentido com o estado real

**Backend disponível pra você testar:**

O backend da Fase 3 está completo e mergeado em develop. Pode subir local:

```bash
# Terminal separado
cd financas_bot_telegram
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

E aí ter o backend rodando em `localhost:8080`. Pra explorar a API: `http://localhost:8080/swagger-ui.html`.

**Alternativa sem o backend rodando:** use o MSW (já configurado na FE-02). Ativa setando `VITE_USE_MOCK=true` no `.env.development`. Os mocks devolvem dados fakes mas consistentes com a estrutura real.

**Sequência das 9 tarefas, na ordem exata:**

1. `FE-03` — API client (fetch wrapper) — ver `docs/runbooks/ROTEIRO-FRONTEND.md` seção 5, item FE-03
2. `FE-04` — Roteamento + AuthGuard + páginas Entrar/Erro/Home placeholder
3. `FE-05` — Componente `PedidoCard` + `StatusBadge` + `src/lib/formato.ts`
4. `FE-06` — Componentes de filtro (FiltroStatus, SeletorMes, BarraBusca)
5. `FE-07` — Página Home com TanStack Query (lista, Timeline, paginação, filtros sincronizados com URL)
6. `FE-08` — `CabecalhoApp` com saudação + resumo do mês
7. `FE-09` — `ModalComprovante` (iframe pra suportar imagem e PDF)
8. `FE-10` — PWA: manifest + service worker via `vite-plugin-pwa`
9. `FE-11` — Acessibilidade e revisão final (a11y, aria-labels, contraste, navegação por teclado)

**Lembretes finais:**

- **Cookies cross-origin:** o backend em `localhost:8080` e o front em `localhost:5173` são origins diferentes. Toda chamada fetch precisa de `credentials: 'include'` pra o cookie de sessão ir. O backend já tem CORS configurado pra `http://localhost:5173`.
- **Mobile-first sempre.** Testar em DevTools com viewport mobile (iPhone 12 ~390px) em cada tarefa. O mockup `variante-c-timeline.html` é mobile-first; replique fielmente o visual.
- **Não inventar feature.** Se sentir que falta funcionalidade no que o plano pediu, anota no status e segue — não adiciona.
- **Imports do `src/api/tipos.ts`** já foram criados na FE-02. Use eles, não redeclare types.
- **Mocks do MSW** já existem (`src/mocks/handlers.ts`). Use eles pra desenvolver sem backend. Se precisar adicionar handler novo (ex: pra um endpoint que ainda não tem mock), adicione lá.
- **Não esquecer do `<QueryClientProvider>` no `main.tsx`** quando chegar na FE-07 — TanStack Query precisa disso pra funcionar.
- **Spring MVC, não WebFlux.** O backend é Spring MVC. Tudo que você vê em código backend antigo mencionando WebFlux está desatualizado.

**Status final:**

Quando terminar todas as 9 ou parar em alguma, deixa um resumo final em `docs/status/_RESUMO-overnight-front.md` listando:

- O que foi feito (FEs concluídas)
- O que ficou parado e por quê
- Qualquer pendência técnica nova que apareceu (vale registrar em `docs/PENDENCIAS-TECNICAS.md` direto se for clara)
- Qualquer comportamento estranho que você notou

Esse arquivo é o que o humano vai ler primeiro de manhã.

Boa execução. Roda!

---

## Fim do prompt — daqui pra baixo são notas pra você (humano)

---

## Checklist do que esperar de manhã

Se tudo correr bem, ao acordar você terá:

- 9 commits novos na branch `feature/frontend-fase3-completa`
- 9 status reports em `docs/status/FE-03-*.md` até `FE-11-*.md`
- 1 arquivo `docs/status/_RESUMO-overnight-front.md`
- (Eventualmente) novos itens em `docs/PENDENCIAS-TECNICAS.md` se o Claude do front descobriu pendência

**Antes de mergear em develop, eu sugiro:**

1. Revisão visual dos commits via IntelliJ Git tool ou `git log --oneline develop..feature/frontend-fase3-completa`
2. Subir o front local: `cd frontend && npm install && npm run dev` (porta 5173)
3. Subir o back local também: `cd financas_bot_telegram && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
4. Abrir `http://localhost:5173`:
   - Sem cookie → deve cair em `/erro?motivo=precisa-link`
   - Gera convite via Swagger UI ou curl: `POST /admin/api/v1/requisitantes/1/convite`
   - Cola o `?t=<token>` na URL: `http://localhost:5173/entrar?t=<token>`
   - Deve fazer exchange, redirecionar pra Home, mostrar lista de pedidos
5. Testar em mobile via DevTools (toggle device toolbar → iPhone 12)
6. Testar instalação PWA: Chrome → ícone de instalar na barra de URL → instala como app, abre fullscreen
7. Se tudo ok: PR `feature/frontend-fase3-completa` → `develop`, mergeia

## Em caso de problemas overnight

Se você acordar e o Claude do front tiver parado em alguma tarefa:
- Leia `docs/status/FE-XX-PARADO.md` (ou o último status report)
- Decida: deixa o problema pra mim resolver (planejador), ou corrige direto
- Se for ajustar e re-executar, basta dar continue: "Voltei. Resolve o problema da FE-XX e continua a sequência."
