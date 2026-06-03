/**
 * derrubar-stack.ts — encerra backend e frontend iniciados pelo subir-stack.ts.
 *
 * Uso:
 *   tsx e2e/scripts/derrubar-stack.ts
 *
 * Lê os PIDs de frontend/.e2e-pids, envia SIGTERM (ou taskkill no Windows),
 * aguarda até 5s, SIGKILL se necessário (Unix). Remove o arquivo de PIDs.
 */

import { readFile, unlink } from 'fs/promises';
import { existsSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';
import { spawnSync } from 'child_process';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const frontendDir = resolve(__dirname, '..', '..');
const pidsFile = resolve(frontendDir, '.e2e-pids');
const isWin = process.platform === 'win32';

const SIGTERM_GRACE_MS = 5_000;
const POLL_INTERVAL_MS = 200;

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Verifica se o processo está vivo (signal 0 = só checar existência). */
function processoVivo(pid: number): boolean {
  try {
    process.kill(pid, 0);
    return true;
  } catch {
    return false;
  }
}

/** Envia sinal de término ao processo. */
function encerrar(pid: number): void {
  if (isWin) {
    // No Windows, taskkill /F /T mata a árvore de processos filhos também
    spawnSync('taskkill', ['/PID', String(pid), '/F', '/T'], {
      stdio: 'ignore',
    });
  } else {
    try {
      process.kill(pid, 'SIGTERM');
    } catch {
      // Processo já encerrou
    }
  }
}

/** Aguarda até 5s pelo encerramento; SIGKILL se necessário (Unix). */
async function aguardarEncerramento(pid: number, nome: string): Promise<void> {
  if (isWin) return; // taskkill /F já é síncrono e forçado

  const deadline = Date.now() + SIGTERM_GRACE_MS;
  while (Date.now() < deadline && processoVivo(pid)) {
    await new Promise((r) => setTimeout(r, POLL_INTERVAL_MS));
  }

  if (processoVivo(pid)) {
    console.log(`  ⚡ SIGKILL em ${nome} (PID ${pid}) — não encerrou em ${SIGTERM_GRACE_MS}ms`);
    try {
      process.kill(pid, 'SIGKILL');
    } catch {
      // Ignorar
    }
  }
}

// ── Main ─────────────────────────────────────────────────────────────────────

async function main(): Promise<void> {
  if (!existsSync(pidsFile)) {
    console.log('ℹ️  .e2e-pids não encontrado — stack pode já estar derrubada');
    process.exit(0);
  }

  const raw = await readFile(pidsFile, 'utf-8');
  const pids: Record<string, number | undefined> = JSON.parse(raw);

  for (const [nome, pid] of Object.entries(pids)) {
    if (pid == null) continue;

    if (!processoVivo(pid)) {
      console.log(`ℹ️  ${nome} (PID ${pid}) já não está rodando`);
      continue;
    }

    console.log(`🛑 Encerrando ${nome} (PID ${pid})...`);
    encerrar(pid);
    await aguardarEncerramento(pid, nome);
    console.log(`✓ ${nome} encerrado`);
  }

  await unlink(pidsFile);
  console.log('\n✓ Stack derrubada, .e2e-pids removido');
}

main().catch((err: Error) => {
  console.error(`✗ ${err.message}`);
  process.exit(1);
});
