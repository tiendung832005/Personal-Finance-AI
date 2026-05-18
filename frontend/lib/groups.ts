import { apiFetch, decodeJwtPayload, getToken } from '@/lib/api'

export type GroupRole = 'ADMIN' | 'MEMBER'
export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'EXPIRED' | 'CANCELLED'

export type Group = {
  id: number
  name: string
  description: string | null
  createdBy: number
  memberCount: number
  myRole: GroupRole
  createdAt: string
}

export type GroupMember = {
  userId: number
  fullName: string
  email: string
  role: GroupRole
  joinedAt: string
}

export type Invitation = {
  id: number
  groupId: number
  groupName: string
  email: string
  status: InvitationStatus
  expiresAt: string
  inviteLink: string
}

export function getCurrentUserEmail(): string | null {
  const token = getToken()
  if (!token) return null
  const payload = decodeJwtPayload(token)
  return (payload?.sub as string) || null
}

export async function listGroups(): Promise<Group[]> {
  const res = await apiFetch<Group[]>('/api/groups', { method: 'GET' })
  return res.data ?? []
}

export async function getGroup(id: number): Promise<Group> {
  const res = await apiFetch<Group>(`/api/groups/${id}`, { method: 'GET' })
  if (!res.data) throw new Error('Group not found')
  return res.data
}

export async function createGroup(body: {
  name: string
  description?: string
}): Promise<Group> {
  const res = await apiFetch<Group>('/api/groups', {
    method: 'POST',
    body: JSON.stringify(body),
  })
  if (!res.data) throw new Error('Failed to create group')
  return res.data
}

export async function deleteGroup(id: number): Promise<void> {
  await apiFetch<void>(`/api/groups/${id}`, { method: 'DELETE' })
}

export async function listMembers(groupId: number): Promise<GroupMember[]> {
  const res = await apiFetch<GroupMember[]>(`/api/groups/${groupId}/members`, {
    method: 'GET',
  })
  return res.data ?? []
}

export async function inviteMember(
  groupId: number,
  email: string
): Promise<Invitation> {
  const res = await apiFetch<Invitation>(`/api/groups/${groupId}/invite`, {
    method: 'POST',
    body: JSON.stringify({ email }),
  })
  if (!res.data) throw new Error('Failed to create invitation')
  return res.data
}

export async function listPendingInvitations(groupId: number): Promise<Invitation[]> {
  const res = await apiFetch<Invitation[]>(`/api/groups/${groupId}/invitations`, {
    method: 'GET',
  })
  return res.data ?? []
}

export async function listMyPendingInvitations(): Promise<Invitation[]> {
  const res = await apiFetch<Invitation[]>('/api/invitations/pending', { method: 'GET' })
  return res.data ?? []
}

export function tokenFromInviteLink(link: string): string | null {
  try {
    return new URL(link).searchParams.get('token')
  } catch {
    const match = link.match(/[?&]token=([^&]+)/)
    return match ? decodeURIComponent(match[1]) : null
  }
}

export async function acceptInvitation(token: string): Promise<Group> {
  const res = await apiFetch<Group>(
    `/api/invitations/accept?token=${encodeURIComponent(token)}`,
    { method: 'POST' }
  )
  if (!res.data) throw new Error('Failed to accept invitation')
  return res.data
}

export async function kickMember(groupId: number, userId: number): Promise<void> {
  await apiFetch<void>(`/api/groups/${groupId}/members/${userId}`, {
    method: 'DELETE',
  })
}

export async function updateMemberRole(
  groupId: number,
  userId: number,
  role: GroupRole
): Promise<GroupMember> {
  const res = await apiFetch<GroupMember>(
    `/api/groups/${groupId}/members/${userId}/role`,
    {
      method: 'PATCH',
      body: JSON.stringify({ role }),
    }
  )
  if (!res.data) throw new Error('Failed to update role')
  return res.data
}

export async function leaveGroup(groupId: number): Promise<void> {
  await apiFetch<void>(`/api/groups/${groupId}/members/me`, { method: 'DELETE' })
}

export function roleLabel(role: GroupRole): string {
  return role === 'ADMIN' ? 'Quản trị' : 'Thành viên'
}

export function initialsFromName(name: string): string {
  return name
    .split(' ')
    .map(n => n[0])
    .join('')
    .slice(-2)
    .toUpperCase()
}
