import { screen, waitFor } from '@testing-library/react'
import { GoogleSignInButton } from './GoogleSignInButton'
import { authValue, renderRoutes } from '../../test/renderWithAuth'

describe('GoogleSignInButton', () => {
  afterEach(() => { delete window.google })

  it('passes the transient credential to AuthProvider and navigates', async () => {
    const googleLogin = vi.fn().mockResolvedValue(undefined)
    let callback!: (response: GoogleCredentialResponse) => void
    window.google = { accounts: { id: {
      initialize: vi.fn((config) => { callback = config.callback }),
      renderButton: vi.fn((parent) => { const button = document.createElement('button'); button.textContent = 'Continue with Google'; parent.appendChild(button) }),
      cancel: vi.fn(),
    } } }
    renderRoutes([
      { path: '/login', element: <GoogleSignInButton clientId="client-id" /> },
      { path: '/dashboard', element: <h1>Dashboard reached</h1> },
    ], '/login', authValue({ googleLogin }))
    await waitFor(() => expect(screen.getByRole('button', { name: 'Continue with Google' })).toBeInTheDocument())
    await callback({ credential: 'one-time-google-token' })
    expect(await screen.findByText('Dashboard reached')).toBeInTheDocument()
    expect(googleLogin).toHaveBeenCalledWith('one-time-google-token')
  })

  it('fails safely when the client ID is missing', () => {
    renderRoutes([{ path: '/login', element: <GoogleSignInButton clientId="" /> }], '/login', authValue())
    expect(screen.getByRole('button', { name: 'Google sign-in is not configured.' })).toBeDisabled()
  })
})
