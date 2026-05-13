import { API_BASE_URL } from '../lib/ambiente'

export class ApiError extends Error {
  constructor(
    public readonly codigo: number,
    mensagem: string,
  ) {
    super(mensagem)
    this.name = 'ApiError'
  }
}

function buildUrl(path: string, params?: Record<string, string | number | boolean | undefined>): string {
  const url = new URL(`${API_BASE_URL}${path}`)
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.append(key, String(value))
      }
    })
  }
  return url.toString()
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (response.status === 401) {
    // Dispara evento customizado para ser capturado pelo router
    window.dispatchEvent(new CustomEvent('finbot:sessao-expirada'))
    console.warn('[API] Sessão expirada — redirecionando para /erro')
    throw new ApiError(401, 'Sessão expirada')
  }

  if (!response.ok) {
    let mensagem = `Erro ${response.status}`
    try {
      const body = await response.json()
      if (body?.erro?.mensagem) {
        mensagem = body.erro.mensagem
      }
    } catch {
      // ignora se body não for JSON
    }
    throw new ApiError(response.status, mensagem)
  }

  return response.json() as Promise<T>
}

export const client = {
  async get<T>(path: string, params?: Record<string, string | number | boolean | undefined>): Promise<T> {
    const response = await fetch(buildUrl(path, params), {
      method: 'GET',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
    })
    return handleResponse<T>(response)
  },

  async post<T>(path: string, body: unknown): Promise<T> {
    const response = await fetch(buildUrl(path), {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    return handleResponse<T>(response)
  },
}
