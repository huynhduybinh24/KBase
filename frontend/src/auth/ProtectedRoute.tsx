import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './useAuth'

export function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth()
  const location = useLocation()
  if (isLoading) return <RouteLoading />
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}

export function PublicOnlyRoute() {
  const { isAuthenticated, isLoading } = useAuth()
  if (isLoading) return <RouteLoading />
  return isAuthenticated ? <Navigate to="/dashboard" replace /> : <Outlet />
}

export function RootRedirect() {
  const { isAuthenticated, isLoading } = useAuth()
  if (isLoading) return <RouteLoading />
  return <Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />
}

function RouteLoading() {
  return (
    <main className="grid min-h-screen place-items-center bg-slate-950 text-slate-200" aria-live="polite">
      <div className="flex items-center gap-3">
        <span className="h-5 w-5 animate-spin rounded-full border-2 border-cyan-400 border-t-transparent" />
        <span>Loading your workspace…</span>
      </div>
    </main>
  )
}
