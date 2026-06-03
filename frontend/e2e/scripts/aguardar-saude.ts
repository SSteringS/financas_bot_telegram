/**
 * aguardar-saude.ts — polling de healthcheck com backoff exponencial.
 *
 * Uso como módulo:
 *   import { aguardarHealthcheck } from './aguardar-saude.ts';
 *   await aguardarHealthcheck('http://localhost:8080/actuator/health', 60_000);
 *
 * Uso como CLI:
 *   tsx e2e/scripts/aguardar-saude.ts <url> <timeoutMs>
 *   Exit 0 se respondeu, exit 1 se timeout.
 */

import { resolve } from 'path';
import { fileURLToPath } from 'url';

const INITIAL_DELAY_MS = 500;
const MAX_DELAY_MS = 5_000;

/**
 * Faz polling até a URL responder com status 2xx ou até esgotar o timeout.
 * Lança Error com mensagem clara se o timeout for atingido.
 */
export async function aguardarHealthcheck(
  url: string,
  timeoutMs: number,
): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  let delay = INITIAL_DELAY_MS;

  while (Date.now() < deadline) {
    try {
      const res = await fetch(url, { signal: AbortSignal.timeout(3_000) });
      if (res.ok) return;
    } catch {
      // Servidor ainda não está pronto — continuar polling
    }

    const remaining = deadline - Date.now();
    if (remaining <= 0) break;
    await new Promise((r) => setTimeout(r, Math.min(delay, remaining)));
    delay = Math.min(delay * 2, MAX_DELAY_MS);
  }

  throw new Error(
    `Healthcheck falhou: ${url} não respondeu em ${timeoutMs}ms`,
  );
}

// ── CLI ──────────────────────────────────────────────────────────────────────
const __filename = fileURLToPath(import.meta.url);
if (resolve(process.argv[1] ?? '') === resolve(__filename)) {
  const [, , url, timeoutArg] = process.argv;
  if (!url) {
    console.error('Uso: tsx aguardar-saude.ts <url> <timeoutMs>');
    process.exit(1);
  }
  const timeoutMs = Number(timeoutArg) || 10_000;

  aguardarHealthcheck(url, timeoutMs)
    .then(() => {
      console.log(`✓ ${url} está respondendo`);
      process.exit(0);
    })
    .catch((err: Error) => {
      console.error(`✗ ${err.message}`);
      process.exit(1);
    });
}
