import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { AuthPanel } from '../components/AuthPanel'
import { FormField } from '../components/FormField'
import { validateEmail } from '../lib/validation'

export function LoginPage() {
  const { login } = useAuth()
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
    if (!password) nextErrors.password = 'Password is required.'
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
    <AuthPanel title="Welcome back" subtitle="Sign in to continue to your knowledge workspace." footer={<>New to KBase? <Link className="font-medium text-cyan-300 hover:text-cyan-200" to="/register">Create an account</Link></>}>
      {registered && <p role="status" className="mb-5 rounded-xl border border-emerald-400/25 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-200">Account created. You can sign in now.</p>}
      {apiError && <p role="alert" className="mb-5 rounded-xl border border-rose-400/25 bg-rose-400/10 px-4 py-3 text-sm text-rose-200">{apiError}</p>}
      <form className="space-y-5" onSubmit={submit} noValidate>
        <FormField id="login-email" label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} error={errors.email} />
        <FormField id="login-password" label="Password" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} error={errors.password} />
        <button disabled={pending} className="w-full rounded-xl bg-cyan-400 px-4 py-3 text-sm font-semibold text-slate-950 transition hover:bg-cyan-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-cyan-300 disabled:cursor-not-allowed disabled:opacity-60">
          {pending ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </AuthPanel>
  )
}
