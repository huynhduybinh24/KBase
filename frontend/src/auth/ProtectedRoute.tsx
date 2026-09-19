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
    <main className="grid min-h-screen place-items-center text-[#667085]" aria-live="polite">
      <div className="flex items-center gap-3 rounded-2xl border border-[#E7E9F0] bg-white px-5 py-4 shadow-sm">
        <span className="h-4 w-4 animate-pulse rounded-full bg-[#5B5BD6]" aria-hidden="true" />
        <span className="text-sm font-medium">Loading your workspace…</span>
      </div>
    </main>
  )
}
