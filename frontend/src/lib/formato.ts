import { format, isToday, isYesterday, parseISO } from 'date-fns'
import { ptBR } from 'date-fns/locale'

export function formatarMoeda(valor: number): string {
  return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export function formatarData(isoDate: string): string {
  const data = parseISO(isoDate)
  return format(data, "d 'de' MMMM", { locale: ptBR })
}

export function formatarDataRelativa(isoDate: string): string {
  const data = parseISO(isoDate)
  if (isToday(data)) return 'Hoje'
  if (isYesterday(data)) return 'Ontem'
  return formatarData(isoDate)
}

export function formatarDiaSemana(isoDate: string): string {
  const data = parseISO(isoDate)
  return format(data, "EEEE, d 'de' MMMM", { locale: ptBR })
}

// Retorna "MAI", "ABR", etc. — abreviação do mês em maiúsculas
export function abreviarMes(isoDate: string): string {
  const data = parseISO(isoDate)
  return format(data, 'MMM', { locale: ptBR }).toUpperCase().replace('.', '')
}

// Retorna o dia do mês como número
export function diaDaMes(isoDate: string): number {
  return parseISO(isoDate).getDate()
}
