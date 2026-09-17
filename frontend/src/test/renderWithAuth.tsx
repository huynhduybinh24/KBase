import { render } from '@testing-library/react'
import { createMemoryRouter, RouterProvider, type RouteObject } from 'react-router-dom'
import { AuthContext, type AuthContextValue } from '../auth/AuthProvider'

export const testUser = { id: 'user-id', email: 'person@example.com', roles: ['USER'] }

export function authValue(overrides: Partial<AuthContextValue> = {}): AuthContextValue {
  return {
    user: null, isAuthenticated: false, isLoading: false,
    login: vi.fn(), register: vi.fn(), logout: vi.fn(), ...overrides,
  }
}

export function renderRoutes(routes: RouteObject[], initialEntry: string, auth: AuthContextValue) {
  const router = createMemoryRouter(routes, { initialEntries: [initialEntry] })
  return {
    router,
    ...render(<AuthContext.Provider value={auth}><RouterProvider router={router} /></AuthContext.Provider>),
  }
}
