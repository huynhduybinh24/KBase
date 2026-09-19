import type { AvatarPreset, UserProfile } from '../types/profile'

const presetClasses: Record<AvatarPreset, string> = {
  violet: 'bg-gradient-to-br from-[#DDD6FE] to-[#A78BFA] text-[#4C1D95] ring-[#C4B5FD]',
  blue: 'bg-gradient-to-br from-[#BFDBFE] to-[#60A5FA] text-[#1E3A8A] ring-[#93C5FD]',
  rose: 'bg-gradient-to-br from-[#FECDD3] to-[#FB7185] text-[#881337] ring-[#FDA4AF]',
  teal: 'bg-gradient-to-br from-[#99F6E4] to-[#2DD4BF] text-[#134E4A] ring-[#5EEAD4]',
}

export function Avatar({ email, displayName, preset, imageUrl, size = 'md', tone }: {
  email?: string
  displayName?: string
  preset?: AvatarPreset | null
  imageUrl?: string
  size?: 'sm' | 'md' | 'lg'
  tone?: 'violet' | 'blue'
}) {
  const initial = (displayName || email)?.trim().charAt(0).toUpperCase() || '?'
  const dimensions = size === 'sm' ? 'h-8 w-8 text-xs' : size === 'lg' ? 'h-20 w-20 text-xl' : 'h-10 w-10 text-sm'
  if (imageUrl) return <img src={imageUrl} alt="" className={`${dimensions} shrink-0 rounded-full object-cover ring-2 ring-white/80`} />
  return <span aria-hidden="true" className={`grid shrink-0 place-items-center rounded-full font-bold ring-1 ring-inset ${dimensions} ${presetClasses[preset ?? tone ?? 'violet']}`}>{initial}</span>
}

export function ProfileAvatar({ profile, imageUrl, size = 'md' }: { profile?: UserProfile; imageUrl?: string; size?: 'sm' | 'md' | 'lg' }) {
  return <Avatar email={profile?.email} displayName={profile?.displayName} preset={profile?.avatarType === 'PRESET' ? profile.avatarPreset : null} imageUrl={imageUrl} size={size} />
}
