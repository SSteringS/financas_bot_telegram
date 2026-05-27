import { useState } from 'react'
import { format } from 'date-fns'
import { PedidoCard } from '../components/PedidoCard'
import { FiltroStatus } from '../components/FiltroStatus'
import { SeletorMes } from '../components/SeletorMes'
import { BarraBusca } from '../components/BarraBusca'
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
    descricao: 'Boleto IPTU — 2ª parcela (pago, sem comprovante)',
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
  const [filtroStatus, setFiltroStatus] = useState<'TUDO' | 'PENDENTE' | 'PAGO'>('TUDO')
  const [mes, setMes] = useState(format(new Date(), 'yyyy-MM'))
  const [busca, setBusca] = useState('')

  return (
    <div className="max-w-md mx-auto min-h-screen bg-white">
      <div className="px-4 py-3 border-b border-zinc-100">
        <p className="text-xs font-mono text-zinc-400">_showcase (dev only)</p>
        <h2 className="text-sm font-semibold text-zinc-700 mt-1">Componentes — Fase 3</h2>
      </div>

      {/* SeletorMes */}
      <div className="px-4 py-3 border-b border-zinc-100 sticky top-0 bg-white z-10">
        <p className="text-xs text-zinc-400 mb-2">SeletorMes — valor: {mes}</p>
        <SeletorMes value={mes} onChange={setMes} />
      </div>

      {/* FiltroStatus */}
      <div className="px-4 py-3 border-b border-zinc-100">
        <p className="text-xs text-zinc-400 mb-2">FiltroStatus — valor: {filtroStatus}</p>
        <FiltroStatus
          value={filtroStatus}
          onChange={setFiltroStatus}
          contadores={{ tudo: 12, pendente: 3, pago: 9 }}
        />
      </div>

      {/* BarraBusca */}
      <div className="px-4 py-3 border-b border-zinc-100">
        <p className="text-xs text-zinc-400 mb-2">BarraBusca — valor: "{busca}"</p>
        <BarraBusca value={busca} onChange={setBusca} />
      </div>

      {/* PedidoCards */}
      <div className="px-4 py-3 space-y-3">
        <p className="text-xs text-zinc-400 mb-1">PedidoCard — todos os estados</p>
        {pedidosExemplo.map((pedido) => (
          <PedidoCard
            key={pedido.id}
            pedido={pedido}
            onAbrirComprovante={() => setAberto(pedido.id)}
          />
        ))}
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
    </div>
  )
}
