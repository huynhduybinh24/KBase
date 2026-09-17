import { screen } from '@testing-library/react'
import { ProtectedRoute } from './ProtectedRoute'
import { authValue, renderRoutes, testUser } from '../test/renderWithAuth'

const routes = [
  { element: <ProtectedRoute />, children: [{ path: '/dashboard', element: <h1>Protected dashboard</h1> }] },
  { path: '/login', element: <h1>Login destination</h1> },
]

describe('ProtectedRoute', () => {
  it('redirects unauthenticated users to login', async () => {
    renderRoutes(routes, '/dashboard', authValue())
    expect(await screen.findByText('Login destination')).toBeInTheDocument()
  })

  it('renders protected content for authenticated users', async () => {
    renderRoutes(routes, '/dashboard', authValue({ user: testUser, isAuthenticated: true }))
    expect(await screen.findByText('Protected dashboard')).toBeInTheDocument()
  })

  it('shows loading UI without redirecting while auth restores', () => {
    renderRoutes(routes, '/dashboard', authValue({ isLoading: true }))
    expect(screen.getByText('Loading your workspace…')).toBeInTheDocument()
    expect(screen.queryByText('Login destination')).not.toBeInTheDocument()
  })
})
