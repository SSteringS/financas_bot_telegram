import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Entrar } from './Entrar'
import { exchangeToken } from '../api/auth'

vi.mock('../api/auth')
vi.mock('../hooks/useAuth', () => ({
  useAuth: vi.fn(),
  invalidarCacheAuth: vi.fn(),
}))

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>()
  return { ...actual, useNavigate: () => mockNavigate }
})

const mockExchangeToken = vi.mocked(exchangeToken)

beforeEach(() => {
  mockNavigate.mockReset()
  mockExchangeToken.mockReset()
})

function renderEntrar(url = '/') {
  return render(
    <MemoryRouter initialEntries={[url]}>
      <Entrar />
    </MemoryRouter>,
  )
}

describe('Entrar', () => {
  it('sem ?t na URL → navega para /erro?motivo=token-invalido', async () => {
    renderEntrar('/')
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/erro?motivo=token-invalido', { replace: true })
    })
  })

  it('com ?t=valido → exchangeToken chamado e navega para /', async () => {
    mockExchangeToken.mockResolvedValue({ requisitante: { id: 1, nome: 'Pedro' } })
    renderEntrar('/?t=valido')
    await waitFor(() => {
      expect(mockExchangeToken).toHaveBeenCalledWith('valido')
      expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true })
    })
  })

  it('exchangeToken rejeita → navega para /erro?motivo=token-invalido', async () => {
    mockExchangeToken.mockRejectedValue(new Error('401'))
    renderEntrar('/?t=qualquer')
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/erro?motivo=token-invalido', { replace: true })
    })
  })
})
