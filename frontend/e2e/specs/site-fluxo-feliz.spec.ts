/**
 * site-fluxo-feliz.spec.ts — fluxo principal autenticado.
 *
 * Valida: login via magic link → home com pedidos reais → modal de comprovante.
 *
 * Prerequisito: back + front rodando (npm run e2e:full cuida disso).
 * Dados: seados por semearPedidos em beforeEach; limpos em beforeEach.
 */

import { test, expect } from '@playwright/test';
import { limparDadosE2E, semearPedidos } from '../fixtures/banco.ts';
import { loginE2E } from '../fixtures/auth.ts';

// Data dinâmica: primeiro dia do mês corrente (YYYY-MM-DD).
// Garante que os pedidos sejam exibidos no filtro padrão "mês atual" da home.
const mesAtual = new Date().toISOString().slice(0, 7); // 'YYYY-MM'
const dataMes = `${mesAtual}-01`;

test.beforeEach(async () => {
  await limparDadosE2E();
  await semearPedidos([
    {
      descricao: 'E2E Boleto Energia',
      valor: 287.5,
      status: 'PAGO',
      tipo: 'BOLETO',
      dataPedido: dataMes,
      sKeyComprovante: 'e2e/sample.jpg', // gera comprovante → botão "Ver comprovante" aparece
    },
    {
      descricao: 'E2E PIX Maria',
      valor: 320.0,
      status: 'PENDENTE',
      tipo: 'PIX',
      dataPedido: dataMes,
    },
  ]);
});

test('fluxo feliz: login → home → comprovante', async ({ page }) => {
  // 1. Autenticação via magic link (sem interação manual)
  await loginE2E(page);

  // 2. Navegar para a home
  await page.goto('/');

  // 3. Verificar que os pedidos do mês aparecem na lista
  await expect(page.getByText('E2E Boleto Energia')).toBeVisible();
  await expect(page.getByText('E2E PIX Maria')).toBeVisible();

  // 4. CabecalhoApp: "1 pedido pendente" (E2E PIX Maria é o único PENDENTE)
  await expect(page.getByText(/1 pedido pendente/i)).toBeVisible();

  // 5. Abrir modal de comprovante do boleto pago
  //    Nota: o app usa modais (não rota /pedidos/:id). O botão tem aria-label explícito.
  await page.getByRole('button', { name: /ver comprovante de E2E Boleto Energia/i }).click();

  // 6. Modal deve estar visível (role="dialog" + aria-modal="true" definidos em ModalArquivo)
  await expect(page.getByRole('dialog')).toBeVisible();
});
