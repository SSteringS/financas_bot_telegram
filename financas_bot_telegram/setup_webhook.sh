#!/bin/bash

# --- Configurações ---
LOCAL_PORT=8080
WEBHOOK_ENDPOINT="/webhook"
# ---------------------

# 1. Ler o Token do Bot da variável de ambiente
if [ -z "$TELEGRAM_BOT_TOKEN" ]; then
    echo "ERRO: A variável de ambiente 'TELEGRAM_BOT_TOKEN' não foi encontrada."
    echo "Por favor, configure-a com o seu token do bot do Telegram."
    exit 1
fi

# Verifica se o jq está instalado
if ! command -v jq &> /dev/null
then
    echo "ERRO: O comando 'jq' não foi encontrado. Por favor, instale-o."
    echo "No Git Bash, você pode instalar com: pacman -S jq"
    exit 1
fi

echo "Iniciando ngrok na porta $LOCAL_PORT..."

# 2. Iniciar o ngrok em background se não estiver rodando
if ! pgrep -x "ngrok" > /dev/null
then
    # Inicia o ngrok em background. A saída é redirecionada para /dev/null.
    ngrok http $LOCAL_PORT > /dev/null &
    # Aguarda um pouco para o ngrok e sua API iniciarem
    sleep 5
else
    echo "Ngrok já está em execução."
fi

echo "Buscando URL pública do ngrok..."

# 3. Obter a URL pública da API local do ngrok
NGROK_API_URL="http://localhost:4040/api/tunnels"
PUBLIC_URL=$(curl -s $NGROK_API_URL | jq -r '.tunnels[] | select(.proto=="https") | .public_url')

if [ -z "$PUBLIC_URL" ]; then
    echo "Não foi possível encontrar a URL HTTPS do ngrok. Verifique se o ngrok está rodando corretamente."
    exit 1
fi

echo "URL do ngrok encontrada: $PUBLIC_URL"

# 4. Montar a URL do setWebhook do Telegram
TELEGRAM_WEBHOOK_URL="https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/setWebhook?url=${PUBLIC_URL}${WEBHOOK_ENDPOINT}"

echo "Configurando webhook no Telegram..."

# 5. Chamar a API do Telegram para configurar o webhook
TELEGRAM_RESPONSE=$(curl -s $TELEGRAM_WEBHOOK_URL)

# 6. Verificar a resposta
OK=$(echo $TELEGRAM_RESPONSE | jq -r '.ok')
DESCRIPTION=$(echo $TELEGRAM_RESPONSE | jq -r '.description')

if [ "$OK" == "true" ]; then
    echo "✅ Webhook configurado com sucesso!"
    echo "Descrição: $DESCRIPTION"
else
    ERROR_CODE=$(echo $TELEGRAM_RESPONSE | jq -r '.error_code')
    echo "❌ Falha ao configurar o webhook."
    echo "Código de Erro: $ERROR_CODE"
    echo "Descrição: $DESCRIPTION"
fi