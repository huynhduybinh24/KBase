import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AuthProvider } from './AuthProvider'
import { useAuth } from './useAuth'
import { getAccessToken, setAccessToken } from '../lib/storage'
import * as authApi from '../api/authApi'

vi.mock('../api/authApi')
const user = { id: 'user-id', email: 'person@example.com', roles: ['USER'] }

function Probe() {
  const auth = useAuth()
  return <><span>{auth.isLoading ? 'loading' : auth.user?.email ?? 'anonymous'}</span><button onClick={auth.logout}>logout</button><button onClick={() => void auth.login({ email: 'person@example.com', password: 'Password123!' })}>login</button></>
}

describe('AuthProvider', () => {
  it('restores a valid token through /api/users/me', async () => {
    setAccessToken('valid')
    vi.mocked(authApi.getCurrentUser).mockResolvedValue(user)
    render(<AuthProvider><Probe /></AuthProvider>)
    expect(screen.getByText('loading')).toBeInTheDocument()
    expect(await screen.findByText(user.email)).toBeInTheDocument()
    expect(authApi.getCurrentUser).toHaveBeenCalledOnce()
  })

  it('clears an invalid token and becomes unauthenticated', async () => {
    setAccessToken('invalid')
    vi.mocked(authApi.getCurrentUser).mockRejectedValue(new Error('invalid'))
    render(<AuthProvider><Probe /></AuthProvider>)
    expect(await screen.findByText('anonymous')).toBeInTheDocument()
    expect(getAccessToken()).toBeNull()
  })

  it('clears auth state and token on logout', async () => {
    setAccessToken('valid')
    vi.mocked(authApi.getCurrentUser).mockResolvedValue(user)
    render(<AuthProvider><Probe /></AuthProvider>)
    await screen.findByText(user.email)
    await userEvent.click(screen.getByRole('button', { name: 'logout' }))
    await waitFor(() => expect(screen.getByText('anonymous')).toBeInTheDocument())
    expect(getAccessToken()).toBeNull()
  })

  it('stores the returned token and authenticated user on login', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: 'new-token', tokenType: 'Bearer', expiresIn: 3600, user,
    })
    render(<AuthProvider><Probe /></AuthProvider>)
    await screen.findByText('anonymous')
    await userEvent.click(screen.getByRole('button', { name: 'login' }))
    expect(await screen.findByText(user.email)).toBeInTheDocument()
    expect(getAccessToken()).toBe('new-token')
  })
})
