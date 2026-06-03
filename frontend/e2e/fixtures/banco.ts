/**
 * banco.ts — helpers de banco para a suíte E2E.
 *
 * Usa mysql2/promise com as credenciais de .env.e2e.
 * Requisitante de teste: id=99 ("E2E Test User").
 * NUNCA toca em dados com requisitante_id != 99.
 */

import { createConnection, Connection } from 'mysql2/promise';
import { config as loadEnv } from 'dotenv';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
loadEnv({ path: resolve(__dirname, '..', '..', '.env.e2e') });

// ── Tipos públicos ────────────────────────────────────────────────────────────

export interface PedidoFixture {
  descricao: string;
  valor: number;
  status: 'PENDENTE' | 'PAGO';
  tipo: 'BOLETO' | 'PIX' | 'TED' | 'AGENDAMENTO' | 'OUTRO';
  dataPedido: string; // YYYY-MM-DD
  sKeyComprovante?: string; // opcional — imagem_url do comprovante, só para PAGO
}

// ── Conexão singleton ────────────────────────────────────────────────────────

let _conn: Connection | null = null;

async function getConn(): Promise<Connection> {
  if (_conn) return _conn;

  const host = process.env['E2E_DB_HOST'];
  const port = process.env['E2E_DB_PORT'];
  const user = process.env['E2E_DB_USER'];
  const password = process.env['E2E_DB_PASSWORD'];
  const database = process.env['E2E_DB_NAME'];

  if (!host || !user || !password || !database) {
    throw new Error(
      'Variáveis de banco ausentes. Verifique .env.e2e (copie de .env.e2e.example).\n' +
        `  E2E_DB_HOST=${host ?? '(ausente)'}\n` +
        `  E2E_DB_USER=${user ?? '(ausente)'}\n` +
        `  E2E_DB_NAME=${database ?? '(ausente)'}`,
    );
  }

  _conn = await createConnection({
    host,
    port: Number(port ?? 3306),
    user,
    password,
    database,
  });
  return _conn;
}

/** Fecha a conexão (chamar em globalTeardown quando implementado). */
export async function fecharConexao(): Promise<void> {
  if (_conn) {
    await _conn.end();
    _conn = null;
  }
}

// ── Funções públicas ──────────────────────────────────────────────────────────

/**
 * Garante que o requisitante de teste (id=99) exista.
 * Idempotente: rodar N vezes não duplica nem falha.
 */
export async function garantirRequisitanteE2E(): Promise<void> {
  const c = await getConn();
  await c.query(
    `INSERT IGNORE INTO requisitante
       (id, nome, telefone, email, ativo, criado_em, canal_preferido)
     VALUES (99, 'E2E Test User', '+5511000000099', 'e2e@test.local', TRUE, NOW(), 'TELEGRAM')`,
  );
}

/**
 * Limpa APENAS os dados do requisitante de teste (id=99).
 *
 * ⚠️  SEGURANÇA: nenhum DELETE aqui toca registros com requisitante_id != 99.
 *     Dados do Pedro (id=1) ficam intocados.
 *
 * Ordem de deleção respeita FK constraints:
 *   comprovantes → pedidos_pagamento → auth_token
 * mensagem_processada: NÃO limpa — usa update_id único por run (sem risco de colisão).
 */
export async function limparDadosE2E(): Promise<void> {
  const c = await getConn();

  // 1. Comprovantes dos pedidos do requisitante 99
  await c.query(
    `DELETE FROM comprovantes
      WHERE pedido_id IN (
        SELECT id FROM pedidos_pagamento WHERE requisitante_id = 99
      )`,
  );

  // 2. Pedidos do requisitante 99
  await c.query(
    `DELETE FROM pedidos_pagamento WHERE requisitante_id = 99`,
  );

  // 3. Tokens de autenticação do requisitante 99
  await c.query(
    `DELETE FROM auth_token WHERE requisitante_id = 99`,
  );
}

/**
 * Insere pedidos de teste vinculados ao requisitante 99.
 * Os IDs gerados são auto-incremento — use querySql para recuperá-los.
 */
export async function semearPedidos(pedidos: PedidoFixture[]): Promise<void> {
  const c = await getConn();

  for (const p of pedidos) {
    const [result] = await c.query<import('mysql2').ResultSetHeader>(
      `INSERT INTO pedidos_pagamento
         (requisitante_id, descricao, valor, status, tipo, data_pedido, data_criacao)
       VALUES (99, ?, ?, ?, ?, ?, NOW())`,
      [p.descricao, p.valor, p.status, p.tipo, p.dataPedido],
    );

    if (p.sKeyComprovante && p.status === 'PAGO') {
      await c.query(
        `INSERT INTO comprovantes (pedido_id, imagem_url, data_pagamento)
         VALUES (?, ?, NOW())`,
        [result.insertId, p.sKeyComprovante],
      );
    }
  }
}

/**
 * Query genérica para asserções nos testes.
 * Ex.: await querySql('SELECT valor FROM pedidos_pagamento WHERE requisitante_id = 99')
 */
export async function querySql<T = Record<string, unknown>>(
  sql: string,
  params: unknown[] = [],
): Promise<T[]> {
  const c = await getConn();
  const [rows] = await c.query(sql, params);
  return rows as T[];
}
