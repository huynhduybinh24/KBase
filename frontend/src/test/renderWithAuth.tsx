import { render } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createMemoryRouter, RouterProvider, type RouteObject } from 'react-router-dom'
import { AuthContext, type AuthContextValue } from '../auth/AuthProvider'

export const testUser = { id: 'user-uuid-123', email: 'person@example.com', roles: ['USER'] }

export function authValue(overrides: Partial<AuthContextValue> = {}): AuthContextValue {
  return {
    user: null, isAuthenticated: false, isLoading: false,
    login: vi.fn(), register: vi.fn(), googleLogin: vi.fn(), logout: vi.fn(), ...overrides,
  }
}

export function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  })
}

export function renderRoutes(
  routes: RouteObject[],
  initialEntry: string,
  auth: AuthContextValue,
  queryClient?: QueryClient,
) {
  const qc = queryClient ?? makeQueryClient()
  const router = createMemoryRouter(routes, { initialEntries: [initialEntry] })
  return {
    router,
    queryClient: qc,
    ...render(
      <QueryClientProvider client={qc}>
        <AuthContext.Provider value={auth}>
          <RouterProvider router={router} />
        </AuthContext.Provider>
      </QueryClientProvider>,
    ),
  }
}
