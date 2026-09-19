import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { AuthPanel } from '../components/AuthPanel'
import { FormField } from '../components/FormField'
import { validateEmail } from '../lib/validation'
import { GoogleSignInButton } from '../auth/google/GoogleSignInButton'
import { useLanguage } from '../i18n/LanguageProvider'

export function LoginPage() {
  const { login } = useAuth()
  const { text } = useLanguage()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [apiError, setApiError] = useState('')
  const [pending, setPending] = useState(false)
  const registered = Boolean((location.state as { registered?: boolean } | null)?.registered)

  async function submit(event: FormEvent) {
    event.preventDefault()
    const nextErrors: Record<string, string> = {}
    const emailError = validateEmail(email)
    if (emailError) nextErrors.email = emailError
    if (!password) nextErrors.password = text('Password is required.', 'Vui lòng nhập mật khẩu.')
    setErrors(nextErrors)
    setApiError('')
    if (Object.keys(nextErrors).length) return
    setPending(true)
    try {
      await login({ email: email.trim(), password })
      navigate('/dashboard', { replace: true })
    } catch (error) {
      setApiError(getErrorMessage(error))
    } finally {
      setPending(false)
    }
  }

  return (
    <AuthPanel title={text('Welcome back', 'Chào mừng trở lại')} subtitle={text('Sign in to continue to your knowledge workspace.', 'Đăng nhập để tiếp tục vào không gian tri thức của bạn.')} footer={<>{text('New to KBase?', 'Chưa có tài khoản KBase?')} <Link className="font-semibold text-[#5B5BD6] hover:text-[#4F46C8]" to="/register">{text('Create an account', 'Tạo tài khoản')}</Link></>}>
      {registered && <p role="status" className="mb-5 rounded-xl border border-[#ABEFC6] bg-[#ECFDF3] px-4 py-3 text-sm text-[#067647]">{text('Account created. You can sign in now.', 'Đã tạo tài khoản. Bạn có thể đăng nhập ngay.')}</p>}
      {apiError && <p role="alert" className="mb-5 rounded-xl border border-[#FECDCA] bg-[#FEF3F2] px-4 py-3 text-sm text-[#B42318]">{apiError}</p>}
      <form className="space-y-5" onSubmit={submit} noValidate>
        <FormField id="login-email" label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} error={errors.email} />
        <FormField id="login-password" label={text('Password', 'Mật khẩu')} type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} error={errors.password} />
        <button disabled={pending} className="h-12 w-full rounded-xl bg-[#5B5BD6] px-4 text-sm font-semibold text-white shadow-[0_6px_14px_rgba(91,91,214,.22)] transition hover:-translate-y-px hover:bg-[#4F46C8] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#5B5BD6] disabled:cursor-not-allowed disabled:opacity-60">
          {pending ? text('Signing in…', 'Đang đăng nhập…') : text('Sign in', 'Đăng nhập')}
        </button>
      </form>
      <div className="my-5 flex items-center gap-3 text-xs text-[#98A2B3]"><span className="h-px flex-1 bg-[#E4E7EC]" /><span>{text('or continue with', 'hoặc tiếp tục với')}</span><span className="h-px flex-1 bg-[#E4E7EC]" /></div>
      <GoogleSignInButton />
    </AuthPanel>
  )
}
