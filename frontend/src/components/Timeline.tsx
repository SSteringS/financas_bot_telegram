import { PedidoCard } from './PedidoCard'
import type { PedidoResumo } from '../api/tipos'
import { formatarDiaSemana, formatarDataRelativa, abreviarMes, diaDaMes } from '../lib/formato'
import { isToday } from 'date-fns'
import { parseISO } from 'date-fns'

interface TimelineProps {
  pedidos: PedidoResumo[]
  onAbrirComprovante: (pedidoId: number) => void
}

interface GrupoDia {
  data: string // YYYY-MM-DD
  pedidos: PedidoResumo[]
  totalValor: number
}

function agruparPorDia(pedidos: PedidoResumo[]): GrupoDia[] {
  const mapa = new Map<string, PedidoResumo[]>()
  for (const pedido of pedidos) {
    const dia = pedido.dataPedido
    if (!mapa.has(dia)) mapa.set(dia, [])
    mapa.get(dia)!.push(pedido)
  }

  // Já vêm ordenados do servidor (dataPedido DESC), mantém a ordem das chaves
  return Array.from(mapa.entries()).map(([data, pedidosDia]) => ({
    data,
    pedidos: pedidosDia,
    totalValor: pedidosDia.reduce((acc, p) => acc + p.valor, 0),
  }))
}

function HeaderDia({ data, totalValor, quantidadePedidos }: { data: string; totalValor: number; quantidadePedidos: number }) {
  const dataObj = parseISO(data)
  const hoje = isToday(dataObj)
  const abrev = abreviarMes(data)
  const dia = diaDaMes(data)

  const corFundo = hoje ? 'bg-zinc-900 text-white' : 'bg-amber-100 text-amber-900'
  const labelDia = hoje ? 'HOJE' : abrev

  return (
    <div className="mb-2 -ml-7 flex items-center gap-3 sticky top-[140px] bg-white py-2 z-[5]">
      <div
        className={`w-12 h-12 rounded-full flex flex-col items-center justify-center flex-shrink-0 border-2 border-white ${corFundo}`}
        aria-hidden="true"
      >
        <span className="text-[10px] font-medium leading-none">{labelDia}</span>
        <span className="text-sm font-bold leading-none mt-0.5">{dia}</span>
      </div>
      <div>
        <p className="text-sm font-semibold text-zinc-900">{formatarDiaSemana(data)}</p>
        {!hoje && (
          <p className="text-xs text-zinc-500">
            {quantidadePedidos} {quantidadePedidos === 1 ? 'pedido' : 'pedidos'} ·{' '}
            {totalValor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}
          </p>
        )}
        {hoje && (
          <p className="text-xs text-zinc-500">{formatarDataRelativa(data)}</p>
        )}
      </div>
    </div>
  )
}

export function Timeline({ pedidos, onAbrirComprovante }: TimelineProps) {
  const grupos = agruparPorDia(pedidos)

  return (
    <div className="relative pl-12 pr-5 py-4">
      <div
        className="absolute left-[23px] top-0 bottom-0 w-0.5 bg-zinc-200"
        aria-hidden="true"
      />
      {grupos.map((grupo) => (
        <div key={grupo.data}>
          <HeaderDia
            data={grupo.data}
            totalValor={grupo.totalValor}
            quantidadePedidos={grupo.pedidos.length}
          />
          <div className="space-y-3 ml-2 mb-4">
            {grupo.pedidos.map((pedido) => (
              <PedidoCard
                key={pedido.id}
                pedido={pedido}
                onAbrirComprovante={() => onAbrirComprovante(pedido.id)}
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}
