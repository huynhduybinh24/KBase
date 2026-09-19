import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <main className="grid min-h-screen place-items-center px-4 text-center text-[#16181D]">
      <div>
        <p className="text-sm font-bold uppercase tracking-[0.25em] text-[#5B5BD6]">404</p>
        <h1 className="mt-3 text-4xl font-semibold tracking-tight">Page not found</h1>
        <p className="mt-4 text-[#667085]">The page you requested does not exist.</p>
        <Link to="/" className="mt-7 inline-flex rounded-xl bg-[#5B5BD6] px-5 py-3 text-sm font-semibold text-white shadow-md hover:bg-[#4F46C8] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#5B5BD6]">Return to KBase</Link>
      </div>
    </main>
  )
}
