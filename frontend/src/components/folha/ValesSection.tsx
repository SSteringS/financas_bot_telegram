/**
 * ValesSection — exibe vales do mês selecionado + seletor de mês + botão registrar vale (FE-016).
 *
 * Props: recebe dados prontos do useFolhaFuncionario hook (sem query interna).
 */

import { useState } from 'react'
import { format, parseISO } from 'date-fns'
import { ptBR } from 'date-fns/locale'
import { formatarMoeda } from '../../lib/formato'
import { ValeForm } from './ValeForm'
import type { Vale, ValeRequest } from '../../types/folha'

interface ValesSectionProps {
  vales: Vale[]
  mesSelecionado: string          // "YYYY-MM"
  onMesChange: (mes: string) => void
  onCriarVale: (data: ValeRequest) => Promise<unknown>
}

/** Gera os últimos N meses no formato "YYYY-MM" (mais recente primeiro) */
function gerarMeses(n: number): { valor: string; label: string }[] {
  const hoje = new Date()
  return Array.from({ length: n }, (_, i) => {
    const d = new Date(hoje.getFullYear(), hoje.getMonth() - i, 1)
    const valor = format(d, 'yyyy-MM')
    const label = format(d, "MMM/yyyy", { locale: ptBR }).replace('.', '')
    return { valor, label }
  })
}

const MESES_OPCOES = gerarMeses(6)

export function ValesSection({ vales, mesSelecionado, onMesChange, onCriarVale }: ValesSectionProps) {
  const [formAberto, setFormAberto] = useState(false)

  async function handleCriarVale(data: ValeRequest) {
    await onCriarVale(data)
    setFormAberto(false)
  }

  return (
    <section aria-labelledby="vales-titulo" data-testid="vales-section">
      {/* Cabeçalho da seção */}
      <div className="flex items-center justify-between mb-3">
        <h2 id="vales-titulo" className="text-base font-semibold text-zinc-800">
          Vales do mês
        </h2>
        <button
          onClick={() => setFormAberto((v) => !v)}
          className="text-xs font-medium text-blue-600 hover:text-blue-800 transition-colors"
          aria-label="Registrar vale"
        >
          + Registrar vale
        </button>
      </div>

      {/* Seletor de mês */}
      <div className="mb-3">
        <select
          value={mesSelecionado}
          onChange={(e) => onMesChange(e.target.value)}
          aria-label="Mês de referência"
          className="text-sm rounded-lg border border-zinc-200 px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
        >
          {MESES_OPCOES.map(({ valor, label }) => (
            <option key={valor} value={valor}>
              {label}
            </option>
          ))}
        </select>
      </div>

      {/* Formulário inline */}
      {formAberto && (
        <div className="mb-3">
          <ValeForm onSubmit={handleCriarVale} onCancelar={() => setFormAberto(false)} />
        </div>
      )}

      {/* Lista de vales */}
      {vales.length === 0 ? (
        <p className="text-sm text-zinc-400 py-4 text-center" data-testid="vales-vazio">
          Nenhum vale registrado neste mês.
        </p>
      ) : (
        <ul className="space-y-2">
          {vales.map((vale) => (
            <li
              key={vale.id}
              data-testid={`vale-item-${vale.id}`}
              className={`flex items-center justify-between rounded-xl px-4 py-3 ${
                vale.fechado
                  ? 'bg-zinc-50 text-zinc-400'
                  : 'bg-white border border-zinc-100 text-zinc-700'
              }`}
            >
              <div>
                <p
                  className={`text-sm font-medium ${vale.fechado ? 'line-through text-zinc-400' : 'text-zinc-800'}`}
                  data-testid={vale.fechado ? 'vale-fechado' : undefined}
                >
                  {vale.descricao}
                </p>
                <p className="text-xs text-zinc-400 mt-0.5">
                  {format(parseISO(vale.dataPedido), "d 'de' MMM", { locale: ptBR })}
                </p>
              </div>
              <span
                className={`text-sm font-semibold ${vale.fechado ? 'text-zinc-400 line-through' : 'text-red-600'}`}
              >
                {formatarMoeda(vale.valor)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
