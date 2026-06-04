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

// ── Vales ─────────────────────────────────────────────────────────────────────

/** Shape do response de GET /api/funcionarios/{id}/vales (ValeResponse.java) */
export interface Vale {
  id: number
  funcionarioId: number
  categoria: string          // 'VALE'
  descricao: string
  valor: number              // BigDecimal → number
  status: string             // 'PENDENTE' | 'PAGO'
  fechado: boolean           // true = pertence a um Pedido FOLHA já fechado
  dataPedido: string         // LocalDate → "YYYY-MM-DD"
  dataCriacao: string        // LocalDateTime → ISO string
}

/** Body para POST /api/funcionarios/{id}/vales (ValeRequest.java) */
export interface ValeRequest {
  descricao: string
  valor: number
  dataPedido?: string        // LocalDate → "YYYY-MM-DD" (opcional no backend)
}

// ── Adiantamentos ─────────────────────────────────────────────────────────────

/** Shape do response de GET /api/funcionarios/{id}/adiantamentos (AdiantamentoResponse.java) */
export interface Adiantamento {
  id: number
  funcionarioId: number
  descricao: string
  valorTotal: number         // BigDecimal → number
  valorParcela: number       // BigDecimal → number
  numParcelas: number
  parcelasPagas: number
  parcelasRestantes: number  // campo computado pelo backend (numParcelas - parcelasPagas)
  dataInicio: string         // LocalDate → "YYYY-MM-DD"
  ativo: boolean
  criadoEm: string           // LocalDateTime → ISO string
}

/** Body para POST /api/funcionarios/{id}/adiantamentos (AdiantamentoRequest.java) */
export interface AdiantamentoRequest {
  descricao: string
  valorTotal: number
  valorParcela: number
  numParcelas: number
  dataInicio: string         // LocalDate → "YYYY-MM-DD"
}

// ── Fechamentos ───────────────────────────────────────────────────────────────

/** Shape do response de GET/POST /api/funcionarios/{id}/fechamentos (PedidoFolhaResponse.java) */
export interface Fechamento {
  id: number
  funcionarioId: number
  valor: number              // BigDecimal → number (valor líquido calculado)
  status: string             // 'PENDENTE' | 'PAGO' | 'CANCELADO'
  observacao: string | null  // breakdown: "salário R$X - vales R$Y - adiantamentos R$Z + ajuste R$W"
  mesReferencia: string      // LocalDate → "YYYY-MM-DD" (primeiro dia do mês, ex: "2026-06-01")
  dataCriacao: string        // LocalDateTime → ISO string
}
