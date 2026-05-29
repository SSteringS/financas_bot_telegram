#!/bin/bash
# Bootstrap idempotente da EC2 finbot.
# Executado via user_data na criação da instância.
# Cada passo verifica se já está feito antes de agir.
set -euo pipefail

DOMAIN_NAME="${domain_name}"
CADDY_VERSION="2.9.1"
FINBOT_HOME="/opt/finbot"

log() { echo "[bootstrap] $*" | tee -a /var/log/bootstrap.log; }

# ---------- Java 21 ----------
if ! command -v java &>/dev/null; then
    log "Instalando Java 21..."
    dnf update -y
    dnf install -y java-21-amazon-corretto-headless
else
    log "Java já instalado: $(java -version 2>&1 | head -1)"
fi

# ---------- Usuário e pasta finbot ----------
if ! id finbot &>/dev/null; then
    log "Criando usuário finbot..."
    useradd -r -s /bin/false finbot
fi

if [ ! -d "$FINBOT_HOME" ]; then
    mkdir -p "$FINBOT_HOME"
fi
chown finbot:finbot "$FINBOT_HOME"

# ---------- Unit systemd finbot.service ----------
if [ ! -f /etc/systemd/system/finbot.service ]; then
    log "Instalando finbot.service..."
    cat > /etc/systemd/system/finbot.service <<'UNIT'
[Unit]
Description=Finbot Spring Boot application
After=network.target

[Service]
Type=simple
User=finbot
WorkingDirectory=/opt/finbot
ExecStart=/usr/bin/java -jar /opt/finbot/app.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=finbot

[Install]
WantedBy=multi-user.target
UNIT
    systemctl daemon-reload
    systemctl enable finbot
fi

# ---------- Caddy ----------
if [ ! -x /usr/bin/caddy ]; then
    log "Instalando Caddy $CADDY_VERSION (ARM64)..."
    curl -fsSL "https://github.com/caddyserver/caddy/releases/download/v$CADDY_VERSION/caddy_$${CADDY_VERSION}_linux_arm64.tar.gz" \
        | tar -xz -C /usr/bin caddy
    chmod +x /usr/bin/caddy
fi

if ! id caddy &>/dev/null; then
    log "Criando usuário caddy..."
    useradd --system --home /var/lib/caddy --shell /usr/sbin/nologin caddy
fi

mkdir -p /etc/caddy /var/lib/caddy /var/log/caddy
chown -R caddy:caddy /var/lib/caddy /var/log/caddy

# ---------- Caddyfile ----------
cat > /etc/caddy/Caddyfile <<CADDYFILE
api.$DOMAIN_NAME {
    reverse_proxy https://localhost:8443 {
        transport http {
            tls_insecure_skip_verify
        }
    }
}
CADDYFILE

# ---------- Unit systemd caddy.service ----------
if [ ! -f /etc/systemd/system/caddy.service ]; then
    log "Instalando caddy.service..."
    cat > /etc/systemd/system/caddy.service <<'UNIT'
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
    systemctl daemon-reload
fi
systemctl enable caddy

# ---------- Cap do journald (SystemMaxUse=200M) ----------
JOURNALD_DROP_IN="/etc/systemd/journald.conf.d/finbot.conf"
mkdir -p /etc/systemd/journald.conf.d
if [ ! -f "$JOURNALD_DROP_IN" ]; then
    log "Aplicando teto do journald..."
    cat > "$JOURNALD_DROP_IN" <<'CONF'
[Journal]
SystemMaxUse=200M
CONF
    systemctl restart systemd-journald
fi

# ---------- Keystore self-signed para webhook Telegram (porta 8443) ----------
# Opção (a): regenerar no boot. O cert muda a cada recreate →
#   após recreate é necessário re-registrar o webhook no Telegram:
#   curl -F "url=https://<ip>:8443/<token>" \
#        -F "certificate=@/opt/finbot/keystore.pem" \
#        https://api.telegram.org/bot<token>/setWebhook
# Alternativa de médio prazo: migrar o webhook para Caddy+LE (elimina o keystore).
KEYSTORE_PATH="$FINBOT_HOME/keystore.p12"
if [ ! -f "$KEYSTORE_PATH" ]; then
    log "Gerando keystore self-signed para o webhook..."
    PUBLIC_IP=$(curl -sf http://169.254.169.254/latest/meta-data/public-ipv4 || echo "localhost")
    dnf install -y openssl
    openssl req -x509 -newkey rsa:2048 -keyout /tmp/key.pem -out /tmp/cert.pem \
        -days 3650 -nodes \
        -subj "/CN=$PUBLIC_IP"
    openssl pkcs12 -export \
        -in /tmp/cert.pem -inkey /tmp/key.pem \
        -out "$KEYSTORE_PATH" \
        -name finbot \
        -passout pass:finbot123
    cp /tmp/cert.pem "$FINBOT_HOME/keystore.pem"
    chown finbot:finbot "$KEYSTORE_PATH" "$FINBOT_HOME/keystore.pem"
    rm -f /tmp/key.pem /tmp/cert.pem
    log "Keystore gerado. LEMBRETE: re-registrar webhook no Telegram com o novo cert."
fi

# ---------- Iniciar serviços ----------
log "Iniciando serviços..."
# finbot só inicia depois que o deploy colocar o app.jar
if [ -f "$FINBOT_HOME/app.jar" ]; then
    systemctl start finbot || true
fi
systemctl start caddy || true

log "Bootstrap concluído."
