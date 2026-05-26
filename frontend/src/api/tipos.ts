// Fachada de tipos — derivada do OpenAPI spec (frontend/openapi.json).
// Não editar os tipos diretamente: atualizar o snapshot e regenerar.
//
// Ciclo de atualização do contrato:
//   1. Com o back rodando na 8080: npm run gen:api   → atualiza openapi.json
//   2. npm run gen:types                              → regenera tipos-gerados.ts
//   3. Corrigir eventuais quebras de tipo neste arquivo e commitar os diffs.
//
// Prova de não-drift: npm run gen:types && git diff --exit-code src/api/tipos-gerados.ts
import type { components } from './tipos-gerados'

type S = components['schemas']

// ── Enums (valores em runtime — enum TS, não string literal union) ─────────

export enum StatusPedido {
  PENDENTE = 'PENDENTE',
  PAGO = 'PAGO',
  // CANCELADO existe no spec (PedidoResumoDTO.status) mas ainda não é tratado
  // pelo front — adicionar aqui quando o app precisar exibir o estado.
}

export enum TipoPagamento {
  BOLETO = 'BOLETO',
  PIX = 'PIX',
  TED = 'TED',
  AGENDAMENTO = 'AGENDAMENTO',
  OUTRO = 'OUTRO',
}

// ── Entidades de domínio — derivadas do spec ──────────────────────────────

// Derivado de S['RequisitanteDTO']: campos id e nome tornados obrigatórios.
export type Requisitante = Required<Pick<S['RequisitanteDTO'], 'id' | 'nome'>>

// Derivado de S['PedidoResumoDTO']: campos estruturais obrigatórios.
// tipo/status mantêm enum TS (necessário para comparações em runtime).
export interface PedidoResumo
  extends Required<
    Pick<S['PedidoResumoDTO'], 'id' | 'valor' | 'descricao' | 'dataPedido' | 'dataPagamento' | 'temComprovante'>
  > {
  tipo: TipoPagamento
  status: StatusPedido
}

// Detalhe é o mesmo que resumo sem temComprovante (decisão pré-existente do app)
export type PedidoDetalhe = Omit<PedidoResumo, 'temComprovante'>

// O spec expõe PaginaDTOPedidoResumoDTO (não genérico).
// Mantido genérico no front para reutilização.
export interface Pagina<T> {
  items: T[]
  total: number
  pagina: number
  tamanho: number
  totalPaginas: number
}

// Derivado de S['ResumoMesDTO'] + S['ResumoStatusDTO']
type _StatusItem = Required<S['ResumoStatusDTO']>

export interface ResumoMes extends Required<Pick<S['ResumoMesDTO'], 'mes'>> {
  todos: _StatusItem
  pendentes: _StatusItem
  pagos: _StatusItem
}

// ── Auth ─────────────────────────────────────────────────────────────────

// Derivado de S['AuthExchangeRequest']: { token: string }
export type AuthExchangeRequest = S['AuthExchangeRequest']

export interface AuthMeResponse {
  requisitante: Requisitante
}

export interface Erro {
  erro: {
    codigo: string
    mensagem: string
  }
}

// ── Filtros de listagem (abstração do cliente — sem equivalente direto no spec) ──

export interface ListarPedidosFiltro {
  status?: 'pendente' | 'pago' | 'todos'
  tipo?: TipoPagamento | TipoPagamento[]
  de?: string // YYYY-MM-DD
  ate?: string // YYYY-MM-DD
  busca?: string
  page?: number
  tamanho?: number
}
