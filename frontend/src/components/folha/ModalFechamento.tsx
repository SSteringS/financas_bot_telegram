/**
 * ModalFechamento — STUB para FE-017.
 *
 * Interface de props estabelecida por FE-016 para contratos com FolhaFuncionarioPage.
 * FE-017 substitui este stub pela implementação completa com:
 *   - preview em tempo real (salário - vales - parcelas adiantamentos + ajuste)
 *   - alerta para valor negativo
 *   - POST /api/funcionarios/{id}/fechamentos
 *   - toast de confirmação
 *
 * Props definidas aqui são o contrato que FE-017 deve seguir sem alteração.
 */

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

export function ModalFechamento({ open, mes, onClose }: ModalFechamentoProps) {
  if (!open) return null

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={`Fechar mês ${mes}`}
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/50"
    >
      <div className="bg-white rounded-t-2xl w-full max-w-md p-6 pb-8">
        <p className="text-sm font-semibold text-zinc-800 mb-2">
          Fechar mês {mes}
        </p>
        <p className="text-xs text-zinc-400 mb-6">
          Implementação completa disponível na task FE-017.
        </p>
        <button
          onClick={onClose}
          className="w-full text-sm text-zinc-500 border border-zinc-200 rounded-xl py-2 hover:bg-zinc-50 transition-colors"
        >
          Fechar
        </button>
      </div>
    </div>
  )
}
