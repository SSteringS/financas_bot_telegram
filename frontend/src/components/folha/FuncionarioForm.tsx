import { useState } from 'react'
import { FormaPagamento } from '../../types/folha'
import type { Funcionario, FuncionarioRequest, TipoConta } from '../../types/folha'
import { formatarMoeda } from '../../lib/formato'

interface FuncionarioFormProps {
  /** Quando definido, o form está no modo edição com os dados do funcionário */
  funcionario?: Funcionario
  onSubmit: (data: FuncionarioRequest) => Promise<void>
  onCancelar: () => void
}

const FORMA_PAGAMENTO_OPTIONS = [
  { value: FormaPagamento.PIX, label: 'PIX' },
  { value: FormaPagamento.TED, label: 'TED (transferência bancária)' },
]

const TIPO_CONTA_OPTIONS: { value: TipoConta; label: string }[] = [
  { value: 'CORRENTE', label: 'Corrente' },
  { value: 'POUPANCA', label: 'Poupança' },
]

export function FuncionarioForm({ funcionario, onSubmit, onCancelar }: FuncionarioFormProps) {
  const [nome, setNome] = useState(funcionario?.nome ?? '')
  const [salarioBase, setSalarioBase] = useState(funcionario?.salarioBase?.toString() ?? '')
  const [formaPagamento, setFormaPagamento] = useState<FormaPagamento>(
    funcionario?.formaPagamento ?? FormaPagamento.PIX,
  )
  const [chavePix, setChavePix] = useState(funcionario?.chavePix ?? '')
  const [banco, setBanco] = useState(funcionario?.banco ?? '')
  const [agencia, setAgencia] = useState(funcionario?.agencia ?? '')
  const [conta, setConta] = useState(funcionario?.conta ?? '')
  const [tipoConta, setTipoConta] = useState<TipoConta>(
    (funcionario?.tipoConta as TipoConta) ?? 'CORRENTE',
  )
  const [contaPropria, setContaPropria] = useState(funcionario?.contaPropria ?? true)
  const [obsPagamento, setObsPagamento] = useState(funcionario?.obsPagamento ?? '')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  const modoEdicao = funcionario !== undefined
  const isPix = formaPagamento === FormaPagamento.PIX

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setErro(null)

    const salario = parseFloat(salarioBase.replace(',', '.'))
    if (isNaN(salario) || salario <= 0) {
      setErro('Salário base deve ser um valor positivo.')
      return
    }

    const payload: FuncionarioRequest = {
      nome: nome.trim(),
      salarioBase: salario,
      formaPagamento,
      contaPropria,
      obsPagamento: obsPagamento.trim() || undefined,
    }

    if (isPix) {
      payload.chavePix = chavePix.trim()
    } else {
      payload.banco = banco.trim()
      payload.agencia = agencia.trim()
      payload.conta = conta.trim()
      payload.tipoConta = tipoConta
    }

    try {
      setEnviando(true)
      await onSubmit(payload)
    } catch (err: unknown) {
      if (err instanceof Error) {
        setErro(err.message)
      } else {
        setErro('Erro ao salvar funcionário.')
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4" data-testid="funcionario-form">
      {/* Nome */}
      <div>
        <label className="block text-sm font-medium text-zinc-700 mb-1" htmlFor="func-nome">
          Nome completo <span className="text-red-500">*</span>
        </label>
        <input
          id="func-nome"
          type="text"
          value={nome}
          onChange={(e) => setNome(e.target.value)}
          required
          maxLength={120}
          placeholder="Ex: Maria das Graças"
          className="w-full border border-zinc-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-400"
        />
      </div>

      {/* Salário base */}
      <div>
        <label className="block text-sm font-medium text-zinc-700 mb-1" htmlFor="func-salario">
          Salário base (R$) <span className="text-red-500">*</span>
        </label>
        <input
          id="func-salario"
          type="number"
          value={salarioBase}
          onChange={(e) => setSalarioBase(e.target.value)}
          required
          min="0.01"
          step="0.01"
          placeholder="0,00"
          className="w-full border border-zinc-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-400"
        />
      </div>

      {/* Forma de pagamento */}
      <div>
        <label className="block text-sm font-medium text-zinc-700 mb-1" htmlFor="func-forma">
          Forma de pagamento <span className="text-red-500">*</span>
        </label>
        <select
          id="func-forma"
          value={formaPagamento}
          onChange={(e) => setFormaPagamento(e.target.value as FormaPagamento)}
          className="w-full border border-zinc-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-400 bg-white"
        >
          {FORMA_PAGAMENTO_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>

      {/* Campos condicionais — PIX */}
      {isPix && (
        <div data-testid="campos-pix">
          <label className="block text-sm font-medium text-zinc-700 mb-1" htmlFor="func-chave-pix">
            Chave PIX <span className="text-red-500">*</span>
          </label>
          <input
            id="func-chave-pix"
            type="text"
            value={chavePix}
            onChange={(e) => setChavePix(e.target.value)}
            required={isPix}
            placeholder="CPF, e-mail, telefone ou chave aleatória"
            className="w-full border border-zinc-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-400"
          />
        </div>
      )}

      {/* Campos condicionais — TED */}
      {!isPix && (
        <div className="space-y-3" data-testid="campos-ted">
          <div>
            <label className="block text-sm font-medium text-zinc-700 mb-1" htmlFor="func-banco">
              Banco <span className="text-red-500">*</span>
            </label>
            <input
              id="func-banco"
              type="text"
              value={banco}
              onChange={(e) => setBanco(e.target.value)}
              required={!isPix}
              placeholder="Ex: Bradesco, Itaú, Nubank"
              maxLength={50}
              className="w-full border border-zinc-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-400"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-zinc-700 mb-1" htmlFor="func-agencia">
                Agência <span className="text-red-500">*</span>
              </label>
              <input
                id="func-agencia"
                type="text"
                value={agencia}
                onChange={(e) => setAgencia(e.target.value)}
                required={!isPix}
                placeholder="0000"
                maxLength={20}
                className="w-full border border-zinc-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-400"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-zinc-700 mb-1" htmlFor="func-conta">
                Conta <span className="text-red-500">*</span>
              </label>
              <input
                id="func-conta"
                type="text"
                value={conta}
                onChange={(e) => setConta(e.target.value)}
                required={!isPix}
                placeholder="00000-0"
                maxLength={30}
                className="w-full border border-zinc-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-400"
              />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-zinc-700 mb-1" htmlFor="func-tipo-conta">
              Tipo de conta <span className="text-red-500">*</span>
            </label>
            <select
              id="func-tipo-conta"
              value={tipoConta}
              onChange={(e) => setTipoConta(e.target.value as TipoConta)}
              className="w-full border border-zinc-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-400 bg-white"
            >
              {TIPO_CONTA_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
        </div>
      )}

      {/* Conta própria */}
      <div className="flex items-center gap-2">
        <input
          id="func-conta-propria"
          type="checkbox"
          checked={contaPropria}
          onChange={(e) => setContaPropria(e.target.checked)}
          className="w-4 h-4 accent-zinc-800"
        />
        <label className="text-sm text-zinc-700 cursor-pointer" htmlFor="func-conta-propria">
          Conta própria do funcionário
        </label>
      </div>
      {!contaPropria && (
        <p className="text-xs text-amber-600 bg-amber-50 px-3 py-2 rounded-lg">
          Pagamento será feito para conta de terceiro (familiar ou representante).
        </p>
      )}

      {/* Observações */}
      <div>
        <label className="block text-sm font-medium text-zinc-700 mb-1" htmlFor="func-obs">
          Observações de pagamento
        </label>
        <textarea
          id="func-obs"
          value={obsPagamento}
          onChange={(e) => setObsPagamento(e.target.value)}
          maxLength={500}
          rows={2}
          placeholder="Ex: Pagar até o dia 5"
          className="w-full border border-zinc-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-400 resize-none"
        />
      </div>

      {/* Erro */}
      {erro && (
        <p className="text-sm text-red-600 bg-red-50 px-3 py-2 rounded-lg" role="alert">
          {erro}
        </p>
      )}

      {/* Ações */}
      <div className="flex gap-2 pt-2">
        <button
          type="button"
          onClick={onCancelar}
          disabled={enviando}
          className="flex-1 py-2.5 border border-zinc-300 rounded-xl text-sm font-medium text-zinc-700 hover:bg-zinc-50 transition-colors disabled:opacity-50"
        >
          Cancelar
        </button>
        <button
          type="submit"
          disabled={enviando}
          className="flex-1 py-2.5 bg-zinc-900 text-white rounded-xl text-sm font-semibold hover:bg-zinc-700 transition-colors disabled:opacity-50"
        >
          {enviando ? 'Salvando…' : modoEdicao ? 'Salvar alterações' : 'Cadastrar funcionário'}
        </button>
      </div>

      {/* Resumo salário (modo edição) */}
      {modoEdicao && funcionario && (
        <p className="text-xs text-zinc-400 text-center">
          Salário atual: {formatarMoeda(funcionario.salarioBase)}
        </p>
      )}
    </form>
  )
}
