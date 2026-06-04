/**
 * ModalFechamento.test.tsx — testes do modal de fechamento mensal (FE-017).
 *
 * Testa o cálculo em tempo real, alerta de valor negativo, loading state e fechamento sem render.
 */

import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ModalFechamento } from './ModalFechamento'
import type { Adiantamento, Fechamento } from '../../types/folha'

// Mock da API de fecharMes
vi.mock('../../api/folha', () => ({
  fecharMes: vi.fn(),
}))

import { fecharMes } from '../../api/folha'
const fecharMesMock = vi.mocked(fecharMes)

// ── Fixtures ──────────────────────────────────────────────────────────────────

const adiantamentoA: Adiantamento = {
  id: 1,
  funcionarioId: 10,
  descricao: 'Óculos',
  valorTotal: 600,
  valorParcela: 200,
  numParcelas: 3,
  parcelasPagas: 0,
  parcelasRestantes: 3,
  dataInicio: '2026-05-01',
  ativo: true,
  criadoEm: '2026-05-01T09:00:00',
}

const defaultProps = {
  open: true,
  funcionarioId: 10,
  mes: '2026-06',
  salarioBase: 2000,
  totalVales: 150,
  listaAdiantamentosAtivos: [adiantamentoA],
  onClose: vi.fn(),
  onConfirmado: vi.fn(),
}

// ── Testes ────────────────────────────────────────────────────────────────────

describe('ModalFechamento — não renderiza quando fechado', () => {
  it('não renderiza o modal quando open=false', () => {
    render(<ModalFechamento {...defaultProps} open={false} />)
    expect(screen.queryByTestId('modal-fechamento')).not.toBeInTheDocument()
  })
})

describe('ModalFechamento — cálculo em tempo real', () => {
  it('exibe o valor final correto: salário(2000) - vales(150) - parcelas(200) = 1650', () => {
    render(<ModalFechamento {...defaultProps} />)
    // valorFinal = 2000 - 150 - 200 + 0 = 1650
    const valorFinal = screen.getByTestId('valor-final')
    expect(valorFinal).toHaveTextContent('1.650')
  })

  it('campo ajuste atualiza o valor final em tempo real', () => {
    render(<ModalFechamento {...defaultProps} />)
    const inputAjuste = screen.getByTestId('input-ajuste')

    // Adicionar bônus de 100
    fireEvent.change(inputAjuste, { target: { value: '100' } })

    // valorFinal = 2000 - 150 - 200 + 100 = 1750
    expect(screen.getByTestId('valor-final')).toHaveTextContent('1.750')
  })

  it('exibe alerta vermelho quando valor final é negativo', () => {
    render(
      <ModalFechamento
        {...defaultProps}
        salarioBase={200}    // salário baixo para forçar negativo
        totalVales={300}
      />,
    )
    // valorFinal = 200 - 300 - 200 + 0 = -300
    expect(screen.getByTestId('alerta-negativo')).toBeInTheDocument()
  })
})

describe('ModalFechamento — confirmação', () => {
  it('botão "Confirmar fechamento" fica desabilitado durante o POST (loading)', async () => {
    let resolverFechamento!: (v: Fechamento) => void
    fecharMesMock.mockReturnValue(
      new Promise<Fechamento>((resolve) => {
        resolverFechamento = resolve
      }),
    )

    render(<ModalFechamento {...defaultProps} />)
    const btn = screen.getByTestId('btn-confirmar')
    fireEvent.click(btn)

    // Durante o loading o botão fica disabled
    await waitFor(() => {
      expect(btn).toBeDisabled()
      expect(btn).toHaveTextContent('Fechando…')
    })

    // Resolver a promise para não deixar a promise pendente
    const fechamentoFake: Fechamento = {
      id: 99,
      funcionarioId: 10,
      valor: 1650,
      status: 'PENDENTE',
      observacao: null,
      mesReferencia: '2026-06-01',
      dataCriacao: '2026-06-04T12:00:00',
    }
    resolverFechamento(fechamentoFake)
    await waitFor(() => expect(defaultProps.onConfirmado).toHaveBeenCalled())
  })
})
