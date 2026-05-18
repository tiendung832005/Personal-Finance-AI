'use client'

import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { Bell } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'
import { PendingInvitationsPanel } from '@/components/family/pending-invitations-panel'
import { listMyPendingInvitations } from '@/lib/groups'
import { getToken } from '@/lib/api'
import { cn } from '@/lib/utils'

export function InvitationNotifications() {
  const [count, setCount] = useState(0)
  const [open, setOpen] = useState(false)

  const refreshCount = useCallback(async () => {
    if (!getToken()) {
      setCount(0)
      return
    }
    try {
      const list = await listMyPendingInvitations()
      setCount(list.length)
    } catch {
      setCount(0)
    }
  }, [])

  useEffect(() => {
    refreshCount()
    const id = setInterval(refreshCount, 60_000)
    return () => clearInterval(id)
  }, [refreshCount])

  return (
    <Popover
      open={open}
      onOpenChange={next => {
        setOpen(next)
        if (next) refreshCount()
      }}
    >
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" className="relative" aria-label="Thông báo lời mời">
          <Bell className="h-5 w-5" />
          {count > 0 && (
            <span
              className={cn(
                'absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-bold text-destructive-foreground'
              )}
            >
              {count > 9 ? '9+' : count}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-80 p-0">
        <div className="border-b border-border px-4 py-3">
          <p className="font-semibold text-sm">Lời mời tham gia nhóm</p>
          <p className="text-xs text-muted-foreground">
            {count > 0
              ? `${count} lời mời đang chờ bạn`
              : 'Không có lời mời mới'}
          </p>
        </div>
        {count > 0 ? (
          <PendingInvitationsPanel
            compact
            onChanged={() => {
              refreshCount()
              setOpen(false)
            }}
          />
        ) : (
          <p className="px-4 py-6 text-center text-sm text-muted-foreground">
            Chưa có lời mời nào.
          </p>
        )}
        <div className="border-t border-border p-2">
          <Button variant="ghost" size="sm" className="w-full" asChild>
            <Link href="/family" onClick={() => setOpen(false)}>
              Xem trang Gia đình
            </Link>
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  )
}
