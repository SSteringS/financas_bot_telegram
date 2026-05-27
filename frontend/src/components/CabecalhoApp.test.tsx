import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { CabecalhoApp } from './CabecalhoApp'
import { useResumo } from '../hooks/useResumo'

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({ requisitante: { id: 1, nome: 'Pedro Marques' }, status: 'autenticado' }),
}))

vi.mock('../hooks/useResumo')

const mockUseResumo = vi.mocked(useResumo)

const resumoMaio = {
  mes: '2026-05',
  todos: { quantidade: 6, total: 7227.4 },
  pendentes: { quantidade: 3, total: 5239.9 },
  pagos: { quantidade: 3, total: 1987.5 },
}

const resumoAbril = {
  mes: '2026-04',
  todos: { quantidade: 9, total: 7777.4 },
  pendentes: { quantidade: 0, total: 0 },
  pagos: { quantidade: 9, total: 7777.4 },
}

beforeEach(() => {
  mockUseResumo.mockImplementation((mes) => ({
    data: mes === '2026-04' ? resumoAbril : resumoMaio,
    isLoading: false,
  }) as ReturnType<typeof useResumo>)
})

function renderCabecalho(url = '/') {
  return render(
    <MemoryRouter initialEntries={[url]}>
      <CabecalhoApp />
    </MemoryRouter>,
  )
}

describe('CabecalhoApp', () => {
  it('regressão Bug B: ?mes=2026-04 na URL → cabeçalho mostra abril (0 pendentes)', () => {
    renderCabecalho('/?mes=2026-04')
    expect(screen.getByText(/Nenhum pedido pendente este mês/)).toBeInTheDocument()
    expect(mockUseResumo).toHaveBeenCalledWith('2026-04', '')
  })

  it('mostra primeiro nome e contagem de pendentes de maio', () => {
    renderCabecalho()
    expect(screen.getByText(/Olá Pedro/)).toBeInTheDocument()
    expect(screen.getByText(/pedidos pendentes/)).toBeInTheDocument()
  })

  it('sem pendentes → texto "Nenhum pedido pendente este mês."', () => {
    mockUseResumo.mockReturnValue({
      data: { ...resumoMaio, pendentes: { quantidade: 0, total: 0 } },
      isLoading: false,
    } as ReturnType<typeof useResumo>)
    renderCabecalho()
    expect(screen.getByText(/Nenhum pedido pendente este mês/)).toBeInTheDocument()
  })

  it('isLoading → skeleton visível, saudação não renderizada', () => {
    mockUseResumo.mockReturnValue({
      data: undefined,
      isLoading: true,
    } as ReturnType<typeof useResumo>)
    renderCabecalho()
    expect(document.querySelector('.animate-pulse')).toBeTruthy()
    expect(screen.queryByText(/Olá Pedro/)).not.toBeInTheDocument()
  })
})
