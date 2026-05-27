import { StatusPedido } from '../api/tipos'

interface StatusBadgeProps {
  status: StatusPedido
}

export function StatusBadge({ status }: StatusBadgeProps) {
  if (status === StatusPedido.PAGO) {
    return (
      <span className="px-2 py-0.5 bg-emerald-100 text-emerald-800 text-[10px] font-bold uppercase rounded">
        Pago
      </span>
    )
  }
  return (
    <span className="px-2 py-0.5 bg-amber-100 text-amber-800 text-[10px] font-bold uppercase rounded">
      Pendente
    </span>
  )
}
