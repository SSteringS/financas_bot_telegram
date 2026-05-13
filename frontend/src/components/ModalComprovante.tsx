// Stub mínimo — implementação completa em FE-09
interface ModalComprovanteProps {
  pedidoId: number | null
  onClose: () => void
}

export function ModalComprovante({ pedidoId, onClose }: ModalComprovanteProps) {
  if (pedidoId === null) return null

  return (
    <div
      className="fixed inset-0 bg-black/60 z-50 flex items-end sm:items-center justify-center"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label="Comprovante"
    >
      <div
        className="bg-white rounded-t-2xl sm:rounded-2xl w-full sm:max-w-lg p-6"
        onClick={(e) => e.stopPropagation()}
      >
        <p className="text-sm text-zinc-500">Comprovante — pedido {pedidoId}</p>
        <p className="text-xs text-zinc-400 mt-1">Implementação completa em FE-09</p>
        <button
          className="mt-4 w-full py-2 bg-zinc-900 text-white rounded-lg text-sm"
          onClick={onClose}
        >
          Fechar
        </button>
      </div>
    </div>
  )
}
