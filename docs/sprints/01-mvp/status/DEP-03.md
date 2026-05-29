---
task: DEP-03
titulo: "Subdomínio api.satyansaita.com + proxy reverso Caddy"
data: 2026-05-26
branch: feature/dep-03-api-subdominio-proxy
responsavel: claude-back
estado: concluido
gates:
  build: na
  lint: na
  testes: na
  testes_total: na
  testes_novos: na
  branch_convencao: ok
  territorio: ok
commits:
  - 46a7c74
pr: null
desvios: 0
pendencias_humano: 0
---

# DEP-03 — Subdomínio `api.satyansaita.com` + proxy reverso Caddy

## O que foi feito (parte automatizada — Terraform)

- `dns.tf`: criado `aws_route53_record.api_a` — A record `api.satyansaita.com` → `3.228.138.109` (EIP da EC2), TTL 300.
- `network.tf`: adicionados ingress 443 (HTTPS API via Caddy) e 80 (HTTP-01 challenge Let's Encrypt) no `aws_security_group.ec2_sg`.
- `terraform apply` executado: **1 add, 1 change, 0 destroy**. EC2 não foi recriada.
- DNS verificado: `nslookup api.satyansaita.com` → `3.228.138.109` ✓

## Desvios do plano

Nenhum.

## Decisões tomadas durante a execução

- Description da ingress rule de 80 escrita sem apóstrofo (`"HTTP - Lets Encrypt challenge + redirect"`) — AWS SG não aceita apóstrofo na description.
- Opção A do plano adotada (Caddy `reverse_proxy https://localhost:8443` com `tls_insecure_skip_verify`): zero mudança na app Spring, webhook segue intocado.

---

## Decisões pendentes (esperando humano)

> ✅ **RESOLVIDO (2026-05-27):** Caddy instalado na EC2 e validado — ver seção "Validação" abaixo. Não há mais pendência de humano (`pendencias_humano: 0`, `estado: concluido`). O roteiro de instalação abaixo fica registrado por histórico/reprodutibilidade.

**1 — Instalar Caddy na EC2 via SSH (parte manual obrigatória)**

A parte de proxy na EC2 não pode ser automatizada via `user_data` (só roda na criação da instância). O humano precisa executar os comandos abaixo via SSH para completar a task.

### Pré-condição

DNS já está propagado (`api.satyansaita.com` → `3.228.138.109`) e porta 80 já está aberta no SG.

### Comandos (executar via SSH na EC2)

```bash
# 1. Baixar o binário Caddy ARM64 estático
CADDY_VERSION="2.9.1"
curl -fsSL "https://github.com/caddyserver/caddy/releases/download/v${CADDY_VERSION}/caddy_${CADDY_VERSION}_linux_arm64.tar.gz" \
  | sudo tar -xz -C /usr/bin caddy
sudo chmod +x /usr/bin/caddy

# 2. Criar usuário e diretórios (conforme convenção do Caddy)
sudo useradd --system --home /var/lib/caddy --shell /usr/sbin/nologin caddy
sudo mkdir -p /etc/caddy /var/lib/caddy /var/log/caddy
sudo chown -R caddy:caddy /var/lib/caddy /var/log/caddy

# 3. Criar o Caddyfile
sudo tee /etc/caddy/Caddyfile > /dev/null <<'CADDYFILE'
api.satyansaita.com {
    reverse_proxy https://localhost:8443 {
        transport http {
            tls_insecure_skip_verify
        }
    }
}
CADDYFILE

# 4. Criar unit systemd do Caddy
sudo tee /etc/systemd/system/caddy.service > /dev/null <<'UNIT'
[Unit]
Description=Caddy
Documentation=https://caddyserver.com/docs/
After=network.target network-online.target
Requires=network-online.target

[Service]
Type=notify
User=caddy
Group=caddy
ExecStart=/usr/bin/caddy run --environ --config /etc/caddy/Caddyfile
ExecReload=/usr/bin/caddy reload --config /etc/caddy/Caddyfile --force
TimeoutStopSec=5s
LimitNOFILE=1048576
PrivateTmp=true
ProtectSystem=full
AmbientCapabilities=CAP_NET_ADMIN CAP_NET_BIND_SERVICE

[Install]
WantedBy=multi-user.target
UNIT

# 5. Habilitar e subir o Caddy
sudo systemctl daemon-reload
sudo systemctl enable caddy
sudo systemctl start caddy

# 6. Verificar status (Caddy vai obter o cert Let's Encrypt automaticamente)
sudo systemctl status caddy
sudo journalctl -u caddy -n 50
```

### Validação após instalação

```bash
# No seu terminal local — confirmar cert válido e resposta da API
curl -v https://api.satyansaita.com/actuator/health

# Deve retornar HTTP 200 com cert Let's Encrypt (sem -k)
# Checar: "issuer: C=US; O=Let's Encrypt; CN=R11"
```

Também confirmar que o webhook do Telegram segue funcionando (mandar mensagem pro bot e verificar que o `finbot.service` responde normalmente).

---

## Validação (2026-05-27)

Parte manual (Caddy) instalada na EC2 e validada pelo humano. Cadeia **proxy → app → security** confirmada com `curl -i https://api.satyansaita.com/api/v1/resumo`:

```
HTTP/1.1 401 Unauthorized
Server: Caddy
Content-Type: application/json;charset=UTF-8
{"codigo":"SESSAO_AUSENTE","mensagem":"Cookie de sessão ausente"}
```

O 401 com corpo do próprio app (`SESSAO_AUSENTE`, gerado pela security da BE-12), servido via `Server: Caddy` e **sem `-k`** no curl, prova: TLS válido (cert Let's Encrypt), Caddy terminando HTTPS, proxy alcançando o Spring na 8443, e a camada de auth processando. DEP-03 fechado de ponta a ponta.

**Correção do smoke-test:** o plano original sugeria `/actuator/health` (→ 404: sem `spring-boot-starter-actuator` no `pom.xml`) e tentou-se `/v3/api-docs` (→ 404: `springdoc.api-docs.enabled=false`/`swagger-ui.enabled=false` em prod, por design). Os dois 404 são **esperados**, não falha de proxy — por isso a validação foi feita em `/api/v1/resumo`.

**Para o Reviewer confirmar contra a realidade:**
- `curl -i https://api.satyansaita.com/api/v1/resumo` → 401 `SESSAO_AUSENTE` (cadeia ok).
- Cert é Let's Encrypt (curl sem `-k`; checar o issuer).
- **Regressão do webhook do Telegram** na 8443 — a Opção A não tocou na 8443, mas confirmar mandando mensagem pro bot.
- `terraform plan` limpo (DNS + SG aplicados; sem replace da EC2).

> **Achado correlato (fora do escopo do DEP-03, mas bloqueia o front):** `application-prod.properties` tem `app.cors.allowed-origin`, `app.cookie.domain` e `app.frontend.base-url` apontando pra `finbot.satyan.com.br` em vez de `satyansaita.com`. É o escopo do **DEP-05**.

---

## Próximos passos / observações pro próximo

- **DEP-05** (CORS + cookie `Domain=.satyansaita.com`) pode começar agora que o hostname está no ar — e tem o bug de domínio em `application-prod.properties` (acima) pra corrigir.
- DEP-04 (pipeline do front) é independente e pode rodar em paralelo.
- Renovação automática do cert é feita pelo Caddy (ACME interno) — não precisa de cron externo.

## Arquivos criados/modificados

- `financas_bot_telegram/infra/dns.tf` (modificado: adicionado `aws_route53_record.api_a`)
- `financas_bot_telegram/infra/network.tf` (modificado: ingress 80 e 443 no `ec2_sg`)
