import { Link } from 'react-router-dom'
import type { Project } from '../../../types/project'
import { useAuth } from '../../../auth/useAuth'

interface Props {
  project: Project
}

export function ProjectCard({ project }: Props) {
  const { user } = useAuth()
  const isOwner = user?.id === project.owner.id
  const updatedDate = new Date(project.updatedAt).toLocaleDateString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric',
  })
  const accent = project.name.charCodeAt(0) % 3
  const iconTone = [
    'from-[#5B5BD6] to-[#7C6EE6]',
    'from-[#3B82F6] to-[#06B6D4]',
    'from-[#8B5CF6] to-[#6366F1]',
  ][accent]

  return (
    <article className="glass-panel group relative flex min-h-52 flex-col justify-between rounded-[22px] p-6 transition duration-200 hover:-translate-y-1 hover:border-white hover:bg-white/76 hover:shadow-[0_18px_42px_rgba(45,48,90,.16)]">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0 flex-1">
          <div className="flex items-center justify-between gap-2">
            <span className={`grid h-11 w-11 place-items-center rounded-[14px] bg-gradient-to-br ${iconTone} text-sm font-black text-white shadow-sm`}>{project.name.charAt(0).toUpperCase()}</span>
            <span
              className={`inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-bold ${
                isOwner
                  ? 'bg-[#F0EDFF] text-[#6941C6] ring-1 ring-inset ring-[#DDD6FE]'
                  : 'bg-[#EFF8FF] text-[#175CD3] ring-1 ring-inset ring-[#B2DDFF]'
              }`}
            >
              {isOwner ? 'Owner' : 'Member'}
            </span>
          </div>
          <h3 className="mt-4 truncate text-lg font-bold leading-snug tracking-[-0.015em] text-[#16181D]">
            {project.name}
          </h3>
          {project.description && (
            <p className="mt-2 line-clamp-2 text-sm leading-6 text-[#667085]">
              {project.description}
            </p>
          )}
        </div>
      </div>

      <div className="mt-5 flex items-center justify-between gap-3">
        <p className="text-xs font-medium text-[#98A2B3]">Updated {updatedDate}</p>
        <Link
          to={`/projects/${project.id}`}
          className="flex items-center rounded-xl border border-[#E2E4EA] bg-white px-3 py-2 text-sm font-semibold text-[#475467] transition hover:border-[#B8B8ED] hover:bg-[#FAFAFF] hover:text-[#4F46C8] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#5B5BD6]"
          aria-label={`Open project ${project.name}`}
        >
          Open →
        </Link>
      </div>
    </article>
  )
}
