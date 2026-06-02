/**
 * Global setup stub — será substituído em QA-003 (fixtures banco + auth).
 * Criado em QA-001 apenas para que `playwright.config.ts` possa ser carregado
 * sem erro de módulo durante `--list` e verificações de config.
 */
export default async function globalSetup(): Promise<void> {
  // QA-003 implementará: await garantirRequisitanteE2E()
}
