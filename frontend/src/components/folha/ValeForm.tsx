/**
 * ValeForm — formulário inline para registrar um vale (FE-016).
 *
 * Campos: descrição, valor, data do pedido (default: hoje).
 * Não usa react-hook-form — campos controlados com useState.
 */

import { useState } from 'react'
import type { ValeRequest } from '../../types/folha'

interface ValeFormProps {
  onSubmit: (data: ValeRequest) => Promise<void>
  onCancelar: () => void
}

function dataHoje(): string {
  return new Date().toISOString().slice(0, 10) // "YYYY-MM-DD"
}

export function ValeForm({ onSubmit, onCancelar }: ValeFormProps) {
  const [descricao, setDescricao] = useState('')
  const [valor, setValor] = useState('')
  const [dataPedido, setDataPedido] = useState(dataHoje())
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!descricao.trim() || !valor) return

    setSalvando(true)
    setErro(null)
    try {
      await onSubmit({
        descricao: descricao.trim(),
        valor: parseFloat(valor),
        dataPedido,
      })
    } catch {
      setErro('Erro ao registrar vale. Tente novamente.')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      data-testid="vale-form"
      className="bg-zinc-50 rounded-xl p-4 space-y-3 border border-zinc-200"
    >
      <p className="text-sm font-medium text-zinc-700">Registrar vale</p>

      <div>
        <label htmlFor="vale-descricao" className="block text-xs text-zinc-500 mb-1">
          Descrição *
        </label>
        <input
          id="vale-descricao"
          type="text"
          value={descricao}
          onChange={(e) => setDescricao(e.target.value)}
          required
          className="w-full rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="Ex: adiantamento pessoal"
        />
      </div>

      <div>
        <label htmlFor="vale-valor" className="block text-xs text-zinc-500 mb-1">
          Valor (R$) *
        </label>
        <input
          id="vale-valor"
          type="number"
          min="0.01"
          step="0.01"
          value={valor}
          onChange={(e) => setValor(e.target.value)}
          required
          className="w-full rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="0,00"
        />
      </div>

      <div>
        <label htmlFor="vale-data" className="block text-xs text-zinc-500 mb-1">
          Data
        </label>
        <input
          id="vale-data"
          type="date"
          value={dataPedido}
          onChange={(e) => setDataPedido(e.target.value)}
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
          {salvando ? 'Salvando…' : 'Salvar vale'}
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
