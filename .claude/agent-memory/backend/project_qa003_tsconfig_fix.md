---
name: e2e tsconfig allowImportingTsExtensions fix
description: QA-001 criou e2e/tsconfig.json com allowImportingTsExtensions:false, mas os fixtures e scripts usam extensão .ts nos imports (necessário para tsx ESM). Corrigido em QA-003.
type: project
---

`frontend/e2e/tsconfig.json` foi criado em QA-001 com `allowImportingTsExtensions: false`. Os arquivos E2E usam a extensão `.ts` nos imports (ex: `import { X } from './banco.ts'`) porque o runtime tsx em modo ESM resolve melhor com a extensão explícita.

**Correção aplicada em QA-003:** `allowImportingTsExtensions: false → true`. Válido porque `noEmit: true` já estava presente.

**Why:** sem a correção, `npx tsc -p e2e/tsconfig.json --noEmit` falha com TS5097 em todos os arquivos que importam com `.ts`.

**How to apply:** se criar novos arquivos E2E com imports `.ts` explícitos, verificar que `e2e/tsconfig.json` tem `allowImportingTsExtensions: true`. Já está corrigido desde QA-003.
