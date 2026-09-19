import { act, fireEvent, render, screen } from '@testing-library/react'
import { DynamicBackground, backgroundSlides } from './DynamicBackground'

function mockMotion(reduced: boolean) {
  vi.stubGlobal('matchMedia', vi.fn().mockReturnValue({
    matches: reduced,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  }))
}

describe('DynamicBackground', () => {
  beforeEach(() => { vi.useFakeTimers(); mockMotion(false) })
  afterEach(() => { vi.useRealTimers(); vi.unstubAllGlobals() })

  it('configures all four supplied backgrounds and renders the first image', () => {
    expect(backgroundSlides.map((slide) => slide.src)).toEqual([
      '/assets/kbase/background-01-city-clean.png',
      '/assets/kbase/background-02-workspace-clean.png',
      '/assets/kbase/background-03-book-clean.png',
      '/assets/kbase/background-04-mountain-clean.png',
    ])
    render(<DynamicBackground />)
    expect(document.querySelector('img[src="/assets/kbase/background-01-city-clean.png"]')).toBeInTheDocument()
  })

  it('advances after seven seconds', () => {
    render(<DynamicBackground />)
    act(() => vi.advanceTimersByTime(7000))
    expect(document.querySelector('img[src="/assets/kbase/background-02-workspace-clean.png"]')).toBeInTheDocument()
  })

  it('selects a slide manually and restarts autoplay from that selection', () => {
    render(<DynamicBackground />)
    act(() => vi.advanceTimersByTime(3000))
    fireEvent.click(screen.getByRole('button', { name: 'Show background 4' }))
    act(() => vi.advanceTimersByTime(6999))
    expect(screen.getByRole('button', { name: 'Show background 4' })).toHaveAttribute('aria-current', 'true')
    act(() => vi.advanceTimersByTime(1))
    expect(screen.getByRole('button', { name: 'Show background 1' })).toHaveAttribute('aria-current', 'true')
  })

  it('keeps the first slide static when reduced motion is requested', () => {
    mockMotion(true)
    render(<DynamicBackground />)
    act(() => vi.advanceTimersByTime(21000))
    expect(screen.getByRole('button', { name: 'Show background 1' })).toHaveAttribute('aria-current', 'true')
  })
})
