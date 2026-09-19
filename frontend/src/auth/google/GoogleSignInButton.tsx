import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../../api/client'
import { useAuth } from '../useAuth'
import { useLanguage } from '../../i18n/LanguageProvider'

const GIS_URL = 'https://accounts.google.com/gsi/client'
let scriptPromise: Promise<void> | null = null

function loadGoogleScript() {
  if (window.google) return Promise.resolve()
  if (scriptPromise) return scriptPromise
  scriptPromise = new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(`script[src="${GIS_URL}"]`)
    const script = existing ?? Object.assign(document.createElement('script'), { src: GIS_URL, async: true, defer: true })
    script.addEventListener('load', () => resolve(), { once: true })
    script.addEventListener('error', () => reject(new Error('Google Identity Services could not be loaded')), { once: true })
    if (!existing) document.head.appendChild(script)
  })
  return scriptPromise
}

export function GoogleSignInButton({ clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID }: { clientId?: string }) {
  const { googleLogin } = useAuth()
  const { language, text } = useLanguage()
  const navigate = useNavigate()
  const buttonRef = useRef<HTMLDivElement>(null)
  const pendingRef = useRef(false)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!clientId || !buttonRef.current) return
    let active = true
    loadGoogleScript().then(() => {
      if (!active || !window.google || !buttonRef.current) return
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: async ({ credential }) => {
          if (!credential || pendingRef.current) return
          pendingRef.current = true
          setPending(true); setError('')
          try { await googleLogin(credential); navigate('/dashboard', { replace: true }) }
          catch (reason) { setError(getErrorMessage(reason) || 'Unable to sign in with Google. Please try again.') }
          finally { pendingRef.current = false; setPending(false) }
        },
      })
      buttonRef.current.replaceChildren()
      window.google.accounts.id.renderButton(buttonRef.current, {
        theme: 'outline', size: 'large', text: 'continue_with', shape: 'rectangular', width: 360, locale: language,
      })
    }).catch(() => active && setError(text('Unable to load Google sign-in. Please try again.', 'Không thể tải đăng nhập Google. Vui lòng thử lại.')))
    return () => { active = false }
  }, [clientId, googleLogin, language, navigate, text])

  if (!clientId) return <button type="button" disabled className="h-11 w-full rounded-xl border border-[#D0D5DD] bg-white text-sm font-medium text-[#667085] opacity-70">{text('Google sign-in is not configured.', 'Đăng nhập Google chưa được cấu hình.')}</button>
  return <div className="space-y-2">
    <div ref={buttonRef} aria-label={text('Continue with Google', 'Tiếp tục với Google')} className={pending ? 'pointer-events-none flex min-h-11 justify-center opacity-60' : 'flex min-h-11 justify-center'} />
    {pending && <p role="status" className="text-center text-xs text-[#667085]">Signing in with Google…</p>}
    {error && <p role="alert" className="text-sm text-[#B42318]">{error}</p>}
  </div>
}
