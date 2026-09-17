import axios, { AxiosError, AxiosHeaders } from 'axios'
import { clearAccessToken, getAccessToken } from '../lib/storage'
import { ApiClientError, type BackendApiError } from '../types/api'

export const AUTH_INVALIDATED_EVENT = 'kbase:auth-invalidated'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  headers: { Accept: 'application/json' },
  timeout: 30_000,
})

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<BackendApiError>) => {
    const status = error.response?.status
    const requestUrl = error.config?.url ?? ''
    const hadToken = Boolean(getAccessToken())
    if (status === 401 && hadToken && !requestUrl.endsWith('/api/auth/login')) {
      clearAccessToken()
      window.dispatchEvent(new Event(AUTH_INVALIDATED_EVENT))
    }

    const data = error.response?.data
    const headers = error.response?.headers
    const retryHeader = getHeader(headers, 'retry-after')
    const retryAfterSeconds = retryHeader ? Number.parseInt(String(retryHeader), 10) : undefined
    const requestId = getHeader(headers, 'x-request-id')
    const fallback = status === 429
      ? 'Too many requests. Please wait and try again.'
      : status === 503
        ? 'The service is temporarily unavailable. Please try again shortly.'
        : 'Something went wrong. Please try again.'

    return Promise.reject(new ApiClientError(
      data?.message || fallback,
      status,
      data?.fieldErrors ?? {},
      requestId,
      Number.isNaN(retryAfterSeconds) ? undefined : retryAfterSeconds,
    ))
  },
)

export function getErrorMessage(error: unknown): string {
  if (error instanceof ApiClientError) {
    if (error.status === 429 && error.retryAfterSeconds) {
      return `${error.message} Try again in ${error.retryAfterSeconds} seconds.`
    }
    return error.message
  }
  return 'Something went wrong. Please try again.'
}

function getHeader(headers: unknown, name: string): string | undefined {
  if (!headers || typeof headers !== 'object') return undefined
  if (headers instanceof AxiosHeaders) {
    const value = headers.get(name)
    return value == null ? undefined : String(value)
  }
  const entry = Object.entries(headers as Record<string, unknown>)
    .find(([key]) => key.toLowerCase() === name.toLowerCase())
  return entry?.[1] == null ? undefined : String(entry[1])
}
