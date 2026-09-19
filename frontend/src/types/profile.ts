export type AvatarType = 'NONE' | 'PRESET' | 'UPLOAD'
export type AvatarPreset = 'violet' | 'blue' | 'rose' | 'teal'

export interface UserProfile {
  userId: string
  email: string
  fullName: string | null
  displayName: string
  roles: string[]
  avatarType: AvatarType
  avatarPreset: AvatarPreset | null
  hasAvatar: boolean
  avatarUpdatedAt: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface UpdateProfileRequest {
  displayName: string
  avatarType?: 'NONE' | 'PRESET'
  avatarPreset?: AvatarPreset | null
}

export interface LegacyClientProfile {
  fullName?: string
  displayName?: string
  avatarType?: 'preset' | 'upload'
  avatarValue?: AvatarPreset | string
}
