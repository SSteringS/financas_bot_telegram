import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ModalComprovante } from './ModalComprovante'

vi.mock('../api/pedidos', () => ({
  urlComprovante: (id: number) => `http://test-api/api/v1/pedidos/${id}/comprovante`,
}))

describe('ModalComprovante', () => {
  it('não renderiza quando pedidoId é null', () => {
    const { container } = render(<ModalComprovante pedidoId={null} onClose={vi.fn()} />)
    expect(container.firstChild).toBeNull()
  })

  it('renderiza quando pedidoId está definido', () => {
    render(<ModalComprovante pedidoId={42} onClose={vi.fn()} />)
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByTitle('Comprovante de pagamento')).toBeInTheDocument()
  })

  it('chama onClose ao clicar no backdrop', () => {
    const onClose = vi.fn()
    render(<ModalComprovante pedidoId={42} onClose={onClose} />)
    fireEvent.click(screen.getByRole('dialog'))
    expect(onClose).toHaveBeenCalled()
  })

  it('chama onClose ao clicar no botão X', () => {
    const onClose = vi.fn()
    render(<ModalComprovante pedidoId={42} onClose={onClose} />)
    fireEvent.click(screen.getByLabelText('Fechar comprovante'))
    expect(onClose).toHaveBeenCalled()
  })

  it('chama onClose ao pressionar ESC', () => {
    const onClose = vi.fn()
    render(<ModalComprovante pedidoId={42} onClose={onClose} />)
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalled()
  })

  it('iframe tem src correto', () => {
    render(<ModalComprovante pedidoId={42} onClose={vi.fn()} />)
    const iframe = screen.getByTitle('Comprovante de pagamento') as HTMLIFrameElement
    expect(iframe.src).toContain('/api/v1/pedidos/42/comprovante')
  })
})
