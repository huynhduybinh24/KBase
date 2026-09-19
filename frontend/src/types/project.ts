// --------------------------------------------------------------------------
// Exact TypeScript mirror of backend Java DTOs — do not guess field names.
// --------------------------------------------------------------------------

/** Mirrors ProjectUserResponse.java (record) */
export interface ProjectUser {
  id: string   // UUID
  email: string
}

/** Mirrors ProjectResponse.java (record) */
export interface Project {
  id: string   // UUID
  name: string
  description: string | null
  owner: ProjectUser
  createdAt: string  // ISO-8601 Instant
  updatedAt: string  // ISO-8601 Instant
}

/** Mirrors ProjectMemberRole.java (enum) */
export type ProjectMemberRole = 'OWNER' | 'MEMBER'

/** Mirrors ProjectMemberResponse.java (record) */
export interface ProjectMember {
  id: string           // UUID — member record id
  userId: string       // UUID — the actual user id
  email: string
  role: ProjectMemberRole
  joinedAt: string     // ISO-8601 Instant
}

/** Mirrors CreateProjectRequest.java / UpdateProjectRequest.java */
export interface ProjectFormData {
  name: string
  description: string
}

/** Mirrors AddProjectMemberRequest.java */
export interface AddMemberRequest {
  email: string
}
