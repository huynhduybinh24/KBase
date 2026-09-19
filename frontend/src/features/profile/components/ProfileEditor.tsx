import { useEffect, useState } from 'react'
import { getErrorMessage } from '../../../api/client'
import { Avatar } from '../../../components/Avatar'
import type { AvatarPreset, UserProfile } from '../../../types/profile'
import { useDeleteAvatar, useUpdateProfile, useUploadAvatar } from '../hooks/useProfile'

const presets: AvatarPreset[] = ['violet', 'blue', 'rose', 'teal']
type Selection = { type: 'CURRENT' } | { type: 'PRESET'; preset: AvatarPreset } | { type: 'UPLOAD'; file: File; url: string } | { type: 'NONE' }

export function ProfileEditor({ profile, onSaved, onCancel }: { profile: UserProfile; onSaved?: () => void; onCancel?: () => void }) {
  const [displayName, setDisplayName] = useState(profile.displayName)
  const [selection, setSelection] = useState<Selection>({ type: 'CURRENT' })
  const [error, setError] = useState('')
  const update = useUpdateProfile()
  const upload = useUploadAvatar()
  const remove = useDeleteAvatar()
  const pending = update.isPending || upload.isPending || remove.isPending

  useEffect(() => () => { if (selection.type === 'UPLOAD') URL.revokeObjectURL(selection.url) }, [selection])

  function chooseFile(file?: File) {
    if (!file) return
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) { setError('Choose a PNG, JPEG, or WebP image.'); return }
    if (file.size > 2 * 1024 * 1024) { setError('Avatar image must be 2 MB or smaller.'); return }
    setSelection((current) => { if (current.type === 'UPLOAD') URL.revokeObjectURL(current.url); return { type: 'UPLOAD', file, url: URL.createObjectURL(file) } })
    setError('')
  }

  async function save() {
    const name = displayName.trim().replace(/\s+/g, ' ')
    if (name.length < 2 || name.length > 120) { setError('Display name must be between 2 and 120 characters.'); return }
    try {
      if (selection.type === 'PRESET') await update.mutateAsync({ displayName: name, avatarType: 'PRESET', avatarPreset: selection.preset })
      else if (selection.type === 'NONE') {
        await update.mutateAsync({ displayName: name })
        if (profile.avatarType !== 'NONE') await remove.mutateAsync()
      }
      else {
        await update.mutateAsync({ displayName: name })
        if (selection.type === 'UPLOAD') await upload.mutateAsync(selection.file)
      }
      onSaved?.()
    } catch (caught) { setError(getErrorMessage(caught)) }
  }

  return (
    <div className="rounded-2xl border border-[#E7E9F0] bg-white/75 p-4">
      <label htmlFor="profile-display-name" className="text-xs font-bold text-[#344054]">Display name</label>
      <input id="profile-display-name" value={displayName} onChange={(event) => setDisplayName(event.target.value)} maxLength={120} className="mt-1.5 h-11 w-full rounded-xl border border-[#DDE1EA] bg-white px-3 text-sm outline-none focus:border-[#5B5BD6] focus:ring-4 focus:ring-[#5B5BD6]/10" />
      <fieldset className="mt-4"><legend className="text-xs font-bold text-[#344054]">Avatar</legend><div className="mt-2 flex flex-wrap gap-2">{presets.map((preset) => <button key={preset} type="button" aria-label={`Use ${preset} avatar`} aria-pressed={selection.type === 'PRESET' && selection.preset === preset} onClick={() => setSelection({ type: 'PRESET', preset })} className="rounded-full p-1 aria-pressed:ring-2 aria-pressed:ring-[#5B5BD6]"><Avatar email={profile.email} displayName={displayName} preset={preset} size="sm" /></button>)}</div></fieldset>
      {selection.type === 'UPLOAD' && <div className="mt-3 flex items-center gap-3"><Avatar email={profile.email} displayName={displayName} imageUrl={selection.url} /><span className="text-xs text-[#667085]">Ready to upload</span></div>}
      <div className="mt-4 grid grid-cols-2 gap-2"><label className="flex cursor-pointer items-center justify-center rounded-xl border border-dashed border-[#B9BCE8] bg-[#F8F7FF] px-3 py-2.5 text-sm font-semibold text-[#4F46C8] hover:bg-[#F0EDFF] focus-within:ring-2 focus-within:ring-[#5B5BD6]">Upload image<input aria-label="Upload avatar" type="file" accept="image/png,image/jpeg,image/webp" onChange={(event) => chooseFile(event.target.files?.[0])} className="sr-only" /></label><button type="button" onClick={() => setSelection({ type: 'NONE' })} className="rounded-xl border border-[#DDE1EA] px-3 py-2.5 text-sm font-semibold text-[#475467] hover:bg-[#F8F9FC]">Reset avatar</button></div>
      {error && <p role="alert" className="mt-2 text-xs text-[#B42318]">{error}</p>}
      <div className="mt-4 flex gap-2"><button type="button" disabled={pending} onClick={() => void save()} className="flex-1 rounded-xl bg-[#5B5BD6] px-3 py-2.5 text-sm font-bold text-white hover:bg-[#4F46C8] disabled:opacity-60">{pending ? 'Saving…' : 'Save changes'}</button>{onCancel && <button type="button" onClick={onCancel} className="rounded-xl border border-[#DDE1EA] px-3 py-2.5 text-sm font-semibold text-[#475467]">Cancel</button>}</div>
    </div>
  )
}
