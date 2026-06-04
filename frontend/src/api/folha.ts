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
import type { Funcionario, FuncionarioRequest } from '../types/folha'

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

// ── Extensões futuras (FE-016) ────────────────────────────────────────────────
// listarVales(), criarVale(), listarAdiantamentos(), criarAdiantamento(),
// cancelarAdiantamento(), listarFechamentos(), fecharMes()
// serão adicionadas neste arquivo por FE-016.
