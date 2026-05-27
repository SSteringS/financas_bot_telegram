import { useQuery } from '@tanstack/react-query'
import { obterResumo } from '../api/pedidos'
import type { ResumoMes } from '../api/tipos'

export function useResumo(mes: string, busca?: string) {
  return useQuery<ResumoMes>({
    queryKey: ['resumo', mes, busca ?? ''],
    queryFn: () => obterResumo({ mes, busca: busca || undefined }),
    staleTime: 60_000,
  })
}
