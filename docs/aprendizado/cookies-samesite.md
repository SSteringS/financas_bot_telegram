# Cookies — atributo SameSite e proteção CSRF

## Contexto da dúvida

Na BE-11 a gente configurou o cookie `finbot_session` com `.sameSite("Lax")`. A pergunta foi: o que essa configuração significa e por que esse valor.

## Resumo destilado

`SameSite` é um atributo do cookie que diz ao **navegador** quando ele deve anexar o cookie em requests cross-site. É uma defesa contra **CSRF** (Cross-Site Request Forgery) — ataque onde um site malicioso faz uma request pra sua API usando o cookie autenticado do usuário.

Três valores possíveis:

| Valor | Quando o navegador envia | Trade-off |
|---|---|---|
| `Strict` | Nunca em cross-site. Nem ao clicar link de email/notificação. | Mais seguro, mas quebra UX de "link manda no zap, pai clica e abre logado" |
| `Lax` | Em navegação top-level (link, URL típica). Não em subrequests (iframe, AJAX cross-site, POST automático). | Bom equilíbrio. **Default da maioria dos browsers modernos.** |
| `None` | Sempre, mesmo cross-site. Requer `Secure=true`. | Anula a proteção contra CSRF. Use só quando precisa MESMO (auth federada, embedding) |

**No nosso caso, `Lax` é a escolha certa**:
- Front (`finbot.dom.br`) e API (`api.finbot.dom.br`) são **same-site** (mesmo registrable domain `finbot.dom.br`), então fetch do front pra API funciona normalmente
- Site malicioso `evil-site.com` tentando POST pra nossa API com cookie do pai → bloqueado (cross-site, cookie não anexado)
- UX preservada: pai clica no link mágico do zap e cai no site, fluxo top-level, cookie funciona

## Pontos-chave

- **Site ≠ Origin.** Site = registrable domain (`finbot.dom.br`); Origin = scheme + host + port (`https://finbot.dom.br:443`). SameSite opera em "site", não em "origin".
- **`Lax` é o default** dos navegadores modernos quando o servidor não declara. Declarar explicitamente é apenas tornar a intenção visível e blindado contra versões antigas.
- **`SameSite=None` exige `Secure=true`** — combinação obrigatória, browsers rejeitam senão.
- **Camadas complementares pra cookie de sessão:**
  - `HttpOnly=true` → previne XSS (JavaScript não lê o cookie)
  - `Secure=true` → previne interceptação em redes não-confiáveis (só HTTPS)
  - `SameSite=Lax` → previne CSRF (não vai em requests cross-site)
- **`SameSite` não protege contra leitura cross-origin** — pra isso é CORS. Os dois mecanismos são complementares.

## Pra aprofundar

- Cross-Site Request Forgery (CSRF) — leitura clássica: OWASP CSRF cheat sheet
- Padrão "Cookie Prefixes" (`__Host-`, `__Secure-`) — proteção adicional
- Diferença entre cookies de primeira parte e terceira parte (third-party cookies)
- Compromissos do padrão "SameSite=None" e por que os browsers estão dificultando
