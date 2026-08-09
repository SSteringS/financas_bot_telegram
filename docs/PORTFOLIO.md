# FinBot — Bot de Finanças Multi-canal com Painel Web

## Visão Geral
- **Nome:** FinBot
- **Status:** em andamento — em produção com usuário real, evoluindo por sprints
- **Tipo:** projeto pessoal com usuário real (meu pai usa em produção)
- **Problema que resolve:** meu pai envia pedidos de pagamento (foto de boleto, pix, transferência) por mensagem; o bot registra o pedido, guarda o comprovante na nuvem e um painel web permite consultar os pedidos, filtrar por período/status e baixar comprovantes — substituindo o fluxo manual de procurar comprovantes perdidos em conversas de chat

## Stack Técnica
- **Linguagem:** Java 21 (backend) + TypeScript em modo strict (frontend)
- **Framework:** Spring Boot 3.4.5 (Spring MVC), Spring Data JPA, Bean Validation, documentação de API com springdoc-openapi
- **Banco de dados:** MySQL 8 (RDS em produção, local em dev) com migrations versionadas via Flyway
- **Cloud/Deploy:** AWS — EC2 (t4g.micro, Amazon Linux 2023, systemd), RDS, S3 com buckets separados dev/prod, Secrets Manager para credenciais; infraestrutura codificada em Terraform; CI/CD com GitHub Actions (build do JAR + deploy via SSH a cada merge na main)
- **Frontend:** React 18 + Vite + Tailwind CSS, React Router 7, TanStack Query 5, PWA instalável no celular, MSW para mockar a API em desenvolvimento, tipos TypeScript gerados automaticamente a partir do contrato OpenAPI do backend
- **Canais de mensagem:** Telegram (webhook) e WhatsApp (Cloud API oficial da Meta)
- **Testes:** JUnit + testes de slice do Spring no backend; Vitest + Testing Library no frontend; suíte E2E com Playwright que sobe a stack completa (frontend + backend + MySQL) e mocka a API do Telegram com WireMock
- **Outros:** arquitetura hexagonal (ports & adapters), autenticação com troca de token por JWT, HTTPS

## Decisões Técnicas Relevantes
- **Canais de mensagem como adapters de uma arquitetura hexagonal.** Telegram e WhatsApp são adapters de entrada/saída plugados no mesmo núcleo de domínio. Quando o WhatsApp foi adicionado como segundo canal, nenhum caso de uso mudou — só nasceram adapters novos. A regra de negócio não sabe de onde a mensagem veio.
- **O controller do webhook nunca retorna erro 5xx.** Se o processamento falha, a API responde 200 e registra o erro em log. Motivo: ao receber 5xx, o Telegram reenvia o mesmo update repetidamente (retry storm), o que multiplicava o problema em vez de resolvê-lo. Combinado com controle de idempotência (cada mensagem processada é registrada e ignorada se chegar de novo), o sistema tolera reentregas sem duplicar pedidos.
- **Notificações via eventos in-process do Spring em vez de fila externa.** Kafka/SQS adicionariam um serviço a mais numa infraestrutura que roda numa única instância pequena. Eventos do próprio Spring resolvem o desacoplamento hoje, e a interface de evento deixa a troca por fila externa localizada em um ponto só, se o volume um dia justificar.
- **API oficial do WhatsApp (Meta Cloud API) em vez de biblioteca não-oficial.** Bibliotecas não-oficiais são mais convenientes, mas trazem risco de banimento da conta e de quebra silenciosa de contrato. Escolhi a API oficial mesmo sabendo que ela impõe processo de verificação de empresa.
- **Frontend e API em hostnames separados.** Decisão que forçou tratar corretamente CORS, cookies SameSite e requests de preflight — em vez de esconder o problema atrás de um proxy único.
- **Contrato de API como fonte única de verdade.** O backend publica a especificação OpenAPI e o frontend gera seus tipos TypeScript a partir dela. Se o contrato muda, a divergência aparece como erro de compilação no front — não como bug em runtime.
- **Desenvolvimento orquestrado com múltiplos agentes de IA.** O projeto é construído por instâncias de Claude Code com papéis separados — planejador, implementador backend, implementador frontend, revisor independente e QA — cada um com território de pastas próprio, trabalhando em worktrees git paralelos. Cada tarefa tem contrato de entrada (plano), contrato de saída (status report) e passa por revisão adversarial e análise de cobertura de testes antes do merge. O humano é o gate final de qualidade, revisando um único PR de integração por sprint. As decisões de arquitetura são registradas como ADRs versionados no repositório.

## O Que Aprendi Construindo
- **Webhook exige design defensivo.** Provedores reenviam mensagens; se o processamento não for idempotente, cada retry vira um pedido duplicado. Idempotência e semântica correta de status HTTP no webhook são requisito de primeira hora, não refinamento.
- **CORS e SameSite são problemas diferentes que aparecem juntos.** CORS controla quem pode chamar a API a partir do browser; SameSite controla quando o cookie viaja junto. Com front e API em hostnames separados, é preciso resolver os dois — e entender qual erro vem de qual mecanismo.
- **Migrations versionadas desde o primeiro dia.** Adotei Flyway depois que o banco já existia em produção, o que exigiu criar um baseline cuidadoso do schema existente. Começar com geração automática de schema (`ddl-auto`) em produção é uma dívida que só cresce.
- **API oficial com restrições vale mais que API não-oficial conveniente.** O WhatsApp Business exige verificação de empresa para operar fora do sandbox — uma restrição externa que virou bloqueio de cronograma real e me obrigou a replanejar a sprint. Dependências de terceiros precisam entrar no planejamento como risco, não como detalhe.
- **Teste E2E de sistema com dependência externa pede mock na borda.** A suíte Playwright sobe a aplicação real de ponta a ponta e mocka apenas a API do Telegram (com WireMock). Assim o teste cobre a cadeia inteira — do webhook ao banco ao painel — sem depender da disponibilidade de um serviço de terceiro.
- **Orquestrar agentes de IA é engenharia de processo.** Paralelismo entre agentes só funciona com isolamento explícito de território, contratos de artefato entre as etapas, revisão independente de quem implementou e um único ponto de integração humano. Sem isso, agentes em paralelo viram conflito de merge e retrabalho. O desenho desse workflow acabou sendo o aprendizado mais transferível do projeto.

## O Que Faria Diferente
- **Começaria com arquitetura hexagonal desde o MVP.** O núcleo nasceu acoplado ao Telegram e o refactor para portas agnósticas de canal foi feito depois, sob código existente — custou uma sprint parcial que teria sido evitada com o desenho certo no início.
- **Adotaria Flyway na primeira migration**, não depois do banco já existir em produção.
- **Definiria convenções de API e storage no início** — versionamento de rota (`/api/v1/`) e estrutura de pastas no S3 foram padronizados tardiamente, gerando correções que não criam valor de produto.

## Marco Atual / Próximo Passo
- **Concluído:** MVP em produção (bot Telegram + painel web com autenticação JWT + deploy na AWS); segundo canal de mensagem (WhatsApp) com arquitetura de adapters, idempotência e observabilidade (Micrometer/CloudWatch); primeira fase da suíte E2E com Playwright
- **Em andamento:** módulo de folha de pagamento (schema, entidades, endpoints e telas)
- **Próximo passo:** concluir o módulo de folha de pagamento, fazer o deploy das sprints acumuladas e destravar o WhatsApp em produção (verificação de empresa pendente na Meta)

## Link do GitHub
https://github.com/SSteringS/financas_bot_telegram

## Potencial para Post
- [x] Portfólio (MVP em produção com usuário real + multi-canal)
- [x] Decisão técnica (por que o webhook nunca retorna 5xx / eventos in-process em vez de fila)
- [x] Erro que aprendi (adotar Flyway com o banco já em produção; verificação do WhatsApp Business como bloqueio externo)
- [x] Tutorial (arquitetura hexagonal na prática: adicionando um segundo canal de mensagem sem tocar no domínio)
- [x] Artigo no Medium (desenvolvimento multiagente com Claude Code: papéis, territórios, gates e revisão adversarial)
