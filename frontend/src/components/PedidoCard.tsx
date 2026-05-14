import { StatusBadge } from './StatusBadge'
import { StatusPedido } from '../api/tipos'
import type { PedidoResumo } from '../api/tipos'
import { formatarMoeda, formatarData } from '../lib/formato'

interface PedidoCardProps {
  pedido: PedidoResumo
  onAbrirComprovante: () => void
}

export function PedidoCard({ pedido, onAbrirComprovante }: PedidoCardProps) {
  const pago = pedido.status === StatusPedido.PAGO

  const legendaData = pago && pedido.dataPagamento
    ? pedido.dataPedido === pedido.dataPagamento
      ? 'Pago no mesmo dia'
      : `Pedido em ${formatarData(pedido.dataPedido)} · pago em ${formatarData(pedido.dataPagamento)}`
    : null

  return (
    <div className="bg-white border border-zinc-200 rounded-xl p-3">
      <div className="flex items-start justify-between gap-2 mb-1">
        <p className="text-sm font-semibold text-zinc-900">{pedido.descricao}</p>
        <StatusBadge status={pedido.status} />
      </div>

      <p className="text-lg font-bold text-zinc-900 mb-1">{formatarMoeda(pedido.valor)}</p>

      {legendaData && (
        <p className="text-xs text-zinc-500 mb-3">{legendaData}</p>
      )}

      {pago && pedido.temComprovante && (
        <button
          className="w-full bg-emerald-600 text-white py-2.5 rounded-xl text-sm font-semibold flex items-center justify-center gap-2 hover:bg-emerald-700 transition-colors"
          onClick={onAbrirComprovante}
          aria-label={`Ver comprovante de ${pedido.descricao}`}
          style={{ minHeight: '44px' }}
        >
          <svg
            className="w-4 h-4"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
            />
          </svg>
          Ver comprovante
        </button>
      )}
    </div>
  )
}
