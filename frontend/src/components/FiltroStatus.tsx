type ValorStatus = 'TUDO' | 'PENDENTE' | 'PAGO'

interface FiltroStatusProps {
  value: ValorStatus
  onChange: (value: ValorStatus) => void
  contadores?: { tudo: number; pendente: number; pago: number }
}

const opcoes: { valor: ValorStatus; label: string; chave: keyof NonNullable<FiltroStatusProps['contadores']> }[] = [
  { valor: 'TUDO', label: 'Tudo', chave: 'tudo' },
  { valor: 'PENDENTE', label: 'Pendente', chave: 'pendente' },
  { valor: 'PAGO', label: 'Pago', chave: 'pago' },
]

export function FiltroStatus({ value, onChange, contadores }: FiltroStatusProps) {
  return (
    <div className="flex gap-2" role="group" aria-label="Filtrar por status">
      {opcoes.map((opcao) => {
        const ativo = value === opcao.valor
        const count = contadores?.[opcao.chave]
        const label = count !== undefined ? `${opcao.label} (${count})` : opcao.label

        let className = 'flex-1 px-3 py-2.5 rounded-lg text-xs font-semibold transition-colors min-h-[44px] '

        if (ativo) {
          className += 'bg-zinc-900 text-white'
        } else if (opcao.valor === 'PENDENTE') {
          className += 'bg-amber-50 text-amber-800 border border-amber-200'
        } else if (opcao.valor === 'PAGO') {
          className += 'bg-emerald-50 text-emerald-800 border border-emerald-200'
        } else {
          className += 'bg-zinc-100 text-zinc-700'
        }

        return (
          <button
            key={opcao.valor}
            className={className}
            onClick={() => onChange(opcao.valor)}
            aria-pressed={ativo}
          >
            {label}
          </button>
        )
      })}
    </div>
  )
}
