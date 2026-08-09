import { describe, it, expect, vi, beforeAll, afterEach, afterAll } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { Home } from './Home'

vi.mock('../lib/ambiente', () => ({ API_BASE_URL: 'http://test-api' }))
vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({ requisitante: { id: 1, nome: 'Pedro Marques' }, status: 'autenticado' }),
  invalidarCacheAuth: vi.fn(),
}))

const pedidosMaio = [
  { id: 142, valor: 287.5, descricao: 'Boleto Energia Elétrica', tipo: 'BOLETO', status: 'PAGO', dataPedido: '2026-05-08', dataPagamento: '2026-05-09', temComprovante: true },
  { id: 141, valor: 1500, descricao: 'Aluguel Maio', tipo: 'TED', status: 'PAGO', dataPedido: '2026-05-05', dataPagamento: '2026-05-05', temComprovante: true },
  { id: 140, valor: 350, descricao: 'Plano de Saúde', tipo: 'BOLETO', status: 'PENDENTE', dataPedido: '2026-05-07', dataPagamento: null, temComprovante: false },
  { id: 139, valor: 89.9, descricao: 'Internet Fibra', tipo: 'BOLETO', status: 'PENDENTE', dataPedido: '2026-05-06', dataPagamento: null, temComprovante: false },
  { id: 138, valor: 200, descricao: 'PIX Farmácia São João', tipo: 'PIX', status: 'PAGO', dataPedido: '2026-05-03', dataPagamento: '2026-05-03', temComprovante: true },
  { id: 137, valor: 4800, descricao: 'Agendamento IPTU Parcela 5', tipo: 'AGENDAMENTO', status: 'PENDENTE', dataPedido: '2026-05-02', dataPagamento: null, temComprovante: false },
]

const resumoMaio = {
  mes: '2026-05',
  todos: { quantidade: 6, total: 7227.4 },
  pendentes: { quantidade: 3, total: 5239.9 },
  pagos: { quantidade: 3, total: 1987.5 },
}

const resumoAbril = {
  mes: '2026-04',
  todos: { quantidade: 9, total: 7777.4 },
  pendentes: { quantidade: 0, total: 0 },
  pagos: { quantidade: 9, total: 7777.4 },
}

const resumoEnergia = {
  mes: '2026-05',
  todos: { quantidade: 1, total: 287.5 },
  pendentes: { quantidade: 0, total: 0 },
  pagos: { quantidade: 1, total: 287.5 },
}

const server = setupServer(
  http.get('http://test-api/api/v1/resumo', ({ request }) => {
    const url = new URL(request.url)
    const mes = url.searchParams.get('mes') ?? '2026-05'
    const busca = url.searchParams.get('busca')?.toLowerCase()
    if (busca?.includes('energia')) return HttpResponse.json(resumoEnergia)
    if (mes === '2026-04') return HttpResponse.json(resumoAbril)
    return HttpResponse.json(resumoMaio)
  }),
  http.get('http://test-api/api/v1/pedidos', ({ request }) => {
    const url = new URL(request.url)
    const status = url.searchParams.get('status') ?? 'todos'
    let items = [...pedidosMaio]
    if (status === 'pendente') items = items.filter((p) => p.status === 'PENDENTE')
    else if (status === 'pago') items = items.filter((p) => p.status === 'PAGO')
    return HttpResponse.json({ items, total: items.length, pagina: 0, tamanho: 20, totalPaginas: 1 })
  }),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

function renderHome(url = '/?mes=2026-05') {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[url]}>
        <Home />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('Home', () => {
  it('carga inicial em maio: contadores mostram Tudo(6) Pendente(3) Pago(3)', async () => {
    renderHome('/?mes=2026-05')
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Tudo \(6\)/ })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /Pendente \(3\)/ })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /Pago \(3\)/ })).toBeInTheDocument()
    })
  })

  it('regressão Bug A: clicar em Pendente não colapsa os contadores', async () => {
    renderHome('/?mes=2026-05')
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Tudo \(6\)/ })).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: /Pendente \(3\)/ }))
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Tudo \(6\)/ })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /Pendente \(3\)/ })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /Pago \(3\)/ })).toBeInTheDocument()
    })
  })

  it('troca de mês via URL: ?mes=2026-04 mostra contadores de abril', async () => {
    renderHome('/?mes=2026-04')
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Tudo \(9\)/ })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /Pendente \(0\)/ })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /Pago \(9\)/ })).toBeInTheDocument()
    })
  })

  it('busca + contadores: ?busca=energia reflete só os matches', async () => {
    renderHome('/?mes=2026-05&busca=energia')
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Tudo \(1\)/ })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /Pendente \(0\)/ })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /Pago \(1\)/ })).toBeInTheDocument()
    })
  })

  it('clicar em "Ver foto/PDF do pedido" abre modal de foto', async () => {
    renderHome('/?mes=2026-05')
    await waitFor(() => screen.getAllByRole('button', { name: /ver foto\/pdf do pedido/i }))
    const botoesFoto = screen.getAllByRole('button', { name: /ver foto\/pdf do pedido/i })
    fireEvent.click(botoesFoto[0])
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('Foto/PDF do pedido')).toBeInTheDocument()
  })

  it('clicar em "Ver comprovante" abre modal de comprovante', async () => {
    renderHome('/?mes=2026-05')
    await waitFor(() => screen.getAllByRole('button', { name: /ver comprovante de/i }))
    const botoesComp = screen.getAllByRole('button', { name: /ver comprovante de/i })
    fireEvent.click(botoesComp[0])
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('Comprovante')).toBeInTheDocument()
  })

  it('modais são mutuamente exclusivos: abrir foto fecha comprovante', async () => {
    renderHome('/?mes=2026-05')
    await waitFor(() => screen.getAllByRole('button', { name: /ver comprovante de/i }))

    fireEvent.click(screen.getAllByRole('button', { name: /ver comprovante de/i })[0])
    expect(screen.getByText('Comprovante')).toBeInTheDocument()

    fireEvent.click(screen.getAllByRole('button', { name: /ver foto\/pdf do pedido/i })[0])
    expect(screen.queryByText('Comprovante')).not.toBeInTheDocument()
    expect(screen.getByText('Foto/PDF do pedido')).toBeInTheDocument()
  })

  it('modais são mutuamente exclusivos: abrir comprovante fecha foto', async () => {
    renderHome('/?mes=2026-05')
    await waitFor(() => screen.getAllByRole('button', { name: /ver foto\/pdf do pedido/i }))

    fireEvent.click(screen.getAllByRole('button', { name: /ver foto\/pdf do pedido/i })[0])
    expect(screen.getByText('Foto/PDF do pedido')).toBeInTheDocument()

    fireEvent.click(screen.getAllByRole('button', { name: /ver comprovante de/i })[0])
    expect(screen.queryByText('Foto/PDF do pedido')).not.toBeInTheDocument()
    expect(screen.getByText('Comprovante')).toBeInTheDocument()
  })
})
