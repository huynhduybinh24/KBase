import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as projectApi from '../../../api/projectApi'
import { getErrorMessage } from '../../../api/client'
import type { ProjectFormData } from '../../../types/project'

// ---------------------------------------------------------------------------
// Stable query key factory
// ---------------------------------------------------------------------------
export const projectKeys = {
  all: ['projects'] as const,
  list: () => [...projectKeys.all, 'list'] as const,
  detail: (id: string) => [...projectKeys.all, 'detail', id] as const,
}

// ---------------------------------------------------------------------------
// useProjects — list all accessible projects
// ---------------------------------------------------------------------------
export function useProjects() {
  return useQuery({
    queryKey: projectKeys.list(),
    queryFn: projectApi.getProjects,
  })
}

// ---------------------------------------------------------------------------
// useProject — single project by id
// ---------------------------------------------------------------------------
export function useProject(id: string) {
  return useQuery({
    queryKey: projectKeys.detail(id),
    queryFn: () => projectApi.getProject(id),
    enabled: Boolean(id),
  })
}

// ---------------------------------------------------------------------------
// useCreateProject
// ---------------------------------------------------------------------------
export function useCreateProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: ProjectFormData) => projectApi.createProject(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: projectKeys.list() })
    },
  })
}

// ---------------------------------------------------------------------------
// useUpdateProject
// ---------------------------------------------------------------------------
export function useUpdateProject(projectId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: ProjectFormData) => projectApi.updateProject(projectId, data),
    onSuccess: (updated) => {
      queryClient.setQueryData(projectKeys.detail(projectId), updated)
      void queryClient.invalidateQueries({ queryKey: projectKeys.list() })
    },
  })
}

// ---------------------------------------------------------------------------
// useDeleteProject
// ---------------------------------------------------------------------------
export function useDeleteProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (projectId: string) => projectApi.deleteProject(projectId),
    onSuccess: (_data, projectId) => {
      queryClient.removeQueries({ queryKey: projectKeys.detail(projectId) })
      void queryClient.invalidateQueries({ queryKey: projectKeys.list() })
    },
  })
}

// ---------------------------------------------------------------------------
// Re-export helper so callers don't import from two places
// ---------------------------------------------------------------------------
export { getErrorMessage }
