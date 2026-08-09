import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ModalArquivo } from './ModalArquivo'

const defaultProps = {
  titulo: 'Arquivo de teste',
  iframeTitle: 'Iframe de teste',
  ariaLabelFechar: 'Fechar arquivo',
  ariaLabelDownload: 'Baixar arquivo',
  tituloDownload: 'Baixar',
  urlFn: (id: number) => `http://test-api/arquivo/${id}`,
}

describe('ModalArquivo', () => {
  it('não renderiza quando pedidoId é null', () => {
    const { container } = render(
      <ModalArquivo {...defaultProps} pedidoId={null} onClose={vi.fn()} />,
    )
    expect(container.firstChild).toBeNull()
  })

  it('renderiza quando pedidoId está definido', () => {
    render(<ModalArquivo {...defaultProps} pedidoId={42} onClose={vi.fn()} />)
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByTitle('Iframe de teste')).toBeInTheDocument()
    expect(screen.getByText('Arquivo de teste')).toBeInTheDocument()
  })

  it('chama onClose ao clicar no backdrop', () => {
    const onClose = vi.fn()
    render(<ModalArquivo {...defaultProps} pedidoId={42} onClose={onClose} />)
    fireEvent.click(screen.getByRole('dialog'))
    expect(onClose).toHaveBeenCalled()
  })

  it('chama onClose ao clicar no botão X', () => {
    const onClose = vi.fn()
    render(<ModalArquivo {...defaultProps} pedidoId={42} onClose={onClose} />)
    fireEvent.click(screen.getByLabelText('Fechar arquivo'))
    expect(onClose).toHaveBeenCalled()
  })

  it('chama onClose ao pressionar ESC', () => {
    const onClose = vi.fn()
    render(<ModalArquivo {...defaultProps} pedidoId={42} onClose={onClose} />)
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalled()
  })

  it('iframe tem src correto via urlFn', () => {
    render(<ModalArquivo {...defaultProps} pedidoId={42} onClose={vi.fn()} />)
    const iframe = screen.getByTitle('Iframe de teste') as HTMLIFrameElement
    expect(iframe.src).toContain('/arquivo/42')
  })

  it('chama urlFn com o pedidoId correto', () => {
    const urlFn = vi.fn((id: number) => `http://test-api/arquivo/${id}`)
    render(<ModalArquivo {...defaultProps} pedidoId={99} onClose={vi.fn()} urlFn={urlFn} />)
    expect(urlFn).toHaveBeenCalledWith(99)
  })

  it('exibe o titulo personalizado no heading', () => {
    render(<ModalArquivo {...defaultProps} titulo="Foto do pedido" pedidoId={1} onClose={vi.fn()} />)
    expect(screen.getByText('Foto do pedido')).toBeInTheDocument()
  })
})
