import { useRef } from 'react'
import { format, subMonths, parseISO } from 'date-fns'
import { ptBR } from 'date-fns/locale'

interface SeletorMesProps {
  value: string // YYYY-MM
  onChange: (value: string) => void
}

function gerarUltimos12Meses(): string[] {
  const meses: string[] = []
  const hoje = new Date()
  for (let i = 0; i < 12; i++) {
    meses.push(format(subMonths(hoje, i), 'yyyy-MM'))
  }
  return meses
}

function formatarMesLabel(mesAnoStr: string): string {
  // parseISO precisa de formato completo — adiciona dia
  const data = parseISO(`${mesAnoStr}-01`)
  return format(data, 'MMMM', { locale: ptBR })
    .replace(/^\w/, (c) => c.toUpperCase())
}

export function SeletorMes({ value, onChange }: SeletorMesProps) {
  const meses = gerarUltimos12Meses()
  const containerRef = useRef<HTMLDivElement>(null)

  return (
    <div
      ref={containerRef}
      className="flex gap-2 overflow-x-auto pb-1"
      style={{ scrollbarWidth: 'none' }}
      role="listbox"
      aria-label="Selecionar mês"
    >
      {meses.map((mes) => {
        const ativo = mes === value
        return (
          <button
            key={mes}
            role="option"
            aria-selected={ativo}
            className={`flex-shrink-0 px-4 py-2 rounded-full text-sm font-medium transition-colors min-h-[44px] ${
              ativo
                ? 'bg-zinc-900 text-white font-semibold'
                : 'bg-zinc-100 text-zinc-700'
            }`}
            onClick={() => onChange(mes)}
          >
            {formatarMesLabel(mes)}
          </button>
        )
      })}
    </div>
  )
}
