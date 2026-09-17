import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

export function AuthPanel({ title, subtitle, footer, children }: {
  title: string
  subtitle: string
  footer: ReactNode
  children: ReactNode
}) {
  return (
    <main className="relative grid min-h-screen place-items-center overflow-hidden bg-slate-950 px-4 py-10 text-slate-100">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(34,211,238,.13),transparent_34%),radial-gradient(circle_at_80%_75%,rgba(99,102,241,.12),transparent_36%)]" />
      <div className="relative w-full max-w-md">
        <Link to="/" className="mb-7 flex w-fit items-center gap-3 rounded-lg focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-cyan-300">
          <span className="grid h-11 w-11 place-items-center rounded-xl bg-cyan-400 text-lg font-black text-slate-950">K</span>
          <span className="text-xl font-semibold tracking-tight">KBase</span>
        </Link>
        <section className="rounded-2xl border border-white/10 bg-slate-900/80 p-6 shadow-2xl shadow-black/30 backdrop-blur sm:p-8">
          <header className="mb-7">
            <h1 className="text-2xl font-semibold tracking-tight text-white">{title}</h1>
            <p className="mt-2 text-sm leading-6 text-slate-400">{subtitle}</p>
          </header>
          {children}
          <div className="mt-7 border-t border-white/10 pt-5 text-center text-sm text-slate-400">{footer}</div>
        </section>
      </div>
    </main>
  )
}
