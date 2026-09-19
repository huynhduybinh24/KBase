import { apiClient } from './client'
import type { UpdateProfileRequest, UserProfile } from '../types/profile'

export async function getMyProfile(): Promise<UserProfile> {
  return (await apiClient.get<UserProfile>('/api/users/me/profile')).data
}

export async function updateMyProfile(request: UpdateProfileRequest): Promise<UserProfile> {
  return (await apiClient.put<UserProfile>('/api/users/me/profile', request)).data
}

export async function uploadMyAvatar(file: File): Promise<UserProfile> {
  const data = new FormData()
  data.append('file', file)
  return (await apiClient.post<UserProfile>('/api/users/me/avatar', data)).data
}

export async function getMyAvatarBlob(): Promise<Blob> {
  return (await apiClient.get<Blob>('/api/users/me/avatar', { responseType: 'blob' })).data
}

export async function deleteMyAvatar(): Promise<void> {
  await apiClient.delete('/api/users/me/avatar')
}
