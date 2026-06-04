/**
 * FolhaFuncionarioPage — tela de operação do funcionário (FE-016 + FE-017).
 *
 * Rota: /folha/funcionarios/:id
 * Exibe: vales do mês, adiantamentos ativos, fechamentos anteriores.
 * Botão "Fechar mês" aparece somente se o mês selecionado ainda não foi fechado.
 * Após confirmação do fechamento (FE-017): exibe banner de sucesso + invalida cache.
 */

import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { format } from 'date-fns'
import { ptBR } from 'date-fns/locale'
import { formatarMoeda } from '../../lib/formato'
import { useFolhaFuncionario } from '../../hooks/folha/useFolhaFuncionario'
import { ValesSection } from '../../components/folha/ValesSection'
import { AdiantamentosSection } from '../../components/folha/AdiantamentosSection'
import { FechamentosSection } from '../../components/folha/FechamentosSection'
import { ModalFechamento } from '../../components/folha/ModalFechamento'
import type { Fechamento } from '../../types/folha'

/** Retorna o mês corrente no formato "YYYY-MM" */
function mesCorrente(): string {
  const hoje = new Date()
  return `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, '0')}`
}

export function FolhaFuncionarioPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const funcionarioId = parseInt(id ?? '0', 10)

  const [mesSelecionado, setMesSelecionado] = useState(mesCorrente)
  const [modalAberto, setModalAberto] = useState(false)
  const [toastSucesso, setToastSucesso] = useState<string | null>(null)

  const {
    funcionario,
    vales,
    adiantamentos,
    fechamentos,
    isLoading,
    mesFechado,
    criarVale,
    criarAdiantamento,
    cancelarAdiantamento,
    invalidarPosFechamento,
  } = useFolhaFuncionario(funcionarioId, mesSelecionado)

  function handleFecharMesConfirmado(fechamento: Fechamento) {
    setModalAberto(false)
    invalidarPosFechamento()
    setToastSucesso(`Mês ${fechamento.mesReferencia.slice(0, 7)} fechado — ${formatarMoeda(fechamento.valor)}`)
    setTimeout(() => setToastSucesso(null), 4000)
  }

  // Label do mês para o botão
  const mesBotaoLabel = (() => {
    const [ano, mes] = mesSelecionado.split('-')
    const d = new Date(parseInt(ano), parseInt(mes) - 1, 1)
    return format(d, "MMMM/yyyy", { locale: ptBR })
  })()

  // Total de vales abertos (para o ModalFechamento FE-017)
  const totalValesAbertos = vales
    .filter((v) => !v.fechado)
    .reduce((acc, v) => acc + v.valor, 0)

  if (isLoading && !funcionario) {
    return (
      <div className="max-w-md mx-auto bg-white min-h-screen pb-24">
        <div className="px-5 pt-20 text-center">
          <p className="text-sm text-zinc-400">Carregando…</p>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-md mx-auto bg-white min-h-screen pb-24">
      {/* Header */}
      <header className="px-5 pt-6 pb-4 border-b border-zinc-100">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/folha')}
            className="p-1.5 rounded-lg hover:bg-zinc-100 transition-colors"
            aria-label="Voltar para funcionários"
          >
            <svg
              className="w-5 h-5 text-zinc-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold text-zinc-900 truncate">
                {funcionario?.nome ?? `Funcionário #${id}`}
              </h1>
              {funcionario && !funcionario.contaPropria && (
                <span className="shrink-0 text-xs font-medium bg-orange-100 text-orange-700 px-2 py-0.5 rounded-full">
                  Conta de terceiro
                </span>
              )}
            </div>
            {funcionario && (
              <p className="text-sm text-zinc-400 mt-0.5">
                Salário base: {formatarMoeda(funcionario.salarioBase)}
              </p>
            )}
          </div>
        </div>
      </header>

      {/* Conteúdo */}
      <main className="px-5 py-6 space-y-8">
        {/* Seção Vales */}
        <ValesSection
          vales={vales}
          mesSelecionado={mesSelecionado}
          onMesChange={setMesSelecionado}
          onCriarVale={criarVale}
        />

        {/* Botão fechar mês — só aparece se o mês ainda não foi fechado */}
        {!mesFechado && (
          <div className="pt-1">
            <button
              onClick={() => setModalAberto(true)}
              data-testid="btn-fechar-mes"
              className="w-full bg-emerald-600 text-white text-sm font-semibold py-3 rounded-xl hover:bg-emerald-700 transition-colors"
            >
              Fechar mês {mesBotaoLabel}
            </button>
          </div>
        )}

        {/* Seção Adiantamentos */}
        <AdiantamentosSection
          adiantamentos={adiantamentos}
          onCriarAdiantamento={criarAdiantamento}
          onCancelarAdiantamento={cancelarAdiantamento}
        />

        {/* Seção Fechamentos */}
        <FechamentosSection fechamentos={fechamentos} />
      </main>

      {/* Toast de sucesso (FE-017) */}
      {toastSucesso && (
        <div
          className="fixed bottom-6 left-1/2 -translate-x-1/2 bg-emerald-600 text-white text-sm font-medium px-5 py-3 rounded-full shadow-lg z-50"
          role="status"
          data-testid="toast-sucesso"
        >
          ✓ {toastSucesso}
        </div>
      )}

      {/* Modal Fechamento (FE-017) */}
      {funcionario && (
        <ModalFechamento
          open={modalAberto}
          funcionarioId={funcionarioId}
          mes={mesSelecionado}
          salarioBase={funcionario.salarioBase}
          totalVales={totalValesAbertos}
          listaAdiantamentosAtivos={adiantamentos}
          onClose={() => setModalAberto(false)}
          onConfirmado={handleFecharMesConfirmado}
        />
      )}
    </div>
  )
}
