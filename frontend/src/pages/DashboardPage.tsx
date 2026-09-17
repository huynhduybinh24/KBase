import { useAuth } from '../auth/useAuth'

const features = [
  { title: 'Projects', description: 'Organize knowledge by team and initiative.', mark: 'P' },
  { title: 'Documents', description: 'Upload, search, and manage source material.', mark: 'D' },
  { title: 'AI Chat', description: 'Ask grounded questions with clear citations.', mark: 'AI' },
]

export function DashboardPage() {
  const { user } = useAuth()
  return (
    <main className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <section className="overflow-hidden rounded-3xl border border-white/10 bg-gradient-to-br from-slate-900 to-slate-900/55 p-6 shadow-2xl shadow-black/20 sm:p-10">
        <span className="inline-flex rounded-full border border-cyan-300/20 bg-cyan-300/10 px-3 py-1 text-xs font-medium text-cyan-200">Workspace ready</span>
        <h1 className="mt-5 max-w-3xl text-3xl font-semibold tracking-tight text-white sm:text-4xl">Your knowledge, ready to become useful.</h1>
        <p className="mt-4 max-w-2xl text-base leading-7 text-slate-400">Welcome, <span className="text-slate-200">{user?.email}</span>. The KBase foundation is connected and ready for the next milestone.</p>
      </section>

      <section className="mt-10" aria-labelledby="workspace-tools">
        <div className="flex items-end justify-between gap-4">
          <div>
            <h2 id="workspace-tools" className="text-xl font-semibold text-white">Workspace tools</h2>
            <p className="mt-1 text-sm text-slate-400">Coming in the next frontend milestones.</p>
          </div>
        </div>
        <div className="mt-5 grid gap-4 md:grid-cols-3">
          {features.map((feature) => (
            <article key={feature.title} className="rounded-2xl border border-white/10 bg-slate-900/65 p-6 transition hover:-translate-y-0.5 hover:border-cyan-300/25">
              <div className="grid h-11 w-11 place-items-center rounded-xl border border-white/10 bg-white/5 text-sm font-bold text-cyan-300">{feature.mark}</div>
              <h3 className="mt-5 font-semibold text-white">{feature.title}</h3>
              <p className="mt-2 text-sm leading-6 text-slate-400">{feature.description}</p>
              <p className="mt-5 text-xs font-medium uppercase tracking-wider text-slate-500">Coming soon</p>
            </article>
          ))}
        </div>
      </section>
    </main>
  )
}
