import { StatusBadge } from './StatusBadge'
import { StatusPedido } from '../api/tipos'
import type { PedidoResumo } from '../api/tipos'
import { formatarMoeda, formatarData } from '../lib/formato'

interface PedidoCardProps {
  pedido: PedidoResumo
  onAbrirComprovante: () => void
  onAbrirFotoPedido: () => void
}

export function PedidoCard({ pedido, onAbrirComprovante, onAbrirFotoPedido }: PedidoCardProps) {
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
          className="w-full bg-emerald-600 text-white py-2.5 rounded-xl text-sm font-semibold flex items-center justify-center gap-2 hover:bg-emerald-700 transition-colors mb-2"
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

      <button
        className="w-full border border-zinc-300 bg-white text-zinc-700 py-1.5 rounded-lg text-xs font-medium flex items-center justify-center gap-1.5 hover:bg-zinc-50 transition-colors"
        onClick={onAbrirFotoPedido}
        aria-label={`Ver foto/PDF do pedido: ${pedido.descricao}`}
        style={{ minHeight: '36px' }}
      >
        <svg
          className="w-3.5 h-3.5"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
          />
        </svg>
        Ver foto/PDF do pedido
      </button>
    </div>
  )
}
