import { useState } from 'react'
import { getErrorMessage } from '../../../api/client'
import { useRemoveMember } from '../hooks/useMembers'
import { ConfirmDialog } from './ConfirmDialog'
import type { ProjectMember } from '../../../types/project'

interface Props {
  projectId: string
  member: ProjectMember
}

export function RemoveMemberButton({ projectId, member }: Props) {
  const [open, setOpen] = useState(false)
  const [error, setError] = useState('')
  const removeMember = useRemoveMember(projectId)

  async function handleConfirm() {
    setError('')
    try {
      await removeMember.mutateAsync(member.userId)
      setOpen(false)
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={() => { setOpen(true); setError('') }}
        className="rounded-lg px-2.5 py-1.5 text-xs font-semibold text-[#B42318] transition hover:bg-[#FEF3F2] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#F04438]"
        aria-label={`Remove ${member.email}`}
      >
        Remove
      </button>

      <ConfirmDialog
        isOpen={open}
        title="Remove member"
        message={`Remove ${member.email} from this project? They will lose access immediately.`}
        confirmLabel="Remove"
        onConfirm={handleConfirm}
        onCancel={() => setOpen(false)}
        isLoading={removeMember.isPending}
      />

      {error && (
        <p role="alert" className="mt-1 text-xs text-[#B42318]">{error}</p>
      )}
    </>
  )
}
