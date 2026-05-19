import { describe, it, expect } from 'vitest'
import { formatarMoeda, formatarData, formatarDataRelativa } from './formato'

describe('formatarMoeda', () => {
  it('formata valor inteiro', () => {
    expect(formatarMoeda(1500)).toBe('R$ 1.500,00')
  })

  it('formata valor decimal', () => {
    expect(formatarMoeda(287.5)).toBe('R$ 287,50')
  })

  it('formata zero', () => {
    expect(formatarMoeda(0)).toBe('R$ 0,00')
  })
})

describe('formatarData', () => {
  it('formata data de maio', () => {
    expect(formatarData('2026-05-04')).toBe('4 de maio')
  })

  it('formata data de abril', () => {
    expect(formatarData('2026-04-15')).toBe('15 de abril')
  })
})

describe('formatarDataRelativa', () => {
  it('retorna string de data para datas passadas', () => {
    const result = formatarDataRelativa('2026-01-01')
    expect(typeof result).toBe('string')
    expect(result.length).toBeGreaterThan(0)
  })
})
