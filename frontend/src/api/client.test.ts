import MockAdapter from 'axios-mock-adapter'
import { apiClient, AUTH_INVALIDATED_EVENT } from './client'
import { getAccessToken, setAccessToken } from '../lib/storage'
import { ApiClientError } from '../types/api'

const mock = new MockAdapter(apiClient)

afterEach(() => mock.reset())

describe('API client', () => {
  it('adds Authorization when a token exists', async () => {
    setAccessToken('abc')
    mock.onGet('/protected').reply((config) => [200, { authorization: config.headers?.Authorization }])
    const response = await apiClient.get('/protected')
    expect(response.data.authorization).toBe('Bearer abc')
  })

  it('does not add Authorization without a token', async () => {
    mock.onGet('/public').reply((config) => [200, { authorization: config.headers?.Authorization ?? null }])
    const response = await apiClient.get('/public')
    expect(response.data.authorization).toBeNull()
  })

  it('clears expired auth, emits invalidation, and preserves diagnostics', async () => {
    setAccessToken('expired')
    const listener = vi.fn()
    window.addEventListener(AUTH_INVALIDATED_EVENT, listener)
    mock.onGet('/api/users/me').reply(401, {
      timestamp: '2026-01-01T00:00:00Z', status: 401, error: 'Unauthorized',
      message: 'Authentication is required', path: '/api/users/me', fieldErrors: {},
    }, { 'X-Request-ID': 'request-7' })

    const error = await apiClient.get('/api/users/me').catch((caught) => caught)
    expect(error).toBeInstanceOf(ApiClientError)
    expect(error.requestId).toBe('request-7')
    expect(getAccessToken()).toBeNull()
    expect(listener).toHaveBeenCalledOnce()
    window.removeEventListener(AUTH_INVALIDATED_EVENT, listener)
  })

  it('does not emit invalidation for a rejected login', async () => {
    setAccessToken('previous')
    const listener = vi.fn()
    window.addEventListener(AUTH_INVALIDATED_EVENT, listener)
    mock.onPost('/api/auth/login').reply(401, {})
    await expect(apiClient.post('/api/auth/login')).rejects.toBeInstanceOf(ApiClientError)
    expect(listener).not.toHaveBeenCalled()
    window.removeEventListener(AUTH_INVALIDATED_EVENT, listener)
  })
})
