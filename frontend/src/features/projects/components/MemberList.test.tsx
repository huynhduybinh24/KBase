import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import MockAdapter from 'axios-mock-adapter'
import { apiClient } from '../../../api/client'
import { MemberList } from './MemberList'
import { authValue, renderRoutes, testUser } from '../../../test/renderWithAuth'
import type { ProjectMember } from '../../../types/project'

const mock = new MockAdapter(apiClient)

afterEach(() => mock.reset())

const ownerMember: ProjectMember = {
  id: 'mem-1', userId: testUser.id, email: testUser.email, role: 'OWNER', joinedAt: '2026-01-01T00:00:00Z',
}
const bobMember: ProjectMember = {
  id: 'mem-2', userId: 'bob-id', email: 'bob@example.com', role: 'MEMBER', joinedAt: '2026-02-01T00:00:00Z',
}

function setupMemberList(isOwner: boolean, members: ProjectMember[] | (() => ProjectMember[])) {
  mock.onGet('/api/projects/proj-1/members').reply(() => [
    200,
    typeof members === 'function' ? members() : members,
  ])
  return renderRoutes(
    [{
      path: '/',
      element: (
        <MemberList
          projectId="proj-1"
          isOwner={isOwner}
          currentUserId={testUser.id}
        />
      ),
    }],
    '/',
    authValue({ user: testUser, isAuthenticated: true }),
  )
}

describe('MemberList', () => {
  it('renders members with role badges', async () => {
    setupMemberList(true, [ownerMember, bobMember])
    expect(await screen.findByText('bob@example.com')).toBeInTheDocument()
    expect(screen.getAllByText('MEMBER').length).toBeGreaterThan(0)
    expect(screen.getAllByText('OWNER').length).toBeGreaterThan(0)
  })

  it('shows the add member form for owners', async () => {
    setupMemberList(true, [ownerMember])
    expect(await screen.findByLabelText(/Add member by email/i)).toBeInTheDocument()
  })

  it('hides the add member form for non-owners', async () => {
    setupMemberList(false, [ownerMember])
    await screen.findByText(testUser.email)
    expect(screen.queryByLabelText(/Add member by email/i)).not.toBeInTheDocument()
  })

  it('shows Remove button for owner next to MEMBER rows', async () => {
    setupMemberList(true, [ownerMember, bobMember])
    expect(await screen.findByRole('button', { name: `Remove ${bobMember.email}` })).toBeInTheDocument()
  })

  it('does NOT show Remove button for the OWNER row', async () => {
    setupMemberList(true, [ownerMember, bobMember])
    await screen.findByText(testUser.email)
    expect(screen.queryByRole('button', { name: `Remove ${testUser.email}` })).not.toBeInTheDocument()
  })

  it('hides Remove buttons for non-owners', async () => {
    setupMemberList(false, [ownerMember, bobMember])
    await screen.findByText('bob@example.com')
    expect(screen.queryByRole('button', { name: /Remove/i })).not.toBeInTheDocument()
  })

  it('shows error state when members fetch fails', async () => {
    mock.onGet('/api/projects/proj-1/members').reply(500, {
      status: 500, error: 'Internal Server Error', message: 'Something went wrong. Please try again.',
    })
    renderRoutes(
      [{ path: '/', element: <MemberList projectId="proj-1" isOwner={true} currentUserId={testUser.id} /> }],
      '/',
      authValue({ user: testUser, isAuthenticated: true }),
    )
    expect(await screen.findByRole('alert')).toBeInTheDocument()
  })

  it('adds a member successfully', async () => {
    const newMember: ProjectMember = { id: 'mem-3', userId: 'carol-id', email: 'carol@example.com', role: 'MEMBER', joinedAt: '2026-03-01T00:00:00Z' }
    let currentMembers = [ownerMember]
    mock.onPost('/api/projects/proj-1/members').reply(() => {
      currentMembers = [ownerMember, newMember]
      return [201, newMember]
    })

    setupMemberList(true, () => currentMembers)
    await userEvent.type(await screen.findByLabelText(/Add member by email/i), 'carol@example.com')
    await userEvent.click(screen.getByRole('button', { name: 'Add' }))

    expect(await screen.findByRole('status')).toHaveTextContent('carol@example.com added as a member')
    expect(await screen.findByText('carol@example.com')).toBeInTheDocument()
  })

  it('shows 409 conflict error when adding duplicate member', async () => {
    mock.onPost('/api/projects/proj-1/members').reply(409, {
      status: 409, error: 'Conflict', message: 'User is already a project member', fieldErrors: {},
    })
    setupMemberList(true, [ownerMember, bobMember])
    await userEvent.type(await screen.findByLabelText(/Add member by email/i), 'bob@example.com')
    await userEvent.click(screen.getByRole('button', { name: 'Add' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('User is already a project member')
  })

  it('removes a member after confirmation', async () => {
    let currentMembers = [ownerMember, bobMember]
    mock.onDelete(`/api/projects/proj-1/members/${bobMember.userId}`).reply(() => {
      currentMembers = [ownerMember]
      return [204]
    })

    setupMemberList(true, () => currentMembers)
    await userEvent.click(await screen.findByRole('button', { name: `Remove ${bobMember.email}` }))
    // Confirm dialog should appear
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Remove' }))
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    )
    await waitFor(() =>
      expect(screen.queryByText('bob@example.com')).not.toBeInTheDocument(),
    )
  })
})
