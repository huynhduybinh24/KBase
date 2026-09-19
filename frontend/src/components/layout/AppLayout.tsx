import { Outlet, useLocation } from 'react-router-dom'
import { Header } from './Header'
import { StaticBackground } from '../DynamicBackground'

export function AppLayout() {
  const { pathname } = useLocation()
  const isDashboard = pathname === '/dashboard'
  const isProjectDetail = /^\/projects\/[^/]+$/.test(pathname)
  return (
    <div className="relative min-h-screen overflow-hidden text-[#111827]">
      {!isDashboard && <StaticBackground src={isProjectDetail ? '/assets/kbase/background-03-book-clean.png' : '/assets/kbase/background-01-city-clean.png'} mobilePosition="58% center" theme={isProjectDetail ? 'dark' : 'light'} />}
      <Header />
      <div className="relative z-10"><Outlet /></div>
    </div>
  )
}
