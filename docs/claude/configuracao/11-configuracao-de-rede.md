<!-- Fonte: https://code.claude.com/docs/pt/network-config | Baixado em: 2026-08-06 | slug: network-config -->

# Configuração de rede empresarial

> Configure Claude Code para ambientes empresariais com servidores proxy, Autoridades de Certificação (CA) personalizadas e autenticação mútua de Transport Layer Security (mTLS).

Claude Code suporta várias configurações de rede e segurança empresariais através de variáveis de ambiente. Isso inclui rotear o tráfego através de servidores proxy corporativos, confiar em Autoridades de Certificação (CA) personalizadas e autenticar com certificados de Transport Layer Security (mTLS) mútuo para segurança aprimorada.

<Note>
  Todas as variáveis de ambiente mostradas nesta página também podem ser configuradas em [`settings.json`](https://code.claude.com/docs/pt/settings).
</Note>

<h2 id="proxy-configuration">
  Configuração de proxy
</h2>

<h3 id="environment-variables">
  Variáveis de ambiente
</h3>

Claude Code respeita variáveis de ambiente de proxy padrão:

```bash theme={null}
# Proxy HTTPS (recomendado)
export HTTPS_PROXY=https://proxy.example.com:8080

# Proxy HTTP (se HTTPS não estiver disponível)
export HTTP_PROXY=http://proxy.example.com:8080

# Ignorar proxy para solicitações específicas - formato separado por espaço
export NO_PROXY="localhost 192.168.1.1 example.com .example.com"
# Ignorar proxy para solicitações específicas - formato separado por vírgula
export NO_PROXY="localhost,192.168.1.1,example.com,.example.com"
# Ignorar proxy para todas as solicitações
export NO_PROXY="*"
```

<Note>
  Claude Code não suporta proxies SOCKS.
</Note>

<h3 id="basic-authentication">
  Autenticação básica
</h3>

Se seu proxy exigir autenticação básica, inclua credenciais na URL do proxy:

```bash theme={null}
export HTTPS_PROXY=http://username:password@proxy.example.com:8080
```

<Warning>
  Evite codificar senhas em scripts. Use variáveis de ambiente ou armazenamento seguro de credenciais.
</Warning>

<Tip>
  Para proxies que exigem autenticação avançada (NTLM, Kerberos, etc.), considere usar um serviço LLM Gateway que suporte seu método de autenticação.
</Tip>

<h2 id="ca-certificate-store">
  Armazenamento de certificados CA
</h2>

Por padrão, Claude Code confia tanto em seus certificados CA Mozilla agrupados quanto no armazenamento de certificados do seu sistema operacional. Ler o armazenamento do SO requer um runtime com `tls.getCACertificates`: o instalador nativo sempre possui, e instalações npm precisam do Node 22.15 ou posterior. Em versões mais antigas do Node, apenas o conjunto agrupado e `NODE_EXTRA_CA_CERTS` se aplicam. Proxies de inspeção TLS empresariais, como CrowdStrike Falcon e Zscaler, funcionam sem configuração adicional quando seu certificado raiz é instalado no armazenamento de confiança do SO e o runtime pode lê-lo.

`CLAUDE_CODE_CERT_STORE` aceita uma lista separada por vírgulas de fontes. Os valores reconhecidos são `bundled` para o conjunto de CA Mozilla enviado com Claude Code e `system` para o armazenamento de confiança do sistema operacional. O padrão é `bundled,system`.

Para confiar apenas no conjunto de CA Mozilla agrupado:

```bash theme={null}
export CLAUDE_CODE_CERT_STORE=bundled
```

Para confiar apenas no armazenamento de certificados do SO:

```bash theme={null}
export CLAUDE_CODE_CERT_STORE=system
```

<Note>
  `CLAUDE_CODE_CERT_STORE` não possui uma chave de esquema dedicada em `settings.json`. Defina-a através do bloco `env` em `~/.claude/settings.json` ou diretamente no ambiente do processo.
</Note>

<h2 id="custom-ca-certificates">
  Certificados CA personalizados
</h2>

Se seu ambiente empresarial usa uma CA personalizada, configure Claude Code para confiar nela diretamente:

```bash theme={null}
export NODE_EXTRA_CA_CERTS=/path/to/ca-cert.pem
```

<h2 id="mtls-authentication">
  Autenticação mTLS
</h2>

Para ambientes empresariais que exigem autenticação de certificado de cliente:

```bash theme={null}
# Certificado de cliente para autenticação
export CLAUDE_CODE_CLIENT_CERT=/path/to/client-cert.pem

# Chave privada do cliente
export CLAUDE_CODE_CLIENT_KEY=/path/to/client-key.pem

# Opcional: Frase de acesso para chave privada criptografada
export CLAUDE_CODE_CLIENT_KEY_PASSPHRASE="your-passphrase"
```

Claude Code lê os arquivos de certificado e chave na inicialização e os relê cada vez que aplica as configurações, incluindo quando as configurações mudam durante uma sessão. Para rotacionar o certificado e a chave, substitua os arquivos nos mesmos caminhos.

<h2 id="network-access-requirements">
  Requisitos de acesso à rede
</h2>

Claude Code requer acesso aos seguintes URLs. Coloque esses URLs na lista de permissões em sua configuração de proxy e regras de firewall, especialmente em ambientes de rede containerizados ou restritos.

| URL                            | Necessário para                                                                                                                                                                                                                                                                                                                                                                                                                              |
| ------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `api.anthropic.com`            | Solicitações da API Claude                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `claude.ai`                    | Autenticação de conta claude.ai                                                                                                                                                                                                                                                                                                                                                                                                              |
| `platform.claude.com`          | Autenticação de conta do Anthropic Console                                                                                                                                                                                                                                                                                                                                                                                                   |
| `mcp-proxy.anthropic.com`      | [Conectores MCP do claude.ai](https://code.claude.com/docs/pt/mcp#use-mcp-servers-from-claude-ai), incluindo conectores que um administrador da organização configura. O tráfego do conector é roteado através deste proxy; os conectores são ativados por padrão para usuários autenticados no claude.ai. Para desabilitar, defina [`ENABLE_CLAUDEAI_MCP_SERVERS=false`](https://code.claude.com/docs/pt/env-vars) ou a configuração [`disableClaudeAiConnectors`](https://code.claude.com/docs/pt/settings#available-settings) |
| `downloads.claude.ai`          | Downloads de executáveis de plugins; instalador nativo e atualizador automático nativo                                                                                                                                                                                                                                                                                                                                                       |
| `storage.googleapis.com`       | Contagens de instalação e metadados de plugins mostrados em `/plugin`. Uploads de [artefatos](https://code.claude.com/docs/pt/artifacts) assinados tentam este host primeiro; a publicação volta para `api.anthropic.com` quando ele está bloqueado                                                                                                                                                                                                                      |
| `storage.googleapis.com`       | Instalador nativo e atualizador automático nativo em versões anteriores a 2.1.116                                                                                                                                                                                                                                                                                                                                                            |
| `bridge.claudeusercontent.com` | Ponte WebSocket da extensão [Claude no Chrome](https://code.claude.com/docs/pt/chrome)                                                                                                                                                                                                                                                                                                                                                                                   |
| `*.claudeusercontent.com`      | Visualização de [artefatos](https://code.claude.com/docs/pt/artifacts) no claude.ai. O visualizador carrega o conteúdo de cada artefato de um subdomínio isolado dessa origem. Necessário no navegador do visualizador, não pelo próprio CLI                                                                                                                                                                                                                             |
| `raw.githubusercontent.com`    | Feed de changelog para [`/release-notes`](https://code.claude.com/docs/pt/commands) e as notas de lançamento mostradas após atualizar                                                                                                                                                                                                                                                                                                                                    |

Se você instalar Claude Code através do npm ou gerenciar sua própria distribuição binária, os usuários finais não precisam do instalador nativo e os usos do atualizador automático de `downloads.claude.ai`. Os outros usos na tabela se aplicam independentemente do método de instalação.

Claude Code também envia telemetria operacional opcional por padrão, que você pode desabilitar com variáveis de ambiente. Consulte [Serviços de telemetria](https://code.claude.com/docs/pt/data-usage#telemetry-services) para saber como desabilitá-la antes de finalizar sua lista de permissões.

Ao usar [Amazon Bedrock](https://code.claude.com/docs/pt/amazon-bedrock), [Google Cloud's Agent Platform](https://code.claude.com/docs/pt/google-vertex-ai), [Microsoft Foundry](https://code.claude.com/docs/pt/microsoft-foundry) ou uma sessão de [gateway de aplicativos Claude](https://code.claude.com/docs/pt/claude-apps-gateway) conectada, o tráfego de modelo e autenticação vão para seu provedor ou gateway em vez de `api.anthropic.com`, `claude.ai` ou `platform.claude.com`. A ferramenta WebFetch ainda chama `api.anthropic.com` para sua [verificação de segurança de domínio](https://code.claude.com/docs/pt/data-usage#webfetch-domain-safety-check) a menos que você defina `skipWebFetchPreflight: true` em [configurações](https://code.claude.com/docs/pt/settings).

[Claude Code na web](https://code.claude.com/docs/pt/claude-code-on-the-web) e [Code Review](https://code.claude.com/docs/pt/code-review) se conectam aos seus repositórios a partir da infraestrutura gerenciada pela Anthropic. Se sua organização GitHub Enterprise Cloud restringe o acesso por endereço IP, ative [herança de lista de permissão de IP para GitHub Apps instalados](https://docs.github.com/en/enterprise-cloud@latest/organizations/keeping-your-organization-secure/managing-security-settings-for-your-organization/managing-allowed-ip-addresses-for-your-organization#allowing-access-by-github-apps). O Claude GitHub App registra seus intervalos de IP, portanto, ativar essa configuração permite acesso sem configuração manual. Para [adicionar os intervalos à sua lista de permissões manualmente](https://docs.github.com/en/enterprise-cloud@latest/organizations/keeping-your-organization-secure/managing-security-settings-for-your-organization/managing-allowed-ip-addresses-for-your-organization#adding-an-allowed-ip-address) em vez disso, ou para configurar outros firewalls, consulte [Endereços IP da API Anthropic](https://platform.claude.com/docs/en/api/ip-addresses).

Para instâncias [GitHub Enterprise Server](https://code.claude.com/docs/pt/github-enterprise-server) auto-hospedadas atrás de um firewall, coloque na lista de permissões os mesmos [Endereços IP da API Anthropic](https://platform.claude.com/docs/en/api/ip-addresses) para que a infraestrutura Anthropic possa alcançar seu host GHES para clonar repositórios e postar comentários de revisão.

<h3 id="desktop-and-claude-ai">
  Desktop e claude.ai
</h3>

A tabela anterior cobre principalmente o CLI autônomo. O aplicativo Claude Desktop e claude.ai em um navegador carregam seu código de aplicação de hosts CDN adicionais da Anthropic, incluindo `assets-proxy.anthropic.com`. Permitir `claude.ai` enquanto bloqueia esses hosts produz uma página em branco em vez de um erro. Consulte [requisitos de acesso à rede](https://code.claude.com/docs/pt/desktop#network-access-requirements) na página Desktop.

<h2 id="additional-resources">
  Recursos adicionais
</h2>

* [Configurações de Claude Code](https://code.claude.com/docs/pt/settings)
* [Referência de variáveis de ambiente](https://code.claude.com/docs/pt/env-vars)
* [Guia de solução de problemas](https://code.claude.com/docs/pt/troubleshooting)
