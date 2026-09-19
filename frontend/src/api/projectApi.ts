import { apiClient } from './client'
import type { Project, ProjectFormData } from '../types/project'

/** GET /api/projects — all accessible projects for the current user */
export function getProjects(): Promise<Project[]> {
  return apiClient.get<Project[]>('/api/projects').then((r) => r.data)
}

/** GET /api/projects/:id */
export function getProject(id: string): Promise<Project> {
  return apiClient.get<Project>(`/api/projects/${id}`).then((r) => r.data)
}

/** POST /api/projects — 201 Created */
export function createProject(data: ProjectFormData): Promise<Project> {
  return apiClient.post<Project>('/api/projects', data).then((r) => r.data)
}

/** PUT /api/projects/:id — owner only */
export function updateProject(id: string, data: ProjectFormData): Promise<Project> {
  return apiClient.put<Project>(`/api/projects/${id}`, data).then((r) => r.data)
}

/** DELETE /api/projects/:id — owner only, 204 No Content */
export function deleteProject(id: string): Promise<void> {
  return apiClient.delete(`/api/projects/${id}`).then(() => undefined)
}
