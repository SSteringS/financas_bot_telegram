/**
 * Tipos do domínio de Folha de Pagamento — EVO-09.
 *
 * Derivados dos DTOs do backend (BE-025 / BE-026 / BE-027 / BE-028).
 * Shape de referência: FuncionarioResponse.java, FuncionarioRequest.java
 *
 * Este arquivo será ESTENDIDO pelas tasks FE-016 (Vale, Adiantamento, Fechamento).
 */

// ── Enums ─────────────────────────────────────────────────────────────────────

export enum FormaPagamento {
  PIX = 'PIX',
  TED = 'TED',
}

// tipoConta vem como String no backend (não enum serializado)
export type TipoConta = 'CORRENTE' | 'POUPANCA'

// ── Funcionário ───────────────────────────────────────────────────────────────

/** Shape do response de GET /api/funcionarios e GET /api/funcionarios/{id} */
export interface Funcionario {
  id: number
  nome: string
  salarioBase: number
  formaPagamento: FormaPagamento
  chavePix: string | null
  banco: string | null
  agencia: string | null
  conta: string | null
  tipoConta: TipoConta | null
  contaPropria: boolean
  obsPagamento: string | null
  diaPagamentoReferencia: number | null
  ativo: boolean
  criadoEm: string    // LocalDateTime → ISO string
  atualizadoEm: string
}

/** Body para POST /api/funcionarios e PUT /api/funcionarios/{id} */
export interface FuncionarioRequest {
  nome: string
  salarioBase: number
  formaPagamento: FormaPagamento
  chavePix?: string
  banco?: string
  agencia?: string
  conta?: string
  tipoConta?: TipoConta
  contaPropria: boolean
  obsPagamento?: string
  diaPagamentoReferencia?: number
}

// ── Extensões futuras (FE-016) ────────────────────────────────────────────────
// Vale, ValeRequest, Adiantamento, AdiantamentoRequest, Fechamento
// serão adicionados neste arquivo por FE-016.
