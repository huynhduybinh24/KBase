import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

URL.createObjectURL = vi.fn(() => 'blob:test-avatar')
URL.revokeObjectURL = vi.fn()

afterEach(() => {
  cleanup()
  window.localStorage.clear()
})
