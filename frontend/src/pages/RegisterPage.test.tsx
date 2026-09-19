import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RegisterPage } from './RegisterPage'
import { authValue, renderRoutes } from '../test/renderWithAuth'
import { ApiClientError } from '../types/api'

function setup(register = vi.fn()) {
  return { register, ...renderRoutes([
    { path: '/register', element: <RegisterPage /> },
    { path: '/login', element: <h1>Login after registration</h1> },
  ], '/register', authValue({ register })) }
}

async function fill(password = 'Password123!', confirm = password) {
  await userEvent.type(screen.getByLabelText('Full Name'), 'Person Example')
  await userEvent.type(screen.getByLabelText('Email'), 'person@example.com')
  await userEvent.type(screen.getByLabelText('Password'), password)
  await userEvent.type(screen.getByLabelText('Confirm password'), confirm)
}

describe('RegisterPage', () => {
  it('requires a full name without changing the backend payload', async () => {
    const { register } = setup()
    await userEvent.type(screen.getByLabelText('Email'), 'person@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'Password123!')
    await userEvent.type(screen.getByLabelText('Confirm password'), 'Password123!')
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }))
    expect(screen.getByText('Enter your full name.')).toBeInTheDocument()
    expect(register).not.toHaveBeenCalled()
  })

  it('renders and rejects invalid email', async () => {
    const { register } = setup()
    expect(screen.getByRole('heading', { name: 'Create your account' })).toBeInTheDocument()
    expect(screen.getByLabelText('Continue with Google')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Email'), 'bad-email')
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }))
    expect(screen.getByText('Enter a valid email address.')).toBeInTheDocument()
    expect(register).not.toHaveBeenCalled()
  })

  it('rejects a password mismatch', async () => {
    const { register } = setup()
    await fill('Password123!', 'Password456!')
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }))
    expect(screen.getByText('Passwords do not match.')).toBeInTheDocument()
    expect(register).not.toHaveBeenCalled()
  })

  it('registers and routes to login', async () => {
    const register = vi.fn().mockResolvedValue(undefined)
    setup(register)
    await fill()
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }))
    expect(await screen.findByText('Login after registration')).toBeInTheDocument()
    expect(register).toHaveBeenCalledWith({ fullName: 'Person Example', email: 'person@example.com', password: 'Password123!' })
  })

  it('shows backend registration errors', async () => {
    setup(vi.fn().mockRejectedValue(new ApiClientError('Email is already registered', 409)))
    await fill()
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Email is already registered')
  })
})
