import { useState } from 'react'
import { Link } from 'react-router-dom'
import { DynamicBackground, type BackgroundTheme } from '../components/DynamicBackground'
import { useLanguage } from '../i18n/LanguageProvider'

export function DashboardPage() {
  const [theme, setTheme] = useState<BackgroundTheme>('light')
  const { text } = useLanguage()
  const dark = theme === 'dark'

  return (
    <main className="mx-auto flex min-h-[calc(100vh-88px)] max-w-7xl flex-col px-4 pb-20 pt-7 sm:px-6 sm:pt-10 lg:px-8">
      <DynamicBackground onThemeChange={setTheme} />
      <section className="relative flex flex-1 flex-col items-center justify-center pb-5 text-center">
        <h1 className={`max-w-3xl text-[38px] font-extrabold leading-[1.02] tracking-[-.05em] transition-colors sm:text-[52px] ${dark ? 'text-white drop-shadow-[0_3px_18px_rgba(0,0,0,.35)]' : 'text-[#111B4D] drop-shadow-[0_2px_18px_rgba(255,255,255,.8)]'}`}>
          {text('Your knowledge.', 'Tri thức của bạn.')}<br /><span className="bg-gradient-to-r from-[#4338CA] via-[#7C3AED] to-[#2563EB] bg-clip-text text-transparent">{text('One place.', 'Một nơi duy nhất.')}</span>
        </h1>
        <p className={`mt-5 max-w-2xl text-sm font-semibold leading-6 transition-colors sm:text-base ${dark ? 'text-white/90' : 'text-[#33446F]'}`}>
          {text("Organize project documents, discover information instantly, and get answers grounded in your team's knowledge.", 'Sắp xếp tài liệu dự án, tìm thông tin tức thì và nhận câu trả lời dựa trên tri thức của đội nhóm.')}
        </p>
        <div className="mt-6 flex flex-col gap-3 sm:flex-row">
          <Link to="/projects" className="inline-flex h-12 items-center justify-center rounded-[14px] bg-gradient-to-r from-[#4F46E5] to-[#6D5CE7] px-6 text-sm font-bold text-white shadow-[0_12px_28px_rgba(79,70,229,.3)] transition hover:-translate-y-0.5 hover:shadow-[0_16px_34px_rgba(79,70,229,.36)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white">{text('Explore projects', 'Khám phá dự án')} <span className="ml-2" aria-hidden="true">→</span></Link>
          <button type="button" disabled title={text('AI Assistant is coming soon', 'Trợ lý AI sắp ra mắt')} className="glass-panel h-12 cursor-not-allowed rounded-[14px] px-6 text-sm font-bold text-[#34406B] opacity-80">{text('Ask KBase AI · Coming soon', 'Hỏi KBase AI · Sắp ra mắt')}</button>
        </div>

        <Link to="/projects" aria-label="Search projects and knowledge" className="glass-panel group mt-7 flex h-15 w-full max-w-2xl items-center rounded-[20px] px-5 text-left transition hover:-translate-y-0.5 hover:bg-white/75 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-5 w-5 shrink-0 text-[#4F46E5]" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>
          <span className="ml-3 flex-1 text-sm font-medium text-[#65709A]">{text('Search projects and knowledge...', 'Tìm kiếm dự án và tri thức...')}</span>
          <span className="hidden rounded-lg border border-white/80 bg-white/50 px-2 py-1 text-[11px] font-bold text-[#7B849F] sm:block">{text('Browse', 'Duyệt')}</span>
        </Link>

        <aside className="mt-5 max-w-xl rounded-[22px] border border-white/20 bg-[#111827]/52 px-6 py-3.5 text-sm font-medium text-white shadow-[0_16px_45px_rgba(15,23,42,.2)] backdrop-blur-xl">
          {text('Turn scattered project information into shared knowledge.', 'Biến thông tin dự án rời rạc thành tri thức chung.')}
        </aside>
      </section>
    </main>
  )
}
