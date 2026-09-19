import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as memberApi from '../../../api/projectMemberApi'
import type { AddMemberRequest } from '../../../types/project'

// ---------------------------------------------------------------------------
// Stable query key factory — scoped under the project detail key tree
// ---------------------------------------------------------------------------
export const memberKeys = {
  members: (projectId: string) => ['projects', 'detail', projectId, 'members'] as const,
}

// ---------------------------------------------------------------------------
// useMembers — list members for a project
// ---------------------------------------------------------------------------
export function useMembers(projectId: string) {
  return useQuery({
    queryKey: memberKeys.members(projectId),
    queryFn: () => memberApi.getMembers(projectId),
    enabled: Boolean(projectId),
  })
}

// ---------------------------------------------------------------------------
// useAddMember
// ---------------------------------------------------------------------------
export function useAddMember(projectId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: AddMemberRequest) => memberApi.addMember(projectId, request),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memberKeys.members(projectId) })
    },
  })
}

// ---------------------------------------------------------------------------
// useRemoveMember
// ---------------------------------------------------------------------------
export function useRemoveMember(projectId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (userId: string) => memberApi.removeMember(projectId, userId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: memberKeys.members(projectId) })
    },
  })
}
