import { useState, type FormEvent } from 'react'
import { FormField } from '../../../components/FormField'
import { getErrorMessage } from '../../../api/client'
import type { ProjectFormData } from '../../../types/project'

interface Props {
  defaultValues?: Partial<ProjectFormData>
  onSubmit: (data: ProjectFormData) => Promise<void>
  onCancel?: () => void
  submitLabel?: string
  isLoading?: boolean
}

export function ProjectForm({
  defaultValues,
  onSubmit,
  onCancel,
  submitLabel = 'Save',
  isLoading = false,
}: Props) {
  const [name, setName] = useState(defaultValues?.name ?? '')
  const [description, setDescription] = useState(defaultValues?.description ?? '')
  const [nameError, setNameError] = useState('')
  const [formError, setFormError] = useState('')

  function validate(): boolean {
    setNameError('')
    if (!name.trim()) {
      setNameError('Project name is required.')
      return false
    }
    if (name.trim().length > 200) {
      setNameError('Project name must be 200 characters or less.')
      return false
    }
    if (description.length > 5000) {
      setFormError('Description must be 5,000 characters or less.')
      return false
    }
    return true
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError('')
    if (!validate()) return
    try {
      await onSubmit({ name: name.trim(), description: description.trim() })
    } catch (err) {
      setFormError(getErrorMessage(err))
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-5">
      <FormField
        id="project-name"
        label="Name"
        type="text"
        value={name}
        onChange={(e) => { setName(e.target.value); setNameError('') }}
        error={nameError}
        autoFocus
        disabled={isLoading}
        placeholder="e.g. Q3 Product Research"
      />

      <div className="flex flex-col gap-1.5">
        <label htmlFor="project-description" className="text-sm font-semibold text-[#344054]">
          Description <span className="font-normal text-[#98A2B3]">(optional)</span>
        </label>
        <textarea
          id="project-description"
          value={description}
          onChange={(e) => { setDescription(e.target.value); setFormError('') }}
          disabled={isLoading}
          placeholder="What is this project about?"
          rows={4}
          className="w-full resize-none rounded-xl border border-[#DDE1EA] bg-white px-3.5 py-3 text-sm text-[#16181D] shadow-sm outline-none transition placeholder:text-[#98A2B3] focus:border-[#5B5BD6] focus:ring-4 focus:ring-[#5B5BD6]/10 disabled:bg-[#F5F6FA] disabled:opacity-60"
          aria-describedby={formError ? 'project-form-error' : undefined}
        />
        <p className="text-right text-xs text-[#98A2B3]">{description.length}/5000</p>
      </div>

      {formError && (
        <p id="project-form-error" role="alert" className="rounded-xl border border-[#FECDCA] bg-[#FEF3F2] px-4 py-3 text-sm text-[#B42318]">
          {formError}
        </p>
      )}

      <div className="flex items-center justify-end gap-3 pt-1">
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            disabled={isLoading}
            className="h-11 rounded-xl border border-[#DDE1EA] bg-white px-4 text-sm font-semibold text-[#475467] transition hover:bg-[#F9FAFB] disabled:opacity-50"
          >
            Cancel
          </button>
        )}
        <button
          type="submit"
          disabled={isLoading}
          className="h-11 rounded-xl bg-[#5B5BD6] px-5 text-sm font-semibold text-white shadow-md transition hover:bg-[#4F46C8] disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#5B5BD6]"
        >
          {isLoading ? 'Saving…' : submitLabel}
        </button>
      </div>
    </form>
  )
}
