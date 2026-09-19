import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Header } from './Header'
import { authValue, renderRoutes, testUser } from '../../test/renderWithAuth'

describe('Header logout', () => {
  it('clears auth through the provider action and navigates to login', async () => {
    const logout = vi.fn()
    renderRoutes([
      { path: '/dashboard', element: <Header /> },
      { path: '/login', element: <h1>Logged out</h1> },
    ], '/dashboard', authValue({ user: testUser, isAuthenticated: true, logout }))
    expect(screen.queryByRole('button', { name: 'Log out' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Open account menu' }))
    await userEvent.click(screen.getByRole('button', { name: 'Log out' }))
    expect(logout).toHaveBeenCalledOnce()
    expect(await screen.findByText('Logged out')).toBeInTheDocument()
  })
})
