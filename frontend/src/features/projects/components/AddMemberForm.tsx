import { useState, type FormEvent } from 'react'
import { FormField } from '../../../components/FormField'
import { getErrorMessage } from '../../../api/client'
import { useAddMember } from '../hooks/useMembers'

interface Props {
  projectId: string
}

export function AddMemberForm({ projectId }: Props) {
  const [email, setEmail] = useState('')
  const [emailError, setEmailError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [formError, setFormError] = useState('')

  const addMember = useAddMember(projectId)

  function validateEmail(value: string): boolean {
    setEmailError('')
    if (!value.trim()) {
      setEmailError('Email is required.')
      return false
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim())) {
      setEmailError('Please enter a valid email address.')
      return false
    }
    if (value.trim().length > 320) {
      setEmailError('Email must be 320 characters or less.')
      return false
    }
    return true
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError('')
    setSuccessMessage('')
    if (!validateEmail(email)) return

    try {
      const member = await addMember.mutateAsync({ email: email.trim() })
      setEmail('')
      setSuccessMessage(`${member.email} added as a member.`)
    } catch (err) {
      setFormError(getErrorMessage(err))
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-3">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div className="flex-1">
          <FormField
            id="add-member-email"
            label="Add member by email"
            type="email"
            value={email}
            onChange={(e) => { setEmail(e.target.value); setEmailError(''); setFormError(''); setSuccessMessage('') }}
            error={emailError}
            placeholder="colleague@example.com"
            disabled={addMember.isPending}
          />
        </div>
        <div>
          <button
            type="submit"
            disabled={addMember.isPending}
            className="h-12 w-full rounded-xl bg-[#5B5BD6] px-5 text-sm font-semibold text-white shadow-sm transition hover:bg-[#4F46C8] disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#5B5BD6] sm:w-auto"
          >
            {addMember.isPending ? 'Adding…' : 'Add'}
          </button>
        </div>
      </div>
      {formError && (
        <p role="alert" className="rounded-xl border border-[#FECDCA] bg-[#FEF3F2] px-3 py-2.5 text-sm text-[#B42318]">
          {formError}
        </p>
      )}
      {successMessage && (
        <p role="status" className="rounded-xl border border-[#ABEFC6] bg-[#ECFDF3] px-3 py-2.5 text-sm text-[#067647]">
          {successMessage}
        </p>
      )}
    </form>
  )
}
