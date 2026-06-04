/**
 * ModalFechamento — modal de fechamento mensal com cálculo em tempo real (FE-017).
 *
 * Recebe os dados carregados pela FolhaFuncionarioPage via props:
 *   salarioBase, totalVales, listaAdiantamentosAtivos.
 *
 * Calcula localmente: valorFinal = salarioBase - totalVales - totalParcelas + ajuste.
 * Sem roundtrip para preview — só o POST final vai ao backend.
 *
 * Após sucesso: chama onConfirmado(fechamento) → FolhaFuncionarioPage invalida o cache.
 * Erro 409 (mês já fechado): mensagem clara.
 */

import { useState } from 'react'
import { formatarMoeda } from '../../lib/formato'
import { fecharMes } from '../../api/folha'
import { ApiError } from '../../api/client'
import type { Adiantamento, Fechamento } from '../../types/folha'

export interface ModalFechamentoProps {
  open: boolean
  funcionarioId: number
  mes: string                          // "YYYY-MM" (ex: "2026-06")
  salarioBase: number
  totalVales: number                   // soma dos vales abertos do mês
  listaAdiantamentosAtivos: Adiantamento[]
  onClose: () => void
  onConfirmado: (fechamento: Fechamento) => void
}

export function ModalFechamento({
  open,
  funcionarioId,
  mes,
  salarioBase,
  totalVales,
  listaAdiantamentosAtivos,
  onClose,
  onConfirmado,
}: ModalFechamentoProps) {
  const [ajuste, setAjuste] = useState('0')
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  if (!open) return null

  const totalParcelas = listaAdiantamentosAtivos.reduce(
    (sum, adt) => sum + adt.valorParcela,
    0,
  )
  const ajusteNum = parseFloat(ajuste) || 0
  const valorFinal = salarioBase - totalVales - totalParcelas + ajusteNum
  const negativo = valorFinal < 0

  async function handleConfirmar() {
    setSalvando(true)
    setErro(null)
    try {
      const fechamento = await fecharMes(funcionarioId, mes, ajusteNum)
      onConfirmado(fechamento)
    } catch (e) {
      if (e instanceof ApiError && e.codigo === 409) {
        setErro('Mês já fechado. Recarregue a página.')
      } else {
        setErro('Erro ao fechar mês. Tente novamente.')
      }
      setSalvando(false)
    }
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={`Fechar mês ${mes}`}
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/50"
      data-testid="modal-fechamento"
    >
      <div className="bg-white rounded-t-2xl w-full max-w-md p-6 pb-10 space-y-5">
        {/* Título */}
        <div>
          <h2 className="text-base font-bold text-zinc-900">Fechar mês {mes}</h2>
          <p className="text-xs text-zinc-400 mt-0.5">Preview do cálculo baseado nos dados carregados.</p>
        </div>

        {/* Breakdown */}
        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-zinc-600">Salário base</span>
            <span className="font-medium text-zinc-800">{formatarMoeda(salarioBase)}</span>
          </div>

          <div className="flex justify-between text-sm">
            <span className="text-zinc-600">
              Vales ({listaAdiantamentosAtivos.length === 0 && totalVales === 0 ? '0' : '−'})
            </span>
            <span className="font-medium text-red-600">
              − {formatarMoeda(totalVales)}
            </span>
          </div>

          <div className="flex justify-between text-sm">
            <span className="text-zinc-600">
              Parcelas adiantamentos ({listaAdiantamentosAtivos.length})
            </span>
            <span className="font-medium text-red-600">
              − {formatarMoeda(totalParcelas)}
            </span>
          </div>

          {/* Ajuste */}
          <div className="flex items-center justify-between gap-3">
            <label htmlFor="modal-ajuste" className="text-sm text-zinc-600 shrink-0">
              Ajuste (bônus/desconto)
            </label>
            <input
              id="modal-ajuste"
              type="number"
              step="0.01"
              value={ajuste}
              onChange={(e) => setAjuste(e.target.value)}
              className="w-28 text-right rounded-lg border border-zinc-200 px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              aria-label="Ajuste"
              data-testid="input-ajuste"
            />
          </div>

          <div className="border-t border-zinc-100 pt-2 flex justify-between text-sm font-semibold">
            <span className="text-zinc-700">Valor final</span>
            <span
              className={negativo ? 'text-red-600' : 'text-emerald-600'}
              data-testid="valor-final"
            >
              {formatarMoeda(valorFinal)}
            </span>
          </div>
        </div>

        {/* Alerta valor negativo */}
        {negativo && (
          <div
            className="bg-red-50 border border-red-200 rounded-xl px-4 py-2"
            data-testid="alerta-negativo"
          >
            <p className="text-xs text-red-700 font-medium">
              ⚠ Valor negativo — verificar vales e adiantamentos antes de confirmar.
            </p>
          </div>
        )}

        {/* Erro */}
        {erro && (
          <p className="text-xs text-red-600" data-testid="modal-erro">
            {erro}
          </p>
        )}

        {/* Ações */}
        <div className="flex gap-3">
          <button
            onClick={onClose}
            disabled={salvando}
            className="flex-1 text-sm text-zinc-600 border border-zinc-200 rounded-xl py-2.5 hover:bg-zinc-50 disabled:opacity-50 transition-colors"
          >
            Cancelar
          </button>
          <button
            onClick={handleConfirmar}
            disabled={salvando}
            className="flex-1 bg-emerald-600 text-white text-sm font-semibold py-2.5 rounded-xl hover:bg-emerald-700 disabled:opacity-50 transition-colors"
            data-testid="btn-confirmar"
          >
            {salvando ? 'Fechando…' : 'Confirmar fechamento'}
          </button>
        </div>
      </div>
    </div>
  )
}
