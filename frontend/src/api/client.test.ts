import { describe, it, expect, vi, beforeEach } from 'vitest'
import { client, ApiError } from './client'

// Mockar módulo ambiente
vi.mock('../lib/ambiente', () => ({
  API_BASE_URL: 'http://test-api',
}))

const mockFetch = vi.fn()
vi.stubGlobal('fetch', mockFetch)

function makeResponse(body: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  } as Response
}

beforeEach(() => {
  mockFetch.mockReset()
})

describe('client.get', () => {
  it('faz GET com credentials include', async () => {
    mockFetch.mockResolvedValue(makeResponse({ ok: true }))
    await client.get('/test')
    expect(mockFetch).toHaveBeenCalledWith(
      'http://test-api/test',
      expect.objectContaining({ method: 'GET', credentials: 'include' }),
    )
  })

  it('serializa query params corretamente', async () => {
    mockFetch.mockResolvedValue(makeResponse([]))
    await client.get('/api/v1/pedidos', { status: 'pago', page: 0 })
    const url = mockFetch.mock.calls[0][0] as string
    expect(url).toContain('status=pago')
    expect(url).toContain('page=0')
  })

  it('ignora params undefined', async () => {
    mockFetch.mockResolvedValue(makeResponse([]))
    await client.get('/api/v1/pedidos', { status: undefined, page: 0 })
    const url = mockFetch.mock.calls[0][0] as string
    expect(url).not.toContain('status=')
    expect(url).toContain('page=0')
  })

  it('lança ApiError em status não-ok', async () => {
    mockFetch.mockResolvedValue(makeResponse({ erro: { codigo: 'X', mensagem: 'Erro teste' } }, 400))
    await expect(client.get('/test')).rejects.toThrow(ApiError)
  })

  it('dispara evento e lança ApiError em 401', async () => {
    const events: Event[] = []
    window.addEventListener('finbot:sessao-expirada', (e) => events.push(e))
    mockFetch.mockResolvedValue(makeResponse({}, 401))
    await expect(client.get('/test')).rejects.toThrow(ApiError)
    expect(events).toHaveLength(1)
  })
})

describe('client.post', () => {
  it('faz POST com body JSON', async () => {
    mockFetch.mockResolvedValue(makeResponse({ ok: true }))
    await client.post('/api/v1/auth/exchange', { token: 'abc' })
    expect(mockFetch).toHaveBeenCalledWith(
      'http://test-api/api/v1/auth/exchange',
      expect.objectContaining({
        method: 'POST',
        credentials: 'include',
        body: JSON.stringify({ token: 'abc' }),
      }),
    )
  })
})
