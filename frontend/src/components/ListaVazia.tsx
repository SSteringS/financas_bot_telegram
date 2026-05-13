export function ListaVazia() {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-8 text-center">
      <div className="w-16 h-16 bg-zinc-100 rounded-full flex items-center justify-center mb-4">
        <svg
          className="w-8 h-8 text-zinc-300"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.5}
            d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
          />
        </svg>
      </div>
      <p className="text-sm font-medium text-zinc-500">Nenhum pedido neste filtro</p>
      <p className="text-xs text-zinc-500 mt-1">Tente mudar o mês ou o filtro de status</p>
    </div>
  )
}
