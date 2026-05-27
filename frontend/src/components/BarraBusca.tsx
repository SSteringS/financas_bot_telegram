import { useState, useEffect, useRef } from 'react'

interface BarraBuscaProps {
  value: string
  onChange: (value: string) => void
  placeholder?: string
}

export function BarraBusca({ value, onChange, placeholder = 'Buscar pedido…' }: BarraBuscaProps) {
  const [inputValue, setInputValue] = useState(value)
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Sincroniza se o value externo mudar (ex: reset de filtros)
  useEffect(() => {
    setInputValue(value)
  }, [value])

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const novoValor = e.target.value
    setInputValue(novoValor)

    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      onChange(novoValor)
    }, 300)
  }

  // Limpa timeout ao desmontar
  useEffect(() => {
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [])

  return (
    <div className="relative">
      <label htmlFor="barra-busca" className="sr-only">
        {placeholder}
      </label>
      <div className="absolute inset-y-0 left-3 flex items-center pointer-events-none">
        <svg
          className="w-4 h-4 text-zinc-400"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
          />
        </svg>
      </div>
      <input
        id="barra-busca"
        type="search"
        value={inputValue}
        onChange={handleChange}
        placeholder={placeholder}
        className="w-full pl-9 pr-4 py-2.5 bg-zinc-50 border border-zinc-200 rounded-xl text-sm text-zinc-900 placeholder:text-zinc-400 focus:outline-none focus:ring-2 focus:ring-zinc-900 focus:border-transparent"
      />
    </div>
  )
}
