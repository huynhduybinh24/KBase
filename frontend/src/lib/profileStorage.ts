import type { LegacyClientProfile } from '../types/profile'

const PROFILE_PREFIX = 'kbase.profile:'

function profileKey(email: string) {
  return `${PROFILE_PREFIX}${email.trim().toLowerCase()}`
}

export function getClientProfile(email?: string | null): LegacyClientProfile {
  if (!email) return {}
  try {
    const raw = window.localStorage.getItem(profileKey(email))
    return raw ? JSON.parse(raw) as LegacyClientProfile : {}
  } catch {
    return {}
  }
}

export function removeClientProfile(email: string) {
  window.localStorage.removeItem(profileKey(email))
}
