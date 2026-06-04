/**
 * AdiantamentoForm — formulário inline para registrar um adiantamento parcelado (FE-016).
 *
 * Campos: descrição, valor da parcela, nº de parcelas (total calculado), data de início.
 * valorTotal é computado (valorParcela × numParcelas) e enviado ao backend.
 * Não usa react-hook-form — campos controlados com useState.
 */

import { useState } from 'react'
import { formatarMoeda } from '../../lib/formato'
import type { AdiantamentoRequest } from '../../types/folha'

interface AdiantamentoFormProps {
  onSubmit: (data: AdiantamentoRequest) => Promise<void>
  onCancelar: () => void
}

function dataHoje(): string {
  return new Date().toISOString().slice(0, 10)
}

export function AdiantamentoForm({ onSubmit, onCancelar }: AdiantamentoFormProps) {
  const [descricao, setDescricao] = useState('')
  const [valorParcela, setValorParcela] = useState('')
  const [numParcelas, setNumParcelas] = useState('')
  const [dataInicio, setDataInicio] = useState(dataHoje())
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  const valorParcelaNum = parseFloat(valorParcela) || 0
  const numParcelasNum = parseInt(numParcelas) || 0
  const valorTotal = valorParcelaNum * numParcelasNum

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!descricao.trim() || !valorParcela || !numParcelas) return

    setSalvando(true)
    setErro(null)
    try {
      await onSubmit({
        descricao: descricao.trim(),
        valorTotal,
        valorParcela: valorParcelaNum,
        numParcelas: numParcelasNum,
        dataInicio,
      })
    } catch {
      setErro('Erro ao registrar adiantamento. Tente novamente.')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      data-testid="adiantamento-form"
      className="bg-zinc-50 rounded-xl p-4 space-y-3 border border-zinc-200"
    >
      <p className="text-sm font-medium text-zinc-700">Novo adiantamento parcelado</p>

      <div>
        <label htmlFor="adt-descricao" className="block text-xs text-zinc-500 mb-1">
          Descrição *
        </label>
        <input
          id="adt-descricao"
          type="text"
          value={descricao}
          onChange={(e) => setDescricao(e.target.value)}
          required
          className="w-full rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="Ex: óculos"
        />
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label htmlFor="adt-parcela" className="block text-xs text-zinc-500 mb-1">
            Valor da parcela (R$) *
          </label>
          <input
            id="adt-parcela"
            type="number"
            min="0.01"
            step="0.01"
            value={valorParcela}
            onChange={(e) => setValorParcela(e.target.value)}
            required
            className="w-full rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="0,00"
          />
        </div>

        <div>
          <label htmlFor="adt-num-parcelas" className="block text-xs text-zinc-500 mb-1">
            Nº de parcelas *
          </label>
          <input
            id="adt-num-parcelas"
            type="number"
            min="1"
            step="1"
            value={numParcelas}
            onChange={(e) => setNumParcelas(e.target.value)}
            required
            className="w-full rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="1"
          />
        </div>
      </div>

      {valorTotal > 0 && (
        <p className="text-xs text-zinc-500">
          Total: <span className="font-semibold text-zinc-700">{formatarMoeda(valorTotal)}</span>
        </p>
      )}

      <div>
        <label htmlFor="adt-data-inicio" className="block text-xs text-zinc-500 mb-1">
          Início dos descontos *
        </label>
        <input
          id="adt-data-inicio"
          type="date"
          value={dataInicio}
          onChange={(e) => setDataInicio(e.target.value)}
          required
          className="w-full rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      {erro && <p className="text-xs text-red-600">{erro}</p>}

      <div className="flex gap-2 pt-1">
        <button
          type="submit"
          disabled={salvando}
          className="flex-1 bg-blue-600 text-white text-sm font-medium py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
        >
          {salvando ? 'Salvando…' : 'Salvar adiantamento'}
        </button>
        <button
          type="button"
          onClick={onCancelar}
          className="px-4 text-sm text-zinc-600 hover:text-zinc-900 transition-colors"
        >
          Cancelar
        </button>
      </div>
    </form>
  )
}
