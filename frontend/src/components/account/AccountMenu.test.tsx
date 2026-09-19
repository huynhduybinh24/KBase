import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import MockAdapter from 'axios-mock-adapter'
import { apiClient } from '../../api/client'
import type { UserProfile } from '../../types/profile'
import { AccountMenu, getUserDisplayName } from './AccountMenu'
import { authValue, renderRoutes, testUser } from '../../test/renderWithAuth'

const mock = new MockAdapter(apiClient)
const profile: UserProfile = {
  userId: testUser.id, email: testUser.email, fullName: 'Person Example', displayName: 'Persisted Person',
  roles: ['USER'], avatarType: 'NONE', avatarPreset: null, hasAvatar: false,
  avatarUpdatedAt: null, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
}

beforeEach(() => mock.onGet('/api/users/me/profile').reply(200, profile))
afterEach(() => mock.reset())

function renderAccountMenu(logout = vi.fn()) {
  return { logout, ...renderRoutes([
    { path: '/dashboard', element: <><AccountMenu /><main>Dashboard content</main></> },
    { path: '/profile', element: <h1>Profile page</h1> },
    { path: '/login', element: <h1>Logged out</h1> },
  ], '/dashboard', authValue({ user: testUser, isAuthenticated: true, logout })) }
}

describe('getUserDisplayName', () => {
  it('falls back to the email local part', () => expect(getUserDisplayName({ id: '1', email: 'huynhduybinh242k5@gmail.com', roles: ['USER'] })).toBe('huynhduybinh242k5'))
  it('prefers the persisted backend profile', () => expect(getUserDisplayName(testUser, profile)).toBe('Persisted Person'))
})

describe('AccountMenu', () => {
  it('loads and displays the backend profile', async () => {
    renderAccountMenu()
    expect(await screen.findByText('Persisted Person')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Open account menu' }))
    expect(screen.getByRole('dialog', { name: 'Account panel' })).toBeInTheDocument()
    expect(screen.getByText(testUser.email)).toBeInTheDocument()
    expect(screen.getByText('USER')).toBeInTheDocument()
  })

  it('closes on outside click and Escape', async () => {
    renderAccountMenu()
    const trigger = screen.getByRole('button', { name: 'Open account menu' })
    await userEvent.click(trigger); await userEvent.click(screen.getByText('Dashboard content'))
    expect(screen.queryByRole('dialog', { name: 'Account panel' })).not.toBeInTheDocument()
    await userEvent.click(trigger); await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('dialog', { name: 'Account panel' })).not.toBeInTheDocument()
  })

  it('updates display name and preset through the backend', async () => {
    mock.onPut('/api/users/me/profile').reply((config) => {
      const request = JSON.parse(config.data)
      return [200, { ...profile, displayName: request.displayName, avatarType: request.avatarType, avatarPreset: request.avatarPreset }]
    })
    renderAccountMenu()
    await screen.findByText('Persisted Person')
    await userEvent.click(screen.getByRole('button', { name: 'Open account menu' }))
    await userEvent.click(screen.getByRole('button', { name: 'Edit profile' }))
    await userEvent.clear(screen.getByLabelText('Display name'))
    await userEvent.type(screen.getByLabelText('Display name'), 'Premium Person')
    await userEvent.click(screen.getByRole('button', { name: 'Use blue avatar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    await waitFor(() => expect(mock.history.put).toHaveLength(1))
    expect(JSON.parse(mock.history.put[0].data)).toEqual({ displayName: 'Premium Person', avatarType: 'PRESET', avatarPreset: 'blue' })
  })

  it('uploads a supported avatar using multipart data', async () => {
    mock.onPut('/api/users/me/profile').reply(200, profile)
    mock.onPost('/api/users/me/avatar').reply(200, { ...profile, avatarType: 'UPLOAD', hasAvatar: true })
    renderAccountMenu()
    await screen.findByText('Persisted Person')
    await userEvent.click(screen.getByRole('button', { name: 'Open account menu' }))
    await userEvent.click(screen.getByRole('button', { name: 'Edit profile' }))
    const file = new File(['avatar'], 'avatar.png', { type: 'image/png' })
    await userEvent.upload(screen.getByLabelText('Upload avatar'), file)
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    await waitFor(() => expect(mock.history.post).toHaveLength(1))
    expect(mock.history.post[0].data).toBeInstanceOf(FormData)
  })

  it('rejects unsupported local avatar files before upload', async () => {
    renderAccountMenu()
    await screen.findByText('Persisted Person')
    await userEvent.click(screen.getByRole('button', { name: 'Open account menu' }))
    await userEvent.click(screen.getByRole('button', { name: 'Edit profile' }))
    await userEvent.upload(screen.getByLabelText('Upload avatar'), new File(['x'], 'x.svg', { type: 'image/svg+xml' }), { applyAccept: false })
    expect(screen.getByRole('alert')).toHaveTextContent('PNG, JPEG, or WebP')
    expect(mock.history.post).toHaveLength(0)
  })

  it('renders a protected avatar blob and revokes its object URL', async () => {
    mock.resetHandlers()
    mock.onGet('/api/users/me/profile').reply(200, { ...profile, avatarType: 'UPLOAD', hasAvatar: true, avatarUpdatedAt: '2026-02-01T00:00:00Z' })
    mock.onGet('/api/users/me/avatar').reply(200, new Blob(['image'], { type: 'image/png' }))
    const view = renderAccountMenu()
    await waitFor(() => expect(document.querySelector('img[src="blob:test-avatar"]')).toBeInTheDocument())
    view.unmount()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:test-avatar')
  })

  it('resets a persisted avatar through the delete endpoint', async () => {
    mock.resetHandlers()
    mock.onGet('/api/users/me/profile').reply(200, { ...profile, avatarType: 'PRESET', avatarPreset: 'blue', hasAvatar: true })
    mock.onPut('/api/users/me/profile').reply(200, { ...profile, avatarType: 'PRESET', avatarPreset: 'blue', hasAvatar: true })
    mock.onDelete('/api/users/me/avatar').reply(204)
    renderAccountMenu()
    await screen.findByText('Persisted Person')
    await userEvent.click(screen.getByRole('button', { name: 'Open account menu' }))
    await userEvent.click(screen.getByRole('button', { name: 'Edit profile' }))
    await userEvent.click(screen.getByRole('button', { name: 'Reset avatar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    await waitFor(() => expect(mock.history.delete).toHaveLength(1))
  })

  it('keeps logout behavior unchanged', async () => {
    const { logout } = renderAccountMenu()
    await userEvent.click(screen.getByRole('button', { name: 'Open account menu' }))
    await userEvent.click(screen.getByRole('button', { name: 'Log out' }))
    expect(logout).toHaveBeenCalledOnce()
    expect(await screen.findByRole('heading', { name: 'Logged out' })).toBeInTheDocument()
  })
})
