/**
 * FolhaFuncionarioPage — STUB para FE-016.
 *
 * Esta página será implementada pela task FE-016 (tela de vales, adiantamentos e fechamentos).
 * O stub garante que a rota /folha/funcionarios/:id existe e é navegável a partir de FE-015.
 */
import { useNavigate, useParams } from 'react-router-dom'

export function FolhaFuncionarioPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()

  return (
    <div className="max-w-md mx-auto bg-white min-h-screen pb-24">
      <header className="px-5 pt-6 pb-4 border-b border-zinc-100">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/folha')}
            className="p-1.5 rounded-lg hover:bg-zinc-100 transition-colors"
            aria-label="Voltar para funcionários"
          >
            <svg className="w-5 h-5 text-zinc-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <h1 className="text-xl font-bold text-zinc-900">Folha do funcionário</h1>
        </div>
      </header>

      <div className="px-5 py-12 text-center text-sm text-zinc-400">
        <p className="mb-1">Funcionário #{id}</p>
        <p>Esta tela será implementada na task FE-016.</p>
      </div>
    </div>
  )
}
