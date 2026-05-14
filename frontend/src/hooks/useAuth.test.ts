import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { useAuth, invalidarCacheAuth } from './useAuth'

vi.mock('../api/auth', () => ({
  obterMe: vi.fn(),
}))

import { obterMe } from '../api/auth'
const mockObterMe = vi.mocked(obterMe)

beforeEach(() => {
  invalidarCacheAuth()
  vi.resetAllMocks()
})

describe('useAuth', () => {
  it('começa em status loading', () => {
    mockObterMe.mockReturnValue(new Promise(() => {}))
    const { result } = renderHook(() => useAuth())
    expect(result.current.status).toBe('loading')
    expect(result.current.requisitante).toBeNull()
  })

  it('fica autenticado quando obterMe resolve', async () => {
    mockObterMe.mockResolvedValue({ requisitante: { id: 1, nome: 'Pedro' } })
    const { result } = renderHook(() => useAuth())
    await waitFor(() => expect(result.current.status).toBe('autenticado'))
    expect(result.current.requisitante).toEqual({ id: 1, nome: 'Pedro' })
  })

  it('fica nao-autenticado quando obterMe rejeita', async () => {
    mockObterMe.mockRejectedValue(new Error('401'))
    const { result } = renderHook(() => useAuth())
    await waitFor(() => expect(result.current.status).toBe('nao-autenticado'))
    expect(result.current.requisitante).toBeNull()
  })
})
