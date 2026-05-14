import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, act } from '@testing-library/react'
import { BarraBusca } from './BarraBusca'

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
})

describe('BarraBusca', () => {
  it('renderiza com placeholder padrão', () => {
    vi.useRealTimers()
    render(<BarraBusca value="" onChange={vi.fn()} />)
    expect(screen.getByPlaceholderText('Buscar pedido…')).toBeInTheDocument()
    vi.useFakeTimers()
  })

  it('não chama onChange antes de 300ms', () => {
    const onChange = vi.fn()
    render(<BarraBusca value="" onChange={onChange} />)
    const input = screen.getByRole('searchbox')

    fireEvent.change(input, { target: { value: 'abc' } })
    expect(onChange).not.toHaveBeenCalled()

    act(() => { vi.advanceTimersByTime(299) })
    expect(onChange).not.toHaveBeenCalled()
  })

  it('chama onChange após 300ms', () => {
    const onChange = vi.fn()
    render(<BarraBusca value="" onChange={onChange} />)
    const input = screen.getByRole('searchbox')

    fireEvent.change(input, { target: { value: 'abc' } })
    act(() => { vi.advanceTimersByTime(300) })
    expect(onChange).toHaveBeenCalledWith('abc')
  })

  it('debounce: digitação rápida resulta em uma única chamada', () => {
    const onChange = vi.fn()
    render(<BarraBusca value="" onChange={onChange} />)
    const input = screen.getByRole('searchbox')

    fireEvent.change(input, { target: { value: 'a' } })
    act(() => { vi.advanceTimersByTime(100) })
    fireEvent.change(input, { target: { value: 'ab' } })
    act(() => { vi.advanceTimersByTime(100) })
    fireEvent.change(input, { target: { value: 'abc' } })
    act(() => { vi.advanceTimersByTime(300) })

    expect(onChange).toHaveBeenCalledTimes(1)
    expect(onChange).toHaveBeenCalledWith('abc')
  })
})
