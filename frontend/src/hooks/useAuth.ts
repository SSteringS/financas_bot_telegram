import { useState, useEffect } from 'react'
import { obterMe } from '../api/auth'
import type { Requisitante } from '../api/tipos'

type StatusAuth = 'loading' | 'autenticado' | 'nao-autenticado'

interface UseAuthReturn {
  requisitante: Requisitante | null
  status: StatusAuth
}

// Cache de sessão simples pra evitar chamadas repetidas no ciclo de vida
let cachedRequisitante: Requisitante | null = null
let cachedStatus: StatusAuth = 'loading'

export function useAuth(): UseAuthReturn {
  const [requisitante, setRequisitante] = useState<Requisitante | null>(cachedRequisitante)
  const [status, setStatus] = useState<StatusAuth>(cachedStatus)

  useEffect(() => {
    if (cachedStatus !== 'loading') return

    obterMe()
      .then((data) => {
        cachedRequisitante = data.requisitante
        cachedStatus = 'autenticado'
        setRequisitante(data.requisitante)
        setStatus('autenticado')
      })
      .catch(() => {
        cachedRequisitante = null
        cachedStatus = 'nao-autenticado'
        setRequisitante(null)
        setStatus('nao-autenticado')
      })
  }, [])

  return { requisitante, status }
}

export function invalidarCacheAuth(): void {
  cachedRequisitante = null
  cachedStatus = 'loading'
}
