import { useState } from 'react'
import { PedidoCard } from '../components/PedidoCard'
import { StatusPedido, TipoPagamento } from '../api/tipos'
import type { PedidoResumo } from '../api/tipos'

const pedidosExemplo: PedidoResumo[] = [
  {
    id: 1,
    descricao: 'Boleto Energia Elétrica',
    valor: 287.5,
    tipo: TipoPagamento.BOLETO,
    status: StatusPedido.PAGO,
    dataPedido: '2026-05-03',
    dataPagamento: '2026-05-04',
    temComprovante: true,
  },
  {
    id: 2,
    descricao: 'TED Construtora Silva',
    valor: 4200.0,
    tipo: TipoPagamento.TED,
    status: StatusPedido.PENDENTE,
    dataPedido: '2026-05-04',
    dataPagamento: null,
    temComprovante: false,
  },
  {
    id: 3,
    descricao: 'PIX João — Aluguel sala',
    valor: 1500.0,
    tipo: TipoPagamento.PIX,
    status: StatusPedido.PAGO,
    dataPedido: '2026-05-02',
    dataPagamento: '2026-05-02',
    temComprovante: true,
  },
  {
    id: 4,
    descricao: 'Boleto IPTU — 2ª parcela (sem comprovante ainda)',
    valor: 850.3,
    tipo: TipoPagamento.BOLETO,
    status: StatusPedido.PAGO,
    dataPedido: '2026-04-28',
    dataPagamento: '2026-04-30',
    temComprovante: false,
  },
]

export function Showcase() {
  const [aberto, setAberto] = useState<number | null>(null)

  return (
    <div className="max-w-md mx-auto p-4 space-y-3">
      <div className="py-2 border-b border-zinc-100 mb-4">
        <p className="text-xs font-mono text-zinc-400">_showcase (dev only)</p>
        <h2 className="text-sm font-semibold text-zinc-700 mt-1">PedidoCard — todos os estados</h2>
      </div>

      {aberto !== null && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 m-4 max-w-sm w-full">
            <p className="text-sm font-semibold text-zinc-900 mb-2">Modal comprovante (placeholder)</p>
            <p className="text-xs text-zinc-500 mb-4">Pedido ID: {aberto}</p>
            <button
              className="w-full py-2 bg-zinc-900 text-white rounded-lg text-sm"
              onClick={() => setAberto(null)}
            >
              Fechar
            </button>
          </div>
        </div>
      )}

      {pedidosExemplo.map((pedido) => (
        <PedidoCard
          key={pedido.id}
          pedido={pedido}
          onAbrirComprovante={() => setAberto(pedido.id)}
        />
      ))}
    </div>
  )
}
