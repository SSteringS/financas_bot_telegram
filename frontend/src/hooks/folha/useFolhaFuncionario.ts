/**
 * useFolhaFuncionario — hook agregador das queries e mutations da Tela Folha (FE-016).
 *
 * Agrega 4 queries independentes: funcionário, vales do mês, adiantamentos ativos, fechamentos.
 * Expõe mutations para criar vale, criar adiantamento e cancelar adiantamento.
 * fecharMes() é adicionado por FE-017.
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  buscarFuncionario,
  listarVales,
  criarVale,
  listarAdiantamentos,
  criarAdiantamento,
  cancelarAdiantamento,
  listarFechamentos,
} from '../../api/folha'
import type { ValeRequest, AdiantamentoRequest } from '../../types/folha'

export function useFolhaFuncionario(funcionarioId: number, mesSelecionado: string) {
  const qc = useQueryClient()

  const funcionarioQuery = useQuery({
    queryKey: ['funcionario', funcionarioId],
    queryFn: () => buscarFuncionario(funcionarioId),
    staleTime: 60_000,
  })

  const valesQuery = useQuery({
    queryKey: ['vales', funcionarioId, mesSelecionado],
    queryFn: () => listarVales(funcionarioId, mesSelecionado),
    staleTime: 30_000,
  })

  const adiantamentosQuery = useQuery({
    queryKey: ['adiantamentos', funcionarioId],
    queryFn: () => listarAdiantamentos(funcionarioId),
    staleTime: 30_000,
  })

  const fechamentosQuery = useQuery({
    queryKey: ['fechamentos', funcionarioId],
    queryFn: () => listarFechamentos(funcionarioId),
    staleTime: 30_000,
  })

  const criarValeMutation = useMutation({
    mutationFn: (data: ValeRequest) => criarVale(funcionarioId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['vales', funcionarioId] })
    },
  })

  const criarAdiantamentoMutation = useMutation({
    mutationFn: (data: AdiantamentoRequest) => criarAdiantamento(funcionarioId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['adiantamentos', funcionarioId] })
    },
  })

  const cancelarAdiantamentoMutation = useMutation({
    mutationFn: (adiantamentoId: number) => cancelarAdiantamento(adiantamentoId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['adiantamentos', funcionarioId] })
    },
  })

  /**
   * mesFechado: true se já existe um Pedido FOLHA para o mês selecionado.
   * mesReferencia vem como "YYYY-MM-DD" (primeiro dia do mês); compara prefixo "YYYY-MM".
   */
  const mesFechado = (fechamentosQuery.data ?? []).some((f) =>
    f.mesReferencia.startsWith(mesSelecionado),
  )

  return {
    funcionario: funcionarioQuery.data,
    vales: valesQuery.data ?? [],
    adiantamentos: adiantamentosQuery.data ?? [],
    fechamentos: fechamentosQuery.data ?? [],
    isLoading:
      funcionarioQuery.isLoading ||
      valesQuery.isLoading ||
      adiantamentosQuery.isLoading ||
      fechamentosQuery.isLoading,
    mesFechado,
    criarVale: (data: ValeRequest) => criarValeMutation.mutateAsync(data),
    criarAdiantamento: (data: AdiantamentoRequest) =>
      criarAdiantamentoMutation.mutateAsync(data),
    cancelarAdiantamento: (id: number) => cancelarAdiantamentoMutation.mutateAsync(id),
    /** Invalida vales + fechamentos — chamado por FE-017 após confirmar fechamento */
    invalidarPosFechamento: () => {
      qc.invalidateQueries({ queryKey: ['fechamentos', funcionarioId] })
      qc.invalidateQueries({ queryKey: ['vales', funcionarioId] })
    },
  }
}
