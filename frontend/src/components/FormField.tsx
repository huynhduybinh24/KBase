import type { InputHTMLAttributes } from 'react'

export function FormField({ label, error, ...props }: InputHTMLAttributes<HTMLInputElement> & {
  label: string
  error?: string
}) {
  const errorId = error ? `${props.id}-error` : undefined
  return (
    <div>
      <label htmlFor={props.id} className="mb-2 block text-sm font-medium text-slate-200">{label}</label>
      <input
        {...props}
        aria-invalid={Boolean(error)}
        aria-describedby={errorId}
        className="w-full rounded-xl border border-white/10 bg-slate-950/70 px-3.5 py-3 text-sm text-white outline-none transition placeholder:text-slate-600 hover:border-white/20 focus:border-cyan-400 focus:ring-2 focus:ring-cyan-400/20 aria-invalid:border-rose-400"
      />
      {error && <p id={errorId} role="alert" className="mt-1.5 text-sm text-rose-300">{error}</p>}
    </div>
  )
}
