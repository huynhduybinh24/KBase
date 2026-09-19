import { apiClient } from './client'
import type { AddMemberRequest, ProjectMember } from '../types/project'

/** GET /api/projects/:projectId/members */
export function getMembers(projectId: string): Promise<ProjectMember[]> {
  return apiClient.get<ProjectMember[]>(`/api/projects/${projectId}/members`).then((r) => r.data)
}

/** POST /api/projects/:projectId/members — owner only, 201 Created */
export function addMember(projectId: string, request: AddMemberRequest): Promise<ProjectMember> {
  return apiClient.post<ProjectMember>(`/api/projects/${projectId}/members`, request).then((r) => r.data)
}

/** DELETE /api/projects/:projectId/members/:userId — owner only, 204 No Content */
export function removeMember(projectId: string, userId: string): Promise<void> {
  return apiClient.delete(`/api/projects/${projectId}/members/${userId}`).then(() => undefined)
}
