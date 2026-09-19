import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../api/authApi'
import { AUTH_INVALIDATED_EVENT } from '../api/client'
import { clearAccessToken, getAccessToken, setAccessToken } from '../lib/storage'
import type { LoginRequest, RegisterRequest, UserResponse } from '../types/auth'

export interface AuthContextValue {
  user: UserResponse | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (request: LoginRequest) => Promise<void>
  register: (request: RegisterRequest) => Promise<void>
  googleLogin: (credential: string) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const logout = useCallback(() => {
    clearAccessToken()
    setUser(null)
  }, [])

  useEffect(() => {
    let active = true
    async function restore() {
      if (!getAccessToken()) {
        setIsLoading(false)
        return
      }
      try {
        const restored = await authApi.getCurrentUser()
        if (active) setUser(restored)
      } catch {
        clearAccessToken()
        if (active) setUser(null)
      } finally {
        if (active) setIsLoading(false)
      }
    }
    void restore()
    return () => { active = false }
  }, [])

  useEffect(() => {
    window.addEventListener(AUTH_INVALIDATED_EVENT, logout)
    return () => window.removeEventListener(AUTH_INVALIDATED_EVENT, logout)
  }, [logout])

  const login = useCallback(async (request: LoginRequest) => {
    const response = await authApi.login(request)
    setAccessToken(response.accessToken)
    setUser(response.user)
  }, [])

  const register = useCallback(async (request: RegisterRequest) => {
    await authApi.register(request)
  }, [])

  const googleLogin = useCallback(async (credential: string) => {
    const response = await authApi.googleLogin(credential)
    setAccessToken(response.accessToken)
    setUser(response.user)
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    user,
    isAuthenticated: user !== null,
    isLoading,
    login,
    register,
    googleLogin,
    logout,
  }), [user, isLoading, login, register, googleLogin, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
