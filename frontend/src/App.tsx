import { BrowserRouter, Routes, Route, useNavigate } from 'react-router-dom'
import { useEffect, lazy, Suspense } from 'react'
import { AuthGuard } from './components/AuthGuard'
import { Entrar } from './paginas/Entrar'
import { Erro } from './paginas/Erro'
import { Home } from './paginas/Home'

const Showcase = import.meta.env.DEV ? lazy(() => import('./paginas/_Showcase').then((m) => ({ default: m.Showcase }))) : null

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
        {import.meta.env.DEV && Showcase && (
          <Route
            path="/_showcase"
            element={
              <Suspense fallback={<div className="p-4 text-zinc-400 text-sm">Carregando…</div>}>
                <Showcase />
              </Suspense>
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
