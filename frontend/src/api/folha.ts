/**
 * folha.ts — funções de API para o domínio de Folha de Pagamento (EVO-09).
 *
 * Endpoints de FE-015 (funcionários):
 *   GET    /api/funcionarios           → listarFuncionarios()
 *   POST   /api/funcionarios           → criarFuncionario()
 *   PUT    /api/funcionarios/{id}      → atualizarFuncionario()
 *   DELETE /api/funcionarios/{id}      → desativarFuncionario()
 *   GET    /api/funcionarios/{id}      → buscarFuncionario()
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
  return client.get<Funcionario[]>('/api/funcionarios')
}

export async function buscarFuncionario(id: number): Promise<Funcionario> {
  return client.get<Funcionario>(`/api/funcionarios/${id}`)
}

export async function criarFuncionario(data: FuncionarioRequest): Promise<Funcionario> {
  return client.post<Funcionario>('/api/funcionarios', data)
}

export async function atualizarFuncionario(id: number, data: FuncionarioRequest): Promise<Funcionario> {
  return client.put<Funcionario>(`/api/funcionarios/${id}`, data)
}

export async function desativarFuncionario(id: number): Promise<void> {
  return client.delete<void>(`/api/funcionarios/${id}`)
}

// ── Vales (FE-016) ────────────────────────────────────────────────────────────

export async function listarVales(funcionarioId: number, mes: string): Promise<Vale[]> {
  return client.get<Vale[]>(`/api/funcionarios/${funcionarioId}/vales`, { mes })
}

export async function criarVale(funcionarioId: number, data: ValeRequest): Promise<Vale> {
  return client.post<Vale>(`/api/funcionarios/${funcionarioId}/vales`, data)
}

// ── Adiantamentos (FE-016) ────────────────────────────────────────────────────

export async function listarAdiantamentos(funcionarioId: number): Promise<Adiantamento[]> {
  return client.get<Adiantamento[]>(`/api/funcionarios/${funcionarioId}/adiantamentos`)
}

export async function criarAdiantamento(
  funcionarioId: number,
  data: AdiantamentoRequest,
): Promise<Adiantamento> {
  return client.post<Adiantamento>(`/api/funcionarios/${funcionarioId}/adiantamentos`, data)
}

/** DELETE /api/funcionarios/adiantamentos/{adiantamentoId} → 204 No Content */
export async function cancelarAdiantamento(adiantamentoId: number): Promise<void> {
  return client.delete<void>(`/api/funcionarios/adiantamentos/${adiantamentoId}`)
}

// ── Fechamentos (FE-016) ──────────────────────────────────────────────────────

export async function listarFechamentos(funcionarioId: number): Promise<Fechamento[]> {
  return client.get<Fechamento[]>(`/api/funcionarios/${funcionarioId}/fechamentos`)
}

// fecharMes() será adicionado por FE-017 (POST /api/funcionarios/{id}/fechamentos)
