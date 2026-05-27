import { useEffect, useRef } from 'react'
import { urlComprovante } from '../api/pedidos'

interface ModalComprovanteProps {
  pedidoId: number | null
  onClose: () => void
}

export function ModalComprovante({ pedidoId, onClose }: ModalComprovanteProps) {
  const fecharBtnRef = useRef<HTMLButtonElement>(null)
  const triggerRef = useRef<Element | null>(null)

  useEffect(() => {
    if (pedidoId !== null) {
      // Salva elemento que disparou o modal para retornar o foco ao fechar
      triggerRef.current = document.activeElement
      // Move foco para o botão de fechar
      setTimeout(() => fecharBtnRef.current?.focus(), 50)
    } else if (triggerRef.current instanceof HTMLElement) {
      triggerRef.current.focus()
      triggerRef.current = null
    }
  }, [pedidoId])

  // Fecha com ESC
  useEffect(() => {
    if (pedidoId === null) return
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [pedidoId, onClose])

  // Bloqueia scroll do body quando modal está aberto
  useEffect(() => {
    if (pedidoId !== null) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
    return () => { document.body.style.overflow = '' }
  }, [pedidoId])

  if (pedidoId === null) return null

  const src = urlComprovante(pedidoId)

  return (
    <div
      className="fixed inset-0 bg-black/60 z-50 flex items-end sm:items-center justify-center"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-comprovante-titulo"
    >
      {/* Painel do modal */}
      <div
        className="bg-white rounded-t-2xl sm:rounded-2xl w-full sm:max-w-2xl flex flex-col"
        style={{ height: '90dvh', maxHeight: '90dvh' }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header do modal */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-zinc-100 flex-shrink-0">
          <h2
            id="modal-comprovante-titulo"
            className="text-sm font-semibold text-zinc-900"
          >
            Comprovante
          </h2>
          <button
            ref={fecharBtnRef}
            onClick={onClose}
            className="p-2 rounded-lg hover:bg-zinc-100 transition-colors"
            aria-label="Fechar comprovante"
            style={{ minWidth: '44px', minHeight: '44px' }}
          >
            <svg
              className="w-5 h-5 text-zinc-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* iframe — ocupa o espaço restante */}
        <div className="flex-1 overflow-hidden">
          <iframe
            src={src}
            className="w-full h-full border-0"
            title="Comprovante de pagamento"
            sandbox="allow-same-origin allow-scripts allow-popups"
          />
        </div>

        {/* Botão de download */}
        <div className="px-4 py-3 border-t border-zinc-100 flex-shrink-0">
          <a
            href={src}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center justify-center gap-2 w-full py-2.5 bg-emerald-600 text-white rounded-xl text-sm font-semibold hover:bg-emerald-700 transition-colors"
            aria-label="Baixar comprovante em nova aba"
            style={{ minHeight: '44px' }}
          >
            <svg
              className="w-4 h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            Baixar comprovante
          </a>
        </div>
      </div>
    </div>
  )
}
