import { useQuery, useQueryClient } from '@tanstack/react-query'
import { listarPedidos } from '../api/pedidos'
import type { ListarPedidosFiltro, Pagina, PedidoResumo } from '../api/tipos'

export function usePedidos(filtros: ListarPedidosFiltro) {
  return useQuery<Pagina<PedidoResumo>>({
    queryKey: ['pedidos', filtros],
    queryFn: () => listarPedidos(filtros),
    placeholderData: (prev) => prev, // keepPreviousData equivalente em v5
    staleTime: 30_000,
  })
}

export function useInvalidarPedidos() {
  const qc = useQueryClient()
  return () => qc.invalidateQueries({ queryKey: ['pedidos'] })
}
