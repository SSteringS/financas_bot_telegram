import { client } from './client'
import { API_BASE_URL } from '../lib/ambiente'
import type { PedidoResumo, PedidoDetalhe, Pagina, ResumoMes, ListarPedidosFiltro } from './tipos'

export async function listarPedidos(filtros: ListarPedidosFiltro): Promise<Pagina<PedidoResumo>> {
  const params: Record<string, string | number | boolean | undefined> = {}
  if (filtros.status) params['status'] = filtros.status
  if (filtros.de) params['de'] = filtros.de
  if (filtros.ate) params['ate'] = filtros.ate
  if (filtros.busca) params['busca'] = filtros.busca
  if (filtros.page !== undefined) params['page'] = filtros.page
  if (filtros.tamanho !== undefined) params['tamanho'] = filtros.tamanho

  // tipo pode ser array — adicionar cada um separado
  if (filtros.tipo) {
    const tipos = Array.isArray(filtros.tipo) ? filtros.tipo : [filtros.tipo]
    tipos.forEach((t) => {
      params['tipo'] = t // último sobrescreve, mas para API real usa múltiplos params
    })
  }

  return client.get<Pagina<PedidoResumo>>('/api/v1/pedidos', params)
}

export async function buscarPedido(id: number): Promise<PedidoDetalhe> {
  return client.get<PedidoDetalhe>(`/api/v1/pedidos/${id}`)
}

export function urlFotoPedido(id: number): string {
  return `${API_BASE_URL}/api/v1/pedidos/${id}/foto-pedido`
}

export function urlComprovante(id: number): string {
  return `${API_BASE_URL}/api/v1/pedidos/${id}/comprovante`
}

export async function obterResumo(): Promise<ResumoMes> {
  return client.get<ResumoMes>('/api/v1/resumo')
}
