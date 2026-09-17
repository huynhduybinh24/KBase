import { screen } from '@testing-library/react'
import { routes } from './router'
import { authValue, renderRoutes, testUser } from '../test/renderWithAuth'

describe('application router', () => {
  it('routes unauthenticated root to login', async () => {
    renderRoutes(routes, '/', authValue())
    expect(await screen.findByRole('heading', { name: 'Welcome back' })).toBeInTheDocument()
  })

  it('routes authenticated root to dashboard', async () => {
    renderRoutes(routes, '/', authValue({ user: testUser, isAuthenticated: true }))
    expect(await screen.findByText('Your knowledge, ready to become useful.')).toBeInTheDocument()
  })

  it('renders the not-found page', async () => {
    renderRoutes(routes, '/does-not-exist', authValue())
    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
  })
})
