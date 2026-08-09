---
task: FE-015
titulo: "Tela Funcionários — lista + formulário de cadastro/edição"
data: 2026-06-04
branch: feature/fe-015-tela-funcionarios
responsavel: claude-front
estado: concluido
gates:
  build: ok
  lint: ok
  testes: ok
  testes_total: 70
  testes_novos: 9
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
e2e_full:
  exigido: false
  executado: false
  status: nao-aplicavel
commits:
  - 50fc8ab
pr: pendente
desvios: 1
pendencias_humano: 0
---

# FE-015 — Tela Funcionários — lista + formulário de cadastro/edição

---

## O que foi feito

### Shape real da API (BE-025)

Verificado em `FuncionarioResponse.java` e `FuncionarioController.java` antes de implementar.

```
GET /api/funcionarios → List<FuncionarioResponse>
  id: Long
  nome: String
  salarioBase: BigDecimal   (number no JSON)
  formaPagamento: "PIX" | "TED"
  chavePix: String | null
  banco: String | null
  agencia: String | null
  conta: String | null
  tipoConta: String | null  ("CORRENTE" | "POUPANCA" — não é enum serializado, é String)
  contaPropria: boolean
  obsPagamento: String | null
  diaPagamentoReferencia: Integer | null
  ativo: boolean
  criadoEm: LocalDateTime → ISO string
  atualizadoEm: LocalDateTime → ISO string

POST/PUT /api/funcionarios → FuncionarioRequest (mesmo shape, sem id/criadoEm/atualizadoEm/ativo)
DELETE /api/funcionarios/{id} → 204 No Content
GET /api/funcionarios/{id} → FuncionarioResponse
```

Nota sobre `tipoConta`: o backend declara como `String` no DTO (não enum Java serializado), mas os valores aceitos são "CORRENTE" e "POUPANCA" conforme schema da tabela.

### Arquivos criados

- `src/types/folha.ts` — `Funcionario`, `FormaPagamento` (enum), `TipoConta` (union), `FuncionarioRequest`. Arquivo projetado para extensão por FE-016 (seção de comentário no final).
- `src/api/folha.ts` — `listarFuncionarios()`, `buscarFuncionario()`, `criarFuncionario()`, `atualizarFuncionario()`, `desativarFuncionario()`. Projetado para extensão por FE-016.
- `src/paginas/folha/FuncionariosPage.tsx` — lista de funcionários ativos com navegação para `/folha/funcionarios/{id}`, form inline para cadastro/edição, confirmação de desativação, badge laranja para `contaPropria=false`.
- `src/paginas/folha/FolhaFuncionarioPage.tsx` — **STUB** para FE-016. Garante que a rota existe e é navegável.
- `src/components/folha/FuncionarioForm.tsx` — formulário controlado com campos condicionais PIX/TED via `useState`. Sem `react-hook-form` (não instalado).
- `src/components/folha/FuncionarioForm.test.tsx` — 9 testes novos.

### Arquivos modificados

- `src/api/client.ts` — adicionados métodos `put()` e `delete()` (ausentes, necessários para FE-015 e FE-016).
- `src/App.tsx` — adicionadas rotas `/folha` e `/folha/funcionarios/:id` dentro de `AuthGuard`.
- `src/components/CabecalhoApp.tsx` — adicionado link "Folha" no cabeçalho principal para navegar para `/folha`.

---

## Validações realizadas

| Validação | Resultado |
|---|---|
| `npm test` (Vitest) 70/70 | ✓ |
| `npm run lint` | ✓ limpo |
| `npm run build` (TS + Vite) | ✓ exit 0 |
| Campos PIX ocultos quando forma=TED | ✓ (teste) |
| Campos TED ocultos quando forma=PIX | ✓ (teste) |
| Badge laranja para `contaPropria=false` | ✓ (teste + visual) |
| Modo edição preenche campos corretamente | ✓ (teste) |
| `client.put()` e `client.delete()` adicionados | ✓ |
| Rota `/folha` existe e está protegida por AuthGuard | ✓ |
| Rota `/folha/funcionarios/:id` existe (stub FE-016) | ✓ |

---

## Desvios do plano

**Desvio 1 (escopo expandido — necessidade técnica):** adicionados métodos `put()` e `delete()` em `src/api/client.ts`. O plano não mencionava explicitamente essa necessidade, mas sem eles era impossível implementar `atualizarFuncionario()` e `desativarFuncionario()`. Nenhum risco — expansão mínima e backward-compatible.

---

## Decisões tomadas durante a execução

**Formulário controlado com `useState` em vez de `react-hook-form`:** `react-hook-form` não está instalado no projeto. Usar `useState` para os campos condicionais é mais leve e suficiente para o formulário atual. Se a complexidade crescer, instalar `react-hook-form` entra como task separada.

**`tipoConta` como `type TipoConta = 'CORRENTE' | 'POUPANCA'`:** o backend usa `String` no DTO, mas os valores são restritos. Optei por union type no front para type safety sem instalar Zod.

**`FolhaFuncionarioPage` como stub explícito:** o plano diz "clique na linha navega para `/folha/funcionarios/{id}`". A rota precisa existir mesmo sem FE-016. Criei um stub com header básico e mensagem "Em construção".

**Link "Folha" no `CabecalhoApp`:** o plano pede "adicionar aba" na navegação. O `CabecalhoApp` é o cabeçalho existente — adicionei o link ali para não criar novo componente de navegação. FE-016 pode refinar se necessário.

---

## Arquivos criados/modificados

- `src/types/folha.ts` (criado)
- `src/api/folha.ts` (criado)
- `src/api/client.ts` (modificado: métodos `put` e `delete`)
- `src/paginas/folha/FuncionariosPage.tsx` (criado)
- `src/paginas/folha/FolhaFuncionarioPage.tsx` (criado — stub para FE-016)
- `src/components/folha/FuncionarioForm.tsx` (criado)
- `src/components/folha/FuncionarioForm.test.tsx` (criado — 9 testes)
- `src/App.tsx` (modificado: rotas `/folha` e `/folha/funcionarios/:id`)
- `src/components/CabecalhoApp.tsx` (modificado: link de navegação "Folha")
- `docs/sprints/03-folha-pagamento/status/FE-015-tela-funcionarios.md` (criado)
