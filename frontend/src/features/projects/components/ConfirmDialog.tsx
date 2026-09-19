import { useRef, useEffect } from 'react'

interface Props {
  isOpen: boolean
  title: string
  message: string
  confirmLabel?: string
  onConfirm: () => void
  onCancel: () => void
  isLoading?: boolean
  danger?: boolean
}

export function ConfirmDialog({
  isOpen,
  title,
  message,
  confirmLabel = 'Confirm',
  onConfirm,
  onCancel,
  isLoading = false,
  danger = true,
}: Props) {
  const cancelRef = useRef<HTMLButtonElement>(null)

  // Focus the cancel button when the dialog opens for keyboard safety
  useEffect(() => {
    if (!isOpen) return
    cancelRef.current?.focus()
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !isLoading) onCancel()
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [isOpen, isLoading, onCancel])

  if (!isOpen) return null

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-dialog-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-[#17172F]/35 p-4 backdrop-blur-sm"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0"
        onClick={onCancel}
        aria-hidden="true"
      />

      {/* Panel */}
      <div className="relative w-full max-w-md rounded-[24px] border border-white/70 bg-white p-6 shadow-[0_24px_60px_rgba(33,35,58,.22)] sm:p-7">
        <h2 id="confirm-dialog-title" className="text-lg font-bold text-[#16181D]">
          {title}
        </h2>
        <p className="mt-2 text-sm leading-6 text-[#667085]">{message}</p>

        <div className="mt-6 flex items-center justify-end gap-3">
          <button
            ref={cancelRef}
            type="button"
            onClick={onCancel}
            disabled={isLoading}
            className="h-11 rounded-xl border border-[#DDE1EA] bg-white px-4 text-sm font-semibold text-[#475467] transition hover:bg-[#F9FAFB] disabled:opacity-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#5B5BD6]"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isLoading}
            className={`h-11 rounded-xl px-4 text-sm font-semibold transition disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 ${
              danger
                ? 'bg-[#D92D20] text-white shadow-sm hover:bg-[#B42318] focus-visible:outline-[#F04438]'
                : 'bg-[#5B5BD6] text-white hover:bg-[#4F46C8] focus-visible:outline-[#5B5BD6]'
            }`}
          >
            {isLoading ? 'Working…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
