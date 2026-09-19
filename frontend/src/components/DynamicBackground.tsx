import { useCallback, useEffect, useState, type CSSProperties } from 'react'

export type BackgroundTheme = 'light' | 'dark'

export type BackgroundSlide = {
  src: string
  desktopPosition: string
  mobilePosition: string
  theme: BackgroundTheme
}

export const backgroundSlides: BackgroundSlide[] = [
  { src: '/assets/kbase/background-01-city-clean.png', desktopPosition: 'center center', mobilePosition: '58% center', theme: 'light' },
  { src: '/assets/kbase/background-02-workspace-clean.png', desktopPosition: 'center center', mobilePosition: '65% center', theme: 'light' },
  { src: '/assets/kbase/background-03-book-clean.png', desktopPosition: 'center center', mobilePosition: '58% center', theme: 'dark' },
  { src: '/assets/kbase/background-04-mountain-clean.png', desktopPosition: 'center center', mobilePosition: '45% center', theme: 'light' },
]

function useReducedMotion() {
  const [reduced, setReduced] = useState(() => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false)
  useEffect(() => {
    const media = window.matchMedia?.('(prefers-reduced-motion: reduce)')
    if (!media) return
    const update = () => setReduced(media.matches)
    media.addEventListener?.('change', update)
    return () => media.removeEventListener?.('change', update)
  }, [])
  return reduced
}

export function DynamicBackground({ intervalMs = 7000, onThemeChange }: {
  intervalMs?: number
  onThemeChange?: (theme: BackgroundTheme) => void
}) {
  const reducedMotion = useReducedMotion()
  const [current, setCurrent] = useState(0)
  const [previous, setPrevious] = useState<number | null>(null)
  const [paused, setPaused] = useState(false)
  const [visible, setVisible] = useState(() => !document.hidden)

  const selectSlide = useCallback((index: number) => {
    setCurrent((active) => {
      if (active === index) return active
      setPrevious(active)
      return index
    })
  }, [])

  const advance = useCallback(() => {
    setCurrent((active) => {
      setPrevious(active)
      return (active + 1) % backgroundSlides.length
    })
  }, [])

  useEffect(() => {
    backgroundSlides.forEach(({ src }) => { const image = new Image(); image.src = src })
  }, [])

  useEffect(() => {
    onThemeChange?.(backgroundSlides[current].theme)
  }, [current, onThemeChange])

  useEffect(() => {
    const handleVisibility = () => setVisible(!document.hidden)
    document.addEventListener('visibilitychange', handleVisibility)
    return () => document.removeEventListener('visibilitychange', handleVisibility)
  }, [])

  useEffect(() => {
    if (reducedMotion || paused || !visible) return
    const timer = window.setTimeout(advance, intervalMs)
    return () => window.clearTimeout(timer)
  }, [advance, current, intervalMs, paused, reducedMotion, visible])

  const slide = backgroundSlides[current]
  return (
    <>
      <div className={`dynamic-background theme-${slide.theme}`} data-testid="dynamic-background">
        {previous !== null && (
          <picture className="background-layer background-layer-previous" aria-hidden="true">
            <source media="(max-width: 640px)" srcSet={backgroundSlides[previous].src} />
            <img src={backgroundSlides[previous].src} alt="" style={{ '--desktop-position': backgroundSlides[previous].desktopPosition, '--mobile-position': backgroundSlides[previous].mobilePosition } as CSSProperties} />
          </picture>
        )}
        <picture key={current} className={`background-layer background-layer-current ${reducedMotion ? 'no-motion' : ''}`} aria-hidden="true">
          <source media="(max-width: 640px)" srcSet={slide.src} />
          <img src={slide.src} alt="" style={{ '--desktop-position': slide.desktopPosition, '--mobile-position': slide.mobilePosition } as CSSProperties} />
        </picture>
        <div className="background-overlay" aria-hidden="true" />
      </div>
      <div className="slideshow-controls">
        <button type="button" className="slideshow-pause" onClick={() => setPaused((value) => !value)} aria-label={paused ? 'Resume background slideshow' : 'Pause background slideshow'}>{paused ? '▶' : 'Ⅱ'}</button>
        <div className="slideshow-indicators" aria-label="Choose background">
          {backgroundSlides.map((item, index) => <button key={item.src} type="button" aria-label={`Show background ${index + 1}`} aria-current={index === current ? 'true' : undefined} onClick={() => selectSlide(index)} className={index === current ? 'active' : ''} />)}
        </div>
      </div>
    </>
  )
}

export function StaticBackground({ src, position = 'center center', mobilePosition = position, theme = 'light', className = '' }: {
  src: string
  position?: string
  mobilePosition?: string
  theme?: BackgroundTheme
  className?: string
}) {
  return <div className={`dynamic-background static-background theme-${theme} ${className}`} aria-hidden="true"><img src={src} alt="" style={{ '--desktop-position': position, '--mobile-position': mobilePosition } as CSSProperties} /><div className="background-overlay" /></div>
}
