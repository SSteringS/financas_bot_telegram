/**
 * subir-stack.ts — sobe backend Spring Boot + frontend Vite para a suíte E2E.
 *
 * Uso:
 *   tsx e2e/scripts/subir-stack.ts
 *
 * Pré-condições:
 *   - MySQL rodando em localhost:3306
 *   - Java 21 disponível no PATH
 *   - .env.e2e preenchido (copiado de .env.e2e.example)
 *
 * Ao sair: PIDs salvos em frontend/.e2e-pids para derrubar-stack.ts usar.
 */

import { spawn } from 'child_process';
import { createConnection } from 'net';
import { writeFile } from 'fs/promises';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';
import { aguardarHealthcheck } from './aguardar-saude.ts';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const frontendDir = resolve(__dirname, '..', '..');
const backendDir = resolve(__dirname, '..', '..', '..', 'financas_bot_telegram');
const pidsFile = resolve(frontendDir, '.e2e-pids');
const isWin = process.platform === 'win32';

const BACK_TIMEOUT_MS = Number(process.env['E2E_BACK_TIMEOUT_MS']) || 60_000;
const FRONT_TIMEOUT_MS = 30_000;
const MYSQL_TIMEOUT_MS = 5_000;

// ── Verificação MySQL (TCP connect) ──────────────────────────────────────────

function verificarMySQL(): Promise<void> {
  return new Promise((ok, fail) => {
    const socket = createConnection({ host: 'localhost', port: 3306 });

    const timer = setTimeout(() => {
      socket.destroy();
      fail(
        new Error(
          'MySQL não respondeu em localhost:3306 — suba seu MySQL local e rode de novo',
        ),
      );
    }, MYSQL_TIMEOUT_MS);

    socket.on('connect', () => {
      clearTimeout(timer);
      socket.destroy();
      ok();
    });

    socket.on('error', () => {
      clearTimeout(timer);
      fail(
        new Error(
          'MySQL não respondeu em localhost:3306 — suba seu MySQL local e rode de novo',
        ),
      );
    });
  });
}

// ── Spawn helpers ─────────────────────────────────────────────────────────────

function spawnBackground(
  cmd: string,
  args: string[],
  cwd: string,
): ReturnType<typeof spawn> {
  const proc = spawn(cmd, args, {
    cwd,
    detached: true,
    stdio: 'ignore',
    shell: isWin, // permite .cmd e .bat no Windows sem cmd.exe explícito
    windowsHide: true,
  });
  proc.unref(); // não bloqueia saída do processo pai
  return proc;
}

// ── Main ─────────────────────────────────────────────────────────────────────

async function main(): Promise<void> {
  // 1. Verificar MySQL
  console.log('🔍 Verificando MySQL em localhost:3306...');
  await verificarMySQL();
  console.log('✓ MySQL disponível\n');

  // 2. Subir backend
  console.log('🚀 Subindo backend Spring Boot (profile dev)...');
  const mvnCmd = isWin ? 'mvnw.cmd' : './mvnw';
  const backProc = spawnBackground(
    mvnCmd,
    ['spring-boot:run', '-Dspring-boot.run.profiles=dev'],
    backendDir,
  );

  console.log(`   PID back: ${backProc.pid}`);
  console.log(`   Aguardando healthcheck (timeout ${BACK_TIMEOUT_MS / 1000}s)...`);
  await aguardarHealthcheck('http://localhost:8080/actuator/health', BACK_TIMEOUT_MS);
  console.log('✓ Backend respondendo em localhost:8080\n');

  // 3. Subir frontend Vite
  console.log('🚀 Subindo frontend Vite (porta 5173)...');
  const viteCmd = isWin
    ? resolve(frontendDir, 'node_modules', '.bin', 'vite.cmd')
    : resolve(frontendDir, 'node_modules', '.bin', 'vite');
  const frontProc = spawnBackground(
    viteCmd,
    ['--port', '5173', '--strictPort'],
    frontendDir,
  );

  console.log(`   PID front: ${frontProc.pid}`);
  console.log(`   Aguardando healthcheck (timeout ${FRONT_TIMEOUT_MS / 1000}s)...`);
  await aguardarHealthcheck('http://localhost:5173', FRONT_TIMEOUT_MS);
  console.log('✓ Frontend respondendo em localhost:5173\n');

  // 4. Salvar PIDs
  const pids = { back: backProc.pid, front: frontProc.pid };
  await writeFile(pidsFile, JSON.stringify(pids, null, 2), 'utf-8');
  console.log(`✓ PIDs salvos em .e2e-pids`);
  console.log('  Stack pronta para os testes.\n');
}

main().catch((err: Error) => {
  console.error(`\n✗ ${err.message}`);
  process.exit(1);
});
