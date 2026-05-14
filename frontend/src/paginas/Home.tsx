import { useState, useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'
import { format, startOfMonth, endOfMonth, parseISO } from 'date-fns'
import { usePedidos } from '../hooks/usePedidos'
import { SeletorMes } from '../components/SeletorMes'
import { FiltroStatus } from '../components/FiltroStatus'
import { BarraBusca } from '../components/BarraBusca'
import { Timeline } from '../components/Timeline'
import { CarregandoLista } from '../components/CarregandoLista'
import { ListaVazia } from '../components/ListaVazia'
import { ModalComprovante } from '../components/ModalComprovante'
import { CabecalhoApp } from '../components/CabecalhoApp'
import type { Pagina, PedidoResumo } from '../api/tipos'

type FiltroStatusValue = 'TUDO' | 'PENDENTE' | 'PAGO'

function mesAtual(): string {
  return format(new Date(), 'yyyy-MM')
}

function calcularDates(mes: string): { de: string; ate: string } {
  const dataBase = parseISO(`${mes}-01`)
  return {
    de: format(startOfMonth(dataBase), 'yyyy-MM-dd'),
    ate: format(endOfMonth(dataBase), 'yyyy-MM-dd'),
  }
}

function statusParaApi(valor: FiltroStatusValue): 'pendente' | 'pago' | 'todos' {
  if (valor === 'PENDENTE') return 'pendente'
  if (valor === 'PAGO') return 'pago'
  return 'todos'
}

export function Home() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [pedidoIdAberto, setPedidoIdAberto] = useState<number | null>(null)
  const [pagina, setPagina] = useState(0)

  const mes = searchParams.get('mes') ?? mesAtual()
  const filtroStatus = (searchParams.get('status') as FiltroStatusValue) ?? 'TUDO'
  const busca = searchParams.get('busca') ?? ''

  const { de, ate } = calcularDates(mes)

  const { data, isFetching, isError, refetch } = usePedidos({
    status: statusParaApi(filtroStatus),
    de,
    ate,
    busca: busca || undefined,
    page: pagina,
    tamanho: 20,
  })

  const atualizarParam = useCallback(
    (chave: string, valor: string) => {
      setSearchParams((prev) => {
        const novo = new URLSearchParams(prev)
        if (valor) {
          novo.set(chave, valor)
        } else {
          novo.delete(chave)
        }
        return novo
      })
      setPagina(0)
    },
    [setSearchParams],
  )

  const handleMes = useCallback((novoMes: string) => atualizarParam('mes', novoMes), [atualizarParam])
  const handleStatus = useCallback((novoStatus: FiltroStatusValue) => atualizarParam('status', novoStatus), [atualizarParam])
  const handleBusca = useCallback((novaBusca: string) => atualizarParam('busca', novaBusca), [atualizarParam])

  const todosOsPedidos: PedidoResumo[] = data?.items ?? []
  const temMais = data ? pagina < (data as Pagina<PedidoResumo>).totalPaginas - 1 : false

  const contadores = {
    tudo: data?.total ?? 0,
    pendente: data?.items.filter((p) => p.status === 'PENDENTE').length ?? 0,
    pago: data?.items.filter((p) => p.status === 'PAGO').length ?? 0,
  }

  return (
    <div className="max-w-md mx-auto bg-white min-h-screen pb-24">
      {/* Cabeçalho com saudação e resumo */}
      <CabecalhoApp />

      {/* Seletor de mês — sticky */}
      <div className="px-5 py-4 sticky top-0 bg-white border-b border-zinc-100 z-10">
        <SeletorMes value={mes} onChange={handleMes} />
      </div>

      {/* Filtro de status */}
      <div className="px-5 py-3 border-b border-zinc-100">
        <FiltroStatus
          value={filtroStatus}
          onChange={handleStatus}
          contadores={contadores}
        />
      </div>

      {/* Barra de busca */}
      <div className="px-5 py-3 border-b border-zinc-100">
        <BarraBusca value={busca} onChange={handleBusca} />
      </div>

      {/* Carregando initial load */}
      {isFetching && !data && <CarregandoLista />}

      {/* Erro */}
      {isError && (
        <div className="flex flex-col items-center justify-center py-16 px-8 text-center">
          <p className="text-sm text-zinc-500 mb-4">Não foi possível carregar os pedidos.</p>
          <button
            className="px-4 py-2 bg-zinc-900 text-white text-sm rounded-lg"
            onClick={() => refetch()}
          >
            Tentar novamente
          </button>
        </div>
      )}

      {/* Lista */}
      {!isError && (
        <>
          {todosOsPedidos.length === 0 && !isFetching ? (
            <ListaVazia />
          ) : (
            <Timeline
              pedidos={todosOsPedidos}
              onAbrirComprovante={setPedidoIdAberto}
            />
          )}

          {/* Carregar mais */}
          {temMais && (
            <div className="px-5 pb-6">
              <button
                className="w-full py-3 border border-zinc-200 rounded-xl text-sm text-zinc-600 font-medium hover:bg-zinc-50 transition-colors"
                onClick={() => setPagina((p) => p + 1)}
                disabled={isFetching}
              >
                {isFetching ? 'Carregando…' : 'Carregar mais'}
              </button>
            </div>
          )}
        </>
      )}

      {/* Modal de comprovante */}
      <ModalComprovante
        pedidoId={pedidoIdAberto}
        onClose={() => setPedidoIdAberto(null)}
      />
    </div>
  )
}
