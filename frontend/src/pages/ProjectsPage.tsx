import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useProjects, useCreateProject } from '../features/projects/hooks/useProjects'
import { ProjectCard } from '../features/projects/components/ProjectCard'
import { ProjectForm } from '../features/projects/components/ProjectForm'
import type { ProjectFormData } from '../types/project'

export function ProjectsPage() {
  const navigate = useNavigate()
  const [showCreate, setShowCreate] = useState(false)

  const { data: projects, isLoading, isError, error } = useProjects()
  const createProject = useCreateProject()

  async function handleCreate(data: ProjectFormData) {
    const created = await createProject.mutateAsync(data)
    setShowCreate(false)
    navigate(`/projects/${created.id}`)
  }

  return (
    <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 sm:py-12 lg:px-8">
      {/* Page header */}
      <div className="glass-panel relative mb-9 flex flex-col items-start justify-between gap-5 overflow-hidden rounded-[24px] px-6 py-7 sm:flex-row sm:items-center sm:px-8">
        <div className="pointer-events-none absolute -right-16 -top-24 h-56 w-56 rounded-full bg-[#5B5BD6]/10 blur-3xl" />
        <div>
          <p className="text-sm font-semibold text-[#5B5BD6]">Workspace</p>
          <h1 className="mt-1 text-3xl font-bold tracking-[-0.035em] text-[#16181D]">Projects</h1>
          <p className="mt-2 text-sm text-[#667085]">
            Create focused spaces for your team's knowledge.
          </p>
        </div>
        {!showCreate && (
          <button
            type="button"
            id="create-project-btn"
            onClick={() => setShowCreate(true)}
            className="relative h-11 shrink-0 rounded-xl bg-[#5B5BD6] px-5 text-sm font-semibold text-white shadow-[0_6px_14px_rgba(91,91,214,.22)] transition hover:-translate-y-px hover:bg-[#4F46C8] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#5B5BD6]"
          >
            + New project
          </button>
        )}
      </div>

      {/* Create project modal */}
      {showCreate && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-[#17172F]/35 p-4 backdrop-blur-sm">
        <section
          role="dialog"
          aria-modal="true"
          aria-labelledby="create-project-heading"
          className="glass-panel w-full max-w-[520px] rounded-[24px] bg-white/78 p-6 sm:p-8"
        >
          <h2 id="create-project-heading" className="text-xl font-bold tracking-[-0.02em] text-[#16181D]">New project</h2>
          <p className="mb-6 mt-2 text-sm leading-6 text-[#667085]">Start a workspace for documents and project knowledge.</p>
          <ProjectForm
            onSubmit={handleCreate}
            onCancel={() => setShowCreate(false)}
            submitLabel="Create project"
            isLoading={createProject.isPending}
          />
        </section>
        </div>
      )}

      {/* Loading skeleton */}
      {isLoading && !projects && (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3" aria-label="Loading projects">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="glass-panel h-52 animate-pulse rounded-[22px]" />
          ))}
        </div>
      )}

      {/* Error state */}
      {isError && (
        <div className="rounded-xl border border-[#FECDCA] bg-[#FEF3F2] px-5 py-4">
          <p role="alert" className="text-sm text-[#B42318]">
            {error instanceof Error ? error.message : 'Failed to load projects.'}
          </p>
        </div>
      )}

      {/* Empty state */}
      {projects && projects.length === 0 && !showCreate && (
        <div className="glass-panel flex flex-col items-center justify-center rounded-[24px] border-dashed px-6 py-20 text-center">
          <div className="grid h-16 w-16 place-items-center rounded-[20px] bg-gradient-to-br from-[#EEF0FF] to-[#E9F3FF] text-xl font-black text-[#5B5BD6] ring-1 ring-[#DCDDF7]">P</div>
          <h2 className="mt-5 text-xl font-bold text-[#16181D]">No projects yet</h2>
          <p className="mt-2 max-w-sm text-sm leading-6 text-[#667085]">
            Create your first project and start building your team's knowledge base.
          </p>
          <button
            type="button"
            onClick={() => setShowCreate(true)}
            className="mt-6 h-11 rounded-xl bg-[#5B5BD6] px-5 text-sm font-semibold text-white shadow-md transition hover:bg-[#4F46C8] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#5B5BD6]"
          >
            Create project
          </button>
        </div>
      )}

      {/* Project grid */}
      {projects && projects.length > 0 && (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {projects.map((project) => (
            <ProjectCard key={project.id} project={project} />
          ))}
        </div>
      )}
    </main>
  )
}
