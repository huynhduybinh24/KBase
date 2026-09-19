import { Link } from 'react-router-dom'

export function BrandMark({ to = '/', compact = false, responsive = false }: { to?: string; compact?: boolean; responsive?: boolean }) {
  return (
    <Link
      to={to}
      aria-label="KBase home"
      className="group flex w-fit items-center gap-3 rounded-xl focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#5B5BD6]"
    >
      {compact ? (
        <img src="/assets/kbase/kbase-icon.png" alt="KBase" className="h-10 w-10 object-contain" />
      ) : (
        <picture>
          {responsive && <source media="(max-width: 639px)" srcSet="/assets/kbase/kbase-icon.png" />}
          <img src="/assets/kbase/kbase-logo-web.png" alt="KBase" className="h-10 w-[132px] object-contain" />
        </picture>
      )}
    </Link>
  )
}
