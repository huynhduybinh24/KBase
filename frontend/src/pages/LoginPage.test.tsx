import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { LoginPage } from './LoginPage'
import { authValue, renderRoutes } from '../test/renderWithAuth'
import { ApiClientError } from '../types/api'

function setup(login = vi.fn()) {
  return { login, ...renderRoutes([
    { path: '/login', element: <LoginPage /> },
    { path: '/dashboard', element: <h1>Dashboard reached</h1> },
    { path: '/register', element: <h1>Register</h1> },
  ], '/login', authValue({ login })) }
}

describe('LoginPage', () => {
  it('renders and prevents invalid submission', async () => {
    const { login } = setup()
    expect(screen.getByRole('heading', { name: 'Welcome back' })).toBeInTheDocument()
    expect(screen.getAllByRole('img', { name: 'KBase' })).toHaveLength(1)
    expect(screen.getByRole('img', { name: 'KBase' })).toHaveAttribute('src', '/assets/kbase/kbase-logo-web.png')
    expect(screen.queryByText('Build knowledge that stays useful.')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Continue with Google')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Email'), 'invalid')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(screen.getByText('Enter a valid email address.')).toBeInTheDocument()
    expect(login).not.toHaveBeenCalled()
  })

  it('logs in and routes to dashboard', async () => {
    const login = vi.fn().mockResolvedValue(undefined)
    setup(login)
    await userEvent.type(screen.getByLabelText('Email'), 'person@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'Password123!')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByText('Dashboard reached')).toBeInTheDocument()
    expect(login).toHaveBeenCalledWith({ email: 'person@example.com', password: 'Password123!' })
  })

  it('shows a safe backend failure', async () => {
    setup(vi.fn().mockRejectedValue(new ApiClientError('Invalid email or password', 401)))
    await userEvent.type(screen.getByLabelText('Email'), 'person@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password')
  })

  it('disables submit while pending', async () => {
    let finish!: () => void
    setup(vi.fn(() => new Promise<void>((resolve) => { finish = resolve })))
    await userEvent.type(screen.getByLabelText('Email'), 'person@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'Password123!')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(screen.getByRole('button', { name: 'Signing in…' })).toBeDisabled()
    finish()
    await waitFor(() => expect(screen.queryByText('Signing in…')).not.toBeInTheDocument())
  })
})
