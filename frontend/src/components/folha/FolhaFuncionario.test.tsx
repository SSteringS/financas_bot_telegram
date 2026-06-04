/**
 * FolhaFuncionario.test.tsx — testes das seções de ValesSection, AdiantamentosSection
 * e FechamentosSection (FE-016).
 *
 * As seções recebem dados via props — sem necessidade de mock de queries.
 */

import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ValesSection } from './ValesSection'
import { AdiantamentosSection } from './AdiantamentosSection'
import { FechamentosSection } from './FechamentosSection'
import type { Vale, Adiantamento, Fechamento } from '../../types/folha'

// ── Fixtures ──────────────────────────────────────────────────────────────────

const valeAberto: Vale = {
  id: 1,
  funcionarioId: 10,
  categoria: 'VALE',
  descricao: 'Mercado',
  valor: 150,
  status: 'PENDENTE',
  fechado: false,
  dataPedido: '2026-06-03',
  dataCriacao: '2026-06-03T10:00:00',
}

const valeFechado: Vale = {
  ...valeAberto,
  id: 2,
  descricao: 'Farmácia',
  valor: 80,
  fechado: true,
  status: 'PAGO',
}

const adiantamentoAtivo: Adiantamento = {
  id: 5,
  funcionarioId: 10,
  descricao: 'Óculos',
  valorTotal: 600,
  valorParcela: 200,
  numParcelas: 3,
  parcelasPagas: 1,
  parcelasRestantes: 2,
  dataInicio: '2026-05-01',
  ativo: true,
  criadoEm: '2026-05-01T09:00:00',
}

const fechamento: Fechamento = {
  id: 20,
  funcionarioId: 10,
  valor: 1700,
  status: 'PENDENTE',
  observacao: 'salário R$2000 - vales R$150 - adiantamentos R$200 + ajuste R$50',
  mesReferencia: '2026-05-01',
  dataCriacao: '2026-06-01T12:00:00',
}

// ── ValesSection ──────────────────────────────────────────────────────────────

describe('ValesSection — estado vazio', () => {
  it('exibe mensagem de estado vazio quando não há vales', () => {
    render(
      <ValesSection
        vales={[]}
        mesSelecionado="2026-06"
        onMesChange={vi.fn()}
        onCriarVale={vi.fn()}
      />,
    )
    expect(screen.getByTestId('vales-vazio')).toBeInTheDocument()
  })
})

describe('ValesSection — lista de vales', () => {
  it('renderiza vales e aplica classe de tachado nos fechados', () => {
    render(
      <ValesSection
        vales={[valeAberto, valeFechado]}
        mesSelecionado="2026-06"
        onMesChange={vi.fn()}
        onCriarVale={vi.fn()}
      />,
    )

    // Vale aberto — sem tachado
    const itemAberto = screen.getByTestId('vale-item-1')
    expect(itemAberto).toBeInTheDocument()
    expect(screen.queryByTestId('vale-fechado')).toBeInTheDocument() // vale fechado marcado

    // Vale fechado tem data-testid="vale-fechado"
    const itemFechado = screen.getByTestId('vale-item-2')
    expect(itemFechado).toBeInTheDocument()
  })

  it('clicar em "+ Registrar vale" exibe ValeForm', () => {
    render(
      <ValesSection
        vales={[]}
        mesSelecionado="2026-06"
        onMesChange={vi.fn()}
        onCriarVale={vi.fn()}
      />,
    )
    expect(screen.queryByTestId('vale-form')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /registrar vale/i }))
    expect(screen.getByTestId('vale-form')).toBeInTheDocument()
  })

  it('cancelar ValeForm oculta o formulário', () => {
    render(
      <ValesSection
        vales={[]}
        mesSelecionado="2026-06"
        onMesChange={vi.fn()}
        onCriarVale={vi.fn()}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /registrar vale/i }))
    expect(screen.getByTestId('vale-form')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /cancelar/i }))
    expect(screen.queryByTestId('vale-form')).not.toBeInTheDocument()
  })
})

// ── AdiantamentosSection ──────────────────────────────────────────────────────

describe('AdiantamentosSection — lista de adiantamentos', () => {
  it('renderiza adiantamento com parcela X/total correta', () => {
    render(
      <AdiantamentosSection
        adiantamentos={[adiantamentoAtivo]}
        onCriarAdiantamento={vi.fn()}
        onCancelarAdiantamento={vi.fn()}
      />,
    )

    // parcelasPagas=1, numParcelas=3 → deve mostrar "2/3" (próxima parcela a ser paga)
    const parcelaEl = screen.getByTestId('parcela-5')
    expect(parcelaEl).toHaveTextContent('2/3')
  })

  it('botão cancelar chama onCancelarAdiantamento com o id correto', async () => {
    const onCancelar = vi.fn().mockResolvedValue(undefined)
    window.confirm = vi.fn().mockReturnValue(true)

    render(
      <AdiantamentosSection
        adiantamentos={[adiantamentoAtivo]}
        onCriarAdiantamento={vi.fn()}
        onCancelarAdiantamento={onCancelar}
      />,
    )

    fireEvent.click(screen.getByTestId('btn-cancelar-5'))

    await waitFor(() => {
      expect(onCancelar).toHaveBeenCalledWith(5)
    })
  })
})

// ── FechamentosSection ─────────────────────────────────────────────────────────

describe('FechamentosSection — histórico de fechamentos', () => {
  it('renderiza fechamento com mês de referência e valor', () => {
    render(<FechamentosSection fechamentos={[fechamento]} />)

    const item = screen.getByTestId('fechamento-item-20')
    expect(item).toBeInTheDocument()
    // Deve exibir "maio de 2026" (mes_referencia = "2026-05-01")
    expect(item).toHaveTextContent(/maio/i)
  })

  it('clicar no fechamento expande a observação (accordion)', () => {
    render(<FechamentosSection fechamentos={[fechamento]} />)

    // Observação não aparece antes do clique
    expect(screen.queryByTestId('observacao-20')).not.toBeInTheDocument()

    fireEvent.click(screen.getByTestId('btn-expandir-20'))
    expect(screen.getByTestId('observacao-20')).toBeInTheDocument()
    expect(screen.getByTestId('observacao-20')).toHaveTextContent(/salário/)
  })
})
