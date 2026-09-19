import type { ReactNode } from 'react'
import { useLocation } from 'react-router-dom'
import { BrandMark } from './BrandMark'
import { StaticBackground } from './DynamicBackground'
import { LanguageSwitcher } from './LanguageSwitcher'

export function AuthPanel({ title, subtitle, footer, children }: {
  title: string
  subtitle: string
  footer: ReactNode
  children: ReactNode
}) {
  const { pathname } = useLocation()
  const isRegister = pathname === '/register'
  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-8 text-[#16181D] sm:px-6">
      <div className="fixed right-4 top-4 z-50 sm:right-6 sm:top-6"><LanguageSwitcher /></div>
      <StaticBackground className="auth-background" src={isRegister ? '/assets/kbase/background-04-mountain-clean.png' : '/assets/kbase/background-02-workspace-clean.png'} mobilePosition={isRegister ? '45% center' : '65% center'} />
      <div className="relative z-10 w-full max-w-[460px]">
        <section className="rounded-[26px] border border-white/60 bg-white/78 p-6 shadow-[0_24px_70px_rgba(32,44,94,.18)] backdrop-blur-[22px] sm:p-9">
          <div className="mb-7"><BrandMark /></div>
          <header className="mb-7">
            <h1 className="text-2xl font-bold tracking-[-0.025em] text-[#16181D] sm:text-[28px]">{title}</h1>
            <p className="mt-2 text-sm leading-6 text-[#667085]">{subtitle}</p>
          </header>
          {children}
          <div className="mt-7 border-t border-[#E7E9F0] pt-5 text-center text-sm text-[#667085]">{footer}</div>
        </section>
      </div>
    </main>
  )
}
