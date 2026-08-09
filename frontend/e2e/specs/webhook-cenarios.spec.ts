/**
 * webhook-cenarios.spec.ts — canal de entrada Telegram parametrizado.
 *
 * Valida que diferentes tipos de Update chegam em POST /webhook/telegram
 * e o backend responde 200 (ADR 0003: webhook nunca retorna 5xx).
 *
 * MVP — 2 cenários: texto puro + sticker (ambos não criam pedido).
 * Estrutura parametrizada (for-of) permite adicionar cenários com 1 linha.
 *
 * Prerequisito: back rodando com user 99 em telegram.allowed-user-ids.
 * Nota: o backend NÃO valida HMAC (X-Telegram-Bot-Api-Secret-Token) —
 *   autorização é via allowedUserIds na config. Header omitido.
 */

import { test, expect } from '@playwright/test';
import { limparDadosE2E, querySql } from '../fixtures/banco.ts';
import {
  telegramUpdateTextoPuro,
  telegramUpdateSticker,
} from '../fixtures/payloads-telegram.ts';

const BACKEND = process.env['E2E_BACKEND_URL'] ?? 'http://localhost:8080';

interface PedidoRow {
  valor: string;
  descricao: string;
  tipo: string;
  status: string;
}

interface Cenario {
  nome: string;
  payload: object;
  esperaPedido: boolean;
  asserts?: (pedido: PedidoRow) => void;
}

const cenarios: Cenario[] = [
  {
    nome: 'texto puro retorna 200 sem criar pedido',
    payload: telegramUpdateTextoPuro({ fromUserId: 99, text: 'Oi bot' }),
    esperaPedido: false,
  },
  {
    nome: 'sticker retorna 200 sem criar pedido (handler genérico BE-15)',
    payload: telegramUpdateSticker({ fromUserId: 99 }),
    esperaPedido: false,
  },
  // TODO Fase 1.1: adicionar cenário foto+caption após decisão de mock de download de mídia Telegram (ADR 00XX)
  //   Bloqueado por: decisão sobre como o back trata getFile em E2E (§3 da spec qa-suite-e2e-fase1.md)
  //   Quando desbloqueado: 1 entrada aqui + 1 factory em payloads-telegram.ts
];

test.beforeEach(async () => {
  await limparDadosE2E();
});

for (const c of cenarios) {
  test(`webhook: ${c.nome}`, async ({ request }) => {
    // POST direto ao backend (Playwright APIRequestContext, não browser)
    const res = await request.post(`${BACKEND}/webhook/telegram`, {
      data: c.payload,
    });

    // ADR 0003: webhook NUNCA retorna 5xx — sempre 200
    expect(res.status()).toBe(200);

    // Asserção de banco: pedido criado ou não, conforme esperado para o cenário
    const pedidos = await querySql<PedidoRow>(
      'SELECT valor, descricao, tipo, status FROM pedidos_pagamento WHERE requisitante_id = 99',
    );

    if (c.esperaPedido) {
      expect(pedidos).toHaveLength(1);
      c.asserts?.(pedidos[0]);
    } else {
      expect(pedidos).toHaveLength(0);
    }
  });
}
