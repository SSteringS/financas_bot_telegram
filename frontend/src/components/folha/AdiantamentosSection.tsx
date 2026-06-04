/**
 * AdiantamentosSection — exibe adiantamentos ativos do funcionário (FE-016).
 *
 * Props: recebe dados prontos do useFolhaFuncionario hook.
 */

import { useState } from 'react'
import { formatarMoeda } from '../../lib/formato'
import { AdiantamentoForm } from './AdiantamentoForm'
import type { Adiantamento, AdiantamentoRequest } from '../../types/folha'

interface AdiantamentosSectionProps {
  adiantamentos: Adiantamento[]
  onCriarAdiantamento: (data: AdiantamentoRequest) => Promise<unknown>
  onCancelarAdiantamento: (id: number) => Promise<unknown>
}

export function AdiantamentosSection({
  adiantamentos,
  onCriarAdiantamento,
  onCancelarAdiantamento,
}: AdiantamentosSectionProps) {
  const [formAberto, setFormAberto] = useState(false)
  const [cancelando, setCancelando] = useState<number | null>(null)

  async function handleCriarAdiantamento(data: AdiantamentoRequest) {
    await onCriarAdiantamento(data)
    setFormAberto(false)
  }

  async function handleCancelar(id: number) {
    if (!confirm('Cancelar este adiantamento? As parcelas restantes não serão descontadas.')) {
      return
    }
    setCancelando(id)
    try {
      await onCancelarAdiantamento(id)
    } finally {
      setCancelando(null)
    }
  }

  return (
    <section aria-labelledby="adt-titulo" data-testid="adiantamentos-section">
      {/* Cabeçalho */}
      <div className="flex items-center justify-between mb-3">
        <h2 id="adt-titulo" className="text-base font-semibold text-zinc-800">
          Adiantamentos ativos
        </h2>
        <button
          onClick={() => setFormAberto((v) => !v)}
          className="text-xs font-medium text-blue-600 hover:text-blue-800 transition-colors"
          aria-label="Novo adiantamento"
        >
          + Novo adiantamento
        </button>
      </div>

      {/* Formulário inline */}
      {formAberto && (
        <div className="mb-3">
          <AdiantamentoForm
            onSubmit={handleCriarAdiantamento}
            onCancelar={() => setFormAberto(false)}
          />
        </div>
      )}

      {/* Lista */}
      {adiantamentos.length === 0 ? (
        <p className="text-sm text-zinc-400 py-4 text-center" data-testid="adiantamentos-vazio">
          Nenhum adiantamento ativo.
        </p>
      ) : (
        <ul className="space-y-2">
          {adiantamentos.map((adt) => (
            <li
              key={adt.id}
              data-testid={`adiantamento-item-${adt.id}`}
              className="bg-white border border-zinc-100 rounded-xl px-4 py-3"
            >
              <div className="flex items-start justify-between gap-2">
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-zinc-800 truncate">{adt.descricao}</p>
                  <p className="text-xs text-zinc-400 mt-0.5">
                    Parcela{' '}
                    <span
                      className="font-semibold text-zinc-600"
                      data-testid={`parcela-${adt.id}`}
                    >
                      {adt.parcelasPagas + 1}/{adt.numParcelas}
                    </span>{' '}
                    · {formatarMoeda(adt.valorParcela)}/mês
                  </p>
                </div>
                <div className="flex flex-col items-end gap-1 shrink-0">
                  <span className="text-sm font-semibold text-zinc-800">
                    {formatarMoeda(adt.valorTotal)}
                  </span>
                  <button
                    onClick={() => handleCancelar(adt.id)}
                    disabled={cancelando === adt.id}
                    className="text-xs text-red-500 hover:text-red-700 disabled:opacity-50 transition-colors"
                    aria-label={`Cancelar adiantamento ${adt.descricao}`}
                    data-testid={`btn-cancelar-${adt.id}`}
                  >
                    {cancelando === adt.id ? 'Cancelando…' : 'Cancelar'}
                  </button>
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
