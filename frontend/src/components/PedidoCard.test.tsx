import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { PedidoCard } from './PedidoCard'
import { StatusPedido, TipoPagamento } from '../api/tipos'
import type { PedidoResumo } from '../api/tipos'

const pedidoPago: PedidoResumo = {
  id: 1,
  valor: 287.5,
  descricao: 'Boleto Energia Elétrica',
  tipo: TipoPagamento.BOLETO,
  status: StatusPedido.PAGO,
  dataPedido: '2026-05-08',
  dataPagamento: '2026-05-09',
  temComprovante: true,
}

const pedidoPendente: PedidoResumo = {
  id: 2,
  valor: 350,
  descricao: 'Plano de Saúde',
  tipo: TipoPagamento.BOLETO,
  status: StatusPedido.PENDENTE,
  dataPedido: '2026-05-07',
  dataPagamento: null,
  temComprovante: false,
}

const defaultCallbacks = {
  onAbrirComprovante: vi.fn(),
  onAbrirFotoPedido: vi.fn(),
}

describe('PedidoCard — botão Ver foto/PDF do pedido', () => {
  it('aparece em pedidos pagos com comprovante', () => {
    render(<PedidoCard pedido={pedidoPago} {...defaultCallbacks} />)
    expect(screen.getByRole('button', { name: /ver foto\/pdf do pedido/i })).toBeInTheDocument()
  })

  it('aparece em pedidos pendentes sem comprovante', () => {
    render(<PedidoCard pedido={pedidoPendente} {...defaultCallbacks} />)
    expect(screen.getByRole('button', { name: /ver foto\/pdf do pedido/i })).toBeInTheDocument()
  })

  it('clicar chama onAbrirFotoPedido', () => {
    const onAbrirFotoPedido = vi.fn()
    render(
      <PedidoCard
        pedido={pedidoPago}
        onAbrirComprovante={vi.fn()}
        onAbrirFotoPedido={onAbrirFotoPedido}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /ver foto\/pdf do pedido/i }))
    expect(onAbrirFotoPedido).toHaveBeenCalledOnce()
  })
})

describe('PedidoCard — botão Ver comprovante', () => {
  it('aparece quando pedido está pago e tem comprovante', () => {
    render(<PedidoCard pedido={pedidoPago} {...defaultCallbacks} />)
    expect(screen.getByRole('button', { name: /ver comprovante de/i })).toBeInTheDocument()
  })

  it('não aparece em pedidos pendentes', () => {
    render(<PedidoCard pedido={pedidoPendente} {...defaultCallbacks} />)
    expect(screen.queryByRole('button', { name: /ver comprovante de/i })).not.toBeInTheDocument()
  })

  it('clicar chama onAbrirComprovante', () => {
    const onAbrirComprovante = vi.fn()
    render(
      <PedidoCard
        pedido={pedidoPago}
        onAbrirComprovante={onAbrirComprovante}
        onAbrirFotoPedido={vi.fn()}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /ver comprovante de/i }))
    expect(onAbrirComprovante).toHaveBeenCalledOnce()
  })
})
