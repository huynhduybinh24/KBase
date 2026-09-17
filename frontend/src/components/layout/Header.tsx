import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'

export function Header() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="border-b border-white/10 bg-slate-950/85 backdrop-blur-xl">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-4 sm:px-6 lg:px-8">
        <div className="flex min-w-0 items-center gap-3">
          <div className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-cyan-400 font-black text-slate-950 shadow-lg shadow-cyan-400/20" aria-hidden="true">K</div>
          <div>
            <p className="font-semibold tracking-tight text-white">KBase</p>
            <p className="hidden text-xs text-slate-400 sm:block">Your team knowledge workspace</p>
          </div>
        </div>
        <nav className="flex min-w-0 items-center gap-3" aria-label="Account navigation">
          <span className="hidden max-w-64 truncate text-sm text-slate-300 md:block">{user?.email}</span>
          <button type="button" onClick={handleLogout} className="rounded-lg border border-white/15 px-3 py-2 text-sm font-medium text-slate-100 transition hover:border-cyan-300/60 hover:bg-white/5 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-cyan-300">
            Log out
          </button>
        </nav>
      </div>
    </header>
  )
}
