import { http, HttpResponse } from 'msw'
import { StatusPedido, TipoPagamento } from '../api/tipos'
import type { PedidoResumo, Pagina, ResumoMes, AuthMeResponse } from '../api/tipos'

const BASE = import.meta.env.VITE_API_BASE_URL

// Estado de autenticação simulado — reset ao recarregar a página
let autenticado = false

const pedidosFake: PedidoResumo[] = [
  {
    id: 142,
    valor: 287.5,
    descricao: 'Boleto Energia Elétrica',
    tipo: TipoPagamento.BOLETO,
    status: StatusPedido.PAGO,
    dataPedido: '2026-05-08',
    dataPagamento: '2026-05-09',
    temComprovante: true,
  },
  {
    id: 141,
    valor: 1500.0,
    descricao: 'Aluguel Maio',
    tipo: TipoPagamento.TED,
    status: StatusPedido.PAGO,
    dataPedido: '2026-05-05',
    dataPagamento: '2026-05-05',
    temComprovante: true,
  },
  {
    id: 140,
    valor: 350.0,
    descricao: 'Plano de Saúde',
    tipo: TipoPagamento.BOLETO,
    status: StatusPedido.PENDENTE,
    dataPedido: '2026-05-07',
    dataPagamento: null,
    temComprovante: false,
  },
  {
    id: 139,
    valor: 89.9,
    descricao: 'Internet Fibra',
    tipo: TipoPagamento.BOLETO,
    status: StatusPedido.PENDENTE,
    dataPedido: '2026-05-06',
    dataPagamento: null,
    temComprovante: false,
  },
  {
    id: 138,
    valor: 200.0,
    descricao: 'PIX Farmácia São João',
    tipo: TipoPagamento.PIX,
    status: StatusPedido.PAGO,
    dataPedido: '2026-05-03',
    dataPagamento: '2026-05-03',
    temComprovante: true,
  },
  {
    id: 137,
    valor: 4800.0,
    descricao: 'Agendamento IPTU Parcela 5',
    tipo: TipoPagamento.AGENDAMENTO,
    status: StatusPedido.PENDENTE,
    dataPedido: '2026-05-02',
    dataPagamento: null,
    temComprovante: false,
  },
  {
    id: 136,
    valor: 287.5,
    descricao: 'Boleto Energia Elétrica',
    tipo: TipoPagamento.BOLETO,
    status: StatusPedido.PAGO,
    dataPedido: '2026-04-28',
    dataPagamento: '2026-04-29',
    temComprovante: true,
  },
  {
    id: 135,
    valor: 1500.0,
    descricao: 'Aluguel Abril',
    tipo: TipoPagamento.TED,
    status: StatusPedido.PAGO,
    dataPedido: '2026-04-05',
    dataPagamento: '2026-04-05',
    temComprovante: true,
  },
  {
    id: 134,
    valor: 350.0,
    descricao: 'Plano de Saúde',
    tipo: TipoPagamento.BOLETO,
    status: StatusPedido.PAGO,
    dataPedido: '2026-04-10',
    dataPagamento: '2026-04-11',
    temComprovante: true,
  },
  {
    id: 133,
    valor: 450.0,
    descricao: 'Consulta Cardiologista',
    tipo: TipoPagamento.PIX,
    status: StatusPedido.PAGO,
    dataPedido: '2026-04-15',
    dataPagamento: '2026-04-15',
    temComprovante: true,
  },
  {
    id: 132,
    valor: 89.9,
    descricao: 'Internet Fibra',
    tipo: TipoPagamento.BOLETO,
    status: StatusPedido.PAGO,
    dataPedido: '2026-04-08',
    dataPagamento: '2026-04-09',
    temComprovante: true,
  },
  {
    id: 131,
    valor: 180.0,
    descricao: 'Mercado Extra',
    tipo: TipoPagamento.PIX,
    status: StatusPedido.PAGO,
    dataPedido: '2026-04-20',
    dataPagamento: '2026-04-20',
    temComprovante: true,
  },
  {
    id: 130,
    valor: 4800.0,
    descricao: 'IPTU Parcela 4',
    tipo: TipoPagamento.BOLETO,
    status: StatusPedido.PAGO,
    dataPedido: '2026-04-02',
    dataPagamento: '2026-04-03',
    temComprovante: true,
  },
  {
    id: 129,
    valor: 320.0,
    descricao: 'Condomínio',
    tipo: TipoPagamento.BOLETO,
    status: StatusPedido.PAGO,
    dataPedido: '2026-04-01',
    dataPagamento: '2026-04-02',
    temComprovante: true,
  },
  {
    id: 128,
    valor: 75.0,
    descricao: 'Farmácia Popular',
    tipo: TipoPagamento.PIX,
    status: StatusPedido.PAGO,
    dataPedido: '2026-04-25',
    dataPagamento: '2026-04-25',
    temComprovante: false,
  },
]

export const handlers = [
  http.get(`${BASE}/api/v1/pedidos`, ({ request }) => {
    const url = new URL(request.url)
    const status = url.searchParams.get('status') ?? 'todos'
    const de = url.searchParams.get('de')
    const ate = url.searchParams.get('ate')
    const busca = url.searchParams.get('busca')?.toLowerCase()
    const page = parseInt(url.searchParams.get('page') ?? '0')
    const tamanho = Math.min(parseInt(url.searchParams.get('tamanho') ?? '20'), 50)

    let filtrados = [...pedidosFake]

    if (status === 'pendente') {
      filtrados = filtrados.filter((p) => p.status === StatusPedido.PENDENTE)
    } else if (status === 'pago') {
      filtrados = filtrados.filter((p) => p.status === StatusPedido.PAGO)
    }

    if (de) filtrados = filtrados.filter((p) => p.dataPedido >= de)
    if (ate) filtrados = filtrados.filter((p) => p.dataPedido <= ate)

    if (busca) {
      filtrados = filtrados.filter(
        (p) =>
          p.descricao.toLowerCase().includes(busca) ||
          p.valor.toString().includes(busca),
      )
    }

    // Ordem: dataPedido DESC, id DESC
    filtrados.sort((a, b) => {
      if (b.dataPedido !== a.dataPedido) return b.dataPedido.localeCompare(a.dataPedido)
      return b.id - a.id
    })

    const total = filtrados.length
    const totalPaginas = Math.ceil(total / tamanho)
    const items = filtrados.slice(page * tamanho, (page + 1) * tamanho)

    const resposta: Pagina<PedidoResumo> = { items, total, pagina: page, tamanho, totalPaginas }
    return HttpResponse.json(resposta)
  }),

  http.get(`${BASE}/api/v1/pedidos/:id`, ({ params }) => {
    const pedido = pedidosFake.find((p) => p.id === Number(params['id']))
    if (!pedido) return new HttpResponse(null, { status: 404 })
    const { temComprovante: _tc, ...detalhe } = pedido
    return HttpResponse.json(detalhe)
  }),

  http.get(`${BASE}/api/v1/pedidos/:id/foto-pedido`, () => {
    return new HttpResponse(null, {
      status: 302,
      headers: {
        Location: 'https://placehold.co/600x400/e2e8f0/475569?text=Foto+Pedido',
        'Cache-Control': 'private, max-age=600',
      },
    })
  }),

  http.get(`${BASE}/api/v1/pedidos/:id/comprovante`, ({ params }) => {
    const pedido = pedidosFake.find((p) => p.id === Number(params['id']))
    if (!pedido || pedido.status !== StatusPedido.PAGO || !pedido.temComprovante) {
      return new HttpResponse(null, { status: 404 })
    }
    return new HttpResponse(null, {
      status: 302,
      headers: {
        Location: 'https://placehold.co/600x800/f0fdf4/166534?text=Comprovante',
        'Cache-Control': 'private, max-age=600',
      },
    })
  }),

  http.get(`${BASE}/api/v1/resumo`, () => {
    const mesAtual = '2026-05'
    const pedidosMes = pedidosFake.filter((p) => p.dataPedido.startsWith(mesAtual))
    const pendentes = pedidosMes.filter((p) => p.status === StatusPedido.PENDENTE)
    const pagos = pedidosMes.filter((p) => p.status === StatusPedido.PAGO)

    const resumo: ResumoMes = {
      mesAtual,
      pendentes: {
        quantidade: pendentes.length,
        total: pendentes.reduce((acc, p) => acc + p.valor, 0),
      },
      pagos: {
        quantidade: pagos.length,
        total: pagos.reduce((acc, p) => acc + p.valor, 0),
      },
    }
    return HttpResponse.json(resumo)
  }),

  http.post(`${BASE}/api/v1/auth/exchange`, async ({ request }) => {
    const body = (await request.json()) as { token?: string }
    if (!body.token) return new HttpResponse(null, { status: 401 })
    autenticado = true
    const resposta: AuthMeResponse = { requisitante: { id: 1, nome: 'Pedro Marques' } }
    return HttpResponse.json(resposta)
  }),

  http.get(`${BASE}/api/v1/auth/me`, () => {
    if (!autenticado) return new HttpResponse(null, { status: 401 })
    const resposta: AuthMeResponse = { requisitante: { id: 1, nome: 'Pedro Marques' } }
    return HttpResponse.json(resposta)
  }),
]
