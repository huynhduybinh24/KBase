import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

export type Language = 'en' | 'vi'
type LanguageContextValue = { language: Language; setLanguage: (language: Language) => void; text: (english: string, vietnamese: string) => string }
const STORAGE_KEY = 'kbase.language'
const fallback: LanguageContextValue = { language: 'en', setLanguage: () => undefined, text: (english) => english }
const LanguageContext = createContext<LanguageContextValue>(fallback)

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<Language>(() => localStorage.getItem(STORAGE_KEY) === 'vi' ? 'vi' : 'en')
  const setLanguage = (next: Language) => { localStorage.setItem(STORAGE_KEY, next); setLanguageState(next) }
  useEffect(() => { document.documentElement.lang = language }, [language])
  const value = useMemo(() => ({ language, setLanguage, text: (en: string, vi: string) => language === 'vi' ? vi : en }), [language])
  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>
}

export function useLanguage() { return useContext(LanguageContext) }
