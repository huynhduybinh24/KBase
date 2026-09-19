import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import { getClientProfile, removeClientProfile } from '../../lib/profileStorage'
import type { UserResponse } from '../../types/auth'
import type { LegacyClientProfile, UserProfile } from '../../types/profile'
import { ProfileAvatar } from '../Avatar'
import { ProfileEditor } from '../../features/profile/components/ProfileEditor'
import { useAvatarObjectUrl, useMyProfile, useUpdateProfile } from '../../features/profile/hooks/useProfile'
import { useLanguage } from '../../i18n/LanguageProvider'

type DisplayableUser = UserResponse & Partial<{ displayName: string; fullName: string; name: string; username: string }>
export function getUserDisplayName(user: DisplayableUser | null | undefined, profile?: Pick<UserProfile, 'displayName' | 'fullName'> | LegacyClientProfile) {
  if (!user) return 'Account'
  const preferred = [profile?.displayName, profile?.fullName, user.displayName, user.fullName, user.name, user.username].find((value) => value?.trim())
  return preferred?.trim() || user.email.split('@')[0] || user.email || 'Account'
}
function Chevron({ open }: { open: boolean }) { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className={`h-4 w-4 transition-transform ${open ? 'rotate-180' : ''}`} aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg> }

export function AccountMenu() {
  const { user, logout } = useAuth()
  const { text } = useLanguage()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(false)
  const root = useRef<HTMLDivElement>(null)
  const migrated = useRef(false)
  const profileQuery = useMyProfile()
  const migration = useUpdateProfile()
  const profile = profileQuery.data
  const avatar = useAvatarObjectUrl(profile)
  const displayName = getUserDisplayName(user, profile)

  useEffect(() => {
    if (!profile || !user || migrated.current) return
    migrated.current = true
    const legacy = getClientProfile(user.email)
    const fallback = user.email.split('@')[0]
    const legacyName = legacy.displayName?.trim() || legacy.fullName?.trim()
    const preset = legacy.avatarType === 'preset' ? legacy.avatarValue : undefined
    if (profile.fullName == null && profile.displayName === fallback && (legacyName || preset)) {
      migration.mutate({ displayName: legacyName || fallback, avatarType: preset ? 'PRESET' : undefined, avatarPreset: preset as 'violet' | 'blue' | 'rose' | 'teal' | undefined }, { onSuccess: () => { if (legacy.avatarType !== 'upload') removeClientProfile(user.email) } })
    }
  }, [profile, user, migration])

  useEffect(() => {
    if (!open) return
    const outside = (event: PointerEvent) => { if (!root.current?.contains(event.target as Node)) setOpen(false) }
    const escape = (event: KeyboardEvent) => { if (event.key === 'Escape') setOpen(false) }
    document.addEventListener('pointerdown', outside); document.addEventListener('keydown', escape)
    return () => { document.removeEventListener('pointerdown', outside); document.removeEventListener('keydown', escape) }
  }, [open])

  function handleLogout() { logout(); navigate('/login', { replace: true }) }
  return <div ref={root} className="relative shrink-0">
    <button type="button" aria-label="Open account menu" aria-haspopup="dialog" aria-expanded={open} onClick={() => setOpen((value) => !value)} className={`flex h-11 items-center gap-2 rounded-[14px] px-1.5 text-[#344054] transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#5B5BD6] sm:px-2 ${open ? 'bg-white/30' : 'hover:bg-white/20'}`}><ProfileAvatar profile={profile} imageUrl={avatar.url} size="sm"/><span className="hidden max-w-40 truncate text-[13px] font-semibold md:block">{displayName}</span><Chevron open={open}/></button>
    {open && <section role="dialog" aria-label="Account panel" className="absolute right-0 top-[calc(100%+10px)] z-50 max-h-[min(740px,calc(100vh-90px))] w-[min(370px,calc(100vw-24px))] overflow-y-auto rounded-[22px] border border-white/60 bg-white/92 p-3 shadow-[0_22px_60px_rgba(15,23,42,.2)] backdrop-blur-[22px]">
      <header className="flex items-center gap-3 rounded-2xl bg-gradient-to-br from-[#F8F7FF] to-[#EFF6FF] p-3"><ProfileAvatar profile={profile} imageUrl={avatar.url} size="lg"/><div className="min-w-0 flex-1"><p className="text-base font-bold text-[#18204B]">{displayName}</p><p className="break-words text-xs text-[#667085]">{user?.email}</p><div className="mt-2 flex gap-1">{(profile?.roles ?? user?.roles ?? []).map((role) => <span key={role} className="rounded-full bg-white px-2 py-0.5 text-[10px] font-bold text-[#6941C6] ring-1 ring-[#DDD6FE]">{role}</span>)}</div></div></header>
      {profileQuery.isError && <p className="mt-2 rounded-xl bg-[#FFFAEB] px-3 py-2 text-xs text-[#B54708]">{text('Profile is temporarily unavailable. Showing account fallback.', 'Thông tin hồ sơ tạm thời không khả dụng.')}</p>}
      {editing && profile ? <div className="mt-3"><ProfileEditor profile={profile} onSaved={() => setEditing(false)} onCancel={() => setEditing(false)}/></div> : <div className="mt-2"><button type="button" disabled={!profile} onClick={() => setEditing(true)} className="w-full rounded-xl px-3 py-2.5 text-left text-sm font-semibold text-[#4F46C8] hover:bg-[#F5F3FF] disabled:opacity-50">{text('Edit profile', 'Chỉnh sửa hồ sơ')}</button></div>}
      <div className="my-1 h-px bg-[#E7E9F0]"/><button type="button" onClick={handleLogout} className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold text-[#B42318] hover:bg-[#FEF3F2]">{text('Log out', 'Đăng xuất')}</button>
    </section>}
  </div>
}
