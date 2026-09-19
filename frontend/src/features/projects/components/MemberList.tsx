import { useMembers } from '../hooks/useMembers'
import { AddMemberForm } from './AddMemberForm'
import { RemoveMemberButton } from './RemoveMemberButton'
import type { ProjectMember } from '../../../types/project'
import { Avatar } from '../../../components/Avatar'

interface Props {
  projectId: string
  isOwner: boolean
  currentUserId: string
}

function roleBadge(role: ProjectMember['role']) {
  return role === 'OWNER'
    ? 'bg-[#F0EDFF] text-[#6941C6] ring-[#DDD6FE]'
    : 'bg-[#EFF8FF] text-[#175CD3] ring-[#B2DDFF]'
}

export function MemberList({ projectId, isOwner, currentUserId }: Props) {
  const { data: members, isLoading, isError, error } = useMembers(projectId)

  return (
    <section aria-labelledby="members-heading" className="glass-panel rounded-[24px] p-5 sm:p-7">
      <div className="mb-5 flex items-center justify-between gap-4">
        <div><h2 id="members-heading" className="text-xl font-bold tracking-[-0.02em] text-[#16181D]">Members</h2><p className="mt-1 text-sm text-[#667085]">People with access to this workspace.</p></div>
      </div>

      {isOwner && (
        <div className="mb-6 rounded-2xl border border-[#E2E4F3] bg-[#FAFBFF] p-4 sm:p-5">
          <AddMemberForm projectId={projectId} />
        </div>
      )}

      {isLoading && (
        <div className="space-y-2" aria-label="Loading members">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="h-16 animate-pulse rounded-xl bg-[#F2F4F7]" />
          ))}
        </div>
      )}

      {isError && (
        <p role="alert" className="rounded-xl border border-[#FECDCA] bg-[#FEF3F2] px-4 py-3 text-sm text-[#B42318]">
          {error instanceof Error ? error.message : 'Failed to load members.'}
        </p>
      )}

      {members && members.length === 0 && (
        <p className="text-sm text-[#98A2B3]">No members yet.</p>
      )}

      {members && members.length > 0 && (
        <ul className="overflow-hidden rounded-2xl border border-white/65 bg-white/45 divide-y divide-white/60" aria-label="Project members">
          {members.map((member) => {
            const isCurrentUser = member.userId === currentUserId
            const canRemove = isOwner && member.role !== 'OWNER' && !isCurrentUser
            return (
              <li key={member.id} className="flex items-center justify-between gap-3 px-4 py-3.5 transition hover:bg-[#FAFBFF]">
                <div className="min-w-0 flex items-center gap-3">
                  <Avatar email={member.email} tone={member.role === 'OWNER' ? 'violet' : 'blue'} />
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-[#344054]">
                      {member.email}
                      {isCurrentUser && <span className="ml-1.5 text-xs font-normal text-[#98A2B3]">(you)</span>}
                    </p>
                    <p className="mt-0.5 text-xs text-[#98A2B3]">Joined {new Date(member.joinedAt).toLocaleDateString(undefined, { month: 'short', year: 'numeric' })}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-bold ring-1 ring-inset ${roleBadge(member.role)}`}>
                    {member.role}
                  </span>
                  {canRemove && (
                    <RemoveMemberButton projectId={projectId} member={member} />
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
