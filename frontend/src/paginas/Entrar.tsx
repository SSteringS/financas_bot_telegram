import { useEffect, useRef } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { exchangeToken } from '../api/auth'
import { invalidarCacheAuth } from '../hooks/useAuth'

export function Entrar() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const tentouRef = useRef(false)

  useEffect(() => {
    // Evita execução dupla em StrictMode
    if (tentouRef.current) return
    tentouRef.current = true

    const token = searchParams.get('t')
    if (!token) {
      navigate('/erro?motivo=token-invalido', { replace: true })
      return
    }

    invalidarCacheAuth()

    exchangeToken(token)
      .then(() => {
        navigate('/', { replace: true })
      })
      .catch(() => {
        navigate('/erro?motivo=token-invalido', { replace: true })
      })
  }, [searchParams, navigate])

  return (
    <div className="min-h-screen bg-white flex items-center justify-center">
      <div className="text-center space-y-3">
        <div className="w-8 h-8 border-2 border-zinc-900 border-t-transparent rounded-full animate-spin mx-auto" />
        <p className="text-zinc-500 text-sm">Verificando seu acesso…</p>
      </div>
    </div>
  )
}
