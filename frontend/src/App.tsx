import { BrowserRouter, Routes, Route, useNavigate } from 'react-router-dom'
import { useEffect } from 'react'
import { AuthGuard } from './components/AuthGuard'
import { Entrar } from './paginas/Entrar'
import { Erro } from './paginas/Erro'
import { Home } from './paginas/Home'

function SessaoExpiradaListener() {
  const navigate = useNavigate()

  useEffect(() => {
    const handler = () => {
      navigate('/erro?motivo=sessao-expirada', { replace: true })
    }
    window.addEventListener('finbot:sessao-expirada', handler)
    return () => window.removeEventListener('finbot:sessao-expirada', handler)
  }, [navigate])

  return null
}

function AppRoutes() {
  return (
    <>
      <SessaoExpiradaListener />
      <Routes>
        <Route path="/entrar" element={<Entrar />} />
        <Route path="/erro" element={<Erro />} />
        <Route
          path="/"
          element={
            <AuthGuard>
              <Home />
            </AuthGuard>
          }
        />
        {import.meta.env.DEV && (
          <Route
            path="/_showcase"
            element={
              <div className="p-4">
                <p className="text-zinc-400 text-xs mb-4">Showcase (dev only)</p>
              </div>
            }
          />
        )}
      </Routes>
    </>
  )
}

function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  )
}

export default App
