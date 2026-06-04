/**
 * FechamentosSection — exibe histórico de fechamentos mensais (Pedidos FOLHA) em accordion (FE-016).
 *
 * Props: recebe dados prontos do useFolhaFuncionario hook.
 * Ordenação por mes_referencia DESC assumida (backend já retorna nessa ordem).
 */

import { useState } from 'react'
import { format, parseISO } from 'date-fns'
import { ptBR } from 'date-fns/locale'
import { formatarMoeda } from '../../lib/formato'
import type { Fechamento } from '../../types/folha'

interface FechamentosSectionProps {
  fechamentos: Fechamento[]
}

const STATUS_BADGE: Record<string, string> = {
  PENDENTE: 'bg-yellow-100 text-yellow-700',
  PAGO: 'bg-green-100 text-green-700',
  CANCELADO: 'bg-zinc-100 text-zinc-500',
}

export function FechamentosSection({ fechamentos }: FechamentosSectionProps) {
  const [expandido, setExpandido] = useState<number | null>(null)

  function toggleExpandir(id: number) {
    setExpandido((prev) => (prev === id ? null : id))
  }

  return (
    <section aria-labelledby="fechamentos-titulo" data-testid="fechamentos-section">
      <h2 id="fechamentos-titulo" className="text-base font-semibold text-zinc-800 mb-3">
        Fechamentos anteriores
      </h2>

      {fechamentos.length === 0 ? (
        <p className="text-sm text-zinc-400 py-4 text-center" data-testid="fechamentos-vazio">
          Nenhum fechamento realizado.
        </p>
      ) : (
        <ul className="space-y-2">
          {fechamentos.map((f) => {
            const aberto = expandido === f.id
            const mesLabel = format(parseISO(f.mesReferencia), "MMMM 'de' yyyy", { locale: ptBR })

            return (
              <li
                key={f.id}
                data-testid={`fechamento-item-${f.id}`}
                className="bg-white border border-zinc-100 rounded-xl overflow-hidden"
              >
                {/* Linha principal — clique abre/fecha */}
                <button
                  onClick={() => toggleExpandir(f.id)}
                  aria-expanded={aberto}
                  className="w-full flex items-center justify-between px-4 py-3 text-left hover:bg-zinc-50 transition-colors"
                  data-testid={`btn-expandir-${f.id}`}
                >
                  <div>
                    <p className="text-sm font-medium text-zinc-800 capitalize">{mesLabel}</p>
                    <span
                      className={`mt-0.5 inline-block text-xs font-medium px-2 py-0.5 rounded-full ${STATUS_BADGE[f.status] ?? 'bg-zinc-100 text-zinc-500'}`}
                    >
                      {f.status}
                    </span>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <span className="text-sm font-semibold text-zinc-800">
                      {formatarMoeda(f.valor)}
                    </span>
                    <svg
                      className={`w-4 h-4 text-zinc-400 transition-transform ${aberto ? 'rotate-180' : ''}`}
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                      aria-hidden="true"
                    >
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </div>
                </button>

                {/* Observação expandida */}
                {aberto && (
                  <div
                    className="border-t border-zinc-100 px-4 py-3 bg-zinc-50"
                    data-testid={`observacao-${f.id}`}
                  >
                    {f.observacao ? (
                      <p className="text-xs text-zinc-600 whitespace-pre-line">{f.observacao}</p>
                    ) : (
                      <p className="text-xs text-zinc-400 italic">Sem detalhes disponíveis.</p>
                    )}
                  </div>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
