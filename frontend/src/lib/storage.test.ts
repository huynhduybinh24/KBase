import { clearAccessToken, getAccessToken, setAccessToken } from './storage'

describe('token storage', () => {
  it('saves, reads, and clears the access token centrally', () => {
    expect(getAccessToken()).toBeNull()
    setAccessToken('signed-token')
    expect(getAccessToken()).toBe('signed-token')
    clearAccessToken()
    expect(getAccessToken()).toBeNull()
  })
})
