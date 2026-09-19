import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import MockAdapter from 'axios-mock-adapter'
import { apiClient } from '../api/client'
import { routes } from '../router/router'
import { authValue, renderRoutes, testUser } from '../test/renderWithAuth'

const mock = new MockAdapter(apiClient)

afterEach(() => mock.reset())

describe('Dashboard module navigation', () => {
  it('renders active modules and accessible unavailable modules in the header', async () => {
    renderRoutes(routes, '/dashboard', authValue({ user: testUser, isAuthenticated: true }))
    expect(await screen.findByRole('link', { name: 'Dashboard' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: 'Projects' })).toHaveAttribute('href', '/projects')
    for (const label of ['Documents', 'Search', 'AI Chat']) {
      expect(screen.getByRole('button', { name: label })).toHaveAttribute('aria-disabled', 'true')
    }
  })

  it('navigates to Projects without reloading', async () => {
    mock.onGet('/api/projects').reply(200, [])
    renderRoutes(routes, '/dashboard', authValue({ user: testUser, isAuthenticated: true }))
    await userEvent.click(await screen.findByRole('link', { name: 'Projects' }))
    expect(await screen.findByRole('heading', { name: 'Projects' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Projects' })).toHaveAttribute('aria-current', 'page')
  })

  it('does not navigate when a future module is activated', async () => {
    renderRoutes(routes, '/dashboard', authValue({ user: testUser, isAuthenticated: true }))
    await userEvent.click(await screen.findByRole('button', { name: 'Documents' }))
    expect(screen.getByRole('heading', { name: /Your knowledge/i })).toBeInTheDocument()
  })
})

describe('Protected project routes', () => {
  it('redirects /projects to /login when unauthenticated', async () => {
    renderRoutes(routes, '/projects', authValue())
    expect(await screen.findByRole('heading', { name: 'Welcome back' })).toBeInTheDocument()
  })

  it('redirects /projects/:id to /login when unauthenticated', async () => {
    renderRoutes(routes, '/projects/some-id', authValue())
    expect(await screen.findByRole('heading', { name: 'Welcome back' })).toBeInTheDocument()
  })

  it('renders /projects when authenticated', async () => {
    mock.onGet('/api/projects').reply(200, [])
    renderRoutes(routes, '/projects', authValue({ user: testUser, isAuthenticated: true }))
    expect(await screen.findByRole('heading', { name: 'Projects' })).toBeInTheDocument()
  })
})

describe('Dashboard presentation', () => {
  it('removes the old feature dock while keeping the hero project action', async () => {
    renderRoutes(routes, '/dashboard', authValue({ user: testUser, isAuthenticated: true }))
    expect(await screen.findByRole('link', { name: /Explore projects/i })).toHaveAttribute('href', '/projects')
    expect(screen.queryByText('Explore KBase')).not.toBeInTheDocument()
    expect(screen.queryByText('Your knowledge toolkit')).not.toBeInTheDocument()
    expect(document.querySelector('#dashboard-projects-card')).not.toBeInTheDocument()
  })

  it('shows branding only once in the navbar and not in the hero', async () => {
    renderRoutes(routes, '/dashboard', authValue({ user: testUser, isAuthenticated: true }))
    expect(await screen.findAllByRole('img', { name: 'KBase' })).toHaveLength(1)
  })

  it('opens the compact mobile module menu and keeps account actions separate', async () => {
    renderRoutes(routes, '/dashboard', authValue({ user: testUser, isAuthenticated: true }))
    await userEvent.click(await screen.findByRole('button', { name: 'Toggle navigation menu' }))
    const mobileMenu = screen.getByText('More modules').parentElement
    expect(mobileMenu).not.toBeNull()
    expect(within(mobileMenu!).getAllByText('Coming soon')).toHaveLength(3)
    expect(within(mobileMenu!).queryByRole('button', { name: 'Log out' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Open account menu' })).toBeInTheDocument()
  })
})
