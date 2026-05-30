import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { format, subDays } from 'date-fns'
import { Timeline } from './Timeline'
import { StatusPedido, TipoPagamento } from '../api/tipos'
import type { PedidoResumo } from '../api/tipos'

const hoje = format(new Date(), 'yyyy-MM-dd')
const ontem = format(subDays(new Date(), 1), 'yyyy-MM-dd')

let nextId = 1
function makePedido(overrides: Partial<PedidoResumo> & { dataPedido: string }): PedidoResumo {
  return {
    id: nextId++,
    valor: 100,
    descricao: 'Teste',
    tipo: TipoPagamento.PIX,
    status: StatusPedido.PAGO,
    dataPagamento: null,
    temComprovante: false,
    ...overrides,
  }
}

describe('Timeline', () => {
  it('3 pedidos em 2 datas distintas → renderiza 2 grupos', () => {
    const pedidos = [
      makePedido({ dataPedido: '2026-05-08' }),
      makePedido({ dataPedido: '2026-05-05' }),
      makePedido({ dataPedido: '2026-05-05' }),
    ]
    render(<Timeline pedidos={pedidos} onAbrirComprovante={vi.fn()} onAbrirFotoPedido={vi.fn()} />)
    const headers = screen.getAllByText(/de maio/)
    expect(headers).toHaveLength(2)
  })

  it('pedido com dataPedido = hoje → label "Hoje"', () => {
    const pedidos = [makePedido({ dataPedido: hoje })]
    render(<Timeline pedidos={pedidos} onAbrirComprovante={vi.fn()} onAbrirFotoPedido={vi.fn()} />)
    expect(screen.getByText('Hoje')).toBeInTheDocument()
  })

  it('pedido com dataPedido = ontem → label "Ontem"', () => {
    const pedidos = [makePedido({ dataPedido: ontem })]
    render(<Timeline pedidos={pedidos} onAbrirComprovante={vi.fn()} onAbrirFotoPedido={vi.fn()} />)
    expect(screen.getByText('Ontem')).toBeInTheDocument()
  })

  it('lista vazia → renderiza sem quebrar', () => {
    const { container } = render(<Timeline pedidos={[]} onAbrirComprovante={vi.fn()} onAbrirFotoPedido={vi.fn()} />)
    expect(container.firstChild).toBeTruthy()
  })
})
