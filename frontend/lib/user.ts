import { apiFetch } from '@/lib/api'

export type UserProfile = {
  id: number
  email: string
  fullName: string
  avatarUrl: string | null
  phone: string | null
  createdAt: string
}

export async function getMyProfile(): Promise<UserProfile> {
  const res = await apiFetch<UserProfile>('/api/users/me', { method: 'GET' })
  if (!res.data) throw new Error('Profile not found')
  return res.data
}

export async function updateMyProfile(body: {
  fullName: string
  avatarUrl?: string | null
  phone?: string | null
}): Promise<UserProfile> {
  const res = await apiFetch<UserProfile>('/api/users/me', {
    method: 'PATCH',
    body: JSON.stringify(body),
  })
  if (!res.data) throw new Error('Failed to update profile')
  return res.data
}

export async function changeMyPassword(body: {
  currentPassword: string
  newPassword: string
}): Promise<void> {
  await apiFetch<void>('/api/users/me/change-password', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function profileInitials(fullName: string): string {
  return fullName
    .split(' ')
    .filter(Boolean)
    .map(n => n[0])
    .join('')
    .slice(-2)
    .toUpperCase()
}
