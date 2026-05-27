export function CarregandoLista() {
  return (
    <div className="space-y-3 px-4 py-4" aria-busy="true" aria-label="Carregando pedidos">
      {[1, 2, 3].map((i) => (
        <div key={i} className="bg-white border border-zinc-200 rounded-xl p-3 animate-pulse">
          <div className="flex items-start justify-between gap-2 mb-2">
            <div className="h-4 bg-zinc-200 rounded w-48" />
            <div className="h-4 bg-zinc-200 rounded w-16" />
          </div>
          <div className="h-6 bg-zinc-200 rounded w-28 mb-2" />
          <div className="h-3 bg-zinc-100 rounded w-36" />
        </div>
      ))}
    </div>
  )
}
