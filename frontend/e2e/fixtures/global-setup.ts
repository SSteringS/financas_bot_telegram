/**
 * global-setup.ts — inicialização global da suíte E2E.
 *
 * Chamado 1x pelo Playwright antes de qualquer teste (playwright.config.ts → globalSetup).
 * Garante que o requisitante de teste (id=99) existe no banco.
 *
 * Falha ruidosamente se .env.e2e não estiver configurado.
 */

import { existsSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';
import { config as loadEnv } from 'dotenv';
import { garantirRequisitanteE2E, fecharConexao } from './banco.ts';

const __dirname = dirname(fileURLToPath(import.meta.url));
const envFile = resolve(__dirname, '..', '..', '.env.e2e');

export default async function globalSetup(): Promise<void> {
  // Validação explícita: .env.e2e deve existir
  if (!existsSync(envFile)) {
    throw new Error(
      'Arquivo .env.e2e não encontrado. Copie .env.e2e.example e preencha.\n' +
        `  Esperado em: ${envFile}`,
    );
  }

  // Carregar vars (pode já estar carregado pelo playwright.config.ts — idempotente)
  loadEnv({ path: envFile });

  console.log('[global-setup] Garantindo requisitante E2E (id=99)...');
  await garantirRequisitanteE2E();
  console.log('[global-setup] ✓ Requisitante E2E pronto');

  // Fechar conexão após setup — cada test worker abre a sua
  await fecharConexao();
}
