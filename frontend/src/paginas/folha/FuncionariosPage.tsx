import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { listarFuncionarios, criarFuncionario, atualizarFuncionario, desativarFuncionario } from '../../api/folha'
import { FuncionarioForm } from '../../components/folha/FuncionarioForm'
import { formatarMoeda } from '../../lib/formato'
import type { Funcionario, FuncionarioRequest } from '../../types/folha'

type PainelAberto = { tipo: 'novo' } | { tipo: 'editar'; funcionario: Funcionario } | null

export function FuncionariosPage() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [painelAberto, setPainelAberto] = useState<PainelAberto>(null)
  const [confirmandoDesativar, setConfirmandoDesativar] = useState<number | null>(null)

  const { data: funcionarios, isLoading, isError, refetch } = useQuery<Funcionario[]>({
    queryKey: ['funcionarios'],
    queryFn: listarFuncionarios,
    staleTime: 30_000,
  })

  async function handleSubmit(data: FuncionarioRequest) {
    if (painelAberto?.tipo === 'editar') {
      await atualizarFuncionario(painelAberto.funcionario.id, data)
    } else {
      await criarFuncionario(data)
    }
    qc.invalidateQueries({ queryKey: ['funcionarios'] })
    setPainelAberto(null)
  }

  async function handleDesativar(id: number) {
    await desativarFuncionario(id)
    qc.invalidateQueries({ queryKey: ['funcionarios'] })
    setConfirmandoDesativar(null)
  }

  return (
    <div className="max-w-md mx-auto bg-white min-h-screen pb-24">
      {/* Cabeçalho */}
      <header className="px-5 pt-6 pb-4 border-b border-zinc-100">
        <div className="flex items-center gap-3 mb-1">
          <button
            onClick={() => navigate('/')}
            className="p-1.5 rounded-lg hover:bg-zinc-100 transition-colors"
            aria-label="Voltar para início"
          >
            <svg className="w-5 h-5 text-zinc-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <h1 className="text-xl font-bold text-zinc-900">Folha de pagamento</h1>
        </div>
        <p className="text-sm text-zinc-500 pl-8">Gerencie funcionários domésticos</p>
      </header>

      {/* Formulário (painel inline) */}
      {painelAberto && (
        <div className="px-5 py-4 border-b border-zinc-100 bg-zinc-50">
          <h2 className="text-sm font-semibold text-zinc-900 mb-4">
            {painelAberto.tipo === 'novo' ? 'Novo funcionário' : `Editar — ${painelAberto.funcionario.nome}`}
          </h2>
          <FuncionarioForm
            funcionario={painelAberto.tipo === 'editar' ? painelAberto.funcionario : undefined}
            onSubmit={handleSubmit}
            onCancelar={() => setPainelAberto(null)}
          />
        </div>
      )}

      {/* Botão novo */}
      {!painelAberto && (
        <div className="px-5 py-4">
          <button
            onClick={() => setPainelAberto({ tipo: 'novo' })}
            className="w-full py-3 bg-zinc-900 text-white rounded-xl text-sm font-semibold hover:bg-zinc-700 transition-colors flex items-center justify-center gap-2"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Novo funcionário
          </button>
        </div>
      )}

      {/* Loading */}
      {isLoading && (
        <div className="px-5 py-8 text-center text-sm text-zinc-400">Carregando…</div>
      )}

      {/* Erro */}
      {isError && (
        <div className="flex flex-col items-center py-12 px-5 text-center">
          <p className="text-sm text-zinc-500 mb-4">Não foi possível carregar os funcionários.</p>
          <button
            onClick={() => refetch()}
            className="px-4 py-2 bg-zinc-900 text-white text-sm rounded-lg"
          >
            Tentar novamente
          </button>
        </div>
      )}

      {/* Lista de funcionários */}
      {!isLoading && !isError && (
        <>
          {(!funcionarios || funcionarios.length === 0) ? (
            <div className="px-5 py-12 text-center text-sm text-zinc-400">
              Nenhum funcionário cadastrado.
            </div>
          ) : (
            <ul className="divide-y divide-zinc-100" role="list">
              {funcionarios.map((func) => (
                <li key={func.id}>
                  {/* Confirmação de desativação */}
                  {confirmandoDesativar === func.id ? (
                    <div className="px-5 py-4 bg-red-50">
                      <p className="text-sm text-red-700 mb-3">
                        Desativar <strong>{func.nome}</strong>? Esta ação pode ser revertida pela equipe de suporte.
                      </p>
                      <div className="flex gap-2">
                        <button
                          onClick={() => setConfirmandoDesativar(null)}
                          className="flex-1 py-2 border border-zinc-300 rounded-lg text-sm text-zinc-700 hover:bg-white transition-colors"
                        >
                          Cancelar
                        </button>
                        <button
                          onClick={() => handleDesativar(func.id)}
                          className="flex-1 py-2 bg-red-600 text-white rounded-lg text-sm font-medium hover:bg-red-700 transition-colors"
                        >
                          Confirmar desativação
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="px-5 py-4">
                      {/* Linha clicável → navega para FE-016 */}
                      <button
                        className="w-full text-left"
                        onClick={() => navigate(`/folha/funcionarios/${func.id}`)}
                        aria-label={`Ver folha de ${func.nome}`}
                      >
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className="text-sm font-semibold text-zinc-900 truncate">
                                {func.nome}
                              </span>
                              {/* Badge laranja para conta de terceiro */}
                              {!func.contaPropria && (
                                <span className="px-2 py-0.5 bg-orange-100 text-orange-700 text-[10px] font-bold uppercase rounded shrink-0">
                                  Terceiro
                                </span>
                              )}
                            </div>
                            <p className="text-xs text-zinc-500 mt-0.5">
                              Salário: {formatarMoeda(func.salarioBase)} · {func.formaPagamento}
                            </p>
                          </div>
                          <svg className="w-4 h-4 text-zinc-400 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                          </svg>
                        </div>
                      </button>

                      {/* Ações */}
                      <div className="flex gap-2 mt-2">
                        <button
                          onClick={(e) => {
                            e.stopPropagation()
                            setPainelAberto({ tipo: 'editar', funcionario: func })
                          }}
                          className="px-3 py-1.5 text-xs font-medium text-zinc-600 border border-zinc-200 rounded-lg hover:bg-zinc-50 transition-colors"
                          aria-label={`Editar ${func.nome}`}
                        >
                          Editar
                        </button>
                        <button
                          onClick={(e) => {
                            e.stopPropagation()
                            setConfirmandoDesativar(func.id)
                          }}
                          className="px-3 py-1.5 text-xs font-medium text-red-600 border border-red-200 rounded-lg hover:bg-red-50 transition-colors"
                          aria-label={`Desativar ${func.nome}`}
                        >
                          Desativar
                        </button>
                      </div>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  )
}
