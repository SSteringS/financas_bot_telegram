import { useAuth } from '../hooks/useAuth'
import { useResumo } from '../hooks/useResumo'
import { formatarMoeda } from '../lib/formato'

export function CabecalhoApp() {
  const { requisitante } = useAuth()
  const { data: resumo, isLoading } = useResumo()

  const nomeExibido = requisitante?.nome ?? '…'
  const primeiroNome = nomeExibido.split(' ')[0]

  return (
    <header className="px-5 pt-6 pb-4 border-b border-zinc-100">
      <div className="flex items-center justify-between mb-1">
        <h1 className="text-xl font-bold text-zinc-900">Meus Pagamentos</h1>
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
