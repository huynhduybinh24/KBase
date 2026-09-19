import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as profileApi from '../../../api/profileApi'
import type { UpdateProfileRequest, UserProfile } from '../../../types/profile'
import { useAuth } from '../../../auth/useAuth'

export const profileKeys = {
  all: ['profile'] as const,
  me: (userId?: string) => [...profileKeys.all, 'me', userId ?? 'anonymous'] as const,
  avatar: (userId?: string, updatedAt?: string | null) => [...profileKeys.all, 'avatar', userId ?? 'anonymous', updatedAt ?? 'none'] as const,
}

export function useMyProfile() {
  const { user } = useAuth()
  return useQuery({ queryKey: profileKeys.me(user?.id), queryFn: profileApi.getMyProfile, enabled: Boolean(user) })
}

function useProfileMutation<T>(mutationFn: (value: T) => Promise<UserProfile>) {
  const client = useQueryClient()
  const { user } = useAuth()
  return useMutation({ mutationFn, onSuccess: (profile) => {
    client.setQueryData(profileKeys.me(user?.id), profile)
    void client.invalidateQueries({ queryKey: profileKeys.all })
  } })
}

export function useUpdateProfile() { return useProfileMutation<UpdateProfileRequest>(profileApi.updateMyProfile) }
export function useUploadAvatar() { return useProfileMutation<File>(profileApi.uploadMyAvatar) }

export function useDeleteAvatar() {
  const client = useQueryClient()
  return useMutation({ mutationFn: profileApi.deleteMyAvatar, onSuccess: async () => {
    await client.invalidateQueries({ queryKey: profileKeys.all })
  } })
}

export function useAvatarObjectUrl(profile?: UserProfile) {
  const avatar = useQuery({
    queryKey: profileKeys.avatar(profile?.userId, profile?.avatarUpdatedAt),
    queryFn: profileApi.getMyAvatarBlob,
    enabled: profile?.avatarType === 'UPLOAD',
    staleTime: Infinity,
  })
  const [url, setUrl] = useState<string>()
  useEffect(() => {
    if (!avatar.data) { setUrl(undefined); return }
    const next = URL.createObjectURL(avatar.data)
    setUrl(next)
    return () => URL.revokeObjectURL(next)
  }, [avatar.data])
  return { url, isLoading: avatar.isLoading, isError: avatar.isError }
}
