import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { AuthPanel } from '../components/AuthPanel'
import { FormField } from '../components/FormField'
import { validateEmail, validateRegistrationPassword } from '../lib/validation'
import { GoogleSignInButton } from '../auth/google/GoogleSignInButton'
import { useLanguage } from '../i18n/LanguageProvider'

export function RegisterPage() {
  const { register } = useAuth()
  const { text } = useLanguage()
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [apiError, setApiError] = useState('')
  const [pending, setPending] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    const nextErrors: Record<string, string> = {}
    const normalizedName = fullName.trim().replace(/\s+/g, ' ')
    if (normalizedName.length < 2) nextErrors.fullName = text('Enter your full name.', 'Vui lòng nhập họ và tên.')
    else if (normalizedName.length > 80) nextErrors.fullName = text('Full name must be 80 characters or fewer.', 'Họ và tên không được vượt quá 80 ký tự.')
    const emailError = validateEmail(email)
    const passwordError = validateRegistrationPassword(password)
    if (emailError) nextErrors.email = emailError
    if (passwordError) nextErrors.password = passwordError
    if (password !== confirmPassword) nextErrors.confirmPassword = text('Passwords do not match.', 'Mật khẩu xác nhận không khớp.')
    setErrors(nextErrors)
    setApiError('')
    if (Object.keys(nextErrors).length) return
    setPending(true)
    try {
      await register({ fullName: normalizedName, email: email.trim(), password })
      navigate('/login', { replace: true, state: { registered: true } })
    } catch (error) {
      setApiError(getErrorMessage(error))
    } finally {
      setPending(false)
    }
  }

  return (
    <AuthPanel title={text('Create your account', 'Tạo tài khoản')} subtitle={text('Start building a secure, searchable home for team knowledge.', 'Xây dựng không gian tri thức an toàn và dễ tìm kiếm cho đội nhóm.')} footer={<>{text('Already have an account?', 'Đã có tài khoản?')} <Link className="font-semibold text-[#5B5BD6] hover:text-[#4F46C8]" to="/login">{text('Sign in', 'Đăng nhập')}</Link></>}>
      {apiError && <p role="alert" className="mb-5 rounded-xl border border-[#FECDCA] bg-[#FEF3F2] px-4 py-3 text-sm text-[#B42318]">{apiError}</p>}
      <form className="space-y-5" onSubmit={submit} noValidate>
        <FormField id="register-full-name" label={text('Full Name', 'Họ và tên')} type="text" autoComplete="name" value={fullName} onChange={(e) => setFullName(e.target.value)} error={errors.fullName} />
        <FormField id="register-email" label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} error={errors.email} />
        <FormField id="register-password" label={text('Password', 'Mật khẩu')} type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} error={errors.password} />
        <p className="-mt-3 text-xs leading-5 text-[#98A2B3]">{text('Use at least 8 characters, up to 72 UTF-8 bytes.', 'Dùng ít nhất 8 ký tự, tối đa 72 byte UTF-8.')}</p>
        <FormField id="confirm-password" label={text('Confirm password', 'Xác nhận mật khẩu')} type="password" autoComplete="new-password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} error={errors.confirmPassword} />
        <button disabled={pending} className="h-12 w-full rounded-xl bg-[#5B5BD6] px-4 text-sm font-semibold text-white shadow-[0_6px_14px_rgba(91,91,214,.22)] transition hover:-translate-y-px hover:bg-[#4F46C8] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#5B5BD6] disabled:cursor-not-allowed disabled:opacity-60">
          {pending ? text('Creating account…', 'Đang tạo tài khoản…') : text('Create account', 'Tạo tài khoản')}
        </button>
      </form>
      <div className="my-5 flex items-center gap-3 text-xs text-[#98A2B3]"><span className="h-px flex-1 bg-[#E4E7EC]" /><span>{text('or continue with', 'hoặc tiếp tục với')}</span><span className="h-px flex-1 bg-[#E4E7EC]" /></div>
      <GoogleSignInButton />
    </AuthPanel>
  )
}
