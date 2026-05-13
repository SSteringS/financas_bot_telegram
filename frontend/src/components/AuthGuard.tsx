import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

interface AuthGuardProps {
  children: React.ReactNode
}

export function AuthGuard({ children }: AuthGuardProps) {
  const { status } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (status === 'nao-autenticado') {
      navigate('/erro?motivo=precisa-link', { replace: true })
    }
  }, [status, navigate])

  if (status === 'loading') {
    return (
      <div className="min-h-screen bg-white flex items-center justify-center">
        <div className="animate-pulse text-zinc-400 text-sm">Carregando…</div>
      </div>
    )
  }

  if (status === 'nao-autenticado') {
    return null
  }

  return <>{children}</>
}
