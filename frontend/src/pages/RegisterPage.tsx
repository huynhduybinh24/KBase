import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { AuthPanel } from '../components/AuthPanel'
import { FormField } from '../components/FormField'
import { validateEmail, validateRegistrationPassword } from '../lib/validation'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [apiError, setApiError] = useState('')
  const [pending, setPending] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    const nextErrors: Record<string, string> = {}
    const emailError = validateEmail(email)
    const passwordError = validateRegistrationPassword(password)
    if (emailError) nextErrors.email = emailError
    if (passwordError) nextErrors.password = passwordError
    if (password !== confirmPassword) nextErrors.confirmPassword = 'Passwords do not match.'
    setErrors(nextErrors)
    setApiError('')
    if (Object.keys(nextErrors).length) return
    setPending(true)
    try {
      await register({ email: email.trim(), password })
      navigate('/login', { replace: true, state: { registered: true } })
    } catch (error) {
      setApiError(getErrorMessage(error))
    } finally {
      setPending(false)
    }
  }

  return (
    <AuthPanel title="Create your account" subtitle="Start building a secure, searchable home for team knowledge." footer={<>Already have an account? <Link className="font-medium text-cyan-300 hover:text-cyan-200" to="/login">Sign in</Link></>}>
      {apiError && <p role="alert" className="mb-5 rounded-xl border border-rose-400/25 bg-rose-400/10 px-4 py-3 text-sm text-rose-200">{apiError}</p>}
      <form className="space-y-5" onSubmit={submit} noValidate>
        <FormField id="register-email" label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} error={errors.email} />
        <FormField id="register-password" label="Password" type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} error={errors.password} />
        <p className="-mt-3 text-xs leading-5 text-slate-500">Use at least 8 characters, up to 72 UTF-8 bytes.</p>
        <FormField id="confirm-password" label="Confirm password" type="password" autoComplete="new-password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} error={errors.confirmPassword} />
        <button disabled={pending} className="w-full rounded-xl bg-cyan-400 px-4 py-3 text-sm font-semibold text-slate-950 transition hover:bg-cyan-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-cyan-300 disabled:cursor-not-allowed disabled:opacity-60">
          {pending ? 'Creating account…' : 'Create account'}
        </button>
      </form>
    </AuthPanel>
  )
}
