/**
 * folha.ts — funções de API para o domínio de Folha de Pagamento (EVO-09).
 *
 * Endpoints de FE-015 (funcionários):
 *   GET    /api/v1/funcionarios           → listarFuncionarios()
 *   POST   /api/v1/funcionarios           → criarFuncionario()
 *   PUT    /api/v1/funcionarios/{id}      → atualizarFuncionario()
 *   DELETE /api/v1/funcionarios/{id}      → desativarFuncionario()
 *   GET    /api/v1/funcionarios/{id}      → buscarFuncionario()
 *
 * Este arquivo será ESTENDIDO por FE-016 com funções de vales, adiantamentos e fechamentos.
 */

import { client } from './client'
import type {
  Funcionario,
  FuncionarioRequest,
  Vale,
  ValeRequest,
  Adiantamento,
  AdiantamentoRequest,
  Fechamento,
} from '../types/folha'

// ── Funcionários ──────────────────────────────────────────────────────────────

export async function listarFuncionarios(): Promise<Funcionario[]> {
  return client.get<Funcionario[]>('/api/v1/funcionarios')
}

export async function buscarFuncionario(id: number): Promise<Funcionario> {
  return client.get<Funcionario>(`/api/v1/funcionarios/${id}`)
}

export async function criarFuncionario(data: FuncionarioRequest): Promise<Funcionario> {
  return client.post<Funcionario>('/api/v1/funcionarios', data)
}

export async function atualizarFuncionario(id: number, data: FuncionarioRequest): Promise<Funcionario> {
  return client.put<Funcionario>(`/api/v1/funcionarios/${id}`, data)
}

export async function desativarFuncionario(id: number): Promise<void> {
  return client.delete<void>(`/api/v1/funcionarios/${id}`)
}

// ── Vales (FE-016) ────────────────────────────────────────────────────────────

export async function listarVales(funcionarioId: number, mes: string): Promise<Vale[]> {
  return client.get<Vale[]>(`/api/v1/funcionarios/${funcionarioId}/vales`, { mes })
}

export async function criarVale(funcionarioId: number, data: ValeRequest): Promise<Vale> {
  return client.post<Vale>(`/api/v1/funcionarios/${funcionarioId}/vales`, data)
}

// ── Adiantamentos (FE-016) ────────────────────────────────────────────────────

export async function listarAdiantamentos(funcionarioId: number): Promise<Adiantamento[]> {
  return client.get<Adiantamento[]>(`/api/v1/funcionarios/${funcionarioId}/adiantamentos`)
}

export async function criarAdiantamento(
  funcionarioId: number,
  data: AdiantamentoRequest,
): Promise<Adiantamento> {
  return client.post<Adiantamento>(`/api/v1/funcionarios/${funcionarioId}/adiantamentos`, data)
}

/** DELETE /api/v1/funcionarios/adiantamentos/{adiantamentoId} → 204 No Content */
export async function cancelarAdiantamento(adiantamentoId: number): Promise<void> {
  return client.delete<void>(`/api/v1/funcionarios/adiantamentos/${adiantamentoId}`)
}

// ── Fechamentos (FE-016) ──────────────────────────────────────────────────────

export async function listarFechamentos(funcionarioId: number): Promise<Fechamento[]> {
  return client.get<Fechamento[]>(`/api/v1/funcionarios/${funcionarioId}/fechamentos`)
}

// ── Fechar mês (FE-017) ───────────────────────────────────────────────────────

/**
 * POST /api/v1/funcionarios/{id}/fechamentos
 * Gera o Pedido FOLHA com o valor líquido calculado pelo backend.
 * @param ajuste positivo = bônus; negativo = desconto extra. Padrão: 0.
 */
export async function fecharMes(
  funcionarioId: number,
  mes: string,
  ajuste: number,
): Promise<Fechamento> {
  return client.post<Fechamento>(`/api/v1/funcionarios/${funcionarioId}/fechamentos`, {
    mes,
    ajuste,
  })
}
