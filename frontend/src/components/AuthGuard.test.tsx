import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AuthGuard } from './AuthGuard'
import { useAuth } from '../hooks/useAuth'

vi.mock('../hooks/useAuth')

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>()
  return { ...actual, useNavigate: () => mockNavigate }
})

const mockUseAuth = vi.mocked(useAuth)

beforeEach(() => {
  mockNavigate.mockReset()
})

function renderGuard(children = <div>protegido</div>) {
  return render(
    <MemoryRouter>
      <AuthGuard>{children}</AuthGuard>
    </MemoryRouter>,
  )
}

describe('AuthGuard', () => {
  it('status loading → renderiza "Carregando…"', () => {
    mockUseAuth.mockReturnValue({ status: 'loading', requisitante: null })
    renderGuard()
    expect(screen.getByText('Carregando…')).toBeInTheDocument()
    expect(screen.queryByText('protegido')).not.toBeInTheDocument()
  })

  it('status autenticado → renderiza children', () => {
    mockUseAuth.mockReturnValue({ status: 'autenticado', requisitante: { id: 1, nome: 'Pedro' } })
    renderGuard()
    expect(screen.getByText('protegido')).toBeInTheDocument()
  })

  it('status nao-autenticado → dispara navigate(/erro?motivo=precisa-link)', async () => {
    mockUseAuth.mockReturnValue({ status: 'nao-autenticado', requisitante: null })
    renderGuard()
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/erro?motivo=precisa-link', { replace: true })
    })
  })
})
