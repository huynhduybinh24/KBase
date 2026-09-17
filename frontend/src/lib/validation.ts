export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function validateEmail(email: string): string | null {
  if (!email.trim()) return 'Email is required.'
  if (!EMAIL_PATTERN.test(email)) return 'Enter a valid email address.'
  if (email.length > 320) return 'Email must be 320 characters or fewer.'
  return null
}

export function validateRegistrationPassword(password: string): string | null {
  if (!password) return 'Password is required.'
  if (password.length < 8) return 'Password must be at least 8 characters.'
  if (new TextEncoder().encode(password).length > 72) {
    return 'Password must be 72 UTF-8 bytes or fewer.'
  }
  return null
}
