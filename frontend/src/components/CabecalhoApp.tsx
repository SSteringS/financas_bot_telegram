import { useSearchParams, Link } from 'react-router-dom'
import { format } from 'date-fns'
import { useAuth } from '../hooks/useAuth'
import { useResumo } from '../hooks/useResumo'
import { formatarMoeda } from '../lib/formato'

export function CabecalhoApp() {
  const [searchParams] = useSearchParams()
  const mes = searchParams.get('mes') ?? format(new Date(), 'yyyy-MM')
  const busca = searchParams.get('busca') ?? ''

  const { requisitante } = useAuth()
  const { data: resumo, isLoading } = useResumo(mes, busca)

  const nomeExibido = requisitante?.nome ?? '…'
  const primeiroNome = nomeExibido.split(' ')[0]

  return (
    <header className="px-5 pt-6 pb-4 border-b border-zinc-100">
      <div className="flex items-center justify-between mb-1">
        <h1 className="text-xl font-bold text-zinc-900">Meus Pagamentos</h1>
        <Link
          to="/folha"
          className="px-3 py-1.5 text-xs font-medium text-zinc-600 border border-zinc-200 rounded-lg hover:bg-zinc-50 transition-colors"
          aria-label="Ir para folha de pagamento"
        >
          Folha
        </Link>
      </div>

      {isLoading ? (
        <div className="animate-pulse">
          <div className="h-4 bg-zinc-100 rounded w-48" />
        </div>
      ) : (
        <p className="text-sm text-zinc-500">
          {resumo ? (
            <>
              Olá {primeiroNome}.{' '}
              {resumo.pendentes.quantidade > 0 ? (
                <>
                  {resumo.pendentes.quantidade}{' '}
                  {resumo.pendentes.quantidade === 1 ? 'pedido pendente' : 'pedidos pendentes'}{' '}
                  ({formatarMoeda(resumo.pendentes.total)}).
                </>
              ) : (
                <>Nenhum pedido pendente este mês.</>
              )}
            </>
          ) : (
            <>Olá {primeiroNome}.</>
          )}
        </p>
      )}
    </header>
  )
}
