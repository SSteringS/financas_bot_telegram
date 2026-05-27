import { useSearchParams } from 'react-router-dom'

const MENSAGENS: Record<string, { titulo: string; descricao: string }> = {
  'precisa-link': {
    titulo: 'Acesso necessário',
    descricao: 'Peça um novo link para o Satyan pelo WhatsApp para acessar seus pagamentos.',
  },
  'sessao-expirada': {
    titulo: 'Sessão expirada',
    descricao: 'Sua sessão expirou. Peça um novo link para o Satyan pelo WhatsApp.',
  },
  'token-invalido': {
    titulo: 'Link inválido',
    descricao: 'Esse link já foi usado ou expirou. Peça um novo link para o Satyan pelo WhatsApp.',
  },
}

const PADRAO = {
  titulo: 'Algo deu errado',
  descricao: 'Ocorreu um erro inesperado. Tente novamente ou peça ajuda para o Satyan.',
}

export function Erro() {
  const [searchParams] = useSearchParams()
  const motivo = searchParams.get('motivo') ?? ''
  const { titulo, descricao } = MENSAGENS[motivo] ?? PADRAO

  return (
    <div className="min-h-screen bg-white flex items-center justify-center px-6">
      <div className="max-w-sm w-full text-center space-y-4">
        <div className="w-16 h-16 bg-zinc-100 rounded-full flex items-center justify-center mx-auto">
          <svg
            className="w-8 h-8 text-zinc-400"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"
            />
          </svg>
        </div>
        <h1 className="text-xl font-bold text-zinc-900">{titulo}</h1>
        <p className="text-sm text-zinc-500 leading-relaxed">{descricao}</p>
      </div>
    </div>
  )
}
