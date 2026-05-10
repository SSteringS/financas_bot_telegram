# CLAUDE.md — Frontend (frontend/)

## Responsabilidade

Este módulo é o frontend web do projeto. O Claude do **back não deve tocar nesta pasta**.

## Stack

A definir pelo time. Preencher esta seção ao iniciar o projeto.

## Comunicação com o backend

- Base URL produção: `https://3.228.138.109:8443`
- Certificado auto-assinado — configurar `rejectUnauthorized: false` no cliente HTTP em dev, ou usar o certificado público (`finbot-cert.pem`) para validação
- Endpoints disponíveis: consultar com o Claude do back ou a documentação da API

## Estrutura esperada

```
frontend/
  src/
  public/
  CLAUDE.md
  package.json (ou equivalente)
```

## Fluxo de branches

- Sempre criar branch a partir de `develop`
- Padrão: `feature/frontend-descricao-curta`
- PR: branch → `develop`
- Nunca commitar direto na `main` ou `develop`

## Regras importantes

- Tocar apenas em `frontend/` — nunca editar arquivos em `financas_bot_telegram/`
- Não commitar `.env` com URLs ou tokens reais
- Criar `.env.example` como template para variáveis de ambiente
