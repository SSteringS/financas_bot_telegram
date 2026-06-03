/**
 * auth.ts — helper de autenticação para a suíte E2E.
 *
 * loginE2E(page) executa o fluxo de magic link completo:
 *   1. Gera convite via admin API
 *   2. Extrai token da URL retornada
 *   3. Faz exchange via /api/v1/auth/exchange
 *   4. Injeta cookie finbot_session no contexto do Playwright
 */

import type { Page } from '@playwright/test';
import { config as loadEnv } from 'dotenv';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
loadEnv({ path: resolve(__dirname, '..', '..', '.env.e2e') });

const BACKEND = process.env['E2E_BACKEND_URL'] ?? 'http://localhost:8080';
const ADMIN_SECRET = process.env['E2E_ADMIN_SECRET'];

/**
 * Autentica o requisitante de teste (id=99) no Playwright.
 * Injeta cookie finbot_session no contexto da page.
 *
 * Pré-condição: requisitante 99 deve existir no banco (garantirRequisitanteE2E no globalSetup).
 */
export async function loginE2E(page: Page): Promise<void> {
  if (!ADMIN_SECRET) {
    throw new Error(
      'E2E_ADMIN_SECRET não definido. Verifique .env.e2e (copie de .env.e2e.example).',
    );
  }

  // 1. Gerar convite via admin API
  const conviteRes = await fetch(
    `${BACKEND}/admin/api/v1/requisitantes/99/convite`,
    {
      method: 'POST',
      headers: { 'X-Admin-Key': ADMIN_SECRET },
    },
  );

  if (!conviteRes.ok) {
    throw new Error(
      `Admin API retornou ${conviteRes.status}. ` +
        `Verifique E2E_ADMIN_SECRET e que o backend está rodando em ${BACKEND}.`,
    );
  }

  const { url } = (await conviteRes.json()) as { url: string };

  // 2. Extrair token do query param ?t=
  const token = new URL(url).searchParams.get('t');
  if (!token) {
    throw new Error(`Token não encontrado na URL de convite: ${url}`);
  }

  // 3. Exchange token → cookie finbot_session
  const exchangeRes = await fetch(`${BACKEND}/api/v1/auth/exchange`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token }),
  });

  if (!exchangeRes.ok) {
    throw new Error(`Exchange falhou com status ${exchangeRes.status}`);
  }

  // 4. Extrair valor do cookie do header Set-Cookie
  const setCookie = exchangeRes.headers.get('set-cookie');
  if (!setCookie) {
    throw new Error('Exchange não retornou Set-Cookie header');
  }

  const cookieValue = parseCookieValue(setCookie, 'finbot_session');

  // 5. Injetar cookie no contexto do Playwright
  await page.context().addCookies([
    {
      name: 'finbot_session',
      value: cookieValue,
      domain: 'localhost',
      path: '/',
      httpOnly: true,
      sameSite: 'Lax',
    },
  ]);
}

// ── Helpers internos ──────────────────────────────────────────────────────────

function parseCookieValue(setCookie: string, name: string): string {
  // Set-Cookie: finbot_session=<valor>; Path=/; HttpOnly; SameSite=Lax; Max-Age=...
  const match = setCookie.match(new RegExp(`${name}=([^;]+)`));
  if (!match?.[1]) {
    throw new Error(`Cookie '${name}' não encontrado em Set-Cookie: ${setCookie}`);
  }
  return match[1];
}
