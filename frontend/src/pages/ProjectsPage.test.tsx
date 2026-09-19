import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import MockAdapter from 'axios-mock-adapter'
import { apiClient } from '../api/client'
import { ProjectsPage } from './ProjectsPage'
import { authValue, renderRoutes, testUser } from '../test/renderWithAuth'
import type { Project } from '../types/project'

const mock = new MockAdapter(apiClient)

afterEach(() => mock.reset())

const ownerProject: Project = {
  id: 'proj-111',
  name: 'My Research Project',
  description: 'Important findings',
  owner: { id: testUser.id, email: testUser.email },
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-10T00:00:00Z',
}

const memberProject: Project = {
  id: 'proj-222',
  name: 'Shared Work',
  description: null,
  owner: { id: 'other-user-id', email: 'boss@example.com' },
  createdAt: '2026-02-01T00:00:00Z',
  updatedAt: '2026-02-05T00:00:00Z',
}

function setup(auth = authValue({ user: testUser, isAuthenticated: true })) {
  return renderRoutes(
    [
      { path: '/projects', element: <ProjectsPage /> },
      { path: '/projects/:projectId', element: <h1>Project Detail</h1> },
    ],
    '/projects',
    auth,
  )
}

describe('ProjectsPage', () => {
  it('shows a loading skeleton while fetching', () => {
    mock.onGet('/api/projects').reply(() => new Promise(() => {})) // never resolves
    setup()
    expect(document.querySelector('[aria-label="Loading projects"]')).toBeInTheDocument()
  })

  it('renders project cards when data arrives', async () => {
    mock.onGet('/api/projects').reply(200, [ownerProject, memberProject])
    setup()
    expect(await screen.findByText('My Research Project')).toBeInTheDocument()
    expect(screen.getByText('Shared Work')).toBeInTheDocument()
  })

  it('shows owner badge for owned projects and member badge for others', async () => {
    mock.onGet('/api/projects').reply(200, [ownerProject, memberProject])
    setup()
    await screen.findByText('My Research Project')
    const badges = screen.getAllByText(/Owner|Member/)
    expect(badges.some((b) => b.textContent === 'Owner')).toBe(true)
    expect(badges.some((b) => b.textContent === 'Member')).toBe(true)
  })

  it('shows the empty state when no projects exist', async () => {
    mock.onGet('/api/projects').reply(200, [])
    setup()
    expect(await screen.findByText('No projects yet')).toBeInTheDocument()
    expect(screen.getByText(/Create your first project/i)).toBeInTheDocument()
  })

  it('shows an error state on API failure', async () => {
    mock.onGet('/api/projects').reply(500, {
      status: 500, error: 'Internal Server Error', message: 'Something went wrong. Please try again.',
    })
    setup()
    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong')
  })

  it('opens the create form when "New project" is clicked', async () => {
    mock.onGet('/api/projects').reply(200, [])
    setup()
    await screen.findByText('No projects yet')
    await userEvent.click(screen.getByRole('button', { name: /New project/i }))
    expect(screen.getByRole('heading', { name: 'New project' })).toBeInTheDocument()
  })

  it('validates name is required on create', async () => {
    mock.onGet('/api/projects').reply(200, [])
    setup()
    await screen.findByText('No projects yet')
    await userEvent.click(screen.getByRole('button', { name: /New project/i }))
    await userEvent.click(screen.getByRole('button', { name: /Create project/i }))
    expect(await screen.findByText('Project name is required.')).toBeInTheDocument()
  })

  it('creates a project and navigates to detail', async () => {
    mock.onGet('/api/projects').reply(200, [])
    mock.onPost('/api/projects').reply(201, ownerProject)
    setup()
    await screen.findByText('No projects yet')
    await userEvent.click(screen.getByRole('button', { name: /New project/i }))
    await userEvent.type(screen.getByLabelText('Name'), 'My Research Project')
    await userEvent.click(screen.getByRole('button', { name: /Create project/i }))
    expect(await screen.findByText('Project Detail')).toBeInTheDocument()
  })

  it('shows a backend error on create failure', async () => {
    mock.onGet('/api/projects').reply(200, [])
    mock.onPost('/api/projects').reply(400, {
      status: 400, error: 'Bad Request', message: 'Name is invalid.', fieldErrors: {},
    })
    setup()
    await screen.findByText('No projects yet')
    await userEvent.click(screen.getByRole('button', { name: /New project/i }))
    await userEvent.type(screen.getByLabelText('Name'), 'Bad Name')
    await userEvent.click(screen.getByRole('button', { name: /Create project/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Name is invalid.')
  })
})
