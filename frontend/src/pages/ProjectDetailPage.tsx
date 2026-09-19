import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { useProject, useUpdateProject, useDeleteProject } from '../features/projects/hooks/useProjects'
import { useMembers } from '../features/projects/hooks/useMembers'
import { MemberList } from '../features/projects/components/MemberList'
import { ProjectForm } from '../features/projects/components/ProjectForm'
import { ConfirmDialog } from '../features/projects/components/ConfirmDialog'
import { getErrorMessage } from '../api/client'
import type { ProjectFormData } from '../types/project'

export function ProjectDetailPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()

  const [isEditing, setIsEditing] = useState(false)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [editError, setEditError] = useState('')
  const [deleteError, setDeleteError] = useState('')

  const { data: project, isLoading, isError, error } = useProject(projectId ?? '')
  const { data: members } = useMembers(projectId ?? '')
  const updateProject = useUpdateProject(projectId ?? '')
  const deleteProject = useDeleteProject()

  // ProjectMemberResponse is the backend's authoritative source for project roles.
  const currentRole = members?.find((member) => member.userId === user?.id)?.role
  const isOwner = currentRole === 'OWNER'

  async function handleUpdate(data: ProjectFormData) {
    setEditError('')
    try {
      await updateProject.mutateAsync(data)
      setIsEditing(false)
    } catch (err) {
      setEditError(getErrorMessage(err))
    }
  }

  async function handleDelete() {
    setDeleteError('')
    try {
      await deleteProject.mutateAsync(projectId ?? '')
      navigate('/projects', { replace: true })
    } catch (err) {
      setDeleteError(getErrorMessage(err))
    }
  }

  // ──── Loading ────
  if (isLoading) {
    return (
      <main className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
        <div className="mb-8 h-8 w-48 animate-pulse rounded-lg bg-[#E7E9F0]" />
        <div className="h-52 animate-pulse rounded-[24px] border border-[#E7E9F0] bg-white/70" />
      </main>
    )
  }

  // ──── Error / not found ────
  if (isError || !project) {
    const message = error instanceof Error ? error.message : 'Project not found or you do not have access.'
    return (
      <main className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
        <Link to="/projects" className="mb-6 inline-flex items-center gap-1 text-sm font-semibold text-[#667085] transition hover:text-[#5B5BD6]">
          ← Back to projects
        </Link>
        <div className="mt-4 rounded-xl border border-[#FECDCA] bg-[#FEF3F2] px-5 py-4">
          <p role="alert" className="text-sm text-[#B42318]">{message}</p>
        </div>
      </main>
    )
  }

  const updatedDate = new Date(project.updatedAt).toLocaleDateString(undefined, {
    year: 'numeric', month: 'long', day: 'numeric',
  })

  return (
    <main className="mx-auto max-w-5xl px-4 py-8 sm:px-6 sm:py-12 lg:px-8">
      {/* Breadcrumb */}
      <nav aria-label="Breadcrumb" className="mb-6">
        <Link to="/projects" className="inline-flex items-center gap-1 text-sm font-semibold text-[#667085] transition hover:text-[#5B5BD6]">
          ← Projects
        </Link>
      </nav>

      {/* Project header */}
      <div className="glass-panel relative overflow-hidden rounded-[24px] p-6 before:absolute before:inset-x-0 before:top-0 before:h-1 before:bg-gradient-to-r before:from-[#5B5BD6] before:via-[#8B5CF6] before:to-[#3B82F6] sm:p-8">
        {isEditing ? (
          <div className="fixed inset-0 z-50 grid place-items-center bg-[#17172F]/35 p-4 backdrop-blur-sm">
          <section role="dialog" aria-modal="true" aria-labelledby="edit-project-heading" className="glass-panel w-full max-w-[520px] rounded-[24px] bg-white/80 p-6 sm:p-8">
            <h1 id="edit-project-heading" className="text-xl font-bold text-[#16181D]">Edit project</h1>
            <p className="mb-6 mt-2 text-sm leading-6 text-[#667085]">Update this workspace's name and description.</p>
            {editError && (
              <p role="alert" className="mb-4 rounded-xl border border-[#FECDCA] bg-[#FEF3F2] px-4 py-3 text-sm text-[#B42318]">
                {editError}
              </p>
            )}
            <ProjectForm
              defaultValues={{ name: project.name, description: project.description ?? '' }}
              onSubmit={handleUpdate}
              onCancel={() => { setIsEditing(false); setEditError('') }}
              submitLabel="Save changes"
              isLoading={updateProject.isPending}
            />
          </section>
          </div>
        ) : (
          <>
            <div className="flex items-start justify-between gap-4 flex-wrap">
              <div className="min-w-0">
                <div className="flex items-center gap-2 flex-wrap mb-2">
                  <span
                    className={`inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-bold ${
                      isOwner
                        ? 'bg-[#F0EDFF] text-[#6941C6] ring-1 ring-inset ring-[#DDD6FE]'
                        : 'bg-[#EFF8FF] text-[#175CD3] ring-1 ring-inset ring-[#B2DDFF]'
                    }`}
                  >
                    {currentRole === 'OWNER' ? 'Owner' : 'Member'}
                  </span>
                </div>
                <div className="mt-3 flex items-center gap-4"><span className="grid h-14 w-14 shrink-0 place-items-center rounded-[18px] bg-gradient-to-br from-[#5B5BD6] to-[#3B82F6] text-lg font-black text-white shadow-lg shadow-indigo-500/15">{project.name.charAt(0).toUpperCase()}</span><h1 className="text-2xl font-bold tracking-[-0.035em] text-[#16181D] sm:text-3xl">{project.name}</h1></div>
                {project.description && (
                  <p className="mt-4 max-w-2xl text-sm leading-6 text-[#667085]">{project.description}</p>
                )}
                <p className="mt-4 text-xs font-medium text-[#98A2B3]">
                  Last updated {updatedDate} · Owner: {project.owner.email}
                </p>
              </div>

              {/* Owner-only controls */}
              {isOwner && (
                <div className="flex shrink-0 items-center gap-2">
                  <button
                    type="button"
                    id="edit-project-btn"
                    onClick={() => setIsEditing(true)}
                    className="h-10 rounded-xl border border-[#DDE1EA] bg-white px-4 text-sm font-semibold text-[#475467] shadow-sm transition hover:bg-[#F9FAFB] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#5B5BD6]"
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    id="delete-project-btn"
                    onClick={() => setShowDeleteDialog(true)}
                    className="h-10 rounded-xl border border-[#FECDCA] bg-[#FEF3F2] px-4 text-sm font-semibold text-[#B42318] transition hover:bg-[#FEE4E2] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#F04438]"
                  >
                    Delete
                  </button>
                </div>
              )}
            </div>
          </>
        )}
      </div>

      {/* Members section */}
      <div className="mt-8">
        <MemberList
          projectId={project.id}
          isOwner={isOwner}
          currentUserId={user?.id ?? ''}
        />
      </div>

      {/* Coming soon placeholders */}
      <div className="mt-8 grid gap-4 sm:grid-cols-2">
        {[
          { mark: 'D', title: 'Documents', description: 'Upload, search, and manage source material.' },
          { mark: 'AI', title: 'AI Chat', description: 'Ask grounded questions with clear citations.' },
        ].map((item) => (
          <article key={item.title} className="glass-panel rounded-[22px] p-6">
            <div className="grid h-11 w-11 place-items-center rounded-[14px] bg-gradient-to-br from-[#EEF0FF] to-[#E9F3FF] text-sm font-black text-[#5B5BD6] ring-1 ring-[#DDDFF2]">
              {item.mark}
            </div>
            <h3 className="mt-4 font-bold text-[#16181D]">{item.title}</h3>
            <p className="mt-1.5 text-sm text-[#667085]">{item.description}</p>
            <p className="mt-4 text-xs font-bold uppercase tracking-wider text-[#98A2B3]">Coming soon</p>
          </article>
        ))}
      </div>

      {/* Delete confirmation dialog */}
      {deleteError && (
        <p role="alert" className="mt-4 rounded-xl border border-[#FECDCA] bg-[#FEF3F2] px-4 py-3 text-sm text-[#B42318]">
          {deleteError}
        </p>
      )}
      <ConfirmDialog
        isOpen={showDeleteDialog}
        title="Delete project"
        message={`Delete "${project.name}"? This action cannot be undone. All members and data associated with this project will be permanently removed.`}
        confirmLabel="Delete project"
        onConfirm={handleDelete}
        onCancel={() => { setShowDeleteDialog(false); setDeleteError('') }}
        isLoading={deleteProject.isPending}
      />
    </main>
  )
}
