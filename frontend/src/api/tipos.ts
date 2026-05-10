export enum StatusPedido {
  PENDENTE = 'PENDENTE',
  PAGO = 'PAGO',
}

export enum TipoPagamento {
  BOLETO = 'BOLETO',
  PIX = 'PIX',
  TED = 'TED',
  AGENDAMENTO = 'AGENDAMENTO',
  OUTRO = 'OUTRO',
}

export interface Requisitante {
  id: number
  nome: string
}

export interface PedidoResumo {
  id: number
  valor: number
  descricao: string
  tipo: TipoPagamento
  status: StatusPedido
  dataPedido: string // YYYY-MM-DD
  dataPagamento: string | null
  temComprovante: boolean
}

// Detalhe é o mesmo que resumo, sem temComprovante (o endpoint de comprovante indica disponibilidade)
export type PedidoDetalhe = Omit<PedidoResumo, 'temComprovante'>

export interface Pagina<T> {
  items: T[]
  total: number
  pagina: number
  tamanho: number
  totalPaginas: number
}

export interface ResumoMes {
  mesAtual: string // YYYY-MM
  pendentes: {
    quantidade: number
    total: number
  }
  pagos: {
    quantidade: number
    total: number
  }
}

export interface Erro {
  erro: {
    codigo: string
    mensagem: string
  }
}

export interface AuthExchangeRequest {
  token: string
}

export interface AuthMeResponse {
  requisitante: Requisitante
}

export interface ListarPedidosFiltro {
  status?: 'pendente' | 'pago' | 'todos'
  tipo?: TipoPagamento | TipoPagamento[]
  de?: string // YYYY-MM-DD
  ate?: string // YYYY-MM-DD
  busca?: string
  page?: number
  tamanho?: number
}
