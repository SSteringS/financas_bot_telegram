import { useQuery } from '@tanstack/react-query'
import { obterResumo } from '../api/pedidos'
import type { ResumoMes } from '../api/tipos'

export function useResumo() {
  return useQuery<ResumoMes>({
    queryKey: ['resumo'],
    queryFn: obterResumo,
    staleTime: 60_000,
  })
}
