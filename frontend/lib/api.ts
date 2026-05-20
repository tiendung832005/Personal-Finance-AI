export type ApiResponse<T> = {
  success: boolean
  message?: string
  data?: T
}

export class ApiError extends Error {
  status: number
  payload?: unknown
  constructor(message: string, status: number, payload?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.payload = payload
  }
}

const TOKEN_KEY = 'pfia.token'

export function getToken(): string | null {
  if (typeof window === 'undefined') return null
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (typeof window === 'undefined') return
  if (!token) localStorage.removeItem(TOKEN_KEY)
  else localStorage.setItem(TOKEN_KEY, token)
}

export function logout() {
  setToken(null)
}

export function getApiBaseUrl(): string {
  return (process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')
}

export async function apiFetch<T>(
  path: string,
  init: RequestInit & { skipAuth?: boolean } = {}
): Promise<ApiResponse<T>> {
  const { skipAuth, ...requestInit } = init
  const headers = new Headers(requestInit.headers)
  headers.set('Content-Type', 'application/json')

  if (!skipAuth) {
    const token = getToken()
    if (token) headers.set('Authorization', `Bearer ${token}`)
  }

  const res = await fetch(`${getApiBaseUrl()}${path}`, {
    ...requestInit,
    headers,
  })

  const text = await res.text()
  const payload = text ? safeJsonParse(text) : undefined

  if (!res.ok) {
    const msg =
      (payload as any)?.message ||
      (payload as any)?.error ||
      `Request failed: ${res.status}`
    throw new ApiError(msg, res.status, payload)
  }

  return (payload || { success: true }) as ApiResponse<T>
}

function safeJsonParse(text: string) {
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

export function decodeJwtPayload(token: string): any | null {
  try {
    const [, payload] = token.split('.')
    if (!payload) return null
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(json)
  } catch {
    return null
  }
}

