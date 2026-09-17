export interface BackendApiError {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  fieldErrors: Record<string, string>
}

export class ApiClientError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
    public readonly fieldErrors: Record<string, string> = {},
    public readonly requestId?: string,
    public readonly retryAfterSeconds?: number,
  ) {
    super(message)
    this.name = 'ApiClientError'
  }
}
