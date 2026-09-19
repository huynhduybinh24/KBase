import { useState, type InputHTMLAttributes } from 'react'

export function FormField({ label, error, ...props }: InputHTMLAttributes<HTMLInputElement> & {
  label: string
  error?: string
}) {
  const [passwordVisible, setPasswordVisible] = useState(false)
  const errorId = error ? `${props.id}-error` : undefined
  const isPassword = props.type === 'password'
  return (
    <div>
      <label htmlFor={props.id} className="mb-2 block text-sm font-semibold text-[#344054]">{label}</label>
      <div className="relative">
        <input
          {...props}
          type={isPassword && passwordVisible ? 'text' : props.type}
          aria-invalid={Boolean(error)}
          aria-describedby={errorId}
          className={`h-12 w-full rounded-[13px] border border-white/80 bg-white/88 px-3.5 text-sm text-[#16181D] shadow-[0_2px_8px_rgba(16,24,40,.05)] outline-none transition duration-150 placeholder:text-[#98A2B3] hover:border-[#C7CCD8] focus:border-[#5B5BD6] focus:ring-4 focus:ring-[#5B5BD6]/12 aria-invalid:border-[#F04438] aria-invalid:ring-[#F04438]/10 disabled:bg-[#F5F6FA] disabled:text-[#98A2B3] ${isPassword ? 'pr-12' : ''}`}
        />
        {isPassword && <button type="button" aria-label={passwordVisible ? `Hide ${label.toLowerCase()}` : `Show ${label.toLowerCase()}`} onClick={() => setPasswordVisible((visible) => !visible)} className="absolute right-1.5 top-1/2 grid h-9 w-9 -translate-y-1/2 place-items-center rounded-lg text-[#667085] hover:bg-[#F2F4F7] focus-visible:outline-2 focus-visible:outline-[#5B5BD6]"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="h-4.5 w-4.5" aria-hidden="true">{passwordVisible ? <><path d="M3 3l18 18"/><path d="M10.6 10.6a2 2 0 0 0 2.8 2.8M9.9 4.3A10.8 10.8 0 0 1 12 4c5.5 0 9 8 9 8a17 17 0 0 1-2.1 3.3M6.2 6.2C4.2 7.6 3 10.2 3 12c0 0 3.5 8 9 8 1.5 0 2.8-.6 4-1.4"/></> : <><path d="M3 12s3.5-8 9-8 9 8 9 8-3.5 8-9 8-9-8-9-8z"/><circle cx="12" cy="12" r="3"/></>}</svg></button>}
      </div>
      {error && <p id={errorId} role="alert" className="mt-1.5 text-sm text-[#D92D20]">{error}</p>}
    </div>
  )
}
