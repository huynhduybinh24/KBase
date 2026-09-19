import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import MockAdapter from 'axios-mock-adapter'
import { apiClient } from '../api/client'
import { ProjectDetailPage } from './ProjectDetailPage'
import { authValue, renderRoutes, testUser } from '../test/renderWithAuth'
import type { Project, ProjectMember } from '../types/project'

const mock = new MockAdapter(apiClient)

afterEach(() => mock.reset())

const ownerProject: Project = {
  id: 'proj-abc',
  name: 'Alpha Project',
  description: 'A test project',
  owner: { id: testUser.id, email: testUser.email },
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-06-01T00:00:00Z',
}

const memberProject: Project = {
  id: 'proj-abc',
  name: 'Alpha Project',
  description: 'A test project',
  owner: { id: 'other-owner-id', email: 'owner@example.com' },
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-06-01T00:00:00Z',
}

const ownerMember: ProjectMember = {
  id: 'mem-1', userId: testUser.id, email: testUser.email, role: 'OWNER', joinedAt: '2026-01-01T00:00:00Z',
}
const regularMember: ProjectMember = {
  id: 'mem-2', userId: 'other-user-id', email: 'bob@example.com', role: 'MEMBER', joinedAt: '2026-02-01T00:00:00Z',
}

function setup(project: Project, auth = authValue({ user: testUser, isAuthenticated: true })) {
  const currentMember = project.owner.id === testUser.id
    ? ownerMember
    : { ...regularMember, id: 'mem-current', userId: testUser.id, email: testUser.email }
  mock.onGet(`/api/projects/${project.id}`).reply(200, project)
  mock.onGet(`/api/projects/${project.id}/members`).reply(200, [currentMember, regularMember])
  return renderRoutes(
    [
      { path: '/projects', element: <h1>Projects List</h1> },
      { path: '/projects/:projectId', element: <ProjectDetailPage /> },
    ],
    `/projects/${project.id}`,
    auth,
  )
}

describe('ProjectDetailPage', () => {
  it('renders project name and description', async () => {
    setup(ownerProject)
    expect(await screen.findByText('Alpha Project')).toBeInTheDocument()
    expect(screen.getByText('A test project')).toBeInTheDocument()
  })

  it('shows the owner email in the metadata', async () => {
    setup(ownerProject)
    expect(await screen.findByText(/Owner:/)).toHaveTextContent(testUser.email)
  })

  it('renders member list', async () => {
    setup(ownerProject)
    expect(await screen.findByText('bob@example.com')).toBeInTheDocument()
  })

  it('shows OWNER controls when current user is owner', async () => {
    setup(ownerProject)
    expect(await screen.findByRole('button', { name: 'Edit' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument()
    expect(screen.getByLabelText(/Add member by email/i)).toBeInTheDocument()
  })

  it('hides OWNER controls when current user is a member', async () => {
    setup(memberProject)
    await screen.findByText('Alpha Project')
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/Add member by email/i)).not.toBeInTheDocument()
  })

  it('handles 403 / not found gracefully', async () => {
    mock.onGet('/api/projects/bad-id').reply(403, {
      status: 403, error: 'Forbidden', message: 'You are not a member of this project.', fieldErrors: {},
    })
    mock.onGet('/api/projects/bad-id/members').reply(403)
    renderRoutes(
      [{ path: '/projects/:projectId', element: <ProjectDetailPage /> }],
      '/projects/bad-id',
      authValue({ user: testUser, isAuthenticated: true }),
    )
    expect(await screen.findByRole('alert')).toHaveTextContent('You are not a member')
  })

  it('opens the inline edit form when owner clicks Edit', async () => {
    setup(ownerProject)
    await userEvent.click(await screen.findByRole('button', { name: 'Edit' }))
    expect(screen.getByRole('heading', { name: 'Edit project' })).toBeInTheDocument()
    // Pre-populated values
    expect((screen.getByLabelText('Name') as HTMLInputElement).value).toBe('Alpha Project')
  })

  it('cancels edit and returns to view mode', async () => {
    setup(ownerProject)
    await userEvent.click(await screen.findByRole('button', { name: 'Edit' }))
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(await screen.findByRole('button', { name: 'Edit' })).toBeInTheDocument()
  })

  it('saves updated project name', async () => {
    const updated = { ...ownerProject, name: 'Updated Name' }
    mock.onPut(`/api/projects/${ownerProject.id}`).reply(200, updated)
    setup(ownerProject)
    await userEvent.click(await screen.findByRole('button', { name: 'Edit' }))
    const nameInput = screen.getByLabelText('Name') as HTMLInputElement
    await userEvent.clear(nameInput)
    await userEvent.type(nameInput, 'Updated Name')
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    // After save, edit form should close
    await waitFor(() =>
      expect(screen.queryByRole('heading', { name: 'Edit project' })).not.toBeInTheDocument(),
    )
  })

  it('shows error when update fails', async () => {
    mock.onPut(`/api/projects/${ownerProject.id}`).reply(403, {
      status: 403, error: 'Forbidden', message: 'Only the project owner can perform this action', fieldErrors: {},
    })
    setup(ownerProject)
    await userEvent.click(await screen.findByRole('button', { name: 'Edit' }))
    const nameInput = screen.getByLabelText('Name') as HTMLInputElement
    await userEvent.clear(nameInput)
    await userEvent.type(nameInput, 'New Name')
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Only the project owner')
  })

  it('opens delete dialog on Delete button click', async () => {
    setup(ownerProject)
    await userEvent.click(await screen.findByRole('button', { name: 'Delete' }))
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText(/This action cannot be undone/)).toBeInTheDocument()
  })

  it('cancels delete dialog without deleting', async () => {
    setup(ownerProject)
    await userEvent.click(await screen.findByRole('button', { name: 'Delete' }))
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('deletes project and navigates to /projects', async () => {
    mock.onDelete(`/api/projects/${ownerProject.id}`).reply(204)
    setup(ownerProject)
    await userEvent.click(await screen.findByRole('button', { name: 'Delete' }))
    await userEvent.click(screen.getByRole('button', { name: 'Delete project' }))
    expect(await screen.findByText('Projects List')).toBeInTheDocument()
  })
})
