'use client'

import { Search } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { InvitationNotifications } from '@/components/layout/invitation-notifications'

interface HeaderProps {
  title: string
  subtitle?: string
}

export function Header({ title, subtitle }: HeaderProps) {
  return (
    <header className="flex h-16 items-center justify-between border-b border-border bg-card px-6">
      <div>
        <h1 className="text-xl font-semibold text-foreground">{title}</h1>
        {subtitle && (
          <p className="text-sm text-muted-foreground">{subtitle}</p>
        )}
      </div>

      <div className="flex items-center gap-3">
        <div className="relative hidden md:block">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Tìm kiếm giao dịch..."
            className="w-64 pl-9"
          />
        </div>
        <InvitationNotifications />
      </div>
    </header>
  )
}
