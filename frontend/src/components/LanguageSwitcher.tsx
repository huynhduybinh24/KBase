import { useLanguage } from '../i18n/LanguageProvider'

export function LanguageSwitcher({ compact = false }: { compact?: boolean }) {
  const { language, setLanguage, text } = useLanguage()
  const next = language === 'en' ? 'vi' : 'en'
  return <button type="button" onClick={() => setLanguage(next)} aria-label={text('Switch to Vietnamese', 'Chuyển sang tiếng Anh')} title={text('Switch to Vietnamese', 'Chuyển sang tiếng Anh')} className={`flex h-10 shrink-0 items-center justify-center gap-1.5 rounded-xl border border-white/55 bg-white/35 px-2.5 text-xs font-bold text-[#344054] shadow-sm backdrop-blur-md transition hover:bg-white/60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#5B5BD6] ${compact ? '' : 'min-w-[66px]'}`}>
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="h-4.5 w-4.5" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"/></svg>
    {!compact && <span>{language.toUpperCase()}</span>}
  </button>
}
