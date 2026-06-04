import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react'
import { FuncionarioForm } from './FuncionarioForm'
import { FormaPagamento } from '../../types/folha'
import type { Funcionario } from '../../types/folha'

const funcionarioPixFake: Funcionario = {
  id: 1,
  nome: 'Maria das Graças',
  salarioBase: 2000,
  formaPagamento: FormaPagamento.PIX,
  chavePix: '11999998888',
  banco: null,
  agencia: null,
  conta: null,
  tipoConta: null,
  contaPropria: true,
  obsPagamento: null,
  diaPagamentoReferencia: null,
  ativo: true,
  criadoEm: '2026-06-01T10:00:00',
  atualizadoEm: '2026-06-01T10:00:00',
}

const funcionarioTedFake: Funcionario = {
  ...funcionarioPixFake,
  id: 2,
  nome: 'José da Silva',
  formaPagamento: FormaPagamento.TED,
  chavePix: null,
  banco: 'Bradesco',
  agencia: '0001',
  conta: '12345-6',
  tipoConta: 'CORRENTE',
  contaPropria: false,
}

describe('FuncionarioForm — campos condicionais por forma de pagamento', () => {
  it('exibe campo chave PIX quando forma=PIX é selecionada', () => {
    render(
      <FuncionarioForm onSubmit={vi.fn()} onCancelar={vi.fn()} />,
    )
    // Default é PIX
    expect(screen.getByTestId('campos-pix')).toBeInTheDocument()
    expect(screen.queryByTestId('campos-ted')).not.toBeInTheDocument()
  })

  it('exibe campos TED (banco, agência, conta, tipo) quando forma=TED é selecionada', () => {
    render(
      <FuncionarioForm onSubmit={vi.fn()} onCancelar={vi.fn()} />,
    )
    const select = screen.getByLabelText(/forma de pagamento/i)
    fireEvent.change(select, { target: { value: FormaPagamento.TED } })

    const tedSection = screen.getByTestId('campos-ted')
    expect(tedSection).toBeInTheDocument()
    expect(screen.queryByTestId('campos-pix')).not.toBeInTheDocument()
    // Escopados dentro do container TED para evitar ambiguidade com "Conta própria"
    expect(within(tedSection).getByLabelText(/banco/i)).toBeInTheDocument()
    expect(within(tedSection).getByLabelText(/agência/i)).toBeInTheDocument()
    expect(within(tedSection).getByLabelText(/^conta/i)).toBeInTheDocument()
    expect(within(tedSection).getByLabelText(/tipo de conta/i)).toBeInTheDocument()
  })

  it('ao trocar de TED para PIX, oculta campos TED e exibe campo PIX', () => {
    render(
      <FuncionarioForm onSubmit={vi.fn()} onCancelar={vi.fn()} />,
    )
    const select = screen.getByLabelText(/forma de pagamento/i)

    fireEvent.change(select, { target: { value: FormaPagamento.TED } })
    expect(screen.getByTestId('campos-ted')).toBeInTheDocument()

    fireEvent.change(select, { target: { value: FormaPagamento.PIX } })
    expect(screen.getByTestId('campos-pix')).toBeInTheDocument()
    expect(screen.queryByTestId('campos-ted')).not.toBeInTheDocument()
  })
})

describe('FuncionarioForm — modo edição', () => {
  it('preenche os campos com os dados do funcionário PIX no modo edição', () => {
    render(
      <FuncionarioForm
        funcionario={funcionarioPixFake}
        onSubmit={vi.fn()}
        onCancelar={vi.fn()}
      />,
    )
    expect(screen.getByLabelText(/nome completo/i)).toHaveValue('Maria das Graças')
    expect(screen.getByLabelText(/salário base/i)).toHaveValue(2000)
    expect(screen.getByLabelText(/chave pix/i)).toHaveValue('11999998888')
  })

  it('preenche os campos TED no modo edição com funcionário TED', () => {
    render(
      <FuncionarioForm
        funcionario={funcionarioTedFake}
        onSubmit={vi.fn()}
        onCancelar={vi.fn()}
      />,
    )
    expect(screen.getByTestId('campos-ted')).toBeInTheDocument()
    expect(screen.getByLabelText(/banco/i)).toHaveValue('Bradesco')
    expect(screen.getByLabelText(/agência/i)).toHaveValue('0001')
  })

  it('botão de submit exibe texto correto no modo edição vs criação', () => {
    const { rerender } = render(
      <FuncionarioForm onSubmit={vi.fn()} onCancelar={vi.fn()} />,
    )
    expect(screen.getByRole('button', { name: /cadastrar funcionário/i })).toBeInTheDocument()

    rerender(
      <FuncionarioForm
        funcionario={funcionarioPixFake}
        onSubmit={vi.fn()}
        onCancelar={vi.fn()}
      />,
    )
    expect(screen.getByRole('button', { name: /salvar alterações/i })).toBeInTheDocument()
  })
})

describe('FuncionarioForm — badge conta de terceiro', () => {
  it('exibe aviso quando contaPropria está desmarcado', () => {
    render(
      <FuncionarioForm
        funcionario={funcionarioTedFake}
        onSubmit={vi.fn()}
        onCancelar={vi.fn()}
      />,
    )
    expect(screen.getByText(/conta de terceiro/i)).toBeInTheDocument()
  })
})

describe('FuncionarioForm — submissão', () => {
  it('chama onCancelar ao clicar em Cancelar', () => {
    const onCancelar = vi.fn()
    render(
      <FuncionarioForm onSubmit={vi.fn()} onCancelar={onCancelar} />,
    )
    fireEvent.click(screen.getByRole('button', { name: /cancelar/i }))
    expect(onCancelar).toHaveBeenCalledOnce()
  })

  it('chama onSubmit com os dados corretos ao submeter formulário PIX válido', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    render(
      <FuncionarioForm onSubmit={onSubmit} onCancelar={vi.fn()} />,
    )

    fireEvent.change(screen.getByLabelText(/nome completo/i), { target: { value: 'Teste Silva' } })
    fireEvent.change(screen.getByLabelText(/salário base/i), { target: { value: '1500' } })
    fireEvent.change(screen.getByLabelText(/chave pix/i), { target: { value: 'teste@email.com' } })
    fireEvent.submit(screen.getByTestId('funcionario-form'))

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          nome: 'Teste Silva',
          salarioBase: 1500,
          formaPagamento: FormaPagamento.PIX,
          chavePix: 'teste@email.com',
        }),
      )
    })
  })
})
