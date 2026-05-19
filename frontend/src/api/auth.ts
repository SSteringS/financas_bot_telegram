import { client } from './client'
import type { AuthMeResponse, Requisitante } from './tipos'

export async function exchangeToken(token: string): Promise<{ requisitante: Requisitante }> {
  return client.post<AuthMeResponse>('/api/v1/auth/exchange', { token })
}

export async function obterMe(): Promise<AuthMeResponse> {
  return client.get<AuthMeResponse>('/api/v1/auth/me')
}
