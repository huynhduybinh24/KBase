import { useState, type ReactNode } from 'react'
import { NavLink } from 'react-router-dom'
import { AccountMenu } from '../account/AccountMenu'
import { BrandMark } from '../BrandMark'
import { LanguageSwitcher } from '../LanguageSwitcher'
import { useLanguage } from '../../i18n/LanguageProvider'

function NavIcon({ children }: { children: ReactNode }) {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" className="h-4 w-4 shrink-0" aria-hidden="true">{children}</svg>
}

const activeModules = [
  { to: '/dashboard', label: 'Dashboard', icon: <NavIcon><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></NavIcon> },
  { to: '/projects', label: 'Projects', icon: <NavIcon><path d="M3 7.5h7l2 2h9v9.5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><path d="M3 7.5V5a2 2 0 0 1 2-2h5l2 2h7a2 2 0 0 1 2 2v2.5"/></NavIcon> },
]

const futureModules = [
  { label: 'Documents', icon: <NavIcon><path d="M6 2.5h8l4 4V21H6z"/><path d="M14 2.5V7h4M9 12h6M9 16h6"/></NavIcon> },
  { label: 'Search', icon: <NavIcon><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></NavIcon> },
  { label: 'AI Chat', icon: <NavIcon><path d="m12 3 1.2 3.8L17 8l-3.8 1.2L12 13l-1.2-3.8L7 8l3.8-1.2zM18.5 14l.7 2.3 2.3.7-2.3.7-.7 2.3-.7-2.3-2.3-.7 2.3-.7z"/></NavIcon> },
]

export function Header() {
  const { text } = useLanguage()
  const [menuOpen, setMenuOpen] = useState(false)

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-2 rounded-[13px] border px-3 py-2 text-[13px] font-semibold transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#5B5BD6] ${
      isActive
        ? 'border-white/50 bg-white/55 text-[#3730A3] shadow-[0_2px_10px_rgba(15,23,42,.08)]'
        : 'border-transparent text-[#3F4A68] hover:bg-white/25 hover:text-[#1F2937]'
    }`

  return (
    <header className="sticky top-3 z-40 mx-3 mt-3 rounded-[24px] border border-white/35 bg-[#F8FAFF]/58 shadow-[0_12px_40px_rgba(15,23,42,.14)] backdrop-blur-[20px] backdrop-saturate-125 sm:top-5 sm:mx-auto sm:mt-5 lg:w-[82%] lg:max-w-[1280px]">
      <div className="flex h-[68px] items-center justify-between gap-3 px-3 sm:px-4">
        <div className="flex min-w-0 items-center gap-2 xl:gap-4">
          <BrandMark responsive />
          <span className="hidden h-7 w-px bg-white/45 lg:block" aria-hidden="true" />
          <nav aria-label="Main navigation" className="flex items-center gap-1">
            {activeModules.map(({ to, label, icon }) => (
              <NavLink key={to} to={to} className={linkClass} onClick={() => setMenuOpen(false)}>
                {icon}<span className="hidden sm:inline">{label === 'Dashboard' ? text('Dashboard', 'Tổng quan') : text('Projects', 'Dự án')}</span>
              </NavLink>
            ))}
            <div className="ml-1 hidden items-center gap-1 lg:flex">
              {futureModules.map(({ label, icon }) => (
                <div key={label} className="group relative">
                  <button type="button" aria-disabled="true" aria-describedby={`tooltip-${label.replace(' ', '-').toLowerCase()}`} className="flex cursor-default items-center gap-2 rounded-[13px] border border-transparent px-3 py-2 text-[13px] font-semibold text-[#667085] opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#5B5BD6]">
                    {icon}<span>{label}</span><span className="h-1.5 w-1.5 rounded-full bg-[#7C3AED]" aria-hidden="true" />
                  </button>
                  <span id={`tooltip-${label.replace(' ', '-').toLowerCase()}`} role="tooltip" className="pointer-events-none absolute left-1/2 top-[calc(100%+7px)] -translate-x-1/2 whitespace-nowrap rounded-lg bg-[#111827] px-2.5 py-1.5 text-[11px] font-medium text-white opacity-0 shadow-lg transition group-hover:opacity-100 group-focus-within:opacity-100">{text('Coming soon', 'Sắp ra mắt')}</span>
                </div>
              ))}
            </div>
          </nav>
        </div>

        <div className="flex items-center gap-1 sm:gap-2">
          <LanguageSwitcher compact />
          <AccountMenu />
          <button type="button" aria-label="Toggle navigation menu" aria-expanded={menuOpen} onClick={() => setMenuOpen((open) => !open)} className="grid h-10 w-10 shrink-0 place-items-center rounded-xl border border-white/45 bg-white/25 text-[#344054] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#5B5BD6] lg:hidden">
            <NavIcon>{menuOpen ? <><path d="m6 6 12 12M18 6 6 18"/></> : <><path d="M4 7h16M4 12h16M4 17h16"/></>}</NavIcon>
          </button>
        </div>
      </div>

      {menuOpen && (
        <div className="mx-3 mb-3 rounded-2xl border border-white/45 bg-white/55 p-2 shadow-lg backdrop-blur-xl lg:hidden">
          <p className="px-3 pb-1 pt-2 text-[11px] font-bold uppercase tracking-[.14em] text-[#667085]">{text('More modules', 'Thêm chức năng')}</p>
          {futureModules.map(({ label, icon }) => (
            <button key={label} type="button" aria-disabled="true" title="Coming soon" className="flex w-full cursor-default items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-semibold text-[#667085] opacity-65">
              {icon}<span className="flex-1">{label}</span><span className="text-[11px] font-medium">{text('Coming soon', 'Sắp ra mắt')}</span>
            </button>
          ))}
        </div>
      )}
    </header>
  )
}
