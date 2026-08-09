import { urlComprovante } from '../api/pedidos'
import { ModalArquivo } from './ModalArquivo'

interface ModalComprovanteProps {
  pedidoId: number | null
  onClose: () => void
}

export function ModalComprovante({ pedidoId, onClose }: ModalComprovanteProps) {
  return (
    <ModalArquivo
      pedidoId={pedidoId}
      onClose={onClose}
      titulo="Comprovante"
      iframeTitle="Comprovante de pagamento"
      ariaLabelFechar="Fechar comprovante"
      ariaLabelDownload="Baixar comprovante em nova aba"
      tituloDownload="Baixar comprovante"
      urlFn={urlComprovante}
    />
  )
}
