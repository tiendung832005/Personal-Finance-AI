'use client'

import { useCallback, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Button } from '@/components/ui/button'
import {
  acceptInvitation,
  listMyPendingInvitations,
  tokenFromInviteLink,
  type Invitation,
} from '@/lib/groups'
import { ApiError } from '@/lib/api'
import { useToast } from '@/hooks/use-toast'
import { Bell, Check, Loader2, Users, X } from 'lucide-react'

type Props = {
  compact?: boolean
  onChanged?: () => void
}

export function PendingInvitationsPanel({ compact = false, onChanged }: Props) {
  const router = useRouter()
  const { toast } = useToast()
  const [invitations, setInvitations] = useState<Invitation[]>([])
  const [loading, setLoading] = useState(true)
  const [acceptingId, setAcceptingId] = useState<number | null>(null)
  const [dismissed, setDismissed] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listMyPendingInvitations()
      setInvitations(data)
      if (data.length > 0) setDismissed(false)
    } catch {
      setInvitations([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const handleAccept = async (inv: Invitation) => {
    const token = tokenFromInviteLink(inv.inviteLink)
    if (!token) {
      toast({
        title: 'Lỗi',
        description: 'Link mời không hợp lệ',
        variant: 'destructive',
      })
      return
    }
    setAcceptingId(inv.id)
    try {
      const group = await acceptInvitation(token)
      toast({ title: 'Đã tham gia nhóm', description: group.name })
      await load()
      onChanged?.()
      router.push(`/family/${group.id}`)
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Không thể chấp nhận lời mời'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
      await load()
    } finally {
      setAcceptingId(null)
    }
  }

  if (loading || invitations.length === 0 || dismissed) {
    return null
  }

  if (compact) {
    return (
      <ul className="max-h-80 space-y-2 overflow-y-auto p-1">
        {invitations.map(inv => (
          <li
            key={inv.id}
            className="rounded-lg border border-border bg-secondary/30 p-3 text-sm"
          >
            <p className="font-medium text-foreground">{inv.groupName}</p>
            <p className="text-xs text-muted-foreground mt-1">
              Hết hạn {new Date(inv.expiresAt).toLocaleDateString('vi-VN')}
            </p>
            <Button
              size="sm"
              className="mt-2 w-full gap-1"
              disabled={acceptingId === inv.id}
              onClick={() => handleAccept(inv)}
            >
              {acceptingId === inv.id ? (
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
              ) : (
                <Check className="h-3.5 w-3.5" />
              )}
              Chấp nhận
            </Button>
          </li>
        ))}
      </ul>
    )
  }

  return (
    <div className="mb-6 rounded-xl border border-primary/30 bg-primary/5 p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="flex gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/15">
            <Bell className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h2 className="font-semibold text-foreground">
              Bạn có {invitations.length} lời mời tham gia nhóm
            </h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Chấp nhận để tham gia quản lý tài chính gia đình cùng nhóm.
            </p>
          </div>
        </div>
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 shrink-0"
          onClick={() => setDismissed(true)}
          aria-label="Ẩn tạm"
        >
          <X className="h-4 w-4" />
        </Button>
      </div>

      <ul className="mt-4 space-y-3">
        {invitations.map(inv => (
          <li
            key={inv.id}
            className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-border bg-card px-4 py-3"
          >
            <div className="flex items-center gap-3 min-w-0">
              <Users className="h-5 w-5 shrink-0 text-muted-foreground" />
              <div>
                <p className="font-medium text-foreground">{inv.groupName}</p>
                <p className="text-xs text-muted-foreground">
                  Hết hạn: {new Date(inv.expiresAt).toLocaleString('vi-VN')}
                </p>
              </div>
            </div>
            <Button
              size="sm"
              className="gap-1 shrink-0"
              disabled={acceptingId === inv.id}
              onClick={() => handleAccept(inv)}
            >
              {acceptingId === inv.id ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Check className="h-4 w-4" />
              )}
              Chấp nhận lời mời
            </Button>
          </li>
        ))}
      </ul>
    </div>
  )
}
