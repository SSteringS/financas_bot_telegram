/**
 * a11y-home.spec.ts — acessibilidade em páginas autenticadas.
 *
 * Threshold: zero violações de impacto 'serious' ou 'critical' (Decisão 11 do desenho).
 * Tolera 'minor' e 'moderate' no MVP — pode ser endurecido em sprint futura.
 *
 * Páginas testadas: / (home) e /erro.
 * Nota: /pedidos/:id não existe como rota — app usa modais na home.
 *
 * Prerequisito: back + front rodando.
 */

import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { loginE2E } from '../fixtures/auth.ts';

test('nenhuma violação de a11y séria em páginas autenticadas', async ({ page }) => {
  await loginE2E(page);

  // Testar as rotas reais do app (não existe /pedidos/:id — app usa modais na home)
  const paths = ['/', '/erro'];

  for (const path of paths) {
    await page.goto(path);

    const a11y = await new AxeBuilder({ page }).analyze();
    const seriousOrCritical = a11y.violations.filter(
      (v) => v.impact === 'serious' || v.impact === 'critical',
    );

    expect(
      seriousOrCritical,
      `Violações em ${path}: ${JSON.stringify(seriousOrCritical.map((v) => ({ id: v.id, impact: v.impact, description: v.description })))}`,
    ).toEqual([]);
  }
});
